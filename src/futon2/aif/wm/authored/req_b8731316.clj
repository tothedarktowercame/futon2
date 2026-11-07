(ns futon2.aif.wm.authored.req-b8731316
  "Authored operational clauses for held executable cascade
  f07a4fa80f27c47c28e8ec4c6f2fcd8d7a8bced439e401dcd78fedec468135ba
  (phase :compile, requisition
  b873131665de580b3ced5d24a42e723ef78c3f9a2e004cc09e55c75a6e7a67cf).

  Resolutions for the twelve :operational-clause-owed holes of the
  T-fail-operator-cancelled reading (target
  futon3c/holes/tickets/T-fail-operator-cancelled.md, census-header
  excerpt, source digest 5b82b64f...609fa).  Each decision is an
  operational clause attributed to the reading's occurrence squares over
  that excerpt: the operation names what the occurrence enacts in the
  ticket text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["apparatus/new-failure-class-is-a-design-defect" 0]
   {:operation :classify-failure-class-in-title
    :capability :wm/pattern-apply
    :consumes :ledger-typed-terminal-code
    :produces :named-failure-class-with-multiplicity
    :completion-check
    [:title-names-class-and-multiplicity
     "the ticket title reads 'T-fail-operator-cancelled -- operator-cancelled (4x)', so the failure class is named and counted before anyone responds to it"]}
   ["apparatus/new-failure-class-is-a-design-defect" 1]
   {:operation :mark-class-as-reclassify-candidate
    :capability :wm/pattern-apply
    :consumes :triage-review-observation
    :produces :reclassification-decision-open-on-the-class
    :completion-check
    [:status-banner-names-open-decision
     "the status banner reads 'RECLASSIFY CANDIDATE', keeping the class an open design question rather than an accepted failure"]}
   ["agent/pause-is-not-failure" 2]
   {:operation :attribute-cancels-to-deliberate-watchdog
    :capability :wm/pattern-apply
    :consumes :review-of-19-terminal-codes
    :produces :cancels-classified-as-deliberate-not-defects
    :completion-check
    [:majority-named-deliberate-watchdog-cancels
     "the banner records that the occurrences are 'mostly deliberate watchdog cancels', separating a chosen stop from a failure"]}
   ["hygiene/exempt-the-in-use" 3]
   {:operation :propose-census-exemption-over-fix
    :capability :wm/pattern-apply
    :consumes :deliberate-cancel-attribution
    :produces :exemption-proposal-on-the-table
    :completion-check
    [:exempt-expected-behaviour-proposal-stated
     "the banner proposes 'census-exempting operator cancellation as expected behaviour rather than fixing anything', so the class stays in use while its exemption is decided"]}
   ["buffer-cleaner/observe-categories" 4]
   {:operation :mark-regenerated-census-block-start
    :capability :wm/pattern-apply
    :consumes :census-script-output
    :produces :machine-owned-block-bounded
    :completion-check
    [:census-begin-marker-present
     "the '<!-- census:begin -->' marker opens the machine-regenerated block, bounding what humans may edit"]}
   ["apparatus/new-failure-class-is-a-design-defect" 5]
   {:operation :state-class-field-in-census-block
    :capability :wm/pattern-apply
    :consumes :terminal-code-value-from-ledger
    :produces :class-field-naming-operator-cancelled
    :completion-check
    [:class-field-matches-ledger-terminal-code
     "the census block states '- **class**: `operator-cancelled`', tying the ticket to the ledger's terminal-code"]}
   ["buffer-cleaner/observe-categories" 6]
   {:operation :record-observed-multiplicity
    :capability :wm/pattern-apply
    :consumes :ledger-count-in-window
    :produces :multiplicity-4-in-census-block
    :completion-check
    [:multiplicity-field-states-4
     "the census block states '- **multiplicity**: 4', an observed count rather than an impression"]}
   ["buffer-cleaner/observe-categories" 7]
   {:operation :record-observation-window
    :capability :wm/pattern-apply
    :consumes :ledger-timestamps
    :produces :bounded-window-in-census-block
    :completion-check
    [:window-field-names-both-endpoints
     "the census block states '- **window**: 2026-08-23T10:59 -> 2026-08-24T15:16', bounding the observation in time"]}
   ["agent/pause-is-not-failure" 8]
   {:operation :record-most-frequent-agent-pair
    :capability :wm/pattern-apply
    :consumes :grouped-terminal-codes-by-agent
    :produces :dominant-pair-named-in-census-block
    :completion-check
    [:most-often-field-names-source-and-sender
     "the census block states '- **most often**: `codex-10` <- `countdown-control`', naming who was cancelled by whom before any blame is assigned"]}
   ["apparatus/new-failure-class-is-a-design-defect" 9]
   {:operation :state-current-pipeline-stage
    :capability :wm/pattern-apply
    :consumes :class-response-state
    :produces :stage-believe-recorded-in-census-block
    :completion-check
    [:stage-field-says-believe
     "the census block states '- **stage**: BELIEVE', an unresponded-to class rather than one already in SELECT"]}
   ["apparatus/recorded-failures-before-live-runs" 10]
   {:operation :state-census-provenance-source
    :capability :wm/pattern-apply
    :consumes :invoke-job-ledger-query
    :produces :provenance-field-naming-the-query
    :completion-check
    [:source-field-names-jobs-endpoint-and-field
     "the census block states '- **source**: `GET /api/alpha/invoke/jobs`, field `terminal-code`', so the count is reproducible from the recorded ledger"]}
   ["buffer-cleaner/observe-categories" 11]
   {:operation :mark-regenerated-census-block-end
    :capability :wm/pattern-apply
    :consumes :machine-owned-block
    :produces :human-section-separated-from-census
    :completion-check
    [:census-end-marker-present
     "the '<!-- census:end -->' marker closes the machine block, so everything below it is human-owned and never regenerated"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "f07a4fa80f27c47c28e8ec4c6f2fcd8d7a8bced439e401dcd78fedec468135ba")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
