(ns futon2.aif.fold
  "The FOLD interface — the R16 loop-closure contract (E-close-the-loop).

   R16 is the arc from R10 (act) back to R2 (observe). The act-gate is
   `:pass` iff `ΔF>0 ∧ ΔG<0`, but ΔG is nil until a mission's gap is turned into
   a *construction* to roll out. A **fold** does that:

       fold : (cascade, circumstance) → {:wiring :delta-g :policy-holes}

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
     `:delta-g`      — number | nil. The act-gate's ΔG leg: an EFE-evaluation of
                       the construction (impl #1: the rollout G(π) over it).
                       NEGATIVE ⇒ the construction descends free energy
                       (discharges the sorry). nil ⇒ no reachable path ⇒ the gate
                       must `:abstain-missing-leg`. (Other evaluations — coherence
                       / wholeness — are alternative impls of this same port,
                       documented in E-close-the-loop, to be compared.)
     `:policy-holes` — sequential. What the fold left FREE or could not derive —
                       surfaced, never silently dropped (the fold's coverage
                       discipline, per E-llm-fold).

   `cascade` = the pattern halo condensed around the mission's (have→want) meme;
   `circumstance` = the mission/sorry context the construction must fit. HOW each
   is obtained (query / embedding / store) is solution-side.")

(defn valid-fold-output?
  "True iff M satisfies the fold contract: a map carrying `:wiring`, a `:delta-g`
   that is a number or nil, and a sequential `:policy-holes`."
  [m]
  (and (map? m)
       (contains? m :wiring)
       (let [g (:delta-g m)] (or (nil? g) (number? g)))
       (sequential? (:policy-holes m))))

(defn act-gate-leg
  "The act-gate's ΔG leg from a fold output: a number, or nil ⇒ the gate
   `:abstain-missing-leg`s (it cannot evaluate ΔF∧ΔG)."
  [fold-output]
  (:delta-g fold-output))

(defn closes?
  "Does this fold output give the gate a usable *closing* leg — ΔG present and
   descending (negative)? Convenience over the contract; the gate's `:pass` also
   requires ΔF>0, so this is necessary-not-sufficient for a pass."
  [fold-output]
  (let [g (act-gate-leg fold-output)]
    (boolean (and (number? g) (neg? g)))))
