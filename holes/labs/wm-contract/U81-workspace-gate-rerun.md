# U81 — pinned workspace-gate rerun

Run `bounded-1788892898404-workspace-gate` executed
`bb -cp . checks/wm_workspace_gate.clj` from Futon2
`e98ef1df16feb579dda0359a9b67cb4e05564654`.  The canonical receipt records
the complete start and finish bases, interval, 164 executable checks, ten
failures, and the failed verdict at `data/wm-workspace-gate/latest.edn:1`.
The bounded resource receipt is
`/tmp/futon-bounded-tests/bounded-1788892898404-workspace-gate.resource.edn`;
the run reported `resource-status :clean` under the run id above.

## Repository movement, separately

All five pinned repositories were stable across the interval
2026-09-08T18:41:38.528062569Z–2026-09-08T18:49:43.650050733Z: Futon2,
Mathlib4, p4ng, Futon3, and Futon3c each have identical start and finish
commit, tree, dirty-state, and tracked-diff fields
(`data/wm-workspace-gate/latest.edn:1`).  The receipt therefore says
`:basis-status :stable`; it separately refuses an event-free claim because no
writer fence was declared.  Futon2 had no tracked diff; p4ng, Futon3, and
Futon3c began and ended with unchanged tracked diffs.  This is not an
accepted-red entry and makes no ruling.

## Residual executable-check failures

The exact failure vector and exits are at
`data/wm-workspace-gate/latest.edn:1`.  An exact-name search of
`holes/labs/wm-contract/worklist.edn` was used for the row column below; a
missing exact name is recorded as **not found**, rather than assigned by
interpretation.

| residual | exit | row or not-found record |
|---|---:|---|
| `strict-contract` | 1 | not found |
| `mutable-verdict-claims` | 1 | not found |
| `q-interface-completeness` | 1 | not found |
| `holder` | 1 | not found |
| `organization` | 1 | not found |
| `fold-quarantine` | 1 | not found |
| `lean-sorry-categories` | 1 | U80 records the current checker/category conflict at `holes/labs/wm-contract/worklist.edn:1504` |
| `reload-click-certificate-rehearsal` | 1 | not found |
| `pinned-operational-certificate` | 1 | not found |
| `c174-reconstructible-quarantine-member` | 2 | not found |

This record reports the measured residuals; it does not repair, waive, or
classify them and writes no `:choices` or `:decisions` entry.
