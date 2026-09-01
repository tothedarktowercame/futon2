# C319 — quiet-run narrative record (superseded execution surface)

**Do not execute this document.** C371 replaced it with the receipt-consuming
state machine `scripts/wm_quiet_run_state.py`; see
`C371-quiet-run-state-machine.md`. This file remains the narrative and recovery
provenance that led to the machine.

Date: 2026-09-01. Operator coordinator: `claude-20`. Reload/click operator:
Joe. **Execute from this document only.** C247, C305, C309, C313, and C317
remain the reasoning/provenance record and are not parallel checklists.

No command in this sheet was executed while it was assembled.

## 0. Ask for and record the fence

Say to Joe verbatim:

> Joe — may I have a fenced War Machine operator window? Please keep Futon3c
> running. Park only the semantic watchdog for completed coordinator
> `jit-queue:jit-m94A03-retry-v3`; durably park the two running coordinators
> `jit-queue:jit-all-open-v2` and `ftriangle-live-smoke-v1`; and park the five
> background units listed below. I need a 60-minute planning reservation when all
> report parked. Preparation to READY measured 6m24s and is budgeted at 10
> minutes; reload and live author/reviewer latency are unbounded, so I will send
> `FENCE-RELEASE` explicitly after certification or an orderly abort. Please do
> not resume anything before that message.

Freeze dispatch. Obtain and record `NO-WRITE/NO-JOB UNTIL FENCE-RELEASE` from
exactly the principals represented below: all four WM lanes, publisher
`claude-1`, dispatch coordinator `claude-20`, and Joe. Any additional session
with workspace write authority must first be added by name to `sessions`; an
unnamed promise is not part of the fence. Choose and retain one identifier, for example
`FENCE_ID=wm-quiet-YYYYMMDDTHHMMSSZ`. Create
`/tmp/$FENCE_ID-attestations.json` with these exact true fields; the recorded
acknowledgements, not the booleans alone, are its authority:

```sh
FENCE_ID=wm-quiet-YYYYMMDDTHHMMSSZ
ISSUED_AT=$(date -u +%FT%TZ)
EXPIRES_AT=$(date -u -d '+60 minutes' +%FT%TZ)
cat > "/tmp/$FENCE_ID-attestations.json" <<EOF
{"schema":"wm-writer-fence-attestation-v1",
 "fence-id":"$FENCE_ID",
 "issued-at":"$ISSUED_AT",
 "expires-at":"$EXPIRES_AT",
 "acknowledged-by":{"operator":"joe","dispatch-coordinator":"claude-20",
  "publisher":"claude-1","sessions":["wm-nouns","wm-verbs","wm-organization","wm-evidence"]},
 "writer-population":{"coordinators":["jit-queue:jit-m94A03-retry-v3","jit-queue:jit-all-open-v2","ftriangle-live-smoke-v1"],
  "units":["apm-campaign-babysit-jit-all-open-v2.service","apm-watchdog.timer","apm-watchdog.service","apm-closer.service","apm-axiom-audit.timer","apm-axiom-audit.service","futon-pattern-index.timer","futon-pattern-index.service"]},
 "intended-state":{"coordinators":{"jit-queue:jit-m94A03-retry-v3":"terminal-complete-watchdog-stopped","jit-queue:jit-all-open-v2":"durably-stopped","ftriangle-live-smoke-v1":"durably-stopped"},
  "units":"inactive","writable-handles":"none","c292":"QUIESCENT"}}
EOF
```

Capture the actual pre-fence manifest before changing it:

```sh
RESTORE_KEY=/home/joe/.config/futon/writer-fence-restore.key
test -r "$RESTORE_KEY"
test "$(stat -c %a "$RESTORE_KEY")" = 600
python3 /home/joe/code/futon2/scripts/writer_fence_restore.py capture \
  --fence-id "$FENCE_ID" \
  --key-file "$RESTORE_KEY" \
  --manifest "/tmp/$FENCE_ID-restore-manifest.json"
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
  scripts/proof-eval.sh "(do (require 'futon3c.apm.durable-coordinator 'futon3c.apm.semantic-progress-watchdog) {:coordinator (futon3c.apm.durable-coordinator/status \"data/apm-coordinators/registry.edn\" \"$id\") :watchdog-running? (boolean (futon3c.apm.semantic-progress-watchdog/running? (str \"semantic-progress:\" \"$id\")))})"
done
```

The capture creates the structured, digest-bound restoration authority. The
journal `/tmp/$FENCE_ID-restore.actions.jsonl` does not exist yet; the tool
creates it only after observing a successful park.

Expected: an observed manifest, not a pass. Any new process with an unclassified
write set is `write-set-unknown`: STOP until its owner and write set are named.

## 1. Joe parks the background writers

Joe runs from the canonical serving-JVM surface. The completed/no-runtime
coordinator parks only its independent watchdog; its terminal state remains
untouched:

```sh
cd /home/joe/code/futon3c
wait_inactive () {
  unit=$1 deadline=$((SECONDS + 600))
  while [ "$(systemctl --user show "$unit" -p ActiveState --value)" != inactive ]; do
    [ "$SECONDS" -lt "$deadline" ] || { echo "SERVICE-SETTLE-TIMEOUT $unit"; return 1; }
    sleep 2
  done
}
require_stably_inactive () {
  unit=$1
  first=$(systemctl --user show "$unit" -p ActiveState -p SubState -p InvocationID -p NRestarts)
  sleep 5
  second=$(systemctl --user show "$unit" -p ActiveState -p SubState -p InvocationID -p NRestarts)
  [ "$first" = "$second" ] && printf '%s\n' "$second" | grep -qx 'ActiveState=inactive' \
    || { echo "SERVICE-NOT-STABLY-INACTIVE $unit"; return 1; }
}
scripts/proof-eval.sh '(do (require (quote futon3c.apm.semantic-progress-watchdog)) (futon3c.apm.semantic-progress-watchdog/stop! "semantic-progress:jit-queue:jit-m94A03-retry-v3"))'
python3 /home/joe/code/futon2/scripts/writer_fence_restore.py record \
  --fence-id "$FENCE_ID" --key-file "$RESTORE_KEY" \
  --manifest "/tmp/$FENCE_ID-restore-manifest.json" \
  --journal "/tmp/$FENCE_ID-restore.actions.jsonl" \
  --action rearm-terminal-coordinator --target jit-queue:jit-m94A03-retry-v3

for id in 'jit-queue:jit-all-open-v2' 'ftriangle-live-smoke-v1'
do
  printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/stop! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"$id\"))" \
    | scripts/proof-eval.sh -
done

systemctl --user stop apm-watchdog.timer
# Let an active watchdog invocation finish. The closer has Restart=always, so
# stop it explicitly; systemctl waits for the stop job, and the interval check
# below proves it did not immediately restart.
wait_inactive apm-watchdog.service
systemctl --user stop apm-closer.service
systemctl --user stop apm-axiom-audit.timer
systemctl --user stop futon-pattern-index.timer
# Wait for either active paired service to finish.
wait_inactive apm-axiom-audit.service
wait_inactive futon-pattern-index.service
systemctl --user stop apm-campaign-babysit-jit-all-open-v2.service
for unit in apm-watchdog.timer apm-watchdog.service apm-closer.service \
  apm-axiom-audit.timer apm-axiom-audit.service \
  futon-pattern-index.timer futon-pattern-index.service \
  apm-campaign-babysit-jit-all-open-v2.service
do
  require_stably_inactive "$unit" || exit 1
done
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

After each running coordinator reaches witnessed `:stopped`, invoke `record`
with `--action resume-coordinator --target ID`. After each unit stop that changed
a pre-manifest active/activating unit to inactive, invoke it with `--action
start-unit --target UNIT`. Use the same manifest and JSONL paths shown above.
The tool refuses inactive pre-state, an unobserved park, a duplicate target, a
swapped verb, or a mismatched manifest. Never edit the journal by hand.

## 2. Coordinator independently verifies the fence

```sh
cd /home/joe/code/futon2
python3 checks/writer_fence_evidence.py \
  --fence-id "$FENCE_ID" \
  --attestations /tmp/$FENCE_ID-attestations.json \
  > /tmp/$FENCE_ID-evidence-01.json
fence_exit=$?
cat /tmp/$FENCE_ID-evidence-01.json
test "$fence_exit" -eq 0
```

Expected script exit 0 and top-level verdict `FENCE-VERIFIABLE`. Its two
endpoint captures must agree and show: the completed coordinator remains
durably `:complete`, with no regulator runtime/tick claim and its watchdog
stopped; the two running coordinators are durably stopped with no regulator or
watchdog scheduler/tick claim and a same-epoch quiescence witness;
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
clojure -M:wm-preflight \
  --writer-fence "$FENCE_ID" \
  --writer-fence-evidence "/tmp/$FENCE_ID-evidence-01.json"
```

Expected observation: `:writer-fence {:status :observed-held :id "$FENCE_ID" ...}`
and `:event-free? true`. Preflight reruns the independent evidence checker using
the receipt's structured attestation; the receipt name alone is not proof.
Where a mission ID is supplied, readiness must say
`READY (FENCE-VERIFIED $FENCE_ID)`, not unfenced `READY-CONTENT-ONLY
(event-free unverified)`. This declaration names the already established
fence; it does not acquire one.

## 3. Run the bounded workspace gate

```sh
cd /home/joe/code/futon2
FUTON_WRITER_FENCE_ID="$FENCE_ID" \
FUTON_WRITER_FENCE_EVIDENCE="/tmp/$FENCE_ID-evidence-01.json" \
make workspace-gate
```

Expected: terminal bounded receipt with inner exit 0, outer exit 0, verdict
`pass`, resource status `clean`, stable clean repository bases, gate failures
zero, `contract-authority-current` labelled
`PASS (FENCE-CONDITIONAL $FENCE_ID)`, and `workspace-gate: script-exit=0`.
The environment value names the already-established fence; it does not acquire
one. The bounded summary must display `reason`, `repository-basis-start`,
`repository-basis-finish`, and `repository-basis-stable`; inspect those fields,
not only the inner gate log. Read the named script exit; a
failing Make recipe itself reports exit 2 and must not be mistaken for the
house mutation-slipped meaning.

Immediately rerun the step-2 evidence command to a new numbered `/tmp` file.
Any `repository-basis-changed`, moved/unavailable
basis, new process/job, or missing acknowledgement is `FENCE-BREACH`.

## 4. Produce settled suite receipts

```sh
launch_bounded () {
  output=$(python3 /home/joe/code/futon3c/scripts/bg.py launch-test "$@") || return 1
  printf '%s\n' "$output" >&2
  printf '%s' "$output" | python3 -c 'import json,sys; x=json.load(sys.stdin); assert x.get("ok"); print(x["value"]["id"])'
}
await_bounded () {
  id=$1 deadline=$((SECONDS + 2700))
  while [ "$SECONDS" -lt "$deadline" ]; do
    status=$(python3 /home/joe/code/futon3c/scripts/bg.py test-status "$id") || return 1
    terminal=$(printf '%s' "$status" | python3 -c 'import json,sys; x=json.load(sys.stdin); print("yes" if x and x.get("receipt") else "no")')
    if [ "$terminal" = yes ]; then
      printf '%s\n' "$status"
      printf '%s' "$status" | python3 -c 'import json,sys; x=json.load(sys.stdin); raise SystemExit(0 if x["receipt"].get("outer-exit") == 0 else 1)' \
        || { echo "BOUNDED-JOB-FAILED $id"; return 1; }
      return 0
    fi
    sleep 5
  done
  echo "BOUNDED-JOB-TIMEOUT $id"; return 1
}
FUTON2_JOB_ID=$(launch_bounded \
  'clojure -T:build ci' --agent quiet-window --label quiet-futon2-ci \
  --dir /home/joe/code/futon2 --window production) || exit 1
await_bounded "$FUTON2_JOB_ID" || exit 1
```

Only after that receipt is terminal and accepted, launch Futon3:

```sh
FUTON3_JOB_ID=$(launch_bounded \
  'clojure -X:test' --agent quiet-window --label quiet-futon3-suite \
  --dir /home/joe/code/futon3 --window production) || exit 1
await_bounded "$FUTON3_JOB_ID" || exit 1
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
READINESS_JOB_ID=$(launch_bounded \
  'make run-readiness' --agent quiet-window --label post-reload-readiness \
  --dir /home/joe/code/futon2 --window measurement) || exit 1
await_bounded "$READINESS_JOB_ID" || exit 1
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

If the observer returns `click-status-unavailable`, do not restore writers.
Use the click ID persisted in its receipt and run this read-only recovery
observation:

```sh
RECEIPT=holes/labs/wm-contract/wm-click-resource-YYYYMMDDTHHMMSS.receipt.json
export CLICK_ID=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["click-id"])' "$RECEIPT")
deadline=$((SECONDS + 900)); decision=CONTINUE-WAITING
while [ "$SECONDS" -lt "$deadline" ]; do
  status=$(curl -fsS http://127.0.0.1:7070/api/alpha/wm/click) || { sleep 5; continue; }
  decision=$(printf '%s' "$status" | python3 -c '
import json,sys,os
x=json.load(sys.stdin); wanted=os.environ["CLICK_ID"]
last=x.get("last-result")
if x.get("click-id") != wanted: print("IDENTITY-MISMATCH")
elif x.get("running?") is True: print("CONTINUE-WAITING")
elif isinstance(last,dict) and last.get("outcome") and (last.get("run-id-observation") or {}).get("value"):
 print("TERMINAL " + str(last["run-id-observation"]["value"]))
else: print("TERMINAL-EVIDENCE-ABSENT")')
  printf '%s\n' "$decision"
  case "$decision" in TERMINAL\ *) break;; IDENTITY-MISMATCH|TERMINAL-EVIDENCE-ABSENT) exit 1;; esac
  sleep 5
done
[ "${decision#TERMINAL }" != "$decision" ] || { echo 'ABORT-WITH-WRITERS-PARKED'; exit 1; }
```

`CONTINUE-WAITING` is not release authority. `ABORT-WITH-WRITERS-PARKED`,
identity mismatch, absent terminal evidence, or an unreadable endpoint requires
Joe to inspect the serving JVM; do not restore. Only an exact typed terminal
result permits step 8, and release still requires certification plus the
post-click writer observation below.

## 8. Certify the exact run

```sh
cd /home/joe/code/futon2
make certify-run RUN_ID=<exact-uuid-printed-by-observer> \
  TESTED_JOB_ID=<futon2-ci-job-id-recorded-at-tested-commit>
```

Expected: unique run and receipt, valid fixture pins, matching identities,
temporal enclosure, zero undeclared topology hops, clean resource status,
complete execution, certificate `:verdict :pass`, and command exit 0. A
certificate may instead honestly say `:incomplete` or `:fail`; preserve it and
do not click again.

## 9. Hand off evidence and release the fence

First observe the parked writer population again across an interval:

```sh
python3 checks/writer_fence_evidence.py \
  --fence-id "$FENCE_ID" \
  --attestations "/tmp/$FENCE_ID-attestations.json" \
  --writer-state-only > "/tmp/$FENCE_ID-evidence-post-click.json"
post_fence_exit=$?
cat "/tmp/$FENCE_ID-evidence-post-click.json"
test "$post_fence_exit" -eq 0
grep -q 'WRITERS-STILL-PARKED' "/tmp/$FENCE_ID-evidence-post-click.json"
echo "RELEASE-AUTHORISED $FENCE_ID"
```

This post-click mode deliberately does not claim clean repositories or zero
jobs: the authorised run has created evidence. It proves only that the named
coordinators and units remain parked and no repository writable handle survived
the two observations. Any other result keeps the fence held.

Say to Joe verbatim with actual values substituted:

> The operator run is complete. These verdicts were produced under the C305
> writer fence held from `<FENCE-HELD UTC>` through `<FENCE-RELEASE UTC>`. The
> attached manifest names every acknowledged writer and parked
> coordinator/timer; C292 reported five clean repositories, four idle lanes,
> and zero ordinary/bounded jobs at each pre-click checkpoint. A post-click
> interval observation reported `WRITERS-STILL-PARKED` for the named writer
> population; it did not reassert pre-click repository cleanliness. Gate and suite
> receipts recorded stable start/finish content bases, and no breach of the
> named parked-writer population was observed. The production phase allowed only the exact click/run-bound outputs
> named in certificate `<PATH>`, whose verdict is `<VERDICT>`. This claim is
> conditional on that declared boundary. The gate-time mutable-input
> reconciliation classified every then-current member as content, event, or
> non-verdict library, with zero unexplained; see its revision-bound report
> rather than a copied count. This does not claim that an undeclared external
> mutable input is impossible. FENCE-RELEASE: the pre-fence background manifest
> may now be restored.

Joe restores only typed, successfully observed journal entries. The tool
validates manifest digest, journal order, action class, captured pre-state, and
current parked state before acting, then restores the partial prefix in reverse.

```sh
python3 /home/joe/code/futon2/scripts/writer_fence_restore.py restore \
  --fence-id "$FENCE_ID" --key-file "$RESTORE_KEY" \
  --manifest "/tmp/$FENCE_ID-restore-manifest.json" \
  --journal "/tmp/$FENCE_ID-restore.actions.jsonl" \
  --outcomes "/tmp/$FENCE_ID-restore.outcomes.jsonl"
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

1. Set the recorded `FENCE_ID`; preserve its attestations, structured restore
   manifest, JSONL journal, outcome ledger, inverse-attempt ledger
   (`$FENCE_ID-restore.outcomes.jsonl.attempts.jsonl`), and the locally held
   owner-only authentication key. The fence ID supplied to `restore` must match
   every record.
2. Run the single `writer_fence_restore.py restore` command above. Do not edit
   the journal or select a verb manually. A missing/empty journal reports
   `NOTHING-RECORDED`; a mismatch refuses and leaves remaining writers parked.
   A retry verifies and skips only inverses already recorded successful in the
   append-only outcome ledger.
3. The tool selects `start-registered!` only for captured/current terminal
   `:complete`, and `resume!` only for captured-running/current-witnessed-stopped.
4. Re-run the coordinator `status` loop and `systemctl show` from step 0.
   Success means restored activation intent plus explicit new epoch/InvocationID,
   not equality with the old runtime state.
5. Announce `FENCE-RELEASE (emergency restoration) <FENCE_ID>` and retain the
   manifest/journal. Any failed undo remains named and is escalated to that
   writer's owner; do not compensate by starting every listed writer.

## Abort/undo table

| Last completed phase | Action |
|---|---|
| After acknowledgements, before parking | Mark `/tmp/$FENCE_ID-attestations.json` aborted, announce `FENCE-RELEASE (aborted before parking)`, release dispatch/publisher/session promises, and retain the file. No writer-state undo is needed. |
| Parked, before reload | Preserve failed receipts, say `FENCE-RELEASE (aborted before reload)`, execute the step-9 manifest restoration commands, and confirm with coordinator `status` plus `systemctl show`. A retry starts at step 0. |
| Reloaded, before click | Serving code need not be rolled back. Execute and verify step-9 restoration. On retry, reuse the reload only if `make run-readiness` records its Futon2 identity equal to the newly tested settled commit; otherwise obtain a new preflight and Joe reloads again. |
| Click started or terminal | Never click again merely to clean evidence. First require the observer/service to report a typed terminal outcome and no click write in flight. Then preserve binding, run record, envelope, trace, and logs; certify if exact evidence permits; and restore. If terminal state is unavailable or the click may still be active, keep every background writer parked and escalate to Joe—do not release or restore. |
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
- C360: interval settling, exact attestation population, bounded-job polling,
  click recovery observation, and post-click writer-state evidence.
- C318/C321: observed/attested/unverifiable fence bundle and runnable command.
