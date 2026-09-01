# C426 — model-reduction negative-control regression

**Date:** 2026-09-01  
**Disposition:** repaired; exact mutation retained

## Failure window

The C277 control originally referred to the public `alpha11` and `alpha21`
definitions in `ModelReductionFreeEnergyChangeWitness.lean`. Mathlib4 commit
`b068089813` replaced those definitions with the structured
`reductionChangeReference` fixture but did not update
`ModelReductionFreeEnergyChangeValueNegative.lean`. That commit is the start of
the slip. Futon2 commits `f30a8e7` and `a6e1c24` changed positive-proof receipt
handling; neither changed this negative fixture or its wrapper and neither caused
the regression.

With the old names gone, Lean treated the stale identifiers as implicit
variables and emitted unrelated invalid-argument and unsolved-goal diagnostics.
Those diagnostics did not satisfy the fixture's `#guard_msgs`, so Lean exited 1.
The wrapper correctly requires a guarded negative fixture to exit 0 and therefore
returned exit 2, `mutation-slipped`.

## Meaning of the mutation

The mutation remains sharp: the reference reduction-free-energy change is
exactly `log 2`, while the negative asserts that the same quantity is zero. The
witness did not legitimately absorb the perturbation; its negative fixture had
lost its referent.

Mathlib4 commit `444c22e92c` restores the negative to the same structured
`reductionChangeReference` used by the positive and unfolds that fixture. It does
not make the mutation coarser or change the positive theorem.

The blind interval is bounded exactly by the mathlib4 commit timestamps:
`b068089813` at 2026-09-01 01:51:00 UTC through `444c22e92c` at 03:20:08 UTC,
**1 hour 29 minutes 8 seconds**. The control was not silently green during that
interval: whenever invoked, its guarded Lean file exited 1 and its wrapper
reported exit 2, `mutation-slipped`. The exposure was that focused binding runs
did not invoke it; the later repository-wide gate did.

## Verification

- Positive witness: PASS.
- `--negative-value`: PASS, value mutation rejected for the guarded diagnostic.
- `--negative-type`: PASS, wrong semantic type rejected for the guarded
  diagnostic.

This is an instance of defect class 1 at the negative-control layer: the intended
mutation no longer participated in the rejection, so the control could not earn
its advertised success. The repository-wide gate detected the slip; focused
binding checks had not been running this control.
