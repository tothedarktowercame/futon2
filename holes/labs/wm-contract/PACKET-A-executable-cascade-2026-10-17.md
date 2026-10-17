# Packet A receipt — canonical executable-cascade artifact and validator

**Date:** 2026-10-17 (review round 2 repairs applied same day)
**Excursion:** [[E-implementing-G-over-cascades]] Packet A
**Branch:** `packet-a-executable-cascade` (worktree `/home/joe/code/futon2-packet-a`, base `315872e0b`)
**Status:** artifact + validator + tests delivered; runner/selector/flight path NOT wired (per requisition)

## Review round 2 — defects found by codex-68's probes and repaired

The first delivery (commit 7f2935c90) certified records that violated the
agreed carrier.  Repairs, all with new adversarial controls:

1. **Occurrences only in `:nodes`.**  Void and unread citation results now
   live solely in the construction census (`:census {:open :void :unread}`),
   each entry naming its citation and observation.  They can never appear as
   nodes (validator clause `:census-population`).
2. **Genuine occurrence identity.**  `occurrence-id` no longer falls back to
   the pattern id: a node with neither an explicit `:occurrence-id` nor a
   fragment index is refused at build (`:occurrence-identity-absent`), and a
   node whose identity equals its pattern fails validation
   (`:population-occurrence-collapsed-to-pattern`).
3. **象 provenance enforced.**  Every occurrence carries reading record id,
   exact span, source-text digest, analysis digest, per-reference
   `source_sha256`, and the square's own digest; presence, digest form, and
   square-digest↔square agreement are validated (`:provenance-missing`,
   `:pins-square-invalid`).  The good fixture fails when provenance is
   removed — at build AND at validation.
4. **Gluing evidence validated, not truthy.**  A witness must conform to a
   recognised schema (`{:kind :isomorphism|:consistency, :digest <64-hex>,
   :objects [from to]}`).  `{:anything :passes}` is demoted to
   `:shared-name-candidate` by the builder and, if promoted by hand, caught
   by `:graph-gluing-witness-invalid`.  A conforming witness demoted to
   shared-name is caught by `:cascade-law-shared-name-not-gluing`.
5. **Descriptive vs. operational order.**  Arrangement `:precedes` edges are
   retained as descriptive `:reading-order-edges` (with role labels); no
   transition evidence is demanded of them and they never enter application
   order.  Application order exists only in the executable refinement, with
   independent evidence (build refusal + validator clause
   `:cascade-law-application-order-without-evidence`).
6. **Census bound, not merely recomputed.**  The structural layer carries
   its own digest and the census its own `:census-digest` over the three
   populations; the record binds both (`:structural-digest`, `:census`,
   `:census-digest`).  Substitutions fail on `:projection-census-unbound`,
   `:projection-structural-lost`, or `:identity-digest` depending on how
   much of the chain the tamperer refreshes; a fully-refreshed tamper yields
   a NEW cascade identity, detectably different from the selected digest.
7. **Append-only persistence.**  `save!` refuses when a file already exists
   at the digest path with different content; identical content returns the
   existing path.  Never overwrites (tested by simulating a collision).
8. **Adversarial tests for each exact defect** added — the R2 tests are
   marked `[R2-n]` in the test namespace.

Remaining honest limitation of §6 (recorded, not hidden): a tamperer who
re-digests the ENTIRE chain produces an internally consistent record — the
defense against that is Packet B's selection receipts binding the selected
digest, which is out of Packet A's scope.  What Packet A now guarantees is
that no partial substitution (structural content, census, census binding,
outer digest) certifies, and that the fixture itself fails without provenance.

## Schema (one canonical record)

- `:wm/structural-cascade-v1` — occurrence nodes only (occurrence identity,
  pattern identity, roles, origin, full 象 provenance, validated square),
  descriptive reading order + reading-order edges, witnessed gluings, typed
  shared-name candidates, construction order (occurrences only), census with
  its own digest, authored additions, alternatives, receipts/pins, own digest.
- `:wm/executable-cascade-v1` — executable refinement: evidenced
  application-order edges, operational clauses
  (operation/completion-check/capability), guards, frontier, journal
  reference shape (cascade-digest + node/group + phase + evidence digest).
  Binds the structural digest and census digest. No journal implementation.
- **Identity:** repository evidence digest
  (`futon2.aif.interpretation-evidence/value-digest`) over the record minus
  its `:digest`; content-addressed append-only `save!`, reload re-digests.

Namespace: `futon2.aif.wm.executable-cascade` (one canonical implementation;
no parallel validator). Constructors refuse typed for: unadmitted edge
kinds, absent occurrence identity, invalid provenance, clauses naming
non-occurrences, application-order edges without independent evidence.

## Validator clauses (`validate-cascade`)

Returns `{:status :certified :certificate …}` or
`{:status :cascade-validation-error :clause … :repair …}`; never abstention
or policy refusal. Clauses, in order: `:identity-schema`, `:identity-digest`,
`:projection-structural-lost`, `:projection-census-unbound`,
`:census-population`, `:population-duplicate-occurrence`,
`:population-occurrence-collapsed-to-pattern`, `:population-executable-set`
(executable set = exactly the occurrence set), `:provenance-missing`,
`:graph-endpoint-foreign`, `:pins-square-invalid` (square valid AND its
provenance digest matches the carried square), `:graph-gluing-witness-invalid`,
`:graph-gluing-unwitnessed` (kept for hand-built gluings with no witness at
all), `:cascade-law-shared-name-not-gluing`,
`:cascade-law-application-order-without-evidence`,
`:cascade-law-application-cycle` (gluing/shared-names excluded —
undirected), `:cascade-law-construction-order`, `:operational-coverage`.

Not yet implemented (later packets, honestly absent): G-binding receipts
verification against live scoring records (Packet B — also the full defense
against whole-chain re-digest substitution), journal-entry validation
(Packet C/D), 象 exact-projection image check against a live 象 producer
(Packet E Lean witness), void/unread production fixture from a real target
reading registry (fixture uses the real square receipt and real
scored-candidate shape, synthetic arrangement and provenance).

## Gates run (round 2)

- `clojure -M:test -m cognitect.test-runner -n futon2.aif.wm.executable-cascade-test`
  — **15 tests, 49 assertions, 0 failures, 0 errors.**
- `clj-kondo --lint` on both files — 0 errors, 0 warnings.
- Paren balance verified by reader-level check plus successful compile
  (check-parens.el's file helper is not defined in this environment).

## Adversarial controls planted and the assertions that catch them

| planted defect | caught by |
|---|---|
| void/unread citation inside `:nodes` | `structural-layer-is-the-xiang-diagram` [R2-1]; census entries are not occurrences; sneaked executable census node → `:population-executable-set` |
| occurrence identity collapsing to pattern id | build refusal `:occurrence-identity-absent` [R2-2]; hand collapse → `:population-occurrence-collapsed-to-pattern` |
| provenance removed / malformed / square-digest mismatch | build refusal `:provenance-invalid`; `:provenance-missing`; `:pins-square-invalid` [R2-3] |
| truthy-but-bogus gluing witness | demoted to `:shared-name-candidate` at build; hand promotion → `:graph-gluing-witness-invalid` [R2-4]; conforming witness demoted → `:cascade-law-shared-name-not-gluing` |
| `:precedes` read as application order / evidence demanded of descriptive edges | `:reading-order-edges` retained descriptive [R2-5]; evidence-less application order refused at build and caught by `:cascade-law-application-order-without-evidence` |
| census substitution re-digested | `:projection-census-unbound` (record copy), `:identity-digest`/`:projection-structural-lost` (structural layer) [R2-6] |
| `save!` overwrite | identical → same path; different content at digest path → refused [R2-7] |
| repeated citations collapsing | 2 occurrences through nodes, census, distinct reading-record ids, executable set, projection |
| readings keyed by pattern id | census shifts to unread; asserted directly [R2-8] |
| undirected gluing in directed cycle check | gluing + reverse application edge certifies; genuine cycle → `:cascade-law-application-cycle` |
| projection losing structure | exact equality `projection-loses-nothing` |
| tampering without/with re-digesting | `:identity-digest`; fully-refreshed tamper = new identity, digest inequality asserted |
| save/reload identity drift | reload equality, re-digest, re-validate |
| validator emitting refusal/abstention | every failure is `:cascade-validation-error` |

## Remaining dependencies

- Packet B: emit canonical candidates before G; bind selection receipts to
  this digest (the full answer to whole-chain substitution).
- Packet C: refinement→forgetful-projection Lean pairing; execution journal
  per `journal-reference-shape`.
- Packet D: the five seam error classes in the debugger/click loop.
- Packet E: Lean identity-chain witness + negative modes; only then retire
  `:reading-policy-execution-semantics-owed` and `CascadeGrainSeam.owed`.
- A real-target fixture (live reading registry occurrence data) should
  replace the synthetic arrangement/provenance before Packet A's exit gate
  is claimed complete.

