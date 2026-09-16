# WM-01 carrier delivery

Prepared by codex-8 for Joe, 2026-09-16. Item owner: zai-9.
Subject: `WM-01-carrier-contract` in the existing closure DAG, not full WM-01.
Status: submitted for independent composite review. No implementation changes.

## Delivered interface

`futon2.aif.machine-model/distribution-admission [row support]` is the public
full-row boundary. `support` must be a nonempty vector with distinct identities;
the row must be a map with exactly those keys. Extra keys are refused even when
their mass is zero. Support order and identity-to-mass assignments are preserved.
Reordering support does not reorder values between identities.

The boundary calls `numeric-row-admission`, which accepts nonnegative integers,
ratios, BigDecimal, and finite Float/Double values. Integer, ratio and decimal
values are interpreted exactly; IEEE values are interpreted at their exact
represented binary values. Unsupported types, negative and nonfinite values
refuse. Summation and deviation are exact rational arithmetic.

| Representation | Admission criterion | Legacy tag when admitted |
|---|---|---|
| Only integer/ratio | Exact total equals one | `:exact` |
| Any decimal or IEEE value, including mixed rows | Absolute deviation at most `1/1000000000000` | `:float-carried` |

The second row includes exact decimal representations; the legacy tag does not
mean a row contains IEEE values or has a nonunit sum. Read `:representations`,
`:representation`, `:exact-total`, `:exact-deviation`, `:exactly-normalized?` and
`:criterion` separately. The criterion includes its identity and revision.
Admission never renormalizes. Float32 is widened value-preservingly to Double
only in returned evidence `:values` so EDN round-tripping preserves its value;
`:representations` retains the Float32 tag and the caller's row is unchanged.

Failures have `:ok false` and a typed `:refusal`. Downstream code must inspect
`:ok`; evidence fields on a refused numeric row do not authorize its use.
`numeric-row-admission` alone does not check a supplied support.
`row-sum-admission` is a compatibility projection, not full distribution admission.

## Formal laws and their relation to the runtime interface

`DarkTower.WarMachine.MachineModelSpec.FloatCarriedRow` has rational mass,
nonnegativity, duplicate-free list support, zero mass off support and the same
`1/10^12` absolute sum bound. Nonempty support follows from that bound.
A runtime finite row can be described mathematically by its rational represented
values on the admitted support and zero outside it. This is the contract's
mathematical interpretation, not a new executable total-map adapter or a universal
proof of the Clojure implementation.

`FloatCarriedRow.toProbabilityKernel` requires an explicit exact-total-one proof;
it preserves coordinates and casts rational masses into the reals. Approximate
admission alone does not supply this premise. A normalized decimal/IEEE row can
satisfy it despite the runtime legacy `:float-carried` tag. A nonexact admitted
row cannot be treated as an exact kernel without additional justified work.

The same retained seven-status binary64 row is exercised by
`machine-belief-test/reader-exposes-approximate-production-row-admission` and
represented in `FloatCarriedRowCorrespondence.retainedRow`. Its total is
`36028797018963969/36028797018963968`, deviation `1/2^55`: admitted and nonexact.
The historical independent formal review checked every rational coordinate
against Python `Fraction(double)` and rejected exact conversion of that row.
This packet freshly reruns the runtime reader test; it does not rerun Lean.

## Availability to downstream implementation

| Interface / declaration | Location | Bounded evidence |
|---|---|---|
| Full-row and numeric admission, common support check | `src/futon2/aif/machine_model.clj` | numeric-1 and support-1 accepted; current bytes match support-1 |
| Single-entity reader | `src/futon2/aif/machine_belief.clj` | calls full-row boundary; retains original posterior, model/context and numeric evidence |
| State predictor | `src/futon2/aif/machine_predictive.clj` | calls full-row boundary at initial and every produced row; rejects malformed support before transition |
| Approximate carrier and exact conversion | `mathlib4/DarkTower/WarMachine/MachineModelSpec.lean` | float-carrier-1 accepted; current bytes match |
| Concrete retained-row representation | `mathlib4/DarkTower/WarMachine/FloatCarriedRowCorrespondence.lean` | nonexact row and coordinate theorems accepted; current bytes match |

These are existing callable interfaces and formal declarations. They are
available for downstream bounded implementation against the pinned source.
The row boundary does not invent State/Action/Outcome meanings or model identity.
The single-entity reader's canonical status order and the predictor's policy/model
checks remain additional consumer conditions. Arbitrary support permutations
admitted by the generic boundary need not be accepted by that specialized reader.
New domains, interpreted dynamics and shared identities across all actual
consumers remain the respective delivery/closure obligations.

## Acceptance basis and fresh verification

- numeric-1: futon2 `ac821857` + `7f546b63`; independent review `00915661`.
- support-1: futon2 `9aad9adf`; independent review `0e5f5545`.
- float-carrier-1: mathlib4 `f40c936a64`, futon2 receipts `6f494738`;
  independent review `e99c08c4`.
- `source-verification.json` compares all six accepted support source/test files
  and all three accepted formal source files with their retained hashes: 9/9 match.
- Fresh isolated tests: machine-model 11 tests/319 assertions; machine-belief
  6/45; machine-predictive 6/63; all pass. The model invocation was captured by
  the agent tool transcript; the other two raw outputs and commands are retained
  here in `test-results.json` and the `.log` files. These are existing tests,
  including support-shape, negative/nonfinite/extra-key refusals, exact and
  approximate cases, support permutation, retained row and predictor refusals.
- `owner-response.json` retains zai-9's finding that composite acceptance is the
  missing step. `coordinator-response.json` confirms no overlapping WM-01 work.

## Exact acceptance requested

Determine whether the three already accepted subclauses jointly satisfy the
existing DAG acceptance: shared support and represented-value admission
interfaces, exact versus approximate laws, available downstream. Review the
interface agreement and the same-row correspondence above; identify a concrete
missing obligation if the delivery cannot be accepted.

Acceptance does not establish composed prediction/KL/ordering bounds, universal
runtime/formal correspondence, all proof/witness bindings, R4 admission, registry
publication, measured A/B, actual serving use or full WM-01 closure. It does not
dispatch a downstream task or alter its other prerequisites. No source, runtime,
registry, checklist, preference or WM-06 changes are part of this delivery.
