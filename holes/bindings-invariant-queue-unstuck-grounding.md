# Bindings grounding: invariant-queue-unstuck (`futon3c-d/mission/invariant-queue-unstuck`)

**Author:** zai-10 (reviewed lane — claude-4 reviews + runs build-match).
**CLean source:** `futon6/holes/clean/invariant-queue-unstuck.clean.edn` (5 boxes, build-and-discharge, holes at s1-s4, terminal at s5).
**Method:** spec-derived from box `:produces` meaning + `:text`. NOT reverse-engineered.

## Per-box binding derivation

### s1 — `:produces :single-evidence-boundary`

- **Box text:** "futon3c.evidence.boundary/append! becomes the SOLE path to the evidence store. All ~50 direct estore/append* callers refactor through it."
- **Binding:** **unbound: abstract**
- **Why:** This is a CODE-STRUCTURE INVARIANT — "every evidence append originates
  from boundary/append!" (grep-verifiable: zero direct append* outside boundary.clj).
  The `:produces :single-evidence-boundary` names a routing property of the codebase,
  not a substrate entity. It's an architectural invariant, not a typed node.

### s2 — `:produces :durable-verified-entry`

- **Box text:** "append via estore/append*, then verify-persisted reads the entry BACK from futon1a XTDB and refuses to acknowledge on failure."
- **Binding:** `{:kind :entity :type :evidence}`
- **Why:** The box produces a DURABLE EVIDENCE ENTRY — one that has been written to
  futon1a XTDB and read back. The substrate type is `:evidence` (confirmed inhabited).
  The `:produces :durable-verified-entry` names the verified evidence entry; `:evidence`
  is its type.
- **Coarseness note:** `:evidence` is a GENERIC type shared by all evidence entries,
  not just durable-verified ones. This is the same coarseness trap as state-snapshot-witness
  (generic :evidence passes trivially). I note it honestly: the substrate does not
  distinguish evidence subtypes by entity/type. A more discriminating binding would
  need an instance-level predicate (e.g., evidence with a verify-persisted tag), which
  build-match v1 doesn't support.

### s3 — `:produces :ratcheted-inventory`

- **Box text:** "Coverage ratchet (logic/ratchet.clj): status-ordering strongest→weakest; a family's :status cannot decrease without a matching :family-demoted evidence entry."
- **Binding:** **unbound: abstract**
- **Why:** This is a PROCESS INVARIANT — an enforcement mechanism (pre-commit hook
  comparing staged vs HEAD). The `:produces :ratcheted-inventory` names a property
  of the inventory mutation path, not a substrate entity. The ratchet is code-level
  enforcement over the `.sexp` inventory file, not a substrate-2 typed node.

### s4 — `:produces :live-fire-records`

- **Box text:** "a scheduled sweep walks the operational-families and runs each family's check, emitting one :family-fired {:ok|:violation|:inactive} record per family per run."
- **Binding:** `{:kind :entity :type :evidence}`
- **Why:** The box produces `:family-fired` RECORDS — evidence entries emitted through
  the boundary (s1). These are `:evidence` entities with `:family-fired` event tags.
  The `:produces :live-fire-records` names the canary evidence entries.
- **Coarseness note:** Same as s2 — `:evidence` is generic. The `:family-fired` tag
  distinguishes these from other evidence, but the TYPE is shared. I cannot honestly
  use a more specific entity type because the substrate doesn't have one.

### s5 — `:produces :live-projection` (TERMINAL, `:discharges`)

- **Box text:** "projection taps bridge the three already-operational core.logic layers into the probe registry; the queue becomes a 1-d projection of what is alive throughout the stack."
- **Binding:** **unbound: abstract**
- **Why:** The terminal's `:produces :live-projection` is a PROJECTION ARCHITECTURE —
  a design claim about how the invariant queue becomes a live view. The projection is
  a composition of probe results, not a single substrate entity. The `:discharges
  :four-meta-invariants-bound` names a closure state (four meta-invariants bound),
  not a typed node.

## Summary

| Box | `:produces` | Binding | Why |
|-----|-------------|---------|-----|
| s1 | :single-evidence-boundary | unbound: abstract | code-structure invariant (routing) |
| s2 | :durable-verified-entry | `:entity :evidence` | verified evidence entry (**coarse**) |
| s3 | :ratcheted-inventory | unbound: abstract | process invariant (pre-commit enforcement) |
| s4 | :live-fire-records | `:entity :evidence` | family-fired evidence (**coarse**) |
| s5 | :live-projection | unbound: abstract | projection architecture (composition) |

**2 bound (both generic :evidence — coarse), 3 unbound (abstract).**

## Expected drift prediction

- **s2** (`:evidence`): inhabited (abundantly). **PASS** — but coarse (same as
  state-snapshot-witness: any evidence entity satisfies the type check).
- **s4** (`:evidence`): same. **PASS** — coarse.

**Coarseness flag:** both bound boxes use the generic `:evidence` type. This is the
state-snapshot-witness regime (3/3 coarse pass), not the autoclock-in/pattern-ingest
regime (specific types, discriminating). I cannot honestly use more specific types
because the substrate doesn't distinguish evidence subtypes by entity/type. The
`:family-fired` and verify-persisted properties are TAGS/PROPS on evidence entries,
not distinct types. Build-match v2 (instance predicates) would be needed for finer
discrimination.

**The honest signal:** invariant-queue-unstuck has 2 bound boxes that will pass
coarsely, and 3 unbound boxes. The interesting question is whether the `:family-fired`
evidence entries actually exist in substrate-2 (they're emitted by the probe sweep
to the evidence store, which may or may not be ingested into substrate-2).
