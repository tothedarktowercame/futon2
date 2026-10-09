(ns futon2.aif.selection-order-incident-test
  "Executable negative cases for INCIDENT-2026-09-30-one-policy-selection-
   repeated-refusal.  These tests state Joe's select-then-interpret order.

   Both tests are tagged :incident, which the normal green-suite selection
   excludes.  Run this open incident contract explicitly with:
     clojure -M:test -m cognitect.test-runner -n futon2.aif.selection-order-incident-test -i :incident"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.construction-inputs :as inputs]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.test-support.runner-fixture :as runner-fixture]))

;; Selection reaches cascade-observation-scoring; keep its default cache and
;; every other mutable data-path under the suite root.
(use-fixtures :once runner-fixture/with-hermetic-traces)

(def targets (mapv #(str "M-selection-order-" %) (range 5)))

(def library-slice
  [{:pattern :library/inspect-evidence :rank 1
    :provenance {:retriever :embedding :query "close the target"}}
   {:pattern :library/repair-smallest-gap :rank 2
    :provenance {:retriever :tier0 :query "close the target"}}
   {:pattern :library/verify-result :rank 3
    :provenance {:retriever :embedding :query "close the target"}}])

(defn fixture-input
  "Five open targets with query-time library candidates and deliberately no
   published target-specific interpretation.  :query-time-slices is the input
   the incident says must enter construction and assembly must retain it."
  ([] (fixture-input library-slice))
  ([slice-candidates]
  {:targets targets
   :sources
   {:universes (into {} (map (fn [t] [t {:work/open true :work/closed false}]) targets))
    :wants (into {} (map (fn [t] [t [:work/closed]]) targets))
    :locators (into {}
                    (map (fn [t]
                           [t {:work/open {:class :C4 :fixture true}
                               :work/closed {:class :C4 :fixture true}}])
                         targets))
    :interpretations (into {} (map (fn [t] [t {:patterns {} :receipts {}}]) targets))
    :query-time-slices
    (into {} (map (fn [t]
                    [t {:schema :wm/query-time-library-slice-v1
                        :target t
                        :query "close the target"
                        :candidates slice-candidates
                        :failures []
                        :slice-size (count library-slice)
                        :library-size 100}])
                  targets))
    :horizon-steps 2
    :beta-by-context {:WM {:beta 1}}
    :context-of (constantly :WM)}}))

(defn run-fixture
  ([] (run-fixture library-slice))
  ([slice-candidates]
  (let [assembled (inputs/assemble-cascade-problems (fixture-input slice-candidates))]
    {:assembled assembled
     :result (decision/cascade-decision
              assembled
              {:ticket-queue ticket-queue/empty-declaration
               :focus-inputs
               (assoc (focus/read-inputs)
                      :relations
                      (mapv (fn [target]
                              {:target target :facet "WM" :relation "focus"
                               :source {:repo "fixture" :commit "0" :path "test"
                                        :section "fixture"}
                               :effective-from "2026-01-01T00:00:00Z"})
                            targets))})})))

(defn compact-scores [result]
  (let [cert (get-in result [:decision :selection-certificate])]
    (mapv (fn [i c]
            (merge {:target (get-in c [:id :target])
                    :pattern (get-in c [:id :precedence 0 :id])
                    :g (:g c)}
                   (get-in cert [:scoring i :g-terms])))
          (range) (:candidates cert))))

(deftest ^:incident policy-set-is-not-one
  (let [{:keys [result]} (run-fixture)
        scored (get-in result [:decision :selection-certificate :candidates])
        construction (get-in result [:decision :selection-certificate :target-construction])]
    (is (= (* 2 (count targets)) (count scored))
        (str "each open target must contribute a scored policy before any "
             "target-specific interpretation exists; decision="
             (pr-str (:decision result))))
    (is (every? #(= (set (:slice %)) (set (:pool %))) construction))
    (is (every? #(= 2 (:policy-count %)) construction))))

(deftest ^:incident uninterpreted-pattern-can-be-selected
  (let [{:keys [assembled result]} (run-fixture)
        chosen (get-in result [:decision :action])
        owed (get-in result [:decision :selection-certificate :interpretations-owed])]
    (testing "the library slice enters the existing constructor and selection"
      (is (some? chosen)
          (str "an uninterpreted applicable pattern is selectable, not an "
               "assembly refusal; refusals=" (pr-str (:refusals assembled)))))
    (testing "selection records the honest post-selection interpretation debt"
      (is (and (seq owed)
               (every? #(and (= :interpretation-owed-after-selection (:kind %))
                              (:target %)
                              (:pattern %)
                              (not (:attested? %)))
                       owed))
          (str "chosen patterns need typed interpretation debt and must not be "
               "presented as attested; certificate="
               (pr-str (get-in result [:decision :selection-certificate])))))))

(deftest ^:incident provisional-likelihood-makes-g-policy-dependent
  (let [scores (compact-scores (:result (run-fixture)))
        first-target (filter #(= (first targets) (:target %)) scores)]
    (is (= 2 (count (set (map :g first-target))))
        (str "provisional policies must not be a G tie: " (pr-str first-target)))
    (is (every? pos? (map :ambiguity scores)))
    (is (every? pos? (map :expected-information-gain scores)))))

(deftest ^:incident policy-choice-is-invariant-to-slice-serialization-order
  (let [forward (:result (run-fixture library-slice))
        reversed (:result (run-fixture (vec (reverse library-slice))))
        chosen (fn [r] [(get-in r [:decision :action :target])
                        (get-in r [:decision :action :precedence 0 :id])])]
    (is (= (chosen forward) (chosen reversed)))
    (is (= (mapv #(select-keys % [:target :pattern :g]) (compact-scores forward))
           (mapv #(select-keys % [:target :pattern :g]) (compact-scores reversed))))))

(deftest ^:incident scoring-budget-never-silences-enumerated-targets
  (let [assembled (assoc (inputs/assemble-cascade-problems (fixture-input))
                         :scoring-target-budget
                         {:schema :wm/scoring-target-budget-v1
                          :target-limit 3 :basis :adversarial-fixture
                          :enumerated-target-count 5})
        result (decision/cascade-decision
                assembled
                {:ticket-queue ticket-queue/empty-declaration
                 :focus-inputs
                 (assoc (focus/read-inputs) :relations
                        (mapv (fn [target]
                                {:target target :facet "WM" :relation "focus"
                                 :source {:repo "fixture" :commit "0" :path "test"
                                          :section "fixture"}
                                 :effective-from "2026-01-01T00:00:00Z"})
                              targets))})
        budget (get-in result [:decision :selection-certificate :scoring-target-budget])]
    (is (= 5 (count (:problems assembled))) "all targets constructed before budgeting")
    (is (= 10 (count (get-in result [:decision :selection-certificate :candidates]))))
    (is (empty? (:budget-exhausted-targets budget)))
    (is (empty? (filter #(and (= :scoring (:stage %))
                              (= :budget-exhausted (:reason %)))
                        (:dropped-candidates result))))))
