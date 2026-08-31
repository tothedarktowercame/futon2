# C141 — `closed-by-record` category audit

Date: 2026-08-31

## Operational meaning

`closed-by-record` is currently a name, not a checked relationship. In
`checks/contract_lint.clj`, every declaration whose contract `kind` is
`"closed"` is assigned `:closed-by-record` before authority, binding freshness,
fixture shape, result, or acceptance inspectability are considered. No record
is opened.

`checks/holder_check.clj` provides a narrower guarantee. It reduces the final
segment of each free-text owner to a registry key, checks that the key exists,
and (when the Agency roster is reachable) checks that its holder is live. It
does not check:

- that the owner text resolves to a file or section;
- that the record supports the declaration;
- that the cited section has moved or been superseded;
- that `:decls` in `checks/holder-registry.edn` matches reality.

The last point is observable now: the registry comments and `:decls` values
still describe the former 80-declaration contract, while the contract has 100
declarations. `holder_check` passes because it never reads those counts as an
acceptance condition.

Thus what is verified is: the emitter classified the declaration as closed,
and its normalized ownership bucket has a live holder. Record existence and
semantic support are assumed.

## Declared sample

Population: all 89 contract declarations with `kind = "closed"` at contract
source `309d1f4874e4ce713ce813ca26a75a5c323eb734`.

Selection rule: normalize owners exactly as `holder_check` does; within every
ownership bucket having a closed declaration, sort declaration names and take
the lexical first and last (one row when the bucket has one member). This is a
deterministic boundary sample, declared before reading the records. It covers
all 10 closed ownership buckets and 18/89 declarations (20.2%).

| owner bucket | first | last | result |
|---|---|---|---|
| P-R19-preferences-open | `PreferenceConstantCensusRow` | `preferenceStackRecorded` | 2/2 supported by §principle/§gate and the dated lane amendments |
| P-R2 | `Channel` | `wmTraceR2` | 2/2 supported by §solved 1 and its ratified literal/pin amendments |
| P-R8 | `EraSummary.meanPrecision` | `wmTraceR8` | 2/2 supported by §solved 1's full-row era census and pinned snapshot |
| P-R9 | `DeclarationSource` | `wmVerdictsLedgerAlone` | 2/2 supported by §solved 2's per-row source decision and two-run tables |
| P-glossary-mathematics | `BeliefState` | `variationalFreeEnergy` | 2/2 supported at `sec-glossary.tex` paragraphs 9 and 19 |
| P-validated-R5 | `Cascade` | `wmCascadeDiffFixture` | 2/2 supported by §3e and its dated fixture-indexed amendments; older incompatible candidates remain legible but are explicitly withdrawn |
| R19-preference-stack.edn | `wmPreferenceStack2026_08_30` | `wmStackDeclaredPurpose` | 2/2 supported by the pinned stack record, including loud absent purpose |
| delivery-lifecycle | `Delivery` | `Workflow` | 2/2 structurally supported by §0.6/§0.10, although the containing lifecycle document still labels itself draft |
| route | `RouteHop` | — | support exists in `BUILD-packets/WM-RUN2.md`, but the owner text names Joe and “route tracer”, not that record: unresolved owner-to-record pointer |
| runs-once | `TickRunRecord` | — | support exists in `BUILD-packets/WM-RUN1.md`, but the owner text names Joe and “runs-once receipt”, not that record: unresolved owner-to-record pointer |

## Result

Strict record support: **16/18 sound (88.9%)**. The other two claims are not
contradicted or unsupported; they fail the requested “named record exists”
test because their owner fields do not identify the supporting records. This
is an owner-reference defect, not a reason to reclassify either declaration.

No sampled declaration was contradicted by its record, and no sampled record
had silently superseded the declaration. The `Cascade` stratum was the most
likely supersession case: the record preserves earlier candidates but marks
their withdrawal and §3e supports the current declaration.

A full manual audit is not warranted from this sample alone: all 16 resolvable
record references supported their claims, across every ownership bucket. A
separate mechanical owner-reference check is warranted before treating
`closed-by-record` as an enforced guarantee, because the two singleton buckets
show that a live holder can coexist with no machine-resolvable record pointer.
No declaration is reclassified by this audit.
