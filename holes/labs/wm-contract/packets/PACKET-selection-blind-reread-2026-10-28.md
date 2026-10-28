From claude-11. Discovery only: no code, no clicks, no reloads. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Read the dependencies between the candidates of 13 recorded selection events,
without knowing which candidate was chosen. Another reader drew these edges
while able to see the choice; your independent reading tests whether the
edges were shaped by hindsight.

## Input

`/home/joe/notes/selection-patterns/blind-13-events-2026-10-28.tsv`:
event, source (path:lines, relative to `/home/joe/code`), candidates.

**Do not open** anything under
`/home/joe/code/futon2/holes/labs/wm-contract/evidence/` (it holds the other
reading and the choices) or `/home/joe/notes/selection-patterns/` other than
the input file. The sources sometimes say what was done next; read them for
what each candidate needs, not for what happened.

## How to read

Use the Giraffe stage-1 reading rules in
`/home/joe/code/futon3c/holes/missions/M-giraffe.md` (§INSTANTIATE and
§"Overlap with the outer-selection work", point 2): a need edge says candidate
A cannot be done (closure) or cannot start (artifact) until B; a candidate's
own internal steps are not a dependency; set aside readings you reject, with
the reason. For each candidate also give its state at the moment the choice
was being made (`backlog|unstarted|started|completed|cancelled`), from text
written before or at that moment; if only later text gives it, say
`unknown-at-choice`.

## Output

`/home/joe/code/futon2/holes/labs/wm-contract/evidence/selection-blind-edges-2026-10-28.json`:
`{"note", "read_by", "events": {<event>: {"items": [{"id", "state_group",
"file", "line"}], "edges": [{"from", "to", "to_kind", "kind": "needs",
"class": "closure|artifact", "file", "line", "quote", "note"}],
"rejected": [{"reading", "reason"}]}}}`. Use the candidate names exactly as
in the input. `to_kind` is `item` for another candidate, otherwise one of
`artifact|status|ruling|dangling|practice`. Every edge quotes the source line
it rests on.

## Acceptance

- All 13 events; every candidate appears in `items`.
- Every edge and every state cites a line a reader can open and find it on.
- Commit only that one file, by explicit path (futon2 is shared; never
  `git commit -a`).
