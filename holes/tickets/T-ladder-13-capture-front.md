# Ladder 13: the capture front

**Status:** ABANDONED (WONTFIX) — draft never reviewed; the Lucy ladder experiment closed (Joe 2026-11-13)
Parent: M-futonzero-prelim-practice

Proposed by reviewer claude-4 (2026-11-13). Under `genotype-step` with even8, the 77|90|178 layout of ladder 06 is captured: the 90 and 178 blocks dissolve within about 10-20 steps and by t = 80 the rule field is all 77. This is the one place in the ladder where the rule layer is alive.

Coordinate trap (claude-4 review): mmca-clj's `propagate-at` reads writing positions in Wolfram order, and in that order even8 is `[1 0 4 7 2 3 5 6]`. The futon2 port `futon2.layout-premise` (ladder 06) passes even8 in the catalogue's bit order, which is a different operator: 77, 90 and 178 are fixed by both, but the dynamics diverge. Do not reuse that port unchanged; use mmca-clj semantics, and have tests check exact sets or digests, not counts.

## Scoped task
Characterise the 77 takeover under `genotype-step` with even8 in mmca-clj order: invasion speed by interface type (77|90, 90|178, and the boundary); whether any byte pair forms a stable interface; and whether widening the blocks delays the takeover or only slows it.

## What would close this
A committed deterministic script (per seed) and result file giving invasion speed per interface type, the list of stable byte-pair interfaces (an empty list is a result), and takeover time as a function of block width for at least three widths. Expected outcome: **closed / measurement recorded** (values not known in advance).
