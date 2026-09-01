# O15 — How often should the Lean checks run?

**Class:** ready to decide. **Decides:** Joe. **Written:** 2026-09-01 by `claude-20`.

> ## DECIDED 2026-09-01 by Joe: **Option C**, with a boundary.
>
> Targeted revalidation is adopted now. The full suite is **not** put on any
> cadence, and is not to be run as routine, **until "major milestone" is
> defined** — at which point the full suite runs at each milestone.
>
> Joe: *"since we don't have a definition of major milestone, there's no real
> way to get the overall suite to run at a sensible cadence."*
>
> **This does not stop invoking the gate when something needs it** — `make
> pre-merge` before a review, and the gate run required for a certified commit,
> are unaffected. What is ruled out is scheduling it, or running it as a habit,
> against a cadence nobody can justify. Flagged as my reading; correct it if
> wrong.
>
> Consequence: the trigger for full-suite runs is an **event, not a clock**.
> That is why no cadence answer was satisfying, and it resolves O16 and reframes
> O17. Defining the milestone is now a decision in its own right.

## The question

There are 32 Lean checks whose only job is to prove the *other* checks still
catch mistakes. Right now they run only during the full workspace gate, which
takes several minutes and needs everyone to stop editing. Between those runs,
nothing establishes that the ordinary checks would still notice a broken
result. How often should the 32 run?

## Why it exists

Each of the 32 is a deliberately wrong Lean statement. The check passes when the
machinery *rejects* it. If someone changes a definition such that a check stops
rejecting wrong answers, the ordinary green checks stay green and say nothing —
the failure is that the alarm stopped working, not that anything looks broken.

Concretely: on 2026-09-01 one of these went unexercised for **1 hour 29 minutes**
while four lanes committed continuously. Its wrapper would have reported the
problem correctly. Nothing asked it to.

This is scheduling, not a wiring gap. All 32 are individually runnable today;
`checks/` has a focused wrapper for each, and every one currently passes.

## The criterion

**How long are we willing to not know that a detector stopped detecting, given
what it costs to keep checking?**

That is the whole decision. It is a cost-against-exposure call, which is why it
is yours and not mine — I can supply both numbers but not the appetite.

## What I measured (today, on this machine)

| | |
|---|---|
| One Lean negative control, cold | **6.93 s**, 3.4 GB peak RSS |
| Same, warm repeat | **6.50 s**, 3.4 GB peak RSS |
| All 32 sequentially | **≈ 3 min 30 s** |
| Full workspace gate | **≈ 6 min 35 s** CPU, needs a quiescent tree |

The 3.4 GB is Mathlib being imported, and it is per process — running many at
once is bounded by memory, not by cores, and this machine has already hit a
cgroup throttle from memory pressure. Treat 32-in-parallel as unavailable.

**Related finding:** the RUNBOOK's duration table lists the workspace gate at
32.91 s. That measurement predates these 32 registrations. **The table is
stale** and I will re-measure it regardless of this decision.

## Options

**A — Run all 32 on every commit.** Adds ~3.5 min to every commit. Exposure
drops to one commit. The lanes commit continuously, so this is the option that
changes how the day feels, and I do not recommend it.

**B — Keep them gate-only (today's behaviour).** Costs nothing new. Exposure is
however long since the last full gate — which today was over an hour and is not
bounded by anything.

**C — Targeted revalidation, gate as backstop.** When a change touches a witness,
that packet runs that wrapper's negative modes before it lands: **~6.5 s for one,
~13 s for two**. The full 32 stay on the gate. Exposure for a *touched* check
drops to zero; exposure for an untouched one stays at gate distance, which is
correct, because an untouched check has nothing new to invalidate.

## Recommendation

**C.** The 1:29:08 gap was not a scheduling failure in general — it was a gap for
*one specific check whose dependency was being edited at the time*. Option C
closes exactly that and leaves the rest alone. Option A pays 3.5 minutes on every
commit to re-prove 31 things nobody touched.

C needs one rule added to the handoff protocol — a packet that changes a witness
runs that witness's negative modes — and no new infrastructure.

**This does not fully close it.** Under C, an untouched check can still rot
between gate runs, through a shared dependency the packet did not name. That is
O17: what gate distance we are willing to accept. C makes the common case cheap;
it does not make gate cadence unnecessary.

## What deferring costs

Nothing breaks. Today's behaviour continues, exposure stays at gate distance,
and the gate receipt keeps recording that distance so the number stays visible.
This is the decision becoming cheaper to make, not more urgent.
