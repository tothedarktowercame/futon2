# Run brief — decisions since (none recorded) (occasion run-2026-10-c1293ae5)

### Settled since the previous occasion
- **D-run-brief-1** — The run brief's decisions half is produced from a durable typed ledger (futon2.aif.wm.run-brief), not from a conversation buffer.
  basis: futon2/src/futon2/aif/wm/run_brief.clj; ledger holes/missions/M-run-produces-its-own-brief/run-decisions.edn; generated brief run-brief.md; tests test/futon2/aif/wm/run_brief_test.clj
- **D-run-brief-2** — Occasions and decision entries are typed records with ids, statuses and bases; invalid ledger entries are typed errors, never silent drops.
  basis: validate-occasion / validate-decision / validate-ledger in futon2.aif.wm.run-brief, pinned by run_brief_test.clj including the exact bad-case entry

### Newly needing an operator call
- **D-run-brief-3** — Qualified build-state numbers (established / stale / declared-unestablishable with bases) still need a producer reading the run's own gauges; this parcel built the decisions half only.
  basis: mission checklist line 9 remains unticked in holes/missions/M-run-produces-its-own-brief.md; no emitter exists yet for the numbers half

### Still outstanding
- **D-run-brief-4** — Lane handoff/output recording and a named operator surface presenting decisions and build state together remain open.
  basis: mission checklist lines 10-11 remain unticked; no lane-handoff record or operator command exists yet
