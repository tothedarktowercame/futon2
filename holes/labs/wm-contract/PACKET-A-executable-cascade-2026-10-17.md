# Packet A receipt — canonical executable-cascade artifact and validator

**Date:** 2026-10-17
**Excursion:** [[E-implementing-G-over-cascades]] Packet A
**Branch:** `packet-a-executable-cascade` (worktree `/home/joe/code/futon2-packet-a`, base `315872e0b`)
**Status:** artifact + validator + tests delivered; runner/selector/flight path NOT wired (per requisition)

## Schema (one canonical record)

- `:wm/structural-cascade-v1` — the 象-shaped structural layer: occurrence-keyed
  nodes (occurrence identity `[pattern fragment-index]` kept distinct from
  pattern identity; per-node pinned `:wm/three-halves-square-v1` receipt),
  descriptive `:reading-order`, witnessed `:gluing`, typed
  `:shared-name-candidates` (unwitnessed shared names), `:construction-order`,
  `:authored-additions`, `:alternatives`, `:receipts` (pins).
  Census statuses `:void` / `:unread` / `:open` stay mutually exclusive; only
  `:open` occurrences exist as executable nodes.
- `:wm/executable-cascade-v1` — the executable refinement of that layer:
  evidenced `:application-order` edges, operational clauses
  (operation/completion-check/capability), `:guards`, computed `:frontier`,
  `:census`, `:journal-reference` (shape binding every future journal entry:
  cascade-digest + node/group + phase + evidence digest). No journal
  implementation, per requisition.
- **Identity:** content digest = repository evidence digest
  (`futon2.aif.interpretation-evidence/value-digest`) over the record minus its
  `:digest`. Records are append-never; `save!`/`load-record` are
  content-addressed (file name = digest) and reload re-digests or refuses.

Namespace: `futon2.aif.wm.executable-cascade` (one canonical implementation;
no parallel validator). Constructors refuse typed — unknown edge kinds,
application-order edges without transition evidence, clauses naming non-`:open`
occurrences — rather than inventing capability, order, operation, or gluing.

## Validator clauses (`validate-cascade`)

Returns `{:status :certified :certificate …}` or
`{:status :cascade-validation-error :clause … :repair …}`; never abstention or
policy refusal. Clauses, in order: `:identity-schema`, `:identity-digest`,
`:projection-structural-lost` (structural layer present, correct schema, and
`:census` agrees with the actual node statuses), `:population-duplicate-occurrence`,
`:population-unknown-status`, `:population-occurrence-collapsed-to-pattern`,
`:population-executable-set` (executable set = exactly the `:open` set),
`:graph-endpoint-foreign`, `:pins-square-invalid` (every carried square is a
valid `three-halves-square` receipt), `:graph-gluing-unwitnessed`,
`:cascade-law-shared-name-not-gluing`,
`:cascade-law-application-order-without-evidence` (reading order can never
satisfy an application-order obligation), `:cascade-law-application-cycle`
(gluing/shared-names excluded — undirected), `:cascade-law-construction-order`
(every occurrence named exactly once), `:operational-coverage` (enactable ⇒
usable clause + completion check; open-hole ⇒ explicit hole).

Not yet implemented (later packets, honestly absent here): G-binding receipts
verification against live scoring records (Packet B), journal-entry validation
(Packet C/D), 象 exact-projection image check against a live 象 producer
(Packet E Lean witness), void/unread production fixture from a real target
reading registry (the fixture uses the real square receipt file and the real
scored-candidate shape, but a synthetic arrangement).

## Gates run

- `clojure -M:test -m cognitect.test-runner -n futon2.aif.wm.executable-cascade-test`
  — **12 tests, 34 assertions, 0 failures, 0 errors.**
- `clj-kondo --lint` on both new files — 0 errors, 0 warnings.
- Paren balance checked (harness `check-parens.el` helper unavailable in this
  environment; a reader-level balance check plus `clojure` compiling the file
  via the test run stand in).

## Adversarial controls planted and the assertions that catch them

| planted defect | caught by |
|---|---|
| repeated citations of one pattern collapsing | `structural-layer-is-the-xiang-diagram`, `repeated-citations-do-not-collapse-through-anything` (2 occurrences through square lookup, executable set, projection) |
| squares keyed by pattern id (not occurrence) | `pattern-id-keyed-squares-fail` (census shifts to 3 unread; a carried-over census is caught by `:projection-structural-lost`) |
| unread/void citation promoted to executable node | build refusal `:clause-for-non-open-occurrence`; hand-sealed sneak caught by `:population-executable-set` |
| reading order standing in for application order | build refusal `:application-order-evidence-owed`; hand-sealed evidence-stripped record caught by `:cascade-law-application-order-without-evidence` |
| undirected gluing counted in the directed cycle check | `undirected-gluing…` certifies gluing + reverse application edge; a genuine directed cycle is caught by `:cascade-law-application-cycle` |
| unwitnessed shared name promoted to gluing | builder keeps it as `:shared-name-candidate`; hand promotion caught by `:graph-gluing-unwitnessed`; a witness on a shared-name-candidate is caught by `:cascade-law-shared-name-not-gluing` |
| projection losing structure | `projection-loses-nothing` (exact equality); census drift caught by `:projection-structural-lost` |
| tampering (edge/node/census changes) without re-digesting | `:identity-digest`; with re-digesting, the dependent clauses fire in sequence (`:projection-structural-lost` → `:population-executable-set` → `:graph-endpoint-foreign`/`:cascade-law-construction-order`) |
| save/reload identity drift | `save-reload-identity-stability` (byte-stable EDN, re-digest, re-validate) |
| validator emitting refusal/abstention | `validator-never-abstains` (every failure is `:cascade-validation-error`) |

## Remaining dependencies

- Packet B: emit canonical candidates before G; verify selection receipts against this digest.
- Packet C: refinement→forgetful-projection Lean pairing; execution journal per `journal-reference-shape`.
- Packet D: the five seam error classes in the debugger/click loop.
- Packet E: Lean identity-chain witness + negative modes; only then retire
  `:reading-policy-execution-semantics-owed` and `CascadeGrainSeam.owed`.
- A real-target fixture (live reading registry occurrence data) should replace
  the synthetic arrangement before Packet A's exit gate is claimed complete.
