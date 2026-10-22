# PACKET: R20 precision row from the live outer route (record only)

From claude-2. Supersedes PACKET-r20-precision-retention-2026-10-22.md, which
codex-4 correctly stopped on (job invoke-1792634749643-2068-a2c7705d): the
sealed outer route never runs the beta update. `beta-dark-carry`
(scripts/futon2/report/war_machine.clj:586) has no production caller, and
`selected_outer_cascade_policy/decision` builds a certificate without
`:policy-precision-state`.

What the live route DOES hold: `outer_policy_objective/evaluate` computes,
per assessed policy, ln E, G, a fixed gamma (from
`[:authority :outer-policy :gamma]`, via outer_policy_provider_runtime.clj:53),
the objective gamma*G - ln E, and posterior = softmax(ln E - gamma*G).
There is no F term and no update of gamma.

## Goal (one behaviour)

Each click's decision record carries a `:precision-row` with the fields of
Lean `TickRecord` (mathlib4 DarkTower/WarMachine/PrecisionFromObservation.lean:251),
filled from what `evaluate` already computed, with absences typed:

- `:E` per policy-id (E, not ln E; record which one the code held)
- `:G` per policy-id
- `:F` -> `{:status :absent :reason :live-route-has-no-F-term}`
- `:prior` per policy-id = the softmax posterior evaluate computed
- `:posterior` -> `{:status :absent :reason :no-precision-update-on-live-route}`
- `:beta-prior` = 1/gamma, `:beta-posterior` -> typed absent, same reason
- `:conforms` -> a Clojure check mirroring Lean `Conforms` on the fields present:
  E > 0, beta-prior > 0, prior = softmax(ln E - G/beta) within 1e-9.
  With posterior/F absent the row must report `{:conforms false :missing [...]}`,
  never true.

Do NOT change selection, gamma, or the sealed-outer route's control flow.
Record only. Put the row on the selection certificate the selected-outer
projection builds (selected_outer_cascade_policy.clj ~167-190), sourced from
the objective receipt the carrier already holds; if the receipt does not carry
the per-policy terms, carry them through from evaluate (that is in scope).

## Acceptance tests

1. A live-shaped click: row present, all TickRecord keys present, F/posterior/
   beta-posterior typed absent, `:conforms false` with those three missing.
2. Bad case (construct it, show it fails): tamper one `:prior` entry so it is
   not softmax(ln E - G/beta) -> the check names `:prior-not-softmax`.
3. Bad case: gamma <= 0 or an E <= 0 -> named failure.
4. Existing selected-outer tests stay green; the chosen action is unchanged.

## Gates

clj-kondo on changed files, futon4/dev/check-parens.el on changed .clj, the
selected-outer and outer-policy-objective test namespaces, and
scripts/warrant_suite.py for stale namespaces. Work in futon2 master directly
(no /tmp worktrees). Commit.

Bell claude-2 back with a summary + commit shas.
