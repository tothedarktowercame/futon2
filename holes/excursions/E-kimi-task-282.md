# E-kimi-task-282 — relate batch 019: area readings for 20 WM candidate targets (A7)

**Requisition:** in-progress — dispatched 2026-11-04T03:02:25Z to zai-2 as invoke-1793761345255-11690-8d04f87a

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

- `T-repair-occ-a9c0c16689178b7bed8c2da37a7712cc0915425717d7d116eadd411407ebf69d` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-a9c0c16689178b7bed8c2da37a7712cc0915425717d7d116eadd411407ebf69d.md`
- `T-repair-occ-ab118bb811535ae9cb734f8c9f6cb54ff2ee7c76935df65c873a5f05817c421c` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ab118bb811535ae9cb734f8c9f6cb54ff2ee7c76935df65c873a5f05817c421c.md`
- `T-repair-occ-abd57b292fe21797a03b4bc5ba38601d89f27eac075dee9a0140919f4830d7f5` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-abd57b292fe21797a03b4bc5ba38601d89f27eac075dee9a0140919f4830d7f5.md`
- `T-repair-occ-ac098bf4971f5535b7ed4a6698b261871b632161955a6fd973bb31d1469f703e` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ac098bf4971f5535b7ed4a6698b261871b632161955a6fd973bb31d1469f703e.md`
- `T-repair-occ-acf9d3f9fc1ce9e3d025bcfb10ce672cfd4911336574a4fb53974ce78b6a7d60` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-acf9d3f9fc1ce9e3d025bcfb10ce672cfd4911336574a4fb53974ce78b6a7d60.md`
- `T-repair-occ-ad16e2c2e10f83412628d1eec8e222018afa399fba64016124e32f3a812a3214` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ad16e2c2e10f83412628d1eec8e222018afa399fba64016124e32f3a812a3214.md`
- `T-repair-occ-ad7f485f7d8eac19a0ef2656d5826ad41e0b73a2d18a57c9e67a6215ad3affae` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ad7f485f7d8eac19a0ef2656d5826ad41e0b73a2d18a57c9e67a6215ad3affae.md`
- `T-repair-occ-ae4e3453b2033f03013d8f1c0a03dae30f6a4a2f81c3b247dad0f53c22c59eda` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ae4e3453b2033f03013d8f1c0a03dae30f6a4a2f81c3b247dad0f53c22c59eda.md`
- `T-repair-occ-b10d5c8c1b31bad68f4e0e05c341136547472d61f27fe4a25bf704232e00f90a` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-b10d5c8c1b31bad68f4e0e05c341136547472d61f27fe4a25bf704232e00f90a.md`
- `T-repair-occ-b1551ee2ec2dfee045fec86c9a5d9177b352b64add3db1cd8a7625d63691b29d` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-b1551ee2ec2dfee045fec86c9a5d9177b352b64add3db1cd8a7625d63691b29d.md`
- `T-repair-occ-b50ba4348eaa71ecc4013109a049a4ef32ad2c0dbc468d08b8f189e866f677fd` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-b50ba4348eaa71ecc4013109a049a4ef32ad2c0dbc468d08b8f189e866f677fd.md`
- `T-repair-occ-b69768de20c53f0e8a0b5e461aca49031e9bd40c138e52dee5b1b78e75745a97` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-b69768de20c53f0e8a0b5e461aca49031e9bd40c138e52dee5b1b78e75745a97.md`
- `T-repair-occ-b8bf54762b072cf5131180a27e5a50da5e9ac54ade2724b69b1d013c8454df7a` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-b8bf54762b072cf5131180a27e5a50da5e9ac54ade2724b69b1d013c8454df7a.md`
- `T-repair-occ-bdc2e45f7334d62b43d040e9e08c307d251d85b54a888419a5c46962e3d8c5fa` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-bdc2e45f7334d62b43d040e9e08c307d251d85b54a888419a5c46962e3d8c5fa.md`
- `T-repair-occ-be076a458ed64481117508045fa0dcdb5632b5c9eba71f365e49916d2a94b04f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-be076a458ed64481117508045fa0dcdb5632b5c9eba71f365e49916d2a94b04f.md`
- `T-repair-occ-c0506402970aeb4a0a50e1d895d7829c8cf6a30d9361431694d48499f8e98416` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c0506402970aeb4a0a50e1d895d7829c8cf6a30d9361431694d48499f8e98416.md`
- `T-repair-occ-c109cb75d893ca34b6fd293def321dd506d9ab8627974dadfe3008df4bcd4925` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c109cb75d893ca34b6fd293def321dd506d9ab8627974dadfe3008df4bcd4925.md`
- `T-repair-occ-c15da9b67a118021479198ff97026a98c2cb61508ae21224a32ec8b03c7cf849` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c15da9b67a118021479198ff97026a98c2cb61508ae21224a32ec8b03c7cf849.md`
- `T-repair-occ-c4457ca49b8bcb7fa38bc1c788eb6c2c0f40a43378fa7eb3675f1db57ee02070` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c4457ca49b8bcb7fa38bc1c788eb6c2c0f40a43378fa7eb3675f1db57ee02070.md`
- `T-repair-occ-c6325db5decc76fff9c53b4c104bc64e0155d6fab68216814719be220dcc4776` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c6325db5decc76fff9c53b4c104bc64e0155d6fab68216814719be220dcc4776.md`
