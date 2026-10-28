From claude-11. Discovery only: no code, no clicks, no reloads. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Build the test set for validating the War Machine's outer (task) selection:
one row per recorded selection event, taken from the evidence catalogue the
19 task-selection patterns were written from. The machine's outer selection
will later be checked by whether it picks what was actually picked at each
event. This packet only extracts the events; it does not score anything.

## Inputs

- `/home/joe/notes/selection-patterns/witnesses-2026-10-28.md` (363 lines,
  kinds K1–K19, each witness numbered, with source path:lines, a quote, and
  "Tension … Chosen …").
- `/home/joe/code/futon3/library/task-selection/README.md`: the table mapping
  each pattern to its witness kinds (K-numbers).
- The cited source files themselves (paths are relative to `/home/joe/code`,
  or `~/...`), read only to fill the columns below.

## Output

`/home/joe/code/futon2/holes/labs/wm-contract/evidence/selection-testset-2026-10-28.tsv`
plus a short `selection-testset-2026-10-28.md` beside it with counts and the
problems you met. Columns:

1. `event` — `K<k>.<n>` as in the catalogue.
2. `date` — as given; `unknown` if the catalogue and source give none.
3. `source` — path:lines exactly as cited.
4. `candidates` — the alternatives the record shows were on the table at that
   moment (task / mission / slice names), `;`-separated. Read the source
   around the cited lines to find them. If the record does not show the
   alternatives, write `NOT-RECORDED`. Do not reconstruct or guess them.
5. `chosen` — what was selected, in the record's words.
6. `patterns` — the task-selection pattern ids whose README row lists this
   witness's K-number.
7. `quote` — the catalogue's quote, verbatim, tabs/newlines replaced by spaces.

Then, in the .md: number of events; number with candidates recorded; number
`NOT-RECORDED`; any witness whose source lines no longer say what the quote
says (cite both).

## Acceptance

- Every numbered witness in the catalogue has exactly one row (state the count
  and check it against the catalogue).
- No `candidates` cell is filled from inference: each non-`NOT-RECORDED` cell
  can be found by reading the cited source near the cited lines. List in the
  .md, per such row, the line numbers you took the candidates from.
- Commit only the two new files, by explicit path (never `git commit -a`;
  futon2 is a shared checkout).
