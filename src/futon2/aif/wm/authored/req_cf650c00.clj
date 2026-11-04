(ns futon2.aif.wm.authored.req-cf650c00
  "Authored operational clauses for held executable cascade
  8a9302cba19696222e9844f179b66f14fb93143e1372adaaea574a33b2e3045f
  (phase :compile, requisition cf650c00b4bbda872d3fa39210d985aad44cbd4b933a286112c24222cbf1a1ec).

  Resolutions for the six :operational-clause-owed holes of the
  T-car3-queue-routing-design-note reading (analysis digest
  8bb0a2054338d65a30ae13fdde894ca0238c59dc24725530396fac33dceb889b).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in
  the ticket text, completion-check names the observable condition in
  that text, and :consumes/:produces describe the transition.  Nothing
  here claims a square-derived completion and no application-order edge
  is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/scope-before-action" 0]
   {:operation :declare-phase-1-design-scope
    :capability :wm/pattern-apply
    :consumes :ticket-heading
    :produces :phase-1-scope-record
    :completion-check
    [:heading-names-design-note
     "ticket heading reads 'T-car3-queue-routing — Phase 1 design note'"]}
   ["agent/provisional-claims-ledger" 1]
   {:operation :record-dated-partial-status
    :capability :wm/pattern-apply
    :consumes :triage-status-line
    :produces :partial-status-ledger-entry
    :completion-check
    [:status-dated-partial
     "triage line reads 'Status (triaged 2026-09-01): PARTIAL -- mechanism landed, capture-gap fallback still tolerated.'"]}
   ["contracts/holder-states-the-claim" 2]
   {:operation :state-exact-closure-route-claim
    :capability :wm/pattern-apply
    :consumes :closure-sentence
    :produces :closure-route-claim
    :completion-check
    [:closure-route-exactly-stated
     "closure sentence names proving all accept sites supply stable :from/:surface/:msg-id or accepting the fallback as the contract"]}
   ["agent/evidence-over-assertion" 2]
   {:operation :anchor-accept-site-claim-to-source
    :capability :wm/pattern-apply
    :consumes :turn-queue-line-references
    :produces :anchored-accept-site-claim
    :completion-check
    [:accept-sites-cited-by-line
     "ticket cites turn_queue.clj:213,241,523, turn_queue.clj:221-239 and :msg-id-source as the anchors for the accept-site claim"]}
   ["agent/handoff-preserves-context" 3]
   {:operation :emit-ratification-handoff-status
    :capability :wm/pattern-apply
    :consumes :phase-status-line
    :produces :ratification-handoff-record
    :completion-check
    [:status-reads-model-only
     "status line reads 'Status: model-only proposal for ratification.'"]}
   ["agent/scope-before-action" 4]
   {:operation :fence-implementation-out-of-phase
    :capability :wm/pattern-apply
    :consumes :phase-boundary-phrase
    :produces :phase-scope-fence
    :completion-check
    [:implementation-excluded-this-phase
     "note closes 'No implementation is included in this phase.'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "8a9302cba19696222e9844f179b66f14fb93143e1372adaaea574a33b2e3045f")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
