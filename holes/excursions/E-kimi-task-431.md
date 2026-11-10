# E-kimi-task-431 — Euclid play: case figures (a case is a different figure)

**Requisition:** completed — 2026-11-10T04:08:37Z, job invoke-1794281641606-16581-17095f16, state done

Clocked in by claude-9 for kimi-2 on 2026-11-10 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-case-figures (E-euclid-tutor), for kimi-2, from claude-9.

Why (Joe, 2026-11-10): once cases come into play the game stops feeling like Euclid and starts feeling like filling in a Hilbert-style proof. Codex's playtest agrees: assuming |ab| > |ac| in I.6 leaves the same symmetric figure. A case should be a DIFFERENT FIGURE: the same givens, placed so that the case's assumption visibly holds; and if no placement can satisfy the givens and the case together, the case is impossible, and the page should say that it cannot be drawn (that is exactly what the student then proves).

Goal (server only; I will do the page): in scripts/euclid_play.py, when the move list ends in an OPEN case block (nested blocks: use the innermost open one), the drawing is computed from a placement of the proposition's free points (tr.FREE[n] keys, plus tr.derived_free_points) that satisfies (a) the proposition's hypotheses (tr.hyps_and_steps(n)[0], provenance "h" only, the same facts _start_state uses) and (b) the current case's hypotheses (and those of the enclosing open cases).

1. A numeric evaluator: violation(fact, xy) >= 0, zero when the fact holds, for the fact kinds that occur in hypotheses and case assumptions: on_line (a line object is the line through two named points: take its two points from the objects' jsx "p" pair, or from the hypotheses' dpl / on_line facts), not_on_line, dpl, between, same_side, opposing_sides, not_same_side, eq_len / gt_len / lt_len, eq_ang / gt_ang, peq, neq, neq_line. Strict facts (gt, neq, not_on_line, opposing) need a margin (e.g. 0.05 of the figure size) so the placement is visibly strict. Facts you cannot evaluate: skip, but list them.
2. A search: start from tr.FREE[n], minimise the total violation over the free points' coordinates (no scipy: random restarts + a simple coordinate/pattern search or Nelder-Mead you write; keep it under ~0.5 s for 6 free points); accept when total violation < 1e-6 (equalities) and strict margins hold. Prefer the placement closest to FREE[n] (add a small distance penalty) so the figure changes only as much as the case needs.
3. play(): if a placement is found, redo the DRAWING (not the logic) with it: the same replay of all moves, with xy starting from that placement instead of FREE[n] (refactor so the drawing pass can take an initial xy). Add to the response "case_figure": {"hyps": [case facts], "drawn": true, "moved": [free points that moved]}. If none is found: keep the FREE[n] drawing and return "case_figure": {"hyps": [...], "drawn": false, "why": "no placement satisfies the givens and this case"}; also report facts the evaluator skipped.
4. Do not change the engine, the referee/coverage, or play.html.

Tests (euclid_play_test), each checked against the numbers, not just flags:
- I.2, open case peq a c: drawn, and in the response objects a's xy equals c's xy (to 1e-6), the line BC and given distinctness still hold.
- I.6, open case gt_len a b a c: NOT drawn (I.6's hypotheses force |ab| = |ac| via the equal base angles? check: I.6 hyps are the equal base angles; then |ab| > |ac| is impossible) — if your search does find a placement, the test must show the facts it satisfies, and we discuss.
- I.2, open case on_line a BC: drawn, a on BC numerically.
- A case with no open block: no "case_figure" key; the drawing identical to today's (compare object xy).
- Timing: play() for these cases < 2 s.
Gates: euclid_play_test, plus ~/notes/euclid-cs/play_check.py if port 8124 is free (it starts its own server there; otherwise say you did not run it). Test servers only on 8125, started/stopped by your own pid; never 8100, 8124, 8126, 8127; never signal a process you did not start. Stage only your own hunks (check git diff for edits that are not yours before staging); no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha, the I.6 outcome, timings, and the fact kinds you could not evaluate.
