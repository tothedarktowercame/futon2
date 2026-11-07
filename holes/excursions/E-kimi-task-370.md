# E-kimi-task-370 — E-euclid-tutor: viewer Book trace for I.12 (retry after API timeout)

**Requisition:** completed — 2026-11-07T09:29:41Z, job invoke-1794043292390-15058-86f66898, state done

Clocked in by claude-9 for kimi-1 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Retry note (claude-9): your first run of this packet (E-kimi-task-369) failed at 09:20Z with java.net.http.HttpTimeoutException (a model API timeout, not your fault) before any edits. Start again from the packet below; the tree is as you left it (no trace edits). euclid_review.py and euclid_search.py are kimi-2's dirty files: do not touch or stage them.

Packet P-euclid-viewer-I12 (E-euclid-tutor), for kimi-1, from claude-9.

Your 5dd4348 (I.12) is accepted: the four new rules match Points.lean:60, Diagrammatic.lean:297, Intersections.lean:6 and Book/Prop10.lean:7 line for line; the goal substitute (∠e:h:c = ∟ with e ≠ h on AB) is stated and its argument holds; the multi-base substitution is right, and I checked that it does not reopen the name-capture bug (a line named "a" passed to exists_point_opposite, a circle named "b" passed to intersections_circle_line: both keep their names).
It did expose a hole, which is now fixed in 3766548. intersections_circle_line is the first rule that makes two fresh objects plus a base-less fact (a ≠ b). Bound (x, x), it added neq x x. After (e, g) existed, binding (e, e) passed each own-base identification check and then added neq e e unchecked. Either way the store goes unsat. When a rule makes several objects, think about the facts that relate them, not each one alone.

Goal: Book trace for I.12 in the viewer (?prop=12).
One behaviour: traces/prop12.json built by scripts/euclid_trace.py from PROP12_HYPS_DATA / PROP12_STEPS_DATA, selectable in the viewer.
Files: /home/joe/code/futon0/scripts/euclid_trace.py, euclid_trace_test.py, holes/excursions/E-euclid-tutor-data/viewer/index.html (one <option>), viewer.js only if a new drawing kind is truly needed, traces/prop12.json. Do NOT edit euclid_engine.py, euclid_search.py, euclid_review*.py (kimi-2 is in euclid_search.py now).
Read first: how I.10/I.11 were added (b9d5cca, 18f28d8: git show them): lean_lines_for_named, place_step kinds, verify10/verify11, the tests at the end of euclid_trace_test.py.
- Placement: new kinds for exists_point_opposite (a point on the other side of AB from c: say how far and why), intersections_circle_line (the two numeric circle-line intersections; which one is e and which is g is a free choice: record it as I.1 records its pick), proposition_10 (the midpoint). verify12: h on AB, |h-e| = |h-g|, |c-e| = |c-g|, angle e-h-c a right angle, all to 1e-9; c and d on opposite sides of AB.
- Lean lines: every engine step that Prop12.lean cites links its line; intersection_circle_line_1 (Lean gets it by SMT) has none and says so. Note the `use h` line sits before proposition_8 in Prop12.lean: the order test must still hold or say why not.
- Tests, as for I.10/I.11: rebuild matches disk; Lean lines name the rule and cover every euclid_apply line; step facts hold (metric_facts_hold); bad cases you watch fail: h moved along AB off the midpoint, c moved so the angle is not right, d moved to c's side.
- MEASURE the page, do not just count rows: run headless (python ~/code/chatgpt-tui/.venv/bin/python with playwright; serve the viewer dir on 127.0.0.1:8103 with python3 -m http.server) and check: board bottom <= info-bar top, every placed point drawn at its trace coordinates (JSXGraph point elTypes include 'intersection' and 'otherintersection'), the screen angle e-h-c is 90 ± 0.5 degrees, no console errors. ~/notes/euclid-cs/viewer_check_10_11.py does exactly this for I.10/I.11: copy it to ~/notes/euclid-cs/viewer_check_12.py and adapt. Save a screenshot ~/notes/euclid-cs/kimi-viewer-prop12.png and LOOK at it before you report.
Gates (from scripts/, ~/code/futon6/.venv/bin/python -m pytest): euclid_trace_test; and the I.1-I.11 traces regenerate byte-identical (python3 euclid_trace.py 1,2,3,4,5,10,11 then git status shows no change). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + the measured numbers.
