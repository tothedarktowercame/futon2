# D3/D4 runtime–Lean correspondence and live-click disposition

Date: 2026-10-17

## Integrated runtime

- `f36d6c1ac` wires the selected reading-family cascade into the held runner
  phase after the single G selection.
- `2869a1d1d` binds that held phase to the D4 same-click retry protocol.
- The selected structural identity and compiled executable identity are
  distinct.  The selected/scored/saved structural record retains the first;
  the saved executable refinement and its journal retain the second.
- The live error carries `:resume-port {:retry fn}`.  The persisted checkpoint
  deliberately removes that executable capability while retaining the two
  identities, phase, holes and re-entry inputs.

Focused runtime gate on canonical main:

```text
Ran 67 tests containing 463 assertions.
0 failures, 0 errors.
```

The gate covers the executable carrier, selection identity, compiler,
journal, production runner seam, D4 protocol and selected-reading runner.
No live click was run.

## Lean correspondence

Mathlib4 commit `2c21e66ec4` corrects `IdentityChain`: selection, scoring and
the structural save share one nonzero identity; executable save and journal
share a second nonzero identity.  They are related by the proved exact
forgetful projection, not falsely equated.  A journal bound to the structural
identity is a decided negative witness.

```text
lake env lean DarkTower/WarMachine/ExecutableCascadeRefinement.lean
exit 0
```

## Production ports installed (amendment)

Commit `0353002dc` installs both composition ports:

1. `occurrence-readings` carries each occurrence square belonging to the
   selected cascade policy into the canonical occurrence source.  The runner
   field named `:action` is only the envelope retaining that selected policy;
   neither G nor this adapter scores an atomic action;
2. `full-loop-runtime/run-opportunity!` installs a late-bound D4 handler using
   the live job ledger and explicit author/reviewer cast.  It contacts both in
   work mode, validates the reviewed response, reloads only the named
   namespace, checks its loaded source digest and retries the held phase.

Focused production-composition gate:

```text
Ran 87 tests containing 522 assertions.
0 failures, 0 errors.
```

## Live-click disposition after installation: CONDITIONAL NO-GO

Both downstream ports now exist.  The remaining condition is upstream
evidence: the latest real reading-family selection record
`2026-10-16-d845555a-88f3-442b-aef5-f6d17379cae9` contains no
`:blend-square`, although older interpreted-route records do.  The runtime
must not invent one from fragment roles.  The required source is a
pre-selection, occurrence-keyed authoring registry: for every validated
target-reading citation it binds the four situation elements and their
sources, emits a square accepted by `three-halves-square/validate`, and binds
that square to target-reading digest, occurrence identity and pattern pin.
Construction consumes those records before G compares policies.  Missing or
void records must trigger the next build packet's authoring phase and
re-decision; that producer is not installed by D3/D4, and they are not filled
from the eventual selected-policy envelope.  No click was spent in this
installation or check.
