# E-kimi-task-404 — viewer I.19

**Requisition:** completed — 2026-11-07T15:24:27Z, job invoke-1794064384095-15199-1c2a9727, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I19 (E-euclid-tutor), for kimi-2, from claude-9.

Your d3a26ff (I.18-engine certifies, sum_angles_onlyif emitted as itself with no bridges) is accepted. My bad case (proposition_5' dropped): not certified, "Could not prove: ∠b:c:a < ∠a:b:d + ∠d:b:c", no orphans. One fix of mine (405a14c, comments only): the Prop18.lean line refs were one off (statement 8-10, steps 13-16, sum_angles_if 17).

Keep the job short: run only the I.19 trace tests plus rebuild-matches-disk (-k "19 or rebuild"), commit as soon as they pass, then viewer_check_19, then bell. Kill any test server you start, by pid.

Goal: a Book trace for I.19 in the viewer (prop19.json): the greater angle is subtended by the greater side (∠a:b:c > ∠b:c:a gives |ac| > |ab|).
Lean, /home/joe/code/LeanEuclid/Book/Prop19.lean lines 12-17: by_contra; by_cases |a─c| = |a─b|; branch 1 proposition_5' a b c AB BC AC (equal sides give equal angles, contradicting the hypothesis); branch 2 proposition_18 a c b AC BC AB (|ab| > |ac| gives ∠a:c:b > ∠a:b:c, contradicting it). Engine: PROP19_HYPS_DATA / PROP19_STEPS_DATA / PROP19_GOAL (grep + sed <= 120 lines; it is a by_contra with two branches).
This is a proof by contradiction, like I.14: draw the TRUE configuration (a scalene triangle with ∠a:b:c clearly > ∠b:c:a, so |ac| > |ab|), and say in the info bar of each branch step that it runs under an assumption the proof refutes (branch 1: |ac| = |ab|; branch 2: |ac| < |ab|) and which hypothesis it contradicts. See how I.14's trace handles its by_contra and reuse that. Lean line links by text search, checked against the file (lines 12-17; the by_contra/by_cases lines too if the trace has steps for them).
verify19: hypothesis ∠a:b:c > ∠b:c:a with a visible margin; goal |ac| > |ab| with a visible margin; a off BC. Bad placements you watch fail (at least: ∠a:b:c < ∠b:c:a; isosceles |ac| = |ab|). index.html option "I.19 greater angle, greater side". viewer_check_19.py (copy viewer_check_18.py, port 8120; ~/code/chatgpt-tui/.venv/bin/python); look at the screenshot. Other traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html (viewer.js only if needed), traces/prop19.json, ~/notes/euclid-cs/viewer_check_19.py. kimi-4 has euclid_engine.py in progress (I.23): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
