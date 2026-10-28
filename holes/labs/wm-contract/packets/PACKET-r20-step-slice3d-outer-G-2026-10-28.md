From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3d, in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`,
branch `darktower`, on top of `d7649b2f2`): score each task-selection cascade
with the same G the inner loop uses, `CascadeEFE.stepG q a c = risk (push q a)
c + ambiguity q a` (`DarkTower/WarMachine/CascadeEFE.lean`), over an outer
model built only from facts about tasks as wholes. Today's score
(`cascadeScore`, a `classRisk` of the pick's predicted ending class) is
risk-only; Joe's rule is that the outer loop is the same loop with different
policies, so it uses the same G.

Read first: `/home/joe/code/futon2/holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md`
(the model, decided with Joe 2026-10-28). Hard rule from it: nothing about a
task's inner (tactical) policies is built or read to score the task.

Do not edit any module other than `R20Step.lean`; reuse `CascadeEFE`
definitions (`ProbabilityKernel`, `point`, `push`, `stepG`, `risk`,
`ambiguity`, `finiteKernel`) rather than re-defining them.

## Types

- `ReadinessStatus := ready | notReady | undetermined` (Giraffe's three open
  statuses). Replace `GiraffeFacts.ready : Bool` by `status : ReadinessStatus`
  and define `GiraffeFacts.ready (f) : Bool := f.status = .ready`, so slice-3c
  statements keep their meaning; update 3c proofs/examples as needed.
- `TaskState := doable | blocked`.
- `ReadyGain := none | one | several` (items made ready by the pick landing:
  0, 1, ≥2), from the candidate's `unblocks`.
- Outer observation type `OuterOutcome := EndingClass × ReadyGain`.
- `Makeup` gains starter kits (positive rationals, like C's kit): 
  `stateKit : ReadinessStatus → TaskState → ℚ` and `gainKit : ReadyGain → ℚ`,
  with positivity proofs. `State` gains the learned counts
  `stateCounts`, `gainCounts`, initialised from the kits by `init`.
  (Updating them is slice 3e; in 3d `step` carries them unchanged, and the
  docstring says so.)

## The model for a pick k

- `q`: `stateCounts (facts k).status` normalised, a kernel `Unit → TaskState`.
- `a : TaskState → OuterOutcome`: `doable` ↦ (class of k under
  `outcomeClass o.focus m.relation`, gain of k) with mass `k.reach`, and
  (`stopTheLine`, `none`) with mass `1 − k.reach`; `blocked` ↦ (`stopTheLine`,
  `none`). If k's class is unknown (`outcomeClass = none`), `doable` also goes
  to (`stopTheLine`, `none`). Require `0 ≤ reach ≤ 1` where needed (state it as
  a hypothesis or clamp, and say which).
- `C : Unit → OuterOutcome`: the product of `normalizeC s.c` and normalised
  `gainCounts`. Docstring: a product of two learned marginals; whether it
  meets Q9's factorization ruling is not claimed here.
- No pick: score the point outcome (`stopTheLine`, `none`) against C.

`cascadeScore` becomes `stepG q a C` with the admissibility argument
`stepG` needs; if admissibility cannot be proved for every input, make the
score `EReal` with `⊤` exactly when inadmissible, as `classRisk` does, and
prove when it is finite. Keep `chosenCascadeEntry`/`step` otherwise as they
are.

## Theorems

1. `cascadeScore` equals `CascadeEFE.stepG` of the model above (by `rfl` or
   unfolding) whenever finite.
2. Ambiguity closed form: for a pick k, `ambiguity q a =
   q(doable) · binaryEntropy (k.reach)` (whatever the entropy is called in
   `CascadeEFE`; derive it).
3. Example A (ambiguity matters): two ready candidates, same class, same
   gain, reach 1 vs reach 1/2; the reach-1 cascade's score is strictly lower.
4. Example B (items made ready matter): two ready candidates, same class and
   reach, gain `several` vs `none`, with `gainKit` favouring more; the
   `several` cascade's score is strictly lower.
5. All earlier theorems still build. Where an earlier example's numbers change
   under the new score (e.g. 3b's learned-C flip), restate the new values and
   keep the claim (strict, both clicks) if it still holds; if a claim no
   longer holds, say so in the summary rather than weakening it silently.

Prove by unfolding; no predicates on arbitrary click sequences.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 1, 3 and 4: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).

If `CascadeEFE`'s types do not fit (e.g. `Fintype` on `OuterOutcome`, or the
admissibility proof), stop and bell back with the question.
