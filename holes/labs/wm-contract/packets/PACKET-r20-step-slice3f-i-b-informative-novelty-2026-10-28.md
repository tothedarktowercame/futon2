From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

In `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`, branch
`darktower`, on top of `62e4f91a2`): replace `outerNovelty`. Your 3f-i
finding is right and it exposes a defect in my packet: posterior-weighting
eq. 57's per-count term collapses, by total expectation, to the prior-weighted
term, so the result does not depend on how informative the outcome is. An
outcome independent of the state would still score full novelty. That is not
information gain about the parameter. Do not change `step` or `outerScore`.

## Replacement

For a pick with status σ, θ = P(doable | σ) ~ Beta(a, b) with
a = stateCounts σ .doable, b = stateCounts σ .blocked, and the scored kernel
A (`pickedOuterKernel landing reach`, or the stopped kernel). The outcome
probability is P(o | θ) = θ·A(o|doable) + (1−θ)·A(o|blocked). The expected
information gain is the mutual information I(θ; o); use its second-order
(Fisher-information) approximation:

    outerNovelty = ½ · Var(θ) · Σ_{o : P(o) > 0} (A(o|doable) − A(o|blocked))² / P(o)
    Var(θ) = a·b / ((a+b)² · (a+b+1)),   P(o) = (a·A(o|doable) + b·A(o|blocked)) / (a+b)

No pick: 0. Docstring: this is the standard second-order expansion of the
expected KL between posterior and prior (expected Fisher information times
parameter variance / 2), of the same asymptotic order as Da Costa et al. 2020
eq. 57; in the fully revealing case it is 1/(2(a+b+1)), against
`PatternThetaNovelty.novelty a b = 1/(2(a+b))`. (claude-11 checked it against
a Monte-Carlo estimate of the exact I(θ;o): Beta(1,1) revealing 0.167 vs
0.193, half-informative 0.056 vs 0.062, uninformative 0 vs 0; Beta(20,5)
revealing 0.0192 vs 0.0195.) Keep importing `PatternThetaNovelty` only if a
theorem below uses it.

## Theorems

1. Uninformative: if A(·|doable) = A(·|blocked), `outerNovelty = 0`.
2. Revealing: if the doable and blocked outcome supports are disjoint,
   `outerNovelty = 1/(2(a+b+1))`; and this is `≤ PatternThetaNovelty.novelty a b`.
3. Informativeness matters: same counts, reach 1 vs reach 1/2 (same landing
   ≠ stop): reach 1 gives strictly larger novelty.
4. Counts matter: the 3f-i example (ready with large counts vs undetermined
   with small counts) still strict.
5. Nonnegativity for positive counts.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`, no
  `native_decide`.
- `#print axioms` on theorems 1–4, `learned_selection_click_one`,
  `ambiguity_changes_outer_choice`: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Remove the 3f-i theorems that no longer hold (say which); commit only
  `DarkTower/WarMachine/R20Step.lean` (explicit path).
