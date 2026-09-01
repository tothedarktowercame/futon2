# C434 — cross-repository negative-resolution census

**Date:** 2026-09-01  
**Basis:** futon2 witness registry at `2773627`; mathlib4 at `444c22e92c`

## Population and boundary

Futon2 does not name Lean declarations in its negative fixtures. It names **32
mathlib4 fixture files** under registry mode `:lean-guard-msgs`. The declaration
names used by a mutation live inside those mathlib4 files, usually through an
import of `Holes` or of the corresponding positive-witness module. Thus there is
a cross-repository fixture-path boundary, but the declaration-reference boundary
is inside mathlib4 rather than in futon2 data.

All 32 registered guarded fixture files exist, contain `#guard_msgs`, and
currently elaborate with exit 0. There are **32 name-bearing guarded controls and
0 structurally silent controls** in this population.

## What a rename does

An unresolved name does not pass this scheme. Lean may first interpret a stale
identifier through `autoImplicit`, as happened in C277, but the resulting
diagnostic differs from the diagnostic recorded by `#guard_msgs`. The Lean file
then exits nonzero. Its futon2 wrapper requires exit 0 from the guarded negative,
so it reports exit 2, `mutation-slipped`. C277 is direct evidence of this path.

The resolution property is therefore already enforced by diagnostic identity
plus wrapper exit semantics: missing fixture, missing declaration, changed
diagnostic, or an additional diagnostic is loud. Adding a second name scanner
would be weaker than elaboration and would duplicate Lean's resolver.

The structural census and the behavioural survey should agree at the following
grain: every one of these 32 fixtures must elaborate successfully *because its
expected rejection diagnostic is reproduced*. A behavioural control that claims
success while its guarded fixture exits nonzero would contradict this census and
is a defect. C277 did not do that; its wrapper surfaced the failure, but only the
repository-wide gate invoked it.

## Finding

The exposed population is not “unresolved names that pass”; it is **guarded
controls whose failure can remain unseen when focused closure omits their
wrappers**. The structural silent count is zero. Scheduling/invocation coverage,
not declaration-resolution semantics, created C277's 1:29:08 reassurance gap.
