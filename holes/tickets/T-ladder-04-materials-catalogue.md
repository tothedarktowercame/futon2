# Ladder 04: constructor M0 materials catalogue

**Status:** OPEN — AUTHORIZED STAGE 4
Parent: M-futonzero-prelim-practice

## Scoped task
Implement the fixed-byte and quiescent-subshift catalogue described by TN-fable-construction-reframe in the sandbox mmca-clj checkout.

## Acceptance criteria

- [ ] Tests assert fixed-set sizes 4/16/8/2/4; union size 16; even8 exactly {77,90,101,114,141,154,165,178}; entropy rounded to three decimals is 77=0.694, 78=141=0.406, 90=178=0; and 113 has no bi-infinite quiescent word. Expected outcome: **closed / pass**.
