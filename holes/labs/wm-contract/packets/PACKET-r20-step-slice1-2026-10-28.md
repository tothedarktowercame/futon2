From claude-11. Lean task, one new module. Bell claude-11 back with a summary + commit shas.

## Goal

Write `DarkTower/WarMachine/R20Step.lean` in `/home/joe/code/mathlib4` (branch
master). It is slice 1 of
`/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`.
Read that spec first; it says why. Do not edit `R20Run.lean` or any other
existing module.

The run is computed, not described:

    structure Makeup ...      -- fixed for the whole run
    structure State ...       -- what one click hands the next
    structure Observation ... -- what arrives at a click
    structure Click ...       -- what one click produced (its rows)
    def init (m : Makeup) : State
    def step (m : Makeup) (s : State) (o : Observation) : Click × State
    def run  (m : Makeup) (obs : List Observation) : List Click   -- fold of step from init m

`Click` has no field for an outside candidate list, menu or selection.

## Quantities in this slice

Reuse the existing update functions; do not restate them.

- **d counts** (`Hidden → ℝ`, `Hidden := Fin 2` as in `R20Certificate`). The
  click's evidence prior is `d / Σ d`. Its posterior is
  `EvidenceFromTheProcess.posterior prior m.A o.observation`, computed inside
  `step` (`m.A` is the likelihood in `Makeup`). Next `d` = `d + posterior`
  (Friston 2017 eq. 2.7, as in `R20Run.Carries.counts`).
- **habit**: `R15TemporalHierarchy.SlowState Policy`, advanced by
  `R15TemporalHierarchy.advance` with the observation's habit outcome.
- **θ**: `SlowState PatternIdentity`, advanced by
  `PatternTheta.advanceWitnessed pattern before o.thetaOutcomes`.
- **learned preferences C** (new). Counts `c` over the four terminal ending
  classes of `CTauClassPreference.EndingClass`: focused, related, unrelated,
  stopTheLine (`notYetEvaluated` gets no count). `init` sets
  `c₀ = m.kappa • (55, 35, 5, 5)/100`, with `0 < m.kappa` in `Makeup`. Each click adds
  1 to the count of `o.endingClass`. The click's C is `c / Σ c` as a
  `CTauClassPreference.Distribution` (0 on notYetEvaluated), used in place of
  `terminalPreference` in a `classPreference`-shaped function. Choose ℚ or ℝ
  to match `CTauClassPreference` (it is ℚ).
- **focus**: `State.previousFocus : Nat`; the click records it, and the next
  state takes the observation's focus.
- **run id**: `State.runId : Nat`, incremented by `step`.

Precision and valence are slice 2. Say so in the module docstring, and leave
them out of `State`.

## Theorems (all for every `m` and every `obs`)

1. `c` at click n = `c₀ +` counts of ending classes in the first n observations;
   C at click n is that normalised. With n = 0 it is the 55/35/5/5 kit.
2. `d` at click n = `d₀ +` sum of the first n posteriors (port of
   `run_concentration_accumulates`).
3. Habit at click n is the fold of `advance` over the first n outcomes (port of
   `run_habit_accumulates`); θ likewise with `advanceWitnessed`.
4. Every click's evidence row satisfies `EvidenceConforms` (by unfolding step).
5. Run ids strictly increase along `run`; each click's `previousFocus` is the
   previous observation's focus.
6. C at every click is a probability distribution (nonnegative, total mass 1),
   using the positivity of `kappa`.

Prove each by unfolding `step`/`run`. Do not add predicates on arbitrary click
sequences, and do not build bad runs to show something rejects them.

Add a short concrete example at the end (`#eval` or `example ... := by decide/norm_num`):
a two-click run where the first click ends `focused`, showing the C counts move.

## Gates

- `lake build DarkTower.WarMachine.R20Step` succeeds; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 1, 2, 4 lists only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output in your reply.
- Each definition has a docstring citing the source line the existing module
  cites (Friston 2017 eq. 2.7; Smith, Friston & Whyte 2022 for the `c` update).
- Commit only `DarkTower/WarMachine/R20Step.lean` (stage that path explicitly;
  the checkout is shared, never `git commit -a`).

If something in the spec does not fit the existing types, stop and bell back
with the question rather than changing the spec.
