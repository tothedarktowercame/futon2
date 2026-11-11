# Ladder 11: can phenotype content write a chosen rule byte?

**Status:** OPEN — DRAFT FOR REVIEW
Parent: M-futonzero-prelim-practice
Gate: awaiting Joe and Fable review; do not select or execute.

Dependency: T-ladder-04-materials-catalogue.

## Background
A constructor must turn blank cells into machinery. In MMCA terms: phenotype signals must change a neighbouring cell's rule byte to a chosen one. The only step in `mmca-clj/src/mmca/core.clj` where the phenotype enters the genotype update is the river step (`original-river-combine-rule` with its four-bit phenotype context, then `propagate-at` with `river-writing`). The propagator's source position is random, one of 8.

## Scoped task
For centre byte 178 and every predecessor byte, successor byte, phenotype context (16) and source position (8), compute the resulting byte under the river step. Report: (a) the (predecessor, successor, context) triples whose result is 77 for all 8 source positions, and likewise for 90; (b) for each target, the highest probability over source positions that any triple achieves; (c) the triples where changing only the context changes the set of reachable bytes. Repeat (a)–(c) for the blending step (`blend-rule` then `propagate-at`) under even8, which has no context.

## What would close this
A deterministic test reproduces the three tables from a committed script, with the counts stated in the result file. An empty table (a) is a result and closes the ticket. Do not propose or build a new write mechanism here. Expected outcome: **closed / measurement recorded** (values not known in advance).
