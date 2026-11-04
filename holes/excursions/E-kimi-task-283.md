# E-kimi-task-283 — relate batch 020: area readings for 20 WM candidate targets (A7)

**Requisition:** in-progress — dispatched 2026-11-04T03:03:00Z to zai-3 as invoke-1793761380936-11691-99b54e11

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

- `T-repair-occ-c86a4898faee019ef5558b6f2ab7a7ec0c374e56c82327d33b9241ee28c33ba8` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c86a4898faee019ef5558b6f2ab7a7ec0c374e56c82327d33b9241ee28c33ba8.md`
- `T-repair-occ-c8da1b1833592a40088c17f4bc64aa1a35fb818d713c3fc6de23d31f78630887` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c8da1b1833592a40088c17f4bc64aa1a35fb818d713c3fc6de23d31f78630887.md`
- `T-repair-occ-ca856d27bffcdd8bf0326942b28cf40a578e0e0c615cf7fe6d0ca183faa2889e` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ca856d27bffcdd8bf0326942b28cf40a578e0e0c615cf7fe6d0ca183faa2889e.md`
- `T-repair-occ-ccdfe7a612a3d5072651f7e77a259728d44a094dd01c897ad2aebe8c16904841` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ccdfe7a612a3d5072651f7e77a259728d44a094dd01c897ad2aebe8c16904841.md`
- `T-repair-occ-cedd3d4421cb970ecac84216293f1e359a77475031ad9f438c734746a926f958` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-cedd3d4421cb970ecac84216293f1e359a77475031ad9f438c734746a926f958.md`
- `T-repair-occ-cf59a72bac62057d4857c8e0588780a6e201275ff224e967316e1d8e3a8f9a41` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-cf59a72bac62057d4857c8e0588780a6e201275ff224e967316e1d8e3a8f9a41.md`
- `T-repair-occ-d16904991c7b8b5588112d862800e10a8bae0d6f3d77914946d56920ec187131` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d16904991c7b8b5588112d862800e10a8bae0d6f3d77914946d56920ec187131.md`
- `T-repair-occ-d265c507a643c3a1bdca0297451a09698e107a78d9ab0112ff04fbb7512391ce` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d265c507a643c3a1bdca0297451a09698e107a78d9ab0112ff04fbb7512391ce.md`
- `T-repair-occ-d276a49116c14bd53fd025b0e320c7cb5bb6db344890caa334d11addd0fb5ba9` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d276a49116c14bd53fd025b0e320c7cb5bb6db344890caa334d11addd0fb5ba9.md`
- `T-repair-occ-d2e958b3d0d71a851e2e5c15a5d1400ed4fe4cfdce41d6cedaed128caee9c211` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d2e958b3d0d71a851e2e5c15a5d1400ed4fe4cfdce41d6cedaed128caee9c211.md`
- `T-repair-occ-d4433c0aed98581f3c79efa24db7e1a24b2ff65b480f04ebd801b68250f12824` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d4433c0aed98581f3c79efa24db7e1a24b2ff65b480f04ebd801b68250f12824.md`
- `T-repair-occ-d4d69c06c78c751c19932818dc910dab38702f3c2390e96a8feaaa17b73ec233` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d4d69c06c78c751c19932818dc910dab38702f3c2390e96a8feaaa17b73ec233.md`
- `T-repair-occ-d7b9455b38aeeaee22030c79db02690de8c444612826547c7589d0e6e6c56c02` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d7b9455b38aeeaee22030c79db02690de8c444612826547c7589d0e6e6c56c02.md`
- `T-repair-occ-d9ed3e32d6297fae60ca2cbcef9649b5208398a99bdcd3c1e21c50cdb88692b0` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d9ed3e32d6297fae60ca2cbcef9649b5208398a99bdcd3c1e21c50cdb88692b0.md`
- `T-repair-occ-dc843e159f38d9cdee8388b0d32214725fa44a595069e5791af8837ef398d760` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-dc843e159f38d9cdee8388b0d32214725fa44a595069e5791af8837ef398d760.md`
- `T-repair-occ-dd1e3edbbf147ef9be5152db4a2ae6cead43de44b6cf8ab8fb8e94cf5c6fff22` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-dd1e3edbbf147ef9be5152db4a2ae6cead43de44b6cf8ab8fb8e94cf5c6fff22.md`
- `T-repair-occ-dd7c04a30d87dd8d3293f3f2257ce0158f4ef993a63fef01d9aef06013720b5c` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-dd7c04a30d87dd8d3293f3f2257ce0158f4ef993a63fef01d9aef06013720b5c.md`
- `T-repair-occ-de59f63cd5030d088334dc321e4567d5f68fca24eecaf962f4f9fa35bf9546d2` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-de59f63cd5030d088334dc321e4567d5f68fca24eecaf962f4f9fa35bf9546d2.md`
- `T-repair-occ-e0b7d987118bcafd40f9a87e9dd37ad88b0d670533ca6e973d0a0e7c565996dc` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e0b7d987118bcafd40f9a87e9dd37ad88b0d670533ca6e973d0a0e7c565996dc.md`
- `T-repair-occ-e0fa2c234da21bb5424000baaf9114873acb902913877109f96a2a7a260496bd` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-e0fa2c234da21bb5424000baaf9114873acb902913877109f96a2a7a260496bd.md`
