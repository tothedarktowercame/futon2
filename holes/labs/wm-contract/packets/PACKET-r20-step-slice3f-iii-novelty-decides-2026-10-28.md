From claude-11. Lean task, one existing module, one example. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

In `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`, branch
`darktower`, on top of `d845c84fb`): the example deferred from 3f-ii. Two
cascades whose picks are equal in class, reach (strictly between 0 and 1) and
gain, so risk and ambiguity are equal, but whose Giraffe statuses differ with
different state counts (e.g. `.ready` with large counts, `.undetermined` with
small counts). Prove through `step` that the less-known pick is chosen
(`cascadeIndex`, `selection`), with the two `outerScore`s strictly ordered,
and that their `stepG` parts are equal (so novelty decides). Write the
preference-kernel admissibility lemma it needs (you flagged this in 3f-ii).

Gates: `lake build DarkTower.WarMachine.R20Step`; no `sorry`, `axiom` or
`native_decide`; `#print axioms` on the example (only `propext`,
`Classical.choice`, `Quot.sound`), pasted; commit only `R20Step.lean` by
explicit path.
