(ns futon2.aif.wm.authored.req-aab759a8
  "Authored operational clauses for held executable cascade
  ee1da18c2cf14fcad3b1ba68afc2c98ddd98ced91db7627732df5c3321d5d352
  (phase :compile, requisition
  aab759a850cbf03fdda5c253c1ccb7c0bd71df81c29ac946cb74c3a2d78747de).

  Resolutions for the three :operational-clause-owed holes owned by
  :wm/enactment-compiler.  Each decision is an operational clause: the
  operation names what the compiler enacts for that hole, completion-check
  names the observable condition, and :consumes/:produces describe the
  transition where it is well-defined.  No claim is made about holes outside
  this requisition."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/evidence-over-assertion" 0]
   {:operation :bind-clause-to-observed-evidence
    :capability :wm/enactment-compile
    :consumes :retrieved-source-pins
    :produces :evidence-anchored-clause
    :completion-check
    [:decision-cites-pinned-source
     "every compiled decision for this hole names a full-value source pin it was derived from, rather than an unanchored assertion"]}
   ["agent/escalation-cost-vs-risk" 1]
   {:operation :scale-commitment-to-assessed-risk
    :capability :wm/enactment-compile
    :consumes :hole-risk-assessment
    :produces :cost-matched-escalation-clause
    :completion-check
    [:escalation-cost-recorded-against-risk
     "the compiled clause records the coordination cost paid and the risk it bounds, so the pair can be compared at review"]}
   ["agent/handoff-preserves-context" 2]
   {:operation :carry-requisition-context-into-patch
    :capability :wm/enactment-compile
    :consumes :requisition-context
    :produces :self-identifying-patch
    :completion-check
    [:patch-names-its-requisition
     "the delivered patch namespace records the cascade digest, phase and requisition id it answers, so a later reader can re-derive the dispatch from the source alone"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "ee1da18c2cf14fcad3b1ba68afc2c98ddd98ced91db7627732df5c3321d5d352")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
