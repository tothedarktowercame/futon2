From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3d-iv, in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`,
branch `darktower`, on top of `a4d8c239a`). 3d-ii switched `step` to
`carriedOuterScore` (outer `CascadeEFE.stepG`) and put the 3a and 3b selection
examples inside a block comment (about lines 1600–1867) because their
risk-only arithmetic no longer applies. Bring the selection examples back as
live, proved statements about the new score, and close one gap in the switch.

Part 1 (guards) is done: `a4d8c239a` (`OuterStateInvariant`, `click_cascadeScores_eq_outerScore`). Use those results; this packet is part 2 only.

## 2. The examples, recomputed

- 3a (menu-order tie): restate under the new score; say whether it still ties.
- 3b (learned C flips the choice from A at click 1 to B at click 2): restate
  with the new values. Keep the strict claim at both clicks if it holds; if it
  does not, prove what does hold and say so in the summary. Do not change the
  example's inputs to rescue it without saying so.
- New (review finding on 3d-i): through `step`, two cascades where the risk
  term alone (`CascadeEFE.risk` of the predicted outcome against C) orders
  them one way and the full score orders them the other way, both strictly,
  because of ambiguity. Suggested shape: a reach-1/2 task in a more preferred
  class against a reach-1 task in a less preferred class, C chosen so risk
  favours the first. If no such inputs exist under the definitions, stop and
  say why.

Delete the block comment once its content is replaced; no dead code left.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on the 3b theorems and the new example: only `propext`, `Classical.choice`, `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).
- If too large for one turn: commit the 3a/3b restatements alone, delete what they replace from the block comment, and stop.
