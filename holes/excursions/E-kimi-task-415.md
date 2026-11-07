# E-kimi-task-415 — Euclid: play page (Witness-style)

**Requisition:** in-progress — dispatched 2026-11-07T16:53:48Z to kimi-2 as invoke-1794070428536-15271-07441919

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-page (E-euclid-tutor), for kimi-2, from claude-9.

Your play backend (2d64b7d) is accepted. My bad cases: citing proposition_5 at I.5 and proposition_16 at I.5 are both refused with "I.N is not proved yet"; I.1 truncated does not reach the goal; I.1 with its first move's args reversed fails later at the missing intersects_cc; I.18 with proposition_3's first two args swapped is accepted but does not reach the goal and is not contradictory. Your two flags (GOALS restated inline for 2-5, 10-12; _assume_metric_or_fact for all hyps) are fine as you did them; leave them.

Goal: the student page, holes/excursions/E-euclid-tutor-data/viewer/play.html (one new file; same look as index.html, reuse its drawing code by copying what you need, do not change index.html), plus small additions to scripts/euclid_play.py and its test. Joe plays Book I in order, like the puzzle game The Witness.
1. Left: the proposition list from /api/catalogue in order. I.1 is open; I.N opens when the previous catalogue entry is solved. Solved ones show a tick. Progress (solved set, and each proposition's move list) is kept in localStorage, so a reload keeps it; a "reset progress" link clears it.
2. Centre: the board, drawn from play()'s objects (hypothesis objects at the start, each accepted move's objects added). Clicking a point/line/circle on the board appends its name to the argument being filled.
3. Right: the Lean statement as text, then a move panel: a rule dropdown from the palette (constructions first, then the cited propositions with their I.N names), one input per argument, and an input per new object to name (prefilled with a fresh letter). Backend support for this: add rule_info(name) to euclid_play.py returning {args, cite, binds} where binds are the names a rule's make introduces (find them without changing euclid_engine.py, e.g. from the make lambdas' returned keys on a throwaway state; if a rule's binds cannot be found that way, return [] and the page shows one free "name" field). Include rule_info in each catalogue palette entry; test it for line_from_points, circle_from_points, intersection_circle_circle and one proposition.
4. Each move POSTs the whole move list to /api/play. Accepted: draw it, list it under the board with its added facts in plain form. Refused: keep the move list unchanged and show the error in plain words, e.g. "Not yet shown: the circles BCD and ACE intersect" for a missing premise (write a small fact -> English table for the common kinds; fall back to the raw fact). An "undo last move" button. When goal_reached: a clear "Solved" banner and the next proposition opens.
5. No tutor yet; no hints. by_contra/by_cases moves are out of scope for this page (say so in a note on I.10, 13, 14, 19, 21: "needs a case move, coming next").
Tests: rule_info tests (python); a playwright check script ~/notes/euclid-cs/play_check.py (use ~/code/chatgpt-tui/.venv/bin/python, as for viewer_check_*), on port 8124 with `python3 scripts/euclid_play.py --port 8124`: load the page, play I.1 through the UI using the engine's own steps (filling the inputs), see Solved and I.2 open; reload and see I.1 still solved; a refused move shows the plain error; no console errors; screenshot to ~/notes/euclid-cs/kimi-play.png (look at it); kill the server by the pid from `ss -ltnp`.
Rules: do not modify euclid_engine.py or euclid_trace.py or index.html; never read the engine whole (grep + sed <= 120 lines). kimi-4 is working in euclid_engine.py: do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push. Commit when the checks pass, before belling.
Bell claude-9 back with a summary + commit sha, test counts, and what the page does on I.1 step by step.
