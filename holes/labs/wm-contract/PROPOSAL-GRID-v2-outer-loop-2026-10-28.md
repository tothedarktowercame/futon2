# Proposal: GRID v2 amendment for the R20 outer loop (2026-10-28)

**This proposes changing the specification** (GRID-wm-criteria-completeness-v1.md
and criteria-completeness-deficits.tsv). From claude-11 for codex-11 (owner of
E-wm-criteria-completeness), at Joe's direction 2026-10-28 ("resume the
criteria completeness work ... we may need to adapt some of the criteria").
v1 is retained unchanged; this is the named, versioned amendment the
excursion requires.

## Why

The outer selector GRID v1 was written against was deleted (futon2
`a04b157d2`, DELETE-outer-selection-2026-10-28.md). Its replacement is the
R20 outer loop: Lean `DarkTower/WarMachine/R20Step.lean` slices 3a–3f
(head `2ad839ecc`), Clojure `futon2.aif.outer-score`, `outer-state`,
`outer-observation`, `outer-select`, wired into the missing-task branch of
`full_loop_runtime/run-ordinary-opportunity!` (`768c27e94`). Design:
NOTE-outer-G-computation-2026-10-28.md. Task selection is scored with the
inner loop's G (risk + ambiguity − novelty) over an outer model of tasks as
wholes; no inner policy is built before selection.

## Criteria restated in v2 (the other fourteen stand as in v1)

| ID | v2 reading | Evidence location |
|---|---|---|
| B1 | The field is the verified Giraffe run used (missions source): open items with status, unblocks, cone; exclusions are Giraffe's not-ready / undetermined per item. | `:outer/selection` `:giraffe/run-id`, `:giraffe/manifest-sha256`; the run's `pass.edn` |
| B2 | Readings are produced only for the chosen task, after selection. A missing reading for an unchosen task is not a deficit (that would be the regress). | inner-path reading records for the chosen task |
| B3 | Each menu cascade's outer G is recorded with the chosen index, `:tie?` (distinct tied picks) and `:tied-cascades`; replayable from makeup sha, outer state and Giraffe run. | `:outer/selection` `:cascade-scores`, `:cascade-id`, `:makeup/sha256` |
| B4 | The outer prediction is frozen at selection: the pick's predicted distribution over (ending class, gain), prior over doable/blocked, status, landing. | `:outer/selection` `:prediction` (packet outer-clj-4c) |
| E2 | The next selection consumes click n's outcome through `next-state`; deferral is recorded when the outcome is not yet observable. | `outer-state.edn` `:pending`, the next `:outer/selection` `:outer/learning` (packet outer-clj-5b) |
| E3 | Add the outer case: re-picking the same task after a refusal on unchanged input is a violation (Requirements Q6 restated; not implemented in the selector, so a deficit). | across-run `:outer/selection` targets and terminal outcomes |

Stated limits (not passes): task reach is a makeup default (1/2);
learning yield is a preference with no producer (always `:none`); items made
ready are observed only when the chosen task has closed in a later Giraffe
run (Joe, 2026-10-28).

## Deficits TSV changes

- **B1-TICKETS**: re-express. The missions source has no tickets by
  construction; tickets are a separate Giraffe source (M-giraffe stage 1).
  The open question becomes whether the field should combine both sources.
- **New B4-OUTER-PREDICTION**: closed when outer-clj-4c lands and a live
  click's run record carries `:outer/selection :prediction`.
- **New E2-OUTER-LEARNING**: closed when outer-clj-5b lands and a second
  live click consumes the first's pending outcome (or records a typed
  deferral).
- **New E3-OUTER-REPEAT**: open; the selector does not yet avoid re-picking
  after a refusal on unchanged input.
- E3-T12 and B2/D4-R28 unchanged.
