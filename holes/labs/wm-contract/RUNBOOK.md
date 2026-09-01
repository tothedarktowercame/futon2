# War Machine operator runbook

Updated 2026-08-31. Run commands from `/home/joe/code/futon2` unless noted.

## Which command to run

| Command | Certifies | Does not certify |
|---|---|---|
| `make status` | Runs and renders the workspace gate, contract/strict/absence checks, both bounded suites, obligations, lanes, and referent drift without stopping at the first red. | It is a report, not a production tick or live operational certificate. |
| `make pre-merge` | Required review boundary: hermetic futon2 CI, then the four-repository workspace gate. It stops if CI is red. | It does not run the manual checks below or execute the machine. |
| `make workspace-gate` | Submits the cross-repository War Machine invariants to the bounded testing service, waits visibly when its two admission slots are occupied, and reports the completed resource receipt. Missing sibling repositories fail loudly. | A `QUEUED` line is waiting, not a verdict; `INTERRUPTED exit=130` and `ADMISSION_TIMEOUT` likewise certify nothing. |
| `make ci` | Runs the hermetic futon2 build and test gate (`clojure -T:build ci`). | It says nothing about sibling checkouts or live operator state. |
| `make run-readiness` | Read-only preflight over the gate, contract/schema, bounded suite receipts and capacity, live reviewer roster, and certification command. It prints the filled operator command when ready. | It does not select work, start a tick, or dispatch an agent. |

Use `make pre-merge` for review. Use the narrower commands to reproduce one
part of its result. A successful gate records the HEAD/tree/dirtiness basis of
futon2, mathlib4, p4ng, and futon3; these are provenance, not stale equality
assertions.

The workspace gate includes the throwaway reload → HTTP click → operational
certificate rehearsal (`reload-click-certificate-rehearsal`). It deliberately
does not belong to Futon3's ordinary suite because it reads Futon2 and p4ng
fixtures. Measured 2026-08-31: 5.54 s cold, 5.70 s warm, about 1 GB peak RSS.
`make run-readiness` already consumes the workspace-gate verdict, so a broken
chain becomes a named readiness blocker without running an unbounded duplicate.

Before an operator run, use `make run-readiness`. Every precondition is printed
by name. `NOT-READY (waiting)` means every blocker is `SELF-CLEARING`: wait for
the in-flight delivery, bounded job, or capacity pressure to settle and rerun.
`NOT-READY (needs-you)` means at least one `OPERATOR-ACTION` blocker is present;
it stays loud even when several waiting blockers are also present. The reviewer
is selected live rather than
using the absent `codex-7` default. Only run the printed `POST
/api/alpha/wm/click` command after the preflight says `READY`.

`clojure -M:wm-full-loop ...` **as a process is retired for clicks**
(`CLAUDE.md`, M-omni-wm-runner 2026-07-26): a click runs in-process in the
serving JVM, and starting a runtime JVM from this repo is the thing the rule
forbids. The alias survives only for read-only `status`/`brief`/`review` use
and tests. Readiness printed the retired command until C213.

**Cohort 46 cancellation boundary (2026-08-31):** cancelling an in-flight
Agency job is recorded as a closed `:cancelled` attempt and consumes an attempt
ordinal, but `:cancelled` was not in cohort 46's preregistered outcome taxonomy.
The cancelled attempt therefore begins a new semantic stratum and must not be
pooled with cohort 46's original outcome classes. Do not amend the
preregistration after the fact. See
[`C206-cohort-cancellation-boundary.md`](C206-cohort-cancellation-boundary.md).

Readiness reports two independent axes. Evidence kind `UNAVAILABLE` means a required live resource
cannot be used now (reviewer, roster, or bounded admission). `UNVERIFIED` means
the machine might run, but the exact code/evidence it would use has not passed
the required check. Resolution kind says `SELF-CLEARING` or `OPERATOR-ACTION`.
The axes cannot be collapsed: bounded capacity and reviewer absence are both
unavailable, but capacity drains while an absent reviewer needs a selection.

In particular, the last code-affecting action before the operator run must be a
**bounded** futon2 suite run. An ordinary `bg.py launch "make ci"` can show an
inner green summary but does not emit the outer resource receipt, so it does
not refresh readiness. Bounded receipts record the Git tree SHA and tracked-diff
fingerprint observed before and after the suite. Readiness compares that tested
content with the current tree, not timestamps: an old receipt for an identical
tree remains current, while a recent receipt for another tree is `UNVERIFIED`.
The repository must be clean at both ends of the run and at readiness time;
uncommitted content has no stable tested-tree identity. Legacy receipts without
this provenance are also `UNVERIFIED` and must not be backfilled.

Readiness therefore requires a **quiescent tree**: run it between deliveries,
not while lanes are editing or committing. Pause repository writes long enough
to obtain a clean tree, complete the bounded suite (about 105 seconds in C162),
run readiness, and finish the operator run. The pause extends through the run,
not merely through preflight: readiness does not lock the checkout, and the
loop may read repository data after startup. A change during the suite makes
the receipt basis unstable; a change after the suite makes it differ from the
current tree; a change after readiness is an unguarded time-of-check/time-of-use
change. In each case, stop and repeat from a clean tree rather than treating
the earlier verdict as current.

The clean-tree rule remains repository-wide. There is no maintained manifest
of every source, generated input, classpath resource, and dynamically opened
file the suite or operator loop can consume, so a tested-path subset would be
an assertion without a complete dependency boundary. An isolated committed
worktree could provide a lighter future execution path while other lanes work
elsewhere, but the current shared checkout provides no such isolation.

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
- `DECISION-DUE` (exit 3): no new failure is implied, but an operator answer is
  owed. This includes a bounded-testing retirement window reaching 30
  current-configuration production runs and any Morning Brief attempt with
  unanswered QA objectives. Status names every attempt and objective, and
  distinguishes an unanswered `substantive-achievement` (belief learning is
  blocked) from missing audit-only answers.

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
This classification is a closure step in the same commit that adds the check:
run `bb -cp . -e "(require '[checks.wm-workspace-gate :as g])
(prn (g/inventory-result))"` and require `:unknown ()`, `:missing ()`, and
`:exit 0` before handing the delivery off. A later gate finding means the
adding delivery did not close.

**Why this was skipped four times (C290/C298, 2026-09-01).** The step above was
already written here, and four glossary witnesses still landed without it —
`8fb951e` at 00:06:11Z first, then three more repeating the split. The cause was
an instruction of mine, not a lapse of theirs: after C241 showed that four
concurrent repository-wide gates against a moving tree can certify nothing, I
told every lane **"focused checks only, the repository-wide gate is mine."**
The gate is what detects inventory drift, so nobody ran the thing that would
have caught it, and `{:unknown ()}` stayed stale for hours while being quoted as
evidence.

**The inventory command above is NOT the full gate.** It costs under a second
and requires no settled tree. Run it in the delivering commit even under a
focused-checks instruction; if a standing instruction ever appears to forbid it,
the instruction is wrong. Documentation did not enforce this — the same evening,
a documented ledger-heading convention was violated three times by its own
author.

### Lean contract regeneration

Lean source and its generated contract are a two-phase lane delivery, not one
atomic commit. Commit the `Holes.lean` change first; then regenerate against
that committed authority, rebind affected witness fragments, merge the
registry, and run `contract_authority_current`, strict lint, and the workspace
gate before declaring the lane complete. The intermediate red is truthful.

Every glossary binding that changes `Holes.lean` also requires a Q-interface
closure step: re-verify the Q-facing declarations and refresh the
`:lean-spine` pin in `Q-interface-completeness.edn` only when their semantics
are unchanged. If they changed, leave `PIN_BEHIND` red and report the semantic
change. The checker names this remedy but never performs or accepts the refresh
automatically.

Every newly named contract declaration also requires an explicit model area in
`scripts/generate_variable_situation_accounting.bb`; regenerate
`variable-situation-accounting.edn`, then stage-build the paper before closing
the binding.  `gen_model_coverage.py` must keep rejecting
`:unclassified` and unknown areas; there is no default classification.

### Negative-control reason preservation

All glossary controls whose evidence is Lean elaboration now use
`#guard_msgs`. Run the complete glossary mutation inventory before a
publication/release candidate and after changes to `Holes.lean`, negative
fixtures, modules, or imports; ordinary focused execution is sufficient because
the guarded fixture checks its own diagnostic. Manually review/update exact
diagnostics after a Lean toolchain upgrade, where wording changes are expected.
The former periodic manual inspection of all Lean stderr is retired: C311 made
the intended reason executable. A nonzero Lean exit alone remains inadmissible
evidence; C294 found a type control passing on a missing `.olean`.

For migrated controls, put the expected diagnostic beside the failing command
under `#guard_msgs` and make the outer wrapper require the guarded file to exit
zero. Record the semantic purpose and fixture path under
`:expected-rejection` in `checks/witness-registry.edn`; do not duplicate the
rendered diagnostic there. Import, syntax, extra-diagnostic, and mismatch drift
must turn the focused check red.

EDN/data mutations and direct semantic predicates do not use `#guard_msgs`:
their named predicate returning false is already the executable rejection
reason. They remain in the full mutation inventory, but do not require manual
compiler-diagnostic inspection.

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
make certify-run RUN_ID=<uuid-from-TickRunRecord> \
  TESTED_JOB_ID=<futon2-ci-job-id-from-tested-commit> FENCE_ID=<quiet-run-id>
```

The command deliberately does not guess “latest”. It locates the unique
`tick-run-record-*.edn` carrying that UUID, locates the unique bounded-wrapper
receipt whose interval encloses that tick, stamps the normalized resource
input with the same UUID, writes the operational certificate, and prints the
topology hashes, traversal counts, resource status, and verdict. Missing or
ambiguous run/receipt evidence is a loud failure naming the paths searched.
This command certifies an already completed run; it does not initiate one.

After certification, inspect the run's Morning Brief and record the operator
QA verdicts:

```sh
clojure -M:wm-full-loop brief --attempt-id ATTEMPT_ID
clojure -M:wm-full-loop review ATTEMPT_ID joe
```

`review` is the guided entry surface. It shows the evidence for each still-open
objective, validates the answer vocabulary, requires a nonblank evidence note,
and appends one immutable review record per answer under
`data/wm-morning-brief/reviews/`. For a successful grounded run the four
objectives and answers are:

- `feature-verdict`: `accept-feature`, `accept-with-follow-ups`, or `reject`.
- `selection-quality`: `yes`, `no`, or `uncertain`.
- `substantive-achievement`: `yes`, `partial`, `no`, or `uncertain`.
- `evidence-sufficiency`: `sufficient`, `insufficient`, or `uncertain`.

For non-interactive use, submit each answer separately:

```sh
clojure -M:wm-full-loop qa ATTEMPT_ID OBJECTIVE ANSWER "EVIDENCE NOTE" joe
```

Only `substantive-achievement` projects an independent-weight evidence event
into the next belief update. The other three remain explicit operator and audit
judgments. If QA is not recorded, the Morning Brief item remains pending and
`brief` continues to list its unanswered objectives; there is currently no
expiry or automatic failure. In that case no substantive-achievement event is
available, so the machine does not learn Joe's judgment from that run. `make
status` surfaces this as `DECISION-DUE` and prints the attempt id plus every
outstanding objective.

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
