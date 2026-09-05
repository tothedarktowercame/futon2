;; :F8 leg 1 slice 10 -- second independent probe by the reviewing seat: can an
;; already-accumulated `a` re-enter the accumulation? (the recurrence question).
;; Run before codex-9 delivered; nothing here reads its Lean or its readback.
(require '[futon2.aif.a4a :as a4a])
(defn p [l v] (println l "=" (pr-str v)))
;; Can an already-accumulated `a` be handed back in?
(def corpus {:capabilities ["cap-a" "cap-b"]
             :edges [["cap-a" "m1"] ["cap-a" "m1"] ["cap-b" "m1"] ["cap-b" "m2"]]
             :discharges []})
(def a1 (a4a/corpus->concentration corpus))
(p "a1" (:concentrations a1))
;; reduce-concepts accepts a concentration map (a4a.clj:115-119, concentration-input)
(def r-from-corpus (a4a/reduce-concepts corpus))
(def r-from-conc   (a4a/reduce-concepts a1))
(p "reduce from corpus = reduce from concentration ?" (= r-from-corpus r-from-conc))
(p "merge-scores delta-F" (mapv :delta-F (:merge-scores r-from-corpus)))
(p "concepts" (:concepts r-from-corpus))
(p "concept-concentrations" (:concept-concentrations r-from-corpus))
;; is there ANY public fn taking (previous-a, corpus)?
(p "a4a public arglists"
   (into (sorted-map)
         (for [[s v] (ns-publics 'futon2.aif.a4a)] [s (:arglists (meta v))])))
