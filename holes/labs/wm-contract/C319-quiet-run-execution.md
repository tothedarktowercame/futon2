# C319 — canonical quiet-run execution sheet

Date: 2026-09-01. Operator coordinator: `claude-20`. Reload/click operator:
Joe. **Execute from this document only.** C247, C305, C309, C313, and C317
remain the reasoning/provenance record and are not parallel checklists.

No command in this sheet was executed while it was assembled.

## 0. Ask for and record the fence

Say to Joe verbatim:

> Joe — may I have a fenced War Machine operator window? Please keep Futon3c
> running but park the three durable coordinators and four background units
> listed below. I need a 60-minute planning reservation beginning when all
> report parked. Preparation to READY measured 6m24s and is budgeted at 10
> minutes; reload and live author/reviewer latency are unbounded, so I will send
> `FENCE-RELEASE` explicitly after certification or an orderly abort. Please do
> not resume anything before that message.

Freeze dispatch. Obtain and record `NO-WRITE/NO-JOB UNTIL FENCE-RELEASE` from
all four WM lanes, `claude-1`, Joe, and every other session retaining workspace
write authority. Capture the actual pre-fence manifest before changing it:

```sh
date -u +%FT%TZ
systemctl --user list-timers --all --no-pager
systemctl --user list-units --type=service --state=running --no-pager
ps -eo pid,ppid,lstart,args | rg -i 'watch|timer|cron|generator|publish|apm|bg\.py'
python3 /home/joe/code/futon3c/scripts/bg.py list
python3 /home/joe/code/futon3c/scripts/bg.py test-list
```

Expected: an observed manifest, not a pass. Any new process with an unclassified
write set is `write-set-unknown`: STOP until its owner and write set are named.

## 1. Joe parks the background writers

Joe runs from the canonical serving-JVM surface:

```sh
cd /home/joe/code/futon3c
for id in \
  'jit-queue:jit-m94A03-retry-v3' \
  'jit-queue:jit-all-open-v2' \
  'ftriangle-live-smoke-v1'
do
  printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/stop! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"$id\"))" \
    | scripts/proof-eval.sh -
done

systemctl --user stop apm-campaign-babysit-jit-all-open-v2.service
systemctl --user stop apm-watchdog.timer
systemctl --user stop apm-axiom-audit.timer
systemctl --user stop futon-pattern-index.timer
```

Expected per coordinator: `:ok true`, `:durably-disabled? true`, and
`:status :stopped` with a quiescence witness. `:draining` is not success; wait
for its in-flight tick and observe again. Let already-running closer/audit/index
jobs finish rather than killing them. Never stop `futon3c-zone.service`.

## 2. Coordinator independently verifies the fence

```sh
systemctl --user is-active \
  apm-campaign-babysit-jit-all-open-v2.service \
  apm-watchdog.timer apm-axiom-audit.timer futon-pattern-index.timer \
  apm-closer.service
systemctl --user list-timers --all --no-pager
python3 /home/joe/code/futon3c/scripts/bg.py list
python3 /home/joe/code/futon3c/scripts/bg.py test-list

cd /home/joe/code/futon2
python3 checks/quiescence_check.py
```

Expected: parked units inactive, no active/activating ordinary or bounded job,
and C292 exit 0 with JSON `"verdict": "QUIESCENT"`, five clean repositories,
four idle lanes, and stable state sandwich. Also inspect the three coordinator
registry/state records twice: enabled false, no tick claim, and a quiescence
witness. Record `FENCE-HELD <UTC>` only after every owner acknowledgement and
machine observation agrees.

Important boundary: C292 verifies Git, lane rows, and job lists. It does **not**
see embedded APM coordinators, ignored writable handles, editors, or promises
not to resume. Those remain operator-attested and are re-observed from the
manifest at every checkpoint.

## 3. Run the bounded workspace gate

```sh
cd /home/joe/code/futon2
make workspace-gate
```

Expected: terminal bounded receipt with inner exit 0, outer exit 0, verdict
`pass`, resource status `clean`, stable clean repository bases, gate failures
zero, and `workspace-gate: script-exit=0`. Read the named script exit; a
failing Make recipe itself reports exit 2 and must not be mistaken for the
house mutation-slipped meaning.

Immediately repeat step 2. Any `repository-basis-changed`, moved/unavailable
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
step 2. Then repeat step 2.

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

The reload mutates serving-JVM state. Immediately repeat the machine/owner
fence observations from step 2, then run step 6.

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

Repeat step 2 one final time before the click.

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

Joe restores only entries recorded active/enabled in step 0. For the observed
three coordinators and four units, if the actual manifest confirms them:

```sh
cd /home/joe/code/futon3c
for id in \
  'jit-queue:jit-m94A03-retry-v3' \
  'jit-queue:jit-all-open-v2' \
  'ftriangle-live-smoke-v1'
do
  printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/resume! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"$id\"))" \
    | scripts/proof-eval.sh -
done
systemctl --user start apm-watchdog.timer
systemctl --user start apm-axiom-audit.timer
systemctl --user start futon-pattern-index.timer
systemctl --user start apm-campaign-babysit-jit-all-open-v2.service
```

Coordinator verifies restored states. Do not resume something that was already
inactive in the captured manifest.

## Abort/undo table

| Last completed phase | Action |
|---|---|
| Before parking | Clean abort; nothing to undo. |
| Parked, before reload | Preserve failed receipts, say `FENCE-RELEASE (aborted before reload)`, restore the exact pre-fence manifest. A retry starts at step 0. |
| Reloaded, before click | Serving code need not be rolled back. Restore writers on abort. On retry, reuse the reload only if its recorded Futon2 identity still equals the newly tested settled commit; otherwise obtain a new preflight and Joe reloads again. |
| Click started or terminal | Never click again merely to clean evidence. Preserve binding, run record, observer envelope, trace, and logs. Certify if exact evidence permits; otherwise record incomplete/fail/unavailable, then release and restore. |
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
