# L5-15 — Completion discovery on the selection path; the baseline is a work-phase measurement (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` (≥ this packet). Worktree only. Authority: your L5-15-D (option 2 of §4) and
Joe's ruling 4 (selection is over tasks with task-level facts; feasibility facts only; no
features of inner work). A completion *baseline* is a measurement the work phase makes before
authoring (A4, `mission-observation/capture-before`); it is not a fact about the task that
selection can read, and `task-completion-surface/describe` says so in its own docstring while
emitting an unconditional `:baseline {:status :absent …}` placeholder that Q7 then counts on the
decision path of every excursion or ticket pick.

## Behaviour (the phase contract, declared)

1. `task-completion-surface/describe` emits the discovery carrier without a `:baseline` key. In
   its place a declared phase marker: `:baseline-phase :work` and `:baseline-producer
   :selected-task-revision-baseline` (the capability that measures it, named as today, but as a
   pointer, not an absence). Schema bumps to `:wm/completion-surface-v2`; readers of v1 keep working
   on retained records (say which readers exist; the exporter's `absence-paths` is one).
2. The field fact's `:completion` on the selection path is discovery only: class, counts,
   locators, source pin, phase marker. `selection-absence-paths` therefore counts no baseline
   absence on the selection path. It must still count every *selection* dependency: the pick's
   status, rank, cone, unblocks, reach and pin, and the completion-class when it is
   `:completion-definition-not-found` (a task whose completion is undefined is a typed selection
   fact, keep it visible).
3. The work path is unchanged: where the pre-author baseline is required and absent, that absence
   stays on the work run's path (Q7 of the work kind). Do not touch the work-side readers.
4. Giraffe census consistency (missions omit `:completion` entirely while E/T carry discovery) is
   recorded as a finding for the Giraffe owner in the report, not changed here.
5. No retrospective certification: click 11's record keeps its recorded Q7.

## Tests

- `describe` on a fixture excursion: no `:baseline` key, phase marker present, v2 schema.
- A selection record whose pick fact carries the v2 completion: `selection-absence-paths`
  does not include `[:pick-fact :completion :baseline]`; a pick with
  `:completion-definition-not-found` is still counted as a typed absence on the path.
- A retained v1 record (click 11's, as fixture) exports exactly as before.
- Named, outer, projector namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex11/l5-15`. Bell claude-2 with sha,
counts, and the re-export of a fixture pick before/after.
