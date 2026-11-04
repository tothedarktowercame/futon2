# E-kimi-task-281 — relate batch 018: area readings for 20 WM candidate targets (A7)

**Requisition:** in-progress — dispatched 2026-11-04T03:01:20Z to zai-1 as invoke-1793761280228-11686-fd431f30

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

- `T-repair-occ-7ecc0bdeb4fc9d0457f3fdbbd4a7878ab17775fa22e6b8a3c61a19c14b204fcd` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-7ecc0bdeb4fc9d0457f3fdbbd4a7878ab17775fa22e6b8a3c61a19c14b204fcd.md`
- `T-repair-occ-7f7f1ae4a79505ed379684d46c80da34d00d777bbefd07d893408e8bf653bb46` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-7f7f1ae4a79505ed379684d46c80da34d00d777bbefd07d893408e8bf653bb46.md`
- `T-repair-occ-807e29d7f940596bce913196c65828e8fa0de757ec69828a8e50db9565a3f958` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-807e29d7f940596bce913196c65828e8fa0de757ec69828a8e50db9565a3f958.md`
- `T-repair-occ-8188a6e3d1afda20216868cc0807964202e5958bbe7cae0b8d5711741340bcad` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-8188a6e3d1afda20216868cc0807964202e5958bbe7cae0b8d5711741340bcad.md`
- `T-repair-occ-836a86b20fca2fd5de0e3a7582ff65363f6e4cdb1cc1ecd75b60c845a77042e7` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-836a86b20fca2fd5de0e3a7582ff65363f6e4cdb1cc1ecd75b60c845a77042e7.md`
- `T-repair-occ-867efabc59040b5ad75507ec2ab46c58e46937200890bef32f15af022f812a98` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-867efabc59040b5ad75507ec2ab46c58e46937200890bef32f15af022f812a98.md`
- `T-repair-occ-90b44d468789e55e996453c9b8b1b79c3ca3f1c6dbc35741a1988a25a7458f8f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-90b44d468789e55e996453c9b8b1b79c3ca3f1c6dbc35741a1988a25a7458f8f.md`
- `T-repair-occ-927b47c4d684dbf20eb8aa54cf6a85e76b0d3bfef36f73088d7e0bf6bdbd2b8e` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-927b47c4d684dbf20eb8aa54cf6a85e76b0d3bfef36f73088d7e0bf6bdbd2b8e.md`
- `T-repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc.md`
- `T-repair-occ-950b5e58640d272150d96d6e2ad8827f89c4cac4dc4097c1cbe9c74d1cd09205` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-950b5e58640d272150d96d6e2ad8827f89c4cac4dc4097c1cbe9c74d1cd09205.md`
- `T-repair-occ-97ccfac3f423511499bad1f411993bad4b192fff8bd53990e1c043986603915f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-97ccfac3f423511499bad1f411993bad4b192fff8bd53990e1c043986603915f.md`
- `T-repair-occ-9a0d6042888f6937f9805a80676ad13cf5e72762f8b4815b4ef9d869468f1d3d` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-9a0d6042888f6937f9805a80676ad13cf5e72762f8b4815b4ef9d869468f1d3d.md`
- `T-repair-occ-9ad20018fb86ffb06003b76e6b5674b6f3ae64bb8d0c59b75c8be9803a253829` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-9ad20018fb86ffb06003b76e6b5674b6f3ae64bb8d0c59b75c8be9803a253829.md`
- `T-repair-occ-9b6c9871d59bde364a23f9c346ddc708e2f68f4a026d9afe0aac6c9d538f087d` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-9b6c9871d59bde364a23f9c346ddc708e2f68f4a026d9afe0aac6c9d538f087d.md`
- `T-repair-occ-a1f9953c35ee4ed8a16473911f2bafeaa54b2e8609d3da40fe8e32a1a226423f` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-a1f9953c35ee4ed8a16473911f2bafeaa54b2e8609d3da40fe8e32a1a226423f.md`
- `T-repair-occ-a3438ff06dcbdfc85f021c305f089a974ef545762a481fba7a3d329389556782` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-a3438ff06dcbdfc85f021c305f089a974ef545762a481fba7a3d329389556782.md`
- `T-repair-occ-a70bb6aab60c8d37d8562a669b30c999f12a67137828631950d1fa3d49f91212` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-a70bb6aab60c8d37d8562a669b30c999f12a67137828631950d1fa3d49f91212.md`
- `T-repair-occ-a7641edf8f7951e20dde66669e0aa7a8dc630dc7f3d0a5c20d0eeb4717c389ad` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-a7641edf8f7951e20dde66669e0aa7a8dc630dc7f3d0a5c20d0eeb4717c389ad.md`
- `T-repair-occ-a784a2689057e5869f86b6edb60720a975f989e9f706e81e247afe4d774380d6` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-a784a2689057e5869f86b6edb60720a975f989e9f706e81e247afe4d774380d6.md`
- `T-repair-occ-a88c7ae74d1f758a70a47e456140cd6121c9d27dac6a852c601bf6036161c78e` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-a88c7ae74d1f758a70a47e456140cd6121c9d27dac6a852c601bf6036161c78e.md`
