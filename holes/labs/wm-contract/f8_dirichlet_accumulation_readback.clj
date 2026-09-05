;; F8 leg 1 slice 10: production R17 accumulation readback.
;;
;; PROVENANCE RULE: every expected value on a THEOREM line is transcribed from
;; the named Lean theorem, not derived a second time from the production inputs.
;; Lines whose fact is not statable in Lean are labelled PRODUCTION-ONLY and
;; carry no transcribed expectation; lines Lean states and production has no
;; counterpart for are labelled NOT MEASURED. The delivered readback took two of
;; its expectations from tautologies (`f x = f x` and `(-3 : R) = -3`); those
;; theorems were replaced in review and the lines below follow the replacements.
;;
;; Lean theorem (MachineDirichletAccumulation / ...Witness)  expected here
;; priorReference                          a4a/prior = 0.1
;; oneEdgeReference                        one record -> 1.1
;; twoEdgesReference                       the same record twice -> 2.1
;; twoMissionRowReference                  the untouched cell stays at 0.1
;; machineAppendRaisesItsOwnCell           appending a record adds exactly 1.0
;; machineAppendMovesNoOtherCell           ... and moves no other cell
;; noSingleRecordProducesASoftUpdate       exactly ONE cell moves, never two
;; machineCoordinatesAreNotStable          m1 at column 0, then column 1
;; bmrThresholdReference                   the threshold is -3.0
(require '[futon2.aif.a4a :as a4a]
         '[futon2.aif.bmr :as bmr])

(def out (atom []))
(defn line [theorem label expected actual]
  (swap! out conj
         (format "%-36s %-34s expected %-24s actual %-24s %s"
                 theorem label (pr-str expected) (pr-str actual)
                 (if (= expected actual) "MATCH" "MISMATCH"))))
(defn note [theorem label text] (swap! out conj (format "%-36s %-34s %s" theorem label text)))

;; --- the prior and the increment -------------------------------------------
(def one  {:capabilities ["cap-a"] :edges [["cap-a" "m1"]] :discharges []})
(def two  {:capabilities ["cap-a"] :edges [["cap-a" "m1"] ["cap-a" "m1"]] :discharges []})
(def grid {:capabilities ["cap-a" "cap-b"]
           :edges [["cap-a" "m1"] ["cap-b" "m2"]] :discharges []})
(def c1 (a4a/corpus->concentration one))
(def c2 (a4a/corpus->concentration two))
(def cg (a4a/corpus->concentration grid))

(line "priorReference" "a4a/prior" 0.1 a4a/prior)
(line "oneEdgeReference" "one record -> its cell" 1.1
      (get-in c1 [:concentrations "cap-a" 0]))
(line "twoEdgesReference" "the same record twice" 2.1
      (get-in c2 [:concentrations "cap-a" 0]))
(line "twoMissionRowReference" "the record's own cell" 1.1
      (get-in cg [:concentrations "cap-a" 0]))
(line "twoMissionRowReference" "the untouched cell" 0.1
      (get-in cg [:concentrations "cap-a" 1]))

;; --- one record, one cell: BOTH arms ---------------------------------------
;; The Lean theorems fix the coordinates, so the production fixture must too:
;; both the capability and the mission of the added record already occur, which
;; is what keeps the row width constant. The widening case is the next section.
(def base {:capabilities ["cap-a" "cap-b"]
           :edges [["cap-a" "m1"] ["cap-b" "m2"]] :discharges []})
(def plus {:capabilities ["cap-a" "cap-b"]
           :edges [["cap-a" "m1"] ["cap-b" "m2"] ["cap-a" "m2"]] :discharges []})
(def cb (a4a/corpus->concentration base))
(def cp (a4a/corpus->concentration plus))
(defn cells [c]
  (into {} (for [[cap row] (:concentrations c), [i v] (map-indexed vector row)]
             [[cap i] v])))
(def moved (into {} (filter (fn [[k v]] (not= v (get (cells cb) k))) (cells cp))))
(line "machineAppendRaisesItsOwnCell" "delta at the record's cell" 1.0
      (- (get (cells cp) ["cap-a" 1]) (get (cells cb) ["cap-a" 1])))
(line "machineAppendMovesNoOtherCell" "cells that moved" [["cap-a" 1]]
      (vec (keys moved)))
(line "noSingleRecordProducesASoftUpdate" "how many cells moved" 1 (count moved))

;; --- the coordinates are not stable ----------------------------------------
(def before (a4a/corpus->concentration
             {:capabilities ["cap-a"] :edges [["cap-a" "m1"] ["cap-a" "m2"]] :discharges []}))
(def after (a4a/corpus->concentration
            {:capabilities ["cap-a"]
             :edges [["cap-a" "m1"] ["cap-a" "m2"] ["cap-a" "aaa"]] :discharges []}))
(line "machineCoordinatesAreNotStable" "outcomes before" ["m1" "m2"] (:outcomes before))
(line "machineCoordinatesAreNotStable" "outcomes after" ["aaa" "m1" "m2"] (:outcomes after))
(line "machineCoordinatesAreNotStable" "m1's column before/after" [0 1]
      [(.indexOf ^java.util.List (:outcomes before) "m1")
       (.indexOf ^java.util.List (:outcomes after) "m1")])

;; --- the threshold, which belongs to the OTHER R17 row ----------------------
(line "bmrThresholdReference" "bmr/acceptance-threshold" -3.0 bmr/acceptance-threshold)

;; --- production-only: no expectation is transcribed, because Lean cannot
;;     state either of these (a Lean function is deterministic by definition,
;;     and the absence of an entry point is a fact about a namespace) ---------
(note "PRODUCTION-ONLY" "same corpus twice -> identical"
      (str (= (a4a/corpus->concentration one) (a4a/corpus->concentration one))
           "  (no previous-a is carried across calls)"))
(note "PRODUCTION-ONLY" "the two accumulation entry points"
      (str "corpus->concentration "
           (:arglists (meta (resolve 'futon2.aif.a4a/corpus->concentration)))
           " / reduce-concepts "
           (:arglists (meta (resolve 'futon2.aif.a4a/reduce-concepts)))
           "  -- neither takes a previous concentration together with new records"))
(note "PRODUCTION-ONLY" "arity-2 publics in futon2.aif.a4a"
      (str (->> (ns-publics 'futon2.aif.a4a)
                (filter (fn [[_ v]] (some #(= 2 (count %)) (:arglists (meta v)))))
                (map key) sort vec)
           "  -- listed for completeness; both are model-uncertainty helpers, neither is an accumulation"))

;; --- stated in Lean, no production counterpart exists ----------------------
(note "oneHotDeclaredUpdateIsTheUnitIncrement" "declared one-hot update"
      "NOT MEASURED -- production has no tick-model accumulation to run")
(note "softDeclaredUpdateRaisesTwoCells" "declared soft update"
      "NOT MEASURED -- production has no tick-model accumulation to run")
(note "declaredCoordinatesAreFixedAndMachineCoordinatesAreNot" "Channel/Status arm"
      "NOT MEASURED -- the declared coordinates have no runtime inhabitant here")
(note "recountShapedDoesNotRealise" "the repair predicate"
      "NOT MEASURED -- it constrains a repaired implementation that does not exist")

(println "F8 leg 1 slice 10 -- production R17 accumulation readback")
(doseq [l @out] (println l))
(let [ms (count (filter #(re-find #"MISMATCH" %) @out))
      m  (count (filter #(re-find #"MATCH" %) @out))]
  (println (format "VERDICT: %d MATCH, %d MISMATCH" (- m ms) ms))
  (System/exit (if (zero? ms) 0 1)))
