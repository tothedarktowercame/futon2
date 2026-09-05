;; :F8 leg 1 slice 10 -- reviewing seat's INDEPENDENT probe of the R17 class-(b)
;; divergence (declared o/mu accumulation vs the A4a substrate accumulation),
;; run BEFORE any Lean was dispatched. It calls production `futon2.aif.a4a`,
;; `futon2.aif.bmr` and `futon2.aif.r17-offline` directly.
(require '[futon2.aif.a4a :as a4a]
         '[futon2.aif.bmr :as bmr]
         '[futon2.aif.r17-offline :as r17])

(defn p [label v] (println label "=" (pr-str v)))

;; ---------------------------------------------------------------- 1. the rule
;; One capability, one mission, one edge: what does the cell hold?
(def one-edge {:capabilities ["cap-a"] :edges [["cap-a" "m1"]] :discharges []})
(def c1 (a4a/corpus->concentration one-edge))
(p "prior constant (a4a/prior)" a4a/prior)
(p "one edge -> concentrations" (:concentrations c1))
(p "one edge -> outcomes" (:outcomes c1))

;; Two records into the SAME cell: increment is exactly 1.0 each, no fractions.
(def two-edges {:capabilities ["cap-a"] :edges [["cap-a" "m1"] ["cap-a" "m1"]] :discharges []})
(def c2 (a4a/corpus->concentration two-edges))
(p "two identical edges -> cell" (get-in (:concentrations c2) ["cap-a" 0]))
(p "increment per record" (- (get-in (:concentrations c2) ["cap-a" 0])
                             (get-in (:concentrations c1) ["cap-a" 0])))

;; The row sums: one record puts ALL of its unit mass in ONE cell.
(def two-out {:capabilities ["cap-a"] :edges [["cap-a" "m1"]] :discharges []
              :extra nil})
(def c3 (a4a/corpus->concentration {:capabilities ["cap-a" "cap-b"]
                                    :edges [["cap-a" "m1"] ["cap-b" "m2"]]
                                    :discharges []}))
(p "two caps two missions -> concentrations" (:concentrations c3))
(p "row cap-a minus prior row" (mapv #(- % a4a/prior) (get (:concentrations c3) "cap-a")))

;; ------------------------------------------------- 2. is there an `a` to add to?
;; Declared: a := a + sum_tau o (x) s -- a RECURRENCE with the previous a on the
;; right. Is corpus->concentration a function of any previous concentration?
(p "corpus->concentration arglists"
   (:arglists (meta (resolve 'futon2.aif.a4a/corpus->concentration))))
(p "same corpus twice -> identical (no carry)"
   (= (a4a/corpus->concentration one-edge) (a4a/corpus->concentration one-edge)))
(p "reduce-concepts arglists"
   (:arglists (meta (resolve 'futon2.aif.a4a/reduce-concepts))))

;; -------------------------------------------- 3. column indices across corpora
;; outcomes are SORTED ids indexed by position. Add a mission that sorts first
;; and every existing column index moves.
(def before (a4a/corpus->concentration
             {:capabilities ["cap-a"] :edges [["cap-a" "m1"] ["cap-a" "m2"]] :discharges []}))
(def after (a4a/corpus->concentration
            {:capabilities ["cap-a"] :edges [["cap-a" "m1"] ["cap-a" "m2"] ["cap-a" "aaa"]]
             :discharges []}))
(p "before outcomes" (:outcomes before))
(p "after  outcomes" (:outcomes after))
(p "before row cap-a" (get (:concentrations before) "cap-a"))
(p "after  row cap-a" (get (:concentrations after) "cap-a"))
(p "index of m1 before / after"
   [(.indexOf ^java.util.List (:outcomes before) "m1")
    (.indexOf ^java.util.List (:outcomes after) "m1")])

;; ------------------------------------------------------------ 4. the threshold
(p "bmr/acceptance-threshold" bmr/acceptance-threshold)

;; ------------------------------------------------- 5. what r17-offline consumes
(p "r17-offline/run arglists" (:arglists (meta (resolve 'futon2.aif.r17-offline/run))))
(p "envelope-version" r17/envelope-version)

;; --------------------------------------------- 6. does o or mu reach any of it?
(println "grep counts printed by the shell, not here")
