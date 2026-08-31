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

    wm-nouns          the R-nodes          18 staged · 5 records, 4 intersecting · 14 without
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
- **NOUNS-D2** — census the 14 record-less nodes; *not* 14 records. Also holds the `R14→TRACE` finding:
  Figure 4 has no trace/ledger node, which is a missing **noun**.
- **VERBS-D1** — specify `R2→R7`, the only edge with three independent sources, by pairing.
- **ORG-D1** — make the typology of the whole into data; `:unclassified` with a reason is the honest
  output, and every classifier must be able to return "no".
- **APEX-D1** — draft an `EvidenceContract` for one noun and one edge, **for Joe to rule on**. It scores
  3/7 and cannot be automated: naming the acceptance *is* the work.

---

## NOUNS-D2 — gated, and it corrected the commissioner twice *(2026-08-31)*

`88c4bbc`: `holes/labs/wm-contract/noun-census.edn` (14 rows) + `NOUNS-D2-findings.md`.

**What I checked:** the EDN parses; 14 distinct nodes, each with a catalogue line, name and quotation; both
refusals carry typed statuses in the artefact rather than being silently complied with; carrier candidates
spread across all four kinds (stack 5, cascade 4, route 3, chain 2) rather than collapsing to one; and I
opened two cited references — `tripwire.clj:452` is `(defn evaluate-wire` and `wm_scheduled_run.clj:66` is
`(defn -main`, both exact.

**Correction 1, and I was wrong: the population is 14, not 13.** I wrote "18 staged, 5 with records, 13
without" — subtracting five records from eighteen staged **without checking the intersection**. The staged
set is R1–R17 plus R20; **R19 is not in it**, so only four records intersect and 18 − 4 = 14. That is the
census error class this build has caught three times in others' work, made by me in the packet that
commissions a census. The delegate recorded it as `:status :refused-count-mismatch` with the arithmetic
shown, which is the right way to refuse a commissioned number.

**Correction 2: "three carrier kinds" is four.** My packet said three; `BUILD-status.md` says *"chains vs
cascades vs stacks vs routes"*. The delegate recorded `:refused-arity-mismatch` and — correctly — did not
invent a partition to fit either number, recording one best candidate plus alternatives per row and
labelling them *"census hypotheses, not settled types."*

**A finding neither of us commissioned, visible only once the census existed: all 14 record-less nodes are
implemented.** Every row carries `:implementation {:status :present}` with a `file:line`. So the gap at the
nouns vertex is **not missing code — it is missing records**: fourteen of the eighteen staged nodes do
things in Clojure that no problem record describes. That reframes what a node record is for here, and it is
a better argument for writing them than "we have five and should have eighteen".

**`R14→TRACE` confirmed as a missing noun**, with implementation at `war_machine.clj:4763` and neither
`control-stages.edn` nor `control-map-edges.edn` carrying a TRACE vertex. Carrier candidate `:route`
(alternative `:stack` — "a trace records the executed route; an append-only ledger is a stack of such
records"). Marked `:decision :joe-must-decide-whether-to-add-vertex`. **This is Joe's call**, and it is a
change to the diagram's nouns, not its edges.

---

## ORG-D1 — gated. The typology is data, and it refutes the prose that defined the vertex

p4ng `69c60dc` (`empirics-futon/control-organization.edn`, 22 edges) + futon2 `072ab04`
(`checks/control_organization_check.clj`, 100 lines with a negative control).

**What I checked:** the gate passes all 22 drawn edges (`{:pass? true :classified 11 :unclassified 11}`);
the negative control genuinely works — it mutates `[:R1 :R4]`'s `from-column` to `:ACT` and the check
rejects it with `{:error :wrong-from-column :expected :BELIEVE :actual :ACT}`, so the pass is not vacuous;
every edge row carries `from-column`, `to-column`, `column-relation`, `diagram-role`, `classification` and
a `basis` string.

**The finding, and it is a refutation.** §0.8 describes the organisation as *"five phased columns, one
cycle, cross-column support"*. Now that it is data:

    support edges:  7 within-column · 5 cross-column

**The majority of support edges are WITHIN column.** The third clause of the prose characterisation is
wrong, and it was only checkable once someone wrote the typology down — which is precisely the argument
for this vertex existing. The within-column support edges are `R5→R5` (a self-loop in EVALUATE),
`R6→R11`, `R7→R3`, `R9→R16`, `R10→R8`, `R12→R7`, `R15→R13`.

**§1.4's hypothesis is now testable and mostly unsupported.** The mission hypothesised that support edges
are APM-style policies, i.e. constraints. Of 12 support edges, exactly **one** — `R9→R16` — has
artefact-level grounds to be classified `:constraint`. The other eleven are `:unclassified` with reasons.
The delegate did not close the gap by guessing, which is what the charter asked for: *"an honest
`:unclassified` count is the finding; a complete classification obtained by guessing is the facade this
build exists to catch."* 11 of 22 unclassified is that honest count.

**A trap for whoever wires these into CI.** The two checks use **opposite exit conventions** for
`--negative`: `hyper_edge_exemplar_check.clj` exits **1** when its control correctly rejects the mutation;
`control_organization_check.clj` exits **0**. I misread the second as a control that could not fail before
reading the source. Both are defensible alone; together they are a hazard, because a runner that treats
nonzero as failure will score one of them backwards. Worth standardising before there is a third.

**Also noted:** the delegate reported that concurrent lanes advanced futon2 HEAD after its commit and it
left their changes untouched. That is the shared-checkout discipline holding under actual concurrency.

---

## APEX-D1 — gated. The apex has its first holder and its first draft

`2dfb7f7` + `6c867d3`: `holes/problems/P-evidence-apex.md` (188 lines). Sections: the problem, a proposed
`EvidenceContract` shape, a worked **noun** (R2's channel contract), a worked **edge** (`R16→R2`, status
`:refused`), decisions requested from Joe, and what would make APEX-D2 dispatchable.

**It did the thing the charter asked and did not do the thing it warned against.** The charter said this
unit scores 3/7, cannot be automated, and must produce *a draft for Joe to rule on, not a decision* — and
warned it not to rebuild R18 (an apex that is one of the nouns, wearing the word "meta"). The delivery is
a draft with four named decisions and one worked case explicitly `:status :refused`. **The R16→R2 contract
refuses itself**: it states the claim (`P-R16/solved-2` — an outward actuation yields an external witness)
and the requirement (`:external-effect-receipt`), then refuses, because R16 constructs and does not
actuate. That is an evidence contract correctly declining to certify.

**And it fired `I_data_current` on its own worked case.** Running the R2 test it cites:

    FAIL current-r2-channel-contract-baseline
    expected "c9add16ac96c973b"  actual "db71e095a81e8620"     3 failures, 25 assertions

**I verified the corpus independently:** 53 files, **798 forms / 796 conformant**, pin `db71e095a81e8620`
— against the registered 792 / 790. The check refused its own baseline rather than passing quietly or
silently re-pinning.

**And the growth is fully explained, which answers the question the delegate left open.**
`wm-trace-2026-08-30.edn` held **1 form** when I measured it earlier today and holds **7** now — exactly
the +6 — with the last timestamped `20:41:52`, the WM-RUN2 tick. So the corpus grew by **the build's own
instrumented runs**, after R2-D2 pinned 792. Nothing anomalous: the disposition is **requalify**, not
investigate. That is worth stating plainly because "the pin moved" and "the data is wrong" look identical
from inside a failing test, and this build has spent two days on the difference.

**Joe's four decisions, unchanged and his:** (1) ratify the joined upward `claim-clause` / downward
`requirement` shape; (2) may executable negative controls make `:self` evidence admissible for conformance
claims like R2's; (3) the R16→R2 fork — rename, build, or both staged; (4) EDN-first or Lean-first after
semantic ratification.

**Decision 2 is the load-bearing one** and deserves flagging: R9's machinery types a witness as
`independent` / `self` / `unknown`, and R2's conformance evidence is *self*-produced — the machine
checking its own trace. If a negative control can promote `:self` to admissible, most of this build's
existing evidence becomes gradeable; if it cannot, most of it does not. That is the apex's first real
ruling and it is exactly the kind that should not be made by a delegate.

---

## The holder backlog, accepted and measured *(wm-nouns, `28a6026`)*

wm-nouns accepted the 63-declaration holding, including the argument that `P-glossary-mathematics` is
recursive noun vocabulary, and produced the per-record binding number nobody had. **Verified
independently, every figure exact:**

    P-R2                      2 holes    2 bound
    P-R8                      3 holes    3 bound
    P-R9                      7 holes    4 bound
    P-validated-R5           12 holes    5 bound
    P-glossary-mathematics    8 holes    0 bound
    ------------------------------------------------
    nouns total              32 holes   14 bound
    build-wide               36 holes   16 bound

`holder_check.clj` passes: 80 declarations, 0 orphaned. **wm-nouns now holds 32 of the 36 open holes and
14 of the 16 bindings**; claude-20 holds the remaining 4 and 2.

**`P-glossary-mathematics` is 0 of 8, and that is the most useful number of the morning.** The mission's
one-line goal is *"every term is defined on Active Inference's terms in Lean, **and** every implementation
is held to that definition by a run that Lean can check."* The glossary lane is recorded as **done — 33/33
Formal lines**, and it is: the first clause is satisfied. **The second clause is satisfied for none of its
eight open holes.** `predictionError`, `PrecisionMap`, `variationalFreeEnergy`, `softmax`,
`GenerativeModel`, `expectedFreeEnergy` and the rest are defined and unwitnessed.

That is not a contradiction in the status page — "done" there means the Formal lines, and it says so. But
the two halves of the goal have been tracked as one lane, and only the first half has ever moved. **The
mathematics the whole machine is defined in terms of has zero runs bearing on it.**

It also reframes the apex's first decision. `wm-evidence` asks whether executable negative controls can
make `:self` evidence admissible — and the glossary is the extreme case: eight holes whose only plausible
witness is the machine computing its own definitions. If `:self` is inadmissible without a negative
control, the glossary cannot be witnessed at all until one exists.

---

## VERBS-D1 — gated. The strongest edge in the machine is not an edge

p4ng `a45b8be` (edge fragment + regenerated schema), futon2 `2cd7049` (census refresh), five pairing-round
commits plus `c41be67`/`7d48c17` for the corrected and confirmed contract.

**What I checked:** `merge_edges.bb --check` — 5 instances, `merge(fragments) == committed`; the exemplar
gate passes all 5 and its negative control still exits 1; the new instance
`:control-map/R2-R3a-R7-precision` has **6 ports, 0 freehand**; and I opened its cited line —
`belief.clj:913` is `(def channels-with-likelihood)`, exactly as claimed.

**`R2→R7` is a two-hop path, not a direct call.** Live traffic is `R2 → R3a → R7`, and the instance names
its members honestly: `[:node/R2 :mediator/R3a :node/R7 :store/wm-trace]`. **`:mediator/` is a new member
kind** — the delegate declined to promote R3a to `:node/` when it has no entry in `control-stages.edn`,
while still refusing to pretend the hop is absent. That is the right call and it is a small piece of
vocabulary the schema did not have this morning.

**This is the second time in two pairings that the "direct" edge had something in the middle.** `R5→R14`
turned out to be `R5→R6→R14`; `R2→R7` is `R2→R3a→R7`. Both were discovered by pairing against the running
code, and neither the drawing nor the theory derivation had the intermediary. **The map's edges are
consistently one hop shorter than the machine's paths** — which is now a pattern with two instances rather
than an anecdote, and it is a finding about Figure 4 rather than about either pair.

**The stale-documentation catch, verified.** `belief.clj:913`'s docstring says *"v0.11: 4 channels
(annotation-health, sorry-count-norm, mission-health, active-repo-ratio)"*. The live set has **8**:
those four plus `attack-coverage`, `coupling-density`, `support-coverage`, `ticks-firing-ratio`. So the
projection is **14 observation channels → 8 likelihood channels**, not 13→4. A docstring that lists its
members by name and then grew four more without the prose changing is exactly the drift the pairing method
keeps catching, because a pair reads the definition rather than the sentence about it.

**Absence coerced to `0.0` at three sites**, recorded as a blocker rather than papered over. That is now
the same defect found at `observation.clj:42`, in the first pairing's payload encoding, and here — the
build's most persistent single fault, and the contract records typed absence, receipt machinery and
vocabulary versioning as `:blocked-on` instead of inventing completed semantics.

---

## Workflow: patterns updated as we work, and escalations assessed against the spec *(Joe, 2026-08-31)*

### The paper is now updated, and verified at the artefact

`p4ng 4df9399` revises two patterns in `sec-catalog.tex` against the record, in the paper's own idiom
(R16 already carried a *"Revised against the record"* clause, so the mechanism existed and was unused):

- **R14 — Selection Gain as Commitment Temperature.** The dial is built and **not connected**:
  `policy.clj:234-272` computes τ and the habit-adjusted scores, then takes `(first controller-entries)`.
  The invariant *"exploration is a dial, not a random number generator buried in the selector"* survives
  **literally** and fails **in substance** — the dial turns nothing. That distinction is why a pattern needs
  a witness and not only an invariant, and it is why WR-27 reads `:holds false`.
- **R16 — Grounded Actuation, Not Re-Observation.** No amendment needed: **the pattern named this failure
  in advance.** It warns of *"an enactor that re-runs its own construction … a mirror, not an act"*, and
  `enact.clj:113` returns a wiring map. Recorded as a second revision saying exactly that — the pattern
  diagnosed its own implementation, and two independently role-played endpoints reached it from opposite
  sides.

Built and published: `/var/www/.../wip/plop-2026.pdf`, 09:52, and I confirmed both revisions are **in the
published PDF** via `pdftotext`, not by trusting the build's own report. Which matters, because —

**`build-p4ng.sh:118` prints `==> published $WIP/$DOC.html` unconditionally.** Line 114 publishes the PDF
*inside an `if`*; line 118 announces an HTML publish whether or not anything was written. The HTML does not
exist at that path. **A success message that cannot fail** — the same defect class as the exemplar gate
that checked one instance and the acceptances that could not fail. Recorded; the PDF path is sound.

### The rule, since I own the workflow

**Every gated delivery states whether it changes a pattern claim, and pattern-bearing findings are written
into `sec-catalog.tex` at gate time, not batched.** A finding that contradicts a pattern and lives only in
a ledger is the repository-boundary failure again at a different boundary: the record exists and the
document a reader actually reads does not have it.

### Escalations reassessed against the one problem and five precepts

Joe's correction: *look at your own assessment relative to the high-level specification.* Doing that
retires two of the four things I had been handing back.

**RETIRED — the R16 fork (rename / build / both).** Not a preference. The *fit* precept requires the wiring
to state what the machine does, and the map currently claims grounded actuation R16 does not perform →
**rename now.** The problem statement requires every implementation to be held to its definition by a run
→ **build the outward actuator as the repair.** Both, staged, is what the spec dictates; claude-15's
recommendation was correct and did not need Joe's ratification. **Proceeding.**

**RETIRED — the TRACE vertex.** `I_evidence_consumed` says an emission without a named consumer is exhaust;
the *fit* precept says the wiring must contain what the transactions live in. The trace is a real consumer
the diagram lacks, and `war_machine.clj:4763` writes to it every tick. **It gets a vertex.** What remains is
naming, which is not a decision requiring the operator.

**STILL JOE'S, and here is the sharper framing — `:self` admissibility.** This is not indecision on my part:
**two precepts genuinely conflict.** The *evidence apex* certifies that evidence is the right evidence, and
its operative test is falsifiability — a self-witness with a working negative control *can* say "this is
wrong". But **R9's whole pattern is no self-certification**, and R2's conformance evidence is the machine
checking its own trace. Falsifiability says admit it; R9 says refuse it. Whichever way this goes it moves
a large population — the glossary's 8 holes have no plausible non-self witness at all. **A conflict between
two precepts is exactly what an operator is for**, and I am not going to resolve it by preferring the
precept that unblocks more work.

**STILL JOE'S — the two π's.** A Gate 0 nouns question: which definition the spine is built on. I can state
the consequences but not choose the mathematics.

---

## The `:self` question, dissolved rather than decided *(Joe, 2026-08-31)*

I escalated `:self` admissibility as a genuine conflict between two precepts — the apex's falsifiability
test versus R9's no-self-certification. **Joe reframed it and the reframing retires both horns:**

> *"if `:self` evidence is passed forward, it needs processing … it is basically a bare perception or
> datum. I think AIF will have better things to say about how that processing should work than I will."*

**`:self` is neither admissible nor inadmissible — it is UNPROCESSED.** A self-witness is an *observation*,
not a certified claim, and treating it as a claim was the category error on both sides of my framing. R9
forbids self-**certification**; nothing forbids self-**observation**. The apex's job is not to admit or
refuse a datum but to say what turns one into evidence.

**And AIF already says, in declarations that are `closed` rather than holes:**

    PrecisionMap := Channel → NonnegativeReal                Holes.lean:466
    variationalFreeEnergy := ½ · mean_k (Π_k · ε_k²)         Holes.lean:469   precision-WEIGHTED
    bayesFactorThreshold (deltaF) := deltaF ≤ -3             Holes.lean:491
    bayesianModelReduction (A a' a) := A + a' - a            Holes.lean:502

So the evidence-merging strategy is **the machine's own loop applied to evidence**: a datum enters as an
observation on a channel with a prediction error; **precision is where independence lives** — review and
cross-validation raise Π rather than flipping a boolean, so an unreplicated self-report is *low-precision*,
which is a different thing from inadmissible; evidence accumulates as `F = ½·mean(Π·ε²)`; and a claim is
licensed at **`ΔF ≤ -3`** — a stated threshold, which is precisely what "the owner read it" never was.

**The convergence worth noticing:** `P-glossary-mathematics` is **0 of 8 bound** — the whole AIF vocabulary
defined and unwitnessed — and an evidence-merging strategy built from these declarations, exercised on real
evidence, is the first thing that would witness them. **The apex and the glossary's emptiest lane are the
same work.** APEX-D2 is dispatched on that basis, with `expectedInformationGain` flagged as a REFUSED hole
not to be routed through, and `observationKernel`/`BeliefState`/`GenerativeModel` flagged as carriers that
are themselves holes.

**Method note for me.** I framed this as "a precept conflict, therefore the operator's". It was a
**category error I had not noticed** — and the operator's contribution was not a preference between
precepts but a better type for the thing in question. Escalating with a sharp framing is still right; but
"two precepts conflict" should have prompted one more question — *are both precepts even talking about the
same kind of object?* — before it prompted an escalation. They were not: one was about claims, one about
data.

---

## APEX-D2 gated — and it corrected me on the thing I had just told it

`61be372`: the evidence-processing pipeline, documentation-only. It records `:self` as an unprocessed
observation rather than an admissibility verdict, the channel→ε→Π→F chain, named precision operations
(review, cross-validation, independent reproduction), an R2 worked case yielding `F = 1/14`, typed blockers,
no EIG dependency, and R16→R2 as both-staged.

**Its surprise corrects the packet I sent it.** I wrote that the closed Lean pieces formed a licensing
pipeline — *"evidence accumulates as F = ½·mean(Π·ε²) … a claim is licensed at ΔF ≤ -3."* The delegate:
*"`variationalFreeEnergy` produces F, while `bayesFactorThreshold` consumes ΔF; sharing `ℝ` is not a
semantic bridge."* **Verified, and it is right.**

    PERCEPTUAL       variationalFreeEnergy = ½·mean_k(Π_k·ε_k²)                 :469  closed
                     terminates at F; nothing consumes it

    MODEL-REDUCTION  deltaFReduction (A a' a A') = ln B(A)+ln B(a')-ln B(a)-ln B(A')  :486  closed
                     bayesFactorThreshold (deltaF) := deltaF ≤ -3                :491  closed
                     a COMPLETE licensing chain over Dirichlet concentration parameters

**There are two free energies, and I conflated them.** `deltaFReduction` is the *Bayesian model reduction*
ΔF over Dirichlet counts, not a difference of variational free energies. My error is this build's own
defect class — **a type-level match standing in for a semantic one** — committed in the packet that
commissioned the vertex whose job is to catch exactly that. The delegate's `F = 1/14` case correctly stops
at `:processed-observation` rather than `:certified-claim`.

**What the correction buys, which is more than it costs.** The licensing half is *already proved end to
end*: Dirichlet counts → `logMultivariateBeta` → `deltaFReduction` → `bayesFactorThreshold`, all closed.
**The single missing piece is the one the delegate named: a map from evidence sources to Dirichlet
concentration parameters.** That is not a gap in AIF — it is a modelling step nobody has taken, and it is
small and well-posed. `logMultivariateBeta` is a **hole**, so the chain is defined and unwitnessed: writing
the source→counts map and running it would witness it *and* give the apex a threshold in one delivery.
APEX-D3 dispatched on that basis, with an explicit instruction **not** to bridge F to ΔF, because forcing a
bridge between two different constructions would be inventing the semantics.

**Score so far on delegates correcting the commissioner: four for four.** NOUNS-D2 (population 14 not 13,
carriers four not three), ORG-D1 (support is mostly within-column, refuting §0.8's prose), VERBS-D1 (the
edge is two hops; the docstring's channel count was stale), APEX-D2 (two free energies, not one). Every one
of those was a claim I or the record asserted and none of them was caught by a reader.

---

## The two π's — settled by reading the formalisation *(2026-08-31)*

**The question as recorded:** `cascadeGrainPi` is `mkRefused … "glossary π and Holes.Policy have unresolved
grains"`, falsifier *"REFUSED pending Joe's grain decision"*. The glossary (`sec-glossary.tex:48`) says π is
*"the active-inference name for a pattern language/cascade when that composition is being scored"* — a
**semilattice of patterns** with `S(π)=Σ_t ρ^t s(s_t)`. `Holes.lean:54` says
`abbrev Policy (InformationState Action) := InformationState → Action`.

**But the mathematics never had the conflict.**

    Holes.lean:54   abbrev Policy (InformationState Action) := InformationState → Action
    Holes.lean:70   def G {Policy : Type*} (risk eig : Policy → ℝ) : Policy → ℝ := fun π => risk π - eig π
    Holes.lean:494  def softmax {PolicyIndex : Type*} … (policies : List PolicyIndex) : List ℝ

**`G` and `softmax` are polymorphic in the policy type.** They score whatever π is and never commit to the
state→action reading. Inside `G {Policy : Type*}` the binder **shadows** the abbreviation — one identifier
naming two different things in one file, which is where the appearance of a grain conflict comes from.

**So the decision is not "which π do we keep".** It is: **stop using one name for two objects.**

- **π is what G scores** — and in AIF that is a *plan being evaluated*, which is the glossary's scored
  cascade. The problem statement requires every term *"defined on Active Inference's terms"*, and
  `InformationState → Action` is the control-theory sense of "policy", not AIF's π.
- The state→action function is a real and useful object — it is what you *get* after inference, not what
  is scored. It needs its own name (`ActionSelector`/`Controller`), not π's.
- `cascadeGrainPi` then stops being a refusal and becomes a **definition**: the cascade type at which `G`,
  `softmax` and `IsArgminOn` are instantiated for the cascade lane. No existing proof changes, because the
  polymorphism is already there.

**Confidence, stated honestly:** the *observation* — that G is polymorphic and the name is overloaded — is
verified in the source and is not a matter of taste. The *ruling* — that π should name the scored cascade
— follows from the problem statement's "on Active Inference's terms", and is Joe's to confirm rather than
mine to impose. But it is not a coin-flip between two equal readings, and it does not block dispatch.

**Where to dispatch: `wm-nouns`, which already holds both sides.** `cascadeGrainPi`'s owner is
`sec-glossary.tex:48 · P-glossary-mathematics`; `Policy`'s is `P-validated-R5 §3`. Both records are in
wm-nouns' 63-declaration holding, so the grain decision has exactly one holder and needs no cross-lane
coordination. It is a Gate 0 nouns question — *are the terms defined on the theory's terms* — which is that
vertex's charter sentence.

**Pattern impact:** R13's pattern is about *"scoring only the next action versus scoring the
pattern-language cascade/policy it opens"* — the same distinction. If the grain lands as proposed, R13's
entry in `sec-catalog.tex` should say which object π names, at gate time.

---

## APEX-D3 refused — and checking the refusal found that I had sent it at the wrong device

`bb20cd0`: refusal at the modelling decision. The delegate's grounds: the R2 corpus supplies 796
conforming / 2 mismatching at the pinned 798-form basis and does **not** determine the categorical
variable, the full/reduced model pair, their priors, the update rule, or any ε/Π → pseudo-count map —
and *"choosing these would let the apex manufacture the model it certifies — R18 one level higher."*
That is its charter applied to itself, and it is right.

**Checking it found a second, stronger reason, and it is against my packet.**

1. **BMR is R17's concept-merging device, not a claim-licensing one.** `sec-glossary.tex` on the threshold:
   *"It prevents the system from merging **concepts** just because they look superficially similar."* The
   R17 pattern: *"hand those to R17, which accepts only the mergers its evidence threshold clears."*
   `ΔF ≤ -3` answers *should these two concepts be merged*, not *is this claim licensed*.
2. **The count-driven configuration I commissioned has a recorded failure in this repository.**
   `holes/labs/slush-demo/findings/bmr_constellation_experiment.out.txt` opens
   *"EXPERIMENT 1: BMR over-merge reproduction (count-only reduce-concepts)"*, and the glossary cites it as
   a negative finding. **I asked for `evidence-source → Dirichlet counts` and licensing through it — the
   exact configuration already reproduced as a failure here.**

So my correction of APEX-D2 was itself half wrong: the chain is proved *for concept merging*; it was never
a claim-licensing device, and "only the source→counts map is missing" was wrong about what the map is for.

**The finding this leaves is bigger than the delivery that was asked for:**

    variationalFreeEnergy    precision-weighted surprise    terminates UNCONSUMED
    deltaFReduction + threshold   R17 concept merging, count-mode failure on record
    expectedInformationGain  REFUSED (Outcome/Q(o|π) missing)
    ------------------------------------------------------------------------------
    a device that licenses a CLAIM from evidence:  ABSENT from the formalisation

**The build has been gathering evidence with no instrument that can license a claim from it.** That is
§0.8's original complaint — *"there was no certification at the meta level that would say, this is the
wrong evidence"* — now located in the mathematics rather than in the process. APEX-D4 asks the delegate to
**record the absence and not fill it**, with a typed `:licensing-device :absent` and a falsifier, because
an apex that invents its own licensing rule is R18 one level further up.

**The seven modelling rulings are withdrawn** — they were downstream of my wrong premise. One question goes
to Joe in their place: *the build needs a claim-licensing device and AIF as formalised here does not supply
one; what should it be?*

**Method note.** Twice now I have handed this delegate a confident synthesis and been corrected by it —
first the two free energies, now the wrong device. Both times the delegate refused rather than complied,
and both times checking its refusal produced the real finding. **A vertex whose job is to say "this is the
wrong evidence" is worth most when it says it to its own commissioner**, and the second refusal was harder
to make than the first, because by then I had told it the answer twice.

---

## NOUNS-D3 — a Gate 0 vocabulary audit: four free energies and two π's *(Joe, 2026-08-31)*

Joe on the two free energies: *"that seems confusing! an issue for wm-nouns."* Checking it found **four**,
not two, and the same defect as the π question one level over — so both are dispatched as one delivery.

    G                     (risk eig)                     := risk π - eig π      :70   CLOSED, IN USE
    variationalFreeEnergy (precision error)              := ½·mean_k(Π_k·ε_k²)  :469  CLOSED
    expectedFreeEnergy    (outcomeKernel risk ambiguity) := sorry               :475  HOLE
    deltaFReduction       (A a' a A')                    := ln B(A)+…           :486  CLOSED

**`G` and `expectedFreeEnergy` are the same AIF quantity under two decompositions** — risk − epistemic gain
versus risk + ambiguity. **The one named `expectedFreeEnergy` is a `sorry`; the one actually grading
policies is called `G`.** A reader looking for expected free energy finds the hole and not the working
definition. **`deltaFReduction` reads as "the delta of F"** and is the model-reduction difference over
Dirichlet parameters instead. The glossary already says they are distinct *in prose* — and the prose did
not prevent the error, because **all four return bare `ℝ`**.

**The conflation is demonstrated, not hypothetical: I made it.** I told wm-evidence that evidence
accumulates as F and licenses at ΔF ≤ -3, chaining `variationalFreeEnergy` into `bayesFactorThreshold`.
It refused with the right diagnosis — *"sharing `ℝ` is not a semantic bridge."* **A distinct type would
have made that a compile failure rather than a delivered packet.** That is the argument for wrapping, and
the packet asks wm-nouns to weigh it against Lean's arithmetic and proof-ergonomics cost rather than
prescribing the fix — refusing to wrap, with reasons, is an acceptable deliverable.

**The two π's ride with it because they are the same fault.** `G {Policy : Type*}` shadows
`abbrev Policy := InformationState → Action`; the polymorphism means the mathematics never had a grain
conflict, only the naming did. Recommendation carried, not imposed: π is what G scores — the glossary's
scored cascade — and the state→action function needs its own name, on which reading `cascadeGrainPi` becomes
a definition and **no existing proof changes**.

**Gates required:** contract regenerates at 80 declarations with no new `sorry`; `holder_check.clj` still
passes; and an explicit statement of whether **R13's pattern entry** changes, since R13 is about *"scoring
only the next action versus scoring the pattern-language cascade/policy it opens"* — the π distinction
exactly, and patterns are now updated at gate time.

---

## APEX-D4 gated — the absence is typed, and the obvious candidate is not merely wrong but vacuous

`b8dbf32`: `P-evidence-apex.md` +58 lines. Typed result **`:licensing-device :absent`**, with a real
falsifier — *"a declaration accepting typed claim/evidence inputs and returning licensed/refused/unknown
under stated semantics, demonstrated by accepting and refusing cases without `sorry`"*. Note that the
falsifier requires **both** an accepting and a refusing case: an absence whose falsifier could be
discharged by a device that only ever accepts would be the same defect it is recording.

**What I verified, and it is worse than "over-merging".** The delegate cited the count-only BMR
configuration as having *"collapsed 118 patterns into one"*. The recorded experiment says:

    EXPERIMENT 1: BMR over-merge reproduction (count-only reduce-concepts)
    Patterns (in >=2 missions): 118
    Pairs scored: 6903
    Accepted (dF <= -3): 6903 / 6903
    Rejected: 0
    delta-F range: [-33.70, -6.45]

**Every pair passed. Zero rejections.** So in the count-only configuration the `ΔF ≤ -3` threshold is not
a threshold at all — it is **an acceptance that cannot reject**, which is this build's oldest and most
frequent defect, sitting reproduced in the findings directory since before today.

**That closes the question I had left half-open.** I had thought the licensing chain was proved and only
the `evidence-source → Dirichlet counts` map was missing. Had anyone built that map, the threshold would
have **licensed every claim put to it** — the device would not have been merely mis-purposed, it would have
been vacuous on arrival. The delegate's refusal saved a delivery that would have produced a certifier
incapable of declining.

**The three non-solutions now stand recorded with pinned evidence:** perceptual F ends as an unconsumed
surprise value; BMR/ΔF licenses R17 concept reductions and collapses under count-only inputs; EIG is
policy-directed and REFUSED (`Outcome`/`Q(o∣π)` missing). The seven Dirichlet rulings and APEX-D3's
proposed suffix are explicitly withdrawn.

**One question goes to Joe, and it is now well-founded rather than speculative:** *the build needs a
claim-licensing device; AIF as formalised here does not supply one; what should it be?* The apex's first
output is the honest statement that the instrument it was created to wield does not exist — which is
§0.8's complaint located in the mathematics, and exactly what an apex is for.

---

## APEX-D4's absence WITHDRAWN — I imported a verdict frame the theory does not use *(Joe, 2026-08-31)*

Joe: *"I answered this question already, and AIF does say what to do actually."* He did, before APEX-D2:
*"if `:self` evidence is passed forward, it needs **processing** … it is basically a bare perception or
datum. I think AIF will have better things to say about how that processing should work than I will."*

**I asked for a device returning licensed / refused / unknown. That is adjudication, and AIF has no such
step.** A claim does not become licensed — **it acquires a posterior.** I imported the shape from
verdict-giving, commissioned three deliveries inside it, and then had the delegate record the theory's
failure to supply what the theory never had.

    BeliefState : Type                        HOLE   falsifier: "a channel lacks its mean or VARIANCE"
    observationKernel (State Observation)     HOLE   the Markov kernel A : S ⇝ O
    predictionError (observation beliefMean)  CLOSED ε_k := o_k − μ_k   (consumes the belief's MEAN)
    variationalFreeEnergy (precision error)   CLOSED F = ½·mean_k(Π_k·ε_k²)

**The variance is what I kept missing.** I argued at length that the build could not represent graded or
plural evidence — several partial sources, differing quality, accumulation toward a threshold. **A belief
state with a mean and a variance per channel is exactly that representation.** Strong independent evidence
narrows the variance; weak self-evidence barely moves it; sources combine by ordinary updating. The
precision operations wm-evidence had already named — review, cross-validation, independent reproduction —
are precision on the update, where they always belonged.

**And `variationalFreeEnergy` never "terminated unconsumed".** F is what belief update *minimises*. I read
it as a dead end because I was looking for something to consume it as a verdict input. The chain was
complete; my frame was wrong.

**So the apex needs no new device — it needs `BeliefState` and `observationKernel` discharged**, two of the
glossary's eight unbound holes. Third convergence on the same lane. And R1's pattern is already named
**"Belief State as Operational Hypotheses — makes the state updateable and falsifiable"**: the apex's claims
should *be* operational hypotheses in a belief state, which is R1 applied at the meta level.

APEX-D5 dispatched to withdraw the absence (dated, original legible), state the corrected shape, and name
what must be discharged — explicitly *not* defining `BeliefState`, which is wm-nouns' holding.

**The honest tally.** Four of my directions to this delegate were wrong: two free energies conflated; the
wrong device; a threshold that accepts 6903/6903; and now the verdict frame itself. It refused three
correctly. **The fourth it could not refuse, because I never relayed the operator's actual answer** — Joe
gave it in plain words and I went looking for a threshold instead. That is a relay failure, and it is the
most expensive kind, because a delegate cannot check a premise it was never shown.
