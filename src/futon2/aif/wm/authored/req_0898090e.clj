(ns futon2.aif.wm.authored.req-0898090e
  "Authored operational clauses for held executable cascade
  623b86cba4ae23f6ffed710ce7b73d935f9aea9e82f06856d376423ae287301b
  (phase :compile, requisition
  0898090e48176f4d0b8a39978fb8c8cf5b17ed2939640d791fca4fc01f366083).

  Resolutions for the seventeen :operational-clause-owed holes owned by
  :wm/enactment-compiler.  Each decision is an operational clause routed
  through the single routable capability :wm/pattern-apply; the operation
  names the pattern application that repairs the hole, completion-check
  names the observable condition, and :consumes/:produces describe the
  transition.  No claim is made about holes outside this requisition."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["futon-theory/mission-lifecycle" 0]
   {:operation :apply-mission-lifecycle-to-cascade-repair-entry
    :capability :wm/pattern-apply
    :consumes :unrecorded-cascade-repair-transition
    :produces :auditable-mission-state-transition
    :completion-check
    [:transition-recorded-in-mission-record
     "the cascade repair is recorded as an explicit lifecycle state transition in the mission doc header (Status field) with timestamp and agent attribution, not left as drifting inline work"]}
   ["problems/r2-structured-observation" 1]
   {:operation :apply-structured-observation-to-cascade-evidence-ingest
    :capability :wm/pattern-apply
    :consumes :heterogeneous-cascade-evidence-artifacts
    :produces :normalized-observation-feature-map
    :completion-check
    [:evidence-emits-fixed-normalized-shape
     "each heterogeneous evidence artifact admitted to the cascade is translated into the fixed, normalized observation shape before it enters the control loop, rather than being consumed as free prose"]}
   ["problems/r9-no-self-certification" 2]
   {:operation :apply-no-self-certification-to-phase-completion-verdict
    :capability :wm/pattern-apply
    :consumes :claimant-issued-completion-verdict
    :produces :independently-certified-verdict
    :completion-check
    [:verdict-moves-only-on-independent-evidence
     "the compile-phase completion verdict moves only on evidence made by a party other than the claimant; a self-issued certificate counts as nothing and leaves the phase open"]}
   ["problems/r9-no-self-certification" 3]
   {:operation :apply-no-self-certification-to-evidence-birth-tags
    :capability :wm/pattern-apply
    :consumes :untagged-decision-evidence
    :produces :birth-tagged-evidence-set
    :completion-check
    [:every-admitted-evidence-item-is-birth-tagged
     "every evidence item the compiler admits for these clauses carries a birth tag naming its independent origin; untagged items are rejected rather than silently counted"]}
   ["futon-theory/mission-lifecycle" 4]
   {:operation :apply-mission-lifecycle-to-blocked-dependency
    :capability :wm/pattern-apply
    :consumes :unstated-blocking-dependency
    :produces :blocked-state-with-cited-blocker
    :completion-check
    [:blocked-state-cites-its-blocker
     "any hole resolution that cannot proceed is entered as :blocked in the mission lifecycle with the blocking dependency cited, instead of stalling silently in :active"]}
   ["futon-theory/mission-lifecycle" 6]
   {:operation :apply-mission-lifecycle-to-regression-on-review
    :capability :wm/pattern-apply
    :consumes :review-rejection-without-state-change
    :produces :recorded-regression-transition
    :completion-check
    [:regression-is-an-explicit-transition
     "a reviewer rejection moves the mission back through an explicit :review to :active regression transition that is recorded with its reason, so the trail shows why work reopened"]}
   ["futon-theory/mission-lifecycle" 7]
   {:operation :apply-mission-lifecycle-to-closure-gate
    :capability :wm/pattern-apply
    :consumes :closure-request
    :produces :criteria-gated-done-transition
    :completion-check
    [:done-requires-met-criteria-and-recorded-evidence
     "the :done transition fires only when the mission's declared success criteria are met and the evidence is recorded in the mission record; unmet criteria keep the mission in :review"]}
   ["futon-theory/mission-lifecycle" 8]
   {:operation :apply-mission-lifecycle-to-scope-change
    :capability :wm/pattern-apply
    :consumes :silent-scope-drift
    :produces :explicit-rescope-or-split-decision
    :completion-check
    [:scope-change-is-a-recorded-transition-or-split
     "any change in the cascade's scope is expressed as a recorded lifecycle decision (re-scope, split into sub-missions, or merge), never as invisible drift inside the same state"]}
   ["problems/unqueryable-trails-block-recovery" 9]
   {:operation :apply-unqueryable-trails-repair-to-rejected-path-record
    :capability :wm/pattern-apply
    :consumes :unrecorded-rejected-approach
    :produces :queryable-rejected-path-entry
    :completion-check
    [:rejected-approaches-are-queryable-trail-entries
     "each abandoned approach in the cascade repair is written to a queryable trail entry recording what was tried and why it was rejected, so a later tick can find it instead of repeating it"]}
   ["problems/r2-structured-observation" 10]
   {:operation :apply-structured-observation-to-channel-stability
    :capability :wm/pattern-apply
    :consumes :drifting-channel-semantics
    :produces :stable-channel-meaning-binding
    :completion-check
    [:channel-meanings-stable-across-ticks
     "the named observation channels keep their meaning and ordering across ticks; a semantic change to a channel is a schema version change, not a silent re-keying"]}
   ["problems/r1-belief-state" 11]
   {:operation :apply-belief-state-to-cascade-hypothesis-carry
    :capability :wm/pattern-apply
    :consumes :raw-history-reasoning
    :produces :carried-hypothesis-map-with-precision
    :completion-check
    [:hypotheses-carried-with-explicit-precision
     "the cascade's operational hypotheses are held as a belief-state map carried across ticks with mean and precision both explicitly represented, updated from observations and outcomes rather than rebuilt from raw history each tick"]}
   ["problems/r9-no-self-certification" 12]
   {:operation :apply-no-self-certification-to-check-issuer
    :capability :wm/pattern-apply
    :consumes :claimant-issued-closing-check
    :produces :separate-issuer-closing-check
    :completion-check
    [:closing-check-issued-by-non-claimant
     "the check that closes each hole is issued by machinery or an agent other than the one that authored the resolution; a claimant cannot manufacture or issue the check that closes its own claim"]}
   ["problems/unqueryable-trails-block-recovery" 13]
   {:operation :apply-unqueryable-trails-repair-to-exploration-marking
    :capability :wm/pattern-apply
    :consumes :ambiguous-exploration-status
    :produces :distinguished-explored-and-unexplored-marking
    :completion-check
    [:unexplored-distinguishable-from-explored-rejected
     "the trail marks each candidate path as unexplored, explored-accepted, or explored-and-rejected, so the compiler can distinguish fresh options from known dead ends and recover by returning to a recorded branch point"]}
   ["problems/r2-structured-observation" 14]
   {:operation :apply-structured-observation-to-schema-keyed-machinery
    :capability :wm/pattern-apply
    :consumes :ad-hoc-observation-interpretation
    :produces :schema-keyed-downstream-consumers
    :completion-check
    [:downstream-machinery-keyed-to-observation-schema
     "downstream cascade machinery consumes observations only through the normalized schema's named channels; repeated prose interpretation of raw artifacts is replaced by schema-keyed dispatch"]}
   ["futon-theory/mission-lifecycle" 15]
   {:operation :apply-mission-lifecycle-to-transition-attribution
    :capability :wm/pattern-apply
    :consumes :anonymous-state-transition
    :produces :attributed-timestamped-transition-record
    :completion-check
    [:transition-carries-timestamp-and-agent-attribution
     "every lifecycle transition written during this cascade repair carries a timestamp and agent attribution in the mission record, so the trail shows who moved the mission and when"]}
   ["problems/r8-present-fit-mismatch" 16]
   {:operation :apply-present-fit-mismatch-to-compile-phase-signal
    :capability :wm/pattern-apply
    :consumes :absorbed-prediction-error
    :produces :exposed-present-fit-signal
    :completion-check
    [:present-fit-mismatch-exposed-not-absorbed
     "the mismatch between the compile phase's predicted outcome and the observed outcome is exposed as an explicit present-fit signal keyed to the observation channels, not absorbed as a flattering pass"]}
   ["futon-theory/mission-lifecycle" 17]
   {:operation :apply-mission-lifecycle-to-done-evidence-recording
    :capability :wm/pattern-apply
    :consumes :success-criteria-met-silently
    :produces :done-transition-with-recorded-evidence
    :completion-check
    [:done-transition-cites-its-evidence
     "the terminal :done transition for this cascade cites the recorded evidence items that satisfy the success criteria, so closure is auditable from the mission record alone"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "623b86cba4ae23f6ffed710ce7b73d935f9aea9e82f06856d376423ae287301b")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
