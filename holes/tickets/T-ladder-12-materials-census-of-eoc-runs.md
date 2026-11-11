# Ladder 12: materials census of the edge-of-chaos exemplars

**Status:** OPEN — DRAFT FOR REVIEW
Parent: M-futonzero-prelim-practice
Gate: awaiting Joe and Fable review; do not select or execute.

Dependency: T-ladder-04-materials-catalogue.

## Background
TN-fable-construction-reframe reads coexistence of settled and active regions as a property of how the rule field is laid out. The paper's edge-of-chaos exemplars were found interactively and have not been examined in those terms.

## Scoped task
Regenerate two runs in the sandbox mmca-clj checkout: the offset+1 exemplar, `(run-propagator [1 2 3 4 5 6 7 0] 1 256 600)`, and the river plate, `(run-river 1 240 360)`. For each, over the last 100 genotype rows, compute: (a) the fraction of rule bytes lying in each fixed set of ladder 04's catalogue, in the fixed set of the run's own writing, and in neither; (b) the 10 most frequent bytes with their fractions; (c) with a phenotype cell called settled when it is unchanged over the next 10 steps, the byte distribution on settled cells and on active cells, and the fraction of settled cells whose byte's quiescent set contains their current neighbourhood.

## What would close this
A committed script and result file, a determinism test (two runs give identical output), and a baseline: the same three measurements on the initial row, where bytes are uniform at random. No interpretation beyond stating, for each run, whether catalogue bytes are over-represented relative to the baseline and whether settled and active cells differ in byte distribution. Expected outcome: **closed / measurement recorded** (values not known in advance).
