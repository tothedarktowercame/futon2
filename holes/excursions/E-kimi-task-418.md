# E-kimi-task-418 — Euclid: play configurations I.6, I.7

**Requisition:** in-progress — dispatched 2026-11-07T17:28:18Z to kimi-4 as invoke-1794072498867-15287-33398d49

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-config-I6-I7 (E-euclid-tutor), for kimi-4, from claude-9.

Your I.25 (9ba159e, dc494f8) is accepted. Checked the arg orders against Prop25.lean 13-20 and the case rendering (equal / less under the negated goal: trichotomy). My bad case: dropping the hypothesis |a─c| = |d─f| -> ByContraFailure in the equal-angle case. Final store not contradictory. The engine now replays I.1-I.25.

Context: Joe plays Book I in order on a play page (futon0 scripts/euclid_play.py + viewer/play.html). The page's catalogue is the propositions in euclid_trace.FREE, today 1-5 and 10-21. This packet puts I.6 and I.7 into the sequence.

Goal, for N = 6 and 7 (in scripts/euclid_trace.py, plus euclid_play.py's GOALS table only):
1. FREE[N]: a concrete placement of the hypothesis points for PROP6_HYPS_DATA / PROP7_HYPS_DATA, chosen so every hypothesis fact holds numerically (tr.fact_holds); a scalene-looking triangle, as at I.16-I.21. I.6 and I.7 are proofs by contradiction: the placement is the TRUE configuration (for I.7, the hypotheses as Lean states them), and the by_contra branches are drawn as the viewer already does for I.14/I.19 (bar text saying the supposition is refuted). objects_from_hyps for N; hyps_and_steps(N) returning the engine's own data; Lean links via lean_lines_for_named (Book/Prop06.lean, Prop07.lean); engine-only steps marked as elsewhere.
2. traces/prop6.json, prop7.json written by `euclid_trace.py N --out <viewer traces dir>`, and an <option> in viewer/index.html for each (title in the same style).
3. euclid_play.py GOALS: entries for 6 and 7 (the goal queries the engine's own replay uses for PROP6/PROP7; reuse a named GOAL if one exists). Nothing else in euclid_play.py.
Tests: in euclid_trace_test.py, as for I.21 (rebuild matches disk; Lean lines cover the apply lines; facts hold; one bad placement fails). In euclid_play_test.py: play(6, engine steps) and play(7, engine steps) reach the goal; the catalogue now lists 6 and 7 between 5 and 10, and palette(7) contains proposition_6 and not proposition_7.
Rules: never read euclid_engine.py or euclid_trace.py whole (grep + sed <= 120 lines). Do not modify euclid_engine.py or play.html. Test servers on ports 8126-8127 only, killed by the pid from `ss -ltnp`; never 8100 (Joe's live server). Stage explicit paths only; no commit -a, no stash, no push. Gate: euclid_trace_test (-k "6 or 7 or rebuild") and euclid_play_test.
Bell claude-9 back with a summary + commit sha, test counts, and what each by_contra's bar says.
