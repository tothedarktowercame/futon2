# Restart control and startup inventory discovery

All observations are read-only at 2026-09-13. They establish missing controls,
not permission to restart.

## Shutdown and acceptance

The user unit sends SIGTERM to the whole control group, waits 60 seconds, and
then permits SIGKILL (`~/.config/systemd/user/futon3c-zone.service:18-23`). The
JVM shutdown hook stops agents and servers, but does not first close Agency
acceptance and drain invoke work (`futon3c/dev/bootstrap.clj:787-823`). The
HTTP stop function gives http-kit a default timeout of only 100 ms
(`futon3c/transport/http.clj:9343-9362`). Thus systemd stop is termination, not
a demonstrated request drain.

The single mutation boundary is private `create-invoke-job!`
(`http.clj:1500-1570`). Its callers are auto bellback (1164), parked resume
(1293), direct invoke (4532), bell (5222), announce (5350), whistle stream
(5697), and whistle async (5858). The apparent eight surfaces count separates
the two whistle routes; there are seven lexical call sites. Parked resume is
also triggered internally by completion and the scheduled deadline sweeper
(`http.clj:1320-1365`), so blocking only network routes is insufficient.
Already accepted work runs through `invoke-executor`, agent drainers, futures,
and parked durable inboxes; listener closure alone neither inventories nor
drains those queues.

No existing source seam atomically performs all three required actions:
reject every external creator, disable internal parked releases, and report
zero active/queued/in-flight creators under the ledger writer lock. The next
implementation packet should add exactly that controller around
`create-invoke-job!`: a closed/open ingress state checked inside the same
writer-lock transaction, active-creator accounting begun before the check,
parked sweeper/release participation, and a read-only local authenticated
status endpoint. Queue hold must remain separate because it accepts jobs.

## Startup source inventory

The running command pins `.cpcache/1275620277.basis` and a classpath containing
the futon3c relative `dev/src/resources/library` plus these local repositories:

| repository | observed HEAD |
|---|---|
| futon3c | `26d5a1dc` at this discovery; exact loaded baseline unknown |
| futon0 | `fb1cfb0111d8` |
| futon1 | `26ab382e6721` |
| futon1b | `14621cf328f0` |
| futon2 | `d6b1f863cb87` |
| futon3 | `3db309c28025` |
| futon3a | `58ea67a4f3f5` |
| futon3b | `9795feb546c2` |
| futon4 | `e9bb43301da6` |
| futon5 | `2708b1412cdc` |

These are current checkout identities observed after process start, not proof
of bytes loaded in 1942869. The process command-line SHA-256 and basis path are
necessary pins but do not reveal later source edits or reloads. The accepted
Row 19 HTTP retention diff is narrower than this startup closure. Therefore a
startup acceptance inventory must compare an exact proposed repo-qualified
snapshot against a proven serving baseline per path. That baseline is absent;
blindly approving current HEAD or requiring a clean checkout would answer a
different question. Dirty paths must be listed and reviewed individually,
never reset as a precondition.

## Concrete blockers

1. `:restart/ingress-drain-controller-absent` — no common closed-state and
   active-count boundary covers the seven call sites plus parked timer/release.
2. `:restart/serving-baseline-unknown` — current filesystem revisions are not
   the loaded namespace identities for the broad restart closure.
3. `:restart/held-ingress-verification-lane-absent` — no bounded local lane can
   verify source/commission/archive while public creation stays rejected.

The next safe work is the ingress/drain controller specification and tests in
an isolated JVM. Restart execution remains blocked afterward until a reviewer
accepts the complete startup snapshot and verification lane.
