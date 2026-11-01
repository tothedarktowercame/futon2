# L5-8 — The selection record carries the open roster it enumerated; the projector reads it (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` (≥ the commit this packet lands in). Alert being repaired: click 5 of
2026-11-01 (`wm-click-1d0f3570`), selection run **Q1 violated "631 enumerated / 645 open tasks"**.

## Cause (found; verify, do not rediscover)

The field builder `outer_observation/giraffe-field` enumerates `open-entries` from
`target-authority`, which loads missions with the zero-arg `mission-registry/load-missions`
(substrate route). The projector `scripts/wm_run_facts.clj` builds its open roster from
`snapshot` → `(registry/open-missions (registry/load-missions "/home/joe/code"))` (file route),
used because the selection record has no `:world-at-selection`. The two routes disagree on
operator gating: 14 missions (M-fold-ansatz, M-wm-tripwires, M-a4a-structure-node,
M-象-cascade, M-aif-over-the-operator, …) are `:operator-gated? true` from substrate and not
from files, so the field (631) omits them and the roster (645) counts them. Lean Q1 then reads
a partition that does not cover the roster. This is recomputation across a seam: the
projector must read the roster the producer used, never rebuild it.

## Behaviour

1. `giraffe-field` records the roster it enumerated from, by kind, in `:field-facts`:
   `:open-tasks {:missions [ids] :excursions [ids] :tickets [ids] :source :substrate|:files
   :registry-pin <sha>}` (ids sorted; pin = digest of the sorted id vectors). It is the same
   `open-entries` the partition is validated against, so `enumerated` = union of the three.
2. The selection export (`selection-facts-for-record`) takes `openMissions`,
   `openExcursions`, `openTickets` from `[:field-facts :open-tasks]` when present, else from the
   work projection as today; `sources` names the record path. The work export is unchanged.
3. Nothing else changes: no new exclusion reason, no gating decision. Whether operator-gated
   missions belong in the field is a separate question (Joe's); this packet only makes the two
   sides read one roster.

## Tests

- Fixture registry with one operator-gated mission that the pass has no row for: the field's
  `:open-tasks` equals the enumerated set, the export's `openMissions` equals the record's, and
  the Q1 counts agree (reuse the real-Giraffe fixture in `outer_menu_wiring_test` or the
  `injury_ticket_join_test` field fixture).
- A record without `:open-tasks` (every record before this change) exports exactly as before.
- Named, outer, projector namespaces vs `main`; hermetic root; data count unchanged.

## Also record (report, not code)

`reconciliation/FINDING-registry-routes-disagree-on-gating-claude2-2026-11-01.md` is mine;
append the exact 14 ids and both routes' `:operator-gated?` values from one probe so the
registry owner has the evidence.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex11/l5-8-roster-pin`. Bell
claude-2 with sha, counts vs main, and the probe table.
