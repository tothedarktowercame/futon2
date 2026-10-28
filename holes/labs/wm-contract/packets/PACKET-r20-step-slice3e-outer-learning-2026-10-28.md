From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3e, in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`,
branch `darktower`, on top of `cbb24d1b6`): the outer level learns. After each
click `step` updates the two outer count fields from what was observed, with
explicit rates (no literal 1s: memory/no-hard-coded-rates). Design:
`/home/joe/code/futon2/holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md`
§Learning. The novelty term is NOT in this slice (it is 3f).

## Changes

- `Observation` gains `observedGain : ReadyGain` (how many items the picked
  task's landing actually made ready; supplied from outside, like `field`;
  docstring says so).
- `Makeup` gains rates `etaState, omegaState, etaGain, omegaGain : ℚ` with
  `0 < η`, `0 < ω ≤ 1`, documented as the Smith, Friston & Whyte 2022 §4
  learning/forgetting rates, as for the other counts.
- **Gain counts**: `gainCounts' g = ω_gain · gainCounts g + η_gain · [g = o.observedGain]`
  when the click selected a task; decay only (no addition) when
  `selection = none`.
- **State counts** (Dirichlet learning of the hidden doable/blocked state):
  for the picked task k with status σ = `(o.facts k.target).status`, the
  observed outcome is `(o.endingClass, o.observedGain)`. Its posterior over
  `TaskState` is `q(s) · a(obs | s) / Σ_s' q(s') · a(obs | s')`, with q and a
  the same kernels `outerModel` builds for the chosen cascade. Then
  `stateCounts' σ s = ω_state · stateCounts σ s + η_state · posterior s`, and
  every other status only decays. If the observed outcome has zero
  predicted probability (the denominator is 0), add nothing and only decay;
  say so in the docstring (the observation was impossible under the model;
  later work may treat it as a model failure). No selection: decay only.
- C (ending-class counts) keeps its slice-1b update unchanged.

## Theorems

1. `OuterStateInvariant` still holds for `init` and is preserved by `step`
   under the new updates (positivity survives: ω > 0 keeps positive counts
   positive; additions are nonnegative). `click_cascadeScores_eq_outerScore`
   and every other theorem still builds.
2. Closed form of one state update: for a selected click, the new counts at
   σ equal `ω · old + η · posterior`, and at σ' ≠ σ equal `ω · old`.
3. Example: a ready task that ends `.stopTheLine` with gain `none` moves
   probability toward `blocked` for status `.ready` at the next click
   (`q'(blocked) > q(blocked)`), and a ready task that ends in its class
   with its predicted gain moves it toward `doable`. Use explicit rationals
   with reach strictly between 0 and 1.
4. All slice 1–3d results still build; examples whose state counts change
   only through `step` should keep their claims (3b: check; report any
   change honestly).

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 1 and 3: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).
- If too large for one turn: commit the gain update + invariant first, stop,
  and say so; the state update follows.
