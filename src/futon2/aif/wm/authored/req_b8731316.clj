(ns futon2.aif.wm.authored.req-b8731316
  "Authored operational clauses for held executable cascade
  f07a4fa80f27c47c28e8ec4c6f2fcd8d7a8bced439e401dcd78fedec468135ba
  (phase :compile, requisition
  b873131665de580b3ced5d24a42e723ef78c3f9a2e004cc09e55c75a6e7a67cf).

  Resolutions for the twelve :operational-clause-owed holes of the
  T-fail-operator-cancelled selected-target reading (ticket
  futon3c/holes/tickets/T-fail-operator-cancelled.md).  Each decision
  is an operational clause attributed to the reading's occurrence
  squares over that ticket: the operation names what the occurrence
  enacts in the census text, completion-check names the observable
  condition in that text, and :consumes/:produces describe the
  transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["apparatus/new-failure-class-is-a-design-defect" 0]
   {:operation :name-failure-class-with-multiplicity-in-ticket-title
    :capability :wm/pattern-apply
    :consumes :named-failure-class-with-occurrence-count
    :produces :census-visible-class-entry-for-operator-cancelled
    :completion-check
    [:title-carries-class-name-and-count
     "ticket title 'T-fail-operator-cancelled -- operator-cancelled (4x)' names the class and its multiplicity, so the census artifact carries an addressable entry rather than an anonymous failure"]}
   ["apparatus/new-failure-class-is-a-design-defect" 1]
   {:operation :mark-class-as-reclassify-candidate-not-fix
    :capability :wm/pattern-apply
    :consumes :triage-dated-status-line-with-review-count
    :produces :explicit-reclassification-question-subject-to-census-evidence
    :completion-check
    [:status-line-carries-reclassify-candidate
     "status line '**Status (triaged 2026-09-01): RECLASSIFY CANDIDATE.** 19 at review, mostly deliberate watchdog cancels.' opens the taxonomy question with its triage date and population evidence instead of accepting the class"]}
   ["agent/pause-is-not-failure" 2]
   {:operation :attribute-cancels-to-deliberate-watchdog-action
    :capability :wm/pattern-apply
    :consumes :review-evidence-of-watchdog-origin
    :produces :operator-cancel-classified-as-expected-operation
    :completion-check
    [:status-line-names-deliberate-watchdog-cancels
     "status line 'mostly deliberate watchdog cancels' attributes the cancels to deliberate watchdog action, which is the expected-operation reading the proposal depends on"]}
   ["hygiene/exempt-the-in-use" 3]
   {:operation :census-exempt-operator-cancellation-as-expected-behaviour
    :capability :wm/pattern-apply
    :consumes :reclassification-question-from-status-line
    :produces :structural-fact-exemption-in-census-counts
    :completion-check
    [:proposal-sentence-exempts-rather-than-fixes
     "sentence 'Propose census-exempting operator cancellation as expected behaviour rather than fixing anything.' grants the exemption by structural fact before any scoring, not by fixing the cancel"]}
   ["buffer-cleaner/observe-categories" 4]
   {:operation :fence-machine-extracted-census-block-start
    :capability :wm/pattern-apply
    :consumes :census-facts-preceding-interpretation
    :produces :delimited-observation-block-of-structural-kind-only
    :completion-check
    [:census-begin-marker-fences-the-block
     "HTML comment '<!-- census:begin -->' opens a fence whose fields carry bare key/value facts, keeping collection policy out of the record"]}
   ["apparatus/new-failure-class-is-a-design-defect" 5]
   {:operation :register-machine-readable-class-key
    :capability :wm/pattern-apply
    :consumes :census-block-class-line
    :produces :stable-class-key-joinable-across-census-and-tickets
    :completion-check
    [:census-block-carries-backticked-class-key
     "census line '- **class**: `operator-cancelled`' carries the backticked class key that later gates and the occurrence count join on"]}
   ["buffer-cleaner/observe-categories" 6]
   {:operation :record-bare-multiplicity-without-threshold
    :capability :wm/pattern-apply
    :consumes :counted-occurrences-in-window
    :produces :policy-free-count-feed-for-downstream-decisions
    :completion-check
    [:multiplicity-line-is-bare-count
     "census line '- **multiplicity**: 4' records the count as a bare structural fact with no staleness threshold or verdict baked in"]}
   ["buffer-cleaner/observe-categories" 7]
   {:operation :state-observation-window-without-scoring
    :capability :wm/pattern-apply
    :consumes :timestamp-range-of-census-observation
    :produces :bounded-re-derivable-observation-scope
    :completion-check
    [:window-line-is-bare-timestamp-range
     "census line '- **window**: 2026-08-23T10:59 -> 2026-08-24T15:16' bounds what the census saw without attaching any staleness verdict"]}
   ["agent/pause-is-not-failure" 8]
   {:operation :attribute-cancels-to-named-agent-control-pair
    :capability :wm/pattern-apply
    :consumes :most-often-line-with-agent-and-control
    :produces :provenance-bearing-invoke-job-record-for-cancel
    :completion-check
    [:agent-pair-named-in-most-often-line
     "census line '- **most often**: `codex-10` <- `countdown-control`' names the cancelled agent and the cancelling control so the cancel record has provenance as a control event"]}
   ["apparatus/new-failure-class-is-a-design-defect" 9]
   {:operation :locate-class-at-pipeline-stage
    :capability :wm/pattern-apply
    :consumes :terminal-code-observation-of-cancelled-invoke
    :produces :stage-attributed-class-entry-for-design-question
    :completion-check
    [:stage-line-records-believe
     "census line '- **stage**: BELIEVE' places the cancellation class at the BELIEVE stage, which is where the one-layer-down design question of the reclassification applies"]}
   ["apparatus/recorded-failures-before-live-runs" 10]
   {:operation :cite-requeryable-evidence-field
    :capability :wm/pattern-apply
    :consumes :census-source-line-with-endpoint-and-field
    :produces :registered-evidence-source-checkable-by-later-gates
    :completion-check
    [:source-line-names-endpoint-and-field
     "census line '- **source**: `GET /api/alpha/invoke/jobs`, field `terminal-code`' names the endpoint and field any reviewer can re-read to check the census without spending a live run"]}
   ["buffer-cleaner/observe-categories" 11]
   {:operation :fence-machine-extracted-census-block-end
    :capability :wm/pattern-apply
    :consumes :complete-bounded-observation-block
    :produces :explicit-provenance-boundary-between-facts-and-prose
    :completion-check
    [:census-end-marker-closes-the-block
     "HTML comment '<!-- census:end -->' terminates the observation block so downstream consumers know exactly which bytes are machine-extracted"]}})

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
