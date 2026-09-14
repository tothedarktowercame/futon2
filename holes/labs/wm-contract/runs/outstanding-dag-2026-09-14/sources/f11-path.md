# Row 24/25: minimal truthful F11 certificate path

Status: read-only source review and implementation plan. No emitter, Lean,
runtime, certificate, tracker, or manifest was changed.

## Existing executable path

`derive_certificate_controls.bb` loads `derive_certificate.bb` directly and
constructs its config in `base-config`. `derive-certificate` checks configured
hashes, seven checkpoint shape/order, deliverable/selection shape, route
conformance, one or more equation bindings, and its historical exact scope;
`emit!` then writes EDN and JSON. This ends at `:wm/runtime-certificate-v1`.
There is no Clojure adapter or generator in the repository that constructs
`CertificateStates.FullAttestation`, imports the additive predicate chain, or
asks Lean to decide it.

Two honest false results are executable today, but neither is the requested
end-to-end result. `genuine-original` returns
`:deliverable-chain-incomplete`; `genuine-commission` returns
`:checkpoint-chain-incomplete`. These are acquisition refusals, not complete
typed censuses. Separately, `FullCertificatePredicate.leadCounterexample` is a
complete Lean value whose `FullQualifyingRun` is false because required record,
connection, selection, equation and negative-scope conditions fail. It is a
fixture, not derived from emitter bytes. Relabelling either partial acquisition
as `CensusComplete` would be false.

The accepted Lean chain is:

`CertificateStates` → `FullCertificatePredicate` →
`FullCertificateRunBinding` → `FullCertificateEventBinding` →
`FullCertificateRecordConnectionBinding` →
`FullCertificateResolvedEvidence` → `FullCertificateCrossLayerBinding`.

Each refinement implies `FullQualifyingRun`; none authenticates producer
inputs. The existing emitter neither populates nor calls this chain.

## Minimal implementation diff plan

1. Add `src/futon2/aif/full_attestation_input.clj` (new file). Its sole public
   API is `acquire`, accepting a separately configured authority manifest plus
   candidate source descriptors. It uses `authority-buffer/capture!` once per
   source. It returns either `{:status :acquired :input ...}` or a typed
   acquisition refusal. It must never return a `FullAttestation` when a required
   universe or family is unavailable.
2. Add `holes/labs/wm-contract/emit_full_attestation.bb` (new file). It converts
   only `:acquired` input to occurrence-preserving Lean literals for the
   existing structures, including the typed census and every refinement input.
   It emits a per-run owned module with `#eval decide`/theorem for
   `CensusComplete` and, independently, `¬ CrossLayerQualifyingRun ...` when
   the acquired positive obligations do not all hold. It must not turn a
   refusal into a negative attestation.
3. Add one call at the final semantic gate in `derive-certificate`, after
   equation binding and before the output map. The call consumes the already
   retained byte captures rather than reopening paths. The certificate embeds
   the typed input digest, generated-module digest, Lean exit and boolean
   result. `emit!` stays after this gate.
4. Add focused controls; do not create a positive full-run fixture. The first
   accepted integration target is a fully acquired census with an honestly
   false qualifying predicate and a named negative state.

This is the smallest plan because a generated false theorem is useful only
after the full census universe has been acquired. A shorter adapter that fills
missing lists with `[]` proves a proposition about an invented universe.

## Required acquisition versus parked apparatus

Required now: independently pinned node/connection/equation/divergence
universes; fixed run/event identities; all seven family summaries and complete
member lists; exact claim/declaration/review provenance; corrected semantic
edge authority and legacy-ID mapping; payload-pointer resolution; lifecycle-
specific causal/timestamp relations; and a pinned Lean toolchain/module chain.
These are row-24 predicate inputs and row-25 tamper-detector inputs.

The parked E6b store/carrier/codec/provenance/completeness apparatus is not a
blanket prerequisite. It becomes required only if a declared row-24 family,
node claim, or E6 edge specifically selects an E6b artifact as its authoritative
record/provenance source. Its mere existence cannot supply the missing census
authority, and absence of that store cannot be converted into completeness.
Rows 14, 18, 19 and 22 remain real capability/evidence dependencies where the
required universes name them; this plan does not waive them.

## Tamper tests

- authority-manifest path or digest changed after capture;
- required node/edge/equation removed, duplicated or reordered;
- a missing record family changed to an empty fabricated summary;
- underlying family member omitted, duplicated, cross-run, or byte-mutated;
- selected/enacted action equal but occurrence differs;
- divergence authority borrowed from another occurrence/run/class;
- equation claim paired with another declaration or stale bytes;
- connection classification/endpoints/semantic ID changed while retaining its
  evidence string;
- causal target changed to another existing but wrong-family record;
- trace/tick, checkpoint/close, dispatch/park or review/claim identifier changed;
- timestamp relation violated according to that lifecycle edge;
- unauthorized negative-scope item added, or live `F_pi`/R8 hidden behind the
  sole legacy scalar retirement;
- generated Lean source, imported olean, or toolchain pin changed;
- compiler failure, `sorryAx`, missing theorem, or a generated `true` where the
  expected commissioned result is false.

Every acquisition tamper must refuse before Lean generation. Predicate tamper
fixtures must elaborate only when the named bad input is rejected. No absence
test is an authorization to claim full census.

## Source pins

Exact pins and read-only commands are retained under
`runs/row-24-f11-false-certificate-path-2026-09-13/`. At inspection there was
no current executable derive-certificate-to-FullAttestation bridge.
