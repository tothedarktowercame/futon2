# E-kimi-task-410 — viewer I.21

**Requisition:** completed — 2026-11-07T16:10:46Z, job invoke-1794067255220-15223-275e6530, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I21 (E-euclid-tutor), for kimi-2, from claude-9.

Your viewer I.20 (9fe6a9d) is accepted: links = Prop20.lean 12-17; the extend_point_longer generalization matches System E (bound = the passed segment); my bad case (d at (0.25,0.55), between b and a) fails viewer_check_20 on six checks; real screenshot restored and looked at.

Keep the job short: run only the I.21 trace tests plus rebuild-matches-disk (-k "21 or rebuild"), commit, then viewer_check_21, then bell. Kill any test server you start, by pid. euclid_certify_test.py is still mine (uncommitted fix running): do not touch it.

Goal: a Book trace for I.21 in the viewer (prop21.json): for d inside triangle a-b-c, |b─d| + |d─c| < |b─a| + |a─c| and ∠b:d:c > ∠b:a:c.
Lean, /home/joe/code/LeanEuclid/Book/Prop21.lean lines 13-19: intersection_lines BD AC as e; proposition_20 a b e; proposition_20 e d c; constructor; proposition_16 c e d b; proposition_16 b a e c. Engine: PROP21_HYPS/STEPS/GOAL (euclid_engine.py ~4060-4118; grep + sed <= 120 lines). The engine has engine-only steps Lean's SMT closes silently: triple_incidence_1, intersection_lines_opposing, pasch_4, and a by_contra showing between b d e (two branches, each refuted by pasch_3_opposing). Mark these engine-only, and for the by_contra say in the bar which orders of b, d, e it rules out and why (as in I.14/I.19, the true configuration is drawn: d strictly inside, e on a-c between a and c, d between b and e).
Placement: a scalene triangle with d clearly inside (not near a side). verify21: d inside (same side checks as the hypotheses); e on AC between a and c; between b d e; the two proposition_20 inequalities; the two exterior-angle inequalities (∠b:d:c > ∠d:e:c... use the engine's cited args to say which angles); both goals with visible margins. Last step's bar says why for both conjuncts (the chain |b─d| + |d─c| < |b─d| + |d─e| + |e─c| = |b─e| + |e─c| < |b─a| + |a─e| + |e─c| = |b─a| + |a─c|; and ∠b:d:c > ∠d:e:c... > ∠b:a:c by the two exterior angles). index.html option "I.21 lines from inside a triangle". Bad placements watched to fail (at least: d outside the triangle beyond BC; d on AC). viewer_check_21.py (copy viewer_check_20.py, port 8122); look at the screenshot. Other traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html, traces/prop21.json, ~/notes/euclid-cs/viewer_check_21.py. kimi-4 has euclid_engine.py in progress (I.24): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
