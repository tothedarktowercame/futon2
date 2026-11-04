# E-kimi-task-262 — relate pilot: 20 WM candidate targets to the retained focus (A7)

**Requisition:** in-progress — dispatched 2026-11-04T02:33:59Z to zai-1 as invoke-1793759639873-11583-3b480a53

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
## Kind A7: relate

The War Machine picks the next task by how each candidate relates to the operator's
current focus. Your question for each item: how does this item relate to that focus? You are
reading, not deciding: a reviewer checks your row before it enters the relation corpus, and a
row with a weak citation is worse than `unsure`.

The focus (retained in `futon2/resources/wm/focus/commit-facets-v1.json`): two facets, joined
by a declared same-focus edge.
- **WM**: the War Machine itself. The futon2 outer and inner loops (`futon2/src/futon2/aif/`,
  `wm-contract`, the click loop, its Lean spec `mathlib4/DarkTower/WarMachine/`), AIF policy
  and G, selection, injuries and repair, the meta loop.
- **APM**: the formal proof campaign (APM problems proved in Lean, its seats, registry, blueprints).

Relations (the only three; Lean `CTauClassPreference.lean`, `TargetRelation`):
- `focus`: the item's own work IS WM or APM work: finishing it changes the War Machine or the
  APM campaign directly.
- `associated`: the item is not itself WM/APM work, but the file shows that WM/APM work uses it, depends on it, or
  is blocked by it, or it names a WM/APM mission as its parent or consumer. Cite the line that shows the link.
- `useful-elsewhere`: real work for another part of the stack (memory, Emacs/Arxana, voxterm,
  futon6, money, papers, infrastructure...) with no stated link to WM/APM.

Rules for this kind:
- Judge by what the item's work is FOR, from its own text. A file that mentions the War
  Machine in passing is not `focus`. Shared vocabulary (AIF, G, receipt) is not a link.
- `associated` needs a cited line naming the WM/APM thing it serves or feeds. No such line means
  `useful-elsewhere`, or `unsure` if you suspect a link you cannot cite.
- A ticket (`T-...`) usually names its parent mission or finding; follow that one file if the
  ticket alone does not say what it is for, and cite it.
- An excursion that is a requisition record (`E-kimi-task-N`, `E-...` with a **Requisition:**
  line) relates as the task it carried.

Output per item:
```json
{"stem": "<id>", "kind": "relate",
 "what": {"value": "<one sentence: what the item is for>", "cite": "path:line", "quote": "..."},
 "facet": {"value": "WM|APM|other:<short name>", "cite": "path:line", "quote": "..."},
 "relation": {"value": "focus|associated|useful-elsewhere", "cite": "path:line", "quote": "<the line that decides it>"} OR {"unsure": "<why>"},
 "link": {"value": "<the WM/APM item or path it serves>", "cite": "path:line", "quote": "..."}   (REQUIRED when relation is associated; omit otherwise),
 "confidence": "high|medium|low"}
```
## Items for this batch (20)

- `M-war-machine-aif-completion` (mission) — file `/home/joe/code/futon7/holes/M-war-machine-aif-completion.md`
- `T-arxana-direct-reads` (ticket) — file `/home/joe/code/futon4/holes/tickets/T-arxana-direct-reads.md`
- `M-patterns-done-right` (mission) — file `/home/joe/code/futon0/holes/missions/M-patterns-done-right.md`
- `M-aif-ants-port` (mission) — file `/home/joe/code/futon2/holes/M-aif-ants-port.md`
- `M-essay-corpus-substrate` (mission) — file `/home/joe/code/futon4/holes/missions/M-essay-corpus-substrate.md`
- `M-state-snapshot-witness` (mission) — file `/home/joe/code/futon3c/holes/missions/M-state-snapshot-witness.md`
- `M-intent-curvature` (mission) — file `/home/joe/code/futon3c/holes/missions/M-intent-curvature.md`
- `M-chipwitz-corps` (mission) — file `/home/joe/code/futon3c/holes/missions/M-chipwitz-corps.md`
- `M-goals-and-holes` (mission) — file `/home/joe/code/futon2/holes/M-goals-and-holes.md`
- `T-outer-target-g-policy-prior` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-outer-target-g-policy-prior.md`
- `T-repair-occ-0228a166e753669832ab6342b4896ad310b86284f3793e8e876201bc4c658801` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-0228a166e753669832ab6342b4896ad310b86284f3793e8e876201bc4c658801.md`
- `T-repair-occ-ee9381591717eccb1f3a6c537943b3cd1d7f50131aa440bb3d72f3bddd51aa49` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-ee9381591717eccb1f3a6c537943b3cd1d7f50131aa440bb3d72f3bddd51aa49.md`
- `T-repair-occ-c4f34d0d2b676562a3c76bf7f7292bb73424d271aa0457c5a9fb21977c5d0201` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-c4f34d0d2b676562a3c76bf7f7292bb73424d271aa0457c5a9fb21977c5d0201.md`
- `T-repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-002-resume-protocol-violation` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-002-resume-protocol-violation.md`
- `T-repair-occ-d2f65574326c6cf91328d4061f10e695abdb63444df18c2fa78a8ead4dd54812` (ticket) — file `/home/joe/code/futon2/holes/tickets/T-repair-occ-d2f65574326c6cf91328d4061f10e695abdb63444df18c2fa78a8ead4dd54812.md`
- `E-park-delivery-losses` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-park-delivery-losses.md`
- `E-evidence-flow` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-evidence-flow.md`
- `E-dynamic-queries` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-dynamic-queries.md`
- `E-R8-red-ring-fill` (excursion) — file `/home/joe/code/futon3c/holes/excursions/E-R8-red-ring-fill.md`
- `E-clean` (excursion) — file `/home/joe/code/futon6/holes/excursions/E-clean.md`
