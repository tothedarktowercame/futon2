# C318 — what can verify the background-writer fence

Date: 2026-09-01. Owner: `wm-evidence`. Assessment only: no process, unit,
coordinator, or registry was changed.

## Result

No existing single observation proves the writer fence. The strongest honest
pre-fence verification is a composite of (1) durable coordinator state,
(2) systemd timer and service state, (3) a current writable-handle scan,
(4) C292, and (5) owner acknowledgements. The first four establish observed
facts; the fifth is still required to constrain future actions.

The certificate context should distinguish these explicitly:

- `:writer-fence/observed` — coordinator, unit, handle, job, lane, and Git
  observations, with their times and captured identities;
- `:writer-fence/attested` — operator, coordinator, publisher, and session
  promises not to start new work before `FENCE-RELEASE`;
- `:writer-fence/unverifiable` — absence of an unenumerated future writer and
  cross-authority ABA, for which no monotonic witness exists.

## 1. Durable coordinators

For each of the three named coordinators, a serving-JVM `status` result is a
sufficient observation of *current durable quiescence* only when all of these
agree:

1. registry `:coordinator/enabled? false`;
2. registry lifecycle is not `:running`;
3. durable `:regulator/status :stopped`;
4. `:tick-claim nil`;
5. a `:durable-quiescence-witness` for the same coordinator and current epoch;
6. runtime has no live scheduler for that coordinator.

`durable-coordinator/stop!` writes the disabled registry state before cancelling
the scheduler and writes `:stopped` only under the tick lock, after observing no
claim (`futon3c.apm.durable-coordinator:676-725`). The claim-allowed predicate
also rereads the registry and requires enabled/running state before a tick
(`:568-580`). This makes the six-field result meaningful rather than a label.

It is not a promise about the future. `resume!`, a registry mutation, or an
operator-started replacement can re-enable work. Reload recovery does not
legitimately start a disabled entry, but the registry itself must remain the
same. Therefore capture the registry digest and status results twice around the
other observations, and retain Joe/coordinator's no-resume acknowledgement.

Canonical observation through the serving JVM:

```sh
cd /home/joe/code/futon3c
for id in \
  'jit-queue:jit-m94A03-retry-v3' \
  'jit-queue:jit-all-open-v2' \
  'ftriangle-live-smoke-v1'
do
  printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/status \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"$id\"))" \
    | scripts/proof-eval.sh -
done
sha256sum data/apm-coordinators/registry.edn
```

Any missing field, parse failure, `:enabled? true`, tick claim, non-stopped
durable status, runtime scheduler, or changed registry digest is unavailable/not
parked, never an inferred success.

## 2. Timers and services, including ordering

`systemctl --user is-active` is sufficient for instantaneous unit state, not
for continued inactivity. Observe both timer and paired service, including
invocation IDs:

```sh
systemctl --user show \
  apm-campaign-babysit-jit-all-open-v2.service \
  apm-watchdog.timer apm-watchdog.service apm-closer.service \
  apm-axiom-audit.timer apm-axiom-audit.service \
  futon-pattern-index.timer futon-pattern-index.service \
  -p Id -p ActiveState -p SubState -p InvocationID \
  -p NextElapseUSecRealtime --no-pager
```

The safe parking order is:

1. stop the watchdog **timer first**, preventing a new watchdog activation;
2. wait for any already active watchdog invocation to become inactive;
3. explicitly stop `apm-closer.service` after preserving its terminal work;
4. verify both watchdog timer/service and closer service inactive;
5. stop the audit and pattern timers, let any already-running paired services
   finish, then require both timer and service inactive;
6. stop the babysitter and require inactive.

C313's instruction to let the closer finish and then remain down is not
self-enforcing: the inspected transient `apm-closer.service` has
`Restart=always` and `RestartSec=5min`. With only the watchdog timer parked it
can restart itself. It must be explicitly stopped by its owner. A delay is not
a proof, but a second identical observation after at least the closer restart
interval detects this specific recurrence. Continued non-start still rests on
the unit-owner acknowledgement; systemd state has no reservation primitive.

## 3. Current writable handles

A scoped `/proc` scan is a useful additional observation for the class Git
status misses. The C314 scan found the active axiom audit's JSONL handle and the
serving JVM's coordinator tick-lock handle inside `futon3c`.

Run the following as the same user immediately before declaring the fence and
again at every checkpoint:

```sh
python3 - <<'PY'
import os
roots = tuple(p + '/' for p in (
    '/home/joe/code/futon2', '/home/joe/code/futon3c',
    '/home/joe/code/mathlib4', '/home/joe/code/p4ng', '/home/joe/code/futon3'))
found = []
for pid in (p for p in os.listdir('/proc') if p.isdigit()):
    try:
        fds = os.listdir(f'/proc/{pid}/fd')
    except OSError:
        continue
    for fd in fds:
        try:
            path = os.readlink(f'/proc/{pid}/fd/{fd}')
            if not path.startswith(roots):
                continue
            line = next(x for x in open(f'/proc/{pid}/fdinfo/{fd}')
                        if x.startswith('flags:'))
            flags = int(line.split()[1], 8)
            if flags & 3 not in (1, 2):
                continue
            cmd = open(f'/proc/{pid}/cmdline', 'rb').read().replace(b'\0', b' ').decode(errors='replace')
            found.append((pid, fd, oct(flags), path, cmd))
        except (OSError, StopIteration, ValueError):
            pass
for row in found:
    print('\t'.join(row))
raise SystemExit(1 if found else 0)
PY
```

Exit 0 means no writable handle was observed at that instant. Exit 1 names
observed handles for classification; it is not automatically a breach because
an explicitly authorised evidence write phase may have named handles. The scan
does not detect a process that currently has no file open but can open one a
moment later, memory-mapped write semantics not represented by a live writable
FD, a writer running as an unreadable user, or a write completed between scans.
It supplements rather than replaces the roster and acknowledgements.

## 4. What remains attested rather than independently verified

The following cannot be proved from the existing observations:

- Joe/editor/shell will not start a new writer after the scan;
- `claude-20`, another agent session, or the publisher will not dispatch or
  write after acknowledging the fence;
- no manual/system-level/container process with no currently open writable FD
  will begin writing later;
- an interpreted watcher with the same PID/argv will retain the inspected
  behaviour unless its script/configuration bytes are included in the settled
  basis;
- no cross-authority ABA write-and-restore occurred between observations;
- every in-JVM writer has been enumerated solely because the three known
  durable coordinators were stopped.

These are not reasons to abandon the window. They define its authority model:
the observed bundle verifies the named writers and current handles, while the
complete session/owner acknowledgement roster constrains future starts. An
unknown or missing owner, changed PID/script/unit invocation, registry digest
change, or newly observed handle makes `FENCE-HELD` unavailable.

## Paste-ready verification sequence

After owners report parking, run: coordinator status and registry digest;
systemd `show`; the writable-handle scan; `python3 checks/quiescence_check.py`;
then repeat coordinator status/digest and systemd `show`. Accept only if both
ends agree, every named state is parked, the handle set is empty/classified,
C292 says `QUIESCENT`, and the acknowledgement roster is complete. Record the
entire output. This is a composed evidence bundle, not one atomic machine
verdict, and it must be described that way in the run certificate context.

