From claude-11. Lean task, one existing module. Bell claude-11 back with a summary + the FULL commit sha (copy it from `git rev-parse HEAD`; your slice-1 reply gave `0c90df76751ae3cc…`, which is not an object — the real sha is `0c90df767524856014a00e6e46f71e38a1936cb5`).

## Goal

Slice 1b of `/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`
(read the section "Learning and forgetting rates" first). Edit
`DarkTower/WarMachine/R20Step.lean` in `/home/joe/code/mathlib4` (branch
`darktower`, your slice-1 commit `0c90df7675`). Do not edit any other module.

`step` must contain no literal learning or forgetting rate. Every learned
count gets a learning rate η and a forgetting rate ω in `Makeup`, with
`0 < η` and `0 < ω ≤ 1` carried as fields:

| count | update at each click |
|---|---|
| `d` | `d ← ω_d • d + η_d • posterior` |
| `c` | `c ← ω_c • c + η_c • endingDelta ending` |
| habit | every policy's (α, β) decays by ω_h; the observed policy then gains η_h on α (success) or β (failure) |
| θ | every key's (α, β) decays by ω_θ once per click; then each witnessed outcome for `m.pattern` adds η_θ |

`d` rates are ℝ (d is ℝ); the others are ℚ, matching `CCounts`.

## The Nat obstacle, and the decision

`R15TemporalHierarchy.BetaCounts` has `Nat` fields, so a decayed count cannot
be stored there, and an unlisted policy in `SlowState` silently has the fresh
(1, 1) counts, which would never decay. Do this:

- Define `RatedCounts Key := Key → ℚ × ℚ` (alpha, beta) — a total function,
  so decay reaches every key, listed or not.
- `ofSlowState (s : SlowState Key) : RatedCounts Key :=
  fun k => ((countsFor s k).alpha, (countsFor s k).beta)` (cast to ℚ).
- `init` sets habit and θ to `ofSlowState m.habit₀` / `ofSlowState m.theta₀`.
  `Makeup` keeps `habit₀`, `theta₀` as `SlowState` (the prior is still the
  existing integer kit).
- Rated updates `advanceRated` and `advanceWitnessedRated` (ω once per click,
  then η per counted outcome, in the order `advanceWitnessed` counts them).

## Theorems (for every makeup and every observation list)

1. Closed forms at click n, geometric:
   - `c` at click n = `ω_c^n • c₀ + η_c • Σ_{k<n} ω_c^(n-1-k) • endingDelta o_k`.
   - `d` at click n = `ω_d^n • d₀ + η_d • Σ_{k<n} ω_d^(n-1-k) • posterior_k`
     (posterior history as in slice 1).
   - habit and θ: state the fold of the rated update (as slice 1 did); a
     geometric closed form for them is optional.
2. Special case. With all η = ω = 1:
   - the `c` and `d` closed forms reduce to slice 1's (`initialC m + endingCounts obs`,
     `d₀ + sumBeliefs …`);
   - `advanceRated` with η = ω = 1 commutes with `ofSlowState`:
     `advanceRated 1 1 (ofSlowState s) o = ofSlowState (R15TemporalHierarchy.advance s o)`,
     and likewise for `advanceWitnessedRated` / `PatternTheta.advanceWitnessed`.
     These are the theorems that keep the existing R15/PatternTheta results
     valid as the η = ω = 1 case. You will need `countsFor`/`setCounts`
     lemmas from R15TemporalHierarchy; if they are missing, prove them locally.
3. Keep slice 1's results that still hold with general rates: evidence
   conformance, run ids, previousFocus, C at click 0 is `kit`, and C is a
   probability distribution at every click (positivity now uses
   `0 < ω_c` and `0 < η_c`).
4. Update the concrete example: same two clicks, with a makeup that states
   η_c = 1, ω_c = 9/10 (and a docstring saying why — it is an example value,
   the spec claims none), showing `c .focused` = `[55, 99/2 + 1]` or whatever
   the update actually gives; compute it, don't guess.

Each rate in `exampleMakeup` gets a one-line docstring giving its value's reason.
Prove by unfolding `step`/`runFrom`, as in slice 1. No predicates on arbitrary
click sequences; no invented bad runs.

## Gates

- `lake build DarkTower.WarMachine.R20Step`; no `sorry`, no new `axiom`.
- `#print axioms` on the `c` closed form, the `d` closed form, and the
  η = ω = 1 `advanceRated` theorem: only `propext`, `Classical.choice`,
  `Quot.sound`. Paste the output.
- While you are in the file: add `omit [DecidableEq Policy] in` where the
  unusedSectionVars linter fires.
- Docstrings cite Smith, Friston & Whyte 2022 §4 Eqs. 34 and 37 for η, ω.
- Commit only `DarkTower/WarMachine/R20Step.lean` (stage the path explicitly;
  shared checkout, never `git commit -a`).

If the spec does not fit the existing types in some way not covered above,
stop and bell back with the question.
