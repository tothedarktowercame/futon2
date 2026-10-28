From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Slice 3b of `/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`,
in `DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`, branch
`darktower`, on top of slice 3a `75efd2e9b`): a two-click example in which the
learned C carried in `State` changes which task-selection cascade `step`
chooses, with the scores strictly different at both clicks.

Review finding on 3a that this also addresses: in the 3a example both
cascades pick a `.focus` candidate with reach 1/2, so the two scores are
equal and cascade 0 wins only by menu order: the tie case the docstring calls
a defect. Swapping the menu would change the result. Do not delete that
example; change its docstring to say plainly that the scores tie and the
choice is menu order, so nobody reads it as a selection.

Do not change `step`, `Makeup`, `State`, `Observation`, `Click`, or any
existing theorem.

## The example (values checked by hand; check them against the definitions)

- Makeup: `selectionExampleMakeup` but `kappa := 1` (so one observed ending
  moves C visibly; say so in the docstring), and `relation` with target 1
  `.focus` (class `.focused`) and target 3 `.associated` (class `.related`).
  ω_c = 9/10 and η_c = 1 as in `precisionExampleMakeup`.
- Field: `⟨1, 1/2⟩` and `⟨3, 1/2⟩` (equal reach, so only C separates them).
- Cascade A: `[gate (target = 1)]`; cascade B: `[gate (target = 3)]`.
- Both observations have `endingClass := .related`.

Click 0: C = kit = (focused 55/100, related 35/100, unrelated 5/100,
stopTheLine 5/100). With r = 1/2, risk = r·log(r/C_k) + (1−r)·log((1−r)/C_stop),
so A's risk < B's risk iff C_focused > C_related. A is chosen (≈1.104 vs ≈1.330).

After click 0's `.related` ending, c = 9/10·(55,35,5,5)/100 + (0,1,0,0)
= (99,263,9,9)/200, so C at click 1 = (99, 263, 9, 9)/380. Now
C_related > C_focused and B is chosen (≈1.851 vs ≈1.362).

## Theorems

1. Click 0: A's score < B's score (strict, as `EReal`), `cascadeIndex = 0`,
   selected target 1.
2. Click 1: `normalizeC` of the carried c equals `(99, 263, 9, 9)/380`
   (per ending class).
3. Click 1: B's score < A's score (strict), `cascadeIndex = 1`, selected
   target 3.

Prove by unfolding `run`/`step`. The comparison reduces to
`Real.log_lt_log` on the two C entries, since the stop-the-line term is the
same for both cascades; prove that reduction as a lemma rather than
evaluating logs numerically.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 1 and 3: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path).

If my arithmetic is wrong, prove the correct values and say what differed;
if the flip does not happen under the real definitions, stop and bell back.
