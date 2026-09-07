# F10 `foldC` witness

## What is declared

The positive module imports the ruled F10 carrier and defines its base on
`Outcome SeedObs` (`mathlib4:DarkTower/WarMachine/FoldCWitness.lean:1-11`).
The ruled-sum stack passed to `foldC` is empty, and the first theorem proves
that its fold is the base (`mathlib4:DarkTower/WarMachine/FoldCWitness.lean:13-17`).
This mirrors the runtime declaration: the only folded entry is `c-int`, which
is outside the ruled sum, while the three entries inside or potentially inside
are not folded (`futon2:src/futon2/aif/ruled_outcome_c.clj:52-84`).

The second theorem evaluates two non-commuting layers in both orders at the
ruled `groundedChange` outcome: add then double gives eight, while double then
add gives seven (`mathlib4:DarkTower/WarMachine/FoldCWitness.lean:19-36`).
Thus it elaborates the ordered `List.foldl` expression actually defining
`foldC` (`mathlib4:DarkTower/WarMachine/Holes.lean:7186-7189`).

## What is measured

The check evaluates the runtime namespace and selects only entries that are
both folded and inside the ruled sum
(`futon2:checks/fold_c_witness.clj:42-45`).  Independently, it parses the Lean
stack and derives layer ids from the records of the definitions actually named
in that stack (`futon2:checks/fold_c_witness.clj:28-40`).  Validation requires
those sets and the fixture set to agree
(`futon2:checks/fold_c_witness.clj:53-69`); all three are empty.

The positive receipt pins every witness declaration and the `foldC`
declaration itself (`futon2:holes/labs/wm-contract/fold-c-positive-receipt.edn:14-19`).
The generated registry entry records a passing check without a run identity
(`futon2:checks/witness-registry.edn:190-217`).

The regenerated accounting row is now:

> `:rung :formula-transcribed`; its ladder includes
> `:evidence :passing-witness-check` licensed by
> `futon2/checks/witness-registry.edn:190`; its next blocked rung is
> `:witnessed` because the fixture carries no run identity.

These fields are present at
`futon2:holes/labs/wm-contract/variable-situation-accounting.edn:796-820`.
The count moved exactly one row from `:type-transcribed` to
`:formula-transcribed`: 62→61 and 52→53 respectively; every rung at
`:witnessed` or above remains zero
(`futon2:holes/labs/wm-contract/variable-situation-accounting.edn:3217-3242`).

## What the controls establish

The order-negative file asserts equality after reversing the two
non-commuting layers (`mathlib4:DarkTower/WarMachine/FoldCOrderNegative.lean:7-12`).
The elaborator rejects it because `rfl` cannot identify the two ordered folds.
The folded-negative file inserts `addLayer` and asserts that the base is
unchanged (`mathlib4:DarkTower/WarMachine/FoldCFoldedNegative.lean:7-11`);
the elaborator rejects it because that fold is not definitionally the base.

The checker requires both a nonzero elaborator exit and message fragments that
name the expected sides of each failed equality
(`futon2:checks/fold_c_witness.clj:47-51` and
`futon2:checks/fold_c_witness.clj:71-85`).  Therefore an unrelated compiler
failure cannot certify either control.  The null control recomputes the same
positive facts through the same route and requires structural equality
(`futon2:checks/fold_c_witness.clj:71-94`); it passed with identical empty
runtime and Lean folded sets.

## Gate registration (added in review, 2026-09-07)

As first committed, `checks/fold_c_witness.clj` was a new file under `checks/`
that the workspace gate's completeness alarm did not classify, so the
`:check-inventory` step reported it as unknown and the gate exited 1
(`futon2:checks/wm_workspace_gate.clj:278-285`).  The check was also run by no
gate at all, so the registry's `:result :passed` — the very field the F6 ladder
reads to license `:formula-transcribed` — stood as an assertion rather than as
something a gate re-establishes.

Both are now closed the way every other witness in the registry is: the file is
classified (`futon2:checks/wm_workspace_gate.clj:252`), the positive check runs
as a gate step beside `fold_witness.clj`
(`futon2:checks/wm_workspace_gate.clj:309`), and the two Lean rejections run as
control steps (`futon2:checks/wm_workspace_gate.clj:546-549`).

## Deliberately not declared

The fixture explicitly limits itself to a declaration witness with no run
identity (`futon2:holes/labs/wm-contract/fold-c-reference.edn:1-7`).  No layer
is added to the live ruled sum, no scorer or tick is wired, and no registry
write is performed by runtime code; the measured runtime caller set remains
empty (`futon2:holes/labs/wm-contract/runs/F10-outcome-domain/02-runtime-fold.edn:82`).
In particular this evidence cannot reach
`:witnessed`; the accounting row records that exact boundary
(`futon2:holes/labs/wm-contract/variable-situation-accounting.edn:801-817`).

## Recorded pointer drift

No pointer from this packet was found to aim at the wrong clause.  The runtime
sheet's final paragraph does point to the current fold definition
(`futon2:holes/labs/wm-contract/C577-F10-runtime-fold.md:100-107`).

## Scope limits

This slice supplies one positive Lean witness, two deliberately rejected Lean
claims, one checker, one hand-derived fixture, one pinned receipt, one witness
fragment, its generated registry entry, and regenerated accounting.  It does
not amend `Holes.lean`, whose manifest names `foldC` at
`mathlib4:DarkTower/WarMachine/Holes.lean:7919-7921`, and it supplies no
LIVE-RUN RIDER evidence.
