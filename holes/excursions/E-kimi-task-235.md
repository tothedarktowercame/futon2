# E-kimi-task-235 — A4 lesson: M-expressions-of-interest as a prototype we can learn from

**Requisition:** in-progress — dispatched 2026-11-02T21:06:29Z to kimi-3 as invoke-1793653589291-10507-56eafaca

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


## Kind A4: lesson

The item is a workflow the operator describes as "a prototype we can learn from", not a feature:
a method that was used for a while and then stopped being used. Your question: what was it, when
was it last used, what does the record say about why it stopped, and what did it teach? Only the
files are evidence; the operator's own recollection is not available to you and must not be invented.

Output per item:
```json
{"stem": "<id>", "kind": "lesson",
 "what": {"value": "<the method in one sentence>", "cite": "path:line", "quote": "..."},
 "last-use": {"value": "<date or record of the last real use>", "cite": "path:line", "quote": "..."} OR {"unsure": "..."},
 "friction": [ {"value": "<a recorded difficulty, cost or complaint>", "cite": "path:line", "quote": "..."} ],
 "lessons": [ {"value": "<something the record shows was learned>", "cite": "path:line", "quote": "..."} ],
 "lives-on": {"value": "<any part that continues elsewhere: file, mission, repo>", "cite": "path:line", "quote": "..."} OR {"unsure": "..."},
 "confidence": "high|medium|low"}
```


## Items for this batch (1)

- `M-expressions-of-interest` — file `/home/joe/code/futon5a/holes/missions/M-expressions-of-interest.md` — stated finish (claude-2's reading, verify it): "Acceptance checklist, 0 of 6, line 1204: includes ""pass Joe's re-reading 'really?' test""."
