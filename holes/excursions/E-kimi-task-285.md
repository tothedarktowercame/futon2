# E-kimi-task-285 — relate batch 022: area readings for 20 WM candidate targets (A7)

**Requisition:** completed — 2026-11-04T03:07:53Z, job invoke-1793761506355-11698-ac172ec4, state done

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

- `T-wm-excessive-guardrails-19092026` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-wm-excessive-guardrails-19092026.md`
- `T-wm-turns-are-not-operator-turns` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-wm-turns-are-not-operator-turns.md`
- `T-wm-wrong-corpus-26082026` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-wm-wrong-corpus-26082026.md`
- `T-wm01-runtime-error-reference-19092026` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-wm01-runtime-error-reference-19092026.md`
- `T-zai-chat-transient-timeout` (ticket) — file `/home/joe/code/futon3c/holes/tickets/T-zai-chat-transient-timeout.md`
- `M-usage-hacking` (mission) — file `/home/joe/code/futon0/holes/missions/M-usage-hacking.md`
- `M-a4a-structure-node` (mission) — file `/home/joe/code/futon2/holes/M-a4a-structure-node.md`
- `M-fold-ansatz` (mission) — file `/home/joe/code/futon2/holes/M-fold-ansatz.md`
- `M-run-produces-its-own-brief` (mission) — file `/home/joe/code/futon2/holes/missions/M-run-produces-its-own-brief.md`
- `M-bounded-disposition` (mission) — file `/home/joe/code/futon3c/holes/missions/M-bounded-disposition.md`
- `M-diagramprover` (mission) — file `/home/joe/code/futon3c/holes/missions/M-diagramprover.md`
- `M-librarian` (mission) — file `/home/joe/code/futon4/holes/missions/M-librarian.md`
- `M-or-training-as-learning-system` (mission) — file `/home/joe/code/futon4/holes/missions/M-or-training-as-learning-system.md`
- `M-webarxana` (mission) — file `/home/joe/code/futon4/holes/missions/M-webarxana.md`
- `M-formal-patterns` (mission) — file `/home/joe/code/futon5/holes/M-formal-patterns.md`
- `M-a-sorry-enterprise` (mission) — file `/home/joe/code/futon5a/holes/missions/M-a-sorry-enterprise.md`
- `M-hypergraph-operator` (mission) — file `/home/joe/code/futon5a/holes/missions/M-hypergraph-operator.md`
- `M-superpod-mark3` (mission) — file `/home/joe/code/futon6/holes/missions/M-superpod-mark3.md`
- `M-stack-essay-code-alignment` (mission) — file `/home/joe/code/futon7/holes/M-stack-essay-code-alignment.md`
- `M-stack-morphogenetic-rewrite` (mission) — file `/home/joe/code/futon7/holes/M-stack-morphogenetic-rewrite.md`
