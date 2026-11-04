# Note: how the War Machine ascertains focus (for the PLoP 2026 revision)

Joe, 2026-11-04: the original PLoP 2026 paper did not write up how the machine knows the
operator's focus; the model has improved since and the revision should report these steps.

## What existed

`resources/wm/focus/commit-facets-v1.json`: focus credited from commit paths in two windows
(2026-09-20/21), facets WM and APM, never refreshed. Nine hand-reviewed relation rows. By
2026-11-04 every pick outside those nine failed requirement Q7 (`pickRelation` must be observed).

## What replaces it (in progress)

1. Task areas, read once and durable: A7 area ants (Zai) read each candidate task's own file and
   name the area its work is for, with a cited line (`futon3c/holes/excursions/ant-prompts/A7-relate.md`).
2. Operator focus, read per click and replayable (FOCUS-2a, branch `FOCUS-2a`): the 10 seats Joe
   spoke to most recently in the last 7 days, from his turns in the evidence landscape (futon1b);
   each seat casts a clock half-vote and a text half-vote (one Zai reader call over at most 20 turns);
   focus is the top 3 areas with shares, ties kept. Everything read is retained under its sha;
   replay never re-reads or re-calls the model. Works for any past date.
3. Relation (FOCUS-2b): a task in a focus area is `focus`; linked is `associated`; else
   `useful-elsewhere`; no area row is `:unknown`.

Design turns, each Joe's: focus is a light focus over several things (top 3, not one winner);
clocks are evidence, not noise; no hand-tuned keyword tables, the method must be cheap and
reproducible on demand for any point in history.

## Related and still open

- War Room (futon3/holes/war-room.md, bulletin 12): reclaims focus by reconstructing arcs from git
  and a ledger of stranded work. Candidate FOCUS-3: commits as evidence, arcs as output.
- The War Machine's own focus over previous clicks (Joe, 2026-11-04): the same method over its
  selection records. Keep it a separate reading from the operator's, not a voter in it (WR-0: the
  apparatus must not issue its own verdict; a self-vote reinforces its own picks).
