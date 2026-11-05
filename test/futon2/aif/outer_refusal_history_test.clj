(ns futon2.aif.outer-refusal-history-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.outer-refusal-index :as index]
            [futon2.aif.outer-refusal-history :as history]
            [futon2.test-support.runner-fixture :as fixture]))

(def retained-refusal
  ;; Real refused M-recommendation-bindings terminal record.
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d.edn")

(def retained-repick
  ;; The later real re-pick changed its selection input digest.
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-27-39eb74f5-9db6-4f1d-8d28-03694c6f1fba.edn")

(defn- temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-refusal-history" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- index-record! [dir source]
  (let [record (edn/read-string (slurp source))]
    (index/append-entry!
     (io/file dir history/index-name)
     (history/index-entry record source (index/sha256-file source)))))

(defn- close-and-index
  "Exercise the owned terminal-record writer before the compact projection."
  [result opts]
  (let [dir (temp-dir)
        written (runner/persist-run-record!
                 (merge (fixture/isolated-runner-opts)
                        {:run-record-dir (.getPath dir)
                         :click-id "click-abstention-reason"}
                        opts)
                 "abstention-reason" "2026-11-05T00:00:00Z" result)
        path (:run-record written)
        record (edn/read-string (slurp path))]
    (history/index-entry record path (index/sha256-file path))))

(defn- abstained-result
  ([sorry data] (abstained-result sorry data nil))
  ([sorry data decision]
   {:attempt-id "attempt-abstention-reason"
    :outcome :incomplete
    :wm/route [{:node :SELECTION :via :abstained :at "2026-11-05T00:00:00Z"}]
    :data data
    :checkpoints {:selection (cond-> {:sorry sorry}
                               decision (assoc :judgment
                                               {:controller-decision decision}))}}))

(deftest every-abstention-source-reaches-the-compact-index
  (let [judge (runner/judge-refusal
               (ex-info "cascade decision refused"
                        {:kind :class-unknown-no-scalar-g
                         :missing :target-relation
                         :target "M-judge"})
               nil)
        gate (runner/gate-refusal
              (ex-info "gate" {:error :inadmissible-decision
                                :reason :missing-observation-locators
                                :detail {:target "M-gate"
                                         :missing-tokens [:locator-file]}})
              nil)
        close (fn [carrier]
                (close-and-index
                 (abstained-result
                  {:kind :no-selection :judge-refusal carrier}
                  {:failure-kind :abstained :failure-stage :selection
                   :error "typed refusal" :error-data {:judge-refusal carrier}})
                 {}))]
    (is (= {:target "M-judge" :reason :class-unknown-no-scalar-g
            :missing [:target-relation] :source :judge}
           (:abstention (close judge))))
    (is (= {:target "M-gate" :reason :missing-observation-locators
            :missing [:locator-file] :source :gate}
           (:abstention (close gate)))))
  (let [decision {:status :abstained :reason :no-acting-cascade-candidate
                  :refusals [{:target "M-selection"
                              :kind :target-source-path-absent
                              :missing [:mission-source]}]}
        row (close-and-index
             (abstained-result
              {:kind :no-selection :decision decision}
              {:failure-kind :abstained :failure-stage :selection
               :error "no addressable action"
               :error-data {:decision decision}}
              decision)
             {})]
    (is (= {:target "M-selection" :reason :no-acting-cascade-candidate
            :missing [:mission-source] :source :selection}
           (:abstention row))))
  (let [reading {:status :refused
                 :kind :selected-target-reading-answer-invalid
                 :target "M-reading"
                 :detail {:answer {:status :refused :reason :response-invalid}}}
        row (close-and-index
             (abstained-result
              {:kind :no-selection}
              {:failure-kind :abstained :failure-stage :selection-world
               :error "reading refused"})
             {:selected-target-reading/state
              (atom {:status :completed :record reading})})]
    (is (= {:target "M-reading"
            :reason :selected-target-reading-answer-invalid
            :missing [] :source :selected-target-reading}
           (:abstention row))))
  (let [row (close-and-index
             (abstained-result {:kind :no-selection}
                               {:failure-kind :no-selection
                                :failure-stage :selection
                                :error "legacy close"})
             {})]
    (is (= {:reason :not-retained} (:abstention row)))))

(deftest absent-index-is-distinct-from-empty-history
  (let [dir (temp-dir)
        inputs {:schema :wm/outer-repeat-inputs-v1 :target "M-no-index"}
        comparison (history/compare-history dir inputs)]
    (is (= :history-index-absent (:status comparison)))
    (is (= inputs (:inputs comparison)))
    (is (empty? (:comparisons comparison)))
    (is (= comparison (history/require-new-inputs! comparison)))))

(deftest retained-repick-has-changed-inputs
  (testing "the real recommendation-bindings re-pick is not stopped"
    (let [dir (temp-dir)
          _ (index-record! dir retained-refusal)
          current (history/record-inputs (edn/read-string (slurp retained-repick)))
          comparison (history/compare-history dir current)]
      (is (= "M-recommendation-bindings" (:target current)))
      (is (= :inputs-changed (:status comparison)))
      (is (= [:selection-input-digest]
             (get-in comparison [:comparisons 0 :differing-identities])))
      (is (nil? (:matched-refusal comparison))))))

(deftest cloned-retained-inputs-are-stopped
  (testing "changing only run id cannot authorize unchanged refused inputs"
    (let [dir (temp-dir)
          source (edn/read-string (slurp retained-refusal))
          _ (index-record! dir retained-refusal)
          cloned (assoc source :run/id "2026-10-29-cloned-inputs")
          comparison (history/compare-history dir (history/record-inputs cloned))
          refusal (try
                    (history/require-new-inputs! comparison)
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (= :unchanged-refused-repeat (:status comparison)))
      (is (= "2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d"
             (get-in comparison [:matched-refusal :run-id])))
      (is (empty? (get-in comparison
                          [:matched-refusal :differing-identities])))
      (is (= :outer/unchanged-refused-repeat
             (:failure-kind (ex-data refusal))))
      (is (= (history/record-inputs cloned)
             (get-in (ex-data refusal)
                     [:outer-repeat-comparison :inputs]))))))
