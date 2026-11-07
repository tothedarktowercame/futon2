(ns futon2.aif.wm.authored.req-bff2ee5f
  "Authored operational clauses for held executable cascade
  0c476d7c398165502e817c7e7128aeeb931f2e0e06640df70d5d45a128a8ab31
  (phase :compile, requisition
  bff2ee5f238e88318f9d7a56dd155981793b8c18f54537622c8eefa83fc66fd0).

  Resolutions for the twenty-two :operational-clause-owed holes of the
  T-arxana-direct-reads reading (target
  futon4/holes/tickets/T-arxana-direct-reads.md), including the
  2026-11-14 item-3 update paragraph.  Each decision is an operational
  clause attributed to the reading's occurrence squares over that
  ticket: the operation names what the occurrence enacts in the ticket
  text, completion-check names the observable condition in that text,
  and :consumes/:produces describe the transition.  Nothing here claims
  a square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/scope-before-action" 0]
   {:operation :state-ticket-scope-in-title
    :capability :wm/pattern-apply
    :consumes :ticket-title-naming-end-state
    :produces :scope-declared-live-reads-not-exports
    :completion-check
    [:title-names-live-store-not-exports
     "the ticket title reads 'Arxana browses futon1b hyperedges live, not via exported files', declaring in-bounds and out-of-bounds before any body work"]}
   ["agent/provisional-claims-ledger" 1]
   {:operation :mark-exporter-as-stand-in
    :capability :wm/pattern-apply
    :consumes :joe-framing-of-exporter
    :produces :stopgap-classification-recorded
    :completion-check
    [:opening-records-stopgap-framing
     "the opening records Joe's framing that 'the exporter below is a stopgap', so the export is classified as a stand-in rather than the accepted end state"]}
   ["agent/scope-before-action" 2]
   {:operation :state-direct-read-goal
    :capability :wm/pattern-apply
    :consumes :quoted-joint-conception
    :produces :goal-of-direct-futon1b-read
    :completion-check
    [:conception-quoted-in-opening
     "the opening quotes 'we both had the conception in mind that Arxana would be able to read directly from futon1b', stating the goal before any criteria are set"]}
   ["agent/trail-enables-return" 3]
   {:operation :pin-target-store-in-goal
    :capability :wm/pattern-apply
    :consumes :goal-sentence-naming-a-store
    :produces :futon1b-named-as-returnable-referent
    :completion-check
    [:store-named-in-goal
     "the goal names 'from futon1b' as the concrete target store, so later probes have a pinned referent to return to"]}
   ["process-coherence/status-refresh-before-work" 4]
   {:operation :record-blocked-banner-after-refresh
    :capability :wm/pattern-apply
    :consumes :live-probe-results-404
    :produces :re-derived-blocked-on-live-store-banner
    :completion-check
    [:banner-re-derived-2026-11-04-with-checks-cited
     "the status line reads 'open, blocked-on-live-store (re-derived 2026-11-04, enacting process-coherence/status-refresh-before-work' with the checks cited"]}
   ["process-coherence/status-refresh-before-work" 5]
   {:operation :cite-refresh-enactment-by-name
    :capability :wm/pattern-apply
    :consumes :status-line-rationale-slot
    :produces :refresh-method-made-auditable
    :completion-check
    [:pattern-id-named-in-status-line
     "the status line names 'process-coherence/status-refresh-before-work' as the rule enacted, making the re-derivation's provenance checkable"]}
   ["agent/state-is-hypothesis" 6]
   {:operation :declare-banner-stale-against-check
    :capability :wm/pattern-apply
    :consumes :fresh-probe-observations
    :produces :stale-banner-superseded-not-defended
    :completion-check
    [:prior-banner-named-stale
     "the status body records that the prior banner ('open, parked / browsing unblocked') 'was stale', updating the state hypothesis instead of defending it"]}
   ["agent/evidence-over-assertion" 7]
   {:operation :emit-raw-probe-meter
    :capability :wm/pattern-apply
    :consumes :probe-responses-from-futon1b-7073
    :produces :404-meter-in-status-record
    :completion-check
    [:raw-404-with-body-quoted-per-route
     "the check records 'returns 404 (\"No context found for request\")' on each named route (`/`, `/hyperedges?type=lean/doc-source`, `/hyperedge`)"]}
   ["agent/evidence-over-assertion" 8]
   {:operation :demote-cached-claim-to-unverifiable
    :capability :wm/pattern-apply
    :consumes :cached-claim-client-can-fetch
    :produces :claim-marked-not-currently-verifiable-live
    :completion-check
    [:claim-refuted-by-live-read-in-text
     "the ticket records that '\"the client can already fetch them\" is not currently verifiable live' against the 404 probes"]}
   ["process-coherence/stuck-means-signal" 9]
   {:operation :surface-stale-default-as-standing-signal
    :capability :wm/pattern-apply
    :consumes :docstring-port-default
    :produces :unchanged-defect-made-visible
    :completion-check
    [:stale-default-recorded-with-listener-check
     "the check records that '`futon4-base-url`'s docstring still names :7071, which has no listener (missing item 3 confirmed open)'"]}
   ["agent/provisional-claims-ledger" 10]
   {:operation :emit-explicit-item-status
    :capability :wm/pattern-apply
    :consumes :listener-check-on-item-3
    :produces :item-3-confirmed-open-record
    :completion-check
    [:item-status-measured-not-assumed
     "the ticket marks 'missing item 3 confirmed open' from the missing listener, not an assumed or defaulted closure"]}
   ["agent/provisional-claims-ledger" 11]
   {:operation :split-partial-fix-verdict
    :capability :wm/pattern-apply
    :consumes :accept-header-code-change
    :produces :partly-addressed-in-code-verdict
    :completion-check
    [:code-half-and-unknown-half-separated
     "the check records item 4 as 'partly addressed in code, interactive url.el behavior unverified', separating what was measured from what was not"]}
   ["agent/evidence-over-assertion" 12]
   {:operation :emit-explicit-unavailable-value
    :capability :wm/pattern-apply
    :consumes :unexercised-interactive-url-el-path
    :produces :unverified-marker-in-check-record
    :completion-check
    [:unverified-boundary-stated
     "the check states 'interactive url.el behavior unverified' explicitly, so the reader knows the boundary of the check"]}
   ["agent/sense-deliberate-act" 13]
   {:operation :commit-single-next-move-after-sensing
    :capability :wm/pattern-apply
    :consumes :broken-hyperedge-serving-finding-with-evidence
    :produces :next-move-restore-or-relocate
    :completion-check
    [:next-move-names-concrete-act-on-futon1b
     "the ticket's 'Next move: restore or relocate the hyperedge serving route on futon1b' commits one bounded action after the probe evidence"]}
   ["agent/scope-before-action" 14]
   {:operation :bound-second-half-of-next-move
    :capability :wm/pattern-apply
    :consumes :stale-base-url-default-finding
    :produces :next-move-fix-stale-default
    :completion-check
    [:fix-named-with-evidence-attached
     "the next move continues 'and fix the stale base-URL default', bounding the second fix to one artifact so the fixer need not re-diagnose"]}
   ["agent/trail-enables-return" 15]
   {:operation :receipt-the-check-record
    :capability :wm/pattern-apply
    :consumes :banner-change-claim
    :produces :dated-edn-check-record-in-futon2
    :completion-check
    [:full-check-record-cited-by-path
     "the ticket cites 'futon2 data/wm-d-task-enactment/T-arxana-direct-reads/status-refresh-2026-11-04.edn' as the full check record behind the banner"]}
   ["agent/trail-enables-return" 16]
   {:operation :pin-receipt-to-commit
    :capability :wm/pattern-apply
    :consumes :check-record-file
    :produces :reproducible-commit-pinned-receipt
    :completion-check
    [:commit-pinned-alongside-record
     "the record is pinned '(commit fbe2d376)' so the refresh is reproducible against an exact state of futon2"]}
   ["agent/provisional-claims-ledger" 17]
   {:operation :record-dated-attributed-progress-claim
    :capability :wm/pattern-apply
    :consumes :item-3-docstring-fix
    :produces :item-3-addressed-entry-with-remainder-open
    :completion-check
    [:progress-dated-and-attributed-not-closed
     "the update records 'Item 3 addressed 2026-11-14 (wm-author, full-loop attempt-001)' as progress, not completion"]}
   ["agent/handoff-preserves-context" 18]
   {:operation :update-documented-contract-for-successors
    :capability :wm/pattern-apply
    :consumes :stale-docstring-example-naming-7071
    :produces :docstring-example-naming-futon1b-7073
    :completion-check
    [:docstring-example-now-correct
     "the update records that 'the `futon4-base-url` docstring example now names futon1b :7073', so handoffs carry the right endpoint"]}
   ["agent/provisional-claims-ledger" 19]
   {:operation :enumerate-untouched-criteria
    :capability :wm/pattern-apply
    :consumes :item-3-addressed-entry
    :produces :four-criteria-still-open-record
    :completion-check
    [:remainder-explicit-not-absorbed
     "the update states 'the remaining four completion criteria are untouched and still open', preventing the one fix from inflating into done"]}
   ["agent/evidence-over-assertion" 20]
   {:operation :re-probe-endpoint-before-verdict
    :capability :wm/pattern-apply
    :consumes :prior-404-observation-dated-2026-11-04
    :produces :fresh-404-observation-dated-2026-11-14
    :completion-check
    [:still-404-asserted-from-fresh-probe
     "the update records that 'the :7073 `/hyperedges` route still returns 404' from a current re-probe, not a carry-forward of the old observation"]}
   ["agent/evidence-over-assertion" 21]
   {:operation :state-verdict-with-named-checks
    :capability :wm/pattern-apply
    :consumes :four-named-completion-checks
    :produces :explicit-not-met-verdict
    :completion-check
    [:verdict-names-checks-and-marks-not-met
     "the update ends 'the live-view, endpoint-open, planted-edge, and interactive-Accept checks remain **Not met.**', an anchored verdict rather than a softened summary"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "0c476d7c398165502e817c7e7128aeeb931f2e0e06640df70d5d45a128a8ab31")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
