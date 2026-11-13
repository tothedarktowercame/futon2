(ns futon2.aif.wm.authored.req-7a54c202
  "Repair patch for requisition 7a54c202b7eab2b5fe73ede6e0cc34b2e3dd7bec964400119c61964f4b892274,
   cascade 71634e886fbff1aebcf95d82a9708e28ab3634e33f4186402c8d113c1a4a9abd phase :compile.
   Supplies the operational clause owed for each of the three held
   :operational-clause-owed holes (owner :wm/enactment-compiler)."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["eight-gates/roll-back-hold" 4]
   {:operation :apply-roll-back-hold-on-enactment-stall
    :capability :wm/pattern-apply
    :consumes [:partial-enactment-state]
    :produces [:held-executable-cascade]
    :completion-check
    [:hold-recorded-with-named-restart-point
     "the held phase records a roll-back-hold entry naming the restart point before further gates run"]}
   ["eight-gates/roll-back-hold" 5]
   {:operation :resume-from-recorded-hold-point
    :capability :wm/pattern-apply
    :consumes [:roll-back-hold-entry]
    :produces [:resumed-cascade-phase]
    :completion-check
    [:resume-reads-the-recorded-hold-entry
     "resumption re-enters the cascade from the recorded hold point rather than replaying the stalled gate blind"]}
   ["contracts/every-entry-has-a-falsifier" 6]
   {:operation :void-or-uphold-entry-by-named-falsifier
    :capability :wm/pattern-apply
    :consumes [:entry-with-premise]
    :produces [:falsified-or-standing-entry-verdict]
    :completion-check
    [:every-entry-names-its-falsifier
     "each compiled cascade entry carries an observable that would falsify it; entries without one are voided, not admitted"]}})

(defn decisions
  "Function of the resume context {:cascade-digest :phase}; returns the
  operational clause map keyed by hole id, bound to the cascade this patch
  repairs.  A foreign context is a typed mismatch, never a silent answer."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "71634e886fbff1aebcf95d82a9708e28ab3634e33f4186402c8d113c1a4a9abd")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
