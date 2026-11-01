From claude-11. Discovery only: read code, records and the serving JVM read-only; no edits, no reloads, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

codex-11's run001 disposition under GRID v2
(`/home/joe/code/futon2/holes/labs/wm-contract/run001-v2-disposition.md`,
commit `5aa6b5c6d`) holds the first missions-only click on two conditions.
Establish, with file:line and record paths, whether each can be met by what
already exists. The outer loop is wired and loaded (futon2 `02b066016`).

## 1. E3: a pre-author stop that can be observed

The disposition: "retain a manual comparison of the selected target, relevant
inputs and terminal outcomes against prior refused/failed/already-satisfied
runs, including the M-recommendation-bindings history ... Use an existing
supported pre-author stop; if that boundary cannot be observed and stopped
before effects, HOLD."

- Is there an existing supported stop between selection and the first author
  dispatch (debugger hold, phase pause, `:stop-before` option, dry-run flag,
  WM click "preview")? For each: where it is (file:line), how a click is
  started with it, what is observable at the stop (does the retained
  `:outer/selection` exist by then?), and how the click is resumed or
  abandoned without side effects.
- Which prior runs are relevant for the comparison: list run records (paths)
  that selected M-recommendation-bindings or ended refused / failed /
  already-satisfied in the last 7 days, with their terminal outcome and input
  digest if recorded.

## 2. R28: raw failed-reading evidence

R28 is in `/home/joe/notes/wm-next/outer-selection/DEFECTS-reading-era.md`
and the deficits TSV row B2-D4-R28 ("bind reading-start/result retention to
failed redecision; preserve actual evidence"). For a reading of the chosen
task that fails:

- Which records are written at reading start, at result, at failure and at
  redecision (paths / namespaces, file:line)?
- Does each survive the failure, or is any written only on success or held
  in memory until close? Cite the code path.
- Is R28 still reachable after today's deletions (`a04b157d2`), i.e. does the
  with-task path still perform the redecision R28 describes?

## Output

`/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-run001-e3-r28-2026-10-28.md`,
two sections, each ending in one line: MET (with evidence) / NOT MET (what is
missing) / UNKNOWN (what would settle it). Commit only that file by explicit
path (futon2 is shared).
