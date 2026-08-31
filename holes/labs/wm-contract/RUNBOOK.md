# War Machine operator runbook

Updated 2026-08-31. Run commands from `/home/joe/code/futon2` unless noted.

## Which command to run

| Command | Certifies | Does not certify |
|---|---|---|
| `make status` | Runs and renders the workspace gate, contract/strict/absence checks, both bounded suites, obligations, lanes, and referent drift without stopping at the first red. | It is a report, not a production tick or live operational certificate. |
| `make pre-merge` | Required review boundary: hermetic futon2 CI, then the four-repository workspace gate. It stops if CI is red. | It does not run the manual checks below or execute the machine. |
| `make workspace-gate` | Runs the cross-repository War Machine invariants, semantic controls, inventory alarm, and provenance report. Missing sibling repositories fail loudly. | It does not run the complete futon2/futon3 suites. |
| `make ci` | Runs the hermetic futon2 build and test gate (`clojure -T:build ci`). | It says nothing about sibling checkouts or live operator state. |

Use `make pre-merge` for review. Use the narrower commands to reproduce one
part of its result. A successful gate records the HEAD/tree/dirtiness basis of
futon2, mathlib4, p4ng, and futon3; these are provenance, not stale equality
assertions.

## Reading `make status`

- `OK` (exit 0): no component is red.
- `DEGRADED-AS-EXPECTED` (exit 0): every red component exactly matches a
  referenced, unexpired acceptance. The findings remain printed.
- `DEGRADED-NEW` (exit 1): at least one red has no exact active acceptance, or
  an acceptance changed, expired, or became invalid. Act on this state.

The acceptance inventory is
`checks/wm-status-accepted-red.json`. The report prints every active and unused
entry, including reason, reference, review deadline, and clearing condition.
Adding a line is insufficient: signatures match exactly, references must
exist, and review dates expire. Growth in this visible list is itself a review
signal.

If the workspace gate reports an unknown `checks/*.clj`, a lane has added a
check. Classify that filename in `known-check-files` in
`checks/wm_workspace_gate.clj`, then either add a positive command, add its
semantic control, or record a reasoned manual exclusion. Do not delete the
inventory alarm and do not guess that new code is safe to execute.

## Durable and bounded jobs

Ordinary durable background work uses:

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch "COMMAND" --agent OPERATOR --label LABEL --dir DIR
python3 /home/joe/code/futon3c/scripts/bg.py status JOB_ID
python3 /home/joe/code/futon3c/scripts/bg.py tail JOB_ID 100
```

Tests and builds should use the bounded sibling slice:

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test "clojure -T:build ci" \
  --agent OPERATOR --label futon2-ci --dir /home/joe/code/futon2 --window production
python3 /home/joe/code/futon3c/scripts/bg.py test-status JOB_ID
python3 /home/joe/code/futon3c/scripts/bg.py test-list
python3 /home/joe/code/futon3c/scripts/bg.py test-health
python3 /home/joe/code/futon3c/scripts/bg.py test-kill JOB_ID
```

Read the terminal receipt, not only the inner test summary. A trustworthy pass
has `outer-exit: 0`, `verdict: pass`, clean resource status, no task-limit
event, and complete test counts. Exit 125 distinguishes outer test/resource
failure; cancellation is recorded separately. Receipts retain command exit,
systemd result, task peak, native-thread markers, and `pids.events` deltas.

Current configuration reported by `test-health` is 1,280 tasks per job, 2,560
aggregate, and two concurrent admissions. At 2026-08-31 20:13 UTC the current
configuration window is **17 of 30 production runs: 15 pass, 2 test failure, 0
containment failure**. Eight controls and thirty measurements are excluded;
the original 256-task production failure remains recorded as superseded.
Retirement is evaluated only after 30 current-configuration production runs:
retire when containment failures exceed test failures. Controls,
measurements, and superseded configurations never seed that comparison.

## Deliberately manual

- `bb checks/lane_registry_check.clj` measures commissioner/dispatcher
  discipline (`:stale-holding`), not repository validity. Run it during
  dispatch and closure review; do not make source CI depend on operator timing.
- `bb -cp . checks/wm_operational_certificate.clj --run RUN.edn --resource RESOURCE.edn --certificate OUT.edn`
  consumes an actual operator-triggered run and its bounded resource receipt.
  It cannot honestly run during source-only CI because CI has no live run to
  certify.

These exclusions are printed by the workspace gate. “Manual” means attached
to the named operator event above, not merely available on disk.
