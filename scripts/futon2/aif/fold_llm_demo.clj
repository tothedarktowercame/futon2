(ns futon2.aif.fold-llm-demo
  "Demonstration of impl #2 (the LLM-turn fold) on REAL served-mission cascades —
   E-close-the-loop §6b 'see if it makes sense in context' (Joe, 2026-06-26).
   Sim-only, separate process, NO serving-JVM touch, no writes, no loop.

   Two cases for M-value-creation-loop, both folded by the inhabiting agent
   (claude-10, the engine per E-llm-fold), INJECTED as `turn-fn` so they run
   through the exact `futon2.aif.fold` interface impl #1 does:
     THIN  — the cascade minilm picks at budget 12: [f6/self-play-loop]. ΔF<0
             (below the Bayesian-Occam knee) ⇒ the loop's CONSTRUCTION leg closes
             (real ΔG<0) but the gate :fails on ΔF — honest.
     RICH  — the cascade at a fuller psi/budget 20 (ΔF=0.707>0): 4 patterns ⇒
             both legs close ⇒ the gate :passes. The loop fully closes."
  (:require [futon2.aif.fold-llm :as llm]
            [futon2.aif.fold-classical :as fc]
            [futon2.aif.fold :as fold]
            [clojure.string :as str]))

(defn- prose-fn [pattern-id]
  (try (slurp (str "/home/joe/code/futon3/library/" pattern-id ".flexiarg"))
       (catch Exception _ nil)))

;; ---- THIN case ----------------------------------------------------------
(def thin-cascade ["f6/self-play-loop"])
(def thin-circumstance {:mission "M-value-creation-loop" :psi "value creation loop self play"})
(def thin-cascade-score -0.214)   ; real, from cascade-lane (budget 12)
(def thin-turn
  (pr-str
   {:boxes
    [{:id :value-gap-asker :role "propose a target whose closure CREATES value" :fits-pattern "f6/self-play-loop"
      :addresses-however "trivial questions — Asker scored on value-potential, not volume"}
     {:id :context-answerer :role "construct a candidate value move from substrate-2 context" :fits-pattern "f6/self-play-loop"
      :addresses-however "Answerer ignoring context — the move must cite its substrate-2 neighbourhood"}
     {:id :critic-value-gate :role "score realized value; commit only above threshold" :fits-pattern "f6/self-play-loop"
      :addresses-however "unreliable Critic — gated by the calibration tap"}
     {:id :value-ledger-update :role "commit gated moves; track value/iteration; pause on plateau" :fits-pattern "f6/self-play-loop"
      :addresses-however "noise vs signal — commons must measurably improve or pause"}
     {:id :calibration-tap :role "periodic human (Joe) spot-check calibrating the Critic" :fits-pattern "f6/self-play-loop"
      :addresses-however "Critic drift — human calibration first-class"}]
    :wires [[:value-gap-asker :context-answerer] [:context-answerer :critic-value-gate]
            [:critic-value-gate :value-ledger-update] [:calibration-tap :critic-value-gate]]
    :terminals [:value-ledger-update :calibration-tap]
    :policy-holes
    [{:unfolded-pattern nil :free "the value metric" :why "pattern measures graph density / eval scores; 'value' is undefined for the mission"}
     {:unfolded-pattern nil :free "Critic threshold + calibration cadence" :why "THEN says 'a threshold' / 'periodically' but gives no values"}
     {:unfolded-pattern nil :free "the M-f6-eval scoring protocol" :why "THEN delegates scoring to M-f6-eval — referenced, not in the cascade"}]}))

;; ---- RICH case (4-pattern cascade, ΔF>0) --------------------------------
(def rich-cascade ["f6/self-play-loop" "f6/pattern-as-strategy" "f6/q-turnstile-a" "agent/trail-enables-return"])
(def rich-circumstance {:mission "M-value-creation-loop"
                        :psi "self-play loop asker answerer critic knowledge graph evaluate iterate"})
(def rich-cascade-score 0.707)    ; real, from cascade-lane (fuller psi, budget 20)
(def rich-turn
  (pr-str
   {:boxes
    [{:id :value-gap-asker :role "propose value-creating targets, scored on value-potential" :fits-pattern "f6/self-play-loop"
      :addresses-however "trivial questions — value-potential, not volume"}
     {:id :q-typed-asker :role "annotate each target with the construction-TYPE that resolves it (Q⊢A)" :fits-pattern "f6/q-turnstile-a"
      :addresses-however "retrieval-not-construction — the question specifies what construction counts as adequate"}
     {:id :strategy-answerer :role "construct the move by NAMING + applying a library pattern as strategy (cites it), from substrate-2 context" :fits-pattern "f6/pattern-as-strategy"
      :addresses-however "human-heuristic≠agent-action + ignore-context — agent-operational interpretation, must cite neighbourhood"}
     {:id :critic-adequacy-gate :role "score construction-ADEQUACY for the posed question (not just correctness); commit above threshold" :fits-pattern "f6/q-turnstile-a"
      :addresses-however "answer-to-wrong-question + unreliable Critic — adequacy + calibration"}
     {:id :trail-recorder :role "record each iteration (time/state/decision/action/evidence/reversibility) to a queryable trail" :fits-pattern "agent/trail-enables-return"
      :addresses-however "can't distinguish unexplored from explored-rejected — backtrack + learn from dead ends"}
     {:id :value-ledger-update :role "commit gated moves; track value/iteration; pause on plateau" :fits-pattern "f6/self-play-loop"
      :addresses-however "noise vs signal — commons must measurably improve"}
     {:id :calibration-tap :role "periodic human spot-check calibrating the Critic" :fits-pattern "f6/self-play-loop"
      :addresses-however "Critic drift — human calibration first-class"}]
    :wires [[:value-gap-asker :q-typed-asker] [:q-typed-asker :strategy-answerer]
            [:strategy-answerer :critic-adequacy-gate] [:critic-adequacy-gate :value-ledger-update]
            [:calibration-tap :critic-adequacy-gate]
            [:strategy-answerer :trail-recorder] [:critic-adequacy-gate :trail-recorder]
            [:value-ledger-update :trail-recorder] [:value-ledger-update :value-gap-asker]]
    :terminals [:value-ledger-update :trail-recorder :calibration-tap]
    :policy-holes
    [{:unfolded-pattern nil :free "the value metric" :why "still undefined for the mission — a free choice"}
     {:unfolded-pattern nil :free "PSR/PUR pattern-use recording substrate" :why "pattern-as-strategy says 'track via PSR/PUR records' but names no store here"}
     {:unfolded-pattern nil :free "question-type taxonomy completeness" :why "q-turnstile-a lists proof/example/counterexample/algorithm; whether 'value-move' is a clean addition is free"}
     {:unfolded-pattern nil :free "the M-f6-eval rubric" :why "the Critic's scoring rubric is still external to the cascade"}]}))

(defn run-case [label cascade circumstance cascade-score turn]
  (let [out  (llm/llm-fold cascade circumstance {:turn-fn (constantly turn) :prose-fn prose-fn})
        clas (fc/classical-fold cascade circumstance)
        leg  (fold/coverage-score-leg out)
        verdict (cond (or (nil? cascade-score) (nil? leg)) :abstain-missing-leg
                      (and (pos? cascade-score) (neg? leg)) :pass
                      :else :fail)]
    (println (str "\n================ " label " — M-value-creation-loop ================"))
    (println "CASCADE:" cascade)
    (println "\n--- construction (the agent fold, fitted to the mission) ---")
    (doseq [b (get-in out [:wiring :boxes])]
      (println (format "  [%-22s] %s" (name (:id b)) (:role b))))
    (println "  wires:" (count (get-in out [:wiring :wires])) "| terminals:" (get-in out [:wiring :terminals]))
    (println "--- policy-holes (surfaced, not fabricated) ---")
    (doseq [h (:policy-holes out)] (println (str "  • " (:free h) " — " (:why h))))
    (println "--- interface check + verdict ---")
    (println "  valid-fold-output?" (fold/valid-fold-output? out) "| closes?" (fold/closes? out))
    (println (format "  coverage %d boxes / %d holes  ⇒  ΔG = %.4f"
                     (count (get-in out [:wiring :boxes])) (count (:policy-holes out)) (double leg)))
    (println (format "  ΔF = %+.3f   ΔG = %+.4f   ⇒  GATE: %s" cascade-score (double leg) verdict))
    (println (format "  [contrast] impl #1 classical on the SAME cascade: ΔG=%s, boxes=%d ⇒ %s"
                     (:coverage-score-delta clas) (count (get-in clas [:wiring :boxes]))
                     (if (nil? (:coverage-score-delta clas)) ":abstain (rule-table can't fold these)" "")))))

(defn -main [& _]
  (run-case "THIN (budget 12, ΔF<0)" thin-cascade thin-circumstance thin-cascade-score thin-turn)
  (run-case "RICH (fuller psi, ΔF>0)" rich-cascade rich-circumstance rich-cascade-score rich-turn)
  (println (str "\n" (str/join "" (repeat 60 "─")))
           "\nThe fold supplies the ΔG leg the gate was abstaining for. THIN ⇒ :fail"
           "\n(ΔF below the knee — a cascade-richness problem, not a fold problem). RICH"
           "\n⇒ :pass — both legs close, the loop closes through impl #2."))
