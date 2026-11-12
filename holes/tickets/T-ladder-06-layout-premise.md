# Ladder 06: layout premise check

**Status:** OPEN — DRAFT FOR REVIEW
Parent: M-futonzero-prelim-practice
Gate: awaiting Joe and Fable review; do not select or execute.

Dependency: T-ladder-04-materials-catalogue (delivered in the sandbox futon2 checkout, not mmca-clj: namespace `futon2.materials-catalogue` in `futon2/src/futon2/materials_catalogue.clj` (`catalogue`, `fixed-set`, `materials`, `quiescent-edges`, `quiescent-entropy`, `has-bi-infinite-quiescent-word?`), tests in `futon2/test/futon2/materials_catalogue_test.clj`, commit 5e876ff73).

## Scoped task
Test a 77|90|178 rule field under even8 using both mmca-clj `genotype-step` and propagator-only steps.

## What would close this
Evidence shows `genotype-step` is not invariant and produces interface blends 95 and 250, while propagator-only steps preserve the field exactly. Then stop without choosing an engine and request Joe's ruling for ladders 7–8. Expected outcome: **stopped as expected for human decision**.
