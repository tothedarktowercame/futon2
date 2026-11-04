# E-kimi-task-284 — relate batch 021: area readings for 20 WM candidate targets (A7)

**Requisition:** in-progress — dispatched 2026-11-04T03:04:02Z to zai-1 as invoke-1793761442090-11695-b1b57b17

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

- `T-repair-occ-e2c18a46bbc13f546121374c489a44780536dc14b8ac097c0deced7dc4994c49` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e2c18a46bbc13f546121374c489a44780536dc14b8ac097c0deced7dc4994c49.md`
- `T-repair-occ-e3134d742fe7308eb2d0c1cc9b0e24374e91644669366833c320578717118929` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e3134d742fe7308eb2d0c1cc9b0e24374e91644669366833c320578717118929.md`
- `T-repair-occ-e3d89579ae0e767db5858b6f42ffd4be7232557e8a2e489d1113b20b8f9447e7` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e3d89579ae0e767db5858b6f42ffd4be7232557e8a2e489d1113b20b8f9447e7.md`
- `T-repair-occ-e46f72e83344f27c9db169ab020245552eeea84e73b27afba129efeb3ed85d07` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e46f72e83344f27c9db169ab020245552eeea84e73b27afba129efeb3ed85d07.md`
- `T-repair-occ-e5a7957cab63355a8f2e1e8d00a3dcd45699d5ab178b6e550b10612fa7600351` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e5a7957cab63355a8f2e1e8d00a3dcd45699d5ab178b6e550b10612fa7600351.md`
- `T-repair-occ-e5c66a19433879faa0db646efc9fb0bb7d8bf2f6072366d6cc932b5282fa7b74` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e5c66a19433879faa0db646efc9fb0bb7d8bf2f6072366d6cc932b5282fa7b74.md`
- `T-repair-occ-e62431354b9285c3a249e7331b8582475b1dc426123a35a0432c156ba813844f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e62431354b9285c3a249e7331b8582475b1dc426123a35a0432c156ba813844f.md`
- `T-repair-occ-ea02c83c70ebbf0bea21bea410204bf6a148d7bf5c6d17e9bc078260ff884529` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ea02c83c70ebbf0bea21bea410204bf6a148d7bf5c6d17e9bc078260ff884529.md`
- `T-repair-occ-eb2cd324e7c63fa63920393e6679cfd0be6b50e696c8843acc7204b15f53c1a7` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-eb2cd324e7c63fa63920393e6679cfd0be6b50e696c8843acc7204b15f53c1a7.md`
- `T-repair-occ-eb4c2e8d0cd9951c9b028b8faca4865f666badbbbc996869445090261cd0ed84` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-eb4c2e8d0cd9951c9b028b8faca4865f666badbbbc996869445090261cd0ed84.md`
- `T-repair-occ-eca9b66b62fcd0d987587ba71f84271a375d062533b0b8e3389fffd78c4d1e4e` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-eca9b66b62fcd0d987587ba71f84271a375d062533b0b8e3389fffd78c4d1e4e.md`
- `T-repair-occ-eeb778c43d67d080cc11a7b1df477aabf5f780572b426d4e53dbf5142944e16a` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-eeb778c43d67d080cc11a7b1df477aabf5f780572b426d4e53dbf5142944e16a.md`
- `T-repair-occ-f291316c231c3fe3bc8e43c3874666c43852910e1bb335c0e10514b54f2624a1` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-f291316c231c3fe3bc8e43c3874666c43852910e1bb335c0e10514b54f2624a1.md`
- `T-repair-occ-f573bb4e28c357c64653890af0b364736f8f2a5dd67bd6efb004567253d3ecd6` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-f573bb4e28c357c64653890af0b364736f8f2a5dd67bd6efb004567253d3ecd6.md`
- `T-repair-occ-f852a41bb5c5ca8acf5d9ad9d124bea90b0665c748df34220fb5de9051021298` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-f852a41bb5c5ca8acf5d9ad9d124bea90b0665c748df34220fb5de9051021298.md`
- `T-repair-occ-f91e2dc1aa62bdd27449cf14428cee60a83818c2f6d8f989714cec7af21e2e36` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-f91e2dc1aa62bdd27449cf14428cee60a83818c2f6d8f989714cec7af21e2e36.md`
- `T-repair-occ-fa1ef846963fd8242c0f511cd18c27ff77e3d0c7193b0f8da4b62edb1a8a24f2` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-fa1ef846963fd8242c0f511cd18c27ff77e3d0c7193b0f8da4b62edb1a8a24f2.md`
- `T-repair-occ-fc53807a5675dda64b25f2823eda0ae5e6c43cf41883e119a4e3410a3dafbf55` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-fc53807a5675dda64b25f2823eda0ae5e6c43cf41883e119a4e3410a3dafbf55.md`
- `T-strategic-cascade-emits-disconnected-patterns` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-strategic-cascade-emits-disconnected-patterns.md`
- `T-watu-replay-layer` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-watu-replay-layer.md`
