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

## `absent_is_loud_lint`

```sh
bb -cp . checks/absent_is_loud_lint.clj
bb -cp . checks/absent_is_loud_lint.clj --negative
```

The existing fixtures are genuine asserted controls: the bad fixture must produce exactly 4 violations,
the repaired fixture exactly 0, and the repaired fixture must contain exactly one
recorded-then-substituted case. `--negative` exposes the bad fixture as the semantic input under test.
Observed exits: `0/0`.

## `r8_f_contract`

```sh
bb -cp . checks/r8_f_contract.clj --report /tmp/r8-f-contract.edn
bb -cp . checks/r8_f_contract.clj --negative --report /tmp/r8-f-contract-negative-unused.edn
```

Mutation: select a pre-boundary, pinned `:missing-F-computable` g-map record with neither stored F nor
selection gain, then add stored F. In addition to the era violations, the delta classifier must report
that exact pinned identity as `:reclassification`, never `:append-only-growth`.
The checker must identify that exact record under `:stored-without-gain`, `:stored-not-controller`, and
`:stored-before-boundary`. Observed exits: `1/0`; the current live corpus is independently red at 54 files,
799 forms, and dispositions `755/39/5` against the recorded `755/32/5`. Negative mode writes no report or
Lean fixture.

## C28 decision — staleness must fail qualification

`contract_lint`'s top-level qualification verdict should **not** tolerate stale witness bindings. A stale
hole is not evidence that the contract is structurally malformed, but it is disqualifying evidence for a
gate claiming that the formal spine is currently bound to runs. The implementation should expose two
named verdicts: structural validity may remain green, while qualification is green only with zero stale
bindings; the CLI status used by build gates must be the strict qualification verdict.

Consequence: switching today would deliberately turn current green gates red until the 16 stale bindings
are refreshed or retired. That is the correct signal, but it requires an announced gate migration rather
than being smuggled into C22's mutation-only edits. C28 decides the semantics here; the migration is a
separate bounded implementation unit.

## C35 decision — stable snapshot tests plus a live invariant gate

Do not refresh the four live-corpus literals. Split their two jobs:

1. A committed, compact snapshot fixture tests parsing, channel classification, content-pin computation,
   and Lean emission with exact counts. It changes only when the fixture's intended semantics change.
2. The live-corpus gate asserts invariants, not corpus size: every record has the declared observation-key
   set, no new failure class appears, and every failure is reported with identity. File/form counts and the
   content pin remain emitted evidence, not expected literals.

Deriving a new expected count from each run would ratify whatever the run produced and recreate the
vacuity. Keeping literal live counts guarantees chronic red. The split preserves both reproducible unit
coverage and an honestly red operational monitor; today that live monitor remains red on 2 of 799 forms.

The same construction exists in `r8_f_contract_test`: its current-baseline group now has 7 stale
assertions at 54/799 and 39 stored-F while the predicate-focused groups remain applicable. C35 therefore
governs both R2 and R8 live-corpus tests; fixing only R2 would leave its twin chronically stale.

The C22 mutation establishes that `r2ContractCensus` itself can reject a missing channel. Its historical
vacuity came from the population supplied to it, not from an always-true predicate. Those are different
failure modes and should no longer be cited interchangeably.

## C35 implementation — snapshots separated from live invariants

Canonical snapshot suites:

```sh
bb -cp . test/r2_channel_contract_test.clj
bb -cp . test/r8_f_contract_test.clj
```

The exact-count side is now owned by dated compact fixtures at
`test/fixtures/r2-channel-contract/snapshot.edn` and
`test/fixtures/r8-f-contract/snapshot.edn`. Their full content pins are respectively
`1c2c1fcda2a423a1dfd32ecb03c7741cbcb8169def46c9098e7486dbcadbac63` and
`e5d9801b5f3676e30c421d71c7aaa88956b8e6bd08175217f943743de83818b0`.

The live R2 gate remains red for the stated semantic reason: two real observations omit declared
channels. Its moving file/form/conformance counts and ratio are report evidence, while the assertions
enforce no undeclared channels, no new failure class, identified failures, and the declared 14-channel
interface. Positive/negative exits were `1/0`; the negative mutation added another missing-channel record.

The live R8 gate is green because its era boundary, shape, disposition partition, and finite-F invariants
hold; it no longer compares the live census to literal counts. The recorded `755/32/5` census is pinned
to its 792-form content digest and last-record timestamp. Each live delta is classified as
`:append-only-growth`, `:reclassification`, or `:unexplained`; append-only requires every new identity to
postdate the watermark and satisfy the live invariants. The dispatch began at
`755/39/5` (the required unexplained +7 finding); during verification the shared corpus grew again to
800 forms and `755/40/5`, making the same discrepancy +8. This additional drift is exactly why the live
test asserts the delta kind rather than either moving number. C44's canonical positive/negative invocations
are the two `r8_f_contract.clj` commands above; observed exits were `0/0`. The negative mutation is both
rejected by the era invariants and typed as a pinned-population reclassification, so a permanent green or
an “everything is append-only” classifier was not introduced.

## C25 — R17 generator/disposer wiring guard

```sh
bb -cp . checks/r17_generator_disposer_check.clj --report /tmp/r17-generator-disposer.edn
bb -cp . checks/r17_generator_disposer_check.clj --negative --report /tmp/r17-generator-disposer-negative-unused.edn
```

The positive check records the present count-only all-pairs proposer as `:dormant-guarded`: R17 reaches
`reduce-concepts`, but the production War Machine entrypoint does not reach R17. The semantic mutation
makes that exact all-pairs path live-reachable; the invariant rejects it because an all-pairs enumeration
is not an independent generator. Observed positive/negative exits: `0/0`.

This check becomes a required pre-merge/CI gate when any production scheduler, server, or War Machine
entrypoint first references `r17`, `r17-offline`, or `reduce-concepts`. Until that trigger, running it in a
general green suite would misstate readiness: its green verdict means only that the unsafe path remains
dormant, not that R17 is safe to wire live.
