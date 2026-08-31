# P-DISPATCH-WORKFLOW — how dispatch fails, and what the working versions did

*Opened 2026-08-31 by claude-20, at Joe's request: "worth understanding how that worked so that we can
preserve the good aspects … also worth having a little look through the old code … these kinds of dispatch
issues are not solved once and for all."*

**Scope.** This is the **fifth precept applied to agents rather than artefacts.** §0.10 says every artefact
has exactly one `holder : Role`. This document asks the same question of *work in flight*: who holds it,
how do we know it is moving, and what happens when it is not.

---

## 1. What the historical WM loop got right — preserve this

Joe: *"the historical ones didn't have any dispatch problems that I was aware of."* They did not, and the
mechanism is in `scripts/futon2/report/war_machine.clj:998-1232`. It is better than a threshold.

**Progress is measured, not assumed** (`previous-selection-non-progress?`):

    same-selection?  AND  (belief did not move  OR  outcome ≠ :grounded-change)  ->  non-progress

Two independent signals — did the belief move, and was the outcome grounded. Re-selecting the same target
is fine; re-selecting it *without either signal* is not.

**Non-progress DECAYS value, it does not block** (`:1211-1218`):

    decay = 1.0 / (1.0 + k · non-progress-count)
    mission-value-factor = value · decay

Hyperbolic. **No threshold to tune, no hard stop, and the target can still win if everything else is
worse.** The machine drifts off a stuck target rather than being forbidden it — which is what you want,
because "stuck" is a guess and a hard block makes a wrong guess permanent.

**Repair work is exempt** (`repair-selection?`, `recent-non-progress-count`): selections that are repairs of
the machine's own failures are **skipped when counting non-progress**. Fixing your instrument does not
count against you. Without this, a machine that spends three ticks repairing itself would penalise the
target it was trying to reach.

**Recovery is automatic**: one `:grounded-change` resets the count to zero. Nothing has to notice and clear
a flag.

**This is Joe's stated rule in code** — *"continue while the obstruction moves, stop when it repeats"* —
only softer: **deprioritise when it repeats**, which degrades gracefully where a stop does not.

---

## 2. What APM teaches — the failure modes, which are not solved once and for all

`futon3c/scripts/apm-coordinator-enabled.py` exists because of one specific failure, and its docstring is
the clearest statement of it anywhere in the repos:

> *"The watcher needs to tell a **stalled** regulator from one an operator **switched off**. A disabled
> coordinator has flat ticks and a `:running` durable status, which is indistinguishable from a hang unless
> the registry is consulted."*

**Idle and off are the same observation.** That is exactly today's defect: four delegates finished, nothing
was dispatched, and *from outside there is no way to tell "between tranches" from "wedged"*. Joe had to
ask. The fix APM found is **a declared expected state** (the registry) to compare the observation against —
not better observation.

**The stall detector itself was fragile.** Same docstring: *"Earlier versions bounded the entry by scanning
forward a fixed distance or to the next known key; both broke."* Two implementations of the thing that
watches for breakage, broken. **An instrument that reports on health needs its own falsifier**, which is
C22's argument arriving from a different direction.

**Three more, from the record:**
- **Dispatch parameters silently dropped** — `apm-pulse` takes the campaign positionally; `--campaign` and
  `--frame` are accepted and ignored. A dispatch that looks correct and targets the wrong thing.
- **A dispatch can succeed against stale code** — `resume!` reloads no code and re-runs no qualification;
  a namespace reload does not re-arm a running coordinator.
- **An exhausted frame must not block the queue** (Joe, after F32): the machine self-heals and resumes;
  void-and-advance was rejected. **The queue surviving is not the same as the work being done.**

---

## 3. What this dispatch series lacks, measured against both

| the WM loop has | this agent-dispatch series has |
|---|---|
| a per-selection progress signal (`mu-moved?`, `:grounded-change`) | **nothing** — I judge progress by reading the delivery |
| a value function with a non-progress penalty | hand-picked priority tiers |
| automatic recovery on progress | n/a |
| repair work exempted from the penalty | n/a |
| **idle detection** | **nothing — the operator noticed** |

**The gap that actually bit:** there is no declared expected state for the lanes. APM's answer was a
registry saying what *should* be running, so an observation of "flat" can be compared against it. This
series has four named lanes and no record of what each is holding, so "idle" is invisible until someone
counts.

---

## 4. The dimension to develop

1. **A lane registry.** Four rows — lane, current item, dispatched-at, expected-by. Idle is then a *value*
   (`:holding nil`), not an absence, which is `I_absent_is_loud` applied to capacity. This is the cheapest
   item and it is the one that would have caught today.
2. **A progress signal per delivery.** The WM's is two-part and both parts are cheap here: *did the queue
   item's state change* (closed / blocked-with-reason / unchanged) and *did an artefact change* (a commit
   touching a file). "Unchanged and no commit" is the non-progress case.
3. **Non-progress decay over queue items, not a block.** If a lane returns twice on one item without
   changing state, the item's priority decays — it stays workable, it stops being next.
4. **Exempt repair.** A lane fixing its own instrument (C16, C22, C26) must not be scored as non-progress
   on the item it was serving. The WM already learned this.
5. **The detectors need falsifiers.** Whatever watches the lanes must have a mutation mode, or it joins the
   six checks in C22 that were never shown able to fail.

**The honest caveat, and it is Joe's:** *"with the APM series, it mostly works, but it doesn't always."*
Every mechanism above is a WM or APM mechanism that worked until it did not. This document is not a design
that solves dispatch; it is a record of what has failed and what degraded gracefully, so the next version
fails in a new way rather than an old one.
