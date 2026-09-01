# C309 — live timers, watchers, and coordinator write sets

Observed 2026-09-01T00:58Z–01:00Z. Owner: `wm-organization`. This is a
point-in-time census to refresh immediately before the C305 writer fence. No
unit or process was stopped, disabled, restarted, or reconfigured during this
discovery.

## Scheduled units

| Unit / cadence observed | Write set established from its executable | Fence disposition and authority |
|---|---|---|
| `mana-snapshot.timer`, ~5 min | Writes `/home/joe/code/storage/futon0/mana-snapshot.json`; reads repository status and Agency/nonstarter HTTP. It does not write any of the five certified repositories. | May remain. Record `write-set-excludes-certified-repositories`. Its snapshot is not certificate evidence. |
| `futon1b-metaspace-sampler.timer`, hourly | Appends `/home/joe/code/storage/futon1b/metaspace/metaspace-YYYY-MM-DD.jsonl`; probes `futon1b-zone.service`. | May remain. Record the same exclusion. |
| `apm-watchdog.timer`, every 15 min | Appends ignored `futon3c/holes/labs/M-diagramprover/apm-driver/closer-heartbeat.log`; if `apm-closer.service` is inactive, restarts it. The closer is an APM campaign writer/dispatcher. | **Must not pulse during the fence. Ask Joe/APM owner to pause the timer or choose a window bounded between pulses after the closer is terminal.** Coordinator authority, not `claude-20` alone. Verify timer next-elapse and `apm-closer` state. |
| `apm-axiom-audit.timer`, scheduled daily | Rewrites ignored `futon3c/.../axiom-audit.jsonl`, appends ignored `axiom-audit-heartbeat.log`, and runs Lean against `/home/joe/code/apm-lean`. | Does not dirty the certified repos, but is a substantial external campaign job. Let an active audit finish; ask Joe/APM owner to prevent a new audit during the window. Do not kill it for cleanliness. |
| `futon-pattern-index.timer`, daily | Reads Futon3 and substrate data; writes `/home/joe/code/data/notions/*`, then Futon6 attestation, roads, carpet, scope, and EFE outputs. It does not target the five certified repositories in the inspected scripts. | Repository write-set is excluded, but the job is heavy and its downstream Futon6 write set is broad. Prefer a gap between pulses; Joe may pause it. If left enabled, record its next elapse and require inactive state throughout. |

There were no user path units. Joe has no user crontab. System cron contained
only OS maintenance (`e2scrub`, apt/dpkg, logrotate); none targets the
workspace. Recheck rather than treating that absence as permanent.

## Continuously running watchers and services

| Observed process/service | Behaviour and write set | Fence disposition and authority |
|---|---|---|
| Two `scripts/apm-watch.sh` processes (plus one older process with the same command) | Reads campaign registry, coordinator, queue and live-frame files; calls the read/report `apm-frame-pulse.py`; emits stdout only. Shell wrappers write only `/tmp/claude-*-cwd` when/if the loop terminates. | Repository-read-only. May remain, but record PIDs and verify command line unchanged. They do not freeze the coordinators they observe. |
| `scripts/apm-watch-projection.sh ... problem-transitions.edn ... coordinator.edn` | Reads the transition/coordinator files and invokes the canonical serving JVM's projection observation; prints health. No file write was found in the script. | Read-only as implemented; may remain. Its subject coordinator is a separate writer and must be fenced. |
| `scripts/futon1b-heap-watch.sh` | Polls `localhost:7073/health`, retains counters in shell memory, writes stdout only; wrapper writes `/tmp/claude-*-cwd` on exit. | Read-only; may remain. |
| Unauthorised-campaign-ticking shell watch | Reads coordinator tick counts and the coordinator registry once per minute; prints changes only. | Read-only; may remain. |
| `apm-campaign-babysit-jit-all-open-v2.service` / `apm-campaign-babysit.py` | **Not read-only.** `reconcile_park_decisions` can rewrite the campaign's `queue-state.edn`; incident handling creates `/tmp` bell packets and sends Agency bells, which can dispatch work. | **Ask Joe/APM owner to stop or positively park it for the whole fence.** `claude-20` must not stop another campaign unilaterally. An inactive service plus unchanged campaign files is the acknowledgement. |
| Futon3c serving JVM | Hosts the Agency and durable coordinators. At observation time the registry contained enabled `jit-m94A03-retry-v3`, `jit-all-open-v2`, and `ftriangle-live-smoke-v1` coordinators. Their state paths are under `futon3c/data/...`; they can tick, persist campaign state, and dispatch agents without a separate OS process. | **Serving JVM must remain for reload/click, but Joe/APM owner must pause every enabled coordinator and acknowledge no resume until `FENCE-RELEASE`.** Verify registry `enabled? false` and stable state paths twice. Stopping watcher shells alone does not fence these writers. |
| `apm-closer.service` | A transient APM closer; observed `activating (auto-restart)`. It can work in `/home/joe/code/apm-lean` and is restarted by `apm-watchdog`. | Let it reach a terminal/parked state, then have Joe/APM owner inhibit watchdog restart for the window. It is outside the five Git repos but can dispatch Agency work and consume shared resources. |
| Futon1b, Math.SE XTDB, voxterm, dbus/accessibility, GPG services | Persistent service/data processes. No code path in this census established writes into the certified source repositories. | May remain. Their service data is outside the Git basis. Futon1b/Futon3c are required dependencies, so stopping them would make the run unavailable. |

## Operational conclusion

A clean window is reachable without tolerating a known repository writer, but
only after Joe/APM-owner coordination. The mandatory asks are:

1. pause the enabled durable APM coordinators without stopping Futon3c;
2. park/stop the JIT babysitter so it cannot reconcile or bell;
3. let `apm-closer` finish/park and prevent `apm-watchdog` from restarting it;
4. avoid the axiom-audit and pattern-index pulse windows;
5. freeze the four War Machine lanes and paper publisher as specified by C305.

The mana and metaspace samplers and the four read-only shell watchers need no
stop. Their continued presence must still be recorded, because a changed
command line or replacement PID invalidates this write-set disposition.

Anything not named above is `write-set-unknown` and blocks `FENCE-HELD`. In
particular, “running inside the serving JVM” is not evidence of read-only
behaviour: the durable coordinators are the counterexample.

The paste-ready Joe request, confirmed stop/resume forms, verification, and
restoration ownership are recorded in
`C313-request-to-park-background-writers.md`.
