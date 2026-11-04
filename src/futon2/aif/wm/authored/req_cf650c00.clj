(ns futon2.aif.wm.authored.req-cf650c00
  "Authored operational clauses for held executable cascade
  8a9302cba19696222e9844f179b66f14fb93143e1372adaaea574a33b2e3045f
  (phase :compile, requisition
  cf650c00b4bbda872d3fa39210d985aad44cbd4b933a286112c24222cbf1a1ec).

  Resolutions for the six :operational-clause-owed holes owned by
  :wm/enactment-compiler.  Each decision is an operational clause whose
  operation and completion-check are grounded in the cited pattern's own
  THEN branch (futon3 library source), not in a square-derived completion;
  no application-order edge is authored here."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/scope-before-action" 0]
   {:operation :declare-in-bounds-and-out-of-bounds
    :capability :wm/pattern-apply
    :consumes :candidate-work-description
    :produces :declared-scope-record
    :completion-check
    [:scope-declared-before-action
     "the in-bounds files, concepts, or regions and the explicitly out-of-bounds ones are stated before substantive action begins"]}

   ["agent/provisional-claims-ledger" 1]
   {:operation :record-provisional-claim-with-resolution-fields
    :capability :wm/pattern-apply
    :consumes :unsupported-claim
    :produces :ledger-entry-with-fields
    :completion-check
    [:ledger-entry-has-required-fields
     "each provisional claim is a ledger entry carrying claim, reason, evidence needed, expiry, and owner, and is resolved to confirmed or retracted"]}

   ["contracts/holder-states-the-claim" 2]
   {:operation :state-holder-correspondence-kind
    :capability :wm/pattern-apply
    :consumes :holder-entry
    :produces :stated-correspondence-record
    :completion-check
    [:holder-entry-names-claim-kind
     "each entry carries one holder naming exactly which correspondence is claimed (model-transcription-only, runtime-correspondence-not-live-path, or runtime-correspondence-live-shadow), never an unstated or stronger claim"]}

   ["agent/evidence-over-assertion" 2]
   {:operation :anchor-substantive-claim-to-evidence
    :capability :wm/pattern-apply
    :consumes :substantive-claim
    :produces :claim-with-evidence-anchor
    :completion-check
    [:every-substantive-claim-anchored
     "every substantive claim points to evidence — a file path, trace, or recorded observation — so assertions are distinguishable from confabulation"]}

   ["agent/handoff-preserves-context" 3]
   {:operation :emit-handoff-record
    :capability :wm/pattern-apply
    :consumes :work-state-at-handoff
    :produces :handoff-record
    :completion-check
    [:handoff-record-carries-required-state
     "the handoff record captures current state hypothesis, decisions made and why, open questions, scope boundaries, and next actions, so the successor does not restart from scratch"]}

   ["agent/scope-before-action" 4]
   {:operation :bind-done-definition-to-declared-scope
    :capability :wm/pattern-apply
    :consumes :declared-scope-record
    :produces :measurable-done-definition
    :completion-check
    [:done-defined-against-scope
     "'done' is defined against the declared scope, so progress and side effects are measurable and observers can tell on-track from wandering"]}})

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
