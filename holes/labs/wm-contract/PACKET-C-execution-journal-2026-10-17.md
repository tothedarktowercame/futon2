# Packet C receipt — append-only execution journal

**Date:** 2026-10-17
**Excursion:** [[E-implementing-G-over-cascades]] Packet C (execution journal ONLY)
**Branch:** `packet-c-execution-journal` (worktree `/home/joe/code/futon2-packet-c`, base exactly `92295c522` = main)
**Status:** journal delivered; scorer/selector/cascade_shape_g, runner enactment, `executable_cascade.clj`, and Lean untouched (required read-only via `futon2.aif.wm.executable-cascade/journal-reference-shape`, never edited)

## What was built

Namespace `futon2.aif.wm.execution-journal`:

- **Entry schema** `:wm/execution-journal-entry-v1`, bound to Packet A's
  `journal-reference-shape`: `:journal-id` (the canonical cascade digest,
  64-hex), `:node-or-group` (one occurrence id OR one declared
  co-application group), `:phase` (`:attempted/:enacted/:open-hole/:closed/:sorry`),
  `:evidence {:kind :digest}` (kind from a recognized set, digest 64-hex),
  `:valid-time` and `:recorded-time` (ISO-8601 instants — Minus40°DB's
  valid-time/recorded-time model), optional `:supersedes` (an entry-id) and
  `:sorry` (a contradiction record).  Every entry is sealed by its own
  content digest (`:entry-id`, repository value-digest).
- **Append-only store**: `append!` writes each entry to
  `<entry-id>.edn`.  Identical existing content → idempotent success
  (`:appended false`); differing content at an entry-id → typed collision
  refusal; never overwrites or removes.  Malformed or unsealed entries are
  refused before any IO.
- **Load**: `load-journal` reconstructs append order from the strictly
  increasing recorded time required by `append!`, verifying each entry's
  shape and seal.  A tampered file is a typed
  `:journal-validation-error` (`:journal-entry-seal-broken` /
  `:journal-entry-invalid`), never silently dropped or repaired.
- **Validation against the cascade**: `validate-journal` checks (in order)
  `:journal-foreign-cascade` (every entry names the index digest),
  `:journal-foreign-node-or-group` (node or declared group admitted),
  `:journal-recorded-time-inversion` (over entries AS GIVEN — append
  order — so history written backwards is seen), `:journal-valid-time-inversion`
  (per node/group), `:journal-supersede-unknown`,
  `:journal-supersede-foreign` (cross-journal supersede),
  `:journal-supersede-conflict` (double supersede).  Returns a sealed
  certificate or typed error — never abstention or policy refusal.
- **Replay**: `replay` infers enactment ONLY from `:enacted` entries with
  `:verified-effect` evidence that are not superseded.  Superseded entries
  contribute nothing; the original entries stay byte-identical (supersession
  is a NEW entry).  Open holes carry as an open continuation of the same
  cascade (`:open-cascade?`) — reaching a design hole is a recorded fact,
  not a failure.  Sorries record contradictions without mutation.
- **Cascade index**: `cascade-index` builds the validation index from a
  Packet A record (digest + executable-node occurrence ids), with declared
  co-application groups supplied by the caller (Packet A's carrier does not
  yet name groups — an honest gap, below).

## Gates run

- `clojure -M:test -m cognitect.test-runner -n futon2.aif.wm.execution-journal-test`
  — **9 tests, 44 assertions, 0 failures, 0 errors.**
- `clj-kondo --lint` on both files — 0 errors, 0 warnings.
- Reader-level paren balance plus successful compile.

## Planted controls and what catches them

| planted defect | caught by |
|---|---|
| malformed entry (bad digest form, bad timestamps, unknown phase/evidence kind) | `entry-shape-and-seal` — construction refusal with the exact defect list |
| duplicate append of the SAME entry | idempotent `:appended false`, not a collision (`save-reload-and-idempotent-append`) |
| forged entry reusing an existing entry-id with different content | `append!` refuses (`journal-entry-id-collision`); never overwrites |
| tampered stored file | `load-journal` → `:journal-entry-seal-broken` |
| entry naming another cascade | `:journal-foreign-cascade` |
| entry naming an unknown node / undeclared group | `:journal-foreign-node-or-group` (declared group `:group-1` admitted) |
| recorded-time running backwards | `:journal-recorded-time-inversion` (validated in append order) |
| valid-time running backwards for the SAME node, including with another node interleaved | `:journal-valid-time-inversion` (interleaving across nodes is free) |
| reversed filesystem modification times | load order remains recorded-time order; equal/backward append time is refused |
| supersede of an unknown entry | `:journal-supersede-unknown` |
| supersede across journals | `:journal-supersede-foreign` |
| double supersede of one entry | `:journal-supersede-conflict` |
| supersession mutating history | original entry unchanged on disk; replay drops only the superseded entry |
| enactment inferred from an open hole | replay: `:open-holes` carried, `:enacted` empty; continuation entry later joins the same journal |
| `:enacted` label paired with non-effect evidence | construction refuses `:phase-evidence-mismatch` |
| sorry mutating either side | sorry is a phase-`:sorry` entry with `:sorry` contradiction map; both sides stay; `:sorries` in replay |

## Honest gaps

- Co-application groups are DECLARED to `validate-journal` via the index;
  Packet A's saved record does not yet carry declared group identities, so
  no code path derives them from the record.  Wiring the declaration into
  the Packet A carrier (a carrier edit, out of scope here) closes this.
- No runner/enactment integration, no debugger/click-loop error classes
  (Packet D), no Lean witness (Packet E), by requisition.

## Independent integration review

The root review rejected filesystem modification time as an append-order
authority and found that adjacent-pair validation missed same-node
valid-time inversions separated by another node. It also rejected certifying
an enacted label carrying only attempt evidence. Recorded time is now a
strict, unique append sequence; each complete node/group history is checked;
phase/evidence pairs are contractual; and supersession stays on one
node/group. The reviewed gates above are the post-repair results.
