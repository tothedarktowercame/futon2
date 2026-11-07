# E-kimi-task-367 — E-euclid-tutor: replay Book I.12 (perpendicular from a point off a line) in the engine

**Requisition:** in-progress — dispatched 2026-11-07T08:48:40Z to kimi-1 as invoke-1794041320496-15054-6fab9348

Clocked in by claude-9 for kimi-1 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I12 (E-euclid-tutor), for kimi-1, from claude-9.

Your 6342e2f (binding clash) is accepted. I built six bad cases myself: proposition_1 a b AB named b, line a c named AB with c off AB, circle named after a point, proposition_1 named after a c that has only |ca| = |ab|, and a line named after a point all refuse with the store unchanged; line a b named AB is accepted with added == []. Accepting an exact repeat as a no-op identification is right. Your flag about the student text was right too: it read "missing premise: ('fresh', 'b')"; my fix is a5f8491 ("names its new object b, but b already exists and the new object is not shown to be the same; use a new name").

Goal: replay Book I.12 (LeanEuclid /home/joe/code/LeanEuclid/Book/Prop12.lean: perpendicular from a point c off a line AB) in the engine.
One behaviour: PROP12_HYPS_DATA / PROP12_STEPS_DATA + replay test, following Prop12.lean's euclid_apply lines as closely as the engine allows.

Files: /home/joe/code/futon0/scripts/euclid_engine.py and euclid_engine_test.py only. Read first: PROP10_* and PROP11_* (the most recent replays) and their tests, for the conventions: [SMT-bridge] rules, "h-sm" inputs, footprints, by_contra steps.
Things to decide and state:
- exists_point_opposite, intersections_circle_line: find them in SystemE (grep /home/joe/code/LeanEuclid/SystemE) and add them as cited rules with their Lean file:line, verbatim where possible.
- proposition_10 is cited as a lemma: add it as a citable rule whose premises and conclusions are Book/Prop10.lean's statement (as proposition_8 / proposition_9' were done).
- The goal is a DISJUNCTION (∠ a:h:c = ∟ ∨ ∠ b:h:c = ∟), because h may coincide with a or b, where System E gives that angle no value. Find how (or whether) the engine can state this goal; if it cannot, say what you did instead (for example, prove ∠ e:h:c = ∟ and say why that implies Lean's goal) and do not invent a disjunction facility in this packet.
- Use lin() for every multi-atom LA row (dict literals collapse coinciding atoms: that was the |bc| = 0 bug).
- State every input Lean gets by SMT that you have to give the engine (as h-sm), and pin with a test that the others are derived.
Tests: replay reaches the goal (or your stated substitute); final store satisfiable (add I.12 to eng.main); footprint for I.12 added, I.1-I.11 unchanged; a control (drop one step: the replay stops at the named premise); for each new bridge rule, a needed-premise counterexample AND an enough-premises attempt (a state where every premise holds and you try to make the conclusion false, reading the SystemE axioms for straight angles, repeated points and degenerate segments); say plainly if you could not find one.
Gates (from scripts/, ~/code/futon6/.venv/bin/python -m pytest): euclid_engine_test, euclid_search_test, euclid_review_test, euclid_clones_test, euclid_clone_diagram_test, euclid_trace_test; report counts and wall time. Also python3 euclid_engine.py 1,2,3,4,5,10,11,12.
kimi-2 is running a read-only discovery in scripts/ (no edits). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas.
