(ns futon2.aif.wm.family-selection-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.wm.family-selection :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- with-empty-learning-ledger [run]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "family-selection-ledger-"
                                 (make-array FileAttribute 0))))]
    (with-redefs [ledger/default-root root]
      (run))))

(use-fixtures :each with-empty-learning-ledger)

(defn- analysis [pattern]
  {:status "analyzed"
   :sentences
   [{:id "s1"
     :fragments [{:start 0 :end 1 :text pattern :intent "act" :target "target"
                  :rationale "fixture fit" :relations ["action"]
                  :pattern_refs [{:id pattern :status "candidate"
                                  :rationale "fixture fit"
                                  :source_sha256 (apply str (repeat 64 "a"))}]}]}]})

(defn- fixture-policy [target n]
  (let [pattern (str target "/p" n)]
    {:target target :mission target :policy-id (str target "/policy-" n)
     :kind :reading-alternatives :analysis (analysis pattern)
     :cascade {:nodes [{:pattern pattern :fragment-index 0 :roles ["action"]}]
               :edges [] :precedence [pattern]}}))

(defn- computed-family [target n]
  {:status :computed :target-id target
   :policies (mapv #(fixture-policy target %) (range n))
   :distinct-count n :reported-count n :failures [] :failure-count 0})

(defn- failed-family [target kind]
  {:status :failed :target-id target :policies [] :distinct-count 0
   :failures [{:kind kind}] :failure-count 1})

(deftest joint-selector-scores-every-policy-once
  (let [result (sut/select-over-families [(computed-family "T-a" 2)
                                          (computed-family "T-b" 3)]
                                         {:beta 1 :enactment-fold nil
                                          :novelty-inputs {}})
        ranked (:ranked result)
        scores (mapv (fn [entry]
                       {:entry entry
                        ;; beta=1 and the recorded unseen-policy habit is 1,
                        ;; hence ln(E)-F-G = -F-G.
                        :selection-score (- (+ (:f entry) (:controller-score entry)))})
                     ranked)
        best (apply max (map :selection-score scores))
        expected (->> scores
                      (filter #(== best (:selection-score %)))
                      (sort-by #(pr-str (get-in % [:entry :action :precedence
                                                   :co-apply :units 0])))
                      first :entry :action)]
    (is (= :selected (:status result)))
    (is (= 5 (count ranked)))
    (is (= 5 (count (mapcat :policies (:target-policy-families result)))))
    (doseq [entry ranked]
      (is (Double/isFinite (double (:f entry))))
      (is (Double/isFinite (double (:controller-score entry))))
      (is (= :co-application-frontier-theta-v1 (:transition-kernel entry)))
      (is (= sut/neutral-co-apply-habit (:habit-provenance entry))))
    (is (= expected (get-in result [:decision :action]))
        "the chosen policy is the argmax computed from the recorded F/G posterior")))

(deftest failed-target-is-counted-and-does-not-enter-selection
  (let [failed (assoc (failed-family "T-missing" :no-current-target-reading)
                      ;; A failed family must contribute no placeholder even
                      ;; if malformed input happens to carry one.
                      :policies [(fixture-policy "T-missing" 99)])
        result (sut/select-over-families [failed (computed-family "T-ready" 1)]
                                         {:beta 1 :enactment-fold nil
                                          :novelty-inputs {}})]
    (is (= :selected (:status result)))
    (is (= 1 (count (:ranked result))))
    (is (= "T-ready" (get-in result [:decision :action :target])))
    (is (= 1 (:failure-count result)))
    (is (= {:kind :no-current-target-reading :target-id "T-missing"}
           (first (:failures result))))))

(deftest all-failed-targets-abstain-with-every-failure
  (let [result (sut/select-over-families
                [(failed-family "T-a" :no-current-target-reading)
                 (failed-family "T-b" :stale-target-reading)]
                {:beta 1 :enactment-fold nil :novelty-inputs {}})]
    (is (= :abstained (:status result)))
    (is (= :no-computed-policy-family (get-in result [:decision :kind])))
    (is (nil? (get-in result [:decision :action])))
    (is (empty? (:ranked result)))
    (is (= 2 (:failure-count result)))
    (is (= #{["T-a" :no-current-target-reading]
             ["T-b" :stale-target-reading]}
           (set (map (juxt :target-id :kind) (:failures result)))))))

(deftest cyclic-policy-is-counted-and-never-ranked
  (let [cyclic-policy (assoc (fixture-policy "T-cycle" 0)
                             :cascade {:nodes [{:pattern "p/a"} {:pattern "p/b"}]
                                       :edges [{:from "p/a" :to "p/b" :kind :precedes}
                                               {:from "p/b" :to "p/a" :kind :precedes}]})
        cyclic-family {:status :computed :target-id "T-cycle"
                       :policies [cyclic-policy] :distinct-count 1
                       :failures [] :failure-count 0}
        result (sut/select-over-families [cyclic-family (computed-family "T-ready" 1)]
                                         {:beta 1 :enactment-fold nil
                                          :novelty-inputs {}})]
    (is (= :selected (:status result)))
    (is (= 1 (count (:ranked result))))
    (is (= "T-ready" (get-in result [:decision :action :target])))
    (is (= 1 (:failure-count result)))
    (is (= :cyclic-arrangement (get-in result [:failures 0 :kind])))
    (is (= "T-cycle" (get-in result [:failures 0 :target-id])))
    (is (= (first (get-in result [:failures 0 :cycle]))
           (last (get-in result [:failures 0 :cycle]))))))
