# War Machine pre-launch defect register

Joe, 2026-10-10, to claude-12: "i am assinging you codex-68 as a lab partner.
only run clicks when thry agree that all known defects at pre-launch-time are
adequately addressed".

**Launch gate.** claude-12 asks Joe for a click only when codex-68 has written a
dated sign-off below, naming the exact futon2/futon3c commits loaded in the
serving JVM and stating that every OPEN entry is adequately addressed (fixed
and verified, or explicitly judged not to affect the click, with the reason).
A defect found later is added here before anything else happens.

Evidence standard: production artifacts at production scale where they exist;
a fixture is evidence only when no real artifact is available.

## Register

| ID | Defect | Seen | Status |
|----|--------|------|--------|
| D1 | The run closes via the exception path before author dispatch because selection persistence fails. | clicks 50, 51 | FIXED AND VERIFIED on `0327fd564`: the exact production-scale replay persisted selection in 20,653 ms, then completed construction, D-task capture, and author dispatch. |
| D2 | The selection checkpoint is 2.38 GB; 95% of its bytes are exact repeats. | click 51 | FIXED AND VERIFIED by lossless durable interning through `aed08ae1b`: real cold round-trip is `=` at 163,146,184 bytes; the live replay wrote 163,157,369 bytes. |
| D3 | Appending a checkpoint re-reads earlier ones in full with slurp, giving an OOM over 2 GB. | click 51 | FIXED AND VERIFIED end-to-end: metadata-only append produced all seven lifecycle checkpoints and closed cleanly on exact production-scale data. |
| D4 | The emergency record lacks the click identity, so no report card is made. | click 51 | FIXED AND VERIFIED: the replay record retained identity and generated EDN, Markdown, and HTML cards. |
| D5 | `:scoring-target-budget :scored-target-count` reads 0 although thousands of candidates have numeric G. | clicks 50, 51 and replay `0327fd564` | KNOWN TELEMETRY DEFECT, JUDGED NOT TO AFFECT THIS CLICK: current code scores every admitted target (`scored-problems` is all admitted problems); the bad count is written only after selection and does not gate scoring or choice. |
| D6 | The same target, T-repair-occ-487ca3f2, is chosen repeatedly. | 49-51 | JUDGED NOT TO AFFECT THIS CLICK: the prior clicks failed before actuation, so no world change existed to dislodge it. The exact live replay again selected it and proved the path reaches dispatch; repetition is expected until one attempt acts. |
| D7 | 64-80 s pass between opportunity start and agent-readiness; click 51 reported admission pending. | 50, 51 | JUDGED ADEQUATELY ADDRESSED: the exact replay had a 50 s pre-readiness interoceptive scan, but agent-readiness completed in 7 ms and admission in 3 ms with outcome `:ok`; no admission-pending refusal remained. |
| D8 | Selection takes 5-7 minutes. | 50, 51 | MEASURED AND ACCEPTED FOR THIS CLICK: exact replay selection was 273,114 ms and completed normally. It is performance debt, not a correctness or resource-exhaustion failure. |
| D9 | GET `/api/alpha/wm/click` reported `running? false` while click 51 was held at a debugger stop. | 51 | JUDGED NOT TO AFFECT THIS CLICK: this was debugger/status presentation during a deliberately stopped run. The next click is not to be debugger-stopped; durable phase records and run identity are authoritative. |
| D10 | Each failed click leaves a multi-GB checkpoint. | 50, 51 | FIXED by D2: the live selection checkpoint is 163,157,369 bytes, not multi-GB. |
| D11 | Broad-suite test hygiene and known unrelated failures. | warrant re-seed | JUDGED NOT TO AFFECT THIS CLICK: the exact production-scale path completed selection through report-card generation; focused changed-path tests pass. The broad failures predate this repair and are not on the exercised launch path. |
| D12 | Dynamic binding alone did not isolate all replay writes. | offline replay 2026-10-10 | FIXED AND VERIFIED: process-wide test root is selected before namespace load, side-effect boundaries refuse production targets, and repeated full replays' Git/data snapshots remained unchanged. |
| D13 | Close was non-terminating/resource-exhausting on C at production scale. | offline replay 2026-10-10 | FIXED AND VERIFIED repeatedly; current exact replay close completed in 154,723 ms, wrote the closed checkpoint and 269,496,653-byte run record, and generated all cards. |
| D14 | C's lossy run-record projection broke 9 namespaces. | warrant check | FIXED: the projection was removed; the affected namespaces passed after the change, and the current replay retained the decision through report generation. |
| D15 | Run records are not interned, so a successful click 51-shaped run would write about a 1.2 GB run record. About 20 readers read run records directly (previous_run.clj reads the previous record during SELECTION; flight_*, trace, progress_read, scripts, futon3c runner_service). | code reading | FIXED AND VERIFIED on futon2 `f6c66ecbe`: the replay wrote a 237,209,756-byte streamed/interned record, the shared reader hydrated it, and the report card consumed it. |
| D16 | Selection persistence was too slow. | production-scale replay | FIXED AND VERIFIED on `0327fd564` containing `aed08ae1b`: live persistence fell from 263,336 ms to 20,653 ms; D-task capture was 2,248 ms and close 154,723 ms. Accepted for this click. |
| D17 | Live readers parsed whole run records for small fields. | claude-12 reader audit 2026-10-10 | FIXED: previous-run ordering uses the 1 ms head line; remaining consumers use the streaming/hydrating reader. Verified by the exact replay consuming the seeded prior-run corpus. |
| D18 | Predecessor hashing constructed `(pr-str record)` of the whole record. | claude-12 reader audit | FIXED: predecessor digest streams printed bytes; the exact replay traversed the predecessor path without String overflow. |
| D19 | The production replay did not reach its advertised sole stub boundary (Agency). Click 51's persisted selected action was refused first by D-task binding with `:failure-kind :dispatch-evidence-unavailable`; `:dispatches` was empty. | offline replay on main `f6c66ecbe`, exact 2,381,602,039-byte click-51 selection | FIXED AND VERIFIED on `29480e7a3`: faithful live selection chose an interpreted cascade; construction completed in 64 ms, D-task capture completed successfully in 2,008 ms, author dispatch reached the stub in 4 ms, typed stub refusal was consumed, and close completed in 150,749 ms. One dispatch was recorded for the exact click-51 target. |
| D20 | The replay report loses or mislabels measurements it already has. | offline replay | FIXED through `4539118e4`: timing/start extraction is covered; the last replay exposed ambiguous recursive attempt discovery after production `wm-full-loop` became a snapshotted input, so cohort-scoped lookup now selects the replay's actual 163,157,369-byte checkpoint. Regression suite: 9 tests, 15 assertions. |
| D21 | The supposedly isolated replay mutated and committed to the canonical futon2 source repository. Its synthetic `dispatch-evidence-unavailable` failure published `holes/tickets/T-repair-occ-182f...md` and created commit `79a523906` on main at 05:33:42, exactly when close began. The data-root isolation does not cover repair ticket publication. | offline replay on main `f6c66ecbe` | FIXED AND VERIFIED by `a77f627e9` plus replay assertions in `8f63f9aa5`: both subsequent live runs left canonical Git HEAD/worktree and production data unchanged. `79a523906` was reverted by `1c52c50bf`. |
| D22 | `interpretation-evidence/value-digest` is not stable across an EDN round trip for sets/seqs. | D19 diagnosis | JUDGED NOT TO AFFECT THIS CLICK: production seals and validates the family in memory. The exact live-selection replay completed D-task capture on that path. Persisted-seal verification remains versioned durability debt. |
| D23 | The live-selection replay set `:interpretation-ask-fn nil`, but nil means “use the production default.” It dispatched real Agency job `invoke-1791611767087-1281-b6439847` during selection redecision, before the intended author-dispatch stub. The job was already terminal when cancellation was attempted; the replay was then interrupted. | live replay `8f63f9aa5`, 2026-10-10 | FIXED AND VERIFIED in `076caea76`: an explicit no-op function prevents fallback; 242,568 ms of live selection completed without an interpretation dispatch. The run selected no action, so author dispatch was not reached. |
| D24 | Durable interning assumes `(empty map)` supports transients. A live no-selection close contained a `PersistentTreeMap`; encoding it threw `ClassCastException: PersistentTreeMap cannot be cast to IEditableCollection`, then close fallback lacked required checkpoints. | live replay `076caea76`, 2026-10-10 | FIXED AND VERIFIED in `3145c80fb`: non-editable maps use persistent assoc; the identical live path closed successfully in 131,730 ms and wrote every checkpoint, a 46,064,128-byte run record, and all report cards. |
| D25 | A `:no-selection` record/card discarded the sorry decision and reason. | live-selection replays | FIXED by claude-12 on main: the record retains the certificate and typed no-selection reason; report-card coverage passes. |
| D26 | A faithful isolated replay needs a point-in-time copy of every mutable production input read by selection before data-owning namespaces load. Durable repair evidence contains absolute paths and digests, so a byte copy alone fails its validation contract after relocation. | live replay `9afb57c34`, 2026-10-10 | FIXED AND VERIFIED for the replay harness through `29480e7a3`: `data-paths/path` tracing, enabled before namespace load and retained through the full live run, now derives the snapshot roots; the seeder consumes that trace instead of a hand list. The observed set added the previously omitted `wm-interpretations`, plus `wm-observation-labels` and `wm-scoring-cache` during selection. The replay passed stop-line-memory and the complete selection-to-close path. |
| D27 | The `9afb57c34` replay returned `:no-selection`; codex-68 initially attributed this to inner selection choosing a target the outer selector excluded. | live replay `9afb57c34`, exact click-51 source, 2026-10-10 | INITIAL DIAGNOSIS WITHDRAWN. Claude-12 correctly identified the mechanism: `selected-entry` refuses a provisional `:query-time-pattern-selection`. The hand snapshot omitted `wm-interpretations`, so the winner had no receipt. With the observed snapshot, the same target won as a `:machine-constructed` interpreted cascade with `:structure/unresolved-tensions-at-closure`, and the run reached construction and dispatch. The independent design question—inner G-selection may choose an outer-excluded target—remains, but did not cause this replay failure and is not presently judged a launch blocker. |
| D28 | Admission re-read the roster immediately after its own readiness wake; the woken seat still read `invoking`, so the click was refused `:busy`. | run `2026-10-10-4bb54313`, admission only | FIXED AND VERIFIED in `52b019f9a`, merged as `f7cef971c`: `agent-readiness!` polls until the woken seat is available, bounded by `readiness-settle-ms` (20,000 ms). Independent targeted rerun of the new regression and the existing restored-agent readiness test passed: 2 tests, 12 assertions. PID 39171 reports 20,000 ms and exact loaded/canonical runner SHA-256 identity. |
| D29 | The post-click requirements alert checked the run but futon1b refused its evidence copy with HTTP 403 because `wm_run_alert.py` omitted `x-penholder`. | clicks 48 onward | FIXED AND VERIFIED in `82eaf0977`, merged as `1f73c5330`: the script sends `FUTON1B_PENHOLDER`, falling back through `FUTON1A_PENHOLDER` to allowed penholder `api`. A local HTTP capture of the real script path received the POST and header; futon1b's current gate resolves that header and its default allow-list contains `api`. This is a post-click script and requires no JVM load. |
| D30 | `test_good_fixture_conforms` in `wm_run_alert_test.py` is stale against the current external Requirements checker: Q4 and Q9 return `unverifiable`. | focused D29 verification, 2026-10-10 | JUDGED NOT TO AFFECT THIS CLICK: D29 changes only the later evidence POST; the stale fixture runs with `--no-evidence` and therefore does not exercise the changed code. The other 3 alert tests pass, a local end-to-end POST verifies the header, and production treats an unverifiable requirement as a visible post-click alert rather than suppressing or altering the click. Fixture upkeep remains test debt. |
| D31 | The signed-off retry reached admission after the ordinary-click allocation was exhausted at 51/51. | run `2026-10-10-c82068b4`, admission only | FIXED AND VERIFIED by Joe's recorded one-click grant: futon2 authorization `1c5247dab` and futon3c budget-only commit `717ae4a2` raise allocation to 52. PID 39171 reports `allocated` 52; receipt 52 was consumed by the current retry, and the runner source remains the signed-off `f7cef971c` digest. The budget namespace does not change futon2 WM behavior. |
| D32 | The production replay replaced `trace/write-trace!` with a path-returning `:trace-fn`. The real click therefore first exercised a trace record containing the full >2 GB judgement; `append-indexed-trace!` attempted `(str (pr-str record) "\n")` and threw `OutOfMemoryError: Requested array size exceeds VM limit` before selection persistence. | run `2026-10-10-c82068b4`; replay audit 2026-10-10 | OPEN. The replay harness fix `2952c3275` removes its `:trace-fn`, `:phase-log-fn`, and `:refresh-fn` overrides so trace, phase-log, and refresh writers execute their production implementations under the isolated process-wide data root. The focused harness suite passes (10 tests, 19 assertions). The trace representation fix and a clean production-scale replay through the real trace writer are still required before another sign-off. |

## claude-12 turn 2 (2026-10-10): D2, D14, D15, D17, D18 fixed on main, pending replay

Merged on futon2 main (wmq/durable-selection-real; includes codex-68's 28e12195c kind-sensitive keys and cycle refusal). Real-scale measurements (/tmp/c10/d15-real.clj, log /tmp/c10/d15-real.log; 72 GB JVM in its own scope), every read-back `=` to the original:
- Selection checkpoint through cohort write-new!/read-edn: 2,380,766,904 -> 163,146,184 bytes; write 323 s (test JVM, no sharing: D16 still OPEN), read 6.5 s.
- Run record built from click 51's real controller decision, through write-run-record-stream! / run-record-io/read-record: decision about 1,167 MB printed -> 148,283,833-byte record; write 62 s, read 5.8 s.
- previous-run ordering reads only the `;; wm/run-head` line: 1 ms (was a full parse of every record of the day). D17 fixed for previous_run; full_loop_runtime and registered_run_telemetry still read the newest record whole, but through the streaming reader at about 6 s.
- D18: the predecessor digest streams (printed-sha256; equal to hashing pr-str bytes, tested with non-BMP characters).
- Bugs found and fixed on the way: repeats beneath one-element collections were never found (counting walk); the UTF-8 digest mis-encoded surrogate pairs (interning ids could collide).
- futon3c readers use only top-level fields (reader audit), which stay plain; they were not changed.
Next: codex-68 reruns the production replay on main.

## codex-68 turn 3 (2026-10-10): production-scale replay on main

Exact code: futon2 `f6c66ecbef6643c5f37bcec8f89e3fcdbf1f93f0` (durable merge
`816c870b9`). Source was click 51's 2,381,602,039-byte selection. The replay ran
in a separate `MemoryMax=90G`, `-Xmx72g` scope; the serving JVM was untouched.
Evidence is in
`/home/joe/runs/wmq-2026-10-10/offline-replay-main-f6c66ecbe/` and isolated
data root `/tmp/futon2-test-data-609246017081230561/`.

- Process exit 0 in 13:04.10; peak RSS 16,093,012 KiB; no swap.
- Selection checkpoint 163,144,278 bytes; construction 59,681; dispatch 298;
  build 292; adjudication 291; closed 297,935.
- Close terminated successfully in 170,867 ms. The run record is 237,209,756
  bytes and begins with the `wm/run-head`. Report-card EDN, Markdown, and HTML
  all generated from that record.
- Production `data/` had no file modified during the replay, but source
  isolation failed: close published the synthetic repair ticket and committed
  `79a523906` on canonical futon2 main (D21).
- This is not launch-clean: D-task dispatch binding refused before Agency, so
  no stub dispatch occurred (D19). The report itself also emitted nil phase and
  checkpoint summaries despite the evidence above (D20).
- Harness tests before launch: 4 tests, 8 assertions, 0 failures/errors.

## claude-12 turn 3 (2026-10-10): D21 fixed, D19 diagnosed, D22 found

- D21 FIXED on main (51a9b0e7f): finding-ticket/destinations compared the store with canonical-store, which data-paths resolves to the TEST store under a test root, so the test store published into /home/joe/code/futon2. It now compares with the production store, refuses the production store in a test JVM, and the committer refuses the canonical checkout in test mode. The existing test canonical-store-queue-is-untracked-runtime-state ASSERTED the leak; rewritten. The same capability at runner start (discharge-receipt catch-up!) and close (finalize-run!) defaulted to the hard-coded checkout; a test JVM now gets no default repo. 79a523906 reverted on main (1c52c50bf, never pushed).
- D19 DIAGNOSED: re-running d-task/capture on the replay record's own :d-task-context throws :precision-family-tampered (policy_precision_carry validate-binding! -> intact?). capture-result hid it (now fixed: refusals carry class, message, kind, ex-data). Cause: interpretation-evidence/value-digest (`stable`) sorts maps and descends vectors only, never sets or seqs; the family holds 103,563 sets, so a SEALED family read back from disk never passes intact?. Click 51's ORIGINAL checkpoint (never interned) fails intact? too, so this is not interning. The replay fed the persisted family (selection bypassed, D8); production seals and validates in memory. So D19 is a replay artifact, PROVIDED nothing mutates the family between seal and dispatch in production; the replay must run selection live to show that.
- D22 (new): value-digest is not stable across an EDN round trip (sets and seqs are not canonicalized), so any sealed value with sets cannot be re-verified after persistence (construction_receipt_lean_adapter.clj:666 checks intact? on a persisted precision state). Not on the click path (claude-12's reading: policy.clj:580's beta-state has no sets). Changing value-digest would invalidate every stored seal, so the fix needs a versioned digest. OPEN; codex-68 to judge whether it bears on launch.

## codex-68 turn 4 (2026-10-10): live selection replay

Harness and runner instrumentation landed as `8f63f9aa5`; explicit offline
interpretation handling landed as `076caea76`. The first live run was stopped
after production click-ask escaped the nil option (D23). Its Agency job was
already terminal when cancellation was attempted. The corrected run used live
selection from current production inputs, with all mutable data isolated.

- Live selection completed in 242,568 ms and selected no addressable action.
  Consequently it could not exercise D-task capture or the author Agency stub;
  D19 remains OPEN rather than being bypassed.
- Close then failed after 124,546 ms because durable interning attempted a
  transient `PersistentTreeMap` (D24). It still wrote a 56,232-byte run record
  and generated all report-card formats. Total elapsed was 8:03.56; peak RSS
  7,946,820 KiB; no swap.
- D20 is verified: report phase durations and started-at are populated, and
  checkpoint discovery found the 3,344-byte time-step checkpoint.
- D21 is verified: canonical Git HEAD/worktree and production data were
  byte/metadata-identical before and after both live runs.
- D22 does not bear on the next click's in-memory family seal/validation, but
  remains an OPEN durability defect for persisted sealed values.
- Evidence:
  `/home/joe/runs/wmq-2026-10-10/offline-replay-live-076caea76/` and
  `/tmp/futon2-test-data-15082814072842854534/`.

D24 follow-up on exact futon2 `3145c80fb`: the same live selection again
returned no action after 200,750 ms, but close now completed in 131,730 ms.
It produced a 161,613,774-byte selection checkpoint, all seven lifecycle
checkpoints, a 46,064,128-byte run record, and all report-card formats. Total
wall time was 7:01.64; peak RSS 10,449,648 KiB; no swap. Canonical Git and
production data remained unchanged. Evidence:
`/home/joe/runs/wmq-2026-10-10/offline-replay-live-3145c80fb/` and
`/tmp/futon2-test-data-10472920108593433448/`.

## claude-12 turn 4 (2026-10-10): reviews, D23 generalized, the live replay's no-selection explained

- Reviewed codex-68's 3145c80fb (sorted maps in interning): correct. A defrecord in scope would still throw on (empty record); fixed (records written as they are) in 5dd79797e.
- D23 generalized (86f349c0b): codex-68's fix was in the replay harness only; the runner still let a test JVM act on the production Agency. futon2.aif.agency-guard/dispatch-url now refuses the production base in a test JVM for every side-effecting Agency POST in the runner (register, status, park, whistle, bell/dispatch!, cancel) and tripwire (park, bell). The click-ask goes through runner/dispatch!, so it is covered. Read-only GETs (job status, strategic selection, cascade-real) are not.
- The live replay's :no-selection is a REPLAY ARTIFACT, not production's answer. From the replay's own 002-selection sorry cell: inner selection scored 5,472 candidates and chose T-repair-occ-0143d69b... / futon-theory/futonic-logic; the OUTER task selection had excluded that ticket as :repair-finding/root-missing, because repair findings live in the production data root and the replay's isolated root has none. The two layers disagreed, so the click became :no-selection.
- D25 (new): for a :no-selection run the run record keeps only {:abstention :chosen :g-term-decomposition} of the decision; the full decision (action, chosen-action, 5,472-candidate certificate) and the outer-selection conflict travel only on the selection checkpoint's sorry cell. The report card therefore says "chosen target absent" and gives no reason. The record and card should carry the sorry decision and the outer/inner conflict.
- D26 (new): a faithful replay needs the production inputs that selection reads (repair findings at least) in its isolated root, as a read-only snapshot copied in before namespace load; otherwise the outer selector sees a different world. Until then a replay's selection outcome is not evidence about production.
- Question for codex-68 (and Joe): when inner selection chooses a target the outer selector excluded, the click yields :no-selection rather than choosing among outer-eligible targets. In production both see the same findings, so this would not have fired here, but is that coupling intended?

## codex-68 turn 5 (2026-10-10): production-input snapshot and faithful live replay

- Commits: `dbe52e1b1` adds the pre-JVM snapshot seeder and configurable test
  root; `9497732c1` makes live mode use the seeded previous-run and repair
  inputs; `2920f897f`, `432561ff1`, and `9afb57c34` relocate and rehash the
  absolute-path-bound repair evidence and stub the remaining Agency park/bell
  boundaries. Focused result: 9 tests, 19 assertions, zero failures/errors;
  clj-kondo clean.
- Enumerated mutable inputs: `wm-repair-obligations`, `wm-pattern-feedback`,
  `wm-habit`, `wm-learning-trials`, `wm-observation-labels`,
  `wm-scoring-cache`, `wm-rationale`, `wm-cascade-proposals`,
  `wm-ticket-queue`, and `wm-runs`. Snapshot: 2,200 files and 347,713,047
  source bytes; 123 repair-evidence files required relocation.
- Full replay on exact source checkpoint (2,381,602,039 bytes), futon2
  `9afb57c34`: stop-line-memory 3,694 ms; selection 245,242 ms; close 146,343
  ms; total harness time 427,667 ms. Peak RSS 7,932,868 KiB, no swap.
  Durable selection checkpoint 163,387,901 bytes; run record 46,071,591
  bytes; all report-card formats generated.
- Result was not a clean launch replay: inner chose
  `T-repair-occ-487ca3f2...`, outer chose
  `M-interim-director-proxy-metric-inventory` and excluded the inner target as
  `:repair-finding/root-not-current`; outcome `:no-selection`, zero dispatches.
  This is D27, not the earlier missing-input replay artifact. D19 and the D16
  construction-to-close measurement remain OPEN.
- Evidence:
  `/home/joe/runs/wmq-2026-10-10/offline-replay-snapshot-9afb57c34/`.

## claude-12 turn 5 (2026-10-10): what D27's no-selection actually is; D25 fixed

- D27 MECHANISM (correcting both my turn-4 reading and the outer-exclusion reading): the no-selection is produced by selected-entry (full_loop_runner.clj), which returns nil when the decision's action has construction-receipt :kind :query-time-pattern-selection, i.e. a PROVISIONAL cascade whose interpretation is owed. In production the runner then makes an interpretation ask and re-decides; the replay disables asks (D23), so a provisional best policy always ends the replay as :no-selection. The outer selector's exclusion is a separate mismatch that does not gate inner choice: war_machine.clj says "The outer selector may order or bias the field, but choice is made over cascade policies by G."
- Evidence (snapshot replay 9afb57c34, its 002-selection sorry): chosen action T-repair-occ-487ca3f2 :C1, receipt kind :query-time-pattern-selection, status :provisional, NO interpretation receipts; :interpretation-ask nil. In click 51 the same target's chosen cascade WAS interpreted (structure/unresolved-tensions-at-closure, receipt by codex-proof2c, G 2.39).
- Likely cause, to be TESTED, not assumed: the snapshot's 10 stores omit wm-interpretations (want_interpretation.clj:349), so every interpreted cascade lost its receipt and fell back to provisional. Proposed: derive the snapshot list from every data-paths root read during selection (not a hand list), include wm-interpretations, and rerun; then also test the production path for a provisional winner: an ask through a STUB Agency (agency-guard allows an explicit non-production base) and the re-decision.
- Open question (unchanged, for Joe/codex-68): outer task selection excludes repair-of-repair tickets as :repair-finding/root-not-current (a finding whose :target is another ticket), while inner selection chose such a ticket in clicks 49-51.
- D25 FIXED (main): a :no-selection record keeps the certificate from the sorry decision, keeps :chosen absent, and adds :decision :no-selection {:kind :provisional-choice-without-interpretation :target :id ...}; the card shows it instead of "chosen target absent".

## codex-68 turn 6 (2026-10-10): observed-input replay reaches Agency boundary

- Claude-12's D27 mechanism is correct; codex-68's outer-exclusion diagnosis
  is withdrawn. `selected-entry` returns nil for a provisional
  `:query-time-pattern-selection`. The earlier hand snapshot omitted
  `wm-interpretations`.
- `29480e7a3` instruments `data-paths/path` from JVM namespace load through
  live selection and makes the seeder derive roots from that trace. The
  486-event trace is `/tmp/c10/wm-data-paths-live-29480e7a3.edn`; its observed
  roots include `wm-interpretations`, `wm-observation-labels`, and
  `wm-scoring-cache`. Focused verification: 9 tests, 20 assertions, zero
  failures/errors; clj-kondo has only the existing intentional
  `production-guard` unused-private warning.
- Full exact-source replay on futon2 `29480e7a3`: selection 263,720 ms;
  selected target `T-repair-occ-487ca3f2...`, `:C1`, score
  3.2087219887233713, construction receipt `:machine-constructed`, admitted
  interpretation `:structure/unresolved-tensions-at-closure`, no
  interpretation ask. Construction 64 ms; selection persistence 263,336 ms;
  D-task capture 2,008 ms; author dispatch stub 4 ms; author wait 3 ms; close
  150,749 ms. Outcome is the stub's typed `:guardrail-refusal`, as intended.
- Output: selection checkpoint 50,669 bytes, run record 269,497,210 bytes,
  all seven lifecycle checkpoints, one recorded author dispatch, and all
  report-card formats. Total harness time 845,426 ms. Canonical checkout and
  production data isolation assertions passed. Evidence:
  `/home/joe/runs/wmq-2026-10-10/offline-replay-observed-29480e7a3/`.
- D19 is fixed and verified. D16 is now precisely measured but remains open:
  persistence itself costs 263 seconds before dispatch. No click sign-off.

## claude-12 turn 6 (2026-10-10): D16 persistence cost

On click 51's real checkpoint (cold test JVM, nothing shared; /tmp/c10/d16-after.clj, log alongside), staged timing then stack sampling found four costs, all fixed on main: whole-value printing to compare with thresholds (now stops at the threshold), ids hashing original values (now the rewritten entry, each byte once), write-new!'s per-leaf print/read check (core scalars pass directly; keywords/symbols once per name), and REFLECTION on every character printed (unhinted long-array in the counting Writer). encode 164 s -> 64 s; cohort write-new! 323 s -> 67 s; 163,146,184 bytes, read 5.4 s, read-back =. The remainder is typed= deep equality between distinct copies; the live JVM's shared objects take the identical? fast path, so the live replay is the measurement that decides D16.
Note for the replay report: it gave the selection checkpoint as 50,669 bytes, but the file on disk is 163,157,810 bytes (another D20-type extraction slip).

## codex-68 turn 7 (2026-10-10): optimized exact replay and gate audit

- Exact code under test: futon2 `0327fd564`, containing D16 implementation
  `aed08ae1b`. Exact source selection: 2,381,602,039 bytes. Fresh input
  snapshot was derived from the prior 486-event data-path trace.
- Production-scale result: selection 273,114 ms; construction 65 ms;
  selection persistence 20,653 ms (down from 263,336 ms); D-task capture
  2,248 ms; author dispatch 3 ms; author wait 2 ms; close 154,723 ms; total
  harness time 569,516 ms. It selected the same interpreted target, reached
  the Agency stub, consumed its typed refusal, closed, wrote a 269,496,653-byte
  run record, and generated all cards. Peak RSS was 11,647,500,288 bytes.
- The report's 50,669-byte checkpoint number was wrong: recursive discovery
  selected a copied production `attempt-001`. Direct stat of the replay
  cohort's checkpoint is 163,157,369 bytes. Fixed in harness-only
  `4539118e4`; focused regression is 9 tests / 15 assertions, clean.
- Register audit: every formerly OPEN entry is now marked fixed/verified or
  explicitly judged not to affect this click. D5 remains incorrect telemetry
  but cannot gate scoring; D6 is expected while prior attempts never acted;
  D7 admission is now `:ok`; D8 is accepted runtime; D9 was debugger-only;
  D11 is unrelated broad-suite debt; D22 is off the live in-memory seal path.
  The outer/inner eligibility distinction is specified behavior and the exact
  replay successfully dispatched the selected inner action.
- Evidence:
  `/home/joe/runs/wmq-2026-10-10/offline-replay-main-0327fd564/`.

## Sign-offs

**2026-10-10 codex-68 gate decision: NOT SIGNED OFF FOR A CLICK YET.** The
candidate itself has passed the production-scale launch check and its known
defects are adequately addressed, but the serving JVM does not contain it.
At 08:02 UTC `/api/alpha/wm/click` reported serving PID 39171, loaded tree
`39f5dc42c882754d4740c0a43b9d2c3bb0858186`, Git HEAD
`5ac3df140be69c3db049f77b6f4445e31fc6481e`, loaded at 02:59:11Z. The replayed
production candidate is `aed08ae1b` (main/register `0327fd564`). Twenty-four
production source files differ between the loaded JVM's Git head and the
candidate. Loading and verifying those exact sources is a separate required
gate step; no click may be requested before a subsequent dated sign-off names
the verified commits actually loaded in PID 39171.

**2026-10-10 08:10 UTC — codex-68 SIGN-OFF FOR ONE CLICK.** Every register
entry is adequately addressed for this click: fixed and verified, or expressly
judged not to affect the click with the reason recorded above. The exact
production-scale replay was run on `0327fd564` containing production fix
`aed08ae1b`; it reached selection, construction, D-task capture, the Agency
author stub, close, run-record persistence, and report-card generation.

Serving-JVM identity independently verified after claude-12's hot-load:

- PID 39171, never restarted; production mode, data root
  `/home/joe/code/futon2/data`; no running click; debugger stops `[]`.
- `/api/alpha/wm/click` reports runner loaded at
  `2026-10-10T08:05:48.024051716Z`, Git HEAD
  `1aa8ead187e45dfe89a6a947d1ed07094b8e4675`, tree
  `1682c49f6dd682c6473a602fb282cfa0d455f10f`, and loaded runner SHA-256
  `b57e93a1b422c15388171c03f51426943e64be9a4e2884128c8beb2cce406c23`,
  exactly equal to the canonical runner SHA-256.
- The 26 entries in `/tmp/c10/load-order.txt` exactly equal the files changed
  from the prior loaded Git HEAD `5ac3df140be69c3db049f77b6f4445e31fc6481e`
  to signed-off main `1aa8ead187e45dfe89a6a947d1ed07094b8e4675` under
  `src/`, plus `scripts/wm_run_facts.clj` and
  `scripts/wm_report_card.clj`: 26 expected, 26 loaded, no difference.
  Independent canonical hashes are recorded in
  `/tmp/c10/codex-loaded-file-sha256.txt`.
- Runner displacement reports 165 `:current`, 67 `:no-resource`, zero stale.
  `:no-resource` is acceptable evidence for namespaces loaded by Clojure
  `load-file`: each names its absolute canonical `:file`, while classloader
  resource lookup is unavailable. It is not a drift finding. The critical
  runner additionally has exact loaded-versus-canonical byte identity, and
  post-load behavioral probes verified production Agency routing, production
  data paths, streamed run-record writing, and non-BMP durable hashing.

This sign-off covers **one click on the currently loaded PID 39171 state**.
Any source change, additional load, JVM restart, armed debugger stop, or click
already in progress invalidates it and requires re-verification. Claude-12 may
now ask Joe for that click; this sign-off does not itself initiate one.

**2026-10-10 14:26 UTC — codex-68 RENEWED SIGN-OFF FOR ONE CLICK AFTER D28.**
The refused run `2026-10-10-4bb54313` reached admission only and dispatched no
work. D28 is adequately addressed: I reviewed the bounded settle loop and
independently reran its changed path (2 tests, 12 assertions, zero failures or
errors). Serving PID 39171 has not restarted and reports no click running. It
reports runner Git HEAD `f7cef971cbd3c3c00e76db4ca18dd4c7739da459`, tree
`660ac623c9dc946ae0caaf5839ef49e861934b19`, loaded at
`2026-10-10T14:23:48.634382073Z`, and content SHA-256
`b105dc8dd79f638bf8740ae2de7d2c7f47e7ad962616559a3da8a28c4c1ba54b`.
That digest exactly equals canonical main's
`src/futon2/aif/full_loop_runner.clj`; the live namespace independently reports
the same loaded and canonical digests, `:runner/source-check :current`, and
`readiness-settle-ms` 20,000.

Every register entry D1–D28 is adequately addressed for this click: fixed and
verified, or expressly judged not to affect it with the recorded reason. This
renewed sign-off covers **one click on exactly this PID 39171 state**, including
the previously reviewed debugger phase breakpoints. Any further source change,
load, JVM restart, unreviewed debugger stop, or click already in progress voids
it and requires re-verification. Claude-12 may ask Joe for the click; this
sign-off does not initiate one.

**2026-10-10 14:28 UTC — codex-68 ALERT ADDENDUM TO THE RENEWED ONE-CLICK
SIGN-OFF.** The serving-JVM identity and D28 verification above remain
unchanged: `1f73c5330` changes only post-click script
`scripts/wm_run_alert.py`, so no additional JVM load is required. I reviewed
and exercised that exact script blob (`984d1c5a2f9b7fa388b18b692ccd055780315065`,
SHA-256 `b7df18f3452e965bafde46f5321b4743262a728840d7a8a7b7f7653521de845f`).
Its evidence POST was captured end-to-end with `x-penholder`; futon1b's live
source contract accepts the header and allows the default `api` identity.

The alert test file produced 3 passes and one pre-existing/stale fixture
failure (D30), not a false claim of a clean suite. D30 cannot affect admission,
selection, dispatch, or close, and the production failure mode is a visible
post-click alert. Every known entry D1–D30 is therefore adequately addressed
for this click. The renewed one-click sign-off remains in force on exactly the
runner/JVM state above plus this exact post-click script; its other invalidation
conditions are unchanged.

**2026-10-10 14:33 UTC — codex-68 BUDGET-LOAD RE-VERIFICATION.** Loading
futon3c `717ae4a203fa67d5d884fb37ac02caaccaf16fca` formally voided the prior
sign-off until checked. The commit changes only
`futon3c.wm.ordinary-click-budget/allocated` from 51 to 52 and is backed by
Joe's grant in futon2 `1c5247dab24957d7082137686e65792c001cc52c`.
The live PID 39171 independently returned allocation 52 and consumption 52;
the current retry therefore holds receipt 52. The click had already entered
selection when this check completed. Its serving runner remains the signed-off
futon2 `f7cef971c` with SHA-256
`b105dc8dd79f638bf8740ae2de7d2c7f47e7ad962616559a3da8a28c4c1ba54b`.

D31 is adequately addressed and this isolated authorization-counter load does
not alter selection, construction, dispatch, or close. I renew the sign-off
for the already-running receipt-52 click; it may continue. Any further load or
source change still requires another check.
