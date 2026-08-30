# WM-RUN1 — the machine runs once, on demand, and leaves the receipt

Owner: claude-15. Builder: codex-5 (you hold war_machine.clj and hit the selector blocker at AUD-D3). Mode: work.

## The declaration you are witnessing (fixed; do not touch Lean)
mathlib4 `5983b68ff2`: `wmRunsOnce` — "the machine can run at least once on demand, leaving a receipt";
`TickRunRecord` = {startedAt, basisCount, basisMaxAt, inputsRead, inputIssues, preferenceLayers, traceWritten,
selectorSeam}. Its falsifier is CURRENTLY FIRING: `war_machine.clj -main` throws "War Machine requires the shared
reason-bearing selector" (your AUD-D3 blocker), and the bb entry is blocked in `lane_futility.clj`. Joe's framing:
on-demand single ticks are first-class (the APM machine works this way); a cron is just one caller.

## Goal (one behaviour: one honest tick from a cold start)
1. A callable entry `run-tick-once` (in war_machine.clj or a thin `scripts/futon2/run_tick_once.clj` — say which):
   resolves the selector seam EXPLICITLY — if the shared reason-bearing selector is available in-process, use it and
   record `:selector-seam "live"`; if not, use the SAME declared stub you used at AUD-D3 and record
   `:selector-seam "stub:<name>"` — never silently either. No env-racing, no cron.
2. The run produces the full result (trace? true): `:input-status`, `:preference-stack`, the decision — and writes
   the trace (loud on failure, per AUD-D3b). From the result it derives a `tick-run-record` EDN matching the Lean
   fields 1:1, written to `futon2/holes/labs/wm-contract/tick-run-record-<date>.edn`.
3. Fix the `lane_futility.clj` bb/JVM classname blocker ONLY IF it is on your path to running once; otherwise route
   around it and record the routing in the findings. Do not repair unrelated machinery.
4. Witness: `futon2/checks/wm_runs_once_witness.clj` (bb) — reads the tick-run-record, checks every field present
   and sane (basisCount > 0, preferenceLayers = 5, traceWritten true, selectorSeam non-empty), `--negative` control
   (a record missing traceWritten fails). Registry row binds `wmRunsOnce` (contract sha from the JSON — read it, do
   not type it; `:result :passed`; note names this packet).

## Acceptance (row-11 first; bare runs, exit codes direct)
- One invocation, cold JVM, completes: show the command and the tick-run-record verbatim. `:selector-seam` states
  what actually selected. The trace file for today gains a tick (stat before/after).
- Witness positive exit 0 / negative exit 1; contract lint at authority `5983b68ff24a…`: `wmRunsOnce` = witnessed.
- kondo + parens on touched files; commit only your paths; REFUSE (with the throw text) if the run cannot complete
  even stubbed — a refusal with the blocker named is a valid outcome of this packet, NOT a failure.

## Report
Bell claude-15 back with: sha(s), the invocation command, the tick-run-record verbatim, seam value, witness verdicts,
lint judgement line, refusals, diffstat.
