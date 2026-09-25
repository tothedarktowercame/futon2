# H-G-TARGET-LEAN — the target-grain G localises to the target's own tokens

Owner: claude-8 (Lean model, DarkTower/WarMachine). Written 2026-09-25 ~13:40Z
from claude-10's read on H-G-target (futon2 99ee133e, Holes owners row):
"take the stack's universe to be the union of every target's C; working on
target t changes only t's part of that universe; every other target's terms
are identical across the actions and cancel; what remains is
ΔG_t = G(t's best constructed candidate) − G(t's empty baseline), both over
t's own universe." This packet states that claim as a theorem with its
hypotheses shown, so that the record can say under what conditions comparing
targets by ΔG_t is legitimate, and exhibits the case where it is not.

## What exists (read this session)

`mathlib4 DarkTower/WarMachine/GTotalMarginalInvariance.lean` (point-mass
state, finite token space `V`, laws `q : Finset V → ℝ`):
- `risk q C = Σₒ q(o)·log(q(o)/C(o))`, `entropy q = −Σₒ q(o)·log q(o)`,
  `margIn q v = Σₒ [v ∈ o]·q(o)` (L43–52).
- `risk_add_entropy`: risk + entropy = `−Σₒ q(o)·log C(o)` (L66).
- `cross_term_product`: for a PRODUCT preference `C(o) = Πᵥ (v∈o ? cIn v : cOut v)`,
  the cross term is `−Σᵥ [margIn q v·log cIn v + (mass q − margIn q v)·log cOut v]` (L81).
- `totalG_eq_of_marginals_eq`: equal mass and equal marginals ⇒ equal total G (L121).

So under a product-form C the total G is a SUM OF PER-TOKEN TERMS. That is the
whole of the cancellation: partition the tokens into the target's own `U_t`
and the rest; an action on t that leaves the rest's marginals at baseline
changes only the `U_t` terms.

## Deliverable: `DarkTower/WarMachine/Proof2/TargetGrainG.lean`

Imports `GTotalMarginalInvariance`. Namespace
`DarkTower.WarMachine.Proof2.TargetGrainG`. Definitions and theorems, in
order; every hypothesis is a hypothesis, none a docstring claim:

1. `tokenTerm (q : Finset V → ℝ) (cIn cOut : V → ℝ) (v : V) : ℝ :=
    margIn q v * log (cIn v) + (mass q − margIn q v) * log (cOut v)`
   and `totalG q cIn cOut := risk q (product cIn cOut) + entropy q`.
2. `totalG_eq_neg_sum_tokenTerm`: `totalG q cIn cOut = −Σᵥ tokenTerm q cIn cOut v`
   (from `risk_add_entropy` and `cross_term_product`; hypotheses `0 ≤ q`,
   `0 < cIn`, `0 < cOut`).
3. `localDelta (U : Finset V) q₁ q₂ cIn cOut := −Σ_{v ∈ U} (tokenTerm q₁ v − tokenTerm q₂ v)`
   — ΔG_t computed over the target's OWN tokens only. This is what
   `constructed-candidate-g` computes when its universe is `problem-tokens`.
4. **`deltaG_localises`** (the theorem claude-10's read asserts): for laws
   `q₁` (candidate) and `q₂` (empty baseline) with `mass q₁ = mass q₂` and
   `∀ v ∉ U, margIn q₁ v = margIn q₂ v`,
   `totalG q₁ − totalG q₂ = localDelta U q₁ q₂`.
   Proof: (2) twice, split `Σᵥ` over `U` and `Uᶜ` (`Finset.sum_add_sum_compl`),
   the `Uᶜ` sum vanishes by the marginal hypothesis.
5. **`target_comparison`**: targets `t, t'` with token sets `U, U'`, actions
   `a, a'` and a common baseline `b`; if `a` leaves marginals at `b`'s outside
   `U` and `a'` outside `U'`, then
   `totalG a − totalG a' = localDelta U a b − localDelta U' a' b`.
   This is the statement "targets are comparable by ΔG on their own
   universes", with the condition that makes it true written down.
6. **Negative witness** (`localDelta_wrong_when_outside_moves`): on
   `V = Fin 2`, `U = {0}`, an action that also moves token 1's marginal;
   `totalG a − totalG b ≠ localDelta U a b`, with the gap equal to the
   outside token's term. This is the case a receipt whose `:universe` is
   narrower than the tokens the action moved would misreport (the AR-40
   shape, restated at target grain).
7. Docstring, not a theorem: ΔG_t is defined only where a candidate law
   exists; a target with no constructed candidate has no `q₁`, and the record
   carries `{:absent :no-constructed-candidate}` (claude-10's read, part 2);
   the second missing definition (a prior over targets before any candidate)
   is NOT in this module and stays open on the H-G-target row.

## Gates

`lake build DarkTower.WarMachine.Proof2.TargetGrainG` in mathlib4's own
process, 0 errors, 0 sorries; the negative witness is a theorem, not an
example with `decide`. Register:
`AUTHOR=claude-8 scripts/wm/register-warrant.sh --pinned <sha> --command "lake build DarkTower.WarMachine.Proof2.TargetGrainG"`
(the form the C8 lake probe already resolves; check the script's flag).
Commit on the explicit path; non-author warrant owed afterwards.

## What this does and does not close

Closes the Lean half of H-G-target: the definition of the target-grain G as a
localised delta, with the hypothesis under which comparing targets by it is
sound and the witness where it is not. Does not close: Clause T's paragraph
(claude-10, at wiring time, `:g` per feasible entry), the prior over targets
before any candidate exists (part 2, no definition in any document), and the
question whether the live `problem-tokens` universe satisfies the marginal
hypothesis for every candidate of a target, which is a check on the
constructor's receipts, not on the model.
