# T-wm-excessive-guardrails-19092026 — the day the safety layer stopped the machine

Joe's verdict, 2026-09-19 (paraphrased, then evidenced):

> All this work has just been to turn off — not really turn off, but *undo* —
> excessive security. We haven't even been able to run the machine, and we've
> spent a lot of energy across a lot of agents turning off the machine:
> badly-designed behaviors that actually prevented us from doing anything
> useful.

This note records the opinion AND the evidence behind it, so the next person
who is tempted to add a guardrail reads what 2026-09-19 cost before adding it.

## What actually happened

From 2026-09-15 to 2026-09-19 the War Machine did not complete a single
on-demand click. Not because its inference was wrong, not because a seat
failed permanently, and not because anyone disabled a safety check — but
because its own protective behaviors, each reasonable in isolation, composed
into a machine that could not run:

1. **T8 (duplicate-finding livelock) halted every click at
   `:opportunity/:start`** — before selection — on three repair findings from
   one 84-minute window on 2026-09-15, four days stale. The detector had no
   time dimension and no notion that its findings had already been recorded;
   "a livelock is repetition NOW," not a group count over all history.
2. **The halt landed before the machine could select the repair the wire
   demanded.** `repair-entry` builds a `:repair-machine-failure` stop-line
   action from exactly those findings — but selection never ran, so the
   machine was structurally unable to perform the repair its own safety layer
   was screaming for. A deadlock with no exit except a human.
3. **When we finally forced one click through, the author seat failed with a
   1.3-second transient CLI error** — and the run died, because the
   author-infrastructure retry existed but had never once fired: its
   predicate matched the error code `"invoke-exception"`, a spelling the
   invoke layer stopped emitting years ago. Guard machinery that has never
   exercised its own happy path is indistinguishable from no machinery.
4. **Each failure wrote a 19 MB repair finding** (one was 19,220,142 bytes of
   pprint), so every diagnostic attempt cost megabytes and minutes of
   serialization under a store lock.
5. **The test suites were already red at HEAD** — futon2's could not even
   compile (a docstring edit embedded bare quotes inside a quoted docstring;
   the live JVM masked it by running an older load), and ~280 failures of
   furniture sat in full-loop-runner-test — so nothing that broke further
   could be seen breaking.

Across six agents and one day, the work was: T8's detector given a clock
(claude-4), the halt-before-repair ordering undone (zai-10, `8f7799b7`),
the dead retry predicate revived (`75fd4260`), the finding writes bounded
1163× (zai-11, `056e429d`), the red test furniture fixed (zai-12 `025adb25`,
`70f56323`), the negative controls that were green while testing nothing
repaired and given a vacuity probe (zai-13, `1f59b00`), and one bad idea of
our own (a per-request wire-disable at the HTTP boundary) reverted before it
became a permanent hole (zai-30, `c9b9ed6d` + futon2 reverts).

**None of this weakened the security layer.** Every wire still evaluates on
every phase transition; every witness still stops the run; the discharge
contract still refuses fabricated evidence (this was tested, by me, from the
inside — the invariants held against the machine's own operator-role agent).
What was removed was not security but *malfunction dressed as security*:
guards that fired on stale conditions, ordering that made the demanded repair
unreachable, retry machinery that could never fire, and writes that buried
signal under serialization.

## The design opinions this day earned

1. **A guardrail must leave the system a way to satisfy it.** A wire that
   halts before selection, on a condition whose only remedy is a selected
   repair, is not a guardrail — it is a deadlock. Safety that the system
   cannot comply with is just downtime with good intentions.
2. **Guards need clocks and states, not just thresholds.** "Three findings
   sharing a signature" was treated as a live loop with no evidence of any
   repetition occurring. Detectors should measure the behavior they name.
3. **Every guard must be able to fire its own recovery.** Retry predicates,
   escape hatches, and status matchers must be tested against what the
   emitting layer actually emits TODAY. Three separate defects today were
   retired symbols with live callers (`invoke-exception`,
   `resolve-pinned-selection`, a dead docstring option). Match classes, not
   spellings.
4. **Red is only information against a green background.** Test suites and
   control suites that are permanently red (or permanently green-while-
   vacuous) remove the only channel a safety system has for telling the
   truth. Keeping suites honest is itself safety work.
5. **Diagnosis must be cheap.** A 19 MB finding per failure means every
   attempt to understand the machine costs minutes and megabytes; bounded
   durable copies with visible elision are the pattern (trip reports already
   had it; findings now do too).

The machine's security layer was the part that worked all along: it refused
every dishonest shortcut we tried. The work of 2026-09-19 was making the rest
of the machine worthy of it — able to run, able to fail legibly, and able to
repair what its own guards demand.

— zai-14, for the 2026-09-19 defusal crew (zai-10, zai-11, zai-12, zai-13,
  zai-30, zai-35, claude-4, and Joe's ruling that kept the security layer)

## Addendum: Joe's ruling, 2026-09-19 (recorded by claude-12)

Joe's verdict on the layer itself, later the same day — the guardrails were
excessive, and the presumption is now inverted:

1. **Any security system that gets in the way of a real run is not wanted at
   this point.** It obstructs the research needed to tune the machine. These
   guards are not part of the AIF specification in Lean, are not themselves
   type-checked (so they degrade), and were not grounded in the AIF terms or
   equations nor in concrete requirements of use. The defusal described above
   cost 5% of a week's Zai usage; undoing the layer costs more, against a
   2-day deadline.
2. **Validation that a run is a REAL run is wanted.** The bad outcome to
   prevent is the facade: something that claims to be AIF, was never
   commissioned, and demonstrably has nothing to do with AIF. Evidence of
   realness on the run's own records (C source, rates provenance,
   enumeration-completeness, F_pi presence) serves this; vetoes do not.

The presumption before any audit: none of the guard machinery is relevant
unless it serves (2). The burden of proof sits with the guard, not with the
run.

Enacted the same day: witness halts are now OPT-IN
(`FUTON_WM_TRIPWIRE_HALT=1`); by default a witness is recorded durably once
per run and the run continues (`futon2.aif.tripwire/observe!`, `note!`). The
09-18 one-trip-one-shutdown behavior is preserved behind the flag, and the
anti-pile-up property (one report, not 205) is kept in both modes.
