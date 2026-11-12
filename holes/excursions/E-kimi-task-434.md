# E-kimi-task-434 — read operator focus areas

**Requisition:** in-progress — dispatched 2026-11-12T23:39:26Z to 猴-1 as invoke-1794526766521-169-b73aa9ae

Clocked in by operator-focus for 猴-1 on 2026-11-12 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Operator focus reader v2

Read Joe's turns grouped by receiving seat. Return JSON only, with no explanation:
`{"answers":[{"seat":"SEAT","area":"AREA","evidence_ids":["ID"]}],"clock_answers":[{"target":"TASK-ID","area":"AREA","line":1}]}`

Return exactly one answer per represented seat. Cite only supplied evidence IDs for that seat. Choose one area from the appended A7 authority.

Judge all supplied turns for a seat together. Do not rank areas.

For every supplied clock target, classify its task excerpt too. `line` must cite a 1-based, nonblank line in that supplied excerpt. Requisition records belong to the area of the task they carried. Do not return a clock answer for a target not supplied.

The A7 authority is appended verbatim at dispatch and its SHA-256 is retained.


A7 AREA AUTHORITY (VERBATIM):
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


INPUT TURNS (JSON):
[{"evidence-id":"emacs-aaa1356582ff39262138d5d6bb15de11","at":"2026-11-12T23:36:21.751110424Z","seat":"claude-4","text":"The region was \"both\".  I tried to find a way to get the system to \"seek\" EoC and I was unsuccessful.  However, I was able to explore parameter space in order to find behaviour regions that were, arguably, exactly like the nomadic desert.  My idea was to use Active Inference to build an exploration system.  It occurs to me, I now have a better AIF explorer, so I might be able to revisit and see if I can explore better now.","captured-target":null},{"evidence-id":"emacs-c8805b6891634049b5794a80ad43d458","at":"2026-11-12T23:33:11.025581824Z","seat":"claude-4","text":"So, interesting question.  Capture: as I understand it is simply \"dead zones\" in the CA or self-replicating regions that become vertical stripes where the actual CA \"genotype\" doesn't change.  Regarding a smooth space, what I managed was to locate through a combination of methods a region where there was more complexity, what I think was likely edge of chaos behaviour.  This relates to \"growing the desert\" (possibly).  That said, I think you're right about the UC being royal science.  What I've tried to do with seeking EoC is indeed about avoiding capture or mere self-replication.","captured-target":null},{"evidence-id":"emacs-5a45fc3f219ec2b8d34629b8838c4054","at":"2026-11-12T23:28:34.643417231Z","seat":"claude-4","text":"Hi Claude, let's discuss the Deleuzian War Machine in relationship to Universal Constructors and my Meta-Cellular Automata research.","captured-target":null}]

CLOCK TARGETS (JSON):
[]