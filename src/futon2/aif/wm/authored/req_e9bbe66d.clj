(ns futon2.aif.wm.authored.req-e9bbe66d
  "Authored operational clauses for held executable cascade
  3eed3a8cbd50ed22534bd9b11361b94ce0366e2c187220713bf345c3d5e5a0dd
  (phase :compile, requisition
  e9bbe66d102d255ec869b50bdeed0a16db6076f6c7bf9c460d0eca00b4c9a6ef).

  Resolutions for the seven :operational-clause-owed holes of the
  T-fail-invoke-interrupted selected-target reading (ticket
  futon3c/holes/tickets/T-fail-invoke-interrupted.md).  Each decision
  is an operational clause attributed to the reading's occurrence
  squares over that ticket: the operation names what the occurrence
  enacts in the census text, completion-check names the observable
  condition in that text, and :consumes/:produces describe the
  transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["invariant-coherence/bounded-disposition" 0]
   {:operation :record-class-and-multiplicity-in-ticket-title
    :capability :wm/pattern-apply
    :consumes :named-failure-class-with-occurrence-count
    :produces :disposition-record-entry-for-interruption-class
    :completion-check
    [:title-carries-class-key-and-multiplicity
     "ticket title 'T-fail-invoke-interrupted -- invoke-interrupted (1x)' names the class key and its multiplicity, so the census artifact carries a disposition entry rather than an anonymous failure"]}
   ["invariant-coherence/bounded-disposition" 1]
   {:operation :hold-class-at-recorded-awaiting-decision-disposition
    :capability :wm/pattern-apply
    :consumes :triage-dated-status-line-with-occurrence-count
    :produces :explicit-still-open-disposition-subject-to-age-bound
    :completion-check
    [:status-line-records-still-open-with-date-and-count
     "status line 'STILL-OPEN, rare. 4 occurrences, latest 08-31. Decide whether interruption is a defect or an expected terminal class.' records the open disposition, its triage date (2026-09-01) and the population evidence the age/count bound acts on"]}
   ["coordination/bounded-execution" 2]
   {:operation :decide-interruption-defect-or-typed-terminal-class
    :capability :wm/pattern-apply
    :consumes :open-disposition-question-from-status-line
    :produces :non-default-disposition-for-interruption-class
    :completion-check
    [:ticket-answers-defect-vs-expected-terminal-class
     "the standing ask 'Decide whether interruption is a defect or an expected terminal class.' is answered one way or the other, closing the STILL-OPEN marker"]}
   ["coordination/artifact-registration" 3]
   {:operation :register-machine-readable-class-key
    :capability :wm/pattern-apply
    :consumes :census-block-class-line
    :produces :stable-class-key-joinable-across-census-and-tickets
    :completion-check
    [:census-block-carries-backticked-class-key
     "census block line '- **class**: `invoke-interrupted`' carries the backticked class key that later gates and the occurrence count join on"]}
   ["coordination/artifact-registration" 4]
   {:operation :attribute-interruption-to-named-agent-pair
    :capability :wm/pattern-apply
    :consumes :most-often-line-with-sender-and-receiver
    :produces :provenance-bearing-invoke-job-record
    :completion-check
    [:agent-pair-named-in-most-often-line
     "census line '- **most often**: `codex-12` <- `codex-15`' names the interrupted and invoking agents so the failure record has provenance"]}
   ["coordination/bounded-execution" 5]
   {:operation :locate-interruption-at-pipeline-stage
    :capability :wm/pattern-apply
    :consumes :terminal-code-observation-of-interrupted-invoke
    :produces :stage-attributed-execution-budget-reading
    :completion-check
    [:stage-line-records-believe
     "census line '- **stage**: BELIEVE' places the interrupted execution at the BELIEVE stage, which is where the budget decision about defect-vs-terminal applies"]}
   ["coordination/artifact-registration" 6]
   {:operation :cite-requeryable-evidence-field
    :capability :wm/pattern-apply
    :consumes :census-source-line-with-endpoint-and-field
    :produces :registered-evidence-source-checkable-by-later-gates
    :completion-check
    [:source-line-names-endpoint-and-field
     "census line '- **source**: `GET /api/alpha/invoke/jobs`, field `terminal-code`' names the endpoint and field any reviewer can re-read to check the occurrence count"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "3eed3a8cbd50ed22534bd9b11361b94ce0366e2c187220713bf345c3d5e5a0dd")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
