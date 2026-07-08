# Bindings grounding: autoclock-in (`futon3c-d/mission/autoclock-in`)

**Author:** zai-10 (reviewed lane — claude-4 reviews + runs build-match).
**CLean source:** `futon6/holes/clean/autoclock-in.clean.edn` (6 boxes, each `:produces`).
**Method:** spec-derived from box `:produces` meaning + `:text`, cross-referenced with
substrate-2 schema (entity types explored via authed Drawbridge, read-only). NOT
reverse-engineered from inhabitation.

## Per-box binding derivation

### s1 — `:produces :canonical-endpoints`

- **Box text:** "Clock targets resolve to the CANONICAL node `<repo>-d/mission/<id>`, never a bare M-* alias"
- **Binding:** `{:kind :entity :type :mission/doc}`
- **Why:** The canonical node IS a mission/doc entity in substrate-2. The box's
  `:produces :canonical-endpoints` names the resolved canonical endpoint — the
  typed entity the clock attaches to. `:mission/doc` is the entity type for
  canonical mission nodes (verified: `futon3c-d/mission/autoclock-in` would be
  such a node).

### s2 — `:produces :durable-clock-edge`

- **Box text:** "persist-clock! writes a clock/clocked-on hyperedge (agent↔canonical-target, witnessed, timestamped) durably to substrate-2"
- **Binding:** `{:kind :hyperedge :type :clock/clocked-on :endpoint-types [:person :mission/doc]}`
- **Why:** The box explicitly names the `clock/clocked-on` hyperedge type
  (confirmed in `clock_lineage.clj`: `(def clock-type "clock/clocked-on")`).
  The endpoints are agent↔canonical-target. The agent SHOULD be a typed entity
  — the box meaning says "agent↔canonical-target" as a relation between two
  parties, and `:person` is the substrate entity type for agents (the agent-id
  namespace has no entity/type, which is a build gap build-match will catch).
  The target endpoint is `:mission/doc` (same as s1). The endpoint types are
  `[:person :mission/doc]` because the box meaning describes a relation between
  an agent (person) and a canonical mission node.

### s3 — `:produces :single-active-spoiled`

- **Box text:** "Single-active: dispatching/editing an agent onto a new target must retract its PRIOR target"
- **Binding:** **unbound: abstract**
- **Why:** This box produces a SPOILED single-active guard — a process invariant,
  not a substrate entity or hyperedge. The box itself is a `:sorry` hole
  (`:hole {:kind :sorry :discharge :sorryProof :satiety :payoff}`). Its
  `:produces :single-active-spoiled` names a spoiled property of the clock
  system, not a typed node or edge. There is no substrate manifestation — the
  spoilage is a behavioral property of the retract logic.

### s4 — `:produces :single-active-invariant`

- **Box text:** "Gu — repair: retract by QUERYING the durable prior targets... making single-active order-INDEPENDENT"
- **Binding:** **unbound: abstract**
- **Why:** This box produces the REPAIRED invariant — order-independence. Like
  s3, it is a theorem property ("for any dispatch order, the set of active
  clocks per agent is exactly {current}"), not a substrate entity or hyperedge.
  The repair changes HOW s2's retract works (querying durable state vs RAM),
  but the `:produces` is the invariant itself, which has no substrate type.

### s5 — `:produces :witnessed-clock`

- **Box text:** "Every clock carries a witness {rule, source, edit-count, window, old/new-target}"
- **Binding:** **unbound: abstract**
- **Why:** The witness is a PROPERTY on the s2 clock edge (stored in `hx/props`
  as `"witness" {...}`), not a separate substrate entity or hyperedge type.
  The box's `:produces :witnessed-clock` decorates the existing durable-clock-edge
  with audit metadata; it does not create a new typed node. The substrate
  manifestation is s2's edge with richer props, not a distinct type.

### s6 — `:produces :reconstitutable-lineage` (TERMINAL, `:discharges`)

- **Box text:** "reconstitute groups durable clocks by canonical target and JOINS the held-work ledger (held/on-mission) per node"
- **Binding:** `{:kind :hyperedge :type :held/on-mission :endpoint-types [:held/item :mission/doc]}`
- **Why:** The box's `:produces :reconstitutable-lineage` is the reconstitution
  read that JOINS clock edges onto the held-work ledger. The join key is the
  `held/on-mission` hyperedge — confirmed in `clock_lineage.clj`:
  `held-by-mission` reads `held/on-mission` edges whose endpoints are
  `[held/item/..., <repo>-d/mission/<stem>]`. The endpoint types are
  `[:held/item :mission/doc]` because the held-work ledger connects a held item
  to a canonical mission node. The reconstitution is the JOIN surface; the
  held/on-mission edge is its substrate manifestation.

## Summary

| Box | :produces | Binding | Why |
|-----|-----------|---------|-----|
| s1 | :canonical-endpoints | `:entity :mission/doc` | canonical node IS a mission/doc entity |
| s2 | :durable-clock-edge | `:hyperedge :clock/clocked-on [:person :mission/doc]` | the clock edge, agent↔mission |
| s3 | :single-active-spoiled | unbound: abstract | spoiled guard is a process invariant |
| s4 | :single-active-invariant | unbound: abstract | order-independence is a theorem property |
| s5 | :witnessed-clock | unbound: abstract | witness is a prop on s2's edge, not a separate type |
| s6 | :reconstitutable-lineage | `:hyperedge :held/on-mission [:held/item :mission/doc]` | reconstitution join surface |

**3 bound, 3 unbound (abstract).** The unbound boxes are honestly abstract:
s3/s4 are invariants/process properties, s5 is a decoration on s2's edge. This
is not a deficiency — it is the honest grounding from box meaning alone.

**Expected drift signal for claude-4's build-match:** s1 (`:mission/doc`) should
be inhabited. s2 (`:clock/clocked-on`) hyperedges exist but their agent endpoints
lack `:entity/type :person` — the build-match query checks endpoint types, so s2
will likely DRIFT (agent:xxx endpoints are untyped strings, not `:person`
entities). s6 (`:held/on-mission`) hyperedges exist and are well-typed
(`:held/item` and `:mission/doc`). This drift is the signal: the clock system
works but its agent endpoints are not typed entities in substrate-2.
