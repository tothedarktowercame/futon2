# E-kimi-task-278 — relate batch 015: area readings for 20 WM candidate targets (A7)

**Requisition:** completed — 2026-11-04T03:01:09Z, job invoke-1793761048048-11669-5556da97, state done

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

- `T-repair-occ-1aa2ba9e8f7100d1df8d3d4f27f43222e5ec1be0fc7db384bce35fb0758504b0` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-1aa2ba9e8f7100d1df8d3d4f27f43222e5ec1be0fc7db384bce35fb0758504b0.md`
- `T-repair-occ-207fb52fd8d64a6c62495a433bee564b8ac9e2a06ab5a0c61f95222ac50d1b9f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-207fb52fd8d64a6c62495a433bee564b8ac9e2a06ab5a0c61f95222ac50d1b9f.md`
- `T-repair-occ-22805298d8d13543063b2c03c062d569461d5d74c3acc3f844a2ef16963da248` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-22805298d8d13543063b2c03c062d569461d5d74c3acc3f844a2ef16963da248.md`
- `T-repair-occ-25dc2faf0ef641860133dc7c67b828163c3415d7ff980cd04861f62669920a38` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-25dc2faf0ef641860133dc7c67b828163c3415d7ff980cd04861f62669920a38.md`
- `T-repair-occ-2613ce611247ef5502fa693d1d7d8a0fbfcd85214a91d2ba2cd7fd18cc34a8ef` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-2613ce611247ef5502fa693d1d7d8a0fbfcd85214a91d2ba2cd7fd18cc34a8ef.md`
- `T-repair-occ-28edd6fe7212afcd432bfcbd452bf7320a7e27e0beb004dd02e24820e1f41290` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-28edd6fe7212afcd432bfcbd452bf7320a7e27e0beb004dd02e24820e1f41290.md`
- `T-repair-occ-2903c37fab429539ab6c907c2b3995a457d554ee08ec211bbb8560af31ced1e2` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-2903c37fab429539ab6c907c2b3995a457d554ee08ec211bbb8560af31ced1e2.md`
- `T-repair-occ-298af66e3cbbbbd4ee0c7e7fad6395ea08c2d725168255aa2678f26fc1f1b322` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-298af66e3cbbbbd4ee0c7e7fad6395ea08c2d725168255aa2678f26fc1f1b322.md`
- `T-repair-occ-29b5ddce251554d1fb1c9e6dc3e6ada925746e49890bbd72fa32671213818aa7` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-29b5ddce251554d1fb1c9e6dc3e6ada925746e49890bbd72fa32671213818aa7.md`
- `T-repair-occ-2aee52c72aa505a77d8a1cf6f7c2c6ad9a2d19bfb162c8c5706a0ad9e0e5643c` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-2aee52c72aa505a77d8a1cf6f7c2c6ad9a2d19bfb162c8c5706a0ad9e0e5643c.md`
- `T-repair-occ-2ce2238e9feb45e23a1a674a3173634306a4c03176f8df7117bb2091378178a5` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-2ce2238e9feb45e23a1a674a3173634306a4c03176f8df7117bb2091378178a5.md`
- `T-repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179.md`
- `T-repair-occ-2ff102a09d24d55886bd0e015afd6651115974b0b28afb5b0ed9f7b9d15b7521` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-2ff102a09d24d55886bd0e015afd6651115974b0b28afb5b0ed9f7b9d15b7521.md`
- `T-repair-occ-301d584d77e38b51ccdfc1aec0252bf7435e29e66c0099148d830680106eb6c3` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-301d584d77e38b51ccdfc1aec0252bf7435e29e66c0099148d830680106eb6c3.md`
- `T-repair-occ-31a74d015cada58e51fa252c043c1a44217ca91b3fca3d9ec7a28a0b11108d85` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-31a74d015cada58e51fa252c043c1a44217ca91b3fca3d9ec7a28a0b11108d85.md`
- `T-repair-occ-32582914c6a6750ae369e43a458c6e9931218b7011bdc8fdb2d7b7c2d77c2285` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-32582914c6a6750ae369e43a458c6e9931218b7011bdc8fdb2d7b7c2d77c2285.md`
- `T-repair-occ-357a436bf00c4265014d45a69ed2cdebd6a643e2f90e56be855daaa40cc16d34` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-357a436bf00c4265014d45a69ed2cdebd6a643e2f90e56be855daaa40cc16d34.md`
- `T-repair-occ-36ee8d409152733880e7b7ba73fb9a2deed26e02d759bfaf97722b5541348fb7` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-36ee8d409152733880e7b7ba73fb9a2deed26e02d759bfaf97722b5541348fb7.md`
- `T-repair-occ-3b52830f57de04083d5e1c67f66b434cb4b306a1b3b912cdabaeed192cfb6d96` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-3b52830f57de04083d5e1c67f66b434cb4b306a1b3b912cdabaeed192cfb6d96.md`
- `T-repair-occ-3bf36f0ae2acbc42ed0195ce636eaa78f4a241e9e56a3a86006ab0dc81295506` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-3bf36f0ae2acbc42ed0195ce636eaa78f4a241e9e56a3a86006ab0dc81295506.md`
