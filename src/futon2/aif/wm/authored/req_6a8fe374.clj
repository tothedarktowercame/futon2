(ns futon2.aif.wm.authored.req-6a8fe374
  "Authored operational clauses for held executable cascade
  5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995
  (phase :compile, requisition
  6a8fe374958818d6246ee46acc6dbdb851c7cdf66930626395572519879d195c).

  Resolutions for the twelve :operational-clause-owed holes of the
  M-typed-memories reading.  Each decision is an operational clause
  attributed to the reading's occurrence squares (analysis digest
  8374b72faccbba95148c79282c6a1aca4877e0ad84a654382cde58e515da5693):
  the operation names what the occurrence enacts in the mission text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  No application-order
  edge is authored here."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["problems/theory-general-enough-to-drive-the-stack" 0]
   {:operation :instantiate-one-vocabulary-across-channels
    :capability :wm/pattern-apply
    :consumes :mission-charter-thesis
    :produces :third-iatc-instantiation
    :completion-check
    [:third-instantiation-recorded
     "mission title frames memories as typed dialogue acts and the prior-art note names memories the third IATC instantiation"]}
   ["agent/reduction-to-kernel" 1]
   {:operation :stage-mvp-plus-upgrade-path
    :capability :wm/pattern-apply
    :consumes :derive-phase-charter
    :produces :prototype-ladder-plan
    :completion-check
    [:p0-landed-accepted
     "mission records P0 LANDED + ACCEPTED 2026-07-22 evening with 4 typed memories through the tool end-to-end"]}
   ["agent/scope-before-action" 2]
   {:operation :stage-one-position-for-argue
    :capability :wm/pattern-apply
    :consumes :phase-ladder-position
    :produces :argue-staged-position
    :completion-check
    [:argue-position-staged
     "mission records 'One position staged for ARGUE (§Staged for ARGUE)'"]}
   ["aif/declare-the-conditioning" 3]
   {:operation :wire-pattern-conditioned-recall
    :capability :wm/pattern-apply
    :consumes :existing-hyperedge-end-query
    :produces :psr-search-pattern-conditioning
    :completion-check
    [:conditioning-key-declared
     "mission names pattern-conditioned recall via the existing futon1b hyperedge :end query wired into psr_search/psr_select"]}
   ["aif/no-self-certification" 4]
   {:operation :require-independent-outcome-witness
    :capability :wm/pattern-apply
    :consumes :lean-trial-closures
    :produces :witnessed-outcome-closures
    :completion-check
    [:trials-independently-witnessed
     "mission records three fresh Lean trials closed with independently witnessed outcomes, audit table in M-shared-memory-control-build-test"]}
   ["agent/eat-your-own-tail" 5]
   {:operation :close-p1-by-feeding-own-selection-loop
    :capability :wm/pattern-apply
    :consumes :memory-substrate-with-pattern-endpoints
    :produces :p1-psr-wiring-closure
    :completion-check
    [:p1-wiring-and-lifecycle-rung-closed
     "mission records 'This closes this mission's P1 PSR wiring and lifecycle rung' after the accepted zai-6 cited-use trial"]}
   ["agent/handoff-preserves-context" 6]
   {:operation :hand-accepted-live-context-to-phase-4
    :capability :wm/pattern-apply
    :consumes :phases-1-3-acceptance-evidence
    :produces :phase-4-entry-context
    :completion-check
    [:phase-4-named-next
     "cross-reference records 'Phases 1-3 are accepted live; Phase 4 is next'"]}
   ["agent/eat-your-own-tail" 7]
   {:operation :populate-substrate-by-construction
    :capability :wm/pattern-apply
    :consumes :typed-epistemic-channel
    :produces :memory-entries-as-act-byproduct
    :completion-check
    [:populates-by-construction-stated
     "mission states the epistemic channel is typed 'so the memory substrate populates by construction'"]}
   ["agent/coordination-has-cost" 8]
   {:operation :group-write-paths-by-hazard-class
    :capability :wm/pattern-apply
    :consumes :typed-bell-write-hazard
    :produces :shared-write-path-resolution-discipline
    :completion-check
    [:hazard-class-link-recorded
     "mission records the memory write path as 'in the same hazard class' with one discipline resolving both"]}
   ["agency/delivery-receipt" 11]
   {:operation :share-use-outcome-receipt-seam
    :capability :wm/pattern-apply
    :consumes :common-memory-contract
    :produces :shared-receipt-seam-with-domain-witnesses
    :completion-check
    [:common-receipt-seam-accepted
     "mission records the shared-regime common use-receipt seam accepted live, with domain endpoints and witnesses differing"]}
   ["agent/student-dispatch" 12]
   {:operation :run-mathematics-as-cheap-test-tier
    :capability :wm/pattern-apply
    :consumes :promotion-gate-ladder
    :produces :common-mechanism-evidence
    :completion-check
    [:test-tier-role-stated
     "mission states mathematics runs are 'a cheap test tier for the common mechanism, not evidence that a WM-specific policy or outcome model is correct'"]}
   ["agent/eat-your-own-tail" 13]
   {:operation :open-self-dialogue-with-challenge-and-retraction
    :capability :wm/pattern-apply
    :consumes :bitemporal-lifecycle-features
    :produces :contestable-past-positions
    :completion-check
    [:challenge-retraction-accepted
     "mission records Phase 3 accepted live adding bitemporal challenge/supersession/retraction and correction, framing memories as the agent's dialogue with its past self"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
