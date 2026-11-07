# E-kimi-task-391 — Euclid viewer I.15 trace

**Requisition:** completed — 2026-11-07T13:28:31Z, job invoke-1794057215673-15146-12da6858, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I15 (E-euclid-tutor), for kimi-2, from claude-9.

Your a0a9a35 (I.14-engine certifies) is accepted. I read the emitted .lean: statement verbatim, by_cases a.sameSide e BD in place of the inner engine by_contra, and dropping branch 2's opposing_sides fact is right because ¬(a.sameSide e BD) is triple_incidence_2's own premise shape. My false variant (euclid_assert d.sameSide c AB): not certified, "Could not prove" at line 17, no orphans.

Goal: a Book trace for I.15 (vertical angles) in the viewer (prop15.json), like your I.13/I.14 ones.
Lean: /home/joe/code/LeanEuclid/Book/Prop15.lean — lines AB and CD cross at e (between a e b, between d e c); three proposition_13 citations; conclusion ∠a:e:c = ∠d:e:b and ∠c:e:b = ∠a:e:d. Engine: kimi-4's 6c69d59 (PROP15_* in euclid_engine.py; read with grep + sed ranges <= 120 lines, never whole).
Do: euclid_trace.py prop 15 (placement with the lines crossing at a non-right angle so the two angle pairs visibly differ; verify15 checking the drawn facts: both betweens, e on both lines, CD ≠ AB, and the two conclusion equalities numerically; Lean line links by text search in Book/Prop15.lean; engine-only steps noted). index.html option; viewer.js only if needed. Bad placements you watch fail (at least: e not between a and b; lines perpendicular is fine geometrically but say whether your check would accept it, and why). viewer_check_15.py (copy viewer_check_14.py; spare port, not 8097/8090/8100); look at the screenshot yourself. I.1-I.14 traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html/viewer.js, traces/prop15.json, ~/notes/euclid-cs/viewer_check_15.py. kimi-4 has euclid_engine.py in progress (I.19): do not touch or stage it. Gates: euclid_trace_test. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
