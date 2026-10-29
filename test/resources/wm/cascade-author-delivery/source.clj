(ns futon2.aif.wm.authored.req-aab759a8
  "Authored operational clauses for held executable cascade
  ee1da18c2cf14fcad3b1ba68afc2c98ddd98ced91db7627732df5c3321d5d352
  (phase :compile, requisition
  aab759a850cbf03fdda5c253c1ccb7c0bd71df81c29ac946cb74c3a2d78747de).

  Resolutions for the three :operational-clause-owed holes of the
  M-reachable-from-boot reading (analysis digest
  b8206e8c53d31177276a14f784ce827242085b4832b1961a69be22b31a6cbe29).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in
  the mission status line, completion-check names the observable
  condition in that text, and :consumes/:produces describe the
  transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/evidence-over-assertion" 0]
   {:operation :anchor-instantiate-completion-claim
    :capability :wm/pattern-apply
    :consumes :mission-status-completion-fragment
    :produces :dated-completion-record
    :completion-check
    [:instantiate-dated-for-named-sibling
     "mission status line reads 'INSTANTIATE complete (2026-05-01) for evidence-store sibling', dating the completion claim and naming its artifact"]}
   ["agent/escalation-cost-vs-risk" 1]
   {:operation :declare-out-of-cadence-hot-fix
    :capability :wm/pattern-apply
    :consumes :mission-urgency-fragment
    :produces :stop-the-line-exception-record
    :completion-check
    [:hot-fix-explicitly-outside-cadence
     "mission status line reads 'STOP-THE-LINE HOT-FIX, outside normal queue cadence', naming both the escalation and its cadence exception"]}
   ["agent/handoff-preserves-context" 2]
   {:operation :record-tracked-sibling-handoff
    :capability :wm/pattern-apply
    :consumes :mission-delegation-fragment
    :produces :codex-handoff-record
    :completion-check
    [:siblings-handed-off-via-named-issue
     "mission status line reads 'Three remaining siblings handed to Codex via GitHub issue #65', anchoring the handoff in a tracked issue number"]}})

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
