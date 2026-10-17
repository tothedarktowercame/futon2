# Packet D1 receipt: enactment compiler

**Excursion:** E-implementing-G-over-cascades, Packet D1
**Worktree:** `/home/joe/code/futon2-pktd1-zai7`, branch
`zai-7/packet-d1-compiler`, base `565c3b264` (main).
**Agent:** zai-7 (requisition invoke-1792196902412-15978 from codex-68).
**Date:** 2026-10-17

## What was built

`src/futon2/aif/wm/cascade-enactment-compiler.clj` (new, standalone — no
edits to `executable_cascade.clj`, `execution_journal.clj`,
`family_selection.clj`, runner/debugger/click-loop, or Lean):

`certified-structural?` — a structural cascade certifies when its
all-holes refinement builds and passes the one Packet A validator (a
durable partial design with explicit holes).

`compile-executable [structural authoring]` — emits the Packet A
executable inputs plus the built, validator-certified canonical record:

- authoring must be **attributed** (`:attribution {:author :authority}`);
  unattributed authoring is refused (`:authoring-unattributed`);
- application order comes only from authored edges with **independent
  operational evidence** (`:witnessed-transition`,
  `:observed-state-transition`, `:product-receipt`); descriptive evidence
  kinds (`:reading-order`, `:fragment-order`, `:construction-order`,
  `:precedence-relation`, `:scope-nesting`, `:gluing`,
  `:gluing-isomorphism`, `:shared-name`) are refused
  (`:descriptive-evidence-not-operational`), unknown kinds refused
  (`:evidence-kind-unknown`), missing evidence refused
  (`:application-order-evidence-owed`), cycles refused
  (`:application-cycle`), non-occurrence endpoints refused
  (`:foreign-occurrence`);
- operation / capability / consumes / produces pass through attributed and
  stamped `:authoring/attribution`; the compiler never invents them;
- completion checks derive from the square only when the square is present
  and validated (`[:square-B-observed <the square's actual B elems>]`);
  an authored square-derived completion the square does not back is refused
  (`:fabricated-square-derivation`);
- every occurrence without a complete clause keeps an **explicit open hole**
  (`:owner :wm/enactment-compiler`, `:re-entry :packet-d1-refinement`).

Refinement is a re-compile with fuller authoring: a NEW record/digest over
the SAME structural layer — the 象 projection identity is preserved and the
earlier record is never mutated (asserted in tests).

## Gates

- `cascade-enactment-compiler-test` (NEW): 11 tests / 45 assertions,
  0 failures
- regression at base: `executable-cascade-test` +
  `selection-cascade-identity-test`: 23 / 102, 0 failures
- clj-kondo 0/0 on both new files; check-parens clean.

## Controls (all planted and caught)

descriptive-edge leakage (reading-order and gluing-isomorphism evidence
refused) · missing transition evidence · unknown evidence kind · foreign
occurrence (clause key and edge endpoint) · fabricated square derivation
(completion claiming B elements the square does not carry) · cyclic
application order · unattributed authoring · uncertified structural input ·
malformed/missing transition receipt digest · open-cascade refinement without mutation (digest changes, structural
identical, prior record still certifies, compilation deterministic).

Independent integration review added mandatory 64-hex receipt identity to
every operational edge. A recognized evidence-kind label alone no longer
counts as independent transition evidence.

## Fixture

Production-shaped repeated occurrence: p/dup cited at fragments 0/1/2
(distinct occurrence ids), p/other sharing fragment 1 (witnessed :overlap
gluing), descriptive edges with the real relation kinds (:context-enables,
:precedes), a void and an unread citation in the construction census, and
the real three-halves square receipt fixture as each occurrence's 象 square.

## Honest gaps

- Guards authored as clauses pass through but the compiler performs no
  guard-vs-state admissibility check (that needs observed state; Packet
  C/D2 territory).
- No wiring into runner/enactment/journal (forbidden here); the compiler is
  a pure library for Packet C's flight boundary to call.
