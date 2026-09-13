# Row 19 controlled-restart preparation

Status: **not authorized and not ready**. This packet performs discovery and a
read-only preflight only. It does not stop, reload, restart, hold, back up, or
write any serving state.

## Measured serving identity

On 2026-09-13 the Java worker was PID 1942869, cwd
`/home/joe/code/futon3c`, in cgroup
`futon3c-zone.service`. The systemd user unit's main wrapper was PID 1942862;
its `ExecStart` was `/home/joe/code/futon3c/scripts/dev-zone-env`. The worker
listened publicly on 7070 and locally on 6768. Exact read-only commands:

```sh
cat /proc/1942869/cgroup
readlink /proc/1942869/cwd
sha256sum /proc/1942869/cmdline
systemctl --user show futon3c-zone.service \
  -p MainPID -p ActiveState -p SubState -p FragmentPath -p ExecStart -p Environment
ss -ltnp | grep -E '1942869|:7070|:6768'
```

The unit, launcher, dependency declaration, and accepted retention source had
SHA-256 respectively `e39dfa6400a92101d3211af4dcb8923801f6961fd7ec94d6aff5635ac58db3f7`,
`bb06d7328aa42860c89ecd3d25b69652a259e4e4c827fc05adb28c1e3d4a6b40`,
`9e2478fe88b52c5dea7a97a5b3320b3d8aabab92ec308f04f16f3db57ffd7398`,
and `defb1ed3b0deeb16ba15857f8efe5ac4e53ed6c442723dfae8808b38406f65fb`.
The full futon3c tree was `7c4dec646e61ce099514318f37c36bf26bbb371f` on
branch `master`; that tree is recorded, not declared reviewed.

## Why preflight currently refuses

Port 7070 is owned directly by the JVM. All eight job-creation surfaces call
the same private creator, but the queue hold still accepts requests and there
is no separately measured listener/proxy drain control. Stopping the unit can
therefore interrupt a request already accepted by the old function. An
independent ingress-fence acceptance record must name the actual control,
cover HTTP, WebSocket bellback, parked resume, invoke, bell, announce, whistle
stream, and whistle, and prove zero accepted, queued, and in-flight creations.

A restart loads the whole classpath, not merely the accepted HTTP file. A
separate full-tree acceptance must pin `futon3c master`, its tree, `deps.edn`,
launcher and unit bytes, each local classpath repo revision/dirty state, and
the reviewed disposition of every difference from the currently running
process. No such acceptance exists. The archive directory is also absent;
the operator must provision and permission it before restart. These yield
typed refusals from `restart-preflight.sh`.

## Operator sequence after independent review

1. From a separate shell, record PID/cgroup/cwd/cmdline, unit properties,
   ports, repo-qualified revisions, dependency/classpath pins, disk space,
   ownership and modes. The present preflight is discovery-only and always
   refuses: file existence is not acceptance. A future independently reviewed
   authority reader, outside this script, must resolve the two records.
2. Activate the reviewed upstream ingress fence. Prove all eight creation
   surfaces reject new work. Wait until active, queued, and in-flight invoke
   creation/delivery/execution joins are zero. Re-run the proof immediately
   before stopping. A queue hold is not this fence.
3. With ingress still rejected, create a mode-0700 backup directory on the
   same durable filesystem. Copy the 0600 hot ledger and any archive files,
   hash source and copies, `fsync` files and directory, then compare hashes.
   Never remove or rewrite the originals. Confirm free space for two complete
   hot-ledger copies plus archive growth. A failed barrier aborts while the
   service remains stopped from accepting work.
4. Execute only `systemctl --user restart futon3c-zone.service` from that
   external shell. Startup reads and compacts the hot ledger; therefore the
   backup and quiescence checks precede startup. Do not restore an old ledger
   over a newer one. If startup might have written any ledger or archive byte,
   keep ingress rejected and fix forward at a newly accepted tree. Returning
   to old code is permitted only with mechanical proof that failure preceded
   every state read/write; uncertainty requires roll-forward. Never delete
   archive evidence.
5. A verification lane that remains reachable while public ingress is held is
   not implemented. Until one exists, the procedure stops here with
   `:restart/held-ingress-verification-lane-absent`; it cannot claim readiness.
   The needed lane must be local-only, authenticate the operator, expose
   process/source identity and one bounded retention/archive operation, and be
   outside the rejected public creation paths.
6. That future lane must perform a bounded explicit archive operation after a
   fresh test job reaches final delivery/execution joins, then compare keyed
   readback immediately. It must not wait seven days, alter the global clock,
   or reconstruct author job 20588.
7. Release ingress last. Retain the preflight, backup hashes, systemd journal
   interval, process identity, API output and archive readback as the operator
   execution record.

The post-restart probe deliberately waits for final delivery/execution joins:
archiving earlier can create a hot/archive disagreement. Roll-forward is the
default because D13 expiry and archive-before-drop must never be regressed.
