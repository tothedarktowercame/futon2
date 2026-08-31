# War Machine operator runbook

Updated 2026-08-31. Run commands from `/home/joe/code/futon2` unless noted.

## Which command to run

| Command | Certifies | Does not certify |
|---|---|---|
| `make status` | Runs and renders the workspace gate, contract/strict/absence checks, both bounded suites, obligations, lanes, and referent drift without stopping at the first red. | It is a report, not a production tick or live operational certificate. |
| `make pre-merge` | Required review boundary: hermetic futon2 CI, then the four-repository workspace gate. It stops if CI is red. | It does not run the manual checks below or execute the machine. |
| `make workspace-gate` | Runs the cross-repository War Machine invariants, semantic controls, inventory alarm, provenance report, and certificate machinery against the content-pinned C167 v20 fixture pair. Missing sibling repositories fail loudly. | It does not run the complete futon2/futon3 suites or certify the newest live run. |
| `make ci` | Runs the hermetic futon2 build and test gate (`clojure -T:build ci`). | It says nothing about sibling checkouts or live operator state. |
| `make run-readiness` | Read-only preflight over the gate, contract/schema, bounded suite receipts and capacity, live reviewer roster, and certification command. It prints the filled operator command when ready. | It does not select work, start a tick, or dispatch an agent. |

Use `make pre-merge` for review. Use the narrower commands to reproduce one
part of its result. A successful gate records the HEAD/tree/dirtiness basis of
futon2, mathlib4, p4ng, and futon3; these are provenance, not stale equality
assertions.

Before an operator run, use `make run-readiness`. Every precondition is printed
by name. `NOT-READY` means repair the named failures and rerun; do not infer
readiness from the summary alone. The reviewer is selected live rather than
using the absent `codex-7` default. Only run the printed
`clojure -M:wm-full-loop once --reviewer ...` command after the preflight says
`READY`.

Readiness blockers have two kinds. `UNAVAILABLE` means a required live resource
cannot be used now (reviewer, roster, or bounded admission). `UNVERIFIED` means
the machine might run, but the exact code/evidence it would use has not passed
the required check. Both block; they call for different action.

In particular, the last code-affecting action before the operator run must be a
**bounded** futon2 suite run. An ordinary `bg.py launch "make ci"` can show an
inner green summary but does not emit the outer resource receipt, so it does
not refresh readiness. Today suite freshness is conservative: the receipt must
finish after the repository's current commit timestamp and the tracked tree
must be clean. Consequently a docs-only commit also makes it stale. Comparing
the tested tree's content would be more precise, as C175's source-blob rule is,
but existing receipts do not record that tree. Do not weaken the proxy; migrate
when the bounded receipt schema records repository tree SHA plus tracked-diff
fingerprint. For this rare operation, rerunning the two-minute suite is the
honest current closure step.

### Expected duration

C162 measured fresh processes with `/usr/bin/time` on 2026-08-31; “cold” is
the first fresh JVM/process and “warm” is an immediate repeat with ordinary OS
caches retained. Dropping kernel caches was deliberately avoided because it is
invasive and unlike the operator path.

| Target | Cold wall | Warm wall | Result |
|---|---:|---:|---|
| `make ci` | 105.65 s | 101.61 s | both exit 0 |
| `make workspace-gate` | 32.91 s | 33.53 s | both exit 0 |
| `make pre-merge` | 118.08 s | 125.35 s | cold exit 0; warm completed red on concurrent strict-contract drift |

The warm composed duration is a real timing but not a green-gate claim. An
earlier immediate repeat stopped in CI at 89.62 seconds on concurrent artefact
drift, demonstrating the intended fail-fast behavior.

CI dominates. Streaming timestamps attributed about 79 of 106 seconds to four
test namespaces: `wm-operational-certificate-test` (28.2 s),
`r8-f-contract-test` (25.9 s), `futon2.aif.full-loop-runner-test` (12.6 s), and
`r2-channel-contract-test` (12.4 s). In the workspace gate,
`c116-pre-boundary-stored-f` took 17.9 of 29.7 seconds. This is fast enough for
the required pre-merge review (roughly two minutes), so no existing check is
moved to nightly. If adoption evidence later shows the review command being
skipped, optimize these named dominators before changing schedules; any future
nightly tier must name its runner and cadence, never merely remove a check.

## Reading `make status`

- `OK` (exit 0): no component is red.
- `DEGRADED-AS-EXPECTED` (exit 0): every red component exactly matches a
  referenced, unexpired acceptance. The findings remain printed.
- `DEGRADED-NEW` (exit 1): at least one red has no exact active acceptance, or
  an acceptance changed, expired, or became invalid. Act on this state.
- `DECISION-DUE` (exit 3): no new failure is implied, but the bounded-testing
  retirement window has reached 30 current-configuration production runs.
  Joe must record keep/retire and begin a new window or change configuration.

The acceptance inventory is
`checks/wm-status-accepted-red.json`. The report prints every active and unused
entry, including reason, reference, review deadline, and clearing condition.
Adding a line is insufficient: signatures match exactly, references must
exist, and review dates expire. Growth in this visible list is itself a review
signal.

The most likely lane-registry red is `:stale-holding`: a lane's job is already
done but the commissioner has not recorded or redispatched it. Exactly one is
the accepted handoff interval. Two or more produce `DEGRADED-NEW` because they
mean dispatcher backlog and idle capacity—not invalid source, but still a real
operator lapse. Clear it by recording or redispatching completed lanes; do not
raise the accepted count to match the backlog.

Status also prints the bounded-testing retirement window directly from
`bg.py test-health`: runs, passes, test failures, containment failures,
eligibility, and the retirement comparison. Eligibility is not an accepted
red; it is a fourth state because a promised decision becoming due is neither
healthy silence nor degradation.

If the workspace gate reports an unknown `checks/*.clj`, a lane has added a
check. Classify that filename in `known-check-files` in
`checks/wm_workspace_gate.clj`, then either add a positive command, add its
semantic control, or record a reasoned manual exclusion. Do not delete the
inventory alarm and do not guess that new code is safe to execute. An active
lane does not make this red expected: it does not prove ownership or execution
disposition. The filename remains `DEGRADED-NEW` until the classification is
committed. Adding only the filename to `known-check-files` is insufficient;
the check must be run or explicitly manual.

### Lean contract regeneration

Lean source and its generated contract are a two-phase lane delivery, not one
atomic commit. Commit the `Holes.lean` change first; then regenerate against
that committed authority, rebind affected witness fragments, merge the
registry, and run `contract_authority_current`, strict lint, and the workspace
gate before declaring the lane complete. The intermediate red is truthful.

Do not broadly accept `workspace-gate exit 1` while Lean work is active: that
could hide another check. An in-flight classification would require the exact
failure set, an explicit Lean-changing lane record, source-blob pins, and an
expiry. Until the lane schema carries those facts, `DEGRADED-NEW` means the
owning Lean lane still owes its closure phase. Unrelated mathlib commits do not
cause churn: freshness follows the pinned `Holes.lean` blob, not repository
HEAD alone.

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

After a bounded operator run completes, certify it with its UUID:

```sh
make certify-run RUN_ID=<uuid-from-TickRunRecord>
```

The command deliberately does not guess “latest”. It locates the unique
`tick-run-record-*.edn` carrying that UUID, locates the unique bounded-wrapper
receipt whose interval encloses that tick, stamps the normalized resource
input with the same UUID, writes the operational certificate, and prints the
topology hashes, traversal counts, resource status, and verdict. Missing or
ambiguous run/receipt evidence is a loud failure naming the paths searched.
This command certifies an already completed run; it does not initiate one.

- `bb checks/lane_registry_check.clj` measures commissioner/dispatcher
  discipline (`:stale-holding`), not repository validity. Run it during
  dispatch and closure review; do not make source CI depend on operator timing.
- `bb -cp . checks/wm_operational_certificate.clj --run RUN.edn --resource RESOURCE.edn --certificate OUT.edn`
  consumes a newly operator-triggered run and its bounded resource receipt.
  The workspace gate proves the machinery against the pinned C167 v20
  run/resource fixtures, including a tampered-record control. That reproducible
  fixture does **not** certify the current/latest operator run; this command
  remains attached to every new live-run event.

The remaining exclusions and their reasons are printed by the workspace gate.
“Manual” means attached
to the named operator event above, not merely available on disk.
