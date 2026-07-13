(ns futon2.aif.fold-classical
  "Impl #1 of the fold interface (`futon2.aif.fold`) — the CLASSICAL fold
   (E-close-the-loop §2). Realizes
       fold : (cascade, circumstance) → {:wiring :coverage-score-delta :policy-holes}
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

   ΔG evaluation is the SHARED coverage→rollout axis (`futon2.aif.fold-eval`) —
   the same path impl #2 (LLM-turn) uses, so the comparison isolates the *build*.
   Alternative ΔG evaluations (all target the same `:coverage-score-delta` port, to compare
   later) live in `fold-eval`. See E-close-the-loop §6b."
  (:require [meme.fold :as mf]
            [futon2.aif.fold-eval :as fe]))

(def default-want-signature "MissionState -> {Wiring, PolicyHoles}")

(defn classical-fold
  "Impl #1 satisfying `futon2.aif.fold`: (cascade, circumstance) →
   {:wiring :coverage-score-delta :policy-holes}. `circumstance` may carry `:want-signature`."
  [cascade circumstance]
  (let [want-sig (or (:want-signature circumstance) default-want-signature)
        wiring   (mf/fold (vec cascade) want-sig)]
    {:wiring wiring
     :coverage-score-delta (fe/coverage-score-delta wiring)
     :policy-holes (:policy-holes wiring)}))
