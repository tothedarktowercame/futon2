# E-kimi-task-428 — Euclid play: derive I.9-I.11 trusted inputs in the game

**Requisition:** in-progress — dispatched 2026-11-10T02:08:11Z to kimi-2 as invoke-1794276491186-16500-5bafbc7d

Clocked in by claude-9 for kimi-2 on 2026-11-10 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-sm-derivations (E-euclid-tutor), for kimi-2, from claude-9.

Context: the play game (futon0 scripts/euclid_play.py) now starts each proposition from the theorem's hypotheses only (442b0c2). The engine's replays of I.5, I.9, I.10, I.11 also carry "h-sm" inputs: facts Lean's SMT supplies silently (see tr.hyps_and_steps(n)[0], entries with provenance "h-sm"). In the game a student must be able to derive them. I.5 is done (one explicit between_symm_parts move, PLAY_EXTRA_STEPS in euclid_play.py). Your job: I.9, I.10, I.11 (AWAITING_SM).

The facts to make derivable in the game:
- I.9: v != d, d != e, f off AB, f off AC  (d from point_between_points_shorter_than, e from proposition_3, f from proposition_1' — read the steps with ep.reference_steps(9)).
- I.10: c off AB (c the apex from proposition_1 on a b) — the reference stops at step 4 needing lines AC and BC different.
- I.11: f off AB (f the apex from proposition_1 on g e) — stops at step 9 needing f != w.

Do it in this order of preference, per fact:
1. Explicit moves with existing rules (between_symm_parts, by_contra blocks the referee covers, proposition_1' instead of proposition_1 where a point off the line is available, ...). Add them to PLAY_EXTRA_STEPS.
2. Only if no existing rule serves: a new CITED move in eng.CONSTRUCTIONS (not a closure rule), marked "[SMT-bridge]" in its cite, with (a) a comment giving the soundness argument from System E (e.g. "an equilateral apex is off its base: if c were on AB, one of a, b, c lies between the others, so one length is the sum of the other two, |ab| = 2|ab|"), (b) RULE_LABELS and ARG_HINTS entries in euclid_play.py, (c) a test that the move is REFUSED when a premise is missing (construct the bad case: e.g. the same move on an isosceles-but-not-equilateral point).
Do NOT edit the engine's PROP*_HYPS_DATA / PROP*_STEPS_DATA (they are the Lean-faithful replay that certify and the viewer use), nor play.html.

Done when: ep.AWAITING_SM is empty; euclid_play_test's test_trusted_inputs_are_not_in_the_game is updated (it pins where 9/10/11 stop today: make it assert they now reach the goal with reference_steps, and that dropping your added steps does not); 9 and 10 are back in test_case_props_reach_goal / test_catalogue_splits_covered if they have case blocks; test_engine_steps_reach_goal covers them.
Gates: euclid_play_test, euclid_engine_test, euclid_trace_test (all from scripts/, ~/code/futon6/.venv/bin/python -m pytest -q). Test servers only on 8125, started and stopped by your own pid; never 8100 (Joe's game) or other ports; never signal a process you did not start. Explicit paths; no commit -a, no stash, no push. Never read the whole engine file: grep for the rule names you need.
Bell claude-9 back with a summary + commit sha, per fact: which way you derived it (existing moves / new move + its soundness argument), and test counts.
