# Where the four-mission whitelist came from: 110 minutes on 2026-07-08

**Date:** 2026-08-26 · claude-13, from Joe: *"what's this whitelist about? That's
definitely a 'finding' not a feature!"*

Confirmed as a finding. Here is the provenance, to the minute, from
`git log` on `futon2`.

## The day

    07:13  9a0a2a2  autoclock-in build-match bindings (zai-10, claude-4 dispatch)
    07:25  3474752  state-snapshot-witness build-match bindings
    07:47  452bfbd  single-entry-point build-match bindings
    08:06  6ceadc4  "Incorporate the three A3 live tests as a re-runnable suite"
                    ← reviewed-candidate-cleans created, 4 entries
    09:56  723cacf  "Ground realized outcome in substrate dial"
                    ← fold_realized reads that map as its domain
    10:11  6261d74  pattern-ingest + invariant-queue-unstuck build-match bindings
                    ← two more missions bound, never added to the map
    10:33  a3cbc56  "Forecast grounded realized expected leg"
    14:20  d36086f  "R14 live-wire migration — grounded feed (flag-gated, dark)"
    16:53  b624242  "Arm all built-dark flags → default ON (Joe-directed via
                    claude-5): live-CAPABLE, latent"

**One hour and fifty minutes** separate a test fixture being created from a
production producer taking it as its domain. By the end of the day it was the
armed default.

## The registry never claimed to be one

`actuator_a3.clj:372`, unchanged since 08:06:

> *Mission → CLean path, **for the A3 live-test suite**. Operational
> verification: does the LIVE substrate match the mission's authored CLean
> structure? **Not a formal proof — a grounded, re-runnable check against the
> running system.***

Nothing there is misleading. The docstring says test suite, and says so twice.
**The over-claim is at the reader, not the writer:** `fold_realized.clj:113`
consults it as if it enumerated the missions the system can ground.

## Where the record does over-claim

Two places, both small and both consequential:

1. **`b624242`: "live-CAPABLE, latent."** The capability being armed was four
   missions, none of them the one the loop actually selects
   (`futon6-d/mission/bayesian-structure-learning`). "Live-capable" is true of
   the wiring and false of the coverage, and the message does not distinguish.
2. **Nobody recorded the narrowing.** `realized-outcome-of` is pure and works
   for *any* enacted decision; `realized-outcome-grounded` works for four
   missions. That is a domain change, and no commit message, docstring, or
   excursion states it. The zai-5 closure of 2026-07-06 verified the *scale
   match* of the two G legs — correctly — and had no reason to check domain,
   because the substitution had not happened yet.

So: not a bill of goods sold, but a **domain contraction that no artefact
records**, arrived at by three individually reasonable steps taken inside one
working day.

## Why this is a pattern and not an anecdote

The shape is reusable and unpleasantly common:

> **A fixture becomes a registry.** Something enumerated for testing — a
> handful of known-good cases — is read by production code as though it
> enumerated the domain. The fixture's docstring says "test"; the consumer does
> not read docstrings. Nothing fails: the consumer returns a well-typed empty
> answer for everything outside the fixture, and the caller cannot distinguish
> "no data" from "out of domain".

Its forces: fixtures are the only curated list available when you need one;
adding a mission to a *test* list feels like test maintenance, so it is not done
when new missions bind; and the failure is silent because "not in the map" and
"nothing to report" produce the same value.

Its resolution is the second clause already proposed for the formal model:
**declare a producer's domain, and refuse a substitution that shrinks it.**

## For PLoP

`p4ng/plop-2026.tex` is the natural home. This is a **pattern with a dated,
minute-resolution provenance and a measured consequence** — seven weeks of ticks
reporting success while γ could not move — which is more than most published
anti-patterns carry. It also pairs with two other instances from today:

- **the wrong-corpus null result** (three parties: claude-19's "zero artifacts",
  my "zero realized outcomes", R8's own promotion note — the last of which
  recorded checking the archive rather than assuming, and checked the wrong
  archive); and
- **evidence pinned to mutable prose** (slice 4: eight of eighteen deposits dead
  because two flexiargs were edited on 08-15 and 08-23).

All three share a signature: **a correct local act, composed into a silent
global failure, invisible to the instruments that were watching.**

## Related

- `holes/NOTE-slice4-slice5-understood.md` — the two faults in series.
- `holes/NOTE-what-stopped-2026-07-08.md` — the migration.
- `p4ng/empirics-futon/NOTE-modular-formalisation-order.md` — the model that would refuse the swap.
