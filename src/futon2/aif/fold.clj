(ns futon2.aif.fold
  "The FOLD interface — the R16 loop-closure contract (E-close-the-loop).

   R16 is the arc from R10 (act) back to R2 (observe). The engineering gate is
   `:pass` iff `cascade-score>0` and `coverage-score-delta<0`. A fold supplies
   the checkable construction and the latter score:

       fold : (cascade, circumstance) → {:wiring :coverage-score-delta :policy-holes}

   This ns is ONLY the contract those folds work to — so a classical, an
   LLM-turn, or an embedding fold all plug into the same socket and are
   comparable (E-close-the-loop §2).

   LOAD-BEARING (Joe, 2026-06-26): the contract is **data-agnostic**. It names
   ports and types only — NO store / corpus / weights / arrow source. If
   code-is-data, the data a fold draws on is part of its *construction*, not the
   interface. (A leak of any data source into this ns is the failure mode.)

   The ports:
     `:wiring`       — the construction (boxes/wires/terminals; what
                       `apply-cascade!` runs). Present on any successful fold.
     `:coverage-score-delta` — number | nil. Negative means the constructed
                       fold covers at least one surfaced obligation; nil means
                       no construction was available and the gate abstains.
                       It carries no EFE semantics.
     `:policy-holes` — sequential. What the fold left FREE or could not derive —
                       surfaced, never silently dropped (the fold's coverage
                       discipline, per E-llm-fold).

   `cascade` = the pattern halo condensed around the mission's (have→want) meme;
   `circumstance` = the mission/sorry context the construction must fit. HOW each
   is obtained (query / embedding / store) is solution-side.")

(defn valid-fold-output?
  "True iff M satisfies the fold contract: a map carrying `:wiring`, a `:coverage-score-delta`
   that is a number or nil, and a sequential `:policy-holes`."
  [m]
  (and (map? m)
       (contains? m :wiring)
       (let [g (:coverage-score-delta m)] (or (nil? g) (number? g)))
       (sequential? (:policy-holes m))))

(defn coverage-score-leg
  "The gate's coverage-score leg, or nil when no construction was evaluated."
  [fold-output]
  (:coverage-score-delta fold-output))

(defn closes?
  "Does this fold output give the gate a negative coverage-score delta?
   The gate also requires a positive cascade score."
  [fold-output]
  (let [g (coverage-score-leg fold-output)]
    (boolean (and (number? g) (neg? g)))))
