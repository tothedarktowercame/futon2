(ns futon2.aif.selection-order-incident-test
  "Executable negative cases for INCIDENT-2026-09-30-one-policy-selection-
   repeated-refusal.  These tests state Joe's select-then-interpret order.

   Both tests are tagged :incident, which the normal green-suite selection
   excludes.  Run this open incident contract explicitly with:
     clojure -M:test -m cognitect.test-runner -n futon2.aif.selection-order-incident-test -i :incident"
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.construction-inputs :as inputs]
            [futon2.aif.ticket-queue :as ticket-queue]))

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
  []
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
                        :candidates library-slice
                        :failures []
                        :slice-size (count library-slice)
                        :library-size 100}])
                  targets))
    :horizon-steps 2
    :beta-by-context {:WM {:beta 1}}
    :context-of (constantly :WM)}})

(defn run-fixture []
  (let [assembled (inputs/assemble-cascade-problems (fixture-input))]
    {:assembled assembled
     :result (decision/cascade-decision
              assembled
              {:ticket-queue ticket-queue/empty-declaration})}))

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
