# WM click runner: data stores on disk

Reviewed P11 trace update (`5874be666` + `89e689716`): selected early-terminal
trace rows may carry `:trace/terminal` and existing `:run/id`. Publication is
deduplicated within one runner invocation, not claimed cross-process exactly-once.
Run record retains present/failed/skipped/absent publication receipt; returned
path-only injections are labelled unverified, production record returns separately
identified. No serving-store observation has been made after landing.

## Reviewed update — 2026-10-19, codex-1

P13: the run record retains Q2/Q8 carriers, including typed absence. The adjacent
requirements sidecar is generated later by recomputation, not by the historical
Lean-artifact directory. See [source-pinned producer map](p13-certificate-producers.md).

P12 requirements sidecar (`5ce3db21c`): `<run-record>.requirements.edn` holds
computed Q verdicts/counts independently of HTTP publication. Sibling temporary
files are replaced atomically and cleaned on ordinary write/replace failure;
the previous sidecar survives those failures. A pending receipt may remain when
the publication result cannot be persisted. This is atomic replacement, not a
claim of fsync-backed power-loss durability. No production store probe performed.

Manifest reader update (P03, `3c4b9da4a`, reviewed worker `d1c2717c5`): reader reports declared/readable/verified/excluded counts, missing/drifted counts and exclusion reason frequencies. Here `:verified` counts usable retained learning rows, not every hash-matching file. Original manifest pins are preserved; changed bytes are not repinned. Parsed verified records travel with dataset rows so provider projection does not reread mutable paths. Constructed three-record regression passed; no live store mutation or serving observation. Original inventory below remains historical.

- futon2 commit read: `1de600e2c` (HEAD at write time; it was `f76041cc7` at the start of the inspection, so HEAD moved while I worked. Working tree otherwise clean except untracked `holes/labs/...`.)
- futon3c commit not recorded; one uncommitted change in `futon3c/src/futon3c/wm/ordinary_click_budget.clj` matters below.
- Inspected: 2026-10-19 08:13 UTC (`date -u`). All mtimes are UTC from `find -printf %T@`. "files" counts regular files (`find -type f`), recursive.
- Method: `grep -rn` of each directory name in `src/ scripts/ checks/` (and futon3c `src/` for the bindings), then reading the named function. file:line values are from lines I read. Where I only saw a grep hit and not the function body, the cell says "grep only".
- Scope: 35 `data/wm-*` entries (34 directories plus the single file `wm-full-loop-phases.edn.log`), `data/wm-full-loop-machinery-NN` cohort dirs counted as 96 (not tabulated), `resources/wm/`, `futon3c/data/wm-click-run-bindings/`, plus one store outside the requested list that the target-reading refusal points to: `/home/joe/code/storage/wm-target-readings/` (`target_reading_registry.clj:15`).

Store count in the table: 38 rows (35 data entries + resources/wm + bindings + wm-target-readings).

## Table

Paths are relative to `/home/joe/code/futon2/` unless absolute. "newest" is the newest file's mtime. "largest" is in the notes column.

| store path | written by (function, file:line) | read by (function, file:line) | pinned/verified on read? | files | newest (UTC) | one-line state of newest record | notes |
|---|---|---|---|---|---|---|---|
| `/home/joe/code/storage/wm-target-readings/targets/<sha(target)>/<excerpt-digest>.edn` | `registry/publish!` target_reading_registry.clj:135; called from `selected_target_reading.clj:493` (same-click repair for the one selected target) and `occurrence_square_authoring.clj:148` | `current-reading` target_reading_registry.clj:181; `family_selection.clj:170` (via `excerpt-digest` :167); `cascade_decision.clj:1010` (`reading-root`) | yes: record is "current" only if its `:excerpt-digest` equals the digest of the source excerpt now; else typed absence `:stale-target-reading` / `:no-current-target-reading` (:188-194). Publish refuses a non-64-hex digest or request digest mismatch (:145-152) | 9 | 2026-10-18 22:38:50 | 9 target dirs, one record each (2026-10-16: 2, 10-17: 3, 10-18: 4); `analysis-digest` stored | Open frontier in the newest run is 780 tasks (requirements.edn Q1/Q2 "NR / 780 open tasks"); 9 readings. Largest file 31,561 B. Not under futon2/data. |
| `data/wm-ordinary-clicks/consumption.jsonl` (+ futon3c code) | `consume!` futon3c/src/futon3c/wm/ordinary_click_budget.clj:173, `refund!` :212 | `availability` ordinary_click_budget.clj:146 (called from futon2 `full_loop_runtime.clj:234-237` via requiring-resolve); `scripts/ordinary_click_budget.py:9` (only knows grant 1, allocation 5: stale) | availability snapshots under a file lock and records sha256 of the ledger bytes in `:ledger-source` | 1 | 2026-10-19 07:37:22 | 157 lines: 152 issues, 5 refunds. Last issue `wm-click-185ed2e7...` by codex-1 at 07:37:22 | 39,991 B. Count and allocation detail in "Stale or empty". |
| `data/wm-runs/` | `full_loop_runner.clj:1047` (`tick-run-record-<run-id>.edn`, dir at :138); report-card generator writes `<run-id>/report-card.*` | `held_out_observations.clj:199`; `run_narrative.clj:44`; futon3c `runner_service.clj` `*run-record-dir*`; several scripts | binding checks `(:run/id record)` = run-id (`run_narrative.clj:46`); no sha on the record itself | 580 | 2026-10-19 07:40:09 | `tick-run-record-2026-10-19-d608312b...` : `:failure {:kind :abstained :stage :selection :error "War Machine selection produced no addressable action"}` | largest file 270,306,985 B (`tick-run-record-2026-10-10-c82068b4...`). |
| `/home/joe/code/futon3c/data/wm-click-run-bindings/` | `persist-click-run-binding!` futon3c runner_service.clj:561 (dir var :135-136); report card at :705 | futon2 `run_narrative.clj:52`; `existing-run-id-clicks` runner_service.clj:542 | `run_narrative.clj:54` throws if `:run-id-observation :value` differs from the run | 274 (203 `click-run-binding-*`, 71 `report-card-*`) | 2026-10-19 07:39:56 | `click-run-binding-wm-click-185ed2e7...`: `:outcome :abstained :binding-status :verified :run-record-status :present` | largest 802 B. Fresh: both of today's clicks (04b2fa01, 185ed2e7) are bound and abstained. |
| `data/wm-trace/` | `trace/write-trace!` trace.clj:814 (called full_loop_runner.clj:5222-5223); lock `wm_run_lock.clj:59`; `.lane-futility-index.edn` via `lane-futility/append-indexed-trace!` (trace.clj:842) | `read-trace` trace.clj:858, `read-all-traces` :902, `read-history-strict` :953; `run_narrative.clj:49` | content only; `run_narrative.clj` requires a unique row for the run id | 79 (76 daily files) | 2026-10-19 03:58:09 | newest daily file `wm-trace-2026-10-19.edn` last written 03:58, before today's two later clicks; its last row has `:accumulation-bmr {:status :absent :reason :proposal-family-unavailable}` | largest 574,969,337 B (`wm-trace-2026-10-09.edn`). Latest run record says `:traceWritten false`. |
| `data/wm-full-loop-phases.edn.log` | `phase-log` spit-append full_loop_runner.clj:370-373 (path :137) | `run_narrative.clj:89`; `tripwire_calibration.clj:14`; `scripts/wm_click_timings.py:21`; `scripts/wm_click_repair_loop.py:510` | none (append-only log) | 1 | 2026-10-19 07:39:47 | one EDN map per line, latest phase event of click d608312b | 10,992,297 B |
| `data/wm-meta-injuries/` | `persist-ledger!` meta_injury_store.clj:100, `transact-classification!` :111, `ingest-record-file!` :136; `persist-transaction!` meta_self_heal_production.clj:16 | `load-durable-ledger` meta_injury_store.clj:53; `full_loop_runtime.clj:408` (`default-ledger-path`) | yes: `:source-pin {:path :sha256}` of ledger bytes (:66), entries carry `:record-pin` sha256 | 19 | 2026-10-19 07:39:47 | `ledger.edn`: 3 entries (ordinals 1-3), all `:observation-kind :ordinary-selection-abstention`, `:attempt-limit 3`. `HEAD.edn`: authority `:status :active`, pinned to run d608312b | 15 per-run `2026-10-19-*.edn` files, 14 of them today. Ledger is at its attempt limit (3 of 3). |
| `data/wm-ticket-queue/queue.edn` | `ticket_queue.clj` `enqueue!` :57 (called `finding_ticket.clj:122`) | `read-declaration` ticket_queue.clj:44; `decision_gate.clj:220`; `policy.clj:502`; `cascade_decision.clj:1353` | `validate!` ticket_queue.clj:20 (schema check); no sha | 2 (queue + lock) | 2026-10-19 07:39:41 | `:schema :wm/ticket-queue-v1 :placement :front`, 136 entries (`:ticket "T-repair-occ-..."`), first inserted 2026-09-22 | 18,770 B. Repo copy `resources/wm/ticket-queue.edn` is the versioned empty (ticket_queue.clj:39). |
| `data/wm-repair-obligations/` | `repair_obligation.clj` `record-review-failure!` :504, `record-system-failure!` :549, `record-implementation!` :1768, `resolve!` :1871, `write-publication-unreachable!` :1682; `finding_ticket.clj` `publish!` :95; `scripts/wm/write-publication-unreachable.clj:19` | `obligation-history` repair_obligation.clj:920, `open-obligations` :948; `scripts/wm_click.sh:363`; `full_loop_runner.clj:1962` | records carry content ids; "pinned" not traced beyond that (grep only) | 801 | 2026-10-19 07:39:41 | newest is under `occurrence-evidence/occ-2903c37f.../` | subdirs: discharge-operations dismissals findings implementations occurrence-evidence publication-unreachable resolutions ticket-links. Largest 19,412,725 B. 201 files in last 7 days. |
| `data/wm-morning-brief/` | `morning_brief.clj` `queue-item!` :169, `write-new!` :106, `queue-operator-gate!` :201 | `read-records` :122, `lifecycle-events` :240; `scripts/wm_status_report.py:542` | content id via `item-identity` :26 (sha256 :22) | 1022 | 2026-10-19 07:39:41 | `lifecycle/ea1-f0c651b4...--attempt-002/queued.edn` | subdirs: addenda archive items lifecycle reviews summaries. Largest 41,177,958 B. 198 files in last 7 days. |
| `data/wm-tripwires/` | `write-trip-report!` tripwire.clj:694 (root :29; calls :874, :920, :1126) | tripwire.clj:118 (slurp); `tripwire_calibration.clj:12,16,18`; `bulletin.clj:45`; `scripts/trip_census.py:25` | none seen | 345 | 2026-10-19 07:37:32 | `trips/trip-8689c34b...edn` | 18 files in last 7 days. Largest 180,905,402 B. Also `incidents/`, `coverage-v2.edn` read by calibration. |
| `data/wm-observation-labels/labels.edn` | `observation_label_store.clj` `publish!` :82 (via `full_loop_runner.clj:1116` `record-declaration-reads!`) | `snapshot` observation_label_store.clj:67 | envelope check `envelope?` :42; label keys include `@sha256:` of declaration | 2 (labels + lock) | 2026-10-19 07:39:47 | `:schema :wm/observation-labels-v1`, labels keyed by [class repo sha path ...] | 3,912,875 B |
| `data/wm-learning/run-record-manifest.edn` | `outer-learning-manifest/record!` outer_learning_manifest.clj:41 (called full_loop_runner.clj:8035) | `read-manifest` :14 (called `outer_learning_runtime.clj:26`) | shape only: `valid?` :22 checks schema and 64-hex sha per entry; file existence not checked there | 1 | 2026-10-19 07:39:47 | 82 entries; 24 of them point into `/tmp/click-scan-*` (path missing today), 58 exist | 14,751 B. Last two entries are today's runs 56988718 and d608312b. See Uncertain. |
| `data/wm-pattern-graph-diffs/` | `pattern-graph-diff/write-diff!` wm/pattern_graph_diff.clj:96; `publish!` call at full_loop_runner.clj:5829 (dir default full_loop_runtime.clj:39) | none found in futon2 (grep `pattern-graph-diff-dir`) | `base.sha256` recorded in the file | 12 | 2026-10-19 03:58:09 | run be38a3a9 target A-self-heal, `run_outcome incomplete`, `terminal carry-identity-unavailable`, `enactment not-verified` | 3,489 B largest |
| `data/wm-executable-cascades/` | `executable-cascade/save!` called cascade_decision.clj:1624 (root :1450) | `load-compiled` wm/cascade_continuation.clj:64, `load-record` (cascade_decision.clj:1625); `cascade_continuation.clj:129` | reloaded after save and re-validated (`certified?`, cascade_decision.clj:1626-1627); files named by digest | 14 | 2026-10-18 21:45:12 | `d005b2f8....edn` | largest 60,980 B. All 14 within 7 days. |
| `data/wm-execution-journals/` | `execution-journal` appends, cascade_decision.clj:1671 (root :1453) | `cascade_continuation.clj:110` (`execution-journal-root`) | files named by digest; `:journal-certificate` in the close record | 31 | 2026-10-18 21:28:03 | per-cascade dirs of step records | 478 B largest |
| `data/wm-cascade-continuation-closes/` | `production-close-adapter` wm/cascade_continuation.clj:68 (spit :93; root full_loop_runtime.clj:43-44, use :495) | same fn: compares existing file to sealed value (:89) | yes: refuses if existing file differs from the newly sealed close | 1 | 2026-10-18 21:06:30 | `:schema :wm/cascade-continuation-close-v1`, source run 2026-10-18-ccaa1478, `:journal-certificate {:status :certified ...}` | 1,187 B. Only one close ever written. |
| `data/wm-cascade-proposals/` | `generate-retrieval!` cascade_proposals.clj:50 (write :68); `record-supply` :130 | `read-bundle` :72, `load-proposals` :104, `load-supply` :115 | yes: evidence file sha256 compared (:93) | 1394 | 2026-09-21 03:41:57 | `8961ab24.../proposal.edn` | largest 16,200,143 B (an evidence `.source`). Nothing in 28 days. |
| `data/wm-habit/cascade-prior.edn` | `publish!` cascade_habit_store.clj:151 (via `record-policy!` :165, `record-selection!` :180, `record-reinforcement!` :187) | `read-snapshot` :41; `attach-habits` :62 | snapshot sha256 via `receipts/sha` (:50), kept in the decision | 2 | 2026-09-23 21:44:05 | `:version 1 :policy-grain :pattern-cascade :alpha 1.0 :counts {...}`, first key count 9 | 2,072 B. Nothing in 26 days. |
| `data/wm-pattern-feedback/events.edn` | `record!` cascade_feedback.clj:187 (path :19-21) | `read-snapshot` :176, `read-events*` :160 | `:selected-action-sha256 = :enacted-action-sha256` join check (:31-33) | 1 | 2026-10-15 17:13:49 | 14 lines; last: run 2026-10-15-b8f72b63, target M-futonzero-mvp, `:status :verified`, `:terminal-outcome :grounded-progress`, positive reinforcement of 4 patterns | 271,126 B. Four days old; every click since abstained. |
| `data/wm-interpretations/` | `publish!` want_interpretation.clj:492 -> `write-atomic!` :391 (store :363); `issue!` :403 | `read-published` :385, `merge-published` :548, `issued-request` :415 | yes: library source sha256 vs file bytes (:159, :208-212); reading sha compare in `merge-published` :583-589 | 22936 (22765 under `evidence/`, 146 `requests/`, 7 `flights/`, 1 `demo/`; 17 per-target `.edn`) | 2026-10-15 17:04:05 | 15 `M-*.edn` + 2 `T-repair-occ-*.edn`; newest `M-futonzero-mvp.edn` | largest 16,631,363 B (`M-daily-scan.edn`). 15 mission files cover 15 targets; open frontier is 780. Nothing in 4 days. |
| `data/wm-d-task-enactment/` | `write-claim!` d_predecessor_task_authority.clj:345 (via `produce!` :388, `complete!` :401); root :26 | `read-predecessor` :352; `verify` :334 | yes: `:selected-action-sha256 = :enacted-action-sha256` (:70-83) and declaration sha256 re-checked (:91) | 1474 | 2026-10-15 15:38:53 | `action-f4fe5730....edn` | largest 26,678 B. Nothing in 4 days. |
| `data/wm-learning-trials/attempts.edn` | `record!` learning_trial_ledger.clj:26 (root :14) | `read-trials` :118 | schema check (:24) | 1 | 2026-09-23 21:44:04 | ledger of attempts | 2,611,115 B. Nothing in 26 days. |
| `data/wm-on-prod-history/checkpoints/` | `checkpoint!` wm/on_prod_history.clj:28 (root :13) | `load-checkpoint` :57 | yes: `sealed`/`valid?` (:15, :18); existing record compared before write (:48) | 151 | 2026-10-19 00:20:25 | `checkpoints/2026-10-19-7a605d2e.../canary-6e655b47....edn` | largest 9,132,370 B. Also `*.log` resume smoke logs. |
| `data/wm-recorded-flights/` | `run-recorded!` wm/recorded_flight.clj:220 (ledger write :305); `scripts/wm_recorded_flight.clj` | same dir read back by recorded_flight.clj (`manifest-valid?` :31, `file-sha256` :23) | yes: manifest sealed, sha256 | 227 | 2026-10-18 03:01:57 | `click5-through-tick-evaluation/ledger/f8bbef5f....edn` | largest 128,516 B. Replay fixtures. |
| `data/wm-click-loop/` | `scripts/wm_click_repair_loop.py:63` (STATE; ledger.jsonl, debugger/*.md, click-NNN-*.log, STOP); `scripts/wm_run_alert.py:83` (alerts.log) | `wm_click_repair_loop.py` (reads previous debugger page, ledger tail) | none | 128 | 2026-10-19 07:40:09 | `alerts.log`; last ledger row (06:35:54) `stop: "done: 2 clicks spent, 0 carried a tick"` | 334 ledger lines. Largest 6,863,374 B. |
| `data/wm-rationale/` | `selection_rationale.clj:380` (spit) | none found (grep) | none | 7 | 2026-09-07 23:12:22 | `rationale-2026-09-07-36820e88...edn` | Nothing in 42 days. |
| `data/wm-certificates/` | none found | none found | n/a | 16 | 2026-10-01 19:46:32 | `runtime-g-2026-10-01-6a6135d6....lean` | `.lean` files named runtime-admission/aqc/construction/enactment-grounding/g. grep for those names in src/scripts/checks/resources: no hits. |
| `data/wm-evaluations/` | none found | none found | n/a | 1 | 2026-10-04 05:54:26 | `click-wm-click-abb61cd8-mechanical-v1.edn`: `:status :partial :scope :mechanical-u1-u2-and-cost`, evaluator codex-10, target M-self-documenting-stack | 9,732 B. Hand-written evaluation. |
| `data/wm-scoring-cache/global-rank.edn` | none found | none found (grep `global-rank`, `scoring-cache` across src/scripts/checks; one grep over all of ~/code `.clj/.py/.bb` was cut at 4 results, none relevant) | n/a | 1 | 2026-10-10 14:35:35 | global ranking dump | 2,200,303 B |
| `data/wm-workspace-gate/latest.edn` | `checks/wm_workspace_gate.clj` (path :16; receipt build :727; control write :120) | stdout of the gate parsed by `scripts/wm_status_report.py:417`, `scripts/run_readiness.py:114`; `checks/exit_code_scope_check.clj` | `:basis-start` lists per-repo git-sha, tree-sha, dirty? | 1 | 2026-10-15 13:59:45 | `:schema :wm-workspace-gate-run/v1`, futon2 git-sha `7c3641d7`, `:dirty? false` | 3,409 B. Reads `latest.edn` only via `:LAST-RECEIPT` line :683 (not traced further). |
| `data/wm-visibility/run-visibility.json` | `holes/labs/wm-contract/scripts/wm_visibility_projector.py` (grep hit on `run-visibility`; function not read) | none found in src/scripts | n/a | 61 | 2026-10-08 05:02:53 | `run_id wm-contract-machinery-57-v1`, `stage planned`, `result pending` | 841 B. Tracks the machinery cohort, which stopped. |
| `data/wm-full-loop/` | `full_loop_cohort.clj` `start-attempt!` :572, `append-checkpoint!` :651, `close-attempt!` :706 (root :26) | `ledger` full_loop_cohort.clj:757, `write-ledger!` :802; `learning_trial_ledger.clj:214-220`; `receipt_construction.clj:549` (all dirs starting `wm-full-loop`) | cohort contract (`resources/wm/cohort-contracts.edn`, full_loop_cohort.clj:124) | 635 | 2026-07-27 10:59:53 | `wm-outer-loop-46-v1/attempt-061/007-closed.edn` | largest 8,285,336 B. Nothing in 84 days. |
| `data/wm-full-loop-canary/` | `full_loop_cli.clj:52` (`canary-<...>`) | none found | n/a | 12 | 2026-07-15 21:00:33 | `self-heal-fire-pattern-retry-2026-07-15.edn` | largest 109,389 B. Nothing in 96 days. |
| `data/wm-full-loop-machinery-NN/` (96 dirs) | cohort machinery (`full_loop_cohort.clj`, root pattern `wm-full-loop*`) | `learning_trial_ledger.clj:214-220`, `receipt_construction.clj:549` | as wm-full-loop | not tabulated | not tabulated | n/a | Counted: `ls -d data/wm-full-loop-machinery-[0-9]* | wc -l` = 96. |
| `data/wm-step/` | none found. `bulletin.clj:325` only mentions `wm_step.sh` in a docstring | none found | n/a | 356 | 2026-09-05 19:47:05 | `w1/world-drift-016-...flagoff-vs-018-...flagoff.edn` | largest 81,110,863 B (`w1/sandbox/wm-trace/wm-trace-2026-09-01.edn`). Experiment sandbox. |
| `data/wm-quarantine/` | none in src/scripts (tests reference it: `full_loop_cohort_test.clj` and two others) | none found | n/a | 2 | 2026-09-21 00:35:17 | `machinery-67-attempt-001/QUARANTINE.md` | largest 3,641,049 B |
| `data/wm-trace-quarantine/` | none found | none found | n/a | 3 | 2026-09-29 00:28:01 | `README.edn`; `wm-trace-2026-09-28.test-leak.edn` | largest 31,960 B |
| `data/wm-trace-escrow-witness/` | `scripts/gate_2g_provenance.clj:17` (witness-dir) | same script | n/a | 2 | 2026-07-08 21:49:38 | `wm-trace-2026-07-08.edn` | 939 B largest. Nothing in 103 days. |
| `resources/wm/` | hand/agent-edited, committed. `outer-learning-authority.edn` last commit af0fa4729 (2026-10-19 07:29) | one reader per file: `approved-algorithms.edn` meta_field_observation.clj:18; `cohort-contracts.edn` full_loop_cohort.clj:124; `handoff-sites.edn` handoff_site_index.clj:7; `observation-contract.edn` observation_checks.clj:3; `outer-learning-authority.edn` outer_learning_runtime.clj:12 (`load-shadow` :21); `run-ending-classification-v1.edn` run_ending_classification.clj:11; `repair-evaluator-admissions.edn` repair_evaluators.clj:83; `learning-trial-prior.edn` learning_trial.clj:12; `attempt-learning-contract-v2.edn` attempt_learning.clj:13; `reading-precision-beta/wm-readiness-2026-10-17-campaign.edn` full_loop_runtime.clj:42 | mixed: `outer-learning-authority` is read as bytes and its `:run-records` pins are conflict-checked against the dynamic manifest (outer_learning_runtime.clj:25-33); others not traced | 35 | 2026-10-19 07:31:31 | `outer-learning-authority.edn`: `:mode :live`, `:outer-policy {:gamma 1.0 ...}`, `:prior {:concentration 1 :authority :operator-provisional}` | largest 240,417 B (`focus/commit-facets-v1.json`). Newest in 7 days: only `outer-learning-authority.edn`. Other files dated 2026-09-21 to 2026-10-17. |

## Stale or empty

Stores with nothing written in the last 7 days (newest mtime before 2026-10-12 08:13) or empty:

| store | newest | days since | note |
|---|---|---|---|
| `data/wm-trace-escrow-witness` | 2026-07-08 | 103 | witness for gate 2g |
| `data/wm-full-loop-canary` | 2026-07-15 | 96 | |
| `data/wm-full-loop` | 2026-07-27 | 84 | cohort data root; the 96 machinery dirs are separate |
| `data/wm-step` | 2026-09-05 | 43 | experiment sandbox, no writer found |
| `data/wm-rationale` | 2026-09-07 | 42 | writer exists (`selection_rationale.clj:380`), no reader found |
| `data/wm-cascade-proposals` | 2026-09-21 | 28 | writer exists (cascade_proposals.clj:50) |
| `data/wm-quarantine` | 2026-09-21 | 28 | |
| `data/wm-learning-trials` | 2026-09-23 | 26 | writer `record!` learning_trial_ledger.clj:26 |
| `data/wm-habit` | 2026-09-23 | 26 | writer `publish!` cascade_habit_store.clj:151; no click has chosen a cascade since |
| `data/wm-trace-quarantine` | 2026-09-29 | 20 | |
| `data/wm-certificates` | 2026-10-01 | 18 | no writer found |
| `data/wm-evaluations` | 2026-10-04 | 15 | no writer found |
| `data/wm-visibility` | 2026-10-08 | 11 | cohort stopped at machinery-57 |
| `data/wm-scoring-cache` | 2026-10-10 | 9 | no writer found |

Written within 7 days but not by recent clicks (newest 3 to 5 days old, matching "every click since 10-15 abstained"): `wm-pattern-feedback` (10-15 17:13), `wm-interpretations` (10-15 17:04), `wm-d-task-enactment` (10-15 15:38), `wm-workspace-gate` (10-15 13:59), `wm-executable-cascades` / `wm-execution-journals` / `wm-cascade-continuation-closes` (10-18 21:xx, from the 10-18 runs).

Empty stores: none among the tabulated ones (smallest is 1 file). `data/wm-click-loop/debugger` newest page for the latest click records `abstained`, see below.

### Target reading store: confirmed nearly empty

- `/home/joe/code/storage/wm-target-readings/` holds 9 records, written 2026-10-16 (2), 10-17 (3), 10-18 (4); newest 2026-10-18 22:38:50. `find ... -type f | wc -l` = 9, `ls targets | wc -l` = 9.
- The registry returns a reading only when its excerpt digest equals the current one (target_reading_registry.clj:181-194).
- The newest run's own requirements record (`data/wm-runs/tick-run-record-2026-10-19-d608312b...edn.requirements.edn`) shows "NR enumerated / 780 open tasks". The mission-interpretation store has 15 mission files (above). I did not find a per-target count of "no current reading" in the run record (grep for `no-current-target-reading` / `stale-target-reading` in the newest two run records: 0 hits each), so the figure "hundreds of eligible targets lacking a reading" is not confirmed from the files; what is confirmed is 9 readings against an open frontier of 780 tasks.
- Writers of readings: `selected_target_reading.clj:493` (the target the outer selector already chose) and `occurrence_square_authoring.clj:148`. A reading is therefore written for a target only after selection has chosen it. Both of today's clicks abstained at selection ("War Machine selection produced no addressable action", debugger page `wm-click-04b2fa01...md`), and the page says "UNCHANGED -- same refusal counts and the same named targets" as the previous abstention (`wm-click-3e373299...md`). Under that pairing no target reaches the reading writer. This is my reading of the call sites, not a traced run.

### Ordinary click budget: consumed, with a mismatch between the two copies

From `data/wm-ordinary-clicks/consumption.jsonl` (python count over the file) and `futon3c/src/futon3c/wm/ordinary_click_budget.clj`:

- 157 lines = 152 issue lines (an entry with `issued-at`) + 5 `"event":"refund"` lines (all dated 2026-10-03/04).
- Grants by authorization sha: d18e4f9c 5, 52f75d1d 5, fa49ed93 5, 1a51bc2e 5, 00e7f9d6 5, c24c903b 3, 2996eb6b 7, and the current authorization `326826ac` (renewal-7, `AUTH-ordinary-click-budget-renewal-7-2026-09-24.md`) 117 issue lines. Charged under the current authorization = 117 - 5 refunded = 112. `charged-click-ids` (ordinary_click_budget.clj:130-137) counts exactly that: issues with no `:event` minus refunds, for the current authorization map only.
- `allocated` (ordinary_click_budget.clj:43) is the literal at line 125. Committed value in HEAD of futon3c (commit cad071d39, 2026-10-19 05:20, "wm: allocate five commissioned clicks") is `111`. The working tree has an uncommitted edit changing it to `113` (`git diff HEAD` shows `-  111)` / `+  113)`).
- So with the committed value: consumed 112 against allocated 111, `:available` = max(0, 111 - 112) = 0, and `consume!` refuses when consumed >= allocated (:188). That agrees with "111 of 111 consumed" if the running JVM loaded 111 and counted before the last issue. With the working-tree value 113: 1 available.
- Which value the serving JVM holds is not visible from files (see Uncertain).
- `scripts/ordinary_click_budget.py:12,19` still knows only the first grant (sha d18e4f9c, allocation 5); its report is stale and not a source for the current state.

### Other stores of the same shape (absence or refusal as the newest record)

- `data/wm-meta-injuries/ledger.edn`: 3 of 3 entries, all `:ordinary-selection-abstention`, `:attempt-limit 3`. The self-heal ledger records only abstentions, and it is at its limit.
- `data/wm-click-loop/ledger.jsonl` last row (06:35:54): `stop: "done: 2 clicks spent, 0 carried a tick"`.
- `data/wm-runs` newest record `d608312b` (07:39): abstained at selection; `data/wm-trace/wm-trace-2026-10-19.edn` last written 03:58, so today's two latest clicks left no trace row (`:traceWritten false` in 56988718's record).
- `data/wm-pattern-graph-diffs` newest (03:58): `run_outcome incomplete`, `terminal carry-identity-unavailable`, `enactment not-verified`.
- `data/wm-pattern-feedback`, `wm-habit`, `wm-learning-trials`: last positive learning event 2026-10-15 (feedback) or earlier; learning stores are not growing while clicks abstain.
- `data/wm-learning/run-record-manifest.edn`: 24 of 82 entries point to `/tmp/click-scan-*` files that no longer exist (see Uncertain).
- `data/wm-cascade-continuation-closes`: one file in total; the cascade path has closed once.
- `data/wm-ticket-queue/queue.edn`: 136 queued repair tickets, first inserted 2026-09-22, file rewritten 07:39 today. This is a queue that fills, with no count of how many are discharged in this pass.

## Written by nothing in futon2

I grepped each directory name (`grep -rIn -F <name> src scripts checks`, also `resources`, and for certificates the file-name stems `runtime-aqc`, `runtime-admission`, `runtime-construction`). No writer found for:

- `data/wm-certificates/` (16 `.lean` files, last 2026-10-01): no code reference to the name or the stems. Probably produced by a Lean/mathlib4 process or by hand; not traced.
- `data/wm-evaluations/` (1 file): no code reference. Hand-written by codex-10 per its `:evaluator`.
- `data/wm-scoring-cache/global-rank.edn`: no code reference to the directory or `global-rank` in futon2.
- `data/wm-step/` (356 files): only a docstring mention at `bulletin.clj:325`. `wm_step.sh` is not in `scripts/` (`ls scripts | grep -i step` finds `live_loop_step.bb` only).
- `data/wm-quarantine/`, `data/wm-trace-quarantine/`: only test references (quarantine) or none.
- `data/wm-visibility/run-visibility.json`: only `holes/labs/wm-contract/scripts/wm_visibility_projector.py`, outside `src/` and `scripts/`.
- `data/wm-full-loop-canary/`: writer exists only at `full_loop_cli.clj:52` (a CLI path); no recent use.

Stores with a writer in futon2 and no reader found in futon2: `wm-rationale` (writer `selection_rationale.clj:380`), `wm-pattern-graph-diffs` (writer wm/pattern_graph_diff.clj:96), `wm-certificates` (neither).

Stores written outside futon2: `futon3c/data/wm-click-run-bindings/` (futon3c `runner_service.clj:561`) and the ledger `consumption.jsonl` (futon3c `ordinary_click_budget.clj:173`). futon2 reads both.

## Uncertain

- **Which allocation the serving JVM holds.** The futon3c file has HEAD value 111 and a working-tree value 113. If the JVM (Agency :7070) loaded the file before the edit, `consume!` refuses; after a reload from the working tree one click is available. I did not query the JVM (not allowed).
- **Whether the ledger count 112 vs "111 of 111" means one extra issue was admitted past the limit.** The ledger shows 112 charged under a 111 allocation. `consume!` checks `consumed >= allocated` (:188) under a lock, so the 112th issue at 07:37:22 should have been admitted only if allocated was at least 112 at that moment. That implies the allocation in force at 07:37 was above 111 (the working-tree 113 fits). I did not read the lines 173-210 body fully, so I did not confirm the lock/ordering.
- **Outer-learning manifest with 24 missing paths.** `read-manifest` (outer_learning_manifest.clj:14) does not check existence; `load-shadow` (outer_learning_runtime.clj:21) feeds all entries to `dataset/produce` inside a `try`. I did not read `dataset/produce` to see if a missing file is skipped, refused, or silently dropped. The 24 are `/tmp/click-scan-*` entries (`ls /tmp/click-scan-16313651870851502188`: no such file or directory).
- **"Hundreds of eligible targets lacking a reading".** Not confirmed from a per-target count in any file I read; see the reading-store section.
- **Pinning on read for `wm-repair-obligations`, `wm-tripwires`, `wm-morning-brief`, `wm-execution-journals`.** I saw the writer and reader function names but did not read their verification code; cells say "none seen" or "grep only".
- **`wm-workspace-gate/latest.edn` readers.** `wm_status_report.py` and `run_readiness.py` read gate stdout lines, not the file; whether anything reads `latest.edn` itself (`checks/wm_workspace_gate.clj:683` LAST-RECEIPT) I did not trace.
- **HEAD moved** from `f76041cc7` to `1de600e2c` during the inspection; line numbers were read at whichever commit was checked out when I read each file and I did not re-verify them after the move.
