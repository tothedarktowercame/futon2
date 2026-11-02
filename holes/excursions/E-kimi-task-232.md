# E-kimi-task-232 — ant A5 rehome batch 1: four futon1a rows, name the futon1b equivalent

**Requisition:** completed — 2026-11-02T21:00:50Z, job invoke-1793653086189-10493-6cf9c248, state done

Clocked in by claude-2 for kimi-3 on 2026-11-02 (one Kimi task, one excursion, so the seat's
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


## Kind A5: rehome

The item depends on futon1a (port 7071), which is superseded by futon1b (port 7073, code at
/home/joe/code/futon1b). Your question: what exactly does the item take from futon1a, and what
in futon1b answers to each dependency?

Output per item:
```json
{"stem": "<id>", "kind": "rehome",
 "dependencies": [
   {"what": {"value": "<the futon1a thing the item uses: endpoint, gate, census, default port, file>", "cite": "path:line", "quote": "..."},
    "futon1b": {"value": "<the futon1b equivalent: path or endpoint>", "cite": "path:line in /home/joe/code/futon1b", "quote": "..."} OR {"unsure": "no equivalent found where I looked: <paths>"}}
 ],
 "port-size": {"value": "small|medium|large", "cite": "path:line", "quote": "..."} OR {"unsure": "..."},
 "confidence": "high|medium|low"}
```


## Items for this batch (4)

- `E-clean-up-substrate-2` — file `/home/joe/code/futon3c/holes/excursions/E-clean-up-substrate-2.md` — stated finish (claude-2's reading, verify it): L174-176: retraction path; empty types; negative-space map
- `M-legacy-sorry-cleanup` — file `/home/joe/code/futon2/holes/M-legacy-sorry-cleanup.md` — stated finish (claude-2's reading, verify it): L7-9: dispositions; gold corpus; logged facet
- `M-stack-hud-refactor` — file `/home/joe/code/futon0/holes/missions/M-stack-hud-refactor.md` — stated finish (claude-2's reading, verify it): L296-301: categories; widgets; alarm ribbon; rename
- `M-populate-substrate-2` — file `/home/joe/code/futon3c/holes/missions/M-populate-substrate-2.md` — stated finish (claude-2's reading, verify it): L142-146: canonical-label map; named query; live explainer counts
