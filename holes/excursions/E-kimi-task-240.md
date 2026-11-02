# E-kimi-task-240 — A1 place: open-unplaced batch 1, packet 5 of 8 (action vocabulary per claude-16's objection)

**Requisition:** completed — 2026-11-02T21:21:34Z, job invoke-1793654231851-10528-e4638c0d, state done

Clocked in by claude-2 for kimi-2 on 2026-11-02 (one Kimi task, one excursion, so the seat's
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


## Kind A1: place

The item is open and nobody has placed it on the map of the stack. Your question: what is it,
is it still open, which of the stack's four therefores does it serve, which declared way of
working it belongs to, and what one action would move it. You are placing, not deciding; a
reviewer scores your placement against another reader's.

Wings (the four therefores of the apex thesis, plus two others):
- A: records carry warrant — an assertion severed from its derivation cannot be checked or learned from.
- B: one queryable self-account — knowing what exists and its state, with provenance, at stack scale.
- C: feedback reaches every participant — the loop's human and stakeholder edges are where learning dies.
- D: accountable next-action choice — deciding what to do next, with a reason, goals included.
- V: stays alive and paid — machines, history, money; the body and energy of the stack.
- none: not about the stack's ways of working (e.g. a paper's content, an outside commitment).

Ways of working (pick the one the item most serves or most lacks; `none` if no fit):
records-carry-warrant, inbox-zero, operator-turns-logged (A); one-queryable-self-account,
backlog-understood (B); feedback-reaches-every-participant, real-users, intents-are-tracked (C);
selection-gives-a-reason (D); stays-alive-and-paid (V); eat-your-own-tail (the agent is inside the process it runs).

Output per item:
```json
{"stem": "<id>", "kind": "place",
 "what": {"value": "<one sentence: what the item is for>", "cite": "path:line", "quote": "..."},
 "status": {"value": "open|closed|stopped|unsure", "cite": "path:line", "quote": "<the status line or the line that shows it>"},
 "wing": {"value": "A|B|C|D|V|none", "cite": "path:line", "quote": "<the line that most shows which therefore it serves>"},
 "way": {"value": "<way id or none>", "cite": "path:line", "quote": "..."},
 "action": {"value": "work|close|retire|held|rehome|cluster:<name>|merge-into:<id>|audit|unsure", "cite": "path:line", "quote": "<the line that supports the action>"},
 "need_goes_to": {"value": "<item id that now carries the need, or 'solved'>", "cite": "path:line", "quote": "..."} OR {"unsure": "..."}   (REQUIRED when action is close or retire; omit otherwise),
 "finish": {"value": "<what done would look like, from the file>", "cite": "path:line", "quote": "..."} OR {"unsure": "no finish stated"},
 "beneficiary": {"value": "self|company|grant-math|product|job|unsure", "cite": "path:line", "quote": "..."},
 "confidence": "high|medium|low"}
```
Action vocabulary (rulings of 2026-11-02, claude-16's objection before packet 4):
- `work`: open, finish stated, nothing stops it. `held`: someone is holding it; say who, in `what` or `finish`.
- `rehome`: the mechanism it depends on is down and not coming back (futon1a on :7071 is the case) but the need stands.
  A gone mechanism alone is NEVER grounds for `retire`. A blocker must be something that can return; futon1a is never a blocker.
- `retire`: only when the problem it addresses is actually solved; then `need_goes_to` is "solved" with the citation that shows it.
- `close`: the stated finish is met; `need_goes_to` names the item that carries any residue, or "solved".
- `cluster:<name>`: belongs with others under one name, each keeping its own meaning (e.g. `cluster:superpod-work`).
- `merge-into:<id>`: only for real supersession, where the other item says the same thing better. When in doubt, cluster.
- `audit`: you cannot say from the file what it accomplished; say what you looked for.
- Whether something is "a prototype we can learn from" is the operator's call: give `unsure` there, with what you saw.

Rules for this kind: a kimi-task requisition (`E-kimi-task-N`) is a record of one dispatch; its
"what" is the task it carried, its status is the requisition line near the top. A draft mission
marked NON-LIVE is `held`, not `work`. Do not place an item by its filename alone.


## Items for this batch (6)

- `E-mfuton-typeclass-scope-state-20261003` — file `/home/joe/code/futon3c/holes/excursions/E-mfuton-typeclass-scope-state-20261003.md`
- `E-outer-selection-landscape` — file `/home/joe/code/futon2/holes/labs/wm-contract/E-outer-selection-landscape.md` — status line as parsed (verify it): OPEN. Capture C1–C3 landed (C3 reviewed 2026-10-26); C4 split into C4a (projecti
- `E-pre-go-live-experiment-2026-09-09` — file `/home/joe/code/futon2/holes/labs/wm-contract/runs/E-pre-go-live-experiment-2026-09-09.md` — status line as parsed (verify it): task commissioned by Joe; executable interpretation proposed for independent
- `E-pre-go-live-independent-pair-2026-09-10` — file `/home/joe/code/futon2/holes/labs/wm-contract/runs/E-pre-go-live-independent-pair-2026-09-10.md`
- `E-pre-go-live-independent-pair-review-2026-09-10` — file `/home/joe/code/futon2/holes/labs/wm-contract/runs/E-pre-go-live-independent-pair-review-2026-09-10.md`
- `E-pre-go-live-interpretation-review-2026-09-09` — file `/home/joe/code/futon3/holes/labs/library-contract/E-pre-go-live-interpretation-review-2026-09-09.md`
