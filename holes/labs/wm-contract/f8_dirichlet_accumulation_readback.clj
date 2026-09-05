;; F8 leg 1 slice 10: production Dirichlet recount readback.
;; Every expected value is transcribed from the named Lean theorem printed on
;; its line; no expected value is recomputed from the production result.
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.a4a :as a4a]
         '[futon2.aif.bmr :as bmr])

(def one {:capabilities ["cap-a"] :edges [["cap-a" "m1"]] :discharges []})
(def two {:capabilities ["cap-a"] :edges [["cap-a" "m1"] ["cap-a" "m1"]] :discharges []})
(def grid {:capabilities ["cap-a" "cap-b"]
           :edges [["cap-a" "m1"] ["cap-b" "m2"]] :discharges []})
(def before (a4a/corpus->concentration
             {:capabilities ["cap-a"] :edges [["cap-a" "m1"] ["cap-a" "m2"]]}))
(def after (a4a/corpus->concentration
            {:capabilities ["cap-a"]
             :edges [["cap-a" "m1"] ["cap-a" "m2"] ["cap-a" "aaa"]]}))
(def c1 (a4a/corpus->concentration one))
(def c2 (a4a/corpus->concentration two))
(def cg (a4a/corpus->concentration grid))
(defn match-line [theorem label expected actual]
  (str theorem " " label " expected=" (pr-str expected) " actual=" (pr-str actual)
       " " (if (= expected actual) "MATCH" "MISMATCH")))
(def checks
  [["priorReference" "prior" 0.1 a4a/prior]
   ["oneEdgeReference" "one edge cell" 1.1 (get-in c1 [:concentrations "cap-a" 0])]
   ["twoEdgesReference" "two edge cell" 2.1 (get-in c2 [:concentrations "cap-a" 0])]
   ["twoMissionRowReference" "cap-a row" [1.1 0.1] (get-in cg [:concentrations "cap-a"])]
   ["recountReference" "same corpus twice" true
    (= (a4a/corpus->concentration one) (a4a/corpus->concentration one))]
   ["machineCoordinatesAreNotStable" "outcomes before" ["m1" "m2"] (:outcomes before)]
   ["machineCoordinatesAreNotStable" "outcomes after" ["aaa" "m1" "m2"] (:outcomes after)]
   ["machineCoordinatesAreNotStable" "m1 index before/after" [0 1]
    [(.indexOf ^java.util.List (:outcomes before) "m1")
     (.indexOf ^java.util.List (:outcomes after) "m1")]]
   ["bmrThresholdReference" "BMR threshold" -3.0 bmr/acceptance-threshold]])
(def lines
  (concat ["F8 leg 1 slice 10 -- production Dirichlet recount readback"]
          (map (fn [[t l e a]] (match-line t l e a)) checks)
          ["recurrenceMissingBothArms declared recurrence: NOT MEASURED in Clojure; production has no previous-a parameter"
           "oneHotOuterProductIsUnitCell/nonOneHotOuterProductIsNotUnitCell: NOT MEASURED in Clojure; declared tick-model rule is absent"
           "machineSatisfiesNoneOfRepairPredicate: NOT MEASURED in Clojure; predicate specifies the repair boundary"
           (if (every? (fn [[_ _ e a]] (= e a)) checks)
             "VERDICT: 9 MATCH, 0 MISMATCH; explicit NOT MEASURED lines retained."
             "VERDICT: MISMATCH.")]))
(let [out (io/file "holes/labs/wm-contract/runs/F8-dirichlet-accumulation/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
