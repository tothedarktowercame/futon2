(ns futon2.aif.fold-eval
  "The fold's EVALUATION axis — how a construction's `:delta-g` is produced
   (E-close-the-loop §6b). Independent of the BUILD axis (classical / LLM-turn /
   embedding): every build targets the same `:delta-g` port, so a build can be
   paired with any evaluation here and scored on the same missions. Sharing this
   ns is what makes the impl #1 vs #2 comparison isolate the *build* — the ΔG path
   is identical.

   Evaluation #1 — COVERAGE→rollout (the impl #1 default, now shared): the
   construction's coverage (folded boxes vs surfaced policy-holes) is the local g
   of a single 'discharge' move; more coverage ⇒ more-negative ΔG ⇒ a descending
   closing leg. No coverage (no boxes) ⇒ nil ⇒ the gate abstains (honest: no
   construction to roll out). Pure — the in-memory meme-step does no I/O.

   Other evaluations targeting the same port, to compare later (E-close-the-loop):
   (b) a multi-move rollout over the box SEQUENCE as a policy; (c) a coherence /
   wholeness score of the wiring (Salingaros); (d) the belly's
   `predictive-goal-outcome-risk`."
  (:require [futon2.aif.rollout :as rollout]))

(defn coverage
  "Fraction of the construction that became boxes (vs surfaced policy-holes).
   nil when nothing folded (no boxes) — the gate then abstains."
  [wiring]
  (let [folded (count (:boxes wiring))
        holes  (count (:policy-holes wiring))
        total  (+ folded holes)]
    (when (pos? folded) (/ (double folded) (double total)))))

(defn coverage->delta-g
  "ΔG = the rollout G(π) of a single discharge move whose local g is −coverage
   (negative = descends EFE). Reuses the canonical `project-policy` accumulator.
   nil coverage ⇒ nil ΔG."
  [cov]
  (when cov
    (:G (rollout/project-policy {:reachable #{} :cap-overlay {}}
                                [{:move/id :fold/discharge :delta-g (- (double cov))}]))))

(defn coverage-delta-g
  "Convenience: a wiring → its coverage→rollout ΔG (nil if nothing folded)."
  [wiring]
  (coverage->delta-g (coverage wiring)))
