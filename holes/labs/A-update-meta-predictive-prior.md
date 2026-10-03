# A-update-meta-predictive-prior — acquire registered evidence for META

**Kind:** Algorithm candidate. **Status:** specified 2026-10-03; not approved
and not live-enabled. **Scope:** revise an existing source-grounded META
predictive prior, never create its initial authority.

## Purpose

Run a bounded, registered stratified acquisition series when the current META
model has an explicit coverage or calibration deficit. The Algorithm freezes
the complete M/E/T/A field before each selection, declares the sampling strata
and propensity, records candidate features and full timing/token/outcome
telemetry, and emits a versioned dataset suitable for a separately reviewed
prior update.

It is not ordinary useful-work selection. It may spend clicks primarily to
improve the model, so it requires its own approval and click allocation before
admission. Merely collecting rows cannot update the live prior.

## META slot proposal

```clojure
{:task-kind :algorithm
 :target "A-update-meta-predictive-prior"
 :next-move :run-algorithm
 :repairs-capability :meta-predictive-coverage-deficit
 :resource-envelope
 {:click-budget :bind-at-launch
  :time-budget-ms :bind-at-launch
  :token-budget :bind-at-launch}
 :evidence-channel
 {:source :complete-pinned-meta-field
  :locator :registered-stratified-run-manifest}
 :stopping-rule :budget-exhausted}
```

## Admission and stopping conditions

- [ ] A source-pinned audit names the missing stratum, channel, or calibration
  condition that the acquisition is intended to repair.
- [ ] The operator approves the exact click budget and declared sampling
  propensities; no ordinary click ration is inferred from this file.
- [ ] Every run retains canonical outer selection, complete field and feature
  pins, chosen propensity, model/token/time telemetry, and terminal outcome.
- [ ] Stop at the declared click budget, when the named deficit is satisfied,
  or on the first provenance/telemetry failure.
- [ ] A separate reviewed update procedure compares the prior and proposed
  successor; acquisition itself cannot alter production weights or likelihoods.

## Failure conditions

Missing propensity, incomplete field coverage, absent telemetry, or an
unregistered run refuses the row. Zero usable rows is a typed acquisition
result, not permission to fabricate a prediction. The Algorithm remains absent
from META support until separately approved in the Algorithm catalog.
