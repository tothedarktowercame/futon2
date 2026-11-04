# E-kimi-task-295 — relate final batch 1: area readings for unplaced WM candidates (A7)

**Requisition:** in-progress — dispatched 2026-11-04T03:20:47Z to zai-2 as invoke-1793762447925-11748-ac90f932

Clocked in by claude-6 for zai-2 on 2026-11-04 (one Kimi task, one excursion, so the seat's
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
## Items for this batch (18)

- `E-jax-demonstrators` (excursion) — file `/home/joe/code/futon5a/holes/excursions/E-jax-demonstrators.md`
- `E-loss-function-shape` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-loss-function-shape.md`
- `E-pace-layered-tower` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-pace-layered-tower.md`
- `E-park-delivery-losses` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-park-delivery-losses.md`
- `T-arxana-direct-reads` (ticket) — file `/home/joe/code/futon4/holes/tickets/T-arxana-direct-reads.md`
- `T-fixture-becomes-registry-26082026` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-fixture-becomes-registry-26082026.md`
- `T-jvm-provenance-before-multi-jvm` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-jvm-provenance-before-multi-jvm.md`
- `T-repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-002-resume-protocol-violation` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-002-resume-protocol-violation.md`
- `T-repair-occ-0228a166e753669832ab6342b4896ad310b86284f3793e8e876201bc4c658801` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-0228a166e753669832ab6342b4896ad310b86284f3793e8e876201bc4c658801.md`
- `T-repair-occ-c4f34d0d2b676562a3c76bf7f7292bb73424d271aa0457c5a9fb21977c5d0201` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c4f34d0d2b676562a3c76bf7f7292bb73424d271aa0457c5a9fb21977c5d0201.md`
- `T-repair-occ-d2f65574326c6cf91328d4061f10e695abdb63444df18c2fa78a8ead4dd54812` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d2f65574326c6cf91328d4061f10e695abdb63444df18c2fa78a8ead4dd54812.md`
- `T-repair-occ-ee9381591717eccb1f3a6c537943b3cd1d7f50131aa440bb3d72f3bddd51aa49` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ee9381591717eccb1f3a6c537943b3cd1d7f50131aa440bb3d72f3bddd51aa49.md`
- `T-strategic-cascade-emits-disconnected-patterns` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-strategic-cascade-emits-disconnected-patterns.md`
- `M-patterns-done-right` (mission) — file `/home/joe/code/futon0/holes/missions/M-patterns-done-right.md`
- `E-distributed-formalisation` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-distributed-formalisation.md`
- `E-fold-embed-pipeline` (excursion) — file `/home/joe/code/futon2/holes/E-fold-embed-pipeline.md`
- `E-live-loop-3` (excursion) — file `/home/joe/code/futon2/holes/E-live-loop-3.md`
- `T-outer-target-g-policy-prior` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-outer-target-g-policy-prior.md`
