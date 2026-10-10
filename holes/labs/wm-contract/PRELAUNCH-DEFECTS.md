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
| D2 | The selection checkpoint is 2.38 GB: the whole decision is stored twice (:judgment :controller-decision and :ground); :observation-schedules is about 578 MB per copy; 153,367 candidate copies for 5,381 policies. | click 51 (attempt-001/002-selection.edn) | OPEN. Part B (8d3aced67) measured 27 MB on a fixture but 1,989,950,922 bytes on the REAL checkpoint (claude-12, /tmp/c10/real-proj.clj): the real duplicate is at :ground :decision, so the dedupe never matched; the observation schedules were not reached (43,048 rows retained). Returned to codex-29 with the rule: measure on the real file. |
| D3 | Appending a checkpoint re-reads earlier ones in full with slurp, giving an OOM over 2 GB (full_loop_cohort.clj:59). | click 51 | fixed in C (874ce8c78), not loaded |
| D4 | The emergency record lacks the click identity, so no report card is made. | click 51 | fixed in C, not loaded |
| D5 | :scoring-target-budget :scored-target-count reads 0 although 5,381 candidates have numeric G. | clicks 50, 51 | OPEN (the fix was in the reverted close-overflow-2) |
| D6 | The same target, T-repair-occ-487ca3f2 (structure/unresolved-tensions-at-closure, G 2.39), is chosen in clicks 49, 50 and 51. Possibly a consequence of D1 (nothing ever changes), but not verified. | 49-51 | OPEN |
| D7 | 64-80 s pass between the opportunity start and agent-readiness, so the admission wait answers :admission-pending. | 50, 51 | OPEN, unexplained |
| D8 | Selection takes 5 min 17 s to 6 min 36 s. | 50, 51 | OPEN, unprofiled |
| D9 | GET /api/alpha/wm/click reported running? false, with click 50's last-result, while click 51 was held at a debugger stop. | 51 | OPEN |
| D10 | Each failed click leaves a multi-GB checkpoint in futon2/data. | 50, 51 | OPEN (follows D2) |
| D11 | Test hygiene: 4 namespaces pass only when another namespace has loaded full-loop-runtime first; the watchdog test is flaky; 76 namespaces fail (known debt). | warrant re-seed | OPEN: judge whether any of it bears on a click |

## Sign-offs

(none yet)
