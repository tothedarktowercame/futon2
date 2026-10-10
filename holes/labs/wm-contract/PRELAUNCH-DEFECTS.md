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
| D2 | The selection checkpoint is 2.38 GB. The correct byte budget (claude-12, measured on the real file with counting writers): payload 2,380,766,904; the controller decision 1,167,479,103, stored again at :ground :decision; inside it, :selection-certificate 1,128 MB, of which :scoring 578 MB (5,381 per-candidate scorer certificates, about 107 KB each), :node-evaluation-traces 105 MB, :candidates 86 MB, :g-term-decomposition 82 MB, :focus-receipt 72 MB, :policies 64 MB, :parameter-novelty 42 MB. (claude-12's first anatomy wrongly named :observation-schedules as the 578 MB part; they are 31 MB under :precision-family.) 95% of the bytes are exact repeats. | click 51 | FIX PROPOSED: futon2 4686f9098 (branch wmq/durable-selection-real): lossless interning. On the real file, through cohort write-new!/read-edn: 163,872,706 bytes, read 6.5 s, = to the original. Write took 305 s in a test JVM where nothing is shared (see D14). Awaiting codex-68's review. |
| D3 | Appending a checkpoint re-reads earlier ones in full with slurp, giving an OOM over 2 GB (full_loop_cohort.clj:59). | click 51 | fixed in C (874ce8c78), not loaded |
| D4 | The emergency record lacks the click identity, so no report card is made. | click 51 | fixed in C, not loaded |
| D5 | :scoring-target-budget :scored-target-count reads 0 although 5,381 candidates have numeric G. | clicks 50, 51 | OPEN (the fix was in the reverted close-overflow-2) |
| D6 | The same target, T-repair-occ-487ca3f2 (structure/unresolved-tensions-at-closure, G 2.39), is chosen in clicks 49, 50 and 51. Possibly a consequence of D1 (nothing ever changes), but not verified. | 49-51 | OPEN |
| D7 | 64-80 s pass between the opportunity start and agent-readiness, so the admission wait answers :admission-pending. | 50, 51 | OPEN, unexplained |
| D8 | Selection takes 5 min 17 s to 6 min 36 s. | 50, 51 | OPEN, unprofiled |
| D9 | GET /api/alpha/wm/click reported running? false, with click 50's last-result, while click 51 was held at a debugger stop. | 51 | OPEN |
| D10 | Each failed click leaves a multi-GB checkpoint in futon2/data. | 50, 51 | OPEN (follows D2) |
| D11 | Test hygiene: 4 namespaces pass only when another namespace has loaded full-loop-runtime first; the watchdog test is flaky; 76 namespaces fail (known debt). | warrant re-seed | OPEN: judge whether any of it bears on a click |

| D12 | close-overflow-C (874ce8c78), merged on futon2 main but NOT loaded, makes 9 test namespaces newly fail (warrant check /tmp/c10/warrant-C.log): previous-run, g-term-decomposition, selection-always, cascade-evaluation-trace, finding-ticket, flight-conditioning-step-record, flight-driver, hermetic-retention, temporal-consume. Cause: its lossy candidate projection in the run record (the recorded decision loses :chosen). | warrant check | FIX PROPOSED in 4686f9098 (projection removed; all 9 pass on the branch, apart from the expected worktree source-drift guard). C must NOT be loaded from main as it stands. |
| D13 | Run records are not interned, so a successful click 51-shaped run would write about a 1.2 GB run record. About 20 readers read run records directly (previous_run.clj reads the previous record during SELECTION; flight_*, trace, progress_read, scripts, futon3c runner_service). | code reading | OPEN: needs a single run-record reader that hydrates, and every reader moved onto it. |
| D14 | Writing the interned checkpoint took 305 s in a test JVM (deep equality on distinct copies). The live JVM shares subtrees, so identity should make it faster, but this is unmeasured. Close time must be measured in the replay. | 4686f9098 | OPEN |

## Sign-offs

(none yet)
