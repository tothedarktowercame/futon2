# Live-loop IV.1 run log — 2026-07-06

**STATUS: FOULED / ABORTED — DO NOT USE AS IV.1 EVIDENCE.**

This attempted run was dispatched with stale/irrelevant read material:
`live-loop-iii-1-explainer.html` and
`futon3c/holes/excursions/E-first-flights-transferred-work.md`. Joe stopped the
experiment before it could be treated as a valid IV.1 run.

Abort evidence:

- Fresh agent: `zai-12`.
- Contaminated job: `invoke-1783347091882-679-435c3be9`.
- Stop job: `invoke-1783347203666-680-b4b80cdb`.
- `zai-12` was deregistered through
  `DELETE /api/alpha/agents/zai-12`.
- The fouled one-shot process was killed by exact PID:
  parent shell `3540902`, Java child `3540904`.
- Process check after kill: no `3540902` or `3540904` remained.
- Scheduler log `logs/wm-scheduled.log` has no line from this fouled attempt;
  its latest completed line was the normal `2026-07-06T14:04:42Z` cron tick.
- Trace file `data/wm-trace/wm-trace-2026-07-06.edn` was last modified at
  `2026-07-06 15:04:42 +0100`, matching the normal `14:04Z` tick, before the
  fouled dispatch reached the manual tick.

Use a fresh IV.1 run log for the corrected run. The corrected dispatch must read
the current cards-IV/IV explainer material only, not the III.1 explainer or the
closed first-flights transfer note.

**Agent**: zai-12 (fresh follow-mode agent)
**Dispatched by**: codex-1 (operator Joe consented to this manual live-loop IV.1 run)
**Start**: 2026-07-06T14:11:44Z
**Mode**: MANUAL IV.1 — same cron-equivalent code path, labelled manual

## Task reference

GROUND-CONTROL.md §2, cards-IV ready state, loop III.1 T-0 run contract.

## 1. T-0 baseline bundle

### Pre-baseline git state (futon2 HEAD)

`ad6ba9d clarify position-aware accretion as watch item`

Dirty: several `holes/` edits and untracked files unrelated to this run (left untouched).

### Commands (run at T-0, immediately before the tick)

```
cd /home/joe/code/futon2 && bb scripts/live_loop_step.bb gate 2f
cd /home/joe/code/futon2 && clojure -M scripts/reference_regression.clj
```

_Output pending — pasting exact text below as each command returns._

<!-- GATE-2F-OUTPUT-HERE -->

<!-- REGRESSION-OUTPUT-HERE -->

## 2. Manual live tick (MANUAL IV.1)

_Command: `cd /home/joe/code/futon2 && clojure -M:wm-scheduled`_

_Start / end / output: pending._

<!-- TICK-OUTPUT-HERE -->

## 3. Post-tick inspection

_wm-scheduled.log tail, newest wm-trace file(s): pending._

<!-- INSPECTION-HERE -->

## 4. Findings

_Pending._
