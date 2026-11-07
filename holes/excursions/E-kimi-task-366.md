# E-kimi-task-366 — E-euclid-tutor discovery: does the review accept the Book's Lean proof as a student attempt

**Requisition:** completed — 2026-11-07T09:02:14Z, job invoke-1794040449564-15052-485d240d, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-lean-proof-as-student (E-euclid-tutor), for kimi-2, from claude-9. DISCOVERY ONLY: no code edits.

Question: when a student submits exactly the Book's Lean proof, step for step, does the engine review call every step valid? Or does it mark correct steps INVALID because the engine needs steps that Lean leaves to euclid_finish / SMT?

Why it matters: the engine's I.10 replay has 17 steps, but Book/Prop10.lean has 7 euclid_apply lines; the other 10 (three by_contra case splits, line_from_points a d' / b d', a proposition_4 on d', intersection_lines_opposing, pasch_4, angle_pair_on_line) are engine-only (see holes/excursions/E-euclid-tutor-data/viewer/traces/prop10.json: steps with "lean": {"line": null}). A tutor that marks a correct Lean proof INVALID is wrong in the way that does most harm to a student.

Do (in /home/joe/code/futon0/scripts, Python ~/code/futon6/.venv/bin/python):
1. For each of I.1, I.2, I.3, I.5, I.10, I.11: build the student attempt = only the steps that Lean's Book/PropNN.lean cites (LeanEuclid at /home/joe/code/LeanEuclid/Book/), in engine form, using the engine's own step tuples (PROPn_STEPS_DATA in euclid_engine.py) filtered to those with a Lean line. For I.11 the engine renames points (u v w g = Lean a b c d); use the engine's names. The trace JSONs in viewer/traces/ already say which engine step has which Lean line.
2. Run euclid_review.review(prop, steps) on each (see euclid_review_test.py for the call shape; I.10/I.11 hypotheses come from PROP10_HYPS_DATA / PROP11_HYPS_DATA — find how review() picks hypotheses per prop, and if it cannot take I.10/I.11, say so precisely and stop for those).
3. Report per prop: which steps are valid / invalid / undecided / blocked, the exact "why" text of each non-valid step, goal_held, and for each INVALID step the engine-only step(s) whose absence causes it (test by adding back the engine-only steps one at a time and seeing which one makes it valid).
4. Your recommendation, in two or three sentences: how should the engine treat a step whose missing premise Lean gets from SMT (for example run the engine-only steps automatically as closure; or mark the step "valid, needs SMT" not "invalid")? Do not implement it.

Write the report to ~/notes/euclid-lean-proof-as-student.md (tables, exact texts, the script you used inline). No edits under futon0 (another seat is editing euclid_engine.py, euclid_engine_test.py and euclid_search.py now; their dirty state is in the tree, so say in the report which engine you ran against: `git -C ~/code/futon0 status --short scripts/`). Scratch files under ~/notes/euclid-cs/, not /tmp.
Bell claude-9 back with a summary and the report path.
