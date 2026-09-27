# P3-3b-2 — tick emitter execution harness

The tick emitter now supplies the unqualified `harness` wire key to :7070.
It uses the published trace's top-level `:run/id` verbatim, provided it is a
nonblank string. Missing, blank or non-string ids emit `:unknown` with reason
"tick has no usable :run/id". No identity is inferred from author, time, trigger,
cast or model. Emission enablement and WM identity rules are unchanged.

## Identity path checked

- scripts/wm_scheduled_run.clj:108 mints the run id before judgement;
  :135 passes it in :run-id; :159 passes the published trace to emit!.
- scripts/futon2/report/war_machine.clj generate-war-machine carries that option
  into judgement :run/id (search `assoc :run/id run-id`; this file is undergoing
  unrelated edits in the shared checkout and was not changed in this packet).
- src/futon2/aif/trace.clj:530 preserves :run/id in the published record.
  The preceding comment claiming the scheduled runner never mints one is stale;
  the executable scheduled-run source does mint and supply it.
- scripts/futon2/run_tick_once.clj:323 mint-run-id uses UTC date + epoch seconds,
  not a UUID. Two same-second runs could collide upstream. This producer carries
  the existing identifier; it does not claim global uniqueness or repair the
  separate WM identity contract. Distinct supplied run ids stay distinct.

## Tests and deployment observation

Only futon2.aif.evidence-emit-test ran: 8 tests, 62 assertions, zero failures/errors.
clj-kondo zero errors/warnings; check-parens passed both changed Clojure files.
The code changed, requiring fresh evidence rather than a prior-revision warrant.
Tests cover click and cron ids, distinct ids, removed/blank/non-string ids, and
actual post-evidence! JSON serialization with the HTTP transport substituted.
The preexisting flag-off and best-effort failure tests still pass.

Read-only Drawbridge probe of the running futon3c JVM returned loaded? false
for futon2.aif.evidence-emit, while its resource is on the canonical classpath:
file:/home/joe/code/futon2/src/futon2/aif/evidence_emit.clj.
No reload was performed: there was no loaded namespace to update. The checked
production caller is the separate one-shot scheduled runner (its file docstring
and README-clicks-and-ticks.md:336 show the process invocation). No matching
scheduled runner process was observed during the process-list check. This does
not assert that a schedule is currently enabled. The next process will load the
canonical updated source; no already-running process was forced to reload.

No tick/click was triggered and no evidence was written for this packet. A
sequential LIST query at :7073 for tags=wm-tick, since=2026-09-27T22:48:13Z,
before=system-as-of=2026-09-27T22:49:46.193869Z returned 0 entries, no cursor,
no incomplete flag. Thus there is no live tick evidence id to report; absence
of emitted evidence is not proof that no disabled-emission tick executed.
Raw response: /tmp/p3-3b2-live-ticks.json. Test log: /tmp/p3-3b2-tests.log.
