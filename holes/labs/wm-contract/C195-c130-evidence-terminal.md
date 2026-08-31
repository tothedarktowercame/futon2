# C195 — terminal evidence position for the seven C130 decisions

Date: 2026-08-31. Commissioner decision: do not build the sequential replay.
This record closes the measurement question, not any of the seven behavioural
decisions.

## What is measured

Two of seven options have **directional immediate-boundary evidence** over one
pinned corpus:

```text
2 raw records
2 unique, sequentially dependent state fingerprints
1 selector-seam stratum (stub:first-ranked-authorized-mission)
0 independent-trial claim
```

- **Missing sorry pressure:** conditional evaluation of the fallback over each
  retained 144-candidate population gives A = abstain and B = select
  `:learn-action-class/:survey-mission` in both records. These are two
  evaluations of a conditional, not two observed fallback invocations; the
  trace does not retain invocation provenance.
- **Belief aggregation:** omitting honestly absent `:sorry-count-norm` changes
  the final immediate driver from `-0.058013863604` to `-0.071218020273` and
  from `-0.062775435448` to `-0.075183402264`. Neither changes sign; refusal
  rejects both incomplete collections. Reconstruction of the baseline driver
  is exact.

These claims stop at the immediate selector/aggregation boundary. They do not
claim downstream rank or selected-action effects. C182 contains the executable
measurement; C186 establishes the sequential dependence and evidential
denominators.

## What is not measurable affordably

The decision-relevant downstream question is whether either branch propagates
into later ranking or selection. A faithful sequential replay needs all five
missing input families identified by C191:

1. pre-coercion inputs for every prediction micro-step;
2. persisted pre-ranking snapshots;
3. raw candidate and forward-model inputs;
4. fallback-invocation provenance;
5. pinned exogenous scan, registry, and evidence inputs.

They must form one coherent replay envelope. Adding one or two fields is not a
cheap partial answer: the replay would borrow the baseline branch's later
outputs for everything omitted, making the counterfactual circular. On top of
the persistence work it needs a two-branch sequential harness and independent
agreement controls.

Even that construction would establish core rank divergence only under a
fixed diagnostic input chain. The stub deterministically chooses the first of
three scheduler candidates, while production selection may consume memory,
relations, calibration, and other evidence. Stub divergence does not imply a
production winner change; stub agreement does not rule one out. Production
selected-action evidence additionally needs the production selector seam.

The earliest possible propagation is same-tick, but there is no proved finite
horizon after which a latent belief difference can be declared irrelevant.
Thus a finite replay can say “no divergence through H,” never “no downstream
effect exists.” The proposed build is large and its strongest result remains
narrower than the seven decisions' authority boundary. The commissioner has
therefore declined it. wm-evidence agrees: none of the five persistence items
alone changes this calculus, and partial persistence risks a falsely complete
instrument.

## Consequence for the seven decisions

They remain judgement calls:

| option | evidence available |
|---|---|
| prediction triple | none on A/B effect |
| belief aggregation | directional immediate driver effect; no downstream effect |
| strategic mode | none on A/B effect |
| missing sorry pressure | directional conditional fallback effect; invocation unobserved |
| rollout-step producer | none on A/B effect |
| unscored rollout move | none on A/B effect |
| fulab temperature | none on A/B effect |

This is a completed evidence result, not an analysis failure. Joe should decide
with bounded directional evidence on two and no option-effect measurement on
five. `DECISIONS-PENDING.md` now says so; it no longer implies that a pending
measurement will resolve them.

## The three unmade declarations

- **Estimand:** no primary downstream outcome was selected among first-rank
  divergence, winner identity, rank displacement, and belief-driver change.
- **Material-effect threshold:** no acceptable driver/rank displacement was
  declared. “Any winner change” is executable, but it does not cover numeric or
  latent effects.
- **Stopping rule:** no replay horizon or uncertainty rule was declared, and
  one observed transition cannot estimate propagation depth.

They remain unmade because the replay they would govern is not being built.
There is also a circularity in trying to derive them empirically: setting a
material threshold requires naming the downstream link that matters, while
measuring that link requires the declined replay envelope and harness. They
could still be imposed as policy, but the current corpus cannot derive them.
Leaving them explicit and unmade is more accurate than manufacturing acceptance
for an instrument that will not exist.

## Current controls

No tick, replay, persistence carrier, or behavioural change is part of C195.

```sh
bb -cp . checks/c130_immediate_option_measurement.clj
bb -cp . checks/preemptive_absence_coercion_lint.clj
make workspace-gate
```

Sources: `C130-absence-decisions.md`,
`C182-immediate-absence-option-measurement.md`,
`C186-diagnostic-evidence-growth.md`, and
`C191-sequential-replay-assessment.md`.
