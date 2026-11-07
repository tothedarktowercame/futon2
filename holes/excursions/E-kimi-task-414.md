# E-kimi-task-414 — Euclid: I.24 second branch + lemma

**Requisition:** completed — 2026-11-07T17:18:22Z, job invoke-1794069925711-15258-4b66e8cb, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I24b (E-euclid-tutor), for kimi-4, from claude-9.

Your I.24a (f01e974, 67bd3e9) is accepted. Checked: arg orders vs Prop24.lean 18-39; the 23' disjunction elimination; the four-position g-on-DF split. My bad cases: angle hypothesis swapped -> ByContraFailure at the step-28 assert; proposition_4 or proposition_19 dropped -> goal not entailed; the branch hypothesis does not leak into the outer store; final store not contradictory.

Goal: the second bullet of Prop24.lean's by_cases (lines 40-59, "Omitted by Euclid"), making the replay the whole of I.24, then the cited lemma.
Step 1: PROP24_STEPS_DATA = the I.24a steps with the closing by_cases given its second branch (the hypothesis that d is NOT on g's side of EF, rendered as the engine renders negated sameSide elsewhere; say how and why that split is exhaustive). Inside it Lean has a nested by_cases g.onLine EF (line 41: first sub-case closes by euclid_finish; write out what the SMT closes and cite the axiom), then extend_point FG g f as h, the four "Not clear why these are needed" asserts (45-48: cite what each needs), proposition_13 twice, proposition_17 twice, the three angle asserts, proposition_19. Follow Lean's argument orders. Keep PROP24A_* tests passing unchanged; add PROP24_* tests: replay passes; drop each of proposition_13 (one), proposition_17 (one), proposition_19 from the second branch -> refused at a named place; final store not contradictory.
Step 2 (separate commit): CONSTRUCTIONS["proposition_24"], premises/conclusion verbatim from Prop24.lean's statement (cite its lines), in the same form as proposition_23's lemma, with the usual lemma tests (applies on the I.24 hyps; refuses with the angle hypothesis reversed).
Rules: never read euclid_engine.py whole (grep + sed <= 120 lines). New goal forms or fact kinds go in all five sites (helper, _fact_holds, apply_step make, replay goal, assume). Any SMT-bridge you need: say it in the bell, with why it is sound. Files: scripts/euclid_engine.py, scripts/euclid_engine_test.py only. kimi-2 is writing new files euclid_play.py / euclid_play_test.py: do not touch them. Stage explicit paths only; no commit -a, no stash, no push. Gate: euclid_engine_test (~/code/futon6/.venv/bin/python -m pytest -q euclid_engine_test.py from scripts/).
Bell claude-9 back with a summary + both commit shas, test count, and what each assert in lines 45-48 needed.
