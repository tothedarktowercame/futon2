(ns futon2.aif.full-loop-cohort-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-cohort :as cohort])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def prereg-path
  "/home/joe/code/futon2/holes/labs/M-aif-full-loop-40/cohort.edn")

(defn tmp-root []
  (.getPath (.toFile (Files/createTempDirectory
                      "full-loop-cohort-test"
                      (make-array FileAttribute 0)))))

(defn term [judgment]
  {:judgment judgment :ground {:kind :test-witness}})

(defn open! [root opportunity-id]
  (cohort/start-attempt!
   prereg-path root
   (term {:opportunity-id opportunity-id
          :trigger :wallclock-cron
          :machine-state {:tick 1}
          :agent-roster []
          :code-state {:git-sha "abc"
                       :git-dirty? false
                       :resolved-mode-flags {}
                       :configuration-digest "test"}
          :semantic-epoch :epoch-1})))

(defn append-required! [root attempt]
  (doseq [checkpoint [:selection :construction :dispatch :build :adjudication]]
    (cohort/append-checkpoint! prereg-path root attempt checkpoint
                               {:sorry {:kind (keyword (str "test-" (name checkpoint)))}})))

(deftest preregistration-is-valid
  (let [p (edn/read-string (slurp prereg-path))]
    (is (cohort/valid-preregistration? p))
    (is (= 40 (get-in p [:stopping-rule :target])))
    (is (false? (get-in p [:grounded-success :artifact-only-counts?])))))

(deftest activation-and-attempts-are-append-only
  (let [root (tmp-root)]
    (testing "pre-activation attempts are refused"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not activated"
                            (open! root "clock/1"))))
    (cohort/activate! prereg-path root)
    (is (= 1 (:attempt/ordinal (open! root "clock/1"))))
    (is (= 2 (:attempt/ordinal (open! root "clock/2"))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"duplicate"
                          (open! root "clock/1")))
    (is (thrown-with-msg? java.nio.file.FileAlreadyExistsException #"activation.edn"
                          (cohort/activate! prereg-path root)))))

(deftest closure-requires-the-whole-dossier
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/complete"))
        close (term {:outcome :agent-unavailable
                     :grounded? false
                     :artifact-only? false
                     :duration-ms 1200
                     :resource-use {:agent-turns 0}})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"checkpoints missing"
                          (cohort/close-attempt! prereg-path root attempt close)))
    (append-required! root attempt)
    (cohort/close-attempt! prereg-path root attempt close)
    (let [ledger (cohort/ledger prereg-path root)]
      (is (= 1 (:attempt-count ledger)))
      (is (= 1 (:closed-count ledger)))
      (is (= {:agent-unavailable 1} (:outcomes ledger)))
      (is (= 5 (get-in ledger [:attempts 0 :typed-sorries]))))))

(deftest grounded-checkpoints-cannot-omit-preregistered-fields
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/incomplete-selection"))]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"invalid checkpoint cell"
         (cohort/append-checkpoint!
          prereg-path root attempt :selection
          (term {:selected-mission "M-x"}))))
    (is (= [[:missing-judgment-key :ranked-candidates]
            [:missing-judgment-key :selected-action]
            [:missing-judgment-key :selection-reasons]]
           (cohort/checkpoint-cell-errors
            (cohort/read-edn prereg-path) :selection
            (term {:selected-mission "M-x"}))))))

(deftest artifact-only-cannot-be-laundered-as-grounded
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/artifact"))]
    (append-required! root attempt)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"invalid close"
                          (cohort/close-attempt!
                           prereg-path root attempt
                           (term {:outcome :grounded-change
                                  :grounded? true
                                  :artifact-only? true
                                  :witness {:before "b" :after "a"
                                            :resolved? true :dial-moved? true}}))))
    (cohort/close-attempt!
     prereg-path root attempt
     (term {:outcome :artifact-only :grounded? false :artifact-only? true
            :duration-ms 20 :resource-use {:agent-turns 0}}))
    (is (= 1 (:artifact-only-count (cohort/ledger prereg-path root))))))

(deftest grounded-change-needs-independent-resolved-witness
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/grounded"))]
    (append-required! root attempt)
    (is (thrown? clojure.lang.ExceptionInfo
                 (cohort/close-attempt!
                  prereg-path root attempt
                  (term {:outcome :grounded-change :grounded? true
                         :artifact-only? false :witness {:resolved? true}}))))
    (cohort/close-attempt!
     prereg-path root attempt
     (term {:outcome :grounded-change :grounded? true :artifact-only? false
            :duration-ms 2000 :resource-use {:agent-turns 1}
            :witness {:before "substrate-before.edn"
                      :after "substrate-after.edn"
                      :resolved? true
                      :dial-moved? true}}))
    (is (= 1 (:grounded-change-count (cohort/ledger prereg-path root))))))

(deftest fresh-ledger-renders-without-legacy-rows
  (let [root (tmp-root)
        value (cohort/ledger prereg-path root)
        html (cohort/render-html value)]
    (is (= 0 (:attempt-count value)))
    (is (= 40 (:remaining value)))
    (is (.contains html "No attempts recorded"))
    (is (.contains html "Historical artifact-only ticks are excluded"))))

(deftest attempt-summary-carries-fresh-repo-observation
  (let [attempt-dir (.toFile (Files/createTempDirectory
                              "cohort-attempt-summary"
                              (make-array FileAttribute 0)))
        event (fn [sequence checkpoint payload]
                {:attempt/id "attempt-016" :attempt/ordinal 16
                 :event/sequence sequence :checkpoint/type checkpoint
                 :payload payload})]
    (spit (io/file attempt-dir "001-time-step.edn")
          (pr-str (event 1 :time-step
                         {:judgment {:opportunity-id "clock/16"}})))
    (spit (io/file attempt-dir "002-selection.edn")
          (pr-str (event 2 :selection
                         {:judgment {:selected-mission "repair-attempt-010"}})))
    (spit (io/file attempt-dir "003-build.edn")
          (pr-str (event 3 :build
                         {:judgment
                          {:validation
                           {:artifact-binding
                            {:fresh-author? true
                             :pre-dispatch-head "2b912f0"
                             :commit "bf14dca"}}}})))
    (let [summary (cohort/attempt-summary attempt-dir)]
      (is (= "bf14dca" (:fresh-commit summary)))
      (is (= "2b912f0"
             (get-in summary [:artifact-binding :pre-dispatch-head]))))))
