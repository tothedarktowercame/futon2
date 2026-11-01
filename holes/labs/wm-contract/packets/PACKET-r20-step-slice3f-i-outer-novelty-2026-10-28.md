From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3f-i, in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`,
branch `darktower`, on top of `6b475f7eb`): define the outer level's novelty
term, the expected information gain about the doable/blocked counts that a
pick would yield. Do NOT change `step` or `outerScore` yet (that is 3f-ii).
Design: `/home/joe/code/futon2/holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md`
§"Learning as a preference", part 1.

Sources: Da Costa et al. 2020, eq. 42 (G = risk + ambiguity − parameter
information gain) and eqs. 55–58 (the one-count Dirichlet approximation,
`refs/dacosta2020.txt:1520-1563`), already formalised for a binary pattern
parameter in `DarkTower/WarMachine/PatternThetaNovelty.lean` (`novelty a b =
1/(2(a+b))`, `novelty_lt_of_total_lt`). Reuse that module (import it); do not
redefine its `novelty`.

## Definition

For a pick k with status σ, counts a = stateCounts σ .doable,
b = stateCounts σ .blocked, prior q from those counts, and the scored
observation kernel `pickedOuterKernel landing reach` (or the stopped kernel):

    outerNovelty = Σ_o P(o) · Σ_s P(s | o) · (1/(2·c_s) − 1/(2·(a+b)))

with c_doable = a, c_blocked = b, P(o) = Σ_s q(s) A(o|s), P(s|o) the posterior
(0 when P(o) = 0). This is eq. 57's per-count term weighted by the posterior
responsibility, because the state is observed only through the outcome.
Docstring: applying the source's observation-parameter arithmetic to the
outer state counts is our model choice, as in `PatternThetaNovelty`.
No pick: novelty 0.

## Theorems

1. Reduction: when the outcome reveals the state exactly (reach = 1 and the
   landing is not `(.stopTheLine, .none)`), `outerNovelty =
   PatternThetaNovelty.novelty a b`.
2. Nonnegativity for positive counts.
3. Monotone: with the kernel fixed and the ratio a:b fixed, larger total
   counts give strictly smaller novelty (or, if only the reduced case is
   provable, state it there and say so).
4. Example: two picks identical except status — one `.ready` with large
   counts, one `.undetermined` with small counts — the undetermined pick has
   strictly larger novelty.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`, no
  `native_decide`.
- `#print axioms` on theorems 1, 3 and 4: only `propext`, `Classical.choice`,
  `Quot.sound`. Also run `#print axioms` on `learned_selection_click_one` and
  `ambiguity_changes_outer_choice` and paste those too.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).
