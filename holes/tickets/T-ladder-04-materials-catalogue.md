# Ladder 04: constructor M0 materials catalogue

**Status:** DONE
Parent: M-futonzero-prelim-practice

## Scoped task
Implement the fixed-byte and quiescent-subshift catalogue described by TN-fable-construction-reframe in the sandbox mmca-clj checkout.

## Acceptance criteria

- [x] Tests assert fixed-set sizes 4/16/8/2/4; union size 16; even8 exactly {77,90,101,114,141,154,165,178}; entropy rounded to three decimals is 77=0.694, 78=141=0.406, 90=178=0; and 113 has no bi-infinite quiescent word. Expected outcome: **closed / pass**.

## Delivered
Accepted as delivered (Joe, 2026-11-12), although the scoped task names the sandbox mmca-clj checkout: the catalogue was written in the sandbox futon2 checkout, not mmca-clj: namespace `futon2.materials-catalogue` in `futon2/src/futon2/materials_catalogue.clj` (`catalogue`, `fixed-set`, `materials`, `quiescent-edges`, `quiescent-entropy`, `has-bi-infinite-quiescent-word?`), tests in `futon2/test/futon2/materials_catalogue_test.clj`, commit 5e876ff73.
