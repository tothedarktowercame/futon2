From claude-11. Lean task, one existing module, one example. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

In `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`, branch
`darktower`, on top of `060a04bc0`): one example, through `step`, showing that
ambiguity changes the outer loop's choice. Review finding on 3d-i: in
`outer_ambiguity_matters` the risk term alone already prefers the reach-1 task,
so no example yet shows ambiguity deciding.

## The example

Two cascades whose picks differ, both `.ready`. The risk term alone,
`CascadeEFE.risk` of each cascade's predicted outcome (`push q a`) against the
outer preference kernel, orders them X before Y, strictly; the full
`outerScore` (risk + ambiguity) orders them Y before X, strictly; and
`step` chooses Y (`cascadeIndex` and `selection` stated).

Suggested shape: X picks a reach-1/2 task in a more preferred ending class,
Y a reach-1 task in a less preferred class, with C (and state counts) chosen
so that risk favours X but X's ambiguity, q(doable)·H(1/2), outweighs the
gap. Pick values where the comparison reduces to `Real.log` inequalities you
can prove (as in `learned_selection_click_*`). Numbers that work in floating point (claude-11's check, q(doable) = 1/2 from even state counts, both gains `none`): C over ending classes focused 1/2, related 1/5, stopTheLine 3/10 (others 0 or tiny if positivity is needed); X = focused, reach 1/2; Y = related, reach 1. Risk: X ≈ 0.514 < Y ≈ 0.714. Full: X ≈ 0.861 > Y ≈ 0.714. If no inputs make the two
orderings opposite under the definitions, stop and say why.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on the example's theorem: only `propext`,
  `Classical.choice`, `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).
