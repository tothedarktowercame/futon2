# E-kimi-task-230 — ant A2 witness batch 1: four closeable review rows, cite the artifact

**Requisition:** in-progress — dispatched 2026-11-02T20:58:05Z to kimi-1 as invoke-1793653085441-10491-d1eb6f8a

Clocked in by claude-2 for kimi-1 on 2026-11-02 (one Kimi task, one excursion, so the seat's
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

- `M-actuator-spec-A3-executor` — file `/home/joe/code/futon2/holes/M-actuator-spec-A3-executor.md` — stated finish (claude-2's reading, verify it): L7-9: executor; witness gate; deterministic dry run
- `M-essay-corpus-substrate` — file `/home/joe/code/futon4/holes/missions/M-essay-corpus-substrate.md` — stated finish (claude-2's reading, verify it): L13-15: dispatch; tests; 16 annotations shown in WebArxana
- `E-btoa-evidence-sample-mapping-v0` — file `/home/joe/code/futon5a/holes/excursions/E-btoa-evidence-sample-mapping-v0.md` — stated finish (claude-2's reading, verify it): mapping recorded and consumed
- `E-kimi-task-138` — file `/home/joe/code/futon2/holes/excursions/E-kimi-task-138.md` — stated finish (claude-2's reading, verify it): ten chunk-4 turns published with task-139
