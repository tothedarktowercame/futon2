# E-kimi-task-401 — viewer I.18

**Requisition:** in-progress — dispatched 2026-11-07T14:52:33Z to kimi-2 as invoke-1794063153363-15190-ebd20aa1

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I18 (E-euclid-tutor), for kimi-2, from claude-9.

Your b9a45a0 (I.17-engine certifies) is accepted: the emitted file equals Prop17.lean's steps plus between_symm, and the pin 5c61a14de642c970 is the sha of the written file. My own bad case (proposition_16 line dropped): not certified, "Could not prove" at euclid_finish, no orphans. On servers: :8100 is mine (the viewer for Joe), leave it; :8104 is not ours either.

Keep the job short: run only the I.18 trace tests (-k 18) plus the rebuild-matches-disk tests, commit as soon as they pass, then write viewer_check_18, then bell. Kill any test server you start, by pid, before belling.

Goal: a Book trace for I.18 in the viewer (prop18.json): in a triangle, the greater side subtends the greater angle (|ac| > |ab| gives ∠a:b:c > ∠b:c:a).
Lean, /home/joe/code/LeanEuclid/Book/Prop18.lean lines 13-17: proposition_3 a c a b AC AB as d; line_from_points b d as BD; proposition_16 b c d a BC AC BD; proposition_5' a b d AB BD AC; sum_angles_if b a c d AB BC. Engine: PROP18_HYPS_DATA / PROP18_STEPS_DATA / PROP18_GOAL (euclid_engine.py ~3640-3675; grep + sed ranges <= 120 lines). The engine's last step is sum_angles_onlyif where Lean has sum_angles_if: link it to line 17 and say in its placement note that the engine uses the other direction of the same axiom (Transfer.lean), so the reader is not misled. Lean line links by text search, checked against the file.
Do: euclid_trace.py prop 18. Placement: a scalene triangle with |ac| clearly > |ab|; d is CONSTRUCTED by step 1 on segment a-c with |ad| = |ab| (not a hypothesis). verify18: between a d c; |ad| = |ab|; ∠a:b:d = ∠a:d:b (I.5'); ∠a:d:b > ∠d:c:b (I.16, exterior angle of triangle b-c-d); ∠a:b:c = ∠a:b:d + ∠d:b:c (d inside the angle); goal ∠a:b:c > ∠b:c:a with a visible margin. Last step's info bar says why: ∠a:b:c > ∠a:b:d = ∠a:d:b > ∠d:c:b = ∠b:c:a. index.html option "I.18 greater side, greater angle". Bad placements you watch fail (at least: d beyond c, i.e. |ad| = |ab| but not between a and c; and |ac| < |ab|). viewer_check_18.py (copy viewer_check_17.py, port 8119; ~/code/chatgpt-tui/.venv/bin/python); look at the screenshot yourself. Other traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html (viewer.js only if needed), traces/prop18.json, ~/notes/euclid-cs/viewer_check_18.py. kimi-4 has euclid_engine.py in progress (I.22'): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
