# E-kimi-task-234 — A3 audit-explain: M-structure-seed-promotion

**Requisition:** completed — 2026-11-02T21:07:49Z, job invoke-1793653588831-10506-b31cceee, state done

Clocked in by claude-2 for kimi-2 on 2026-11-02 (one Kimi task, one excursion, so the seat's
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


## Kind A3: audit-explain

The item is open with a few acceptance boxes unchecked, and the operator asked for an audit:
what did this mission actually accomplish, what is still open, and would closing or dropping it
lose anything? The page must carry an explanation either way, so the explanation is the product.

Output per item:
```json
{"stem": "<id>", "kind": "audit-explain",
 "accomplished": [ {"value": "<one thing the mission delivered>", "cite": "path:line", "quote": "..."} ],
 "open": [ {"box": {"value": "<unchecked line>", "cite": "path:line", "quote": "..."},
            "progress": {"value": "<any record of partial work toward it>", "cite": "path:line", "quote": "..."} OR {"unsure": "no record found in the file or under <paths>"}} ],
 "consumers": {"value": "<who or what uses the delivered parts today: file, mission, endpoint>", "cite": "path:line", "quote": "..."} OR {"unsure": "..."},
 "explanation": {"value": "<=80 words, plain, what it accomplished and what closing would leave undone", "cite": "path:line", "quote": "..."},
 "recommend": {"value": "close|drop|continue", "cite": "path:line", "quote": "<the line that most supports this>"},
 "confidence": "high|medium|low"}
```


## Items for this batch (1)

- `M-structure-seed-promotion` — file `/home/joe/code/futon6/holes/missions/M-structure-seed-promotion.md` — stated finish (claude-2's reading, verify it): L339-340: structure_loss records; measured stopping rule
