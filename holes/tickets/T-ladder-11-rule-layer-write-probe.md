# Ladder 11: can phenotype content write a chosen rule byte?

**Status:** ABANDONED (WONTFIX) — draft never reviewed; the Lucy ladder experiment closed (Joe 2026-11-13)
Parent: M-futonzero-prelim-practice

Dependency: T-ladder-04-materials-catalogue (delivered in the sandbox futon2 checkout, not mmca-clj: namespace `futon2.materials-catalogue` in `futon2/src/futon2/materials_catalogue.clj` (`catalogue`, `fixed-set`, `materials`, `quiescent-edges`, `quiescent-entropy`, `has-bi-infinite-quiescent-word?`), tests in `futon2/test/futon2/materials_catalogue_test.clj`, commit 5e876ff73).

Coordinate trap (claude-4 review): mmca-clj's `propagate-at` reads writing positions in Wolfram order, and in that order even8 is `[1 0 4 7 2 3 5 6]`. The futon2 port `futon2.layout-premise` (ladder 06) passes even8 in the catalogue's bit order, which is a different operator: 77, 90 and 178 are fixed by both, but the dynamics diverge. Do not reuse that port unchanged; use mmca-clj semantics, and have tests check exact sets or digests, not counts.

Catalogue error (claude-4 review): in ladder 04's `futon2.materials-catalogue`, `:w4a` has fixed set {101 114 141 154} where the collapser's is {77 78 177 178}; its test checks only set sizes. Do not rely on `:w4a` from that namespace.

## Background
A constructor must turn blank cells into machinery. In MMCA terms: phenotype signals must change a neighbouring cell's rule byte to a chosen one. The only step in `mmca-clj/src/mmca/core.clj` where the phenotype enters the genotype update is the river step (`original-river-combine-rule` with its four-bit phenotype context, then `propagate-at` with `river-writing`). The propagator's source position is random, one of 8.

## Scoped task
For centre byte 178 and every predecessor byte, successor byte, phenotype context (16) and source position (8), compute the resulting byte under the river step. Report: (a) the (predecessor, successor, context) triples whose result is 77 for all 8 source positions, and likewise for 90; (b) for each target, the highest probability over source positions that any triple achieves; (c) the triples where changing only the context changes the set of reachable bytes. Repeat (a)–(c) for the blending step (`blend-rule` then `propagate-at`) under even8, which has no context.

## What would close this
A deterministic test reproduces the three tables from a committed script, with the counts stated in the result file. An empty table (a) is a result and closes the ticket. Do not propose or build a new write mechanism here. Expected outcome: **closed / measurement recorded** (values not known in advance).
