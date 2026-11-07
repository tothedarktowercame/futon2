(ns futon2.aif.wm.authored.req-ddd3106b
  "Authored operational clauses for held executable cascade
  6f09a7b1b948f218079335dea8657e7b2b8528a589f0ef1f22a3c4ab69fbe31a
  (phase :compile, requisition
  ddd3106b33e793063770f7efcb191ebb3e7866b4847b3cfe00682c9db97cafdf).

  Resolutions for the sixteen :operational-clause-owed holes of the
  T-arxana-direct-reads reading (target
  futon4/holes/tickets/T-arxana-direct-reads.md).  Each decision is an
  operational clause attributed to the reading's occurrence squares over
  that ticket: the operation names what the occurrence enacts in the
  ticket text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["hygiene/observe-the-authority" 0]
   {:operation :name-live-store-as-reading-authority
    :capability :wm/pattern-apply
    :consumes :ticket-title-naming-end-state
    :produces :authority-named-as-futon1b-live-store
    :completion-check
    [:title-names-live-store-not-exports
     "the ticket title reads 'Arxana browses futon1b hyperedges live, not via exported files', naming the authority the capability reads"]}
   ["hygiene/observe-the-authority" 1]
   {:operation :mark-exporter-as-stand-in
    :capability :wm/pattern-apply
    :consumes :joe-framing-of-exporter
    :produces :stopgap-classification-recorded
    :completion-check
    [:opening-records-stopgap-framing
     "the opening records Joe's framing that 'the exporter below is a stopgap', so the export is classified as a stand-in rather than the authority"]}
   ["hygiene/observe-the-authority" 2]
   {:operation :state-direct-read-goal
    :capability :wm/pattern-apply
    :consumes :quoted-joint-conception
    :produces :goal-of-direct-futon1b-read
    :completion-check
    [:conception-quoted-in-opening
     "the opening quotes 'we both had the conception in mind that Arxana would be able to read directly from futon1b', stating the goal the ticket exists to reach"]}
   ["process-coherence/status-refresh-before-work" 3]
   {:operation :record-blocked-banner-after-refresh
    :capability :wm/pattern-apply
    :consumes :live-probe-results-404
    :produces :re-derived-blocked-on-live-store-banner
    :completion-check
    [:banner-re-derived-2026-11-04-with-checks-cited
     "the status line reads 'open, blocked-on-live-store (re-derived 2026-11-04, enacting process-coherence/status-refresh-before-work' with the checks cited"]}
   ["process-coherence/status-refresh-before-work" 4]
   {:operation :name-prior-banner-as-superseded
    :capability :wm/pattern-apply
    :consumes :old-banner-text
    :produces :stale-state-not-silently-inherited
    :completion-check
    [:prior-banner-named-in-status-line
     "the refreshed status line names the prior banner ('prior banner \"open, parked / browsing unblocked\"') so the stale state is not silently inherited"]}
   ["process-coherence/status-refresh-before-work" 5]
   {:operation :declare-banner-stale-against-check
    :capability :wm/pattern-apply
    :consumes :fresh-probe-observations
    :produces :staleness-verdict-with-evidence
    :completion-check
    [:staleness-verdict-backed-by-probes
     "the banner says the prior text 'was stale' and the same breath records the 404 probes that witnessed the mismatch"]}
   ["hygiene/settle-with-meters" 6]
   {:operation :emit-raw-probe-meter
    :capability :wm/pattern-apply
    :consumes :probe-responses-from-futon1b-7073
    :produces :404-meter-in-status-record
    :completion-check
    [:raw-404-with-body-quoted-per-route
     "the check records 'returns 404 (\"No context found for request\")' on each named route (`/`, `/hyperedges?type=lean/doc-source`, `/hyperedge`)"]}
   ["hygiene/observe-the-authority" 7]
   {:operation :demote-cached-claim-to-unverifiable
    :capability :wm/pattern-apply
    :consumes :cached-claim-client-can-fetch
    :produces :claim-marked-not-currently-verifiable-live
    :completion-check
    [:claim-refuted-by-live-read-in-text
     "the ticket records that '\"the client can already fetch them\" is not currently verifiable live' against the 404 probes"]}
   ["process-coherence/status-refresh-before-work" 8]
   {:operation :surface-stale-default-dependency
    :capability :wm/pattern-apply
    :consumes :docstring-port-default
    :produces :item-3-openness-established
    :completion-check
    [:stale-default-recorded-with-listener-check
     "the check records that '`futon4-base-url`'s docstring still names :7071, which has no listener (missing item 3 confirmed open)'"]}
   ["hygiene/settle-with-meters" 9]
   {:operation :emit-explicit-item-status
    :capability :wm/pattern-apply
    :consumes :listener-check-on-item-3
    :produces :item-3-confirmed-open-record
    :completion-check
    [:item-status-measured-not-assumed
     "the ticket marks 'missing item 3 confirmed open' from the missing listener, not an assumed or defaulted closure"]}
   ["hygiene/settle-with-meters" 10]
   {:operation :split-partial-fix-verdict
    :capability :wm/pattern-apply
    :consumes :accept-header-code-change
    :produces :partly-addressed-in-code-verdict
    :completion-check
    [:code-half-and-unknown-half-separated
     "the check records item 4 as 'partly addressed in code, interactive url.el behavior unverified', separating what was measured from what was not"]}
   ["hygiene/settle-with-meters" 11]
   {:operation :emit-explicit-unavailable-value
    :capability :wm/pattern-apply
    :consumes :unexercised-interactive-url-el-path
    :produces :unverified-marker-in-check-record
    :completion-check
    [:unverified-boundary-stated
     "the check states 'interactive url.el behavior unverified' explicitly, so the reader knows the boundary of the check"]}
   ["hygiene/route-to-who-can-act" 12]
   {:operation :route-route-restore-finding
    :capability :wm/pattern-apply
    :consumes :broken-hyperedge-serving-finding-with-evidence
    :produces :next-move-restore-or-relocate
    :completion-check
    [:next-move-names-concrete-act-on-futon1b
     "the ticket's 'Next move: restore or relocate the hyperedge serving route on futon1b' routes the probe-backed finding to whoever can act on futon1b"]}
   ["hygiene/route-to-who-can-act" 13]
   {:operation :route-stale-default-fix
    :capability :wm/pattern-apply
    :consumes :stale-base-url-default-finding
    :produces :next-move-fix-stale-default
    :completion-check
    [:fix-named-with-evidence-attached
     "the next move continues 'and fix the stale base-URL default', carrying the no-listener evidence so the fixer need not re-diagnose"]}
   ["hygiene/receipt-then-gate" 14]
   {:operation :receipt-the-check-record
    :capability :wm/pattern-apply
    :consumes :banner-change-claim
    :produces :dated-edn-check-record-in-futon2
    :completion-check
    [:full-check-record-cited-by-path
     "the ticket cites 'futon2 data/wm-d-task-enactment/T-arxana-direct-reads/status-refresh-2026-11-04.edn' as the full check record behind the banner"]}
   ["hygiene/receipt-then-gate" 15]
   {:operation :pin-receipt-to-commit
    :capability :wm/pattern-apply
    :consumes :check-record-file
    :produces :reproducible-commit-pinned-receipt
    :completion-check
    [:commit-pinned-alongside-record
     "the record is pinned '(commit fbe2d376)' so the refresh is reproducible against an exact state of futon2"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "6f09a7b1b948f218079335dea8657e7b2b8528a589f0ef1f22a3c4ab69fbe31a")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
