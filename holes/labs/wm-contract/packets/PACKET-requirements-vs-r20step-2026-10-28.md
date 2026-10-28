From claude-11. Discovery only: read and write one note; no Lean or Clojure edits, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

The War Machine's run criteria are `DarkTower/WarMachine/Requirements.lean`
(`/home/joe/code/mathlib4`, branch `darktower`): `RunFacts`, `Q1 Q3 Q4 Q5 Q6
Q7 Q9`, `conforms`. They were written for the outer selector deleted today
(`/home/joe/code/futon2/holes/labs/wm-contract/DELETE-outer-selection-2026-10-28.md`),
which built and scored inner cascades for every target before choosing.
Joe (2026-10-28): outer selection is now the same loop as the inner one, its
policies are task-selection cascades, and we resume Lean-first; the criteria
for "the machine runs" must be revised to match.

You built the new loop: `DarkTower/WarMachine/R20Step.lean` (slices 1–3c,
head `d7649b2f2`). Say, for each requirement, what it becomes there.

## For each of Q1, Q3, Q4, Q5, Q6, Q7, Q9 (and each `RunFacts` field they read)

Classify as one of:

- **restate** — the requirement still says something true and wanted about a
  run; give the statement as a theorem about `step`/`run` (Lean signature
  only, no proof), and say which R20Step definitions it reads. Example
  shape: Q3 "no cascade without G" might become "every cascade in
  `m.cascades` has an entry in `click.cascadeScores`".
- **already-holds** — an existing R20Step theorem already states it; name it.
- **retire** — it is about the deleted path (e.g. inner families built for
  every target, `TargetConstruction` before the choice); quote the part that
  ties it to that path.
- **not-yet-modelled** — wanted, but R20Step has nothing it could be stated
  over yet (e.g. inner policies for the chosen task, R20 certificate rows);
  name the spec slice (4, 5 or 6 in
  `/home/joe/code/futon2/holes/labs/wm-contract/SPEC-r20-step-function-2026-10-28.md`)
  where it would land.

Quote each Q's Joe docstring (the `/-! Joe: ... -/` lines) in your row: those
are the reasons the requirement exists, and a restatement must keep them.

## Output

`/home/joe/code/futon2/holes/labs/wm-contract/NOTE-requirements-vs-r20step-2026-10-28.md`:
one section per Q, then a short list: which restated theorems could be proved
now against slices 1–3c, ordered by what each depends on.

Commit only that file, by explicit path (futon2 is shared; never
`git commit -a`). Do not edit `Requirements.lean`: changing the
specification goes to Joe first.
