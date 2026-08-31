# C95 — `wmRunsOnce` diagnosis

Date: 2026-08-31  
Contract authority: `86186c37444ac9f1b9d54818b092bbbc586854f4`

## Finding: the contract comment is stale

`wmRunsOnce` is not actively falsified.  A cold on-demand invocation completed
on 2026-08-30 and left `tick-run-record-2026-08-30.edn`: nine route hops,
`traceWritten true`, five preference layers, five inputs read, and selector seam
`stub:first-ranked-authorized-mission`.  The committed witness accepts that
receipt and rejects the missing-`:traceWritten` control.

The phrase `CURRENTLY FIRING` in `Holes.lean:822-823` describes the earlier
AUD-D3 standalone-report failure, before `run_tick_once.clj` introduced an
explicit seam.  It no longer describes the witnessed runtime state.  This pass
does not amend the declaration or binding.

## The selector seams are two different paths

1. The diagnostic one-tick entrypoint tries to resolve
   `futon3c.peripheral.live-wm-selection/validated-selection`
   (`run_tick_once.clj:16-17,55-62`).  Futon2's dependency set contains Futon3a
   but not Futon3c (`deps.edn:1-8`), and a direct resolution probe returns a
   `FileNotFoundException`.  This is not silently defaulted: `selector-seam`
   installs the named stub and records both the seam and resolution error
   (`run_tick_once.clj:92-99,201-212`).  The stub is bounded to the first ranked
   mission and explicitly says `executed? false` (`run_tick_once.clj:64-90`).

2. The production operator/full-loop path does not require Futon3c on the
   Futon2 classpath.  It POSTs the identical request to the serving Agency
   endpoint (`full_loop_runner.clj:555-574`) and requires an `{:ok true
   :selection {...}}` response (`:576-590`).  It has typed timeout/retry failure
   rather than a stub (`:641-690`), and `run-opportunity!` injects this selector
   into the actual war-machine judgment (`:2411-2426`).

Therefore there is no single selector blocker shared by both paths.  The
standalone live selector is unavailable locally but has a declared diagnostic
fallback; the operator path is structurally complete and depends on the live
HTTP service.

## What can complete today

- A real report tick can complete under the declared-stub condition; the
  existing receipt proves that it already did.  This diagnosis did not run
  another tick or write another receipt.
- The operator full loop has a complete entrypoint:
  `clojure -M:wm-full-loop once` (`full_loop_cli.clj:646-660`).  It does not use
  the diagnostic stub.
- Current read-only readiness reports the configured author `zai-5` and repair
  reviewer `codex-1` idle and invoke-ready, but the configured ordinary reviewer
  `codex-7` has no available session.  That is the first presently observable
  blocker for a default operator-triggered full loop.  It is an agent-readiness
  condition, not the historical selector seam.  An operator-supplied available
  reviewer is supported by `--reviewer`; choosing one is outside this diagnosis.

No later runtime blocker is established without actually arming the operator
run.  The production chain can additionally fail loudly at substrate preflight,
HTTP strategic selection, author/reviewer execution, build, grounding, or QA;
the code types those stages, but the read-only evidence does not justify
claiming any is currently failing.

## Reproduction

```sh
# Read-only: proves why the local live selector cannot resolve.
clojure -M -e '(require (quote futon2.run-tick-once))
  (prn (#'"'"'futon2.run-tick-once/resolve-live-selector))'

# Read-only validation of the already committed completed tick.
bb -cp . checks/wm_runs_once_witness.clj \
  holes/labs/wm-contract/tick-run-record-2026-08-30.edn
bb -cp . checks/wm_runs_once_witness.clj --negative \
  holes/labs/wm-contract/tick-run-record-2026-08-30.edn

# Read-only full-loop readiness; does not dispatch or run a tick.
clojure -M:wm-full-loop status
```

The witness commands exit 0; the positive reports the named stub seam and the
negative reports that the trace-not-written mutation was rejected.  Strict
contract lint remains PASS with counts unchanged: 72 closed-by-record, 16
conformant, 5 refused-implementation, and 1 witnessed.
