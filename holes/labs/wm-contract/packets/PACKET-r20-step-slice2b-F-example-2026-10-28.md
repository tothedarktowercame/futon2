From claude-11. Lean task, one existing module, one example. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 2b of `/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`:
add a concrete two-click example to `DarkTower/WarMachine/R20Step.lean`
(`/home/joe/code/mathlib4`, branch `darktower`, on top of slice 2 `4687b7ac5`)
in which F changes the computed precision and therefore the next click's
prior precision.

Why: the slice-2 example has constant F, so βpost = βprior, the affective
charge is 0 and valence never moves; deleting the valence update from `step`
would leave it passing. Joe (2026-10-28): "we need to be able to have F have
an impact."

Do not change `step`, `Makeup`, `State`, `Observation` or any existing
theorem. Do not edit `R20Run.lean` or `PrecisionFromObservation.lean`.

## The example (values chosen so the softmaxes are exact)

Policy `PrecisionFromObservation.TwoPolicy` (`.a`, `.b`). A new makeup equal
to `precisionExampleMakeup` except `precisionIterations := 1`
(`L := R20Run.hespLevel`, `q₀ := R20Run.half`, so βprior at click 0 is 5/4).
A new observation with

- `E := fun _ => 1` (so `Real.log (E p) = 0`)
- `G := fun | .a => 0 | .b => 5/4 * Real.log 3`
- `F := fun | .a => 0 | .b => -Real.log 3`

Hand computation at click 0 (β = 5/4), with `x := 5/16 * Real.log 3`:

- prior policy scores `0, -log 3` → `priorPolicy = (3/4, 1/4)`
- posterior scores `0, 0` → `posteriorPolicy = (1/2, 1/2)`
- `expectedGChange = (1/2 - 1/4) * (5/4 * log 3) = x`
- βpost at click 0 = `5/4 + x`
- next valence (identity B): `q₊ = (2 + x)/(4 + 5x)`, `q₋ = 2(1 + 2x)/(4 + 5x)`
- βprior at click 1 = `(10 + 17x)/(8 + 10x)`

Check my arithmetic against the definitions; if it is wrong, prove the
correct closed form and say what differed.

## Theorems

1. Click 0's `precision.betaPosterior = 5/4 + 5/16 * Real.log 3`.
2. Click 1's `precision.betaPrior = (10 + 17x)/(8 + 10x)` with x as above
   (state it with x expanded or via a local `def`; either is fine).
3. Click 1's `precision.betaPrior ≠ 5/4` (from `0 < Real.log 3`), and
   strictly greater than 5/4.

Compute by unfolding `run`/`step`; the useful facts are
`Real.exp_log`, `Real.log_one`, `Real.exp_neg`, `Real.log_pos`, and
`softmax` on `TwoPolicy` as a two-term sum. Keep the slice-2 constant-F
example as it is (it documents the fixed-point case).

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 2 and 3: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path; never `git commit -a`).

If the definitions make an exact closed form impossible, stop and bell back
with what you found rather than weakening the statements.
