(ns futon2.aif.fold-classical
  "Impl #1 of the fold interface (`futon2.aif.fold`) — the CLASSICAL fold
   (E-close-the-loop §2). Realizes
       fold : (cascade, circumstance) → {:wiring :delta-g :policy-holes}
   via the deterministic rule-table fold (`meme.fold`) for the construction +
   the rollout G(π) accumulator (`futon2.aif.rollout`) for ΔG.

   ΔG: the fold's COVERAGE (folded boxes vs surfaced policy-holes) is the local
   g of a single 'discharge' move; a cascade that folds completely (few holes)
   discharges the sorry better ⇒ a more-negative ΔG ⇒ the act-gate gets a
   descending closing leg. A cascade that folds NOTHING (no boxes) ⇒ ΔG nil ⇒
   the gate `:abstain-missing-leg`s (honest: there is no construction).

   DATA is solution-side (the interface names none): this impl draws on the
   rule-table `meme.fold/RULES`. The LLM-turn and embedding folds draw on OTHER
   data behind the SAME interface — that's what makes them comparable.

   Alternative ΔG evaluations (Joe, 2026-06-26 — all target the same `:delta-g`
   port, to compare later): (a) coverage-as-local-g via rollout (THIS); (b) a
   multi-move rollout over the box sequence as a policy; (c) a coherence /
   wholeness score of the wiring (Salingaros); (d) the belly's
   `predictive-goal-outcome-risk`. See E-close-the-loop."
  (:require [meme.fold :as mf]
            [futon2.aif.rollout :as rollout]))

(def default-want-signature "MissionState -> {Wiring, PolicyHoles}")

(defn coverage
  "Fraction of the cascade the fold turned into construction boxes (vs surfaced
   policy-holes). nil when nothing folded (no boxes) — the gate then abstains."
  [wiring]
  (let [folded (count (:boxes wiring))
        holes  (count (:policy-holes wiring))
        total  (+ folded holes)]
    (when (pos? folded) (/ (double folded) (double total)))))

(defn- rollout-delta-g
  "ΔG = the rollout G(π) of a single discharge move whose local g is −coverage
   (negative = descends EFE). Reuses the canonical `project-policy` accumulator;
   pure (the in-memory meme-step does no I/O)."
  [cov]
  (when cov
    (:G (rollout/project-policy {:reachable #{} :cap-overlay {}}
                                [{:move/id :fold/discharge :delta-g (- (double cov))}]))))

(defn classical-fold
  "Impl #1 satisfying `futon2.aif.fold`: (cascade, circumstance) →
   {:wiring :delta-g :policy-holes}. `circumstance` may carry `:want-signature`."
  [cascade circumstance]
  (let [want-sig (or (:want-signature circumstance) default-want-signature)
        wiring   (mf/fold (vec cascade) want-sig)]
    {:wiring wiring
     :delta-g (rollout-delta-g (coverage wiring))
     :policy-holes (:policy-holes wiring)}))
