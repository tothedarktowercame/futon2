# Row 18 retained-field applicability and correspondence

Scope: the first form of `data/wm-trace/wm-trace-2026-09-12.edn`, fixed by the
original 147-candidate lead audit. No other field was searched.

## Authority resolution

The audit input retained only actions, ranks and `:controller-score`; its own
result explicitly says `:f-pi :absent` and `:log-habit-priors nil`. The pinned
source bytes remain available unchanged (SHA-256
`3b25d2d43e3ebbcf19fc6f82e103ae49907b81d0cf009fa8d7884e74ef53e175`),
so the first trace form supplies the larger authority envelope:

- 147 ranked candidates with `:controller-score`, sourced
  `:habit-prior-bias`, and stable action identity;
- a previous-prediction `:f-pi-by-candidate-id` envelope with 145 present and
  two typed-absent identities;
- `:policy-precision-state :solve` recording prior 1.0, posterior
  0.9962078734371584, residual -2.5931806441753524e-10, and 145 aligned
  candidates.

The exact production join is `policy_precision.clj:447-493`: F_pi entries are
ordered by their retained rank keys and joined by `habit-prior/policy-key`,
never current rank. `carry-beta` at lines 497-560 uses current
`:controller-score` as G. `beta_habit.clj:41-68` constructs the `:both` habit
vector in precisely that aligned order and refuses missing source.

## Applicability verdict

The retained solve used `:log-prior-placement :none`, not the ruled `:both`
field of the theorem. Therefore it is a real production-shaped numerical
solve, but **not an instance of the theorem's exact posterior**. Its habit
inputs exist and can form the `:both` field; that counterfactual was not solved
or retained in this record. No posterior value is inferred for it.

On the unchanged 145-candidate aligned G encodings, the exact decimal-rational
range is reported by the retained checker. Both c=1 and the proposed m=1/2
rate c=2 fail `c > 3R/2` by a wide margin. The typed conclusion is
`:uniqueness-unknown`, not multiple roots. Neither G nor the prior was changed.

## Source correspondence and gaps

`converge-beta` implements the same scalar residual and supports `:both`, but
its `Math/exp`, bisection, and tolerance are floating computations. A finite
residual below tolerance is not exact equality. The retained bracket is a
floating sign/bracket observation only. A sound numerical correspondence
needs outward-rounded real interval evaluations for exp, delta and residual;
with a proved lower slope bound it could turn residual width into a root
interval. This field fails the sufficient slope/uniqueness criterion, so the
current packet cannot supply that conversion.

The gradient branch additionally clips proposals to floor/ceiling and records
hit counters; clipped or unconverged results do not instantiate a root. The
bisection branch returns `:bracketed? false` on same-sign endpoints.
`carry-beta` carries converged solves unless `:bracketed?` is explicitly false;
otherwise it retains the prior as `:held-unsolved`, while missing fields are
`:held-absent`. `policy/effective-temperature` uses beta only in
`:variational-beta-gamma` and refuses absent/nonpositive beta; engineering
gain floors and spread temperatures are different laws. None of held,
saturated, inapplicable, or non-`:both` states qualifies for the theorem.

`row18_field_applicability.clj` is a nonintegrated checker for this fixed
artifact. It hashes and strictly decodes the same byte buffers it parses,
pins the audit input independently, calls the production alignment function, checks
habit/prior authority and support order, evaluates the sufficient inequality
as BigDecimal rational arithmetic, and commissions missing-F_pi,
missing-prior, missing-habit, and mutation-after-read refusals. Its runner
asserts the exact refusal map; an induced wrong expectation exits nonzero. It
writes no production state (the mutation control uses and deletes a temporary
file).

The actual `carry-beta` qualification is `(converged? && not (false?
bracketed?))`, not strictly `(converged? && bracketed? true)`. Thus a bisect
result with `false` holds, a bisect result with `true` may carry if converged,
and a gradient result whose bracket key is absent may carry if converged.
Unconverged results hold regardless. This is production's lifecycle rule; it
is weaker than theorem qualification. A carried gradient result remains a
floating approximation, and clipping/floor/ceiling hits remain recorded
nonqualifying causes rather than exact roots.
