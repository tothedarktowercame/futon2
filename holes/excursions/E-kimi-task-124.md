# E-kimi-task-124 — FAMILY-flight: the flight producers become one test namespace (M-warrant-limit)

**Requisition:** completed — 2026-09-28T20:37:35Z, job invoke-1790627172163-26218-e71cc995, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition header and git commit both record the kimi job completed with state done; spot-check of task-98 found its deliverable (ready-delta-g) in src. _(WM status classification by zai-1, high confidence; not yet confirmed by the author.)_

Clocked in by claude-8 for kimi-2 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# FAMILY-FLIGHT — the 24 flight producers become one test namespace

From claude-8 (owner, M-warrant-limit). Bell claude-8 back with a summary and commit shas.

## Why
Mission: `futon3c/holes/missions/M-warrant-limit.md`. Joe's rule: a change that makes more than 10 test warrants stale triggers a refactor. The 126 wire tests that ran product code now read committed records, and 60 producer tests assert those records against the product. Each producer is its own test namespace with its own warrant, so an edit to flight code makes up to 24 producer warrants stale. Measured 2026-09-28 20:30Z from the dependency records: 2,209 product definitions have more than 10 dependent tests; with the producers gathered into six family namespaces the figure is 745, and the rest comes from tests this packet does not touch.

A warrant is per test namespace. So the change is: the 24 producers stop being test namespaces of their own, and one family namespace runs them all.

## Change
1. Each producer below is renamed from `…_test.clj` to the same name without `_test` (namespace likewise, with `git mv`), and stays otherwise as it is: same code, same `deftest`, same record. It is now a support namespace.
2. One new test namespace `futon3c.diagramprover.wm-wire-producer-family-flight-test` (`test/futon3c/diagramprover/wm_wire_producer_family_flight_test.clj`) requires all of them and has one `deftest` per producer, named after the producer's record stem, which runs that producer's test var with `(clojure.test/test-vars [#'<producer-ns>/<its-deftest>])` or an equivalent that reports each assertion once. A failure must still name the record field path, and must say which producer it came from.
3. Anything that refers to a producer's old namespace name is updated: search both test trees and `/home/joe/code/storage/test-registry/` is NOT to be edited (reports are history).
4. The readers are not touched. The records are not touched; no record is rewritten or renamed.

## Producers in this family
- `test/futon3c/diagramprover/wm_wire_producer_ask_out_live_census_g32_test.clj` (record stem `ask-out-live-census-g32`)
- `test/futon3c/diagramprover/wm_wire_producer_ask_out_live_census_test.clj` (record stem `ask-out-live-census`)
- `test/futon3c/diagramprover/wm_wire_producer_ask_out_step_test.clj` (record stem `ask-out-step`)
- `test/futon3c/diagramprover/wm_wire_producer_c2_chosen_precedence_test.clj` (record stem `c2-chosen-precedence`)
- `test/futon3c/diagramprover/wm_wire_producer_fold_out_decision_test.clj` (record stem `fold-out-decision`)
- `test/futon3c/diagramprover/wm_wire_producer_fold_out_simple_test.clj` (record stem `fold-out-simple`)
- `test/futon3c/diagramprover/wm_wire_producer_measured_live_kind_pair_test.clj` (record stem `measured-live-kind-pair`)
- `test/futon3c/diagramprover/wm_wire_producer_measured_step_observe_test.clj` (record stem `measured-step-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_measured_tick_observe_test.clj` (record stem `measured-tick-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_publication_enact_observe_test.clj` (record stem `publication-enact-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_publication_increment_observe_test.clj` (record stem `publication-increment-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_publication_publication_observe_test.clj` (record stem `publication-publication-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_publication_r0_test_observe_test.clj` (record stem `publication-r0-test-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_small_observe_test.clj` (record stem `small-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_target_observe_g49_test.clj` (record stem `target-observe-g49`)
- `test/futon3c/diagramprover/wm_wire_producer_target_observe_test.clj` (record stem `target-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_temporal_courier_publication_paths_test.clj` (record stem `temporal-courier-publication-paths`)
- `test/futon3c/diagramprover/wm_wire_producer_temporal_courier_test.clj` (record stem `temporal-courier`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_flight_click_flight_cast_test_cast_test_literal_fixt_test.clj` (record stem `wm-wire-flight-click-flight-cast-test-cast-test-literal-fixt`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_flight_click_flight_record_click_cast_test_literal_f_test.clj` (record stem `wm-wire-flight-click-flight-record-click-cast-test-literal-f`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_flight_run_flight_driver_summary_needs_test_literal__test.clj` (record stem `wm-wire-flight-run-flight-driver-summary-needs-test-literal-`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_flight_run_flight_driver_summary_readings_test_liter_test.clj` (record stem `wm-wire-flight-run-flight-driver-summary-readings-test-liter`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_r10_publication_observed_test_literal_fixture_test.clj` (record stem `wm-wire-r10-publication-observed-test-literal-fixture`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_r1_outer_cascade_flight_entry_chosen_target_test_lit_test.clj` (record stem `wm-wire-r1-outer-cascade-flight-entry-chosen-target-test-lit`)

## Acceptance
- `env -u GIT_DIR -u GIT_WORK_TREE clojure -M:test -n futon3c.diagramprover.wm-wire-producer-family-flight-test` green; the number of assertions equals the sum over the producers run singly before the change (run each once before you start and record its count). One JVM at a time.
- Bad case: with one product function rebound (`with-redefs`) in a scratch test, the family test fails, names the field path and names the producer.
- Bad case: one producer's record changed in a scratch copy makes that producer's deftest fail and leaves the others passing.
- `futon3c.diagramprover.wm-wire-ledger-test` still green with 184 witnessed hermetically and 10 verified; it rewrites `holes/labs/M-wm-wiring/wm-wire-ledger.edn`: restore that file with `git checkout -- <path>` afterwards and do not commit it.
- No file named `wm_wire_producer_*_test.clj` remains for this family except the family file.

## Rules
- One commit, by explicit path (`git mv` plus the new file). Never amend, stash or `git add -A`. `git config user.name` must print `Joseph Corneli`.
- Other families are being merged at the same time by other agents; touch only the files listed here and your family file.
- No edits under `src/` in either repo, to the readers, to `wm_wire_producer_record.clj`, or to the ledger test.
- Do not reload anything into the running JVM, run the rerun worker, or register warrants.
- Gates: clj-kondo on changed Clojure files; `futon4/dev/check-parens.el` (its entry point is now `arxana-check-parens--check-parens`).
- If a producer cannot be run from the family namespace without changing its code, STOP for that producer only: leave it as a test namespace, list it in the report with the reason, and merge the rest.
- Report: `/home/joe/code/storage/test-registry/family-flight/REPORT.md`: assertion counts before and after per producer, the bad cases, run time of the family test, anything left unmerged. Mark each claim Ran or Read.
