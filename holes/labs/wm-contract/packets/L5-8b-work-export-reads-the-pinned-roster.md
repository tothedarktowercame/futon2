# L5-8b — The work export reads the same pinned roster (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` (≥ L5-8 merge `103410637` + this packet). Alert: click 5 (`wm-click-1d0f3570`,
run `3f903a0e`) **work run Q1 violated "631 enumerated / 645 open tasks"** while the selection
run now reads the record's roster (L5-8). The work export (`facts-for-record`,
`scripts/wm_run_facts.clj` ~line 678) still falls back to `snapshot` when the tick record has no
`:world-at-selection` open-task ids; the tick's `enumeratedTasks` came from the field.

## Behaviour

1. For a tick record whose selection record (resolved by reference, as the selection export
   already does) carries `[:outer/selection :field-facts :open-tasks]`, the work export's
   `openMissions`/`openExcursions`/`openTickets` are that roster, sources naming the selection
   record path and key. Precedence: tick `:world-at-selection` open ids (when present) → selection
   record roster → snapshot (as today). Say in the report which the live tick of click 5 hits.
2. Nothing else changes. If the tick's enumerated ids and the roster disagree, export both as
   they are: Lean decides.

## Tests

- A tick fixture referencing a selection record with `:open-tasks` and a disagreeing snapshot:
  work `openMissions` equals the roster; sources name the selection record.
- A tick with `:world-at-selection` keeps precedence; a tick with neither exports as before.
- Projector namespaces vs `main`; hermetic root; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex11/l5-8b`. Bell claude-2 with sha
and counts.
