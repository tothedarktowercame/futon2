# Slices 4 and 5, understood — and why fixing either alone yields nothing

**Date:** 2026-08-26 · claude-13, from Joe: *"before we get into the build,
should we understand slice 4 and slice 5 b/c without them we'll get another
partial solution."*

Correct, and worse than expected: **the two faults compound, and the 07-08
migration was a narrowing as well as an improvement.**

## Slice 4 — evidence pinned to mutable prose

Not one bad deposit. **Eight of eighteen**, all `:prompt-not-reconstructable`:

    ft-aif-head-004            ft-operational-vocabulary-001
    ft-bayesian-structure-learning-003   ft-reachable-from-boot-001
    ft-futonzero-generative-011          ft-state-snapshot-witness-001
    ft-learning-loop-010                 ft-war-machine-pilot-001

Including **`ft-bayesian-structure-learning-003`** — the mission behind all 77
realized outcomes found in `data/wm-trace/`.

**Why.** A deposit pins `:prompt {:sha256 … :prose-sha256 {<pattern> <sha>…}}`,
and `reconstruct-prompt` rebuilds the prompt by slurping current flexiarg prose
from `/home/joe/code/futon3/library`. Of the 15 proses pinned by that deposit on
**2026-07-05**, two have since changed:

    aif/expected-free-energy-scorecard     last commit 2026-08-23
    structure/interest-event-vocabulary    last commit 2026-08-15

So the reconstruction no longer matches, and the deposit is rejected.

**This makes "repair the deposit" the wrong fix — it would break on the next
flexiarg edit.** Editing a pattern's prose is normal and encouraged; today Joe
repointed four `@why` declarations in `futon3` as ordinary H5 work. The defect
is that durable evidence is pinned to a mutable tree. Three real options:

1. **Store the prose in the deposit**, not only its sha — reconstruction stops
   depending on the current tree.
2. **Version the reference** — pin `pattern@git-sha` and read the historical blob.
3. **Downgrade the check** — prose drift becomes a warning carrying the old and
   new shas, not a rejection.

And separately, at a different layer: `fold-escrow/load-deposits` **already
degrades by design** — *"Rejections go to stderr AND the return value; valid
deposits still serve."* It is `actuator_a3/deposits-by-id:149` that throws on
`(seq rejected)`, overriding that. Whether that strictness is load-bearing is a
one-line question for whoever wrote it.

## Slice 5 — the grounded producer is a four-mission whitelist

`realized-outcome-grounded` needs a CLean, obtained via
`fold_realized.clj:113` → `a3/reviewed-candidate-cleans`, which is a **hardcoded
map with four entries**:

    futon5a-d/mission/learning-loop
    futon3c-d/mission/autoclock-in
    futon3c-d/mission/state-snapshot-witness
    futon3c-d/mission/single-entry-point

The mission that produced every one of the 77 realized outcomes is
**`futon6-d/mission/bayesian-structure-learning`** — *not in the map*. So
`reviewed-clean-for` returns nil, `box-match-snapshot` sees no clean,
`bound = 0`, and `realized-score` is nil by `fold_realized.clj:163`'s
`(when (pos? bound) …)`.

The deposit itself carries no `:clean` and no `:box-bindings` either — zero
occurrences of both.

## The finding that reframes 07-08

The old producer needed **no registry**. `realized-outcome-of` is PURE and
computes `:realized-score` as *"the SHARED coverage→rollout ΔG"* over the
post-enactment wiring, with expected taken from the fold's own
`:coverage-score-delta`. Any enacted decision yields a number.

The new producer works only for four hand-registered missions.

**So 2026-07-08 replaced a general producer with a whitelisted one.** It is the
better *quantity* — endpoint counts rather than coverage-ΔG mixed with substrate
state — and it is a **narrower** producer, and only the first half was recorded
at the time. The docstring's *"INERT UNTIL DATA"* reads as a timing caveat; it
is actually a scope caveat.

## Why neither slice alone is enough — Joe's point, confirmed

- **Slice 4 alone:** the corpus loads, `deposit-for-mission` returns the
  bayesian-structure-learning deposit — and slice 5 still gives `bound = 0`.
  No realized outcome.
- **Slice 5 alone:** register the mission's CLean — and slice 4 still throws on
  the corpus read before the CLean is ever consulted. No realized outcome.

They are in series. Fixing one and running would produce exactly the same
silence, and would look like the fix had failed.

## What this changes about the formalisation

The chain property already proposed — *step ⑨ occurs ⟹ a durable realized
outcome exists* — remains right, and gains a second clause worth stating
separately:

> **a producer's domain must be declared, and a producer substitution must not
> shrink it silently.**

`realized-outcome-of` has domain *any enacted decision*;
`realized-outcome-grounded` has domain *four missions*. A model in which the
producer-selection table carries each producer's domain rejects `d36086f` on the
spot — not because the new quantity is wrong, but because the swap narrows the
domain without discharging the difference.

That is a second thing a model catches here, and it was invisible until slices
4 and 5 were read together.

## Related

- `holes/NOTE-grounded-feed-missing-input.md` — codex-22's slice-1b walk and the three layers.
- `holes/NOTE-what-stopped-2026-07-08.md` — the migration.
- `futon3c/holes/excursions/E-R8-red-ring-fill.md` — the excursion these slices belong to.
