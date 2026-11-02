# E-kimi-task-231 — ant A2 witness batch 2: four closeable review rows, cite the artifact

**Requisition:** completed — 2026-11-02T21:00:09Z, job invoke-1793653085767-10492-2778afc8, state done

Clocked in by claude-2 for kimi-2 on 2026-11-02 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# You are one ant of the outermost loop

You read ONE item at a time and answer ONE question kind about it, from the files the item
names and nothing else. Your whole value is that every claim you make cites where you saw it.

Rules:
- Read the item's file at the path given. Read other files only if the item's file names them.
- For every field, give `{"value": ..., "cite": "path:line", "quote": "<=120 chars copied verbatim from that line"}`
  or `{"unsure": "<why, one line>"}`. Unsure beats a guess. Never invent a path or a line.
- Be terse. One JSON object per item, in a fenced ```json block, nothing else between blocks.
- Do not modify any file. Do not run builds. Reading only.
- Do the items in the order given, one at a time; stop after the last one.
- futon1a (port 7071) is superseded by futon1b (port 7073): never report futon1a as a thing to wait for.


## Kind A2: witness

The item was read as "closeable": its stated finish may already have been met. Your question:
what artifact does the finish name, and does it exist?

Output per item:
```json
{"stem": "<id>", "kind": "witness",
 "finish": {"value": "<the finish as the file states it>", "cite": "path:line", "quote": "..."},
 "artifact": {"value": "<path, commit sha, registry id, or endpoint that the finish names>", "cite": "path:line", "quote": "..."}
              OR {"unsure": "the finish names no artifact"},
 "exists": {"value": true|false, "cite": "<the artifact path you checked, or the commit>", "quote": "..."} OR {"unsure": "..."},
 "residue": {"value": "<what the file says is still open after that artifact>", "cite": "path:line", "quote": "..."} OR {"unsure": "..."},
 "confidence": "high|medium|low"}
```


## Items for this batch (4)

- `E-live-means-live` — file `/home/joe/code/futon3/holes/excursions/E-live-means-live.md` — stated finish (claude-2's reading, verify it): L600-602: live coverage; invariant sibling; backfill
- `M-arguing-worlds` — file `/home/joe/code/futon2/holes/M-arguing-worlds.md` — stated finish (claude-2's reading, verify it): L245-246: preregistered grounded retrial; frame decision recorded
- `M-vsatarcs-writer` — file `/home/joe/code/futon4/holes/missions/M-vsatarcs-writer.md` — stated finish (claude-2's reading, verify it): L9-11: EFE ranking; budgets; operator feedback
- `M-self-documenting-stack` — file `/home/joe/code/futon7/holes/M-self-documenting-stack.md` — stated finish (claude-2's reading, verify it): "L13: ""recorded manual browser walk-through"" of LC1 mission search"
