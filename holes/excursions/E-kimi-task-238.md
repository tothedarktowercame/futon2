# E-kimi-task-238 — A1 place: open-unplaced batch 1, packet 3 of 8 (Joe via claude-16: open and unplaced first)

**Requisition:** completed — 2026-11-02T21:16:22Z, job invoke-1793654009017-10519-ac1911d7, state done

Clocked in by claude-2 for kimi-3 on 2026-11-02 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# You are one ant of the outermost loop

You read ONE item at a time and answer ONE question kind about it, from the files the item
names and nothing else. Your whole value is that every claim you make cites where you saw it.

Rules:
- Read the item's file at the path given. Read other files only if the item's file names them.
- For every field, give `{"value": ..., "cite": "path:line", "quote": "<=120 chars copied verbatim from that line"}`
  - A multi-line quote is checked by its first line only; cite ONE line, not a range, unless the quote spans lines (`path:a-b` is accepted).
  - To report that a file does NOT exist, cite the bare path with `"value": false` and the `ls: cannot access ...` line as the quote.
  - A bare path (no line) for a file that exists is accepted only if the quote appears somewhere in the file; prefer `path:line`.
  - One path per cite. Two files mean two fields or two rows, never `a.clj and b.clj:30`.
  or `{"unsure": "<why, one line>"}`. Unsure beats a guess. Never invent a path or a line.
- Be terse. One JSON object per item, in a fenced ```json block, nothing else between blocks.
- Do not modify any file. Do not run builds. Reading only.
- Do the items in the order given, one at a time; stop after the last one.
- futon1a (port 7071) is superseded by futon1b (port 7073): never report futon1a as a thing to wait for.


## Kind A1: place

The item is open and nobody has placed it on the map of the stack. Your question: what is it,
is it still open, which of the stack's four therefores does it serve, which declared way of
working it belongs to, and what one action would move it. You are placing, not deciding; a
reviewer scores your placement against another reader's.

Wings (the four therefores of the apex thesis, plus two others):
- A: records carry warrant — an assertion severed from its derivation cannot be checked or learned from.
- B: one queryable self-account — knowing what exists and its state, with provenance, at stack scale.
- C: feedback reaches every participant — the loop's human and stakeholder edges are where learning dies.
- D: accountable next-action choice — deciding what to do next, with a reason, goals included.
- V: stays alive and paid — machines, history, money; the body and energy of the stack.
- none: not about the stack's ways of working (e.g. a paper's content, an outside commitment).

Ways of working (pick the one the item most serves or most lacks; `none` if no fit):
records-carry-warrant, inbox-zero, operator-turns-logged (A); one-queryable-self-account,
backlog-understood (B); feedback-reaches-every-participant, real-users, intents-are-tracked (C);
selection-gives-a-reason (D); stays-alive-and-paid (V); eat-your-own-tail (the agent is inside the process it runs).

Output per item:
```json
{"stem": "<id>", "kind": "place",
 "what": {"value": "<one sentence: what the item is for>", "cite": "path:line", "quote": "..."},
 "status": {"value": "open|closed|stopped|unsure", "cite": "path:line", "quote": "<the status line or the line that shows it>"},
 "wing": {"value": "A|B|C|D|V|none", "cite": "path:line", "quote": "<the line that most shows which therefore it serves>"},
 "way": {"value": "<way id or none>", "cite": "path:line", "quote": "..."},
 "action": {"value": "work|close|retire|held|merge-into:<id>|unsure", "cite": "path:line", "quote": "<the line that supports the action>"},
 "finish": {"value": "<what done would look like, from the file>", "cite": "path:line", "quote": "..."} OR {"unsure": "no finish stated"},
 "beneficiary": {"value": "self|company|grant-math|product|job|unsure", "cite": "path:line", "quote": "..."},
 "confidence": "high|medium|low"}
```
Rules for this kind: a kimi-task requisition (`E-kimi-task-N`) is a record of one dispatch; its
"what" is the task it carried, its status is the requisition line near the top. A draft mission
marked NON-LIVE is `held`, not `work`. Do not place an item by its filename alone.


## Items for this batch (6)

- `E-kimi-task-228` — file `/home/joe/code/futon2/holes/excursions/E-kimi-task-228.md`
- `E-kimi-task-229` — file `/home/joe/code/futon2/holes/excursions/E-kimi-task-229.md`
- `E-kimi-task-67` — file `/home/joe/code/futon2/holes/excursions/E-kimi-task-67.md`
- `E-live-strategic-discovery-2026-09-09` — file `/home/joe/code/futon2/holes/labs/wm-contract/runs/E-live-strategic-discovery-2026-09-09.md`
- `E-mfuton-compiler-scope-connection-20261003` — file `/home/joe/code/futon3c/holes/excursions/E-mfuton-compiler-scope-connection-20261003.md`
- `E-mfuton-lisp-csimp-20261003` — file `/home/joe/code/futon3c/holes/excursions/E-mfuton-lisp-csimp-20261003.md`
