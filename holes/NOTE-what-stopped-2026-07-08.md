# What stopped the realized outcomes: the 2026-07-08 grounded-feed migration

**Date:** 2026-08-26 · claude-13, from Joe: *"I don't remember about 07-06, it
may be a coincidence. I guess we could look in the Git Log around that time."*

It is a coincidence. **07-06 is simply the last day the old producer ran.** The
cause is two commits on **2026-07-08**.

## The sequence

    07-02 … 07-06   realized-outcome-of (coverage→ΔG) produces 88 outcomes
                    across five trace files; each carries
                    {:policy … :expected-G -0.2 :realized-G -0.5 :tick …}
    07-05  d05dd35  "gamma feed restored: escrow coverage-dG feeds R14's
                     expected leg (operator-armed)"
    07-06  2d13ef2  "fold-realized: zero-coverage semantics for gamma realizedG
                     (T-0 fix)"          ← the day's count drops to 2
           (no trace files 07-07, 07-08 — no runs)
    07-08  d36086f  "fold-realized: R14 live-wire migration — grounded feed
                     (flag-gated, dark)"
    07-08  b624242  "Arm all built-dark flags → default ON (Joe-directed via
                     claude-5): live-CAPABLE, latent"
    07-09  ef1aa64  "R8: carry the strategic belief mu across ticks"
    07-09           first run after the switch — **0 realized outcomes**
    …               0 in every trace file since, through 07-21 (the last)

## What the migration did

It replaced the producer. `*selection-gain-grounded-feed?*`, armed the same day,
says so in its own docstring:

> *When ON — and only when `*live-wire?*` is also ON — the `:realized-outcome`
> feeding γ is the A5 SUBSTRATE DIAL (`realized-outcome-grounded`,
> bound−inhabited endpoint counts) **instead of** `realized-outcome-of`
> (coverage→ΔG …). **INERT UNTIL DATA: γ stays starved until real fold-variance
> flows**, so arming has no effect until a run produces grounded samples.*

`realized-outcome-grounded` reads the world dial via A3 build-match:
`:realized-score` is `bound − inhabited` over reviewed substrate endpoints, and
*"Both legs are endpoint counts, never coverage-ΔG mixed with substrate state."*

**So the migration was a correctness improvement.** The old producer mixed a
coverage delta with substrate state; the new one does not. It is the better
quantity.

**And it has never had an input.** No run since 07-08 has produced grounded
samples, so γ has been starved for seven weeks — exactly as the docstring said
it would be until data flowed, and nobody came back to check whether it had.

## Which corrects the R8 diagnosis a third time

- This morning: *"R8's realised term has no producer."* Wrong — `fold-realized`
  is the producer, built and armed.
- This afternoon: *"the producer has never fired."* Wrong — it fired 88 times
  over five days, in `data/wm-trace/`, which I had not searched.
- Now: **the producer that fired was replaced on 2026-07-08 by a better one
  that has never fired.** R8 went red not from neglect and not from a missing
  instrument, but from an upgrade whose stated precondition — *inert until data*
  — was never met.

## What this does to E-R8-red-ring-fill

Slice 1's question was *"does the wiring reach the producer?"* The better
question is now **"which producer is wired, and does it have inputs?"**, and it
has a concrete first move that needs no R10 run:

> Take one tick from `data/wm-trace/wm-trace-2026-07-04.edn` — where the
> coverage-ΔG producer demonstrably worked — and ask what
> `realized-outcome-grounded` would return for the same mission today. If it
> returns nil or a degenerate `bound − inhabited`, the missing input is named
> and the fix is upstream of γ entirely.

That is an archive exercise, not an operational one, and it can be run without
touching R10.

## Related

- `src/futon2/aif/fold_realized.clj` — both producers, and the arming docstrings.
- `holes/NOTE-step9-reachability.md` — the two-runners finding and the 88 outcomes.
- `futon3c/holes/excursions/E-R8-red-ring-fill.md` — the excursion this revises.
