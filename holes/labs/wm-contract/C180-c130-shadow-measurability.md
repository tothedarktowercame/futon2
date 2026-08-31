# C180 — measurability of the seven C130 absence decisions

Date: 2026-08-31. This is an assessment, not an implementation. No option is
selected and no live scoring, ranking, belief update, sampling, selection, or
actuation changes here.

## Result

All seven questions describe computable branches, but the current trace is not
a replay log for all seven. One is measurable now, three are partly measurable
from current v20 records but need either a more precise option or a pre-coercion
input, and three require new persistence boundaries. None of the seven is
empirically decidable in the normative sense: a shadow can report incidence and
consequence, but cannot decide whether stalling, acting under uncertainty, or
changing a ranking population is acceptable.

The current C108 result is adjacent evidence, not evidence for these options:
two post-v18 records contain 288 shadowed candidates with absent channels, zero
rank changes, and zero incomparable pairs. `trace.clj:249-288` recomputes the
support-typed controller score; it does not execute any C130 A/B branch.

## Per-option assessment

| C130 decision | What can be measured now | Missing evidence / limit | Disposition |
|---|---|---|---|
| Prediction triple | For post-v17 ticks, `:observation-envelope` identifies an absent observation; `:belief-pre`, `:prediction-errors`, `:precision-state`, and `:micro-step-trace` expose the update that actually ran (`war_machine.clj:4360-4444`, `4763-4772`). A shadow can count observation-absence incidence and compute the immediate **omit-channel versus refuse-update** delta. | The producer coerces missing prediction `:mean`, `:variance`, and observation before it creates the record (`free_energy.clj:169-181`; caller at `war_machine.clj:4371-4377`). The resulting `:prediction-error/v1` record proves its output shape, not the presence of all three inputs. Full measurement needs one typed, pre-coercion prediction-triple envelope. Later-rank effects additionally require sequential replay; a one-tick belief delta is not that claim. | **Partly measurable now; one new WM trace field for full coverage.** |
| Strategic mode | The observation envelope can identify incomplete six-feature inputs, while the record retains actual `:mode`, observation, ranked and admissible actions (`free_energy.clj:208-235`; `war_machine.clj:4744-4779`). Option A's incidence and immediate removal of mode-conditioned selection can therefore be counted. | Option B is not one algorithm: “partial/prior rule” must first name the prior and feature policy. The trace has the resulting mode but no separately identified prior-mode input. Ranking impact needs that rule plus a replayable selector input. | **Partly measurable now; option B specification and a prior-mode/input stamp required.** |
| Missing sorry pressure | The envelope distinguishes missing `:sorry-count-norm`, and the trace retains the ranked action population consumed by the fallback selector. Both “abstain/return control” and “continue through non-sorry branches” can be evaluated over a post-v17 record without inventing an observation (`policy.clj:141-163`; `war_machine.clj:4777`). | The corpus currently has only two current-schema diagnostic records through a stub selector, so the result would be a valid bounded measurement, not representative production evidence. The upstream return target for A and the allowed branch set for B must be named before comparing chosen actions. | **Measurable with existing fields once the two branches are made executable; no new trace carrier.** |
| Rollout step producer | Nothing reliable. The trace retains selected/ranked cascade outputs, including final rollout scores, but not the complete move population before `renormalize-priors` and `move-cost`. | `rollout.clj:122-139` forms priors from partial moves and `rollout.clj:149-158` collapses missing delta/score to zero before the surviving rollout is recorded. Measurement needs a per-rollout envelope containing every proposed move, score status/reason, state, and configuration before filtering or fallback. | **Not measurable from current traces; needs a rollout-input envelope.** |
| Unscored rollout move | Nothing reliable today for the same reason: the unscored alternative has already disappeared into numeric cost. | Once the preceding producer envelope exists, exclude-move and refuse-rollout are two computations over the same captured population. This option does not need a second carrier, but it does need its own comparison and falsifier. | **Not measurable now; shares the rollout delivery above.** |
| Fulab temperature | The live result exposes `tau`, logits, probabilities, seed, and chosen candidate in the adapter's tap event (`adapters/fulab.clj:135-174`). That is not a durable War Machine trace input envelope. | `compute-tau` converts missing error to zero before combining it with uncertainty (`adapters/fulab.clj:79-84`). A shadow needs durable error presence/reason, uncertainty inputs, candidates/configuration, and the stable sampling basis. It can compare probability distributions deterministically; comparing sampled winners also requires the recorded seed/basis. | **Not measurable from the current WM corpus; needs an adapter-specific decision envelope.** |
| Belief aggregation | Current records contain the weighted-error collection, producer-contract classifications, pre/post belief, precision state, and micro-step trace. C120 makes legacy and malformed error records distinguishable. A shadow can replay **omit honestly absent/reject malformed** versus **refuse incomplete collection** and report the immediate aggregate and belief-event delta (`belief.clj:1017-1052`; `war_machine.clj:4378-4444`). | The aggregation still defaults missing `:weighted-error` and `:precision` locally, so the shadow must consume C120's status fields rather than the coerced scalars. Claims about later rankings require sequential replay or forward ticks; they cannot be inferred from the immediate delta. | **Measurable now for post-C120 records at the immediate-update boundary; future-ranking effects need sequential evidence.** |

“Not measurable now” does not mean “not measurable in principle.” All seven can
be instrumented once their branches and pre-coercion inputs are named. What
measurement cannot establish is which consequence the operator should permit.
That boundary is clearest for C113's hard-guard authority and C78's outward-act
binding: incidence and simulated outcomes may inform them, but no corpus can
grant safety or outward-effect authority. Those two decisions are outside the
seven and remain authority decisions, not missing shadows.

## Cost and useful grouping

This is neither one delivery nor seven independent carrier migrations:

1. **War Machine replay family (four option evaluators):** prediction triple,
   strategic mode, sorry pressure, and belief aggregation can share the v20
   reader and comparison format. It still needs a typed prediction-input
   envelope and a prior-mode/input stamp for complete coverage. Each option
   needs its own negative control because their refusals have different effects.
2. **Rollout family (two option evaluators):** one new pre-fallback rollout
   envelope supports both producer validation and exclude-versus-refuse.
3. **Fulab family (one evaluator):** one adapter-specific persisted decision
   envelope; the WM tick trace is the wrong carrier.

Thus the minimum honest construction is **three carrier/harness deliveries and
seven branch comparisons**, not one generic shadow and not seven unrelated
schemas. With no new persistence work, only missing-sorry-pressure and the
immediate part of belief aggregation can be measured end to end; prediction
and mode yield partial incidence/delta results. Historical pre-v17 effects
remain unreconstructable because presence was not retained.

## Current controls

No code or trace schema changed. The governing counts therefore remain:

```text
absence coercion: 7 live decision sites
C108 current evidence: 2 records, 288 absent-support candidates,
                       0 rank changes, 0 incomparable pairs
```

Canonical checks:

```sh
bb -cp . checks/preemptive_absence_coercion_lint.clj
bb -cp . checks/absence_scoring_counterfactual.clj
make workspace-gate
```

Sources: `C130-absence-decisions.md`, `C108-support-typed-scoring-shadow.md`,
`C120-precision-producer-contract.md`, and `holes/problems/CRITICAL-PATH.md`.
