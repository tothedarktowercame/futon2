# C22 — semantic falsifiers and canonical invocations

Recorded 2026-08-31 for the four checks given mutation modes by the ORGANIZATION lane.
All negative modes preserve input syntax and shape, never overwrite positive reports,
and use `0 = control passed`, `1 = ordinary failure`, `2 = mutation slipped`.

## `contract_lint`

```sh
AUTH=$(python3 -c 'import json; print(json.load(open("/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"))["source"]["git-sha"])')
bb -cp . checks/contract_lint.clj --contract /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json --registry checks/witness-registry.edn --report /tmp/contract-lint.edn --authority "$AUTH"
bb -cp . checks/contract_lint.clj --negative --contract /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json --registry checks/witness-registry.edn --report /tmp/contract-lint-negative-unused.edn --authority "$AUTH"
```

Mutation: replace the structurally valid contract's authority SHA in memory. Observed exits: `0/0`.

## `r9_independence`

```sh
bb -cp . checks/r9_independence.clj --report /tmp/r9-independence.edn --lean /tmp/r9-independence.lean
bb -cp . checks/r9_independence.clj --negative --report /tmp/r9-independence-negative-unused.edn --lean /tmp/r9-independence-negative-unused.lean
```

Mutation: use a false-only membership decider so an inside producer is mislabeled independent. Observed exits: `0/0`.

## `control_map_lint`

```sh
bb -cp . checks/control_map_lint.clj --report /tmp/control-map-lint.edn
bb -cp . checks/control_map_lint.clj --negative --report /tmp/control-map-lint-negative-unused.edn
```

Mutation: remove one valid drawn edge required by the independent 21-edge baseline. This check does not
open the SVG; `control_map_figure_agreement_check.clj` owns figure agreement. Observed exits: `0/0`.

## `r2_channel_contract`

```sh
bb -cp . checks/r2_channel_contract.clj --report /tmp/r2-channel-contract.edn
bb -cp . checks/r2_channel_contract.clj --negative --report /tmp/r2-channel-contract-negative-unused.edn
```

The positive command also writes the sibling `/tmp/r2-channel-contract.lean`. Mutation: select a
previously conformant real trace record and remove one of the 14 declared observation-channel keys.
Observed exits: `1/0`: the current corpus already has 2 firing records (797/799 conform), and the mutation
adds a third precisely identified key-set failure (796/799 conform). The positive red verdict predates and
is unchanged by the mutation mode.

## Existing test-fixture drift, not absorbed

- `test/control_map_lint_test.clj`: one stale current-data expectation (endpoint agreement counts).
- `test/r2_channel_contract_test.clj`: four stale current-corpus expectations (53→54 files,
  792→799 forms, 790→797 conforming, content pin changed).

The predicate-focused assertions in those files still run; this delivery does not refresh moving-corpus
snapshots under the cover of adding mutation capability.
