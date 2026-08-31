# C122 — free preference constant census

Date: 2026-08-31

The in-language census is non-empty.  It enumerates the complete named
preference surface in `Holes.lean` and records whether each declaration receives
its preference-bearing value/layers as input or supplies a global value itself.

| Declaration | Disposition |
|---|---|
| `C` | **free constant**: returns vertex-local `Obs v → ℝ` without a preference distribution, base, or layer input |
| `G` | risk and epistemic grade are parameters |
| `recordedSnatchRisk` | pinned ablation fixture, not deployment C |
| `PreferenceDistribution` | carrier type, not a value |
| `predictiveOutcomeRisk` | `Cdist` is a parameter |
| `expectedFreeEnergy` | `Cdist` is a parameter |
| `softmax` | habit prior is a parameter |
| `PreferenceLayer.prefers` | supplied as structure data |
| `foldC` | base preference and ordered layers are parameters |

Lean decides that filtering this census for `freeConstant` yields exactly
`[vertexLocalC]`.  Consequently the historical `machineHasNoC` claim is false:
the declaration now records the counterexample as
`freePreferenceConstants ≠ []` rather than retaining an unprovable `Prop`.

This finding does not choose an implementation for `C`; it shows why the
standing refusal is material.  Until `C` is removed or changed to receive a
declared preference carrier/stack, the formal spine itself contains one free
preference value constructor.
