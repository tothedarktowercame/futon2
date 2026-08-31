# C37 — strict contract qualification (capability only)

**Recorded 2026-08-31. Migration state: built, not enabled as the default gate.**

`checks/contract_lint.clj` reports three distinct facts:

- `:structural-valid?`: the registry and authority pin are well-formed. This is the
  existing permissive `:pass?` and remains the default process exit criterion.
- `:bindings-fresh?`: no declaration has a binding whose contract or fixture pin
  has drifted.
- `:strict-pass?`: both structural validity and binding freshness hold.

Select the new verdict with `--strict`. It intentionally exits 1 today; no build
gate has been changed to consume it.

## Current migration cost

The current authority contains **93 declarations: 64 closed-by-record, 5 refused,
8 unwitnessed, and 16 stale**. Strict qualification fails on these **16 bindings**:

`nonDegenerateAblationLaw`, `findF1Containment`, `findF2Receipted`,
`findF3NonSelfCertifying`, `findF4Falsifiable`, `r9VerdictConsultsChecker`,
`r9WmVerdictsSound`, `r9TwoRunCensus`, `r9WmPerRowDeclarations`, `wmTraceR2`,
`r2ContractCensusWmTrace`, `wmTraceR8`, `r8CensusWmTrace`, `r8EraBoundary`,
`preferenceStackLiveRecorded`, and `wmRunsOnce`.

All **16 are `:rerun-and-rebind`**: every stale binding names an executable check
with repository, path, and entrypoint. **Zero require unlocated/manual triage.**
This classification says the repair route exists; it does not claim the rerun will
pass. The stale count remains 16 after C27, C38, and C47; those deliveries moved the
declaration, closed, and unwitnessed counts, but did not reduce this migration cost.

## Canonical invocations

Set `AUTHORITY` to `.source["git-sha"]` from the contract.

```sh
AUTHORITY=$(jq -r '.source["git-sha"]' /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json)
bb checks/contract_lint.clj --contract /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json --registry checks/witness-registry.edn --report /tmp/contract-lint.edn --authority "$AUTHORITY"
bb checks/contract_lint.clj --negative --contract /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json --registry checks/witness-registry.edn --report /tmp/contract-lint.edn --authority "$AUTHORITY"
bb checks/contract_lint.clj --strict --contract /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json --registry checks/witness-registry.edn --report /tmp/contract-lint.edn --authority "$AUTHORITY"
bb checks/holder_check.clj
```

Expected today: default 0, negative 0, strict 1, holder check 0. The strict nonzero
is the reported migration debt, not an enabled build-gate failure.
