# Build plan — takeover, 2026-08-31

*claude-20, taking the owner seat after claude-15's session was poisoned. Everything below was
re-verified from artefacts this morning; where I found the status page stale I say so rather than
silently correcting it.*

---

## 1. The logic of the plan, as it actually stands

The plan has **two axes that grew at once**, which is why it is hard to read as one outline.

**Axis A — the method.** `futon4/holes/delivery-lifecycle.md` §0.7–§0.16. Joe's big tetrahedron: four
vertices — **nouns** (the R-nodes), **verbs** (Figure 4's edges), **organisation** (the typology of
edges and of the whole), **evidence apex** (a standard for which empirics are the right empirics). Each
vertex gets *subdivided* — its own tetrahedron — as it comes into focus (§0.9's Sierpiński rule: a node
recurses when it has internal wiring).

**Axis B — the artefact.** `M-formal-war-machine` §3 DERIVE, staffed as delivery-lifecycle problem
records: one `P-R*.md` per node, deliveries `D1…Dn`, acceptance named in advance.

**They connect because each subdivision spawns a lane:**

| subdivision | move | lane it spawned | state |
|---|---|---|---|
| evidence | §0.12 (08-31) | the apex standard | stated, no lane yet |
| organisation | §0.13 (08-31) | **EDGES** — edges are typed hyper-edges with ports | D1 gated; schema anchored |
| negative space | §0.14 (PROPOSED) | **AUTO** — the workflow octahedron | awaits Joe's word |
| verbs | §0.15 (PROPOSED) | **PILOT** — handoff formulas, guarantee pairs | awaits Joe's word |
| nouns | §0.16 (SEED) | **NOUNS** — what are the carriers? | D1 done+gated (`fe3dcb9`) |

The status page's "top-level moves" list is numbered **1, 2, 5, 4, 3** — the register order, not reading
order. That alone makes it near-unreadable; the table above is the same content in sequence.

---

## 2. Where it stands — verified this morning

**Closed and gated:** R9 (self-certification), R8 (F and the era law), R2 (channel contract), R19
(preference stack — lane closed, every EFE result names its five-layer stack), AUD (D1–D5 + D4b; only
mark2 remains, and that is a Joe decision), Glossary→Lean (33/33 Formal lines), Library wave 2
(`be8c707`), NOUNS-D1, EDGES-D1, WM-RUN1 and WM-RUN2.

**Quiescent:** no builder lane is running. One malformed park (codex-10, `awaiting [None]`, no deadline
— junk, should be cleared). One job delivering from an unrelated caller.

**The one big open thing is the wiring** — and it is the *oldest* open item in the mission
(§1.6 criterion 1, "the wiring is data"), older than the H-series.

---

## 3. The structural problem to organise around

**There are four registries of edges and nothing reconciles them.**

| source | count | what it asserts |
|---|---|---|
| `control-map-edges.edn` `:edges` | **22 drawn** | what Figure 4 *claims* |
| `control-map-edges.edn` `:derived-undrawn` | **26** | what the theory *implies* |
| PREREG §2f, from WM-RUN2 | **9 hops, 6 unmapped** | what the machine *did* |
| `hyper-edge-schema.edn` `:instances` | **4 (3 control-map)** | what is *specified* |

Only **2 of 22** drawn edges carry a `:schema`. And the registries actively disagree:

- **`R6→R14` is on no list** — not drawn, not derived — yet it is in the wired hyper-edge instance,
  discovered by the measured run and confirmed by the R5/R14 pairing.
- **`R5→R14`** is derived-undrawn, and the pair decomposed it into `R5→R6` (drawn) + `R6→R14` (unlisted).
  So the specified instance spans one drawn edge and one edge that exists nowhere else.
- Two of the six unmapped hops (`R2→R7`, `R3→R8`) **triangulate with the node-sim's independent
  role-play**, which is three sources agreeing on edges the map does not draw.

`wmRunConformsToWiring` is honestly unwitnessed, and it cannot be witnessed until these agree.
**This is the coherent centre of the remaining work**: not "specify more edges" but "make the four
accounts of the wiring into one account."

---

## 4. Proposed plan

### Phase 1 — reconcile (blocking, cheap, mine)
One artefact that puts every edge in one table with its provenance: drawn / derived / measured /
specified, and the disagreements explicit. Output: the true denominator. Today "21 edges" and "13 tick-path
ports" and "26 derived" are different populations quoted as if commensurable — the census error this build
has caught three times in other places and not yet in its own headline numbers.

### Phase 2 — fill, by pairing (the method is now measured)
Last night's measurement, from the widened exemplar gate:

    edge produced by two nodes negotiating   6 ports, 0 freehand
    edges produced from written records      2 ports each, all freehand

Freehand is legal but means no interface descriptor exists. **Negotiation produces descriptors;
reconciliation produces placeholders.** So pairs are the fill method, ~25 min and 2 seats per edge, and
the order should be: the 6 measured hops first (the machine did them, so they are describable), then the
derived-undrawn with independent corroboration, then the rest.

### Phase 3 — the apex
The evidence vertex (§0.12) is subdivided in the lifecycle but has **no lane**. It is the vertex that says
which empirics are the right empirics, and it is the reason R18 could not certify. It should get a lane
before more empirics are gathered, or we accumulate more evidence with no standard for it.

### What needs Joe, in the order it blocks things
1. **R16 fork** — build the outward actuator, rename the edge, or both-staged (claude-15 recommended both,
   staged). Blocks R16-D2 and the R16→R2 instance's payload.
2. **The two π's** — glossary scored-cascade π vs `Policy := InformationState → Action`. Blocks the spine.
3. **AUTO-D1 and PILOT-D1** — both PROPOSED, both awaiting your word, neither blocking.
4. **mark2** endpoint-vs-refresh; **R2 turn channel**; **APM authority**; **G-D2** (your paper);
   **second domain** go/no-go.

### What I would not do yet
Start the second domain, or gather more empirics, until Phase 1 gives an honest denominator and the apex
has a lane. Both would add material the current standard cannot grade.
