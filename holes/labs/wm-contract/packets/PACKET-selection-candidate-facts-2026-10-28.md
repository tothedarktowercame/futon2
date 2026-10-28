From claude-11. Discovery only: no code, no clicks, no reloads. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

For the 32 events in
`/home/joe/code/futon2/holes/labs/wm-contract/evidence/selection-testset-2026-10-28.tsv`
whose `candidates` column is not `NOT-RECORDED`, record what the source says
about each candidate at the moment of choice. This decides, from the record,
which facts the War Machine's candidate type must carry so that the
task-selection cascades can be run on these events.

Why: in Lean (`DarkTower/WarMachine/R20Step.lean`, slice 3a/3b) a candidate is
only a target id and a reach, and its ending class comes from its relation to
the focus. The 19 patterns in `/home/joe/code/futon3/library/task-selection/`
gate and order on other things (can it start now, what waits on it, cost,
nearly done, external date, operator asked). We need to know which of these
the record actually gives, per candidate.

## Reuse the Giraffe work (Joe, 2026-10-28: do not reinvent M-giraffe)

Dependencies between candidates are NOT free-text facts here. Record them as
Giraffe need edges, in exactly the shape of
`/home/joe/code/futon3c/holes/tickets/giraffe-edges.json` (`from`, `to`,
`to_kind`, `kind: "needs"`, `file`, `line`, `quote`, `note`, `read_by`), with
a `rejected` list for readings you set aside, following the stage-1 reading
rules in `/home/joe/code/futon3c/holes/missions/M-giraffe.md` §INSTANTIATE
(a candidate's own internal steps are not a dependency; the five need kinds in
§"Overlap with the outer-selection work", point 2). Readiness ("can start
now"), one-step unblocks and cone are then COMPUTED by `futon3c.giraffe.pass`
from those edges and the candidates' states, not read off the prose; so do not
record `can-start-now`, `blocked-by` or `unblocks` as fact rows. Give each
candidate's state-group at the moment of choice (`backlog|unstarted|started|
completed|cancelled`) with its source line, as the pass input needs.

Output 0: `/home/joe/code/futon2/holes/labs/wm-contract/evidence/selection-candidate-edges-2026-10-28.json`
with `{"note", "events": {<event>: {"items": [{"id", "state_group", "file",
"line"}], "edges": [...], "rejected": [...]}}}`. Do not run the pass; claude-11
will.

## Output (the other facts)

`/home/joe/code/futon2/holes/labs/wm-contract/evidence/selection-candidate-facts-2026-10-28.tsv`
plus `selection-candidate-facts-2026-10-28.md` beside it. One TSV row per
(event, candidate). Columns:

1. `event`  2. `candidate` (exactly as in the test set)
3. `fact-kind` — one of: `cost`,
   `value-or-leverage`, `nearly-done`, `date`, `operator-asked`,
   `relation-to-focus`, `chance-of-success`, `other:<short name>`.
   One row per fact; a candidate with three facts has three rows.
4. `value` — the fact in the source's words (short).
5. `source-lines` — path:lines where you read it.

A candidate with no fact in the source gets one row with `fact-kind` =
`NONE-RECORDED`. Do not infer facts from what was chosen or from general
knowledge.

In the .md: per fact-kind, how many events give it for every one of their
candidates (only those kinds can drive a gate or order on that event); which
task-selection patterns' `@guard` lines read each kind (quote the `@guard`);
any event where the chosen candidate is not distinguishable from the others
by any recorded fact.

## Acceptance

- All 32 events covered; every candidate named in the test set appears.
- Every fact row cites source lines a reader can open and find it in.
- Commit only the three new files, by explicit path (futon2 is shared; never
  `git commit -a`).
