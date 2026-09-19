# WM reproduction, 2026-09-19

Owner: codex-15, requested by claude-12 under
`invoke-1789827796478-22360-17fbf8e9`.

**Result: operational `grounded-change`, but NOT a REAL run under the
requested nine-field criterion.** The current tick has no channel trace;
the default realness reader reports historical September 12 ticks instead.

## Route

Run the supplied preflight/click, establish what the serving JVM actually
loaded, reload the committed fixes from canonical checkouts, and repair the
real author-invocation refusal. No detector was disabled, no halt flag was
set, and no file under `data/` was edited by hand.

The checkout initially had futon2 `dabba24d` and futon3c `70f56323`.
Unrelated dirty files were left alone. Runtime-generated cohort 59 also
remains separate from these explicitly staged changes.

## Findings and repairs

1. **Committed code was not loaded.** The live JVM had no
   `tripwire/*halt-on-witness?*` var and U37 was false. The first click also
   wrote a 19,223,923-byte finding, demonstrating the old writer. After it
   terminated, canonical `repair-obligation`, `enumeration-completeness`,
   `tripwire`, `war-machine`, and `full-loop-runner` were reloaded. Readback:
   halt false, enumeration true, bounded finding function present. The
   second click's finding was 19,653 bytes.
2. **The invocation diagnostic was hidden.** The Codex parser's placeholder
   beat actual stderr in error selection. futon3c `54e72b9c` preserves the
   process diagnostic when only a thread-start event was emitted. The actual
   error was `input_too_large`: 1,690,401 characters against a 1,048,576 limit.
   Both initial author dispatch and its one retry hit it. Short probes of the
   same seat/model succeeded, including through the serving JVM.
3. **The author prompt embedded recursive historical findings.** futon2
   `ccfaab3b` uses the existing compact finding projection for a repair
   mission, preserving its discharge contract and naming the full finding
   file for inspection. The actual source finding prints to 1,682,394
   characters; the repaired test prompt is 4,688 characters. The third click
   reached a real author turn with tool execution.
4. **The actual repair and review ran.** codex-23 produced `d1c35641`, which
   gives the feature-card cure prompt the exact four required fields instead
   of `{...}`. codex-24 requested changes because the runner namespace still
   failed on the obsolete cohort fixture. The runner dispatched its normal
   revision round; codex-23 produced `9dfdbcac`, replacing that fixture with
   a hermetic assertion of the actual beyond-window event. Its fresh-JVM
   run passed all 175 tests / 933 assertions. The full output is retained in
   `revision-runner-tests.log`. Independent re-review approved the revision
   and independently reported the same green namespace. The run then
   grounded the commit and closed with `grounded-change`.

## Runs

All three preflights passed: Agency up, distinct codex-23/codex-22/codex-24
on roster, no click already running. The script printed `13/13 clear` plus
T8 deferred to repair. This wording is recorded verbatim in the logs; it
does not mean T8 had no witness. There was no `--force` or per-run disable.

| Run ID | Click ID | Outcome |
|---|---|---|
| `2026-09-19-1789827859` | `wm-click-1acc3e27-81e3-4c2a-9050-88882e429c90` | `build-failed`; stale runtime; both author invocations failed before execution |
| `2026-09-19-1789828199` | `wm-click-79c5a4e0-9fb1-4401-b042-a1eb0e84e865` | `build-failed`; reloaded runtime; diagnostic identifies oversized prompt |
| `2026-09-19-1789828750` | `wm-click-067eb1cf-cf32-4cc7-9b2c-1dd93b17c087` | `grounded-change`; verified binding, confirmed durability; `traceWritten false` |

The third click's production adjudication witness records `:resolved? true`
and `:dial-moved? true`, with implementation
`full-loop/implementation/9dfdbcac6de7b84d18d493918df425b5ed875d66` and
discharge `full-loop/discharge/attempt-002`. These values were read from
`006-adjudication.edn`, not supplied by the operator. See
`terminal-readout.edn` and `terminal-status.json`.

The selected feature-card repair is implemented and `:awaiting-validation`,
not falsely declared fully discharged: its contract requires a distinct
production-shaped successor. This run also wrote the resolution of the
older `repair-attempt-043-build-failed` through the normal validation path.
After completion the final runner source was reloaded from the canonical
checkout, leaving halt false and U37 true.

## Realness

`bb scripts/wm_run_realness.bb` exits 0 but reads **September 12**, four
historical ticks, each 8/9 with U37 absent. Full post-completion output is in
`realness-final.log`. It does not validate any reproduction run.

The explicit date query after the final click exits 1 (`realness-today.log`):

```
no trace file found for "2026-09-19"
```

The third click's own selection checkpoint is independently read in
`third-selection-readout.edn`: it names the correct run, records
`:repair-action-not-traced`, retains a four-candidate
`:cascade-selection-posterior`, and carries U37 `:incomplete`. None of the
seven channel-quantity keys is retained on its selection judgment.

There are two separate architectural facts to resolve before claiming this
meets the requested realness criterion:

- `full_loop_runner.clj` deliberately excludes repair actions from the
  canonical learning trace, to avoid reinforcing a selection it did not
  enact. A repair run needs its own diagnostic retention if it is to prove
  the requested quantities without contaminating that trace.
- `war_machine.clj` retired the flat ranking fields; the v1 realness reader
  explicitly covers only the channel lane. It does not assess the current
  cascade certificates. U37's attachment still reads
  `[:decision :controller-ranking]`, absent from the recorded cascade
  decision. Its `:incomplete` is not a successful census of that posterior.

No historical quantities were copied onto a new tick and no absent quantity
was synthesized to satisfy the checker.

**Decision for Joe:** reconcile the required realness contract with the
commissioned selector. Should a subsequent implementation retain and
validate the current cascade quantities on each run's own diagnostic record
(separate from learning), or restore the legacy channel quantities required
by the nine-field criterion? This is his specification decision: quietly
renaming cascade quantities as F_pi, accepting historical ticks, or
restoring a retired selector would change what counts as a real AIF run.
Neither alternative was assumed approved. The operational click is complete;
the requested realness result remains unmet.

## Validation limits

Both implementation commits passed clj-kondo (0 errors, 0 warnings) and
`futon4/dev/check-parens.el`. Codex adapter: 19 tests, 99 assertions, all
pass. Before the fixture correction, the baseline adapter had two errors
because it called unavailable `python`; its fixtures now use `python3`.

Runner focused regressions: 2 tests, 14 assertions, all pass. These cover
the realistic oversized repair shape, contract retention, finding reference,
and positive/negative author retry behavior. The production finding was
also read directly to measure the repaired prompt.

The pre-change loaded runner namespace finished 173 tests / 921 assertions
with 4 failures, 0 errors. One expected a reviewer after a terminal
`no-execution-evidence` failure; `ccfaab3b` corrects that expectation. The
other three are in `cohort-stopping-rule-returns-cohort-complete-not-repair`:
current cohort code records attempts beyond the sampling window instead of
refusing them, so this old test proceeds to live substrate/dispatch. Its
baseline made two failed zai-1 invocations (quota refusal). The changed
namespace was stopped at that nonhermetic test, exit 143; it is not reported
as green. Baseline code was loaded before edits, but canonical source changed
during that run, so T10 also recorded nonhalting line drift. See
`test-results.txt` for the exact counts and failing test names.

The run's own author subsequently fixed the obsolete cohort fixture in
`9dfdbcac` after independent review requested it. The final author namespace
result is **175 tests / 933 assertions / 0 failures / 0 errors**. Thus the
baseline failures were established before attributing any failure to the
prompt repair, and the live repair workflow subsequently cleared them.
