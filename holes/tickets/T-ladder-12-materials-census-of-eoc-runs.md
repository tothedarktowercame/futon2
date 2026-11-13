# Ladder 12: materials census of the edge-of-chaos exemplars

**Status:** ABANDONED (WONTFIX) — draft never reviewed; the Lucy ladder experiment closed (Joe 2026-11-13)
Parent: M-futonzero-prelim-practice

Dependency: T-ladder-04-materials-catalogue (delivered in the sandbox futon2 checkout, not mmca-clj: namespace `futon2.materials-catalogue` in `futon2/src/futon2/materials_catalogue.clj` (`catalogue`, `fixed-set`, `materials`, `quiescent-edges`, `quiescent-entropy`, `has-bi-infinite-quiescent-word?`), tests in `futon2/test/futon2/materials_catalogue_test.clj`, commit 5e876ff73).

Coordinate trap (claude-4 review): mmca-clj's `propagate-at` reads writing positions in Wolfram order, and in that order even8 is `[1 0 4 7 2 3 5 6]`. The futon2 port `futon2.layout-premise` (ladder 06) passes even8 in the catalogue's bit order, which is a different operator: 77, 90 and 178 are fixed by both, but the dynamics diverge. Do not reuse that port unchanged; use mmca-clj semantics, and have tests check exact sets or digests, not counts.

Catalogue error (claude-4 review): in ladder 04's `futon2.materials-catalogue`, `:w4a` has fixed set {101 114 141 154} where the collapser's is {77 78 177 178}; its test checks only set sizes. Do not rely on `:w4a` from that namespace.

## Background
TN-fable-construction-reframe reads coexistence of settled and active regions as a property of how the rule field is laid out. The paper's edge-of-chaos exemplars were found interactively and have not been examined in those terms.

## Scoped task
Regenerate two runs in the sandbox mmca-clj checkout: the offset+1 exemplar, `(run-propagator [1 2 3 4 5 6 7 0] 1 256 600)`, and the river plate, `(run-river 1 240 360)`. For each, over the last 100 genotype rows, compute: (a) the fraction of rule bytes lying in each fixed set of ladder 04's catalogue, in the fixed set of the run's own writing, and in neither; (b) the 10 most frequent bytes with their fractions; (c) with a phenotype cell called settled when it is unchanged over the next 10 steps, the byte distribution on settled cells and on active cells, and the fraction of settled cells whose byte's quiescent set contains their current neighbourhood.

## What would close this
A committed script and result file, a determinism test (two runs give identical output), and a baseline: the same three measurements on the initial row, where bytes are uniform at random. No interpretation beyond stating, for each run, whether catalogue bytes are over-represented relative to the baseline and whether settled and active cells differ in byte distribution. Expected outcome: **closed / measurement recorded** (values not known in advance).
