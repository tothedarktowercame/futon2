From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

(Dispatch only after slice 1b is reviewed; it builds on 1b's `Makeup`.)

## Goal

Slice 2 of `/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`:
precision and valence computed inside `step`, in
`DarkTower/WarMachine/R20Step.lean` (`/home/joe/code/mathlib4`, branch
`darktower`). Do not edit `R20Run.lean` or `PrecisionFromObservation.lean`;
reuse their definitions (`descend`, `charge`, `affectiveEvidence`,
`valenceUpdate`, `predict`, `priorPolicy`, `posteriorPolicy`,
`expectedGChange`, `firstIterate`, `TickRecord`).

## What moves where

- `Makeup` gains `L : R20Run.AffectiveLevel`, the initial valence belief
  `q₀ : Valence → ℝ`, and `precisionIterations : ℕ` (see below). `Policy`
  needs `[Fintype Policy] [Nonempty Policy]` from here on.
- `State` gains `valence : Valence → ℝ`.
- `Observation` gains the click's `E G F : Policy → ℝ`. (They arrive from
  outside for now; computing E from the habit counts and G, F from the model
  is later work. Say so in the docstring.)
- `Click` gains a `precision : PrecisionFromObservation.TickRecord Policy`
  and the `valence` it started from.

## The computation inside `step`

1. `betaPrior := descend m.L s.valence`.
2. `betaPosterior := iterate m.precisionIterations` of
   `fun b => firstIterate betaPrior b E G F`, starting from `betaPrior`.
   Friston 2017 eq. 2.7 says the update is "iterated to convergence"
   (`refs/friston2017.txt:660-685`); SPM's routine runs a fixed number of
   iterations. The count is a `Makeup` field, never a literal in `step`
   (spec: no hard-coded rates). `IsPosteriorTemperature` is a predicate, not a
   computation, so it cannot be `step`'s definition.
3. The precision row: prior/posterior policies at `betaPosterior`, as
   `TickRecord` requires.
4. Next valence: `predict m.L (valenceUpdate m.L s.valence betaPrior betaPosterior)`.

## Theorems (for every makeup and every observation list)

1. Port of `run_betaPrior_step`: click n+1's `betaPrior` equals
   `descend L (predict L (valenceUpdate L q_n β_n β̄_n))` — now by unfolding
   `step`, with no `RunCertified` hypothesis.
2. Port of `zero_charge_keeps_valence` along the run: if a click's
   `betaPosterior = betaPrior`, the next valence is `predict L` of the current one.
3. Port of `run_betaPrior_constant_of_flat`: with flat rates, a stochastic
   `B` and `Σ q₀ = 1`, `betaPrior = r` at every click. Carry over only the
   hypotheses the proof actually needs; if `nondeg` is still needed, say why.
4. Non-discriminating evidence: if F is constant across policies, the
   computed `betaPosterior = betaPrior` for every `precisionIterations`
   (use `posterior_eq_prior_of_constant_F`). This is the one case where the
   iterate is the exact fixed point; state it as such.
5. The fixed-point link: if the iteration has reached a fixed point
   (`firstIterate betaPrior β̄ E G F = β̄`) and `0 < β̄`, the click's precision
   row satisfies `IsPosteriorTemperature`. Do not claim convergence in general
   (the module says root existence is not proved).
6. Keep every slice-1/1b theorem building.

Prove by unfolding `step`. No predicates on arbitrary click sequences; no
invented bad runs. Extend the concrete example with a two-policy E, G, F and
show the computed `betaPrior` at click 1.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on theorems 1, 3 and 4: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- Docstrings cite Friston 2017 eq. 2.7 and the lines R20Run cites for the
  valence update.
- Commit only `DarkTower/WarMachine/R20Step.lean` (explicit path; never `git commit -a`).

If the spec does not fit the existing types, stop and bell back with the question.
