# R28 bounded witness search — selected-target-reading failed redecision (live)

Owner: zai-6. Date: 2026-10-31. Scope per P2 dispatch and `reconciliation/R28-producer-scope-judgment-2026-10-31.md`:
search retained run records for a LIVE failed **selected-target-reading** redecision whose run record
retains the actual reading (no false `not-required` fallback). Occurrence-square-redecision (39a845a3)
is explicitly out of scope. No new field requirement, no forced failure, no click.

## Search performed (single bounded pass)

Scripts: `r28search.clj`, `r28probe.clj`, `r28probe2.clj`, `r28ctx.clj` (Babashka, read-only) under
`/home/joe/notes/wm-next/criteria-completeness/run001/p2-matrices/`.

Population: all `tick-run-record-2026-1*.edn` in `data/wm-runs/` containing the phase name
`selected-target-reading-redecision` (24 records, 2026-10-16 → 2026-10-31), plus targeted reads of
`selected-target-reading` status/reason/restarts in each.

## Findings

1. **Every record whose redecision phase ran resolved to one of three shapes, none a post-repair failed
   selected-target-reading redecision that retains the reading:**
   - `:published` (succeeded; e.g. 3415b671 — validation `:valid`, cascade-count 1; also ccaa1478,
     f016ed51, 9e2103c1, e0b9aab6, ff4b4d5c, e5f08d91). Success does not exercise failure-retention.
   - `:pending` with `:reason :batch-limit` (8be8ff03, f444287e, 39eb74f5, 8c8ee35f, fd5d1d5f) — a
     different producer scope: multi-record field readings with `:records`/`:frontier`/`:deadline-ms`,
     `:published true`. Not the chosen-task single-target reading failure chain.
   - `:not-applicable` with `:reason :selected-target-reading-not-required` (46f8cac7 — the original
     regression fixture with the false not-required fallback; 9c3a318d, c9d132d8, f805ca4b — the
     selection-route producer, correctly not-required per the producer-scope judgment).
   - No record shows `:restarts [:retry]` or any failure marker on the selected-target-reading producer
     itself; the only in-record retry (`39a845a3`) is occurrence-square, out of scope.
2. **The open repair obligation for exactly this defect exists and is pinned:** inside 3415's repair
   proposals, `repair-occ-e62431354b9285c3a249e7331b8582475b1dc426123a35a0432c156ba813844f`,
   `failure-kind :selected-target-reading-redecision-failed`, `failure-stage
   :selected-target-reading-redecision`, opened `2026-10-26T18:23:17.687471872Z`, status `:open`,
   closure-observation `:unavailable (:unversioned-resolution-has-no-admitted-locator)`.
   Backtrace: `data/wm-repair-obligations/findings/repair-occ-e6243135….edn`
   SHA256 `828297514f01f52d06ada36b83fb21efabb361d97ad07a307711290dfe7c353e`. This is the historical
   (pre-repair, 46f8cac7-era) failure — a regression fixture, not a live success of the repair.
3. **B1 drift observation (incidental):** admitted field counts 544 (3415) → 545 (f805ca4b) on the same
   Giraffe manifest; recorded in the f805ca4b matrix, no violation asserted.

## Verdict

**Live witness owed.** No retained run record exhibits a failed selected-target-reading redecision that
retained the actual reading under the repaired behavior. The honest closure route is the open repair
obligation `repair-occ-e6243135` (its discharge contract requires a distinct production-shaped successor),
not a new field and not an intentional forced failure. B2-D4-R28 stays OPEN; no new requirement proposed.
This changes no tracker status and preserves the B1/E3 closures and all historical records as written.
