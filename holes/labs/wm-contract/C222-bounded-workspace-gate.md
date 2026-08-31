# C222 — bounded workspace-gate admission

Date: 2026-08-31. Consumer: every lane invoking the full workspace gate.

## Verdict contract

`make workspace-gate` now submits the unchanged
`bb -cp . checks/wm_workspace_gate.clj` command through
`futon3c/scripts/bg.py launch-test`. The submitter distinguishes:

- `QUEUED`: both admission slots are occupied; no gate verdict exists;
- `ADMITTED`: a bounded job identity exists, but no verdict exists yet;
- `RECEIPT`: the terminal inner gate exit and outer resource verdict both
  exist;
- `INTERRUPTED exit=130` or `ADMISSION_TIMEOUT`: no verdict.

It does not retry a failed gate. It retries only admission while the service
reports its explicit `admission-cap` state. Admission decisions are protected
by `/tmp/futon-bounded-tests/admission.lock`, so simultaneous submitters cannot
both claim the final slot.

## Saturation control

With two active bounded jobs, the control invocation

```sh
python3 scripts/run_workspace_gate_bounded.py --command true \
  --label c222-queued-control --poll-seconds 0.2 --admission-timeout 30
```

reported `QUEUED {"active": 2, "admission-max": 2}`, then
`ADMITTED id=bounded-1788217337485-c222-queued-control`, then a receipt with
`inner-exit: 0`, `outer-exit: 0`, `resource-status: clean`, and
`verdict: pass`. Waiting was not presented as either failure or success.

## Gate as found

The real bounded run was
`bounded-1788217343807-c222-workspace-gate`. It completed rather than timing
out:

- inner gate exit: **1**;
- outer bounded-service exit: **125**;
- resource status: **clean**;
- gate findings: `q-interface-completeness` stale Lean-spine pin and one
  genuine `belief.clj` referent drift.

Receipt:
`/tmp/futon-bounded-tests/bounded-1788217343807-c222-workspace-gate.receipt.json`.
Resource receipt:
`/tmp/futon-bounded-tests/bounded-1788217343807-c222-workspace-gate.resource.edn`.
The red is a real gate verdict, not contention or native-thread exhaustion.

Canonical invocation:

```sh
make workspace-gate
```
