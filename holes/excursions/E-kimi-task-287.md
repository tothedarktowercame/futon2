# E-kimi-task-287 — relate batch 024: area readings for 20 WM candidate targets (A7)

**Requisition:** in-progress — dispatched 2026-11-04T03:07:15Z to zai-1 as invoke-1793761635601-11706-37a7195e

Clocked in by claude-6 for zai-1 on 2026-11-04 (one Kimi task, one excursion, so the seat's
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
## Kind A7: relate (area reading)

The War Machine picks the next task partly by how it relates to the operator's current focus.
The focus changes from week to week and is measured separately, from the operator's own turns.
What does not change is which AREA of the stack a task's work belongs to. That is your question:
for each item, which area is its work FOR? A reviewer checks your row before it enters the
corpus, and a row with a weak citation is worse than `unsure`.

Areas (pick from this list; `other:<short name>` only when none fits):
- `WM`: the War Machine itself. futon2 outer and inner loops (`futon2/src/futon2/aif/`),
  `wm-contract`, the click loop, its Lean spec (`mathlib4/DarkTower/WarMachine/`), AIF policy
  and G, injuries and repair tickets (`T-repair-*`), the meta loop.
- `Giraffe`: the task map the War Machine selects over: M-giraffe, placements, cones and
  unblocks, the mission field / carpet / embedding landscape.
- `patterns`: the pattern library and its use: flexiarg, pattern retrieval and calibration,
  PSR/PUR, pattern graphs.
- `APM`: the formal proof campaign: APM problems proved in Lean, lemma seats, blueprints, registry.
- `Arxana`: futon4 Arxana and its Emacs browser views.
- `Agency`: futon3c agent coordination: bells, whistles, parks, seats, invoke jobs, delivery to
  REPL buffers, clocks.
- `memory`: memory and evidence stores and recall: minus40, futon1b evidence, recall and retrieval of memories.
- `象`: operator-turn capture and labelling (象, turn records, the reply proforma).
- `futon6`: futon6 and Rob's mfuton work, the Lean mission/math pipeline.
- `voxterm`: the voice terminal.
- `futon5`: futon5's cellular-automaton research: MetaCA, xenotypes, propagators, TPG evolution, and its reproduction notebooks.
- `futonzero`: futonzero: prelim practice, self-play and curriculum over fold deposits, the futonzero MVP and generative work.
- `business`: Hyperreal and Joe's working life: consulting, engagements, expressions of interest, career and money planning (mostly futon7 and futon5a).

Rules for this kind:
- Judge by what the item's work is FOR, from its own text, not by words it shares with an area.
  A file that mentions the War Machine in passing is not `WM`.
- Give a second area only if the file itself names that area as a consumer or parent; cite that line.
- A ticket (`T-...`) usually names its parent mission or finding; follow that one file if the
  ticket alone does not say what it is for, and cite it.
- An excursion that is a requisition record (`E-kimi-task-N`, or any `E-...` with a
  **Requisition:** line) belongs to the area of the task it carried.
- The deciding citation must be a line of prose that says what the work is for. A line that is only
  a path or a link is not a reason.

Output per item:
```json
{"stem": "<id>", "kind": "relate",
 "what": {"value": "<one sentence: what the item is for>", "cite": "path:line", "quote": "..."},
 "area": {"value": "<area from the list>", "cite": "path:line", "quote": "<the line that decides it>"} OR {"unsure": "<why>"},
 "also": {"value": "<second area>", "cite": "path:line", "quote": "..."}   (optional; omit if none),
 "confidence": "high|medium|low"}
```
## Items for this batch (20)

- `M-象-2000` (mission) — file `/home/joe/code/futon3c/holes/missions/M-象-2000.md`
- `M-arxana-roundtrip` (mission) — file `/home/joe/code/futon4/holes/missions/M-arxana-roundtrip.md`
- `M-editorial-assistant` (mission) — file `/home/joe/code/futon4/holes/missions/M-editorial-assistant.md`
- `M-vsatarcs-writer` (mission) — file `/home/joe/code/futon4/holes/missions/M-vsatarcs-writer.md`
- `M-coupling-as-constraint` (mission) — file `/home/joe/code/futon5/holes/missions/M-coupling-as-constraint.md`
- `M-eoi-outbox-management` (mission) — file `/home/joe/code/futon5a/holes/missions/M-eoi-outbox-management.md`
- `M-self-improvement-loop` (mission) — file `/home/joe/code/futon5a/holes/missions/M-self-improvement-loop.md`
- `M-stack-geometry` (mission) — file `/home/joe/code/futon5a/holes/missions/M-stack-geometry.md`
- `M-trip-journal` (mission) — file `/home/joe/code/futon5a/holes/missions/M-trip-journal.md`
- `M-war-machine-wiring` (mission) — file `/home/joe/code/futon5a/holes/missions/M-war-machine-wiring.md`
- `M-artificial-stack-exchange` (mission) — file `/home/joe/code/futon6/holes/missions/M-artificial-stack-exchange.md`
- `M-differentiable-substrate` (mission) — file `/home/joe/code/futon6/holes/missions/M-differentiable-substrate.md`
- `M-hyperreal-dictionary-planning` (mission) — file `/home/joe/code/futon6/holes/missions/M-hyperreal-dictionary-planning.md`
- `M-pheromone-field` (mission) — file `/home/joe/code/futon6/holes/missions/M-pheromone-field.md`
- `M-symbol-grounding` (mission) — file `/home/joe/code/futon6/holes/missions/M-symbol-grounding.md`
- `M-autonomous-doc-maintenance` (mission) — file `/home/joe/code/futon7/holes/M-autonomous-doc-maintenance.md`
- `M-descriptive-essay-of-the-stack` (mission) — file `/home/joe/code/futon7/holes/M-descriptive-essay-of-the-stack.md`
- `M-self-documenting-stack` (mission) — file `/home/joe/code/futon7/holes/M-self-documenting-stack.md`
- `M-signal-roll-up` (mission) — file `/home/joe/code/futon7/holes/M-signal-roll-up.md`
- `M-war-machine-aif-last-mile` (mission) — file `/home/joe/code/futon7/holes/M-war-machine-aif-last-mile.md`
