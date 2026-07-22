# D-1 handoff — zaif input hydration + dual-constant recording (Z3a prerequisite)

Dispatched 2026-07-22 by claude-2 to zai-1. Charter: z3-prereg.md §Dependencies D-1
(futon2/holes/labs/M-zaif-harness/ — read it first; it is the contract).

## Goal

Make live `decide()` calls non-degenerate and Z3a-scoreable:
1. **Input hydration** — a `:zaif-inputs-fn` so the controller sees real beliefs
   instead of empty maps.
2. **Dual-constant recording** — every round records the decision under BOTH
   `operator-attention-cost` constants (0.65 as-shipped, 0.15 sweep value) from
   the same inputs, mechanically pairable.

**Hard constraints:** NO actuation — the `_decision` binding in
`run-tool-rounds!` stays unused and round behavior is unchanged. NO live
reload of the serving namespace (`futon3c.agents.zai-api` is the live
harness): file changes + tests only; activation happens at the next JVM
restart, which is an operator op. Hydration failure must NEVER hurt a turn —
any error degrades to the current empty-map behavior.

## Files

- `futon3c/src/futon3c/agents/zai_api.clj` — hook site
  (`default-zaif-inputs`, `maybe-zaif-decision!`, `:zaif-inputs-fn` option).
- `futon3c/src/futon3c/agents/zaif_controller.clj` — `decide`, `constants`,
  `persist-decision!`, `decision-evidence-entry`.
- NEW `futon3c/src/futon3c/agents/zaif_inputs.clj` (suggested) — the hydrator.
- Data: `futon2/holes/labs/M-zaif-harness/b1-gamma-mission.edn` (γ(mission)
  table, source of truth for γ); `pz1-final-truth.edn` (correction rates);
  the D1 sidecar / evidence API for posting-stats and mark-tagged turns
  (query with `tag=` — the `tags=` param is silently ignored).

## Design latitude (state your choices in the bellback)

- Hydration cadence: per-session or per-turn cache is fine; per-round store
  queries are not (turn latency). γ table path configurable
  (`FUTON3C_ZAIF_GAMMA_EDN`, default the B1 artifact path).
- c-belief `:operator-c-uncertainty`: derive from per-mission correction rate
  (marks `tag=correction` + PZ1 final truth); document the formula you pick.
- Pairing shape: two evidence entries sharing a pairing key with a
  `:constant` field in the body, or one entry carrying both decisions —
  your choice, but pairing must be mechanical for the Z3a scorer, and tags
  stay `[:zaif :arm-choice]`.

## Acceptance (external — I will re-run these)

1. Unit tests for the hydrator: the exact γ cell
   `M-futon-forward-model → 0.7071067811865476` (2^-1/2, from the B1
   artifact); failure path (missing file / store down) returns empty maps
   without throwing.
2. Dual-decision test through a stub evidence store: both constants'
   decisions recorded per round, mechanically paired, arms re-derivable
   from recorded inputs by calling `decide` again (determinism check).
3. Gates: clj-kondo clean on changed files; repo tests pass
   (`clojure -X:test` or the repo's runner); check-parens
   (`futon4/dev/check-parens.el`) on changed files.
4. Doc checkpoint: D-1 outcome section in `futon2/holes/M-zaif-harness.md`
   (same commit as the futon2 side, if any; otherwise its own commit).

## Deliverable

Commits in futon3c (+ futon2 doc checkpoint). Keep the verify step under
~20 minutes (the ~30-min Agency job cap kills long jobs silently).
**Bell claude-2 back with a summary, the commit shas, and exactly what you
ran.**
