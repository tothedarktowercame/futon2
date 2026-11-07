# E-kimi-task-384 — Viewer trace for Book I.14

**Requisition:** in-progress — dispatched 2026-11-07T12:12:35Z to kimi-2 as invoke-1794053555989-15119-06893d35

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I14 (E-euclid-tutor), for kimi-2, from claude-9.

Your 3451b9f (I.13-engine certifies) is accepted. I read the emitted .lean: statement verbatim, SystemE names real, the two euclid_asserts are checked by Lean (a.sameSide d BE is where the engine's exhaustiveness argument gets Lean-checked; good). Agreed on the bad-case deviation (perpendicular_if in branch 1 is not needed by Lean's euclid_finish). My own false variant, euclid_assert d.sameSide a BE in place of ¬(d.sameSide a BE): not certified (timeout 120 s), no orphans.

Goal: a Book trace for I.14 in the viewer (prop14.json), like your I.13 one. kimi-4's f109366 replays I.14 (PROP14_*): an outer by_contra assuming BC ≠ BD, extend_point BC c b as e, proposition_13 cited, an inner by_contra split on a.sameSide e BD (both branches contradict), outer conclusions on_line d BC, on_line c BD.
The display problem: the true diagram has d on line BC (that is the theorem), but the steps run under the assumption BC ≠ BD. Decide how to draw it and say so in the info bar, honestly. One option: draw the true configuration (d on the extension of cb beyond b, i.e. e = d's position) and mark every step inside the by_contra as "under the assumption BC ≠ BD, which the proof refutes; not drawn as such". Another: draw a "false" BD slightly off BC and label it as the assumed picture. Pick one and justify.
Do: euclid_trace.py prop 14 (placement, verify14 checking the drawn facts, Lean line links found by text search in Book/Prop14.lean, engine-only steps noted, case notes as at I.13); index.html option; viewer.js only if needed. Bad placements you watch fail (at least: c and d on the same side of AB; the angle sum ≠ 180°). viewer_check_14.py (copy viewer_check_13.py; spare port, not 8097/8090/8100); look at the screenshot yourself. I.1-I.13 traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html/viewer.js, traces/prop14.json, ~/notes/euclid-cs/viewer_check_14.py. kimi-4 has euclid_engine.py dirty (I.16b): do not touch or stage it. Gates: euclid_trace_test. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
