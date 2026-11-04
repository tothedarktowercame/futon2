# E-kimi-task-277 — relate batch 014: area readings for 20 WM candidate targets (A7)

**Requisition:** completed — 2026-11-04T02:59:51Z, job invoke-1793760983841-11665-3cebdff4, state done

Clocked in by claude-6 for zai-3 on 2026-11-04 (one Kimi task, one excursion, so the seat's
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

- `T-q2-q4-q8-recomputable-on-every-run` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-q2-q4-q8-recomputable-on-every-run.md`
- `T-repair-ea1-4c9c589db930a12238afcf87908511fd6e35ebc908f2a571d090d988a0342f5c--attempt-001-answer-holes-unresolved` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-ea1-4c9c589db930a12238afcf87908511fd6e35ebc908f2a571d090d988a0342f5c--attempt-001-answer-holes-unresolved.md`
- `T-repair-ea1-52835e7be09440de82a95b9022b73b9f849282609a3b8756a58b56adbd65094e--attempt-001-answer-holes-unresolved` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-ea1-52835e7be09440de82a95b9022b73b9f849282609a3b8756a58b56adbd65094e--attempt-001-answer-holes-unresolved.md`
- `T-repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-001-hot-load-failed` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-001-hot-load-failed.md`
- `T-repair-ea1-a6cf24bbaa8845feee13d00b6a1f34ac67de73c54d5993a9460600c2f70749c5--attempt-001-hot-load-failed` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-ea1-a6cf24bbaa8845feee13d00b6a1f34ac67de73c54d5993a9460600c2f70749c5--attempt-001-hot-load-failed.md`
- `T-repair-ea1-e39c50434fe304affa571228e724dea92d7433a474aa24aa51ed42c5195d42d3--attempt-001-open-cascade-author-answer-invalid` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-ea1-e39c50434fe304affa571228e724dea92d7433a474aa24aa51ed42c5195d42d3--attempt-001-open-cascade-author-answer-invalid.md`
- `T-repair-occ-0143d69b8c93fb8b658a6ec1ce6e355f6ac9e837e3f323390d3a0ee22633a282` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-0143d69b8c93fb8b658a6ec1ce6e355f6ac9e837e3f323390d3a0ee22633a282.md`
- `T-repair-occ-02dc6384e2f7b77f2956203b925e2c185586f522fa4d4a785a5dc814f410890b` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-02dc6384e2f7b77f2956203b925e2c185586f522fa4d4a785a5dc814f410890b.md`
- `T-repair-occ-036463620c0c032c9e46aa44b6a6d6b35ed3e0ceb27dc58030f07d8fc2e6c747` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-036463620c0c032c9e46aa44b6a6d6b35ed3e0ceb27dc58030f07d8fc2e6c747.md`
- `T-repair-occ-04e267c8e31f60390f600a6d24056fd9078bc6caa3d234568b9c135e5db48fe9` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-04e267c8e31f60390f600a6d24056fd9078bc6caa3d234568b9c135e5db48fe9.md`
- `T-repair-occ-0552e1353cb6075be1e50824603b0b5898590527f79089df09a212410aaf6483` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-0552e1353cb6075be1e50824603b0b5898590527f79089df09a212410aaf6483.md`
- `T-repair-occ-0848c99bd079e68f877bea37b424ae56abf3d51794f2e61a269570ba5992e3c0` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-0848c99bd079e68f877bea37b424ae56abf3d51794f2e61a269570ba5992e3c0.md`
- `T-repair-occ-095254060d1cbe5b0e8be618592b7140709be23fe7b9108aebf8b1f7167a66f6` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-095254060d1cbe5b0e8be618592b7140709be23fe7b9108aebf8b1f7167a66f6.md`
- `T-repair-occ-10421fbe344701047590fcdbe24684d554aae59c3cb385ca06145f94bbb914f3` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-10421fbe344701047590fcdbe24684d554aae59c3cb385ca06145f94bbb914f3.md`
- `T-repair-occ-1178940a78ca477095a7576fdf9bf2da78797cd602aa3ff01330105b2d8a49ac` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-1178940a78ca477095a7576fdf9bf2da78797cd602aa3ff01330105b2d8a49ac.md`
- `T-repair-occ-11f5a87b1c9068795c1ea177c4d660648bbf681d17fae2ea6172e8040b172ed5` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-11f5a87b1c9068795c1ea177c4d660648bbf681d17fae2ea6172e8040b172ed5.md`
- `T-repair-occ-140c55d90ae88d905c7477391ba0e863be72c0189948c5da685b00dfe72b5b11` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-140c55d90ae88d905c7477391ba0e863be72c0189948c5da685b00dfe72b5b11.md`
- `T-repair-occ-18d00f51d4b31ac6d1aeb99b90837383f171e725225f7b7c22d93a4c24372689` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-18d00f51d4b31ac6d1aeb99b90837383f171e725225f7b7c22d93a4c24372689.md`
- `T-repair-occ-18f0dd165474f7d6b97698cb8430cc44fdf28f234f5ac0bba0f99355a47bd8aa` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-18f0dd165474f7d6b97698cb8430cc44fdf28f234f5ac0bba0f99355a47bd8aa.md`
- `T-repair-occ-1a606e1f7723cb39e56951d81aaf61fb2386c3e37aa4db1472929189509debcd` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-1a606e1f7723cb39e56951d81aaf61fb2386c3e37aa4db1472929189509debcd.md`
