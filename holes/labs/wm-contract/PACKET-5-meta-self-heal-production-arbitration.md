# Packet 5 — production META self-heal arbitration

Date: 2026-10-19

## Production call graph

```text
combined/scheduled/serving caller
  -> full-loop-runtime/run-opportunity!
     -> meta-self-heal-production/route!
        -> meta-injury-store/load-durable-ledger
        -> meta-injury-ledger/active-injuries
        -> coordinator/prepare or sealed transaction reload
        -> claim checkpoint -> atomic ledger + transaction persist
        -> wm-author Agency adapter -> author checkpoint -> persist
        -> wm-reviewer Agency adapter -> review checkpoint -> persist
        -> sanctioned enact/reload adapter -> enact checkpoint -> persist
        -> attributable retry adapter -> terminal transition -> persist
     -> ordinary runner only when route! returns nil (no active injury)
```

An active injury always returns a dedicated-route receipt, including typed
configuration refusal. It cannot fall through to policy-field/G construction.

## Authority and transport

Production accepts either explicitly injected sealed repair ports or all of:

- `:agency-invoke-fn`, invoked with named `wm-author` and `wm-reviewer` seats
  plus work-order session ids;
- `:repair-enact-fn`, the sanctioned reload/enact boundary;
- `:repair-retry-fn`, which must return repair/patch/retry-click attribution.

Agency job id, session id, seat, commit/review payload, repair id, injury id,
attempt, patch SHA and retry click are retained in sealed receipts. Missing
transaction path, ports, diagnosis, or acceptance authority fails closed as
`:meta-self-heal/production-config-required`.

## Persistence and restart

The sealed ledger and transaction are persisted before the next effect at
claim, author, review, and enact. The ledger uses the existing atomic store;
the transaction uses same-directory atomic replacement. A persisted authored
checkpoint was reloaded and resumed with zero additional author calls and one
review/enact/retry call each.

## Ordinary-route quarantine

`construct-selected-action :run-algorithm` now returns the typed refusal
`:meta-algorithm-wrong-route`, naming the dedicated route and the forbidden
ordinary loop-node, fold, policy-selection, and D-task boundaries. Ordinary
mission invariants were not relaxed.

Historical bypass census after repair:

- `97eab2ee8` loop-node bypass: **removed**. A META algorithm presented to the
  ordinary receipt has four `:meta-algorithm-wrong-route` refusals and cannot
  cross `require-complete!`.
- `fa43dd757` construction-fold enrichment: **removed/unreachable**. Direct
  algorithm construction and a replay-shaped legacy algorithm construction
  both refuse before fold realization.
- `42bfb4bb5` empty-universe D-task authority: **removed**. `context` returns a
  typed wrong-route refusal and no token universe or candidate identity.

The registry-derived `:meta-field-observation` and its independently carried
`:meta-field-source-pin` are required production evidence for ordinary META
selection when no active injury exists. The runtime exact-map test now names
both fields, verifies the digest, and proves omitted/substituted authority is
rejected; the assertion was not loosened.

## Gates and limitations

Focused integration and the unchanged Packet-4 harness exercise active-route
preemption, fail-closed configuration, durable checkpoints, restart, terminal
eligibility, identity transport, and zero duplicate effects.

No live ledger, Agency service, reload endpoint, or click was invoked. The
serving adapters are present but live endpoint/cast configuration remains a
deployment prerequisite. The click-acceptance timeout/stale-last-result seam
from the parent ticket remains unresolved and must be repaired separately
before a live proof.
