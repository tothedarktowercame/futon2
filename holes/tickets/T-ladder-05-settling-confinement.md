# Ladder 05: M0 settling and confinement, tested exhaustively

**Status:** OPEN — DRAFT FOR REVIEW
Parent: M-futonzero-prelim-practice
Gate: awaiting Joe and Fable review; do not select or execute.

Dependency: T-ladder-04-materials-catalogue (delivered in the sandbox futon2 checkout, not mmca-clj: namespace `futon2.materials-catalogue` in `futon2/src/futon2/materials_catalogue.clj` (`catalogue`, `fixed-set`, `materials`, `quiescent-edges`, `quiescent-entropy`, `has-bi-infinite-quiescent-word?`), tests in `futon2/test/futon2/materials_catalogue_test.clj`, commit 5e876ff73).

## Scoped task
TN-fable-construction-reframe states two things about byte 77 on rings, from Monte Carlo on random 40-cell rings: it settles from any state in at most 6 steps, and a single flip in a settled ring spreads to depth at most 1. Test both exhaustively: every binary ring for n = 3..20 for settling; every fixed-point ring for n = 4..20, flipped at one site, for damage depth.

## What would close this
A deterministic exhaustive test records, per n: the maximum transient, the number of rings that never reach a fixed point, a worst-case starting ring, and the maximum damage depth. Each of the TN's two claims is then recorded as upheld or refuted, with the smallest counterexample if refuted. Do not narrow the range of n, exclude rings, or redefine "settles" to obtain a pass; a refuted claim is the result. Expected outcome: **closed / settling claim refuted, confinement claim upheld**. Reference values (claude-4, 2026-11-11, independent enumeration): maximum transient is floor(n/2) - 1, so it first exceeds 6 at n = 16 and is 9 at n = 20 (worst start: a single 1); exactly two rings per n, the uniform ones, never settle; damage depth is 1 for every n = 4..20.
