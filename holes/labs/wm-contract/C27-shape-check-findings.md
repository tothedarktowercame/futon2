# CLEANUP C27 — evidence shape checks (2026-08-31)

## Grouping before count

The fourteen unimplemented rows reduce to **nine evidence kinds**:

| Evidence kind | Declarations | Disposition |
|---|---:|---|
| `FindReceiptTable` | 4 | `shape-check-implemented` |
| `VerdictTable` | 3 | `shape-check-implemented` |
| `List R2TickLit` | 1 | `shape-check-implemented` |
| `IllFormedList` | 1 | `shape-check-implemented` |
| `List R8TickLit` | 1 | `shape-check-implemented` |
| `R8DispositionEvidence` | 1 | `shape-check-implemented` |
| `TickRunWitness` | 1 | `shape-check-implemented` |
| `proof term` | 1 | `cannot-shape-check` |
| `PreferenceStackWitness` | 1 | `cannot-shape-check` |

The already implemented `EraTable` check was corrected to read the report's actual nested `:perEra`
shape; `AblationTable` remains conformant.

## Per declaration

- `findF1Containment`, `findF2Receipted`, `findF3NonSelfCertifying`,
  `findF4Falsifiable`: `shape-check-implemented` by `FindReceiptTable`. It
  requires scenarios and rounds, selected patterns backed by receipt entries,
  and each receipt to name a route and warrant file. Removing a selected
  pattern's receipt is rejected.
- `r9WmVerdictsSound`, `r9TwoRunCensus`, `r9WmPerRowDeclarations`:
  `shape-check-implemented` by `VerdictTable`. Ledger and declared runs must
  have the same named rows, typed verdicts and declaration sources, with the
  report's source/soundness checks true. Flipping soundness false is rejected.
- `wmTraceR2`: `shape-check-implemented` by `List R2TickLit`. It requires a
  positive form count, 64-character content pin, and fourteen declared
  channels. Removing the channel census is rejected.
- `r2ContractCensusWmTrace`: `shape-check-implemented` by `IllFormedList`. Its
  count must equal a vector of file-named rows with nonempty missing-channel
  sets. Changing the count is rejected.
- `wmTraceR8`: `shape-check-implemented` by `List R8TickLit`. It requires a
  positive form count, content pin, and disposition tick map. A short pin is
  rejected.
- `r8CensusWmTrace`: `shape-check-implemented` by
  `R8DispositionEvidence`. All three counts must be natural numbers and sum to
  the corpus form count. A mismatched count is rejected.
- `wmRunsOnce`: `shape-check-implemented` by `TickRunWitness`. It requires a
  timestamp, positive store basis, written trace, and a nonempty typed route.
  An empty route is rejected.
- `r9VerdictConsultsChecker`: `cannot-shape-check`. Its evidence is a Lean
  proof term, not an EDN table; elaboration/axiom inspection is the required
  check, and no proof artefact is present at the registry fixture path.
- `preferenceStackLiveRecorded`: `cannot-shape-check`. Its fixture is the
  executable Clojure checker itself rather than an evidence value. The checker
  has a negative control, but there is no serialized `PreferenceStackWitness`
  for `contract_lint` to shape-check.

## Result and canonical invocation

The contract remains **82 declarations: 49 closed / 33 holes**. The linter's
pass predicate is intentionally unchanged. Because freshness is evaluated
before shape judgement, all sixteen old bindings remain `:stale`; C27 changes
their checkability, not their authority pins. Shape results are now
`:conformant 14`, `:shape-check-not-implemented 2`, `:wrong-shape 0`.

Canonical invocation:

```sh
AUTH=$(jq -r '.source["git-sha"]' ../mathlib4/DarkTower/WarMachine/holes-contract.json)
bb checks/contract_lint.clj \
  --contract ../mathlib4/DarkTower/WarMachine/holes-contract.json \
  --registry checks/witness-registry.edn \
  --report /tmp/contract-lint-report.edn \
  --authority "$AUTH"
```

The same command with `--negative` performs the C16 authority mutation and
must exit zero only when the mutation is rejected.
