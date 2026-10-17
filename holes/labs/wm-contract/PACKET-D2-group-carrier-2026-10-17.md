# Packet D2 receipt — declared co-application-group carrier

**Date:** 2026-10-17
**Excursion:** [[E-implementing-G-over-cascades]] Packet D2
**Branch:** `packet-d2-group-carrier` (worktree `/home/joe/code/futon2-packet-d2`, base exactly main `565c3b264`)
**Status:** delivered; scope limited to `executable_cascade.clj`, `execution_journal.clj`, their tests, and this receipt. Scorer/selector/compiler/runner/debugger/Lean untouched.

## What was built

**Carrier (`futon2.aif.wm.executable-cascade`):**
- `build` accepts `:co-application-groups` — declarations
  `{:id stable-group-identity :members [occurrence-id …] :authority …}`.
  Every group is digest-bound on entry: `group-digest` (repository
  value-digest over `:id` + `:members`) is stored as `:group-digest`, and
  the whole record digest covers the group vector.
- Build-time refusals (typed, no defaults): `:co-application-group-invalid`
  with exact defects — `:group-id-absent`, `:group-id-duplicate`,
  `:group-id-collides-with-occurrence`, `:group-membership-invalid`
  (non-vector, <2 members — a 1-occurrence "group" is not a composition —
  or duplicate member within a group), `:group-member-foreign`,
  `:group-member-in-two-groups` (overlap ambiguity: each occurrence in at
  most one group).
- Validator clauses: `:co-application-groups-invalid` (all of the above,
  rechecked on the sealed record) and `:groups-digest-broken` (a group
  whose stored digest no longer binds its identity+membership).

**Journal (`futon2.aif.wm.execution-journal`):**
- `cascade-index` now derives `:groups` ONLY from the record's certified
  `:co-application-groups` carrier, verifying each group's digest. It
  refuses typed (`:journal-index-not-a-cascade`,
  `:journal-index-groups-unbound`) when the record is not the executable
  cascade carrier or a group digest does not bind. Caller-supplied group
  authority (`opts :groups`) is removed: a group the carrier did not
  declare cannot be admitted anywhere downstream.

## Gates run

- `clojure -M:test -m cognitect.test-runner -n futon2.aif.wm.executable-cascade-test -n futon2.aif.wm.execution-journal-test`
  — **29 tests, 115 assertions, 0 failures, 0 errors.**
- `clj-kondo --lint` all four files — 0 errors, 0 warnings.

## Planted controls and what catches them

| planted defect | caught by |
|---|---|
| foreign member (occurrence the cascade does not have) | build `:co-application-group-invalid` `:group-member-foreign` |
| duplicate group identity | build `:group-id-duplicate` |
| duplicate membership (one occurrence in two groups) | build `:group-member-in-two-groups` |
| group id colliding with an occurrence id | build `:group-id-collides-with-occurrence` |
| 1-member "group" | build `:group-membership-invalid` |
| tampered membership in a re-digested record | validator `:groups-digest-broken` |
| invented group with foreign member, fully re-digested | validator `:co-application-groups-invalid` |
| save/reload losing or altering groups | reload equality + re-certification |
| journal index from a non-carrier record | `cascade-index` typed refusal |
| journal index from a record with unbound group digest | `cascade-index` typed refusal |
| journal entry naming an undeclared group | `:journal-foreign-node-or-group` (declared `:group-1` admitted) |
| caller-supplied group authority | removed; index carries exactly the carrier's groups |

## Honest gaps

## Independent integration review

The root review made the declaring authority part of each group's own
digest and requires `cascade-index` to validate the complete canonical
cascade, not merely its local group list. Controls now cover authority
tampering and a locally plausible group carrier inside a broken outer
record. Reviewed gate: **29 tests, 119 assertions, 0 failures/errors**;
clj-kondo 0/0 and clean diff-check.

- The carrier declares groups; the compiler/enactment rules that a group
  enacts as a group with a group completion condition (Packet C §6 rules)
  are not implemented here — runner/compiler out of scope by requisition.
- Group ids are author-chosen stable identities (checked unique and
  non-colliding); a canonical content-derived id scheme was not imposed.
- Application-order edges inside a group are not yet rejected by the
  validator (the flattening rule belongs to the compiler boundary, and its
  carrier home is undecided); noted for the compiler packet.
