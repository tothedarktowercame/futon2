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

---

## Phase 1 — DONE (2026-08-31). The denominator, honestly

`scripts/edge_census.bb` → `holes/labs/wm-contract/edge-census.edn`. Reproducible; re-run it after any
amendment.

    distinct edges across all sources   61
      drawn (Figure 4)                  22      derived (theory)   26
      measured (WM-RUN2)                 9      sim (role-play)    13
      specified (typed, node->node)      4      + 1 node->store

    drawn edges carrying a :schema       2  of 22
    attested by 2+ independent sources   8
    measured but on NO list              5
    drawn only, nothing corroborates    19

**"21 edges" was never the population.** Depending on the question the denominator is 22, 26, 61, or 13
— and the build has been quoting one number across all of them. That is the same census error this
project has caught three times in other people's work and had not yet caught in its own headline.

### The eight worth doing first — attested by two or more sources

    R2  -> R7     derived + measured + sim   <- THREE sources; the only one
    R5  -> R6     drawn + measured + SPEC    <- done
    R7  -> R3     drawn + measured
    R8  -> R5     drawn + measured
    R16 -> R14    derived + sim
    R16 -> R15    derived + sim
    R3  -> R7     derived + sim
    R5  -> R14    derived + sim

`R2→R7` is the strongest unspecified edge in the machine: the theory implies it, the tick did it, and an
isolated role-played node asked for it without seeing either. **That is Phase 2's first pair.**

### Five the machine did that no list claims

    R20 -> R12    scan preamble — interoception feeds observation
    R12 -> R2     scan preamble — calibration feeds observation
    R3  -> R8     belief feeds F directly (sim proposed the REVERSE; both may be real)
    R6  -> R14    selection consults the temperature seam        <- already SPECIFIED
    R14 -> TRACE  the trace write

**`R14→TRACE` is not a missing edge — it is a missing node.** Figure 4 has no trace/ledger vertex at all,
so the exhaust of every tick leaves the diagram through a hole the diagram cannot draw. §0.11
(`I_evidence_consumed`) predicts exactly this omission. Adding it is a change to the *nouns*, not the
verbs, and it belongs to Joe's §0.16 nouns question rather than to the wiring lane.

### Nineteen drawn-only
Figure 4 asserts them; no theory derivation, no measured hop, and no node asked for them. That is not
evidence they are wrong — one tick exercised one path. But it is the honest statement of what the map
claims on its own authority, and it is the population the apex standard (§0.12) would have to grade.

### My own instrument error, caught mid-Phase
The first run reported **65** edges and **10** specified, by taking the member cross-product of each
hyper-edge instance — which invents deliveries the instance never claims (a 3-node instance yields 6
ordered pairs, including `R14→R5`, which nothing asserts). Fixed to pair consecutive `:out`→`:in` ports,
which is what the instance actually states: 61 and 4. **The bug inflated the count of the very thing the
census exists to measure**, and it is the same shape as the errors this build keeps finding — a
convenient expansion standing in for a recorded claim.

---

## The higher-level view — all four vertices, not just the edges *(added after Joe's correction)*

Edges were one facet. Here is the same census across the whole tetrahedron plus the formal spine, measured
this morning.

| vertex | population | how much is specified / graded | measured by |
|---|---|---|---|
| **Nouns** — R-nodes | 18 with stage entries, 17 touched by a drawn edge | **5 of 18 have a problem record** (R2, R8, R9, R16, R19) | `control-stages.edn`, `holes/problems/P-R*.md` |
| **Verbs** — edges | **61 distinct** across four sources; 22 drawn | **2 of 22 carry a `:schema`**; 4 typed as real deliveries | `edge-census.edn` (Phase 1) |
| **Organisation** — fit | hyper-edge schema anchored, 4 instances | the typology *of the whole* — five phased columns, one cycle, cross-column support — **is still prose**, never written as data | `hyper-edge-schema.edn`; §0.8 |
| **Evidence apex** | — | **does not exist. No lane. No holder.** | §0.8: "does not exist"; §0.12 subdivided it and nothing was staffed |
| **Formal spine** — Lean | 80 declarations: 44 closed, 36 holes | **16 of 36 holes bound** to a recorded passing witness; 20 unbound | `holes-contract.json`, `checks/witness-registry.edn` |
| **WR badges** | 16 | 11 hold, 5 do not — but a badge is a *dated sentence*, not a check | `wr-overlay.edn` |

### Two facts that outrank every lane question

**1. There is an ownership vacuum over the entire formal spine.** All **80** declarations — 44 closed and
36 holes — carry `holder: claude-15`, whose session is dead. Not one names a Clojure locus or a fixture
(`clojure-locus` and `fixture` are empty on all 80); the Lean↔Clojure binding lives entirely in
`checks/witness-registry.edn`, which has **7 entries covering 16 of the 36 holes**. So: the contract that
defines what the machine must satisfy has no living holder, and just under half its open obligations are
tied to anything runnable.

**2. The vertex whose job is to grade the other three has never been staffed.** §0.8 recorded the evidence
apex as *"does not exist"* on 08-30; §0.12 subdivided it on 08-31 — *"what is the evidence of the
evidence?"* — and no lane, no record, and no holder followed. Meanwhile the other three vertices kept
producing material: 61 edges, 36 falsifiers, 16 WR badges, nine empirical campaigns.

**The shape of the problem is a ratio, not a backlog.** The build is producing *specifications* far faster
than it can produce a *standard for judging them* — 36 holes with falsifiers against 16 bindings; 61 edges
against 2 schemas; 16 badges that are dated sentences rather than checks. Adding more edges, more nodes or
more empirics widens that gap. **That is the leadership call, and it is not "which edge next".**

### What I would put to Joe, in priority order

1. **Re-hold the contract.** 80 declarations need a living holder. This is mechanical but it is the
   precondition for anyone gating anything — a `holder` field naming a dead session is a governance hole,
   and it is exactly the `I_absent_is_loud` class the build has been enforcing on everyone else.
2. **Staff the apex.** It is the only vertex with no lane, and it is the one the other three report to
   (§0.9's flow-up: each recursing node sends *one typed thing* to the apex). Until it exists, "gated"
   means "the owner read it", which is a person, not a standard.
3. **Then Phase 2 fill**, ordered by the census: `R2→R7` first (three independent sources).
4. **Hold the second domain and further empirics** until 1–3 land. Both add material the current standard
   cannot grade.

### Method note against myself
I dove into edges because that is the vertex I had been working in, and reported a Phase-1 census as
though it were the state of the build. It was the state of one vertex. Joe's correction is the same error
class this project keeps finding — **a measurement of the part presented with the authority of the
whole** — and it is worth recording that the tech lead made it about his own lane.
