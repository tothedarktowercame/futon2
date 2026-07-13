(ns futon2.aif.fold-eval
  "The fold's EVALUATION axis — how a construction's `:coverage-score-delta` is produced
   (E-close-the-loop §6b). Independent of the BUILD axis (classical / LLM-turn /
   embedding): every build targets the same `:coverage-score-delta` port, so a build can be
   paired with any evaluation here and scored on the same missions. Sharing this
   ns is what makes the impl #1 vs #2 comparison isolate the *build* — the ΔG path
   is identical.

   Evaluation #1 — COVERAGE SCORE: the construction's coverage (folded boxes
   vs surfaced policy-holes) becomes a negative engineering score delta. It is
   deliberately not passed through the rollout/EFE vocabulary: no predicted
   outcome distribution is present. No coverage ⇒ nil ⇒ the gate abstains.

   Other evaluations targeting the same port, to compare later (E-close-the-loop):
   (b) a multi-move rollout over the box SEQUENCE as a policy; (c) a coherence /
   wholeness score of the wiring (Salingaros); (d) the belly's
   `predictive-goal-outcome-risk`."
  )

(defn coverage
  "Fraction of the construction that became boxes (vs surfaced policy-holes).
   nil when nothing folded (no boxes) — the gate then abstains."
  [wiring]
  (let [folded (count (:boxes wiring))
        holes  (count (:policy-holes wiring))
        total  (+ folded holes)]
    (when (pos? folded) (/ (double folded) (double total)))))

(defn coverage->score-delta
  "Engineering coverage-score delta = -coverage. More complete constructions
   have a more-negative delta. nil coverage remains nil."
  [cov]
  (when cov
    (- (double cov))))

(defn coverage-score-delta
  "Convenience: wiring → coverage-score delta (nil if nothing folded)."
  [wiring]
  (coverage->score-delta (coverage wiring)))
