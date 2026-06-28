(ns futon2.aif.l2-verify
  "C-cascade-real STANDARD-VERIFY L2 (claude-10, O7): fold a REAL cascade via
   impl #2 (LLM-turn), adapt the wiring to a CLean, and write it out for the
   DarkTower standalone 0-sorry render (the structural-soundness gate).

   GREEN = a well-formed construction over M-value-creation-loop's rich cascade
   (f6/self-play-loop · f6/pattern-as-strategy · f6/q-turnstile-a ·
   agent/trail-enables-return; F=+0.90, sim — no D4 needed). RED = the same
   construction with a deliberate structural break (a dangling consume / a wire
   carrying an unproduced token), to show the gate BITES.

   Writes /tmp/l2-darktower/{green,red}/<id>.clean.edn. The render+lean check is
   the runner `scripts/l2-darktower-verify.sh`. This Clojure side stops at the
   .clean.edn (fold + adapt only)."
  (:require [futon2.aif.fold-llm :as llm]
            [futon2.aif.fold-clean :as fc]
            [clojure.java.io :as io]))

;; The agent turn (impl #2 = the inhabiting agent as the engine, per E-llm-fold),
;; DarkTower-shaped: each box carries :method + the typed-hole directions
;; :consumes/:produces (+ :hole or :discharges). A linear comb over the cascade.
(def green-turn
  {:boxes
   [{:id :s1 :method :construct-auxiliary-object
     :text "Asker proposes a value-creating target as a typed value-question (f6/self-play-loop + q-turnstile-a)."
     :produces :value-target
     :hole {:kind :sorry :discharge :sorryProof :satiety :parse
            :wanted "the value metric is a free policy-hole"}}
    {:id :s2 :method :construct-auxiliary-object
     :text "Answerer constructs a candidate move from substrate-2 context, naming+applying a library pattern (f6/pattern-as-strategy)."
     :consumes [:value-target] :produces :candidate-move
     :hole {:kind :sorry :discharge :sorryProof :satiety :canon
            :wanted "PSR/PUR pattern-use recording substrate is free"}}
    {:id :s3 :method :reduce-to-known-result
     :text "Critic scores construction-adequacy vs threshold; commit gate (q-turnstile-a)."
     :consumes [:candidate-move] :produces :gated-move
     :discharges {:to :critic-threshold}}
    {:id :s4 :method :transport-along-symmetry
     :text "Trail-recorder records the iteration for backtrack/learning (agent/trail-enables-return)."
     :consumes [:gated-move] :produces :trailed-move
     :hole {:kind :sorry :discharge :sorryProof :satiety :role
            :wanted "trail granularity is free"}}
    {:id :s5 :method :reduce-to-known-result
     :text "Value-ledger commits the gated move; value accrues over iterations (f6/self-play-loop)."
     :consumes [:trailed-move] :produces :value-ledger-entry
     :discharges {:to :value-accrues}}]
   :wires [{:from :s1 :to :s2 :carries :value-target}
           {:from :s2 :to :s3 :carries :candidate-move}
           {:from :s3 :to :s4 :carries :gated-move}
           {:from :s4 :to :s5 :carries :trailed-move}]
   :terminals [:s5]
   :policy-holes [{:free "the value metric" :why "undefined for the mission"}
                  {:free "Critic threshold + calibration cadence" :why "not fixed by the circumstance"}]})

;; The gate is TWO-LAYER (honest finding): the Lean render emits the comb wires/
;; consumes as DATA, so it does NOT catch composition breaks — those are caught by
;; the pre-flight (`fold-clean/carries-resolvable?`). The Lean 0-sorry render
;; catches typed-hole TYPE breaks (invalid satiety grade / discharge polarity /
;; ill-formed spine). So we show a RED for EACH layer.

;; RED-COMPOSE (pre-flight layer): s3 consumes a token nothing produces, and its
;; incoming wire carries that ghost token. The comb does not compose ⇒ pre-flight
;; FAILS. (Lean still types it — comb edges are data; that's the honest boundary.)
(def red-compose-turn
  (-> green-turn
      (assoc-in [:boxes 2 :consumes] [:ghost-token])
      (assoc :wires [{:from :s1 :to :s2 :carries :value-target}
                     {:from :s2 :to :s3 :carries :ghost-token}     ; s2 produces :candidate-move, not :ghost-token
                     {:from :s3 :to :s4 :carries :gated-move}
                     {:from :s4 :to :s5 :carries :trailed-move}])))

;; RED-TYPE (Lean layer): a typed hole graded with a NON-EXISTENT satiety grade.
;; The render emits `SatietyGrade.bogusGrade`, which is undefined ⇒ the Lean
;; render FAILS to type ⇒ NOT 0-sorry. This is the gate biting on malformed
;; typed-hole structure (an LLM-fold failure mode).
(def red-type-turn
  (assoc-in green-turn [:boxes 0 :hole :satiety] :bogus-grade))

(def cascade ["f6/self-play-loop" "f6/pattern-as-strategy" "f6/q-turnstile-a" "agent/trail-enables-return"])
(def circumstance {:mission "M-value-creation-loop" :psi "value creation self-play loop"})

(defn- write-clean! [dir proof turn]
  (let [out     (llm/llm-fold cascade circumstance {:turn-fn (constantly turn)})  ; real impl #2 path
        clean   (fc/fold->clean (:wiring out) {:proof proof
                                               :title "value-creating self-play loop (fold-generated)"
                                               :source {:cascade (vec cascade) :mission "M-value-creation-loop"}})
        pre     (fc/carries-resolvable? (:wiring out))
        f       (io/file dir (str proof ".clean.edn"))]
    (io/make-parents f)
    (spit f (fc/->edn clean))
    (println (format "  wrote %s  | pre-flight ok? %s%s"
                     (.getPath f) (:ok? pre)
                     (if (:ok? pre) ""
                         (str " (dangling-carries " (count (:dangling-carries pre))
                              ", dangling-consumes " (count (:dangling-consumes pre)) ")"))))
    (:ok? pre)))

(defn -main [& _]
  (println "L2 STANDARD-VERIFY — fold M-value-creation-loop cascade via impl #2 → CLean")
  (println "GREEN (well-formed — passes pre-flight + Lean 0-sorry):")
  (write-clean! "/tmp/l2-darktower/green" "l2green" green-turn)
  (println "RED-COMPOSE (dangling consume — caught by PRE-FLIGHT; Lean emits comb as data):")
  (write-clean! "/tmp/l2-darktower/red-compose" "l2redcompose" red-compose-turn)
  (println "RED-TYPE (invalid satiety grade — caught by the LEAN render):")
  (write-clean! "/tmp/l2-darktower/red-type" "l2redtype" red-type-turn)
  (println "\n.clean.edn written. Render+check: scripts/l2-darktower-verify.sh"))
