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

---

## The executable plan *(Joe: "not a list — one that can be broken up and validated as we go")*

Generated by `scripts/work_units.bb` → `holes/labs/wm-contract/work-units.edn`, scored against
**delivery-lifecycle §0.14's seven automatability criteria**. Re-runnable; the score is computed from
measured state, not assigned.

    29 work units
      18  dispatchable unattended (7/7)   14 bind-hole · 3 specify-edge · 1 governance
      10  need a split at a decision point (5/7)
       1  operator-only (3/7) — APEX-D1

Every unit carries its own **acceptance** and **executable falsifier**, so a completed unit is validated by
running a check, not by a person reading it. That is the difference between this and a list.

### The constraint that sets the wave size — write contention, not agent count

The box can run 16 agents. **It cannot run 14 bind-units in parallel as written**, because all fourteen
append to one file: `checks/witness-registry.edn` is a single EDN vector. Fourteen agents appending to one
vector is fourteen-way conflict on a shared checkout — and the same is true of the three edge units, which
all write `p4ng/empirics-futon/hyper-edge-schema.edn`.

**Fix, and it is criterion 1 applied to the output port:** each unit writes its **own fragment file** named
for itself — `checks/witness-fragments/<decl>.edn`, `p4ng/empirics-futon/edge-fragments/<edge>.edn` — and a
single cheap **merge step** assembles the registry and re-runs the lint. Fragments make the work
embarrassingly parallel and the merge is one unit with its own acceptance. Without this the plan is
sequential no matter how many agents exist.

### Waves

**Wave 0 — `HOLD-contract`, alone, blocking.** 80 declarations name a dead session. Every later gate cites
a holder, so this is the precondition, and it is mechanical: 7/7. One agent, minutes.

**Wave 1 — 14 bind-hole units, parallel, fragment-per-unit.** Each takes one Lean hole that already has an
evidence type, finds or writes the check that witnesses it, and emits one fragment pinning `contract-sha`
and `run-sha`. Then **merge + `contract_lint`**: the wave is validated by the lint passing at the current
authority, and by the `--negative` control still exiting 1. No reading required.

**Wave 2 — 3 live-edge pairs (6 seats).** `R2→R7` (three independent sources — the strongest edge in the
machine), `R7→R3`, `R8→R5`. All three are *measured*, so the pair describes rather than proposes; last
night's method produced 0-freehand ports in ~25 minutes. Acceptance: the widened exemplar check passes and
freehand count is 0.

**Wave 3 — the 10 splits, as one decision batch.** Six bind-units lack an evidence type, so someone must
choose which check witnesses the hole; four edges are *dead*, so a pair may hit a semantic fork (as
`R16→R2` did — build the actuator or rename the edge). **Per criterion 6 these are split, not disqualified:**
automate up to the decision, take one ruling, automate after. Batching the ten into a single ruling set is
one operator turn instead of ten.

**Not in any wave — `APEX-D1`.** It scores 3/7 and cannot be automated: its job is to *say* what counts as
evidence, and criterion 2 fails by definition — there is no acceptance to name in advance because naming it
is the work. This is operator work, and it is the one thing no number of agents replaces.

### Seat discipline, from last night's incident
Roster `status` is a liveness flag, not an ownership flag — `idle` means "not mid-invoke this instant",
which is wrong for a pooled worker. Before any wave: ask which seats are lendable, dispatch **staggered**
(a 9-job burst exhausted the Agency JVM's native threads and caused a spawn failure in the APM pipeline),
and use `--mode brief` for informational bells (`--mode work` arms a no-execution gate that fails a correct
reply with nothing to run).

### What this makes true
The single point of failure Joe named is not only the `holder` field. It is that **one session held the
contract, the gate, and the queue**. The fragment/merge split breaks the *write* bottleneck; the seven
criteria break the *decision* bottleneck by routing decisions instead of absorbing them; and the per-unit
falsifier breaks the *reading* bottleneck, which is the one that actually made claude-15 a single point of
failure — 25 units a day all validated by one reader.

---

## Wave 0 — DONE, and the write contention is broken *(2026-08-31)*

### Wave 0: the holder is now `by-record`, not a session

**The fix is indirection, not renaming.** Replacing `claude-15` with `claude-20` would have rebuilt the
same single point of failure under a new name. Instead a declaration's holder resolves through its
**durable `owner` record** and `checks/holder-registry.edn`. Reassignment is now **one line per record**.

    inline session holders   80  ->  0        (mathlib4 fcffddd82b)
    declarations             80 (44 closed / 36 holes) — unchanged by the regeneration
    owning records           12, covering all 80

**A finding on the way, worth recording because it nearly wasted the change.** The doc comments said
`holder: claude-15` **77 times** and I edited all 77 — then regenerated, and the contract still said
`claude-15` for all 80. The emitted value comes from **three constructor helpers**
(`mkClosed`/`mkHole`/`mkRefused`, `Holes.lean:647,651,656`), not the doc comments. Two sources for one
fact, only one of them load-bearing; my edit had merely made them disagree. **The real fix was 3 lines.**

### The check that makes the remainder loud

`checks/holder_check.clj` — Wave 0's executable falsifier. It asserts (1) no declaration inline-names a
session, and (2) every owning record resolves to a holder that is assigned *and live on the Agency
roster*. **It exits 1 today, correctly:**

    inline session holders     0   ✓
    unassigned records         6   covering 64 of 80 declarations
      P-validated-R5 21 · P-glossary-mathematics 15 · P-R9 14 · P-R8 8 · P-R2 5 · run-at-least-once 1
    --negative control        exits 1 — the check can fail, so a future pass is not vacuous

That is the honest state: responsibility is now **addressable** (one line each) rather than **vested in a
corpse**. The six assignments are Joe's, and they are the last of Wave 0.

### Write contention broken — fragments, with a round-trip proof

Fourteen bind-units appending to one EDN vector is fourteen-way conflict on a shared checkout. Each unit
now writes its own `checks/witness-fragments/<witness>.edn`, and `scripts/merge_witnesses.bb` assembles
them. Parallel-safe by construction; the merge refuses on duplicate witness names.

**Validated by round-trip rather than by assertion:** `--split` exploded the committed registry into 7
fragments, and `--check` proves `merge(fragments) == committed registry` as sets. The registry now carries
a **GENERATED — DO NOT HAND-EDIT** header, because a generated file that looks hand-editable is how the
next person silently loses their change.

**Still contended, and the same fix applies:** `p4ng/empirics-futon/hyper-edge-schema.edn` is one vector of
instances that all edge-units write. Wave 2 is only three pairs, so it is survivable serially — but it
should get `edge-fragments/` before any wider fan-out.

### Wave 1 is now dispatchable
14 bind-units, fragment-per-unit, no contention, each validated by the lint plus the negative control.
The seat discipline from last night applies: ask which seats are lendable, stagger the dispatch, and use
`--mode brief` for informational bells.

---

## Minting lanes — what is and is not possible from an agent seat *(2026-08-31)*

Joe reaped inactive agents and proposed **minting new lanes rather than borrowing**, which is the right
fix for the collision I caused. Roster after the reaping: **79 registered, 65 drivable**. So minting is
about *dedication*, not scarcity.

**All three registration paths tested; only one produces a worker, and it is not reachable from here.**

| path | outcome |
|---|---|
| `POST /agents` plain | registers a name; installs a **stub** `invoke-fn` returning `"registered-via-http"` — never executes (`transport/http.clj:3030-3033`) |
| `POST /agents` + `session-id` | the field is **silently ignored**; roster reports `session-id: None` |
| `POST /agents` + `ws-bridge: true` | accepted, then bells fail: *"Agent has no invoke handler (ws bridge not connected)"* |
| `DELETE /agents/<id>` | works cleanly; probes removed |

**A lane name is free; a lane worker is a terminal op.** `AGENCY_AGENT_ID=<lane> futon3c/scripts/codex-picker --new`,
which opens an Emacs Codex REPL per lane.

**Correcting last night's account:** I reported that `codex exec` gave no session id. It does —
`codex exec --json` emits `{"type":"thread.started","thread_id":"…"}` as its first line. I had looked for
a session *file* and concluded the id did not exist. The id was never the obstacle, though: the Agency
ignores a supplied `session-id`, and drivability comes from a **WS-connected invoke handler**, not from
the field.

### Proposed lane shape for Wave 1 — fewer lanes than units

14 bind-units does **not** mean 14 lanes, and it should not: a 9-job burst exhausted the Agency JVM's
native threads last night. Batch instead — **4–6 dedicated lanes, each taking 3–4 units sequentially**.
Fragments make the units independent, so batching costs nothing but wall-clock, and it removes both the
thread pressure and most of the minting burden.

Naming makes collision impossible by construction: `wm-w1-1` … `wm-w1-6`. Nothing else on the box knows
those names, and this plan records that they are dedicated — the seat-ownership fact the roster cannot
express, since `status` is a liveness flag and there is no owner field.

---

## Four delegates, one per vertex *(Joe, 2026-08-31)*

Joe's structure, and better than my wave-numbered one: **a vertex persists across waves; a wave does not.**
Four standing delegates replace one session holding all 80 declarations.

    wm-nouns          the R-nodes          18 staged · 5 with records · 13 without
    wm-verbs          the edges            61 distinct · 2 of 22 drawn carry a schema · 8 multi-attested
    wm-organization   the fit              typology of the WHOLE never written as data · 8/13 freehand ports
    wm-evidence       the apex             DOES NOT EXIST — first holder it has ever had

Charters at `holes/problems/BUILD-packets/DELEGATE-wm-*.md` (~100 lines each: shared preamble + vertex
specifics), dispatched staggered 20s.

### Both minting paths verified before being taught

I would not put a procedure in a packet I had not run. Four attempts, two dead ends:

| path | result |
|---|---|
| `POST /api/alpha/agents` (any type) | **stub** — installs an `invoke-fn` returning `"registered-via-http"`; never executes |
| `POST /agents` + `session-id` | field silently ignored |
| `POST /agents` + `ws-bridge: true` | *"no invoke handler (ws bridge not connected)"* — that path is for a remote laptop |
| **`POST /api/alpha/agents/auto`** `{type, session-id, cwd}` | **WORKS** — Agency assigns the name, returns a session-file; minted `zai-4`, belled it, it ran |
| **emacsclient → `codex-repl--open-instance`** with a lane name | **WORKS** — minted all four delegates; `wm-nouns` answered its probe |

`codex-picker --new` fails from a non-TTY seat (*"stdin is not a terminal"*) — its **picker UI** needs a
terminal, but the elisp it ultimately evaluates does not. Calling `codex-repl--open-instance` directly via
emacsclient bypasses the UI and mints the lane. **So minting is self-service after all**, in both
directions, and Joe does not have to run a terminal command per lane.

The delegates are taught the `/auto` path for zai helpers and told explicitly **not** to use plain
`POST /agents`, because that failure is silent — the agent registers, accepts bells, and never runs.

### What each charter carries
The measured state of its vertex (so no delegate starts by re-deriving it), its first delivery, the seven
§0.14 criteria as the standard for what may run unattended, the fragment discipline (never hand-edit a
generated file; write a fragment and merge), and last night's dispatch lessons: never take a seat you were
not given, stagger, `--mode brief` for messages, and **refusal is a deliverable**.

### First deliveries
- **NOUNS-D2** — census the 13 record-less nodes; *not* 13 records. Also holds the `R14→TRACE` finding:
  Figure 4 has no trace/ledger node, which is a missing **noun**.
- **VERBS-D1** — specify `R2→R7`, the only edge with three independent sources, by pairing.
- **ORG-D1** — make the typology of the whole into data; `:unclassified` with a reason is the honest
  output, and every classifier must be able to return "no".
- **APEX-D1** — draft an `EvidenceContract` for one noun and one edge, **for Joe to rule on**. It scores
  3/7 and cannot be automated: naming the acceptance *is* the work.
