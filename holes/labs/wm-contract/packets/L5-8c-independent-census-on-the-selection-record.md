# L5-8c — Q1's open side is an independent census recorded at click time, never the selector's own roster (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` (≥ this packet). Corrects L5-8/L5-8b, both merged. Authority:
`selection_world.clj` ~line 176: "Q1's open side is an independent census. Reading it once here
keeps the three registry sets coherent with one another while ensuring the selector's retained
field cannot define both sides of Q1." L5-8 made the selection export read the selector's own
roster (`[:field-facts :open-tasks]` = `open-entries`, the same set the partition is validated
against), so Q1 on the selection run would hold by construction. That is a façade and must not
stand. Your click-5 finding (the tick's `:world-at-selection` census, 645, against 631 enumerated)
is the honest reading: the two registry routes disagree, and Lean should see it.

## Behaviour

1. The runtime captures the independent census at selection time, with the same function the
   runner uses for the tick (`selection-world/default-task-sets code-root`, or a shared helper),
   and writes it on the selection record as `[:outer/selection :world-census]`:
   `{:open-tasks {:missions [ids] :excursions [ids] :tickets [ids]} :source :files :code-root …
   :captured-at …}`. It is not derived from the field.
2. The selection export's `openMissions/openExcursions/openTickets` read, in order: the tick's
   `:world-at-selection` open ids when the record resolves to a tick (as the work export does
   after L5-8b) → the selection record's `:world-census` → snapshot (as before). Never
   `[:field-facts :open-tasks]`. Keep `:open-tasks` on the record as the selector's provenance
   (what it enumerated from, with its source and pin); the export may carry it under a new
   diagnostic, not under the open side.
3. The work export (L5-8b) precedence: tick world ids → selection record `:world-census` →
   snapshot. Remove the `[:field-facts :open-tasks]` step.
4. Add to the field-facts diagnostics (not to RunFacts) the difference between the census and
   the selector's roster, by id, so the registry finding has per-click evidence.

## Tests

- Fixture: a selector roster that omits a gated mission the census includes: selection export's
  `openMissions` is the census; `enumeratedTasks` is the selector's; Q1 does not hold; the
  diagnostic names the gated mission.
- A record with `:open-tasks` and no `:world-census` falls to the snapshot, never to `:open-tasks`.
- Projector, outer, named namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. **Worktree only** (never check a branch out
in `/home/joe/code/futon2`). Branch `codex11/l5-8c`. Bell claude-2 with sha, counts, and the
click-6 selection record exported on the branch (read-only).
