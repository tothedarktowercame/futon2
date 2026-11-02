# E-kimi-task-233 — A6 acceptance-check: M-superpod-mark2's four unchecked boxes against marks 7/8

**Requisition:** in-progress — dispatched 2026-11-02T21:06:28Z to kimi-1 as invoke-1793653588412-10505-ef889f0a

Clocked in by claude-2 for kimi-1 on 2026-11-02 (one Kimi task, one excursion, so the seat's
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


## Kind A6: acceptance-check

The item is an older mission whose cluster has moved on to later marks. Its acceptance checklist
still has unchecked boxes. Your question, for EACH unchecked box: is it met by a later record,
superseded by a later decision, or still open with no record either way?

Where to look: the later marks live in `/home/joe/code/futon6/holes/` as `M-mark8.md`,
`mark7-*.md`, `mark8-*.md`, and under `/home/joe/code/futon6/holes/missions/`. Search those for
the box's own terms (corpus size, stage 9b, cosine, LeanDojo, learn-to-swim, corpus_ws_bridge,
FAISS, precision, BGE). Do not infer "met" from a plan; only a record of a result counts as met.

Output per item:
```json
{"stem": "<id>", "kind": "acceptance-check",
 "boxes": [
   {"box": {"value": "<the checklist line>", "cite": "path:line", "quote": "..."},
    "verdict": "met|superseded|open",
    "by": {"value": "<the later record that meets or supersedes it, in one sentence>", "cite": "path:line", "quote": "..."} OR {"unsure": "searched <paths>, found nothing that speaks to it"}}
 ],
 "confidence": "high|medium|low"}
```


## Items for this batch (1)

- `M-superpod-mark2` — file `/home/joe/code/futon6/holes/missions/M-superpod-mark2.md` — stated finish (claude-2's reading, verify it): L207-209: 200,000 papers; similarity bar; retrieval pilot
