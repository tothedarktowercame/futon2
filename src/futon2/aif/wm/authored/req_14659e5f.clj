(ns futon2.aif.wm.authored.req-14659e5f
  "Authored operational clauses for held executable cascade
  87ec0d695a753cb5c03c37d80ce6c4ea0f414487367cf28fa203daf4ae2e0a61
  (phase :compile, requisition 14659e5fd62eccf92b873728388df290fbc12bb1c0daf9fcb9fe9950300a7eab).

  Resolutions for the fifteen :operational-clause-owed holes of the
  M-interim-director reading (analysis digest
  ab3c15c9c6cd22de92dd1ab0efb24d2b29621b49c0c9944e8e04b03800982363).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in
  the mission header text, completion-check names the observable
  condition in that text, and :consumes/:produces describe the
  transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/scope-before-action" 0]
   {:operation :name-mission-identity
    :capability :wm/pattern-apply
    :consumes :mission-title-line
    :produces :named-mission-identity
    :completion-check
    [:mission-named-m-interim-director
     "mission title reads '# Mission: M-interim-director' before any work is described"]}
   ["stack-coherence/evidence-ledger" 1]
   {:operation :mark-header-machine-attribution
    :capability :wm/pattern-apply
    :consumes :header-provenance-comment
    :produces :ai-written-header-attribution
    :completion-check
    [:header-marked-ai-written
     "header comment reads '<!-- engine: structural header, AI-written -->'"]}
   ["agent/handoff-preserves-context" 2]
   {:operation :anchor-mission-open-date
    :capability :wm/pattern-apply
    :consumes :date-line
    :produces :dated-mission-anchor
    :completion-check
    [:date-recorded-2026-05-14
     "header records '**Date:** 2026-05-14' anchoring the timebox and authorship timestamps"]}
   ["agent/provisional-claims-ledger" 3]
   {:operation :record-open-identify-status
    :capability :wm/pattern-apply
    :consumes :status-line
    :produces :open-status-ledger-entry
    :completion-check
    [:status-open-head-authored
     "status line reads 'OPEN — HEAD authored (IDENTIFY phase…)'"]}
   ["agent/commitment-varies-with-confidence" 4]
   {:operation :record-round-collapse-mode
    :capability :wm/pattern-apply
    :consumes :engine-mode-flag
    :produces :round-collapse-mode-record
    :completion-check
    [:round-collapse-mode-false-recorded
     "status line records 'round-collapse mode `false`' as the run's commitment setting"]}
   ["agent/budget-bounds-exploration" 5]
   {:operation :bound-mission-timebox
    :capability :wm/pattern-apply
    :consumes :timebox-line
    :produces :two-week-timebox-bound
    :completion-check
    [:timebox-two-weeks-maximum
     "timebox line reads '**Timebox:** 2 weeks maximum from 2026-05-14 (≈10 working days net of 4 travel days)'"]}
   ["agent/escalation-cost-vs-risk" 5]
   {:operation :weigh-predecessor-risk-against-timebox
    :capability :wm/pattern-apply
    :consumes :hard-predecessor-declaration
    :produces :risk-vs-coordination-comparison-basis
    :completion-check
    [:gap-handling-deferred-to-tension
     "header routes the hard-predecessor gap to 'Tension 2 below' rather than silently blocking or proceeding"]}
   ["agent/trail-enables-return" 6]
   {:operation :cite-predecessor-mission-edge
    :capability :wm/pattern-apply
    :consumes :sequencing-role-line
    :produces :reciprocal-predecessor-trail
    :completion-check
    [:successor-to-m-interest-network-coupling
     "sequencing line names M-interest-network-coupling.md and notes its §1.Relationship points back at this mission"]}
   ["agent/coordination-has-cost" 7]
   {:operation :declare-blocking-predecessor-dependency
    :capability :wm/pattern-apply
    :consumes :predecessor-step-reference
    :produces :typed-blocking-dependency
    :completion-check
    [:hard-predecessor-named
     "header states predecessor step (b), 'event vocabulary committed in code', is the hard predecessor for emitting valid EDN checkpoint delta-batches"]}
   ["agent/provisional-claims-ledger" 8]
   {:operation :defer-gap-to-named-tension
    :capability :wm/pattern-apply
    :consumes :dependency-gap
    :produces :tracked-deferral-entry
    :completion-check
    [:tension-2-named-as-resolution-site
     "header defers the dependency gap with 'see Tension 2 below for how this HEAD handles the gap'"]}
   ["agent/scope-before-action" 9]
   {:operation :assign-mission-owner
    :capability :wm/pattern-apply
    :consumes :owner-line
    :produces :named-mission-owner
    :completion-check
    [:owner-joe-futon7
     "owner line reads '**Owner:** Joe (futon7).'"]}
   ["agent/handoff-preserves-context" 10]
   {:operation :cross-reference-working-paper-verbatim
    :capability :wm/pattern-apply
    :consumes :cross-ref-line
    :produces :verbatim-surfacing-promise
    :completion-check
    [:workbook-surfaced-verbatim-in-1-2
     "cross-ref cites v11_year1_scenarios_for_jacobs.xlsx and promises it is 'surfaced verbatim in §1.2 below'"]}
   ["stack-coherence/evidence-ledger" 11]
   {:operation :record-typed-institution-attribution
    :capability :wm/pattern-apply
    :consumes :eoi-instance-entry
    :produces :typed-institution-object-ref
    :completion-check
    [:institution-ref-node-hyperreal
     "eoi entry records 'institution_object_ref = :node/hyperreal-enterprises' with subtype 'self-authored' and stance 'constituted'"]}
   ["agent/evidence-over-assertion" 12]
   {:operation :retain-joe-answers-verbatim
    :capability :wm/pattern-apply
    :consumes :authorship-line
    :produces :verbatim-source-retention
    :completion-check
    [:authors-joe-verbatim-answers
     "authors line records 'Joe (verbatim answers)' as the substantive content"]}
   ["agent/state-is-hypothesis" 13]
   {:operation :separate-machine-structure-from-content
    :capability :wm/pattern-apply
    :consumes :authorship-line
    :produces :separated-authorship-record
    :completion-check
    [:claude-11-minimal-connective-tissue
     "authors line limits claude-11 to 'engine-side ordering and minimal structural connective tissue'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "87ec0d695a753cb5c03c37d80ce6c4ea0f414487367cf28fa203daf4ae2e0a61")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
