# Reviewer 3: enactment and close (after the selected canonical cascade)

Reviewed at futon2 HEAD `0975c7df7` (2026-10-18 03:13, "route executable cascades into production close path"), futon3c at its current checkout. Read only. No test namespaces run. Two throwaway checks: a `bb` one-liner using the same cheshire call as `handoff_contract.clj` (finding 2), and a `bb` read of the click-5 run record and recent trace files (findings 6 and 8).

Answers to the four questions:

- **Live `:wm/pattern-apply` executor.** There is none. Commit `0975c7df7` adds `route-frontier` (`cascade_capability_dispatch.clj:70-85`), which maps the capability to the keyword `:full-loop-build-review-close` and returns a map. Nothing reads that keyword or the `:capability-routing` map (grep over futon2/src finds only the producer). The only executor that produces receipts is `recording-pattern-executor` (`cascade_capability_dispatch.clj:87-126`), which is scoped `:offline-replay` and is called only from `recorded_flight.clj:145-148`. In production, "routed" means the ordinary selected entry carries on into the old mission author/reviewer path.
- **Who authors operational holes live.** No one supplies `:cascade-operational-authoring` in production. The only readers of that key are `cascade_decision.clj` and `recorded_flight.clj`, and futon3c `runner_service.clj:798-813` does not set it. So every live compile uses the empty seam authoring (`cascade_decision.clj:1470-1480`), every node becomes `:open-hole`, and the only live path that could close a hole is D4, the open-cascade resume (`full_loop_runtime.clj:104-134`). D4 asks the author to commit a code patch. It cannot close holes, for the three reasons in findings 1-3.
- **First stop on the live path.** Finding 1, at the D4 author dispatch. The click closes `:incomplete` with `:wm-job-ledger-unavailable`. Findings 2 and 3 are the next two stops behind it.
- **Trace OOM.** The trace writer is real and on the path, and it is exercised before the author is dispatched. The records it would write are 10-12 MB each, and the index has a full-history rebuild path that would read a 569 MB single-line record. Details in finding 8.

No live cascade has ever been saved. Neither `futon3c/data/wm-executable-cascades` nor `futon2/data/wm-executable-cascades` exists (see finding 9 for why futon3c).

## Live path, step by step (what a click that gets past selection would do)

1. `full_loop_runner.clj:6504-6506`: `execute-selected-cascade!` is called when `[:action :precedence :co-apply]` is a map. This call is **not inside `run-phase!`**, so the restart debugger has no held phase here that it could `:retry`. CLAUDE.md rule 4 ("repair the held phase live in the debugger and `:retry`") cannot be applied at this seam. The only retry is D4's own in-process retry.
2. `cascade_decision.clj:1609-1611`: compile with `seam-authoring`. In production this is always `{:attribution {:author :wm/production-seam ...} :clauses {}}`. `cascade_enactment_compiler.clj:211-220` turns every node into a hole, so `:frontier` is empty (`executable_cascade.clj:360-365`). The code then appends one `:open-hole` journal entry per node and returns `{:kind :wm/open-cascade-execution-error :phase :compile :holes [{:hole-id <occurrence-id> ...}] :resume-port {:retry ...}}` (`cascade_decision.clj:1662-1708`). Hole ids are occurrence ids, which are **vectors** such as `["problems/r3-belief-update" 0]`.
3. `full_loop_runner.clj:6511-6521`: the runner calls `:open-cascade-resume-fn`, which `full_loop_runtime.clj:280-282` installs from `runner-opts0`.
4. `full_loop_runtime.clj:120-134`: `recognize` succeeds, then `authoring-requisition` (author ≠ reviewer), then `author-contact`, which is `designated-agent-answer-fn` → `flight-runner/agency-answer-fn` → `runner/dispatch!`. **It stops here (finding 1).**
5. If it got further: `handoff/response-map`, the reviewer contact, `open-resume/handle-error!`, then `validate-answer`. **It would stop here (finding 2).**
6. If it got further: `patch-loader` (`require :reload` plus a source-digest check), then `resume!`, which calls the seam's `retry`. That re-runs `execute-selected-cascade!` with the **same opts**, so it compiles with the same empty authoring and gets the same holes (finding 3). It then returns `:resumed` with a result that is still open. The runner sets `entry` to nil and throws "War Machine abstained or selected no addressable action". `no-entry-failure-data` maps that to `:outcome :incomplete :held-phase :compile` (`full_loop_runner.clj:2216-2221`).
7. If holes were somehow closed: `:cascade-ready` (`cascade_decision.clj:1640-1660`), then `entry = ordinary-entry`, then `construct-selected-action :cascade-candidate` (`full_loop_runner.clj:2406-2436`), then the selection trace is written (`:6802-6807`), then the old mission author prompt (`:7020-7050`), reviewer and close. The executable cascade, its frontier and its routing do not reach the author, the journal, or the run record (findings 4 and 5).

## Findings

### 1. D4 author dispatch has no job ledger, so the first live hole-repair dispatch throws. Severity: blocks-next-click. Confidence: confirmed by reading end to end.

- `full_loop_runtime.clj:276-282` builds `open-cascade-resume-handler` from `runner-opts0`. The comment says "RUNNER-OPTS carries the live job ledger and exact cast". It does not.
- `:registered-run/job-ledger` is minted later, inside `runner/run-opportunity!` (`full_loop_runner.clj:7710`).
- futon3c `runner_service.clj:802-813` passes only `:click-id`, `:phase-log-fn` and `:strategic-selection-invoke-fn`.
- The handler closes over its own `opts`. The runner calls it with the error alone (`full_loop_runner.clj:6515`).
- So `designated-agent-answer-fn` (`full_loop_runtime.clj:46-66`) → `agency-answer-fn` → `runner/dispatch!` reaches `full_loop_runner.clj:1699-1704`. `run-id` is a string (minted at `full_loop_runtime.clj:243-246`) and the ledger is nil, so it throws `{:failure-kind :wm-job-ledger-unavailable}`.
- No try is in between. The exception leaves the selection code outside any `run-phase!`, so the click closes `:incomplete` and no author job is ever created.

This is the same defect that was already fixed for the selected-target-reading port; see the comment at `full_loop_runtime.clj:206-209`, which builds that port late from `(merge opts runner-opts)`. One port was repaired and its sibling was not.

Test adequacy: `full_loop_runtime_test.clj:44-85` stubs `runner/run-opportunity!` (so no ledger is ever minted) and injects `:open-cascade-author-answer-fn` and `:open-cascade-reviewer-answer-fn`, so `dispatch!` is never reached. `:89-97` only checks the seats and mode passed to a redefined `agency-answer-fn`. The stub replaces exactly the dependency that fails.

### 2. D4 answer validation can never match real hole ids. Severity: blocks-next-click (second stop). Confidence: confirmed.

- `open_cascade_resume.clj:215-220` requires `(= (set (map :hole-id holes)) (set (keys (:decisions answer))))`.
- Hole ids are occurrence-id vectors (`executable_cascade.clj:122-128`; see the offline authoring file `data/wm-recorded-flights/click5-through-cascade/offline-operational-authoring.edn`).
- Answers are decoded by `handoff/response-json-object`, which calls `(json/parse-string raw true)` (`handoff_contract.clj:25`). That keywordizes every object key.
- Checked with the same cheshire call in bb: the key `"[\"problems/r3-belief-update\" 0]"` becomes the keyword `:["problems/r3-belief-update" 0]`, and the set comparison is `false`.
- If the agent instead sends `decisions` as an array, `(keys [...])` throws inside `validate-answer`, and nothing catches it.
- Every correct live answer is refused as `:answer-holes-unresolved`.

Test adequacy: `full_loop_runtime_test.clj:47` uses `:hole-id :h`. A keyword survives JSON keywordization, so this case passes by coincidence. `open_cascade_resume` tests use hand-built maps, not decoded JSON.

### 3. D4's repair product never reaches the retried compile. Severity: blocks-next-click (third stop). Confidence: confirmed.

- `hot-load!` (`open_cascade_resume.clj:257-279`) calls the patched var and records its `:product` on the in-memory ledger only.
- `resume!` calls `(:retry ctx)` with the recognized context, which has no product.
- The seam's `retry` is `(fn [_] (execute-selected-cascade! entry opts))` (`cascade_decision.clj:1671-1681`). It ignores its argument, and `opts` still has no `:cascade-operational-authoring`.
- So the retry rebuilds the same all-hole record under the same compiled digest. `resume!`'s only test is digest equality (`open_cascade_resume.clj:303-308`), so it reports `:resumed`, and the click ends `:incomplete` with the holes unchanged.
- The only way a D4 patch could change the outcome is to edit WM source, for example the `seam-authoring` function in cascade_decision.clj, and have it reloaded into the serving JVM. That is outside what the prompt describes, and it conflicts with the ground rules on who edits WM code.
- Also plausible: `patch-loader` (`full_loop_runtime.clj:82-100`) does `(require n :reload)` from the serving classpath. A Zai author that commits in its own worktree or branch would produce `:patch-source-digest-mismatch`.

E-implementing-G-over-cascades §6 says a repair "may append an authorised design decision or revised cascade version". No code path turns an accepted D4 answer's `:decisions` into compiler authoring.

Test adequacy: every D4 test uses a stub retry that returns `{:cascade-digest d}` (`full_loop_runtime_test.clj:52`, `cascade_runner_seam_test.clj:123-127`). No test checks that the holes are fewer after a resume.

### 4. "Routed" is a label: no live executor, no `:enacted` journal entries, and the frontier never reaches the author. Severity: likely-later (reached only after 1-3 are fixed), but it means a carried tick would not be a cascade enactment. Confidence: confirmed.

- On `:cascade-ready`, the runner proceeds with `ordinary-entry` (`full_loop_runner.clj:6525-6527`).
- `construct-selected-action :cascade-candidate` sets `:precedence (vec (:precedence action))` (`full_loop_runner.clj:2427`). For a reading-family action that value is `{:co-apply {...}}`, so the result is `[[:co-apply {...}]]`, and `:shown` stringifies the map entry. This is the "vector-coerce the co-application map" plant that the excursion doc (E-implementing-G-over-cascades §9) says must be caught before authorising a click. I did not trace `fold-cascade/realize` or the classical fold on this input. The fixture test `authored-frontier-routes-into-the-existing-full-loop` gets as far as `:author-dispatch`, which suggests construction does not throw on the fixture. It does not show that the construction is correct.
- The author prompt (`full_loop_runner.clj:7020-7050`) is the mission prompt. The compiled record, the frontier and the per-occurrence clauses are not passed in.
- No production code appends `:enacted` or `:verified-effect` journal entries. grep finds no `execution-journal` call in the runner. The journal for a live cascade only ever holds `:open-hole` entries.
- The spec's definition of done (excursion §10: "its executable frontier guided a real, evidence-checked enactment step") and §9 ("verified effect advances the journal and habit evidence") are not met by this path.
- Separately, `cascade_decision.clj:1640` checks `(seq frontier)` before `(or (seq holes) (seq unread))`. A partly authored cascade (some enactable nodes, some holes) is therefore reported `:cascade-ready`, and its remaining holes and unread citations are dropped instead of held.

### 5. On the ready path, the cascade execution record is not written to the run record. Severity: likely-later. Confidence: confirmed.

`persisted-cascade-execution` (compiled digest, saved path, routing) is attached only to the sorry cell (`full_loop_runner.clj:6640-6644`) and to `no-entry-failure-data` (`:6684`). When the click succeeds, the selection cell is a `term` with no reference to the compiled record. Packet E's exit condition ("the run record contains the saved cascade, validation certificate, compiled frontier, enactment attempt and evidence under one identity") cannot be checked from a successful run's record.

### 6. The recorded tick evaluator reads a field production never writes. Severity: likely-later (offline only, but it is the tool being used to claim readiness). Confidence: confirmed.

- `recorded_tick_evaluation.clj:26` requires `(:trace-written? production-close-record)`.
- Production writes `:traceWritten` (`full_loop_runner.clj:1106`). I read it from the click-5 record: `:traceWritten false`.
- So any real close record would evaluate as `:production-trace-not-written`. The tests (`recorded_tick_evaluation_test.clj:15-32`) only use their own invented `{:trace-written? true}` map.
- Production's `:traceWritten` is set from the selection trace written after construction (`full_loop_runner.clj:6802-6807`), before the author is dispatched. It shows that the selection trace was written. It does not show that the run closed or carried a tick, so even with the key name fixed the criterion is weaker than its docstring says.

### 7. The recorded flight skips the production seam. Severity: likely-later. Confidence: confirmed.

`recorded_flight.clj:136-148` calls `compiler/compile-executable` and `dispatch-frontier` directly. It never calls `execute-selected-cascade!`, so the replay never exercised save/reload/validate, the journal open, `route-frontier`, D4, the runner's handling of `:cascade-ready`, or construction and trace for a cascade. The replay's positive result says nothing about steps 2-7 of the live path above. Under rule 4 ("a replay ... must run through the serving JVM's own code path") it does not count as evidence for this stage.

A minor point: `recording-pattern-executor` reads `[:structural-node :pattern]` (`cascade_capability_dispatch.clj:96`), but executable nodes carry `:pattern` at the top level (`executable_cascade.clj:339-340`). Every offline application record therefore has `:pattern nil`.

### 8. Trace writing and memory. Severity: likely-later. Confidence: measured sizes; the failure itself is plausible.

- The trace is written on the path, at `full_loop_runner.clj:6802` → `trace/write-trace!` → `lane-futility/append-indexed-trace!`. Nothing in my area stubs it.
- Recent records are large. Using bb on `data/wm-trace`: each 10-14 and 10-15 record is about 11-12 MB on one line. About 8 MB of that is `:outer-task-selection`, and 1.5-2.1 MB is `[:decision :selection-certificate]`.
- `wm-trace-2026-10-09.edn` holds a **569 MB single-line record** (run `2026-10-09-513e6445`). It nests the `:run/id`s of earlier runs, which is consistent with history being embedded in the record.
- The largest run record in `data/wm-runs` is 270 MB (10-10).
- Two memory risks on every write:
  - (a) `finalize-accumulation` and `finalize-scan-learn` call `read-history-strict 1` (`trace.clj:953-977`). That parses **every form in the newest daily file** strictly, so each write re-reads the whole of today's file.
  - (b) `current-index-state!` (`lane_futility.clj:180-190`) rebuilds from **all** trace files through `read-trace-file`, which `line-seq`s and `doall`s each file whenever the fingerprint (name, length, mtime of every file) does not match. Any out-of-band touch of one trace file (a sweep, a copy, an mtime change) makes the next live click read about 1.4 GB, including the 569 MB line, inside the serving JVM. The sidecar is currently dated 10-15 17:07, the same as the last trace, so it is probably valid now.
- A cascade-ready click adds the selected canonical cascade with squares to `:decision`. I did not measure how much that adds.

### 9. Executable cascade and journal roots are relative paths. Severity: minor. Confidence: confirmed.

`cascade_decision.clj:1425-1426` sets `"data/wm-executable-cascades"` and `"data/wm-execution-journals"`. The serving JVM's cwd is `/home/joe/code/futon3c` (checked through `/proc/<pid>/cwd` for the :7070 process). Live cascade records and journals would therefore land in futon3c/data, which is gitignored, not in futon2/data where the other WM run data lives. Every other WM default root I saw is absolute. There are no readers of these roots yet.

`execution-journal/append!` (`execution_journal.clj:123-160`) loads the whole shared store on every append and requires strictly increasing recorded-time across **all** journals in it. One bad file in the shared store blocks every later append.

### 10. Q7 does not look at the enactment carrier. Severity: likely-later. Confidence: plausible (I read the walker, but did not run it on a D4-failed record).

`wm_run_facts.clj:282-316` walks `:selection-event`, `:interpretation-ask`, `:terminal-receipt`, `:failure` and the chosen and selected candidate. It counts only maps whose `:status` or `:outcome` is in `#{:absent :not-supplied :missing :refused :failed}`.

D4 and seam failures are `{:status :error ...}` or `:outcome :incomplete`, and `:cascade-execution` is not a root. So a click whose cascade was never enacted, or whose enactment failed, adds nothing to `pathAbsenceCount`. Q7 ("an absence, refusal, or 'not supplied' on the one selected/enacted certificate path is a failure") could hold on a click with no enactment.

Q9 is NR or false on this route by construction (`Q9-diagnosis-2026-10-16.md`: no production factorized C), so no close-path fix changes it. Q2, Q4 and Q8 are computed from selection-side carriers, outside my area. I did not check whether a cascade-ready close changes them.

## What I checked, and what I did not

- **Read in full:** the 0975c7df7 diff, `cascade_capability_dispatch.clj`, `recorded_tick_evaluation.clj`, `open_cascade_resume.clj`, `cascade_enactment_compiler.clj`, `cascade_decision.clj:1425-1716`, `full_loop_runtime.clj`, and the runner from selection through construction (`full_loop_runner.clj:6440-6830`). Also `no-entry-failure-data`, `dispatch!`, the run-opportunity! wrapper, the trace write path (`trace.clj`, `lane_futility.clj`), the journal `append!`/`validate-journal`, the D4 and seam tests, the recorded-tick tests, Q7 and Q9 in Requirements.lean, and `wm_run_facts` Q7.
- **Not read:** the author, reviewer, warrant and close phases after `:author-dispatch` (`full_loop_runner.clj:7050-7600`), `persist-run-record!` beyond the `:traceWritten` and `:decision` block, `fold-cascade/realize` on a reading-family action, the report card, and the Lean term generator.
- **Not run:** any test namespace, the D4 handler against a real Agency, and the exporter on a cascade-ready record (none exists).

## Readiness verdict for my area

Not ready. No live click has reached this stage: no executable-cascade record has ever been saved by a live click. If the next click gets past selection with squares, it will open holes for every occurrence, because production has no operational author. It will then stop at the first D4 author dispatch with `:wm-job-ledger-unavailable` (finding 1). This failure was predictable: the same late-binding defect was already fixed for the sibling port, and the test stubs the runner that mints the ledger. Behind it are two more stops that would also be certain: vector hole ids against keywordized JSON keys (finding 2), and the retry that discards the repair (finding 3). Past those, "routed" sends the cascade into the old mission author path. There it would get no live `:wm/pattern-apply` executor, no `:enacted` journal entries, and no cascade identity in a successful run record (findings 4 and 5). The recorded flight's evidence does not cover any of this, because it bypasses the seam and checks a field name production never writes (findings 6 and 7). Before spending a click, a minimal fix needs: late-binding of the D4 ports, hole-id decoding, threading the accepted decisions into compiler authoring, and a decision about whether "routed" without a pattern-apply executor is acceptable under §10 of the excursion. That last point is a question about the specification for Joe/codex-68, not something I can rule on.
