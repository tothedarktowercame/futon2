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
| D1 | The run closes via the exception path (full_loop_runner.clj:7262) about 75 s after construction ends OK, before the author is dispatched. The exception was discarded; its cause is unknown. This is why the machine does no work. | clicks 50, 51 | OPEN. claude-12 2026-10-10: the 75 s is spent in `(persist-selection! trace-path)` at full_loop_runner.clj:6497 (code as loaded, 43f3d6157), which runs right after construction and before `checkpoint! :construction`. 002-selection.edn did NOT exist after it: the directory entry was created at 03:12:43, inside close. So the first persist of the 2.38 GB cell failed, the run closed, and close-core!'s second persist wrote it (03:12:43 to 03:13:53) before the read-back OOM. Probable cause: D2 (one 2.4 GB cell in a JVM with -Xmx12g). Not proven: the exception was discarded; C logs it. |
| D2 | The selection checkpoint is 2.38 GB. The correct byte budget (claude-12, measured on the real file with counting writers): payload 2,380,766,904; the controller decision 1,167,479,103, stored again at :ground :decision; inside it, :selection-certificate 1,128 MB, of which :scoring 578 MB (5,381 per-candidate scorer certificates, about 107 KB each), :node-evaluation-traces 105 MB, :candidates 86 MB, :g-term-decomposition 82 MB, :focus-receipt 72 MB, :policies 64 MB, :parameter-novelty 42 MB. (claude-12's first anatomy wrongly named :observation-schedules as the 578 MB part; they are 31 MB under :precision-family.) 95% of the bytes are exact repeats. | click 51 | OPEN. codex-68 production replay on C persisted a 1,963,814,739-byte selection checkpoint (only 17.5% smaller), reached 13.0 GB RSS, and made no further observable progress for 38 minutes before SIGKILL. C is not an adequate D2 fix at production scale. Dedupe/schedule work remains required. ALSO: FIX PROPOSED: futon2 4686f9098 (branch wmq/durable-selection-real): lossless interning. On the real file, through cohort write-new!/read-edn: 163,872,706 bytes, read 6.5 s, = to the original. Write took 305 s in a test JVM where nothing is shared (see D14). Awaiting codex-68's review. |
| D3 | Appending a checkpoint re-reads earlier ones in full with slurp, giving an OOM over 2 GB (full_loop_cohort.clj:59). | click 51 | C's metadata-only append was exercised at production scale: after the 1.96 GB selection existed, construction, dispatch, build and adjudication files were appended. However the replay did not close, so D3 is not yet launch-verified end to end. C is not loaded. |
| D4 | The emergency record lacks the click identity, so no report card is made. | click 51 | fixed in C, not loaded |
| D5 | :scoring-target-budget :scored-target-count reads 0 although 5,381 candidates have numeric G. | clicks 50, 51 | OPEN (the fix was in the reverted close-overflow-2) |
| D6 | The same target, T-repair-occ-487ca3f2 (structure/unresolved-tensions-at-closure, G 2.39), is chosen in clicks 49, 50 and 51. Possibly a consequence of D1 (nothing ever changes), but not verified. | 49-51 | OPEN |
| D7 | 64-80 s pass between the opportunity start and agent-readiness, so the admission wait answers :admission-pending. | 50, 51 | OPEN, unexplained |
| D8 | Selection takes 5 min 17 s to 6 min 36 s. | 50, 51 | OPEN, unprofiled |
| D9 | GET /api/alpha/wm/click reported running? false, with click 50's last-result, while click 51 was held at a debugger stop. | 51 | OPEN |
| D10 | Each failed click leaves a multi-GB checkpoint in futon2/data. | 50, 51 | OPEN (follows D2) |
| D11 | Test hygiene: 4 namespaces pass only when another namespace has loaded full-loop-runtime first; the watchdog test is flaky; 76 namespaces fail (known debt). | warrant re-seed | OPEN: judge whether any of it bears on a click |
| D12 | `futon2.data-paths/*data-root*` does not isolate all writes after data-owning namespaces have loaded: at least `full-loop-cohort/default-data-root` and the tripwire store capture production-derived paths at namespace load. The first offline production replay therefore wrote `wm-full-loop/wm-outer-loop-46-v1/attempt-062/001-time-step.edn` and `wm-tripwires/trips/trip-d9b59d2b-....edn` before codex-68 interrupted it. | offline replay 2026-10-10 | OPEN. Both files were moved intact to `/home/joe/backups/wm-offline-replay-leak-20261010/`. Replay now refuses unless process-wide test-root mode was selected before namespace loading; verify with a clean production-tree snapshot. |
| D13 | On C at production scale, close remains non-terminating/resource-exhausting after durable selection persistence. The isolated replay produced the 1.96 GB selection, 59,678-byte construction, 298-byte dispatch, 292-byte build and 291-byte adjudication checkpoints plus a 40,952,555-byte Morning Brief item, but no closed checkpoint or run record; it consumed ~13.0 GB RSS and made no observable progress for 38 minutes. TERM and interactive interrupt did not stop it promptly; it required SIGKILL. | offline replay 2026-10-10, exact click-51 selection | FIXED AND VERIFIED on futon2 `f6c66ecbe`: close terminated in 170,867 ms, produced a 297,935-byte closed checkpoint and 237,209,756-byte run record, and the report card generated. This verifies the former close nontermination, but D19 still prevents an Agency-boundary launch replay. |
| D14 | close-overflow-C (874ce8c78), merged on futon2 main but NOT loaded, makes 9 test namespaces newly fail (warrant check /tmp/c10/warrant-C.log): previous-run, g-term-decomposition, selection-always, cascade-evaluation-trace, finding-ticket, flight-conditioning-step-record, flight-driver, hermetic-retention, temporal-consume. Cause: its lossy candidate projection in the run record (the recorded decision loses :chosen). | warrant check | FIX PROPOSED in 4686f9098 (projection removed; all 9 pass on the branch, apart from the expected worktree source-drift guard). C must NOT be loaded from main as it stands. |
| D15 | Run records are not interned, so a successful click 51-shaped run would write about a 1.2 GB run record. About 20 readers read run records directly (previous_run.clj reads the previous record during SELECTION; flight_*, trace, progress_read, scripts, futon3c runner_service). | code reading | FIXED AND VERIFIED on futon2 `f6c66ecbe`: the replay wrote a 237,209,756-byte streamed/interned record, the shared reader hydrated it, and the report card consumed it. |
| D16 | Writing the interned checkpoint took 305 s in a test JVM (deep equality on distinct copies). The live JVM shares subtrees, so identity should make it faster, but this is unmeasured. Close time must be measured in the replay. | 4686f9098 | PARTLY VERIFIED on futon2 `f6c66ecbe`: selection-end to construction-start was 19,711 ms with live sharing, versus 323 s cold. Construction-end to exception/close-start was still 354,476 ms (the current events do not isolate persistence from intervening dispatch binding), and close was 170,867 ms. Performance is no longer nonterminating but the unexplained 354 s gap remains OPEN. |
| D17 | Live readers parse whole run records to read a field or two: previous_run.clj:57-73 `previous-record-file` parses EVERY record of the newest earlier date (slurp + read-string) to get :startedAt, on every click; full_loop_runtime.clj:26-40 parses the newest record on each judge call; registered_run_telemetry.clj:208-218 at every runner start. With click-51-sized records each parse is about a GB. Possibly behind D7/D8 (unmeasured). | claude-12 reader audit 2026-10-10 | OPEN (claude-12, D15 turn) |
| D18 | enactment_fold_source.clj:152 computes sha256 of `(pr-str record)` for the WHOLE predecessor run record on the live path (full_loop_runtime predecessor -> conditioning-step-from-completed-run). A click-51-sized record exceeds the 2 GB String limit here, interned on disk or not. | claude-12 reader audit | OPEN (claude-12, D15 turn) |
| D19 | The production replay does not reach its advertised sole stub boundary (Agency). Click 51's real selected action is refused first by D-task binding with `:failure-kind :dispatch-evidence-unavailable`; `:dispatches` is empty. Thus the replay closes an exception-path attempt and cannot certify construction -> Agency dispatch -> close. | offline replay on main `f6c66ecbe`, exact 2,381,602,039-byte click-51 selection | OPEN. Either supply the real prerequisite dispatch evidence in the isolated replay or establish that production would refuse identically; do not bypass the binding invariant. Then rerun through the Agency stub. |
| D20 | The replay report loses or mislabels measurements it already has: `phase-summary` reads `:elapsed-ms` while runner events use `:duration-ms`, so every reported phase duration is nil; attempt discovery compares `attempt-001` to the result's external attempt id, so `:checkpoint-bytes` is nil despite seven checkpoint files; `:started-at` is sampled after the run finishes. | offline replay on main `f6c66ecbe` | FIXED AND VERIFIED in `8f63f9aa5`: the live replay report contains real phase durations, the correct start instant, and discovers `001-time-step.edn`; regression tests cover duration and nested attempt discovery. |
| D21 | The supposedly isolated replay mutated and committed to the canonical futon2 source repository. Its synthetic `dispatch-evidence-unavailable` failure published `holes/tickets/T-repair-occ-182f...md` and created commit `79a523906` on main at 05:33:42, exactly when close began. The data-root isolation does not cover repair ticket publication. | offline replay on main `f6c66ecbe` | FIXED AND VERIFIED by `a77f627e9` plus replay assertions in `8f63f9aa5`: both subsequent live runs left canonical Git HEAD/worktree and production data unchanged. `79a523906` was reverted by `1c52c50bf`. |
| D22 | `interpretation-evidence/value-digest` is not stable across an EDN round trip because its canonicalizer descends maps and vectors but not sets or seqs. Any sealed value containing sets cannot be re-verified after persistence. | D19 diagnosis; click-51 family contains 103,563 sets | OPEN, but judged not to affect the next click's live in-memory seal/validate path. A durable fix needs a versioned digest so it does not invalidate existing seals. It remains relevant to persisted precision-state readers. |
| D23 | The live-selection replay set `:interpretation-ask-fn nil`, but nil means “use the production default.” It dispatched real Agency job `invoke-1791611767087-1281-b6439847` during selection redecision, before the intended author-dispatch stub. The job was already terminal when cancellation was attempted; the replay was then interrupted. | live replay `8f63f9aa5`, 2026-10-10 | FIXED AND VERIFIED in `076caea76`: an explicit no-op function prevents fallback; 242,568 ms of live selection completed without an interpretation dispatch. The run selected no action, so author dispatch was not reached. |
| D24 | Durable interning assumes `(empty map)` supports transients. A live no-selection close contained a `PersistentTreeMap`; encoding it threw `ClassCastException: PersistentTreeMap cannot be cast to IEditableCollection`, then close fallback lacked required checkpoints. | live replay `076caea76`, 2026-10-10 | OPEN. Fix the encoder to preserve non-editable map types without transients and rerun the close path. |

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

## Sign-offs

(none yet)
