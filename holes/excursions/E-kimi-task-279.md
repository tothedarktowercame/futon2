# E-kimi-task-279 — relate batch 016: area readings for 20 WM candidate targets (A7)

**Requisition:** completed — 2026-11-04T03:01:19Z, job invoke-1793761119256-11678-38b66535, state done

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
## Items for this batch (20)

- `T-repair-occ-3d71c1a357f40eb2bd4df4063fda1b963e20a5ea189db7356fc70bb4add68812` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-3d71c1a357f40eb2bd4df4063fda1b963e20a5ea189db7356fc70bb4add68812.md`
- `T-repair-occ-3e36e894edb0357df57476b01bef9a9a0eb4a77e38eee3838534c8bfb413877c` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-3e36e894edb0357df57476b01bef9a9a0eb4a77e38eee3838534c8bfb413877c.md`
- `T-repair-occ-3f2b96be3423d070d8175a17447153c8b1128cd26991ef01cb4c5fa965e87fdb` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-3f2b96be3423d070d8175a17447153c8b1128cd26991ef01cb4c5fa965e87fdb.md`
- `T-repair-occ-3fe75d581a8c962ab29a199c63c147caa48f35f9ab30f6ef1a42810a78e06cb4` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-3fe75d581a8c962ab29a199c63c147caa48f35f9ab30f6ef1a42810a78e06cb4.md`
- `T-repair-occ-400245f2a8a38f3adc2dafa2074583ea3841be514f5b9951d2a9684481a932ed` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-400245f2a8a38f3adc2dafa2074583ea3841be514f5b9951d2a9684481a932ed.md`
- `T-repair-occ-402bbfc68fd00fe028e9a3148474df20115581d5d3bb7aa889064fd01077db52` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-402bbfc68fd00fe028e9a3148474df20115581d5d3bb7aa889064fd01077db52.md`
- `T-repair-occ-4088fdd975660fb47b5193ad881aff5e813055f80868cd8128c98b50d2daa50f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-4088fdd975660fb47b5193ad881aff5e813055f80868cd8128c98b50d2daa50f.md`
- `T-repair-occ-40a2d58f136bf0227a8d51d2ccfdfdc8664b1152602526f42e74108a7a790683` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-40a2d58f136bf0227a8d51d2ccfdfdc8664b1152602526f42e74108a7a790683.md`
- `T-repair-occ-4179c6a6bdf3098e585e02004b569ec08e48cd9cb1149a519144ffe0a57563b0` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-4179c6a6bdf3098e585e02004b569ec08e48cd9cb1149a519144ffe0a57563b0.md`
- `T-repair-occ-41bfa0a71f53bbd349dfd32a9978f4a7ee787ad2527f75ae4283c2385f506de0` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-41bfa0a71f53bbd349dfd32a9978f4a7ee787ad2527f75ae4283c2385f506de0.md`
- `T-repair-occ-41cfef2fe832a21b8c1bd6795353d5948e1063d4b8bc04c7566f30b2df014399` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-41cfef2fe832a21b8c1bd6795353d5948e1063d4b8bc04c7566f30b2df014399.md`
- `T-repair-occ-458cfe76d309bd5c86517b4ef5ff8730e19cd1f0f53153087b05dae9d0b302dd` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-458cfe76d309bd5c86517b4ef5ff8730e19cd1f0f53153087b05dae9d0b302dd.md`
- `T-repair-occ-487ca3f2205bf084f5d9bbb6b753cdc3908f106e8ff3aec60243a1bff7c34581` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-487ca3f2205bf084f5d9bbb6b753cdc3908f106e8ff3aec60243a1bff7c34581.md`
- `T-repair-occ-4bc74d192b2e6d43514825b62f5df02c28f3d75abd8e091dfd54af30f5a44f8f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-4bc74d192b2e6d43514825b62f5df02c28f3d75abd8e091dfd54af30f5a44f8f.md`
- `T-repair-occ-51b7af852035373d7581c7487f43bb154bc250c44718ec408429f95f2ab03880` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-51b7af852035373d7581c7487f43bb154bc250c44718ec408429f95f2ab03880.md`
- `T-repair-occ-5643bd66e188fed501ef1b93c4296fcd9957168ad24131656d2c822b197e5ed7` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-5643bd66e188fed501ef1b93c4296fcd9957168ad24131656d2c822b197e5ed7.md`
- `T-repair-occ-5691caac1c58ec374091742f3d95b41338669e917c45f83d3049dd84a9637172` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-5691caac1c58ec374091742f3d95b41338669e917c45f83d3049dd84a9637172.md`
- `T-repair-occ-57ad4ad279ad0336033008c84371e78792b849d58c477e96ecdc0ce736c2825b` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-57ad4ad279ad0336033008c84371e78792b849d58c477e96ecdc0ce736c2825b.md`
- `T-repair-occ-590c884c27a1b73f24bd632823fa03bfa3315eac16f4ef9ad815f9f98b347a58` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-590c884c27a1b73f24bd632823fa03bfa3315eac16f4ef9ad815f9f98b347a58.md`
- `T-repair-occ-591a0cd62b5bf96f1137679dd68dfbff66bbf47ec352443f3b795a8e2fec29e5` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-591a0cd62b5bf96f1137679dd68dfbff66bbf47ec352443f3b795a8e2fec29e5.md`
