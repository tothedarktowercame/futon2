# Independent review — executable typed-gap generator

Reviewer: codex-24. Subject: lead commit `16e0116e` following incomplete
`4678d697`. This review does not rerun the retained passing gates.

## Pin and receipt verification

All twelve entries in `source-pins.json` match current bytes: generator,
controls, input, generated Lean, seven imported Lean sources, and toolchain.
The retained outputs report 2 tests / 18 assertions with zero failures/errors,
generator exit 0, Lean exit 0, three axiom reports and no `sorryAx`, kondo and
parens exit 0. The tampered Lean file exits 1: removing the family entries makes
both `CensusComplete att` and the explicit full-loop typed-gap membership proof
false. The final generated source digest is
`afb80fc611b743502ef23a77d8e254bdde9014222ff7ca22de2153ba712d04fc`,
matching the pinned compiled subject.

## Source/input correspondence

The generated `att` preserves the fixture's escaped node id, claim, scope,
connection id/scope, selection refusal reason, and all seven family reasons.
Declared node and connection lists equal their entry-id projections in input
and output. Families occur exactly in `CertificateStates.allRecordFamilies`
order. `census` is `by decide`. `rejects` applies the accepted
`rejects_missing_record_family` theorem to the explicitly supplied
`.fullLoopCheckpoints` gap for arbitrary `req` and `ev`; `rejects_cross`
contradicts `crossLayer_implies_full`. These theorem applications match the
imported signatures and do not use `fixtureReq` or claim positive qualification.

The strict subset is enforced: isolated scope, `:authority/status :none`, exact
top-level and constructor-specific key sets, nonempty unique ordered node and
connection ids, canonical seven-family order, only unvalidated/typed-absence
nodes, mandatory-unfired connections, refused-shape selection, and typed-gap
families. Unsupported constructors refuse rather than being rewritten.
Strict UTF-8, exact-one-form EDN and SHA-256 equality are checked before
generation. Lean string escaping covers backslash, quote, newline, return and
tab; surrogate code units are explicitly refused.

## Commissioned additional control

One new targeted control inserted a second nonempty `:scope` key into the
pinned fixture, recomputed its integrity digest, and invoked the CLI. It exited
1 with `IllegalArgumentException: Duplicate key: :scope`; no output Lean file
was created. This confirms duplicate EDN fields cannot silently overwrite the
strict schema.

Finding F1 (non-blocking for isolated mechanics, required before embedding the
CLI in another machine consumer): duplicate/malformed EDN failures escape as
reader exceptions and a stack trace rather than the script's structured
`:refusal` data. The current controls intentionally accept any exception.
Normalize reader failures at the CLI boundary if a downstream process needs a
stable typed refusal protocol.

## Verdict and boundary

Accepted for isolated, untrusted structural mechanics only. The CLI accurately
reports `:generated-only`, `:scope :isolated`, `:authority/status :none`, and
`:lean-checked? false`; compilation is a separate retained gate. Its input hash
is integrity, not source authority. It neither acquires a real universe nor
creates a production/full certificate. F11 acquisition, provenance, and
consumer integration remain absent; no row closure or admission follows.
