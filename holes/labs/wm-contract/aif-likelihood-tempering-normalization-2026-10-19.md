# AIF likelihood tempering normalization audit — 2026-10-19

Base: `3d093899ac7ecf6827791b90adfbbb144c8d0e7a`

No live click or full suite was run. The shared main worktree was not edited.

## Determination

The reported concern is correct for the formal categorical `:aif` route.
`belief/update-step` previously used `A(o,s)^kappa` directly and normalized
only the resulting posterior over states. Although `observation-model-v1` is
column-normalized, power tempering destroys that normalization whenever
`kappa != 1`. The missing denominator varies by state for asymmetric columns,
so it does not generally cancel in Bayes.

The repaired law is:

`A_kappa(o|s) = A(o|s)^kappa / sum_o' A(o'|s)^kappa`

followed by `q(s) proportional to A_kappa(observed|s) q_predicted(s)`.

Affected production paths are:

- live updates: `war-machine/apply-arena-belief-events` ->
  `belief/update-belief-batch` -> `update-entity-belief :aif` ->
  `categorical-filter-step` -> `update-step`;
- policy rollouts: `forward-model/predict` (including `efe/compute-efe`) through
  the same `:belief-update-opts {:likelihood-mode :aif}` filter.

The default production resolver `war-machine/arena-likelihood-mode` selects
`:aif`. The explicit environment hatches `legacy` and `a-matrix` do not use the
formal categorical operator. They are likelihood-ratio models, for which a
row-wise power is intentional and scale-free, and remain unchanged.

## Repair and provenance

`belief/tempered-observation-model` is the single public, total operator. It
validates the normalized observation model and finite nonnegative precision,
normalizes every fixed-state outcome column, and returns typed refusals for
invalid or impossible inputs. `kappa=1` returns the original model exactly;
`kappa=0` produces uniform outcome columns and leaves the predicted posterior
unchanged exactly.

Run provenance now carries `:wm/likelihood-tempering-semantics-v2` plus its
content hash inside `belief/model-manifest`. The live arena already persists
that manifest in resolved mode flags and configuration digests, making new
runs distinguishable without rewriting historical records. The separate R7
likelihood-precision declaration now states the difference between categorical
normalization and legacy likelihood-ratio tempering.

## Focused evidence

The production-shaped control uses the actual 7x7 model. It verifies both
dimensions, every tempered column sum, exact `kappa=1`, uniform `kappa=0`, an
asymmetric `kappa=2` posterior that differs from the old missing-denominator
calculation, normalized posterior output, invalid weights, zero/nonfinite
columns, an impossible observation, and explicit legacy/`:a-matrix`
compatibility.

- Combined belief/refusal/production wiring/precision gate: 107 tests, 2364 assertions,
  zero failures and zero errors.
- Changed-file clj-kondo, check-parens, and `git diff --check` are required
  before commit.
- The focused post-commit registry warrant is recorded separately.

No remaining mathematical gap is known in this event-block route. The separate
R7 token-rate likelihood precision machinery is a different observation model
and was audited but not modified beyond its provenance wording.
