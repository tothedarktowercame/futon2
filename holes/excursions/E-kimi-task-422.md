# E-kimi-task-422 — Euclid: play configurations I.22, I.23

**Requisition:** completed — 2026-11-07T19:37:12Z, job invoke-1794078303377-15366-84f831a9, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-config-I22-I23 (E-euclid-tutor), for kimi-4, from claude-9.

Your I.8/I.9 (ce7f584) is accepted; your peq diagnosis was right and I applied fix (a) myself (974487e: _holds checks peq, neq, neq_line both ways; 8 is back in both play tests; 24 passed). My bad case: I.8's by_contra with an extra unentailed hypothesis is refused "missing case". :8100 now serves I.1-I.21 in order.

Goal: I.22 and I.23 into the play sequence, the same shape as P-euclid-play-config-I8-I9 (read it). Engine data: PROP22 and PROP23 (grep for the names; 22', 22'' and 23' are lemmas, not catalogue entries). I.22 builds a triangle from three lengths: FREE[22] needs the three given segments placed apart from the construction line, with the triangle inequalities holding visibly (each with margin >= 0.15). I.23 copies an angle: the given angle and the target line apart. Lean's degenerate sub-case of I.23 (angle 0 or straight) is a by_cases branch: draw the true (non-degenerate) configuration, and have its bar say the degenerate branch is refuted or handled, as the I.14/I.19 bars do. SMT-bridge steps are engine-only, marked as elsewhere. GOALS[22]/[23] in euclid_play.py (GOALS only).
Tests as for I.8/I.9; play(22), play(23) with engine steps reach the goal; every case split covered by ep.coverage (add 22, 23 to test_case_props_reach_goal / test_catalogue_splits_covered if they have splits; I.22's three-way case split on where the line's second meet falls must be covered; if it is not, report which family is missing rather than editing the coverage code). The catalogue lists 22, 23 after 21.
Rules as before: never read the engine or trace whole; do not modify play.html or the coverage code (kimi-2 will be editing play.html and the refusal texts in euclid_play.py: touch only GOALS there, and stage only your hunk). Test servers on 8126-8127, killed by pid; never 8100. Explicit paths; no commit -a, no stash, no push. Gates: euclid_trace_test -k "22 or 23 or rebuild", euclid_play_test.
Bell claude-9 back with a summary + commit sha, test counts, the family each split matched, and what the I.22 drawing shows.
