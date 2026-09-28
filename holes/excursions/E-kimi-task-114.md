# E-kimi-task-114 — PRODUCER-9: one producer test and its wire-test readers (M-warrant-limit)

**Requisition:** in-progress — dispatched 2026-09-28T18:38:20Z to kimi-1 as invoke-1790620700283-26110-e1b70ab0

Clocked in by claude-8 for kimi-1 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# PRODUCER-9 — one producer and its 4 readers: flight/run!, inputs temporal-courier-publication-paths

From claude-8 (owner, M-warrant-limit). Bell claude-8 back with a summary and commit shas.

## Why
Mission: `futon3c/holes/missions/M-warrant-limit.md`. A change to product code must make at most 10 test warrants stale. 126 wire tests run a product operation to obtain the value they check, so each depends on everything that operation uses. The refactor: one producer test runs the operation and asserts its result equals a committed record; the wire tests read the record and load no product code.

## The worked example — read it first, and copy its shape
futon3c commits `5485129f` and `b672ac09` (PRODUCER-1, the temporal courier flight): `git show 5485129f`.
- record: `test/fixtures/wire-producers/temporal-courier@3e102b032c87.edn`
- producer test: `test/futon3c/diagramprover/wm_wire_producer_temporal_courier_test.clj`
- reader support (already exists, do not change it): `test/futon3c/diagramprover/wm_wire_producer_record.clj`
- a converted reader: `test/futon3c/diagramprover/wm_wire_temporal_finalize_temporal_envelope_temporal_cursor_test.clj`
- report and checklist: `/home/joe/code/storage/test-registry/producer-1/REPORT.md`

## This group
- operation: `flight/run!`
- inputs (the support calls the readers make now): `futon3c.diagramprover.wm-wire-temporal-courier-support/live-absent?|futon3c.diagramprover.wm-wire-temporal-courier-support/observe|futon3c.diagramprover.wm-wire-temporal-courier-support/publication-paths`
- record stem: `temporal-courier-publication-paths` -> `test/fixtures/wire-producers/temporal-courier-publication-paths@<first 12 hex of sha256 of its bytes>.edn`
- producer namespace: `futon3c.diagramprover.wm-wire-producer-temporal-courier-publication-paths-test`
- readers (4):
  - `futon3c.diagramprover.wm-wire-flight-run-flight-judge-opts-temporal-receipt-test`
  - `futon3c.diagramprover.wm-wire-r0-enact-step-flight-run-temporal-receipt-test`
  - `futon3c.diagramprover.wm-wire-temporal-finalize-temporal-envelope-temporal-receipt-test`
  - `futon3c.diagramprover.wm-wire-temporal-finalize-temporal-write-once-temporal-receipt-test`

## Checklist
1. Name the exact operation, literal inputs, readers, and every reader-checked field.
2. Run the existing support once; turn only values that differ from run to run (clock, uuid, temporary path) into stated relations, and list them under `:left-out` with the reason. A field a reader checks is never left out.
3. Write the record only when `WM_WIRE_PRODUCER_WRITE=1`; never overwrite a file.
4. Name the file with the first 12 hex digits of the SHA-256 of its exact bytes.
5. The producer compares every checked field path and names the path on failure.
6. Readers require only `clojure.test`, `futon3c.diagramprover.wm-wire` and `futon3c.diagramprover.wm-wire-producer-record`.
7. Keep each reader's namespace, deftest names, `wire` map (`:wire`, `:kind`, `:test`, `:second-layer`) and the meaning of its assertions: writer's value present, not a typed absence, equal to the reader's value.
8. Run producer and readers one JVM at a time: `env -u GIT_DIR -u GIT_WORK_TREE clojure -M:test -n <ns>`.
9. Bad cases: a changed value in a scratch copy of the record makes its reader fail naming the field; one product function rebound with `with-redefs` makes the producer fail naming the field.
10. For each reader show it reaches no product definition: `python3 scripts/warrant_reach.py record --namespace <ns> --closure <edn list of the files a fresh JVM loads for it> --output <file>`; PRODUCER-1's report says how the list was made.

## Do not
- edit anything under `src/` in either repo, any support file, `wm_wire_producer_record.clj`, the ledger test, or a test that is not in the list above;
- run `futon3c.diagramprover.wm-wire-ledger-test` (other groups are being converted at the same time; I run it once for the batch);
- reload anything into the running JVM, run the rerun worker, or register warrants;
- amend, stash or `git add -A`. Commit by explicit path. `git config user.name` must print `Joseph Corneli`.

## Stop
If a reader checks something the operation does not return or write (so there is nothing to record), or the group's tests do not all use the same inputs, STOP, remove your edits, and report which test and why. Do not widen the change.

## Gates and report
clj-kondo and `futon4/dev/check-parens.el` on changed Clojure files; all namespaces of the group green. Report in `/home/joe/code/storage/test-registry/producer-9/REPORT.md`: the record's fields, what was left out and why, reach per reader before and after, the bad cases, what you could not explain. Mark each claim Ran or Read.
