# Ladder 08: M1 memory under fire preregistration

**Status:** ABANDONED (WONTFIX) — the Lucy ladder experiment closed with the preregistration presented and not run (see data/wm-d-task-enactment/T-ladder-08-memory-preregistration/preregistration-memory-under-fire.edn); Joe 2026-11-13
Parent: M-futonzero-prelim-practice

Dependencies: T-ladder-05-settling-confinement and Joe's engine ruling from T-ladder-06-layout-premise.

Joe's engine ruling (2026-11-13): **propagator-only** (rule layer held fixed), on the recommendation of reviewer claude-4, whose probe ran `genotype-step` with the correct even8 on the 77|90|178 layout: the 90 and 178 blocks dissolve within about 10-20 steps and by t = 80 the whole rule field is 77. With propagator-only steps the rule layer is exactly frozen, so this is an experiment on a fixed heterogeneous elementary CA, not on a meta-CA; say so in the result.

Coordinate trap (claude-4 review): mmca-clj's `propagate-at` reads writing positions in Wolfram order, and in that order even8 is `[1 0 4 7 2 3 5 6]`. The futon2 port `futon2.layout-premise` (ladder 06) passes even8 in the catalogue's bit order, which is a different operator: 77, 90 and 178 are fixed by both, but the dynamics diverge. Do not reuse that port unchanged; use mmca-clj semantics, and have tests check exact sets or digests, not counts.

## Scoped task
Write, but do not run, a deterministic preregistration EDN for memory-under-fire: guard width, horizon, penetration limit, and the approximately six-step erosion baseline. Include, as its control arm, the wire test that was ladder 07: a perturbation should arrive at distance d at time d on the 90 channel, with no corresponding arrival when the channel is changed to 77 or routed through a 178 back path, content checked by XOR. Note that ladder 05 refuted the TN's six-step settling claim (maximum transient floor(n/2) - 1), so state the baseline from that result, not from the TN.

## Acceptance criteria

- [x] The EDN parses, names the engine (propagator-only, with even8 in mmca-clj order) and seed, pins all thresholds and the control arm before observation, and is presented to Joe for sign-off. Stop before running the experiment. Expected outcome: **stopped as expected for human sign-off**.

## Delivered (2026-11-13, wm-author)

The preregistration is written and parsed, not run: `data/wm-d-task-enactment/T-ladder-08-memory-preregistration/preregistration-memory-under-fire.edn` (clojure.edn/read-string exits 0; engine `:propagator-only`, even8 `[1 0 4 7 2 3 5 6]` in mmca-clj Wolfram order, seed 20261113). All thresholds pinned before observation — guard width 4, horizon 80, penetration limit 1 — the erosion baseline stated from ladder 05's `floor(n/2) - 1` result (which refuted the TN's six-step claim), not from the TN, and the ladder 07 wire test carried as the control arm (90-channel speed-1 arrival, 77 and 178-back-path non-arrival, XOR content check). The registration's own falsifier names running before sign-off. Stopped here: **stopped as expected for human sign-off**; the experiment runs only after Joe signs.
