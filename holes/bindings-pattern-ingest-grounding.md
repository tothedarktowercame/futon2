# Bindings grounding: pattern-ingest (`futon3-d/mission/pattern-ingest`)

**Author:** zai-10 (reviewed lane — claude-4 reviews + runs build-match).
**CLean source:** `futon6/holes/clean/pattern-ingest.clean.edn` (6 boxes, shape-gate-then-enrich-then-demote, holes at s2-s5, terminal at s6).
**Method:** spec-derived from box `:produces` meaning + `:text`. NOT reverse-engineered.

## Per-box binding derivation

### s1 — `:produces :shape-checked-pattern`

- **Box text:** "Shape-check the flexiarg FIRST: a Toulmin-slotted structure is a precondition of ingest. A malformed pattern is skipped."
- **Binding:** **unbound: abstract**
- **Why:** Shape-check is a VALIDATION GATE — a precondition that determines whether
  a pattern proceeds to s2. It produces a decision (pass/fail), not a substrate entity.
  The pattern vertex itself is s2's output.

### s2 — `:produces :rich-vertex`

- **Box text:** "v1 projector: emit @title, ! conclusion, and one code/v05/pattern-slot edge per named clause onto the code/v05/var vertex"
- **Binding:** `{:kind :entity :type :pattern/component}`
- **Why:** The box explicitly produces a VERTEX ("the projected vertex + slot edges").
  In substrate-2, pattern vertices are `:pattern/component` entities (confirmed inhabited:
  UUID-based entities exist). The `:produces :rich-vertex` names the enriched pattern
  vertex — the `:pattern/component` is its substrate type.

### s3 — `:produces :canonical-or-pending-sigil`

- **Box text:** "Sigil canonicaliser: replace reject-on-invalid with canonicalise (emoji∈allowlist, hanzi∈allowlist), auto-rewrite+git-commit, else mark :sigil/pending"
- **Binding:** `{:kind :entity :type :pattern/sigil}`
- **Why:** The box produces a CANONICALISED SIGIL — a typed entity in substrate-2.
  `:pattern/sigil` entities exist (confirmed: `sigil|akesi|田`, `sigil|alasa|义`, etc.).
  The `:produces :canonical-or-pending-sigil` names the sigil assignment; `:pattern/sigil`
  is its substrate type. Specific type, not generic — should discriminate.

### s4 — `:produces :provenance-linked-vertex`

- **Box text:** "emit a code/v05/pattern-origin hyperedge linking the pattern vertex to (a) the introducing commit and (b) the Evidence entry"
- **Binding:** `{:kind :hyperedge :type "code/v05/pattern-origin" :endpoint-types [:pattern/component :source/commit]}`
- **Why:** The box explicitly names a HYPEREDGE type: `code/v05/pattern-origin`. Its
  endpoints are the pattern vertex (`:pattern/component`) and the introducing commit
  (`:source/commit`). Both are typed entities (confirmed inhabited). The type is a
  multi-segment string (double `/`) so it's stored as a string, not a keyword — same
  as other `code/v05/*` types in the codebase.

### s5 — `:produces :substrate-authoritative-record`

- **Box text:** "Demote the TSV to a DERIVED view: patterns-index.tsv stops being the authority on sigils — XTDB is."
- **Binding:** **unbound: abstract**
- **Why:** This is an AUTHORITY INVERSION — a governance claim about which store is
  authoritative. The `:produces :substrate-authoritative-record` names a state of the
  system (XTDB is authority, TSV is checkout), not a new typed entity. The vertex and
  sigil entities already exist (from s2/s3); s5 changes their GOVERNANCE status.

### s6 — `:produces :inference-demonstration` (TERMINAL, `:discharges`)

- **Box text:** "emit code/v05/related-to edges from each RELATED clause so the pattern library renders as a navigable graph"
- **Binding:** `{:kind :hyperedge :type "code/v05/related-to" :endpoint-types [:pattern/component :pattern/component]}`
- **Why:** The box explicitly names a HYPEREDGE type: `code/v05/related-to`, connecting
  pattern vertices to each other. Both endpoints are `:pattern/component` entities.
  The terminal discharges to `:substrate-query-cross-pattern-inference` — the
  navigable graph is the `code/v05/related-to` edge.

## Summary

| Box | `:produces` | Binding | Why |
|-----|-------------|---------|-----|
| s1 | :shape-checked-pattern | unbound: abstract | validation gate (pass/fail decision) |
| s2 | :rich-vertex | `:entity :pattern/component` | enriched pattern vertex |
| s3 | :canonical-or-pending-sigil | `:entity :pattern/sigil` | canonicalised sigil entity |
| s4 | :provenance-linked-vertex | `:hyperedge "code/v05/pattern-origin" [:pattern/component :source/commit]` | provenance edge |
| s5 | :substrate-authoritative-record | unbound: abstract | governance inversion claim |
| s6 | :inference-demonstration | `:hyperedge "code/v05/related-to" [:pattern/component :pattern/component]` | navigable graph edge |

**4 bound (2 specific entities, 2 specific hyperedges), 2 unbound (abstract).**

## Expected drift prediction

- **s2** (`:pattern/component`): inhabited (UUID-based entities exist). **PASS likely.**
- **s3** (`:pattern/sigil`): inhabited (`sigil|akesi|田`, etc.). **PASS likely.**
- **s4** (`code/v05/pattern-origin` hyperedge): this is the uncertain one. The CLean
  says "on first-seen of a flexiarg qname, emit a pattern-origin hyperedge" — but this
  is a DERIVE mission, not fully landed. The edge type may NOT be inhabited if the
  provenance linking was never built. **DRIFT likely** — the type name exists in the
  CLean design but may not have been written to substrate-2.
- **s6** (`code/v05/related-to` hyperedge): the CLean says "pilot the payoff: emit
  code/v05/related-to edges." Similar uncertainty — this is a PILOT, may not have
  been built. **DRIFT possible.**

**No generic-type coarseness here** — all four bound types are specific. This is the
discriminating regime (like autoclock-in), not the coarse regime (like state-snapshot).
