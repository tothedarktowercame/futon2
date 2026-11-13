# Ladder 06: layout premise check

**Status:** DONE
Parent: M-futonzero-prelim-practice

Dependency: T-ladder-04-materials-catalogue (delivered in the sandbox futon2 checkout, not mmca-clj: namespace `futon2.materials-catalogue` in `futon2/src/futon2/materials_catalogue.clj` (`catalogue`, `fixed-set`, `materials`, `quiescent-edges`, `quiescent-entropy`, `has-bi-infinite-quiescent-word?`), tests in `futon2/test/futon2/materials_catalogue_test.clj`, commit 5e876ff73).

## Scoped task
Test a 77|90|178 rule field under even8 using both mmca-clj `genotype-step` and propagator-only steps.

## Acceptance criteria

- [x] Evidence shows `genotype-step` is not invariant and produces interface blends 95 and 250, while propagator-only steps preserve the field exactly. Then stop without choosing an engine and request Joe's ruling for ladders 7–8. Expected outcome: **stopped as expected for human decision**.

## Delivered

Namespace `futon2.layout-premise` (`futon2/src/futon2/layout_premise.clj`) ports the mmca-clj Wolfram-order step mechanics (`blend-rule`, `propagate-at`, `genotype-step`, propagator-only step; mmca-clj is outside this delivery's repository plan, same precedent as T-ladder-04). Source positions are per-cell data instead of the RNG tape, so the evidence is exhaustive over sources: for a byte in Fix(even8) every source position leaves it unchanged. Tests in `futon2/test/futon2/layout_premise_test.clj` (5 tests, 18 assertions, green):

- Propagator-only steps preserve the 77|90|178 block field exactly, at every source position and every deterministic schedule (so under any RNG tape).
- The `genotype-step` blend stage produces interface blends 95 (77→90 interface: predecessor 77, centre 90, successor 90) and 250 (90→178 interface: predecessor 90, centre 90, successor 178); both lie outside Fix(even8) = {77,90,101,114,141,154,165,178}, so the field is not invariant.
- `futon2.layout-premise/probe-report` returns the ticket's evidence record mechanically.

No engine was chosen: the ladder 7–8 engine decision is Joe's; this delivery stops at the evidence as instructed.
