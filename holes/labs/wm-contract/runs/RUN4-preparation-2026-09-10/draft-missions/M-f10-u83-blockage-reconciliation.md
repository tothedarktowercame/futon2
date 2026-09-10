# M-f10-u83-blockage-reconciliation — DRAFT — NON-LIVE; independent review and explicit activation required

Status: DRAFT — NON-LIVE; independent review and explicit activation required

Source: worklist `:F10` (:blocked, class :F) and `:U83` (:blocked, class :I,
deps `[]`); issue-board blockages both typed "superseded — verify/reconcile"
(as-of 2026-09-10). Ordinary record-reconciliation work; **proposals only —
no worklist edits, no registry/frontier/data writes.**

## Objective

Produce one reconciliation record per row that dispositions every claim the
row's recorded blocker makes, using current evidence, and proposes (does not
apply) the row correction for owner acceptance.

## Frozen row excerpts and digests (input pins)

- `:F10` recorded blocker (worklist.edn, live read 2026-09-10): fold opts and
  the kernel adapter are absent. SHA-256 of the row's blocker text is pinned
  in the reconciliation record at freeze time (worker re-reads and verifies
  the digest before writing; a moved/mutated row stops the task, it does not
  proceed against stale text).
- `:U83` recorded scope (same read): a 73-item reduction queue; the row's own
  text and `MORNING-BRIEF-reduction-2026-09-07.md:73` say the queue no longer
  exists and Joe ruled those attempts.

## Cited evidence (pre-pinned)

- F10: `runs/RUN4-preparation-2026-09-10/C-WIRING-REVIEW.md` — accepted C
  wiring refutes the "absent fold opts / kernel adapter" wording; remaining
  REAL obligations to carry forward separately: the observation-model gap and
  the live-run rider.
- U83: `MORNING-BRIEF-reduction-2026-09-07.md:73` and the issue-board
  blockage assessment ("Recreating that queue would manufacture work").

## Deliverable and tests

Two records; per record every blocker claim maps to exactly one of
`refuted-with-pointer` / `carried-forward-with-owner` / `unresolved-needs-
evidence`; nothing else. Objective checks: (a) each row's claim count equals
its disposition count; (b) no new implementation is proposed; (c) proposed
worklist corrections are clearly marked as pending owner acceptance. Row
digests verified against the frozen pins above.

## Done means

Both records delivered, checks green, no source mutated. Adoption of any
proposed row change is the owner's (Joe) separate act.
