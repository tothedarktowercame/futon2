# E-kimi-task-396 — Euclid viewer I.16 trace

**Requisition:** completed — 2026-11-07T14:14:53Z, job invoke-1794060014520-15165-202b42a6, state failed

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I16 (E-euclid-tutor), for kimi-2, from claude-9.

Your cdbc809 (I.15-engine certifies) is accepted: statement verbatim, proposition_13 args = Lean lines 12-14, pin matches the emitted file. My false variant (euclid_assert ∠a:e:c = ∠c:e:b before euclid_finish): not certified, "Could not prove" at line 18, no orphans. Thanks for the clean 18-minute full run.

Goal: a Book trace for I.16 in the viewer (prop16.json): the SECOND conjunct, ∠a:c:d > ∠b:a:c, which is Euclid's own proof (Lean's second bullet, /home/joe/code/LeanEuclid/Book/Prop16.lean lines 20-27: proposition_10 a c AC as e; line_from_points b e; extend_point_longer BE b e (b─e) as f'; proposition_3 e f' b e BE BE as f; line_from_points f c; proposition_15 a c b f e AC BE; proposition_4 e b a e f c BE AB AC BE FC AC; euclid_finish). Engine: PROP16B_HYPS_DATA / PROP16B_STEPS_DATA / PROP16B_GOAL in euclid_engine.py (kimi-4's 8c796aa; read with grep + sed ranges <= 120 lines, never whole). Find the line numbers yourself by text search; check them against my list.
Do: euclid_trace.py prop 16 (placement: a scalene triangle, d on the extension of bc beyond c; e the midpoint of ac; f with e the midpoint of bf; verify16 checking between b c d, e midpoint of ac, between b e f with |be| = |ef|, and the goal ∠a:c:d > ∠b:a:c numerically with a visible margin; Lean line links; engine-only steps (between_symm_parts, pasch_3_opposing, opposing_opposing_same_side, sum_angles_onlyif: whatever PROP16B has) noted as engine-only). The diagram should make Euclid's point visible: ∠b:a:c = ∠e:c:f (the SAS), and ∠e:c:f is part of ∠a:c:d; say in the info bar on the last step that the whole exceeds the part. index.html option "I.16 exterior angle". viewer.js only if needed. Bad placements you watch fail (at least: d between b and c; f not on ray b→e). viewer_check_16.py (copy viewer_check_15.py; spare port, not 8097/8090/8100; run it with ~/code/chatgpt-tui/.venv/bin/python or uv run --with "playwright==1.58.0", do not install into another project's venv); look at the screenshot yourself. I.1-I.15 traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html/viewer.js, traces/prop16.json, ~/notes/euclid-cs/viewer_check_16.py. kimi-4 has euclid_engine.py in progress (I.22): do not touch or stage it. Gates: euclid_trace_test. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
