# E-kimi-task-416 — Euclid: play case moves

**Requisition:** in-progress — dispatched 2026-11-07T17:05:44Z to kimi-2 as invoke-1794071144261-15273-36737143

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-case-moves (E-euclid-tutor), for kimi-2, from claude-9.

Your play page (c263c49) is accepted. I looked at kimi-play.png. My bad cases (playwright on 8125, fresh browser): clicking the locked I.2 leaves the page on I.1; after one accepted move, line_from_points a a is refused with "Not yet shown: a ≠ a" and the move list stays at one row; no console errors. The page is now live for Joe on :8100 (euclid_play.py, which also serves index.html).

Goal: case moves, so that I.10, I.13, I.14, I.19 and I.21 become playable. The engine already has them as move tuples: ("by_contra", [(branch_hyps, branch_steps), ...], conclusion) and ("by_cases", ...), same shape; see how PROP21_STEPS_DATA and PROP19_STEPS_DATA use them (grep + sed <= 120 lines; never read the engine whole), and eng._by_contra / eng._by_cases for what each checks (by_contra: every branch reaches a contradiction; by_cases: every branch reaches the conclusion; exhaustiveness is the caller's, i.e. here the student's, job).
1. Backend (euclid_play.py): play() must report on an OPEN block, so the student gets feedback while a case is in progress. Accept a move {rule: "by_contra"|"by_cases", branches: [{hyps: [...facts], moves: [...]}], conclusion: [...facts], open: true|false}, nested blocks allowed inside branch moves. For an open block, replay the state up to it, then for the last branch: assume its hyps, apply its moves, and report {branch_contradictory, branch_goal_reached (conclusion holds in the branch), objects}; earlier branches must already close. A closed block goes through the engine's own _by_contra/_by_cases unchanged (refusals reported as the engine words them). Tests: I.21 and I.19 played with the engine's own steps (as closed blocks) reach the goal; I.21 with its by_contra's last branch open and its moves missing reports branch_contradictory false; with them, true; a by_contra whose branch does not contradict is refused when closed.
2. Page (play.html): a "suppose…" move that opens a block: the student picks by_contra or by_cases, enters the branch hypotheses with a small fact builder (kind dropdown for the common kinds: between, same_side, opposing_sides, on_line, eq_len, gt_len, eq_ang, gt_ang, peq; one input per argument, board clicks fill them), and the conclusion. Moves made while a block is open go into its current branch, shown indented. Buttons: "next case" (closes this branch, opens another with new hypotheses) and "close" (closes the block). Show "this case is contradictory ✓" / "the conclusion holds in this case ✓" live. Undo works inside blocks. Remove the "coming next" note.
Checks: the python tests; extend ~/notes/euclid-cs/play_check.py (port 8124) to play I.21 through the UI with the engine's own steps (including its by_contra), reaching Solved; screenshot to ~/notes/euclid-cs/kimi-play-cases.png (look at it); kill the server by the pid from `ss -ltnp`. Do not use port 8100 (Joe's live server).
Rules: do not modify euclid_engine.py, euclid_trace.py or index.html. kimi-4 is working in euclid_engine.py: do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push. Commit when the checks pass, before belling.
Bell claude-9 back with a summary + commit sha, test counts, and which of 10, 13, 14, 19, 21 now play to Solved by their engine steps (and why not, for any that do not).
