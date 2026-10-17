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
- **Load**: `load-journal` reads entries in append order (file mtime,
  equal-mtime batches ordered by recorded-time), verifying each entry's
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
  — **8 tests, 40 assertions, 0 failures, 0 errors.**
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
| valid-time running backwards for the SAME node | `:journal-valid-time-inversion` (interleaving across nodes is free) |
| supersede of an unknown entry | `:journal-supersede-unknown` |
| supersede across journals | `:journal-supersede-foreign` |
| double supersede of one entry | `:journal-supersede-conflict` |
| supersession mutating history | original entry unchanged on disk; replay drops only the superseded entry |
| enactment inferred from an open hole | replay: `:open-holes` carried, `:enacted` empty; continuation entry later joins the same journal |
| enactment inferred from an `:enacted` label without `:verified-effect` evidence | replay checks the evidence kind, not the label |
| sorry mutating either side | sorry is a phase-`:sorry` entry with `:sorry` contradiction map; both sides stay; `:sorries` in replay |

## Honest gaps

- Co-application groups are DECLARED to `validate-journal` via the index;
  Packet A's saved record does not yet carry declared group identities, so
  no code path derives them from the record.  Wiring the declaration into
  the Packet A carrier (a carrier edit, out of scope here) closes this.
- `load-journal` orders equal-mtime write batches by recorded-time; a
  tamperer who rewrites a whole batch in one instant with inverted
  recorded-times within the batch would read in the rewritten order and
  pass the recorded-time clause.  The seal clauses still catch any content
  change to an existing entry-id; this is a timestamp-granularity limit,
  recorded here rather than hidden.
- No runner/enactment integration, no debugger/click-loop error classes
  (Packet D), no Lean witness (Packet E), by requisition.
