# Do not fold apparatus failures into the outer target belief as task outcomes

**Status:** OPEN — FIX NOW (Joe, 2026-10-19)
Parent: M-wm-strategic-mission-selection

## Observed failure

The outer (strategic) belief is a Dirichlet over five outcome classes per
target (`src/futon2/aif/outer_target_belief.clj`, `outcome-vocabulary`),
folded only for the selected target. Run outcomes are classed by
`meta_outcome_dataset/outcome-class` (at `0892cee525d9`):

    #{:abstained :no-selection :build-failed :incomplete :cancelled
      :grounded-no-change :artifact-only}  -> :abstention-or-failure

So a click that ends `:incomplete` or `:build-failed` because the machinery
broke is folded as a failure outcome of the mission it was working on. On
2026-10-18, clicks 2-5 closed `:incomplete` at the cascade resume protocol
(`TN-war-machine-readiness-oct-18-2026.md`) while working on
`M-digital-nomad-patterns`, `M-formal-war-machine` and
`M-self-improvement-loop`. If those were folded, the strategic belief has
learned that those missions fail, and the next outer selection is biased away
from them for a reason that says nothing about the missions.

In active-inference terms machine failure and task outcome are different
hidden causes; the outer observation model treats them as one. R15 of the
PLoP catalogue asks that only witnessed tactical outcomes update the
strategic state.

Related evidence (click-5 checkpoint
`data/wm-on-prod-history/checkpoints/2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc/attempt-001.edn`):
255 target predictions are the uniform prior (1/5 each), 69 reflect one
observation and 3 reflect two; most targets are unobserved, which is the
tie mechanism in `T-outer-target-novelty-term.md`.

## Scoped task

1. Classify each run's terminal outcome by cause before it reaches the outer
   belief: task outcome (closure, progress, blocker, a genuine abstention or
   task failure) versus apparatus failure (resume-protocol violation, tooling
   build failure, transport or dispatch failure, cancellation). Use the typed
   failure kinds the runner already records; where a kind is ambiguous, do not
   fold it.
2. Fold only task outcomes into `outer_target_belief`. Record apparatus
   failures as interoceptive evidence (R20) instead, with the run id.
3. Determine whether the 10-18 readiness clicks were folded, and if so remove
   them from the outer belief's dataset by rule (not by hand-editing data),
   recording the before/after alpha for the affected targets.

## Acceptance evidence

- A fixture run that ends `:incomplete` with a resume-protocol failure kind
  does not change the target's alpha; it appears on the interoceptive record.
- A fixture run that ends with a genuine task abstention or task failure
  still updates alpha as before.
- The before/after alpha for the three 10-18 targets, with the dataset
  digests.

## Provenance

Found by claude-4 while annotating R15 (Hierarchical and Temporal Depth) for
the plop2026 docbook (`p4ng/plop-2026-code-annotations.json`), 2026-10-19.
Joe asked for a fix-now ticket and a bell.
