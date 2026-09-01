# C319 — canonical quiet-run execution sheet

Date: 2026-09-01. Operator coordinator: `claude-20`. Reload/click operator:
Joe. **Execute from this document only.** C247, C305, C309, C313, and C317
remain the reasoning/provenance record and are not parallel checklists.

No command in this sheet was executed while it was assembled.

## 0. Ask for and record the fence

Say to Joe verbatim:

> Joe — may I have a fenced War Machine operator window? Please keep Futon3c
> running but park the three durable coordinators and five background units
> listed below. I need a 60-minute planning reservation beginning when all
> report parked. Preparation to READY measured 6m24s and is budgeted at 10
> minutes; reload and live author/reviewer latency are unbounded, so I will send
> `FENCE-RELEASE` explicitly after certification or an orderly abort. Please do
> not resume anything before that message.

Freeze dispatch. Obtain and record `NO-WRITE/NO-JOB UNTIL FENCE-RELEASE` from
all four WM lanes, `claude-1`, Joe, and every other session retaining workspace
write authority. Choose and retain one identifier, for example
`FENCE_ID=wm-quiet-YYYYMMDDTHHMMSSZ`. Create
`/tmp/$FENCE_ID-attestations.json` with these exact true fields; the recorded
acknowledgements, not the booleans alone, are its authority:

```sh
FENCE_ID=wm-quiet-YYYYMMDDTHHMMSSZ
cat > "/tmp/$FENCE_ID-attestations.json" <<'EOF'
{"operator-no-workspace-write":true,
 "dispatch-frozen":true,
 "publisher-paused":true,
 "sessions-reconciled":true,
 "coordinators-not-resumed-before-release":true}
EOF
```

Capture the actual pre-fence manifest before changing it:

```sh
date -u +%FT%TZ
systemctl --user list-timers --all --no-pager
systemctl --user list-units --type=service --state=running --no-pager
systemctl --user show \
  apm-campaign-babysit-jit-all-open-v2.service \
  apm-watchdog.timer apm-watchdog.service apm-closer.service \
  apm-axiom-audit.timer apm-axiom-audit.service \
  futon-pattern-index.timer futon-pattern-index.service \
  -p Id -p ActiveState -p SubState -p InvocationID \
  -p NextElapseUSecRealtime --no-pager
ps -eo pid,ppid,lstart,args | rg -i 'watch|timer|cron|generator|publish|apm|bg\.py'
python3 /home/joe/code/futon3c/scripts/bg.py list
python3 /home/joe/code/futon3c/scripts/bg.py test-list
```

Also capture each coordinator's registration, durable status/epoch/tick claim,
and process-local runtime. Do not infer these from `:coordinator/enabled?`:

```sh
cd /home/joe/code/futon3c
for id in jit-queue:jit-m94A03-retry-v3 jit-queue:jit-all-open-v2 ftriangle-live-smoke-v1; do
  scripts/proof-eval.sh "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/status \"data/apm-coordinators/registry.edn\" \"$id\"))"
done
```

Create an initially empty `/tmp/$FENCE_ID-restore.actions`. Append an allowlisted
undo action only **after** its corresponding park command succeeds. This is the
authoritative partial-parking journal; never populate it in advance.

Expected: an observed manifest, not a pass. Any new process with an unclassified
write set is `write-set-unknown`: STOP until its owner and write set are named.

## 1. Joe parks the background writers

Joe runs from the canonical serving-JVM surface. The completed/no-runtime
coordinator parks only its independent watchdog; its terminal state remains
untouched:

```sh
cd /home/joe/code/futon3c
scripts/proof-eval.sh '(do (require (quote futon3c.apm.semantic-progress-watchdog)) (futon3c.apm.semantic-progress-watchdog/stop! "semantic-progress:jit-queue:jit-m94A03-retry-v3"))'

for id in 'jit-queue:jit-all-open-v2' 'ftriangle-live-smoke-v1'
do
  printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/stop! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"$id\"))" \
    | scripts/proof-eval.sh -
done

systemctl --user stop apm-watchdog.timer
# Wait for an active watchdog invocation and closer work to finish.
systemctl --user is-active apm-watchdog.service apm-closer.service
systemctl --user stop apm-closer.service
systemctl --user stop apm-axiom-audit.timer
systemctl --user stop futon-pattern-index.timer
# Wait for either active paired service to finish.
systemctl --user is-active apm-axiom-audit.service futon-pattern-index.service
systemctl --user stop apm-campaign-babysit-jit-all-open-v2.service
```

Expected for each running coordinator: `:ok true`, `:durably-disabled? true`, and
`:status :stopped` with a quiescence witness. `:draining` is not success; wait
for its in-flight tick and observe again. Let already-running closer/audit/index
work finish rather than killing it, then explicitly stop
`apm-closer.service`: its `Restart=always` can restart it without the watchdog.
Never stop `futon3c-zone.service`.

**Restoration precondition:** stopping a coordinator is allowed only when its
pre-state has a reversible activation intent. `resume!` is not an inverse for a
durable `:complete` state: it invokes `continue-complete!` and can create new
work. An enabled-but-complete coordinator with no runtime cannot tick, but its
independent semantic watchdog is still a writer. Park only that watchdog.
Record it as `rearm-terminal-coordinator<TAB>ID`; restoration uses
`start-registered!`, which re-arms the watchdog while preserving terminal
`:complete`.

After each successful park, append exactly the corresponding allowlisted action,
for example `resume-coordinator<TAB>ID` or `start-unit<TAB>UNIT`, to
`/tmp/$FENCE_ID-restore.actions`. A failed or merely attempted park gets no row.

## 2. Coordinator independently verifies the fence

```sh
cd /home/joe/code/futon2
python3 checks/writer_fence_evidence.py \
  --attestations /tmp/$FENCE_ID-attestations.json \
  > /tmp/$FENCE_ID-evidence-01.json
fence_exit=$?
cat /tmp/$FENCE_ID-evidence-01.json
test "$fence_exit" -eq 0
```

Expected script exit 0 and top-level verdict `FENCE-VERIFIABLE`. Its two
endpoint captures must agree and show: all three coordinators durably stopped
with no runtime scheduler/tick claim and a same-epoch quiescence witness;
paired timers/services and closer inactive; no writable handle beneath the
five repositories; and C292 `QUIESCENT` with five clean repositories, four idle
lanes, and no jobs. `FENCE-BREACH` (1) and `FENCE-INDETERMINATE` (3) both refuse
the window. Record `FENCE-HELD <UTC> $FENCE_ID` only after this verdict and the
underlying owner acknowledgements agree.

Important boundary: C292 verifies Git, lane rows, and job lists. It does **not**
see embedded APM coordinators, ignored writable handles, editors, or promises
not to resume. `writer_fence_evidence.py` adds coordinator/unit/handle
observations, but future writer absence and owner promises remain attested.

Make the interval claim visible to WM preflight:

```sh
clojure -M:wm-preflight --writer-fence "$FENCE_ID"
```

Expected observation: `:writer-fence {:status :held :id "$FENCE_ID"}` and
`:event-free? true`. Where a mission ID is supplied, readiness must say
`READY (FENCE-CONDITIONAL $FENCE_ID)`, not unfenced `READY-CONTENT-ONLY
(event-free unverified)`. This declaration names the already established
fence; it does not acquire one.

## 3. Run the bounded workspace gate

```sh
cd /home/joe/code/futon2
FUTON_WRITER_FENCE_ID="$FENCE_ID" make workspace-gate
```

Expected: terminal bounded receipt with inner exit 0, outer exit 0, verdict
`pass`, resource status `clean`, stable clean repository bases, gate failures
zero, `contract-authority-current` labelled
`PASS (FENCE-CONDITIONAL $FENCE_ID)`, and `workspace-gate: script-exit=0`.
The environment value names the already-established fence; it does not acquire
one. Read the named script exit; a
failing Make recipe itself reports exit 2 and must not be mistaken for the
house mutation-slipped meaning.

Immediately rerun the step-2 evidence command to a new numbered `/tmp` file.
Any `repository-basis-changed`, moved/unavailable
basis, new process/job, or missing acknowledgement is `FENCE-BREACH`.

## 4. Produce settled suite receipts

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'clojure -T:build ci' --agent quiet-window --label quiet-futon2-ci \
  --dir /home/joe/code/futon2 --window production

python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'clojure -X:test' --agent quiet-window --label quiet-futon3-suite \
  --dir /home/joe/code/futon3 --window production
```

Run them one at a time. For each returned ID:

```sh
python3 /home/joe/code/futon3c/scripts/bg.py test-status JOB_ID
```

Expected for each terminal receipt: inner 0, outer 0, `verdict=pass`, clean
resource status, and identical clean start/finish repository bases matching
step 2. Then rerun the step-2 evidence command.

## 5. Release and perform Joe's reload

```sh
cd /home/joe/code/futon2
make runner-reload-preflight
```

Expected: six passing preconditions, `READY`, the reload command printed rather
than withheld, and `runner-reload-preflight: script-exit=0`.

Say to Joe verbatim:

> The fence is held and the settled gate and suite receipts pass. Please run
> the released canonical reload command now. Do not click yet; I will verify
> the loaded identity and say READY separately.

Joe runs exactly the command released by preflight, expected to be:

```sh
cd /home/joe/code/futon3c && \
clojure -M:dev-admin load-file \
  /home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj
```

The reload mutates serving-JVM state. Immediately rerun the step-2 evidence
command, then run step 6.

## 6. Establish READY without rerunning the click

```sh
python3 /home/joe/code/futon3c/scripts/bg.py launch-test \
  'make run-readiness' --agent quiet-window --label post-reload-readiness \
  --dir /home/joe/code/futon2 --window measurement
python3 /home/joe/code/futon3c/scripts/bg.py test-status JOB_ID
```

Expected outer receipt: pass/clean/stable. Expected inner report: all named
items pass, loaded serving-runner identity is available, clean, and equals the
tested Futon2 commit; verdict `READY`; `run-readiness: script-exit=0`; an
available reviewer is selected. `NOT-READY (waiting)` and `NOT-READY
(needs-you)` are both refusal verdicts even if Make's wrapper prints exit 2.

Rerun the step-2 evidence command one final time before the click.

## 7. Joe performs the observed production click

Say to Joe verbatim:

> READY is verified for the loaded tested commit. Please run the external click
> observer below with reviewer `<SELECTED>`. It performs the one production
> click. Do not use bare curl and do not retry for a cleaner result.

Joe chooses a new receipt filename and runs:

```sh
cd /home/joe/code/futon2
bb -cp . checks/wm_click_resource_observer.clj \
  holes/labs/wm-contract/wm-click-resource-YYYYMMDDTHHMMSS.receipt.json \
  SELECTED
```

Expected: the observer starts before the click and ends after it; scope is
`shared-serving-jvm`; exact click ID and run ID agree with terminal status and
the topology-bearing `TickRunRecord`; resource observation is readable/clean;
execution is terminal. Save the exact printed run ID. This phase authorises
only that binding, run record, trace/store output, observer receipt, and final
certificate. Any other write is a breach.

## 8. Certify the exact run

```sh
cd /home/joe/code/futon2
make certify-run RUN_ID=<exact-uuid-printed-by-observer>
```

Expected: unique run and receipt, valid fixture pins, matching identities,
temporal enclosure, zero undeclared topology hops, clean resource status,
complete execution, certificate `:verdict :pass`, and command exit 0. A
certificate may instead honestly say `:incomplete` or `:fail`; preserve it and
do not click again.

## 9. Hand off evidence and release the fence

Say to Joe verbatim with actual values substituted:

> The operator run is complete. These verdicts were produced under the C305
> writer fence held from `<FENCE-HELD UTC>` through `<FENCE-RELEASE UTC>`. The
> attached manifest names every acknowledged writer and parked
> coordinator/timer; C292 reported five clean repositories, four idle lanes,
> and zero ordinary/bounded jobs at each pre-click checkpoint. Gate and suite
> receipts recorded stable start/finish content bases, and no fence breach was
> observed. The production phase allowed only the exact click/run-bound outputs
> named in certificate `<PATH>`, whose verdict is `<VERDICT>`. This claim is
> conditional on that declared boundary. Seventy-five lexical hybrid-window
> candidates remain unaudited, so it does not claim that an undeclared external
> mutable input is impossible. FENCE-RELEASE: the pre-fence background manifest
> may now be restored.

Joe restores only entries both changed according to
`/tmp/$FENCE_ID-restore.actions` and recorded with reversible active intent in
step 0. Do not blanket-resume the three coordinator IDs. Execute allowlisted
actions in reverse parking order. For a running coordinator, use `resume!` only when
the captured durable pre-state was genuinely running; `enabled? true` alone is
insufficient. For a unit, start it only when its captured `ActiveState` was
active/activating.

```sh
cd /home/joe/code/futon3c
nl -ba "/tmp/$FENCE_ID-restore.actions"
# Read the journal bottom-to-top. For each resume-coordinator row whose captured
# durable pre-state was :running:
printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/resume! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"COORDINATOR_ID\"))" | scripts/proof-eval.sh -
# For each start-unit row whose captured ActiveState was active/activating:
systemctl --user start UNIT
# For a rearm-terminal-coordinator row, after reconfirming durable :complete
# and no runtime scheduler:
printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/start-registered! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"COORDINATOR_ID\"))" | scripts/proof-eval.sh -
```

Coordinator verifies restored states with the same serving-JVM `status` forms
and `systemctl show` command captured in step 0. Expected values equal the
pre-fence **activation intent**, not byte/state identity: a resumed coordinator
may have a new epoch/runtime; a restarted unit has a new InvocationID; a timer
may compute a new next elapse. Record those transitions. Exact epoch, process,
and timer-schedule restoration is impossible. Do not resume something that was
complete, draining, stopped, disabled, or inactive in the manifest.

### Emergency restoration if the coordinator session disappears

Joe can restore a partially parked window without this session:

1. Set the recorded `FENCE_ID`; preserve its attestations, pre-state output, and
   `restore.actions` file.
2. Run `nl -ba /tmp/$FENCE_ID-restore.actions`. Only rows present were actually
   parked; never restore a name merely because it appears elsewhere in this
   document.
3. Work bottom-to-top using the allowlisted command forms above. Refuse a
   coordinator resume unless its captured durable pre-status was `:running`.
   A terminal re-arm requires captured and current `:complete` and uses
   `start-registered!`, never `resume!`.
4. Re-run the coordinator `status` loop and `systemctl show` from step 0.
   Success means restored activation intent plus explicit new epoch/InvocationID,
   not equality with the old runtime state.
5. Announce `FENCE-RELEASE (emergency restoration) <FENCE_ID>` and retain the
   manifest/journal. Any failed undo remains named and is escalated to that
   writer's owner; do not compensate by starting every listed writer.

## Abort/undo table

| Last completed phase | Action |
|---|---|
| Before parking | Clean abort; nothing to undo. |
| Parked, before reload | Preserve failed receipts, say `FENCE-RELEASE (aborted before reload)`, execute the step-9 manifest restoration commands, and confirm with coordinator `status` plus `systemctl show`. A retry starts at step 0. |
| Reloaded, before click | Serving code need not be rolled back. Execute and verify step-9 restoration. On retry, reuse the reload only if `make run-readiness` records its Futon2 identity equal to the newly tested settled commit; otherwise obtain a new preflight and Joe reloads again. |
| Click started or terminal | Never click again merely to clean evidence. Preserve binding, run record, observer envelope, trace, and logs. Run `make certify-run RUN_ID=<exact-id>` if exact evidence permits; otherwise record incomplete/fail/unavailable, then execute and verify step-9 restoration. |
| Certificate persisted | The evidence is immutable for that exact run. Deliver it, release, and confirm restoration. |

Any stable-basis predicate failure is “stop and fix before another window.” Any
basis/writer movement is “window closed; restart from step 0.”

## Evidence authority

Machine-verified: Git HEAD/status and basis movement, lane-registry contents,
ordinary/bounded job listings and receipts, persisted coordinator enabled/state
records, timer/service state, loaded runner identity, click/run identities,
resource envelope, topology route, and certificate predicates.

Operator-attested: no editor/manual session will write later; every writer
owner remains frozen; inspected watcher scripts retain the classified bytes and
configuration; no unknown future process opens a write path; the fence is held
between observations. C292 cannot prove those promises. The certificate
context must retain both lists rather than presenting the attestations as
machine observations.

## Source record

- C247: sequence and measured duration.
- C305: writer fence, checkpoints, restart semantics, ABA limit.
- C309: live scheduled-writer census and write sets.
- C313: verified park/resume operator request.
- C314: adversarial boundary—`write-set-unknown` is unverified, not empty.
- C317: conditional meaning and residual hybrid-window limitation.
- C318/C321: observed/attested/unverifiable fence bundle and runnable command.
