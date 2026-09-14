# Independent review acceptance — r10-caller discovery

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commit 94bb4126
(TN-r10-caller-discovery-2026-09-14.md, discovery only, no source
changes). Verdict: ACCEPTED.

Checked against source (not the note's own claims):

- Ordering hazard confirmed at coordination_ledger.clj:92-135: the
  boundary validates the commission, calls dispatch-fn SYNCHRONOUSLY,
  and only then appends evidence — :r10/recording-failed refuses after
  the dispatch has run. Confirmed click! (runner_service.clj:458-483)
  mints its own click id, guards single-flight with a process-local
  CAS only, and starts a daemon thread before returning — the
  irreversible side effect precedes any durable write. Wrapping click!
  directly could therefore refuse at the HTTP boundary after a click
  had started, and a retry could mint a second click. The TN's
  conclusion (wiring is not currently possible as a caller-only edit)
  follows.
- Pin spot-checks: coordination_ledger.clj, runner_service.clj, and
  transport/http.clj sha256s recomputed — all match the pin table.
- Refusal-control coverage gap confirmed: coordination_ledger_test.clj
  has no :r10/invalid-commission or recording-failure test (grep
  empty), matching §4.
- "No installed crontab" confirmed (crontab -l: none for joe).
- The default-store fallback (or evidence-store estore/!store) at the
  append site matches prerequisite 5's warning: a caller that omits
  the configured durable backend silently records into the default
  atom — production evidence must pin the store explicitly.

Consequence adopted into the ledger: the DAG gains a discovered
hidden-prerequisite node (r10-commission-contract) ahead of any wiring
parcel — the second time in one day discovery-first routing prevented
a wiring packet from shipping a boundary that could misreport dispatch
(cf. TN-row14-live-wiring-blocker). Commission genesis should follow
the existing independently-configured-authority pattern (server-owned,
sha-pinned source the request cannot select), per the standing
principle; a Joe ruling is needed only if that pattern proves not to
fit.
