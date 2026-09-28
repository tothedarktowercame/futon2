# E-kimi-task-125 — FAMILY-decision: the decision producers become one test namespace (M-warrant-limit)

**Requisition:** in-progress — dispatched 2026-09-28T20:26:15Z to kimi-1 as invoke-1790627175126-26219-b8cf2f3f

Clocked in by claude-8 for kimi-1 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# FAMILY-DECISION — the 19 decision producers become one test namespace

From claude-8 (owner, M-warrant-limit). Bell claude-8 back with a summary and commit shas.

## Why
Mission: `futon3c/holes/missions/M-warrant-limit.md`. Joe's rule: a change that makes more than 10 test warrants stale triggers a refactor. The 126 wire tests that ran product code now read committed records, and 60 producer tests assert those records against the product. Each producer is its own test namespace with its own warrant, so an edit to decision code makes up to 19 producer warrants stale. Measured 2026-09-28 20:30Z from the dependency records: 2,209 product definitions have more than 10 dependent tests; with the producers gathered into six family namespaces the figure is 745, and the rest comes from tests this packet does not touch.

A warrant is per test namespace. So the change is: the 19 producers stop being test namespaces of their own, and one family namespace runs them all.

## Change
1. Each producer below is renamed from `…_test.clj` to the same name without `_test` (namespace likewise, with `git mv`), and stays otherwise as it is: same code, same `deftest`, same record. It is now a support namespace.
2. One new test namespace `futon3c.diagramprover.wm-wire-producer-family-decision-test` (`test/futon3c/diagramprover/wm_wire_producer_family_decision_test.clj`) requires all of them and has one `deftest` per producer, named after the producer's record stem, which runs that producer's test var with `(clojure.test/test-vars [#'<producer-ns>/<its-deftest>])` or an equivalent that reports each assertion once. A failure must still name the record field path, and must say which producer it came from.
3. Anything that refers to a producer's old namespace name is updated: search both test trees and `/home/joe/code/storage/test-registry/` is NOT to be edited (reports are history).
4. The readers are not touched. The records are not touched; no record is rewritten or renamed.

## Producers in this family
- `test/futon3c/diagramprover/wm_wire_producer_ask_out_live_census_g19_test.clj` (record stem `ask-out-live-census-g19`)
- `test/futon3c/diagramprover/wm_wire_producer_c2_measured_live_records_read_test.clj` (record stem `c2-measured-live-records-read`)
- `test/futon3c/diagramprover/wm_wire_producer_construction_decision_test.clj` (record stem `construction-decision`)
- `test/futon3c/diagramprover/wm_wire_producer_construction_family_test.clj` (record stem `construction-family`)
- `test/futon3c/diagramprover/wm_wire_producer_construction_kernel_test.clj` (record stem `construction-kernel`)
- `test/futon3c/diagramprover/wm_wire_producer_construction_live_digest_test.clj` (record stem `construction-live-digest`)
- `test/futon3c/diagramprover/wm_wire_producer_publication_locators_observe_test.clj` (record stem `publication-locators-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_rates_observe_g28_test.clj` (record stem `rates-observe-g28`)
- `test/futon3c/diagramprover/wm_wire_producer_rates_observe_g29_test.clj` (record stem `rates-observe-g29`)
- `test/futon3c/diagramprover/wm_wire_producer_rates_observe_g30_test.clj` (record stem `rates-observe-g30`)
- `test/futon3c/diagramprover/wm_wire_producer_rates_observe_test.clj` (record stem `rates-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_rates_products_measured_product_test.clj` (record stem `rates-products-measured-product`)
- `test/futon3c/diagramprover/wm_wire_producer_rates_products_measured_record_test.clj` (record stem `rates-products-measured-record`)
- `test/futon3c/diagramprover/wm_wire_producer_selection_out_observe_test.clj` (record stem `selection-out-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_selection_out_refusal_test.clj` (record stem `selection-out-refusal`)
- `test/futon3c/diagramprover/wm_wire_producer_temperature_observe_test.clj` (record stem `temperature-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_token_input_observe_test.clj` (record stem `token-input-observe`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_construction_assemble_one_r4_kernel_cascade_spec_tes_test.clj` (record stem `wm-wire-construction-assemble-one-r4-kernel-cascade-spec-tes`)
- `test/futon3c/diagramprover/wm_wire_producer_wm_wire_r9_selection_law_decision_per_policy_argmax_test_lit_test.clj` (record stem `wm-wire-r9-selection-law-decision-per-policy-argmax-test-lit`)

## Acceptance
- `env -u GIT_DIR -u GIT_WORK_TREE clojure -M:test -n futon3c.diagramprover.wm-wire-producer-family-decision-test` green; the number of assertions equals the sum over the producers run singly before the change (run each once before you start and record its count). One JVM at a time.
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
- Report: `/home/joe/code/storage/test-registry/family-decision/REPORT.md`: assertion counts before and after per producer, the bad cases, run time of the family test, anything left unmerged. Mark each claim Ran or Read.
