# E-kimi-task-413 — Euclid: play backend (Witness-style, in order)

**Requisition:** completed — 2026-11-07T16:53:07Z, job invoke-1794069925281-15257-49cc590a, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-backend (E-euclid-tutor), for kimi-2, from claude-9.

Your certify I.20 (c2c98f5) is accepted: statement and imports verbatim, all six steps euclid_apply, negatives refused by Lean (not the clock). My bad case: dropping sum_angles_onlyif is CERTIFIED by Lean (euclid_finish closes the angle sum itself), so a negative must drop a step Lean actually needs, as yours did.

Context: Joe wants to PLAY Book I as a student, the propositions in order, like the puzzle game The Witness: a proposition unlocks when the one before it is solved, and each solved proposition becomes a move you can cite later. This packet is the backend only; the page comes next.

Goal: new file scripts/euclid_play.py (stdlib only; imports euclid_engine as eng and euclid_trace as tr) plus scripts/euclid_play_test.py.
1. catalogue(): ordered list, one entry per proposition N in tr.FREE (today 1-5, 10-21), each with: n, title (the viewer's option text in holes/excursions/E-euclid-tutor-data/viewer/index.html), the Lean statement text (Book/PropNN.lean, from "theorem" to ":="), hyps (tr.hyps_and_steps(N)[0]), goal facts (the same goal queries euclid_trace already uses per prop: reuse, do not copy), and palette(N).
2. palette(N): rule names a student may use at N = every eng.CONSTRUCTIONS entry that is not a proposition, plus "proposition_K" and its primed variants for K < N only. Citing proposition_N or later must be refused with a plain message ("I.N is not proved yet").
3. play(N, moves): STATELESS. Build the start state exactly as euclid_trace does for N (metric hyps for 4/5, the prop-3 row, close), then apply each move {rule, args, bindings} with eng.apply_step + close, stopping at the first failure. Return {moves: [{ok, added facts, error}], goal_reached: bool (eng._fact_holds on every goal fact), objects with coordinates (tr.objects_from_hyps + tr.place_step per accepted move, FREE[N] placement), contradictory: eng.contradictory(s)[0]}. Errors: MissingPremise -> the missing fact; unknown rule / rule not in palette / bad arity -> a message. by_contra and by_cases moves: accept them as moves too (same tuples the engine uses) but they are not needed for this packet's tests.
4. HTTP: `python3 euclid_play.py --port P` serves the viewer directory statically AND GET /api/catalogue, POST /api/play {n, moves} (JSON; facts as lists). Bind 127.0.0.1. Test on port 8123 only; kill it by the pid from `ss -ltnp`.
Tests (scripts/euclid_play_test.py, run with ~/code/futon6/.venv/bin/python -m pytest -q from scripts/): for every N in the catalogue whose engine steps are all straight-line (no by_contra/by_cases), play(N, the engine's own steps) reaches the goal; playing all but the last step does not; palette(5) contains proposition_4 and not proposition_5 or later; citing proposition_6 at N=5 is refused with the message; a move with a missing premise (e.g. I.1's intersection_circle_circle before the second circle) returns that premise as the error and goal_reached false; one HTTP round trip on 8123 (catalogue + play) then the server killed.
Rules: never read euclid_engine.py or euclid_trace.py whole (grep + sed <= 120 lines). Do not modify them; if you need a helper from inside a function there, say so in the bell rather than refactoring. kimi-4 is working in euclid_engine.py (I.24): do not touch or stage it. Stage explicit paths only (the two new files); no commit -a, no stash, no push. Commit when tests pass, before belling.
Bell claude-9 back with a summary + commit sha, test count and time, and which catalogue entries reach the goal by replay and which do not (and why).
