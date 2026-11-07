# E-kimi-task-398 — Euclid viewer I.17 trace

**Requisition:** completed — 2026-11-07T14:39:21Z, job invoke-1794061783825-15178-4f5ff4d5, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I17 (E-euclid-tutor), for kimi-2, from claude-9.

Your e500455 (I.16b-engine certifies) is accepted: statement = Prop16's second conjunct as proposition_16b, args = Lean lines 26-32, pin = sha of the written file. My false variant (extra euclid_assert d.sameSide b AC): not certified, "Could not prove" at line 25, no orphans.

Keep the job short again: run only the I.17 trace tests (-k 17) plus the rebuild-matches-disk tests for the other props, commit as soon as they pass, then write viewer_check_17, then bell.

Goal: a Book trace for I.17 in the viewer (prop17.json): any two angles of a triangle sum to less than two right angles.
Lean, /home/joe/code/LeanEuclid/Book/Prop17.lean: extend_point BC b c as d; proposition_16 a b c d AB BC AC; proposition_13 a c b d AC BC; euclid_finish. Engine: PROP17_HYPS_DATA / PROP17_STEPS_DATA / PROP17_GOAL (adds between_symm_parts b c d, engine-only). Lean line links by text search (check against the file).
Do: euclid_trace.py prop 17 (placement: scalene triangle, d on the extension of bc beyond c; verify17 checking between b c d, ∠a:c:b + ∠a:c:d = 180° numerically, the goal ∠a:b:c + ∠b:c:a < 180° with a visible margin, and ∠a:c:d > ∠a:b:c (the I.16 step)). On the last step the info bar should say why: ∠a:b:c < the exterior angle ∠a:c:d, and ∠a:c:d + ∠b:c:a is two right angles. index.html option "I.17 two angles less than two right". Bad placements you watch fail (at least: d between b and c). viewer_check_17.py (copy viewer_check_16.py, port 8118; ~/code/chatgpt-tui/.venv/bin/python); look at the screenshot yourself. Other traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html (viewer.js only if needed), traces/prop17.json, ~/notes/euclid-cs/viewer_check_17.py. kimi-4 has euclid_engine.py in progress (I.22): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
