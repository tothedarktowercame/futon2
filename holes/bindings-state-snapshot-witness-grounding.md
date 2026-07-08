# Bindings grounding: state-snapshot-witness (`futon3c-d/mission/state-snapshot-witness`)

**Author:** zai-10 (reviewed lane — claude-4 reviews + runs build-match).
**CLean source:** `futon6/holes/clean/state-snapshot-witness.clean.edn` (5 boxes, linear build, hole at s4, terminal at s5).
**Method:** spec-derived from box `:produces` meaning + `:text`, cross-referenced with substrate-2
schema (entity types explored via authed Drawbridge, read-only). NOT reverse-engineered.

## Per-box binding derivation

### s1 — `:produces :snapshot-event-shape`

- **Box text:** "Shape-first decision: a SEPARATE `:event :state-snapshot` type (namespace-discriminated subtype `:inventory-snapshot`)"
- **Binding:** **unbound: abstract**
- **Why:** This box produces a TYPE DECISION — the shape of a new event subtype. It is a
  design choice ("separate type, not an extension of `:family-fired`"), not an instance
  of a substrate entity. The `:produces :snapshot-event-shape` names a schema definition,
  not a populated node. No substrate entity is created by deciding the shape.

### s2 — `:produces :boot-reachable-snapshot`

- **Box text:** "snapshot-inventory! is wired into bootstrap.clj... The snapshot evidence thereby becomes the structural witness that boot-time construction actually worked"
- **Binding:** `{:kind :entity :type :evidence}`
- **Why:** The box explicitly produces a snapshot EVIDENCE entry — "the snapshot evidence
  thereby becomes the structural witness". In the source (`snapshot.clj:224-235`),
  `emit-snapshot!` calls `boundary/append!` with `:subject {:ref/type :pattern :ref/id
  "state-snapshot/inventory"}`, creating an evidence entry. The substrate entity type for
  evidence entries is `:evidence` (confirmed inhabited in substrate-2). The box meaning says
  the snapshot IS evidence; `:evidence` is its type.

### s3 — `:produces :cycle-witness`

- **Box text:** "snapshot-inventory! reads docs/structural-law-inventory.sexp and projects each family + sibling-invariant to a flat map — one per-cycle full-state projection"
- **Binding:** **unbound: abstract**
- **Why:** The box produces the PROJECTION — a flat map of family records. This is data
  WITHIN the s2 evidence entry (the `:state` field in `emit-snapshot!`'s body), not a
  separate substrate entity. The `:produces :cycle-witness` names the content/payload of
  the snapshot, not a distinct typed node. The witness is the evidence entry (s2); the
  cycle-witness is what that entry carries.

### s4 — `:produces :snapshot-evidence`

- **Box text:** "Emit exactly one `:event :inventory-snapshot` evidence entry through the boundary, tagged `[:invariant-queue :state-snapshot :inventory]`, subject `:ref/type :pattern :ref/id state-snapshot/inventory`"
- **Binding:** `{:kind :entity :type :evidence}`
- **Why:** This box is the emission step — it explicitly produces an evidence entry through
  the boundary. Same substance as s2 (the evidence entity), but from the emission-path
  perspective. The box is a `:sorry` hole (`:discharge :queryAnswer`), but its `:produces`
  is concretely `:snapshot-evidence` — an entity that should exist in the evidence store.
  The `:evidence` entity type is its substrate manifestation.

### s5 — `:produces :reconstructable-history` (TERMINAL, `:discharges`)

- **Box text:** "Composition with the family-canary: the canary emits per-family deltas, this snapshot emits per-cycle state; together they reconstruct full history"
- **Binding:** `{:kind :entity :type :evidence}`
- **Why:** The terminal's `:produces :reconstructable-history` is the RECONSTITUTION surface
  — the evidence store from which full history can be reconstructed (snapshots + deltas).
  The substrate manifestation of "reconstructable history" is the collection of `:evidence`
  entities that enables reconstruction. The terminal discharges to `:tracer-track-4-2-closed`,
  and the closure is recorded as evidence. The closest single-type substrate manifestation
  is the `:evidence` entity — the type that holds both snapshots and deltas.

## Summary

| Box | `:produces` | Binding | Why |
|-----|-------------|---------|-----|
| s1 | :snapshot-event-shape | unbound: abstract | type-decision/shape, not an instance |
| s2 | :boot-reachable-snapshot | `:entity :evidence` | snapshot evidence entry |
| s3 | :cycle-witness | unbound: abstract | projection payload within s2's evidence |
| s4 | :snapshot-evidence | `:entity :evidence` | emitted evidence entry (the :sorry hole's discharge) |
| s5 | :reconstructable-history | `:entity :evidence` | terminal — reconstitution surface |

**3 bound (all `:evidence`), 2 unbound (abstract).** The unbound boxes are honestly
abstract: s1 is a type-decision (not an instance), s3 is a payload within an evidence
entry (not a distinct type).

## Expected drift prediction

The `:evidence` entity type IS inhabited in substrate-2 (verified: UUID-based entities
exist). So all 3 bound boxes (s2, s4, s5) should PASS the type-inhabitation check —
build-match will see `:evidence` is inhabited.

**However**, the snapshot-specific evidence entries may not be IN substrate-2 at all.
The evidence boundary (`boundary/append!`) writes to futon3c's local evidence store,
which may or may not be ingested into substrate-2 (futon1a/XTDB). The `:evidence`
entities in substrate-2 are UUID-based and none contain "snapshot" in their id. So:

- If build-match checks TYPE inhabitation only (does `:evidence` exist?): **3/3 PASS** —
  the type is inhabited, though possibly by non-snapshot evidence.
- If build-match could check INSTANCE inhabitation (do snapshot-specific entries exist?):
  it would DRIFT — the snapshot evidence is in a local store, not substrate-2.

The type-level match is coarse but honest — the box meaning says the snapshot IS evidence,
and `:evidence` is the correct type name. The finer question (which evidence entries are
snapshots) is beyond what the current build-match asks.
