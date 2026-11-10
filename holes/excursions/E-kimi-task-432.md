# E-kimi-task-432 — Euclid play: impossible cases drawn with the broken given shown

**Requisition:** completed — 2026-11-10T04:56:07Z, job invoke-1794285203931-16648-e7077951, state done

Clocked in by claude-9 for kimi-2 on 2026-11-10 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-impossible-case-figures (E-euclid-tutor), for kimi-2, from claude-9.

Your case figures (fa06b11; reviewed and extended in 15e62c4, 041bb18) redraw the figure so an open case's assumption holds. codex-1's playtest shows the gap: in proofs by contradiction (I.6, I.7, I.19 ...) every case is impossible, so no figure is found and the student sees the usual figure. Euclid's reasoning is visual there too: "if ab were longer than ac, the base angles could not be equal". So for an impossible case, draw a figure where the CASE holds and show which GIVEN breaks.

Goal (server only, scripts/euclid_play.py; the page is mine):
1. When _case_placement finds no placement for (hypotheses + case facts), search again with each single proposition hypothesis left out in turn (only the "h" hypotheses, not the case facts; skip pure incidence/distinctness facts that just name objects, e.g. dpl/on_line of the defining points — they define the figure; relax metric and order facts first: eq_len, eq_ang, gt_*, between, same_side, opposing_sides, not_on_line). Take the first that yields a placement, preferring the one closest to FREE[n] (pen_of) among those found; keep the existing drag-first behaviour (only the case's point moves first).
2. Response: case_figure gets {"drawn": true, "relaxed": [the given(s) left out], "broken": [those givens with their numeric values in the drawing, e.g. "∠abc = 0.81, ∠acb = 0.62"]} — "drawn" true because a figure of the CASE exists; "relaxed" says which given it does not satisfy. Keep {"drawn": false} only when even relaxing one given finds nothing.
3. Keep timing under ~1 s for the playtested cases (cap the number of relaxations tried; say what cap you chose).
Tests (euclid_play_test), checked against numbers:
- I.6, open case gt_len a b a c: drawn with relaxed == the base-angle equality hypothesis, and in the returned xy |ab| > |ac| + margin and the two base angles differ by more than a margin.
- I.2 peq a c (a possible case): unchanged behaviour (no "relaxed" key).
- Find one case where relaxing nothing works and one where even one relaxation fails, or say none exists among I.1-I.25's engine case blocks (scan their by_cases/by_contra case facts with _case_placement and report the table: n, case facts, drawn/relaxed/none, time).
Do not touch the engine, referee/coverage or play.html. Test servers only on 8125 (your own pid); never 8100, 8124, 8126, 8127; never signal a process you did not start. Before staging, check git diff for hunks that are not yours (stage only yours); no commit -a, no stash, no push.
Gates: euclid_play_test (from scripts/, ~/code/futon6/.venv/bin/python -m pytest -q).
Bell claude-9 back with a summary + commit sha, the scan table, timings, and the relaxation cap.
