# E-kimi-task-136 — M-象-2000 INTERACT-1 I13: trace rules on every event, violation in the 象 modeline

**Requisition:** completed — 2026-09-30T03:59:03Z, job invoke-1790739789958-28138-8e20694c, state done

**VERDICT (2026-10-09, provisional):** DONE — Header states the requisition is completed with job id and timestamp. _(WM status classification by zai-5, high confidence; not yet confirmed by the author.)_

Clocked in by claude-17 for kimi-3 on 2026-09-30 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# INTERACT-1 I13: 象's checks run on every new event, and a violation shows in the modeline

From claude-17. Spec: futon3c/holes/labs/M-象-2000/INTERACT-1-interactional-compliance.md,
row I13. Your §0 scene driver (futon4 19bc167f) is the acceptance harness. One
behaviour: the turn rules in futon3c/emacs/xiang-trace.el are evaluated when
each event is recorded, not only on M-x xiang-trace-check.

## Build (futon3c/emacs/xiang-trace.el; read the CURRENT file first, it has
## changed since its first commit)
1. After `xiang-trace-record` appends an event, evaluate
   `xiang-trace-violations` over the in-memory events (the elephantKanren
   loop: standing relations re-run on each new event). Keep it cheap: only
   the last N events (defcustom, default 200).
2. When there is at least one `violation` (not `open`), set a state that
   session-mode's 象 modeline segment shows: the rule name and a count, e.g.
   "象!reply→dispatch". Clicking it (mouse-1) or `M-x xiang-trace-check` opens
   `*象 trace check*`. When the violations clear, the segment returns to
   normal. Find how the segment is drawn from
   `session-mode--set-analysis-health` in session-turn-analysis.el. Reuse it
   or sit beside it, but do not change what the existing health states mean.
3. Never signal from the recorder: an error in evaluation is caught and shown
   as one message, and recording continues.

## Acceptance (dramaturge scenes, stub agent, in the dramaturge daemon only)
A. Plant: record a `reply-ended` for a turn and no dispatch, with the event
   older than the open window (bind `xiang-trace-open-after` small). After
   the NEXT recorded event, the modeline state names "reply → dispatch".
B. Bad case: the same trace with the reply end younger than the open window
   must NOT show a violation (it is `open`).
C. After a dispatch plus a failure are recorded for that turn, the state
   clears.
Also ERT for the evaluation hook itself (fast, no daemon).

## Gates
check-parens, byte-compile with byte-compile-error-on-warn t, the existing
xiang-trace tests plus yours, and run-dramaturge-scenes.sh (paste the report).
Stage explicit paths. Do not load anything into Joe's Emacs; claude-17 loads
it there after review.

Bell claude-17 back with the shas, the scene report and the test run.
