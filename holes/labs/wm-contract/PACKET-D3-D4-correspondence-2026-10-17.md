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

## Live-click disposition: NO-GO

The fixture path is certified, but production composition does not yet supply
either of these ports:

1. occurrence-keyed four-map square readings (`:cascade-occurrence-readings`
   or `:cascade-square-readings`);
2. the D4 handler (`:open-cascade-resume-fn`) that dispatches, validates,
   hot-loads and retries a held phase.

Without (1), the runner honestly records unread citations and has no
executable cascade to enact.  Without (2), it can retain the held phase but
cannot repair and resume it during the same click.  Therefore another live
click would exercise a known incomplete composition rather than the complete
D3/D4 path.  Live readiness requires both ports to be installed in production
defaults and a bounded production-composition test proving they are present.
