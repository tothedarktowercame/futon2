(ns futon2.aif.wm.authored.req-a2dd2a7e
  "Authored operational clauses for held executable cascade
  1b1d4d7c260198455294523e7a40b5afc91f1c9e36964f0bc751b8fbdcdb7a4e
  (phase :compile, requisition
  a2dd2a7e4abea771fe961c3b22d7eaa64c1732fbff577601b8ac9641dadf80ac).

  Resolutions for the twenty-nine :operational-clause-owed holes of the
  M-war-machine-aif-completion reading (target
  futon7/holes/M-war-machine-aif-completion.md).  Each decision is an
  operational clause attributed to the reading's occurrence squares over
  that document: the operation names what the occurrence enacts in the
  mission text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["war-machine/ideal-actual-gap" 0]
   {:operation :bound-mission-object-at-heading
    :capability :wm/pattern-apply
    :consumes :mission-heading-naming-object
    :produces :chartered-mission-with-bounded-object
    :completion-check
    [:heading-names-war-machine-aif-completion
     "mission heading bounds the object as 'M-war-machine-aif-completion' (line 1)"]}
   ["war-machine/operational-not-decorative" 1]
   {:operation :attribute-header-to-ai-engine
    :capability :wm/pattern-apply
    :consumes :structural-header-provenance-comment
    :produces :attributed-scaffold-header-record
    :completion-check
    [:header-declares-ai-authorship
     "the header comment states 'engine: structural header, AI-written', so no reader trusts it as Joe's prose"]}
   ["stack-coherence/evidence-ledger" 2]
   {:operation :stamp-scaffold-pass-with-date
    :capability :wm/pattern-apply
    :consumes :dated-scaffold-production-claim
    :produces :evidence-anchored-date-record
    :completion-check
    [:date-field-carries-reproducible-stamp
     "the Date field records 2026-05-17, anchoring the PM scaffold claim to a point in the ledger"]}
   ["forward-model/seal-the-graduation-criteria" 3]
   {:operation :report-open-status-against-sealed-criterion
    :capability :wm/pattern-apply
    :consumes :numbered-exit-criterion-from-section-0
    :produces :per-criterion-status-record
    :completion-check
    [:status-counts-exactly-one-satisfied-criterion
     "the status line says 'OPEN — exit criterion #2 satisfied' and cites §0 of M-descriptive-essay-of-the-stack.md, criterion #2, as the sealed source"]}
   ["eoi-engine/tension-foreclosure" 4]
   {:operation :gate-progress-on-joe-verbatims
    :capability :wm/pattern-apply
    :consumes :pending-operator-verbatim-input
    :produces :named-waiting-state-with-entry-channel
    :completion-check
    [:waiting-state-names-channel-not-silent
     "the status names what it awaits ('awaiting Joe verbatims via simulated eoi-engine eoi-inward-mission flash') instead of silently inheriting Joe's scope"]}
   ["forward-model/graduate-strategy-by-outcome" 5]
   {:operation :set-completion-bar-at-exemplar-parity
    :capability :wm/pattern-apply
    :consumes :verbatim-bar-quote-from-section-0
    :produces :named-graduation-bar-war-machine-ge-ukrn
    :completion-check
    [:bar-quoted-verbatim-and-named
     "the Bar line lifts the §0 quote verbatim and states the bar as 'War Machine ≥ ukrn-services-simulation coherence'"]}
   ["aif/belief-state-operational-hypotheses" 6]
   {:operation :enumerate-missing-belief-state-components
    :capability :wm/pattern-apply
    :consumes :bar-quotes-gap-list
    :produces :explicit-missing-component-list
    :completion-check
    [:gap-list-enumerates-three-components
     "the bar quote names 'no belief state, no predictive forward model, no EFE-scored action ranking' as today's absent components"]}
   ["war-machine/ideal-actual-gap" 7]
   {:operation :size-the-gap-in-effort-units
    :capability :wm/pattern-apply
    :consumes :gap-description-with-effort-estimate
    :produces :sized-discrepancy-report
    :completion-check
    [:gap-sized-explicitly-not-hours
     "the bar quote closes 'The gap is substantial — weeks of work, not hours'"]}
   ["forward-model/seal-the-graduation-criteria" 8]
   {:operation :bind-timebox-before-evidence
    :capability :wm/pattern-apply
    :consumes :timebox-estimate-sentence
    :produces :pre-committed-effort-window
    :completion-check
    [:timebox-stated-with-final-cap-reserved
     "the Timebox line states 'estimated 3-4 weeks' with the final cap deferred to Joe at §1.5 verbatim authoring"]}
   ["mission-coherence/logic-model-before-code" 9]
   {:operation :reserve-final-cap-to-operator
    :capability :wm/pattern-apply
    :consumes :verbatim-authoring-step-1-5
    :produces :operator-held-decision-rule
    :completion-check
    [:cap-decision-named-before-implementation
     "the Timebox sentence records 'final cap is Joe's call at §1.5 verbatim authoring', settling the rule before any build"]}
   ["contracts/holder-states-the-claim" 10]
   {:operation :state-mission-holder
    :capability :wm/pattern-apply
    :consumes :owner-line-naming-holder
    :produces :holder-record-joe-futon7
    :completion-check
    [:owner-line-names-holder-and-undefined-engineering-owner
     "the Owner line names 'Joe (futon7)' and leaves engineering ownership undefined pending Q-CL1"]}
   ["mission-coherence/logic-model-before-code" 11]
   {:operation :leave-engineering-ownership-pending-question
    :capability :wm/pattern-apply
    :consumes :open-design-question-q-cl1
    :produces :flagged-unresolved-ownership-invariant
    :completion-check
    [:ownership-unresolved-until-q-cl1-answered
     "the Owner line keeps 'engineering ownership undefined pending Q-CL1 (see §3.1)' rather than filling it by assumption"]}
   ["cascade-construction/read-what-exists-first" 12]
   {:operation :record-predecessor-sequencing-edge
    :capability :wm/pattern-apply
    :consumes :sequencing-role-sentence
    :produces :explicit-predecessor-edge-to-morphogenetic-rewrite
    :completion-check
    [:sequencing-asserted-from-existing-missions
     "the Sequencing role line states this mission is 'predecessor for M-stack-morphogenetic-rewrite' with 'Hard predecessor: none in the cluster'"]}
   ["stack-coherence/ready-blocked-triage" 13]
   {:operation :triage-external-dependency-ready-vs-blocked
    :capability :wm/pattern-apply
    :consumes :external-dependency-prose-on-arm-g
    :produces :status-marked-dependency-record
    :completion-check
    [:ready-part-named-separately-from-pending-part
     "the sequencing line marks 'substrate-2 already operational' (ready) while Arms A/B/F live data stays pending"]}
   ["forward-model/the-forward-model-is-a-pattern-cascade" 14]
   {:operation :record-live-data-dependency-for-forward-model
    :capability :wm/pattern-apply
    :consumes :arms-a-b-f-data-supply-condition
    :produces :upstream-data-edge-for-forward-model
    :completion-check
    [:training-data-source-named-with-arms
     "the sequencing line depends 'on Arms A/B/F producing live data the forward model can train on / score against'"]}
   ["contracts/resolve-pointers-at-head" 15]
   {:operation :gather-cross-references-at-document-head
    :capability :wm/pattern-apply
    :consumes :cross-ref-marker-line
    :produces :head-resolved-pointer-list
    :completion-check
    [:cross-ref-block-lists-named-forms
     "the Cross-ref block gathers eight named file pointers at the head of the document rather than burying them in prose"]}
   ["forward-model/graduate-strategy-by-outcome" 16]
   {:operation :cite-predecessor-as-graduation-exemplar
    :capability :wm/pattern-apply
    :consumes :ukrn-cross-reference-entry
    :produces :named-exemplar-bar-record
    :completion-check
    [:exemplar-named-as-both-predecessor-and-bar
     "the first cross-ref calls ukrn-services-simulation 'the predecessor exemplar; the bar this mission graduates the War Machine to'"]}
   ["forward-model/seal-the-graduation-criteria" 17]
   {:operation :cite-sealed-completeness-contract
    :capability :wm/pattern-apply
    :consumes :aif-completeness-doc-pointer
    :produces :cited-r1-r12-criterion-set
    :completion-check
    [:contract-named-with-companion-analogue
     "the cross-ref cites aif-completeness.md as the 'R1-R12 contract; companion to F1-F10 below'"]}
   ["stack-coherence/maturity-evidence-audit" 18]
   {:operation :place-fitness-contract-as-analogue
    :capability :wm/pattern-apply
    :consumes :stack-fitness-completeness-pointer
    :produces :correspondence-between-f1-f10-and-r1-r12
    :completion-check
    [:analogue-claim-stated-explicitly
     "the cross-ref names F1-F10 as 'the homeostat analogue of R1-R12' so maturity claims map across the two frameworks"]}
   ["mission-coherence/logic-model-before-code" 19]
   {:operation :adopt-trajectory-levels-as-sub-deliverable-map
    :capability :wm/pattern-apply
    :consumes :interim-director-inventory-sections-2-5-and-2-6
    :produces :explicit-sub-deliverable-map-before-code
    :completion-check
    [:levels-adopted-as-map-prior-to-implementation
     "the cross-ref says of the L1-L5 trajectory levels 'these levels are this mission's *sub-deliverable map*'"]}
   ["cascade-construction/read-what-exists-first" 20]
   {:operation :read-current-war-machine-source-first
    :capability :wm/pattern-apply
    :consumes :war-machine-clj-pointer-with-inventory
    :produces :read-and-cited-existing-surface
    :completion-check
    [:existing-surface-cited-with-precise-inventory
     "the cross-ref cites war_machine.clj (2850 lines, 13 scan-* projections, AIF observer at lines 1803–2060) before proposing additions"]}
   ["war-machine/state-capture" 21]
   {:operation :locate-existing-observation-state
    :capability :wm/pattern-apply
    :consumes :aif-observer-line-range
    :produces :captured-current-state-record
    :completion-check
    [:existing-observer-located-by-line-range
     "the cross-ref locates the 'AIF observer at lines 1803–2060' naming observation-channels through infer-mode"]}
   ["aif/belief-state-operational-hypotheses" 22]
   {:operation :correct-bar-claim-about-belief-state
    :capability :wm/pattern-apply
    :consumes :precision-note-on-section-0-quote
    :produces :corrected-baseline-mode-inference-exists
    :completion-check
    [:correction-states-what-partially-exists
     "the precision note says '\"no belief state\" understates current state — there is a mode-prior + mode-inference observer'"]}
   ["aif/candidate-pattern-action-space" 23]
   {:operation :enumerate-completion-gap-components
    :capability :wm/pattern-apply
    :consumes :completion-gap-sentence
    :produces :bounded-component-list-not-from-scratch
    :completion-check
    [:gap-listed-as-components-with-explicit-non-goal
     "the cross-ref closes with the enumerated gap 'forward-model + action-space + EFE-scored policy ranking + test harness + regression fixtures, not \"AIF substrate from scratch.\"'"]}
   ["war-machine/half-blind-observation" 24]
   {:operation :cite-related-observation-surface
    :capability :wm/pattern-apply
    :consumes :portfolio-observe-pointer
    :produces :cross-surface-coverage-comparison-record
    :completion-check
    [:related-surface-named-with-channel-count
     "the cross-ref cites futon3c portfolio/observe.clj as the '16-channel portfolio surface', inviting a coverage comparison"]}
   ["war-machine/dynamic-over-static" 25]
   {:operation :cite-operationalised-observation-channels
    :capability :wm/pattern-apply
    :consumes :war-bulletin-9-strategic-context-pointer
    :produces :live-channel-ruling-record-wr-16
    :completion-check
    [:ruling-named-and-operationalised
     "the cross-ref cites war-bulletin-9 for 'WR-16: operationalised exploit-loops as observation channels'"]}
   ["stack-coherence/commit-intent-alignment" 26]
   {:operation :claim-operadic-belief-updates-with-discipline
    :capability :wm/pattern-apply
    :consumes :typed-rewrite-discipline-pointer
    :produces :intent-aligned-update-claim-q-sa3
    :completion-check
    [:update-discipline-named-with-ruling
     "the cross-ref ties README-rewriting.md to 'the WM's belief updates are operadic-by-construction (Q-SA3)'"]}
   ["contracts/holder-states-the-claim" 27]
   {:operation :record-authorship-stages
    :capability :wm/pattern-apply
    :consumes :authors-line-with-pending-stage
    :produces :holder-stated-stage-record
    :completion-check
    [:each-author-carries-their-stage
     "the Authors line records 'Joe (verbatim answers, pending)' and claude-1's role with its date, so no stage word carries itself"]}
   ["war-machine/operational-not-decorative" 28]
   {:operation :limit-engine-role-to-connective-tissue
    :capability :wm/pattern-apply
    :consumes :engine-contribution-delimiter
    :produces :bounded-non-authoritative-scaffold-role
    :completion-check
    [:engine-contribution-explicitly-minimal
     "the Authors line bounds claude-1 to 'engine-side ordering and minimal structural connective tissue, 2026-05-17 PM scaffold'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "1b1d4d7c260198455294523e7a40b5afc91f1c9e36964f0bc751b8fbdcdb7a4e")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
