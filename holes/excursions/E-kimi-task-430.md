# E-kimi-task-430 — Euclid play: every goal clause visible, angle arcs

**Requisition:** completed — 2026-11-10T03:11:13Z, job invoke-1794278966958-16523-937bd720, state done

Clocked in by claude-9 for kimi-2 on 2026-11-10 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-goal-checklist (E-euclid-tutor), for kimi-2, from claude-9.

Both playtesters (kimi-1, codex-1) and Joe put this first: the figure must READ as solved or unsolved. Today play() returns marks (euclid_play.py _length_marks: classes of equal segments + the goal's first clause if it is a length) and play.html drawMarks() colours them. Angles get nothing, and only the first goal clause is shown: at I.5, proving the first of two angle equalities changes nothing visible (codex-1, codex-shots/i5-progress-invisible.png).

Goal (one behaviour: every goal clause visible with its state):
1. Server (_length_marks, or a sibling _goal_marks): for EVERY clause of GOALS[n] (with the witness binding as _length_marks does), report {kind, objects, state} where state is "shown" (eng._fact_holds on the query form) or "pending". Kinds to cover: _eq_len, _gt_len, _eq_ang, _gt_ang, between, on_line, not_on_line, neq, _sum_rt_ang/_lt_sum_rt_ang (words only is fine for the last two). Also equal-ANGLE classes, like the length classes: angle atoms joined by two-term equalities in s.la.rows (atoms ("ang", x, v, y)), restricted to drawn points.
2. Page: a goal checklist under the statement, one line per clause, "☐ ∠abc = ∠acb" / "☑ ...", in plain words (fact words), replacing the single goal line; pending clauses drawn dashed orange on the figure, shown ones in their class colour. Angle classes drawn as arcs at the vertex (JSXGraph "angle" or "arc" element, small radius, same colour per class, 1/2/3 arcs like the ticks), restricted to angles whose three points are drawn.
3. Keep it light: no marks for aux objects; at most ~12 angle marks drawn (largest classes first).
Tests (euclid_play_test): I.5 with the reference moves up to the first proposition_4 shows clause 1 "shown" and clause 2 "pending" (pick the right prefix by checking); at the end both shown; I.1's two clauses both appear. Browser: ~/notes/euclid-cs/play_check.py must still pass (start your own server on 8125 for it: it starts its own on 8124, so run it when 8124 is free, or not at all and say so); look at a screenshot of I.5 mid-proof with view_image and describe it.
Do not touch the engine. Test servers only on 8125, started/stopped by your own pid; never 8100, 8124, 8126, 8127; never signal a process you did not start. Explicit paths; no commit -a, no stash, no push.
Gates: euclid_play_test (from scripts/, ~/code/futon6/.venv/bin/python -m pytest -q).
Bell claude-9 back with a summary + commit sha, test counts, and what the I.5 screenshot shows mid-proof.
