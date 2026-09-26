# PROOF-2a: second sequential Codex slice

codex-2, 2026-09-26, at Joe's request for another slice of comparable size.
This implements the binding/quotation part of the already queued prover
work under ⟨2⟩2b. Independent acceptance remains with claude-8.

## Change and source evidence

**futon3c `887fe169`** changes `diagramprover/wiring.clj` and adds the
binding tests and positional negative controls:

- `:keys`, namespaced keys and renamed selectors such as `{b :beta}` count
  as reads in function parameters and recognised binding forms, including
  nested patterns. A returned map containing `:keys` is not a binding.
- A recognised symbolic selector reaches the public occurrence check, so
  a real `:keys` read no longer needs a literal `:beta` elsewhere in its
  source merely to be noticed.
- Quoted definitions are not source sites. Quoted expressions do not supply
  reads, executed calls, argument provenance, or uses of callee parameters.
  Syntax quotation is treated conservatively; this is not a macro evaluator.
- Scoped binding evidence requires the record's declared name/alias through
  `:as` or a direct named initializer. Nested patterns do not inherit the
  outer record. An unnamed map parameter is not silently assigned a record.

The public-checker tests include the two prior false positives (returned
`:keys` data and quoted function data), renamed bindings, namespaced keys,
wrong record/receiver, unrelated parameters, nested binding scope, defaults,
and unknown binding macros. The positional tests additionally plant quoted
calls, quoted argument expressions and quoted-only parameter uses. They
invoke the real checker; no checker or source-attribution port is stubbed.

## Map replay and generated artifacts

**`9d962400`** records the replay's sole new finding. At futon2 `e92ae532`,
`repair_obligation/record-system-failure!` destructures `outcome` from its
finding argument and persists it under `:failure-outcome`. Recognition of
that read does not identify its finding record with the judge-refusal
record's unscoped `:outcome` wire. It is recorded with that explicit scoping
reason, alongside the existing different-record findings.

The expected report changes **348 → 349**: no findings removed, one standing
finding added, eight to-do findings retained. An EDN comparison verified
that the entire map outside `:expected-findings` is unchanged. Current
comments now distinguish a recognised binding from a proven handoff.

**`52c5bcb4`** removes the obsolete fixed count of unsupported shapes from
the diagram legend. **`7c4351b5`** regenerates the wiring figure, adjacency
and organisation layer at map `9d962400` and futon2 source `8c703dd1`.
Adjacency and organisation data were compared with the previous versions:
only revision pins changed. Both SVGs parse as XML.

**`e6a308f9`** updates the ledger test's pins; **`4f4fcd10`** regenerates
the ledger; **`4f622dc8`** regenerates coverage from committed inputs.
Ledger entries and statuses were checked equal to their preceding versions
apart from the adjacency/map pins:

- **170 wires: 5 verified / 83 hermetic / 82 unverified.**
- Coverage: **150 witness / 15 conditional / 4 failure-path / 1 unreachable.**
- 27 condition texts remain conservatively undecided.

No wire or verification status was added by recognising a binding.

## Validation

**93 tests, 884 assertions; zero failures/errors**, across seven separately
run namespaces. Existing warrants were checked before replacement; the
changed scopes refused as stale (the old map entry was not a warrant).
All seven new warrants have `:warrant? true` and matched postchecks:

| Namespace suffix (`futon3c.diagramprover.`) | Tests/assertions | Warrant |
|---|---:|---|
| `wiring-binding-test` | 6 / 72 | `test-registry-eb221349d04115f5ea1f11d3551d94c8bab10c4648c9ca2ea16fe72e45d99354` |
| `wiring-test` | 35 / 89 | `test-registry-9ab28eff8b73145b80cc6de357a6e909dac32b704d1c2d92f5fdaca870da9594` |
| `wm-positional-passes-test` | 13 / 55 | `test-registry-203adebfe1983ee5f008a1b390510f09f565c4823171f9e355122664a7f94b8b` |
| `wm-flight-wiring-test` | 7 / 21 | `test-registry-4954fbe69fce275eee86dd6d619af62eea7fb1fcbdff21985e4ee58f0b6c1801` |
| `wm-flight-replay-test` | 10 / 17 | `test-registry-ccf64bae15401ff37ba570cca9e994e8c2ed2530f1d4089ca883ecfdd689f334` |
| `wm-record-scope-test` | 18 / 98 | `test-registry-29c509c0d6600d66fa38265f6d1bfab9c518679e506e2409c8f85a85daa7726a` |
| `wm-wire-ledger-test` | 4 / 532 | `test-registry-fa4ac4bcbc5eccf797ab2844d799bb42952db00bee1da10b8b3d09b4745b9f9d` |

clj-kondo and `futon4/dev/check-parens.el` passed for the changed Clojure.
The figure generator's reader scan passed; clj-kondo reports its same two
pre-existing unused-binding warnings, no new warnings or errors.
The replay registration needed explicit `CODE_PATHS` because its only direct
require is a test helper; the load closure remains recorded by the runner.

## Next R6 packet, now narrower

Two source-attribution rules remain, rather than the general inability to
recognise destructuring:

1. A typed positional handoff into a destructured map parameter, including
   which supplied map field reaches which bound local and whether that local
   is used. Current `:passes` requires a plain symbol parameter. A wrong key,
   wrong argument and quoted-only use must fail against the actual sites.
2. The precision record returned through `seal (merge ...)`. Establish which
   fields that wrapper preserves from its source; do not treat an arbitrary
   wrapper as transparent.

Then declare the scoped temperature chain through the already existing
`r14-selection-posterior` box and test the wire. Do not add a duplicate R6
box or count binding recognition itself as evidence that the value crossed
the handoff. The first slice's Cτ compatibility, preference-authority and
intermediate-progress obligations remain open.
