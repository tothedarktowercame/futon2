# C175 — contract authority must match its Lean source

Date: 2026-08-31

## Decision

The workspace gate requires a **hard source-content match** between
`Holes.lean` at `holes-contract.json :source :git-sha` and the current clean
`DarkTower/WarMachine/Holes.lean` blob.

A **source** lag is not silently tolerated or time-bounded. “Until the delivery
that caused it finishes” has no machine-readable boundary in the contract and
would create a second, manually maintained truth about whether the skew is
allowed. A transient red between a `Holes.lean` commit and contract regeneration
is therefore honest: that delivery is not gate-complete until it exports the
contract it changed.

Repository-HEAD lag is allowed and reported when unrelated mathlib files move.
Requiring identical repository commits produced a false red during C175:
mathlib HEAD moved while the `Holes.lean` blob stayed identical. The contract
describes that source file, not every file in mathlib, so blob identity is the
precise invariant.

**C177 amendment.** The checker additionally requires the recorded authority to
equal `git log -1 --format=%H -- DarkTower/WarMachine/Holes.lean`. This states
the generator relationship directly; blob equality remains an independent
content check.

Bindings pinning the old contract are internally reproducible, but that only
answers “do these witnesses match this export?” It does not answer “does this
export describe the Lean source now on disk?” Both claims are required.

## Check

`checks/contract_authority_current.clj` reports the recorded contract authority,
mathlib HEAD, recorded/current `Holes.lean` blobs, and working-tree cleanliness.
It fails when the recorded authority cannot resolve the source, the two source
blobs differ, the working copy is dirty, or the current source is unreadable.

Canonical invocations:

```sh
bb checks/contract_authority_current.clj
bb checks/contract_authority_current.clj --negative-control
```

The negative control substitutes an older/different source blob and passes only
when the checker rejects it.

## State at delivery

The first check correctly reported a real source skew after Fold. During the
delivery, the contract was regenerated and mathlib moved again for unrelated
work: recorded authority and HEAD differed, while both resolved to clean
`Holes.lean` blob `bded4c20…`. The final content-scoped verdict is green and
keeps the repository-level skew visible rather than mistaking it for source
drift.
