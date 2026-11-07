# E-kimi-task-405 — proposition_23 lemma + replay 22''

**Requisition:** in-progress — dispatched 2026-11-07T15:22:58Z to kimi-4 as invoke-1794064978994-15204-fd4ff2c8

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I22pp (E-euclid-tutor), for kimi-4, from claude-9.

Your 105a511 + 37939d4 (I.23 replay) are accepted. My checks: degenerated_angle_if (Transfer.lean:63) and flat_angle_onlyif (:124) read verbatim; the four SMT-bridges (angle_self_zero, angle_ray_eq_zero, off_line_strict_triangle, use_witness) are sound as written (off_line_strict_triangle: for x on L one of the three is an equality). My bad cases: second proposition_20 dropped -> stuck at proposition_22''s gt_sum_len entail; flat_angle_onlyif d c e dropped -> not-entailed eq_ang; witness extended past b instead of past a -> not-entailed; the zero branch's hypothesis replaced by between d c e -> use_witness stuck; real final store not contradictory. One note for the record (no change needed): the inner split "∠d:c:e = 0 / between d c e" is exhaustive only through degenerated_angle_if (not between -> angle 0); the comment says this, as the by_cases contract requires.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines). Commit after each step, before belling.

Why: I.24 (Prop24.lean:18) cites proposition_23', which cites proposition_22''. This packet builds toward it.
1. CONSTRUCTIONS "proposition_23" (Prop23.lean:8-10 verbatim): premises dpl a b AB and formRectilinearAngle d c e CD CE (destructured as in I.23's hyps); makes f with neq f a and ∠f:a:b = ∠d:c:e. Tests: cites and fires on a fresh state; refused when d = c's line degenerate premise is missing (e.g. no dpl for CD).
2. Replay proposition_22'' (Prop22.lean:58-80): proposition_22' plus a point x off FE, and the extra conclusion k.sameSide x FE. The only step that differs from 22' is line 76: intersection_same_side KLH DKL x g f FE as k (in place of intersection_circles). Cite intersection_same_side from SystemE (find it, check verbatim, say its line) and render whatever Lean's SMT discharges for its premises as in I.22/I.22'. Reuse your 22' renderings. Goal: 22''s five conjuncts plus same_side k x FE. Then CONSTRUCTIONS "proposition_22''" verbatim (makes k and g with all six conclusions). Tests: replay passes; dropping x off FE -> refused at intersection_same_side; a by_cases/by_contra branch control as in 22'; store not contradictory; lemma fires; lemma refused with one inequality missing.
Gates: euclid_engine_test (all). Do not touch certify, trace, review or viewer files (kimi-2 has trace/viewer for I.19). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas and the steps with sources.
