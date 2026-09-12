(require '[clojure.edn :as edn]
         '[clojure.string :as str])

(def input-path "holes/labs/wm-contract/runs/row-6-predictive-outcome/readback.edn")
(def output-path "/home/joe/code/mathlib4/DarkTower/WarMachine/MachineForwardModelWitness.lean")
(def report (edn/read-string (slurp input-path)))
(def support (:support report))
(def outcome-names
  {:abstained "abstained" :agent-unavailable "agentUnavailable"
   :artifact-only "artifactOnly" :build-failed "buildFailed"
   :cancelled "cancelled" :dispatch-failed "dispatchFailed"
   :grounded-change "groundedChange" :grounded-no-change "groundedNoChange"
   :guardrail-refusal "guardrailRefusal" :incomplete "incomplete"
   :no-selection "noSelection" :substrate-unavailable "substrateUnavailable"})
(def state-names ["addressed" "falsified" "foreclosed" "refined" "reopened" "spawned" "strengthened"])
(def mapped-outcomes (mapv (comp outcome-names second) (take 7 support)))
(defn qlit [x]
  (let [n (numerator x) d (denominator x)]
    (if (= d 1) (str n) (str "(" n " : ℚ) / " d))))
(defn row [id] (get-in report [:comparisons id :independent-exact-reference]))
(defn mass-cases [id]
  (str/join "\n" (for [o support]
                    (str "  | ." (outcome-names (second o)) " => " (qlit (get (row id) o))))))
(defn terminal-cases [id]
  (str/join "\n" (map (fn [s [_ o]]
                         (str "  | ." s " => " (qlit (get (row id) [:organization o]))))
                       state-names (take 7 support))))
(def lean
  (str "import DarkTower.WarMachine.MachineModelSpec\n\n"
       "namespace DarkTower.WarMachine.MachineForwardModelWitness\n\n"
       "open DarkTower.WarMachine.MachineModelSpec\n\n"
       "inductive O where\n  | abstained | agentUnavailable | artifactOnly | buildFailed | cancelled\n"
       "  | dispatchFailed | groundedChange | groundedNoChange | guardrailRefusal\n"
       "  | incomplete | noSelection | substrateUnavailable\n  deriving DecidableEq\n\n"
       "def allO : List O := [.abstained, .agentUnavailable, .artifactOnly, .buildFailed,\n"
       "  .cancelled, .dispatchFailed, .groundedChange, .groundedNoChange,\n"
       "  .guardrailRefusal, .incomplete, .noSelection, .substrateUnavailable]\n\n"
       "inductive S where | addressed | falsified | foreclosed | refined | reopened | spawned | strengthened\n"
       "  deriving DecidableEq\n\ndef allS : List S := [.addressed, .falsified, .foreclosed, .refined, .reopened, .spawned, .strengthened]\n\n"
       "def advanceTwiceMass : O → ℚ\n" (mass-cases "advance-twice") "\n\n"
       "def cascadeMass : O → ℚ\n" (mass-cases "advance-then-cascade") "\n\n"
       "def advanceTwiceTerminal : S → ℚ\n" (terminal-cases "advance-twice") "\n\n"
       "def cascadeTerminal : S → ℚ\n" (terminal-cases "advance-then-cascade") "\n\n"
       "def positionalA : S → O\n"
       (str/join "\n" (map (fn [s o] (str "  | ." s " => ." o)) state-names mapped-outcomes)) "\n\n"
       "def compose (q : S → ℚ) (o : O) : ℚ :=\n  (allS.map fun s => q s * if positionalA s = o then 1 else 0).sum\n\n"
       "def advanceTwiceRow : FloatCarriedRow O where\n  support := allO\n  mass := advanceTwiceMass\n"
       "  nonnegative := by intro o; cases o <;> norm_num [advanceTwiceMass]\n"
       "  nearNormalised := by norm_num [allO, advanceTwiceMass, floatRowBound]\n\n"
       "def cascadeRow : FloatCarriedRow O where\n  support := allO\n  mass := cascadeMass\n"
       "  nonnegative := by intro o; cases o <;> norm_num [cascadeMass]\n"
       "  nearNormalised := by norm_num [allO, cascadeMass, floatRowBound]\n\n"
       "def advanceTwiceRepeatRow : FloatCarriedRow O := advanceTwiceRow\n\n"
       "theorem advanceTwiceComposition (o : O) : advanceTwiceMass o = compose advanceTwiceTerminal o := by\n"
       "  cases o <;> norm_num [advanceTwiceMass, advanceTwiceTerminal, compose, allS, positionalA]\n\n"
       "theorem cascadeComposition (o : O) : cascadeMass o = compose cascadeTerminal o := by\n"
       "  cases o <;> norm_num [cascadeMass, cascadeTerminal, compose, allS, positionalA]\n\n"
       "theorem repeatedComposition (o : O) : advanceTwiceRepeatRow.mass o = advanceTwiceRow.mass o := rfl\n\n"
       "theorem inheritedExcess :\n"
       "    (allO.map advanceTwiceMass).sum - 1 = (1 : ℚ) / 36028797018963968 ∧\n"
       "    (allO.map cascadeMass).sum - 1 = (1 : ℚ) / 36028797018963968 := by\n"
       "  norm_num [allO, advanceTwiceMass, cascadeMass]\n\n"
       "end DarkTower.WarMachine.MachineForwardModelWitness\n"))

(spit output-path lean)
(prn {:generated output-path :policies 3 :coordinates 36
      :source input-path :reference-method (:reference-method report)})
