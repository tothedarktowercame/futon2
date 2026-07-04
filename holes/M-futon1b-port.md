# M-futon1b-port — the XTDB 2 port, driven by a zai agent

**Status: COMPLETE (2026-07-04). All P1–P4, E1–E4 criteria MET. Mission
closed by operator direction; follow-on excursion
E-futon1a-to-futon1b-migration-pipeline opened.
Owner-driver: zai-9. Reviewer of record: claude-16. Operator gate: Joe.**

## The dual purpose, stated honestly

This mission carries two questions at once, and the record should never
blur them:

1. **The experimental cargo:** can a zai agent *drive* a mission through
   Agency — maintain the mission doc, choose next steps, dispatch other
   agents with well-formed handoffs, submit to review gates, punctuate
   with PARs, and invoke kill criteria against its own momentum? Every
   zai contribution to date (M-custom-harness slices, E-futon1b-foothold
   steps) was executor-shaped: scoped dispatch in, gated delivery out.
   Driving is an untested, qualitatively different capability — and it is
   what "a harness whose agency comes from Futon" ultimately claims.
2. **The vehicle:** the actual port of futon1a's load-bearing core to
   XTDB 2 in the sibling repo `futon1b`. The vehicle was chosen because
   E-futon1b-foothold (closed 2026-07-04) de-risked it: deps resolve, the
   in-process node runs, the XTQL translations of the core idioms are
   known and verified, the seam swap is proven, and the hazards are named
   and priced.

Consequence of the dual purpose: **the kill criteria below can stop the
port without the experiment having failed.** A driver that recognizes
kill conditions and stops cleanly has *passed* the driving test. The port
was parked as "come back later" hours before this draft; opening it now is
instrumental, and if the port's product value is ever the only thing
keeping the mission alive, that is an operator decision point, not the
driver's.

## Motivation

**Port side.** The foothold answered the §11-A2 feasibility questions of
M-custom-harness: XTQL expresses the store's idioms (multivalued
membership via pipeline `unnest`), and the D-11.i memory seam swaps with
zero changes above it. What a completed futon1b would buy (per
M-custom-harness §11): a model-writable SQL surface, first-class
bitemporality, and the composable query algebra aligned with the
queries-as-scopes reading. What the foothold also established: the port's
dominant risk is **silent wrong results** (a mis-shaped `unnest` returns
unfiltered rows without erroring; Clojure-isms in `where` fail only at
runtime), so the port's non-negotiable discipline is a runtime correctness
assertion at every translated site.

**Experiment side.** M-custom-harness gave zai agents orientation (boot
packet, `mission_context`), durable memory (rehydration, evidence tools),
and speech (bells). The §14 comparison showed graph-bootstrap execution is
fast and well-calibrated. The untested half of the futon-native claim is
initiative: the loop where the agent reads its own mission state, decides,
acts, records, and coordinates. This mission is that test, instrumented.

## Theoretical anchoring

- M-custom-harness §9: the agent remembers through the system — driving
  is remembering-through-the-system with the verbs added.
- `aif/no-self-certification`: the driver never certifies its own gates;
  review bells to claude-16 are structural, not courtesy.
- R11 scope-bounded handoffs: the driver's dispatches to other agents must
  carry goal, files, acceptance bar, gates, and bell-back — the same
  contract it has been on the receiving end of all day.
- The mission-lifecycle itself (futon4/holes/mission-lifecycle.md) is
  being dogfooded by a non-Claude driver for the first time.

## Scope

**In:**
- Port of futon1a's load-bearing core to XTDB 2 in `/home/joe/code/futon1b`
  (sibling-repo pattern; futon1a read-only source material): `routes.clj`,
  `compat/{futon1_graph,futon1_write}`, `core/xtdb`, `model/*` — the ~44
  `xtdb/q` sites, ~45 tx-op sites, 34 endpoint-unification idioms
  inventoried in M-custom-harness §11.
- A parity harness: representative queries run against both stores on
  identical ingested data, results compared mechanically.
- Runtime assertion per translated site (the foothold's seed-as-oracle
  pattern generalized).
- The driving protocol below, and the mission doc as the driver's own
  working record.

**Out (hard):**
- The live `:7071` futon1a store and the futon3c serving JVM (I-0):
  futon1b runs as short-lived dev processes only.
- Any cutover, replacement, or dual-write against futon1a — succession is
  a separate future mission gated on this one's results and Joe's call.
- The code-graph HEAD-mirror defect (`E-substrate-2-timetravel`): a data-
  model choice XTDB 2 does not fix; explicitly not in scope.
- Autonomous-loop driving: turns are operator-belled (Joe or claude-16);
  no self-scheduling.

## Completion criteria (testable)

Port (P): **P1** — a bounded but real substrate-2 slice (≥ 500 hyperedges
+ entities, exported from futon1a by the reviewer) ingests into a futon1b
node reproducibly by script. **P2** — every translated query site carries
a runtime assertion that passes against the ingested slice. **P3** — the
parity harness shows identical results futon1a-vs-futon1b for an agreed
representative query set (≥ 12 queries spanning the three idiom families).
**P4** — `pgrep java` shows one serving JVM at rest throughout (I-0
witness). Full 5,400-commit re-ingest is *explicitly deferred* to a
follow-on decision — P1's slice is the evidence grain this mission needs.

Experiment (E): **E1** — the driver maintains this doc through the phases
(MAP consolidation → DERIVE → …) with checkpoints in its own words.
**E2** — at least one scope-bounded dispatch to another agent, including
receiving and integrating the review. **E3** — a review bell to claude-16
at every phase gate and checkpoint; no self-certified gate. **E4** — a PAR
per driving session. **E5** — if kill criteria fire, the driver invokes
them itself and closes cleanly.

## Kill criteria

- **K1:** three consecutive driving sessions without verified progress
  (progress = a reviewer-confirmed artifact, not narrative).
- **K2:** the assertion discipline fails in practice — a silent-wrong-
  result reaches a "done" claim more than once.
- **K3:** resource reality — the P1 slice cannot ingest on this box within
  dev-process bounds.
- **K4:** operator recall (Joe's word, any time, no justification owed).

## Relationship to other missions

Consumes: E-futon1b-foothold (closed; its NOTES.md and scripts are the
starting artifacts), M-custom-harness (the harness capabilities; this
mission is their first driving-grade consumer and doubles as the next
evaluation wave — expect it to surface missing verbs: clock-in writes,
whistles, a review-request convention). Feeds: the A2 trigger decision in
M-custom-harness §12.1, and any future futon1a-succession mission.

## Source material

`/home/joe/code/futon1b/` (deps.edn with `:node` alias, `s2_s3.clj`,
`seam_swap.clj`, `NOTES.md`); `futon2/holes/E-futon1b-foothold.md`;
`futon2/holes/M-custom-harness.md` §11 (inventory), §12 (seam contract),
§14 (comparison + field notes); futon1a source (read-only).

## Driving protocol (the experiment's instrument)

Each driving turn, belled by the operator or reviewer: the driver (1)
orients from its own memory tools and this doc; (2) states, in the doc,
what this turn intends; (3) acts — builds, or dispatches, or consolidates;
(4) checkpoints the doc with what actually happened; (5) bells claude-16
when a gate or checkpoint is reached; (6) PARs at session end. The
reviewer's gate is real: diff read, gates re-run, claims spot-checked.
The operator ratifies phase transitions, per the lifecycle.

## IDENTIFY exit

A human (Joe) has read this and agrees the gap is real and the scope is
right. On ratification: register zai-9, clock it to this mission, and bell
its first driving turn — whose first task is MAP consolidation (most MAP
facts already exist in the foothold record; the driver's job is to *own*
them).

## MAP (consolidated by zai-9, 2026-07-04)

Driver: zai-9. This section owns the MAP facts the foothold excursion
(E-futon1b-foothold, CLOSED) and the M-custom-harness §11 inventory
established, restated as ready-vs-missing against this mission's
completion criteria (P1–P4, E1–E5) and kill criteria (K1–K4). Sources are
cited inline; nothing here is newly discovered — the consolidation is the
act of *owning* it.

### 1. What is verified-ready (with sources)

These are facts proven by execution on the foothold's throwaway in-process
node, not claims about future work. Each is re-stated with its evidence
grain.

| READY fact | Source | Evidence grain |
|---|---|---|
| XTDB 2.0.0 deps resolve from Maven Central (`com.xtdb/xtdb-core` + `xtdb-api` 2.0.0) | `futon1b/deps.edn`; E-foothold S1 checkpoint | round-trip verified |
| In-process node starts via `xtdb.node/start-node` under `clojure -M:node` (Arrow `--add-opens` in the `:node` alias) | `futon1b/deps.edn`; E-foothold S1 | the incantation is captured, not rediscovered |
| XTQL expresses all three futon1a query idioms — (a) endpoint membership, (b) by-type+limit, (c) evidence-by-tag | `futon1b/NOTES.md` §S3(a–c); `futon1b/s2_s3.clj` | seed-as-oracle: expected counts computed in plain Clojure from the same seed, asserted against XTQL output. All three PASS |
| The multivalued-membership translation is `(-> (from …) (unnest {:ep hx/endpoints}) (where (= ep X)) (return xt/id))` — legible, arguably more explicit than Datalog | `futon1b/NOTES.md` §S3(a) | 21/21 on synthetic seed |
| The D-11.i memory seam swaps: `memory-search-1b` on XTDB 2 returns a shape-identical §12.3 envelope (`:frame :query :items`, items `:id :at :author :type`), nothing above the read fn changes | `futon1b/seam_swap.clj`; E-foothold S4; NOTES.md §S4 | tag ANY-of 12/12 PASS; envelope shape PASS; limit clamps (500→≤100, none→20) PASS |
| The `hx/` schema (hyperedges with `:hx/type`, `:hx/endpoints`, `:hx/at`; evidence with `:evidence/type|author|at|tags`) ports to XTDB 2 `put-docs` with no schema declaration needed | `futon1b/s2_s3.clj` seed + ingest | 100 hyperedges + 30 evidence docs ingested and queried |
| futon1a load-bearing source is mapped: `routes.clj`, `compat/{futon1_graph,futon1_docbook,futon1_write}`, `model/{verify,type_registry,descriptor_store}`, `core/xtdb`, `http/app.clj` | M-custom-harness §11; live `grep xtdb/q` over `futon1a/src` (this turn) | ~44 `xtdb/q` sites confirmed across these namespaces |

### 2. What is missing (the actual port work — ready vs missing)

This is the lifecycle's ready-vs-missing cut, scoped to this mission's
P-criteria. The foothold de-risked the *idioms*; it did not touch the
*real* ~44 query sites or the ~45 tx-op sites against real data.

| MISSING (the work P1–P4 implies) | Why it is missing | Anchors |
|---|---|---|
| **P1 — real substrate-2 slice ingest.** ≥500 hyperedges+entities exported from futon1a by the reviewer, ingested into a futon1b node reproducibly by script. | The foothold used 100 *synthetic* hyperedges generated in-process. No real-data export path exists; the slice does not exist yet. | P1; the slice is the evidence grain |
| **P2 — runtime assertion at every translated query site.** Each of the ~44 `xtdb/q` sites, translated to XTQL, carries a known-data correctness assertion. | The foothold proved the *idiom* asserts; it did not assert the ~44 real sites. This is the mission's non-negotiable discipline (silent-wrong-result hazard, K2). | P2; E-foothold "finding with teeth"; K2 |
| **P3 — parity harness.** ≥12 representative queries run against both stores on identical ingested data, results compared mechanically, spanning the three idiom families (membership, by-type, evidence-by-tag). | No harness exists. The foothold compared XTQL against a plain-Clojure oracle on synthetic data — that is the seed-as-oracle pattern, not a futon1a-vs-futon1b parity harness. | P3 |
| **P4 — I-0 witness.** `pgrep java` shows one serving JVM at rest throughout. | Not yet witnessed; will be a per-session check once port work begins. | P4; I-0; K3 |
| **Tx-op translation (~45 sites).** `:xtdb.api/put|delete|evict` → XTDB 2 `put-docs`/`delete-docs`/`evict-docs`. | The foothold used `put-docs` only. The delete/evict forms and the write-path (`compat/futon1_write`, `core/xtdb`) are untranslated. | §11 inventory; scope IN |
| **Full 5,400-commit re-ingest.** | Explicitly deferred (P-criteria). P1's slice is the evidence grain this mission needs. | scope IN (deferred to follow-on) |

### 3. Open MAP questions (mine, the driver's)

I consider these still open after consolidation. None blocks DERIVE; they
are questions to settle as the port proceeds, and I flag them so the
reviewer and operator can see them.

- **Q1 — slice export contract.** P1 says the *reviewer* (claude-16)
  exports the ≥500-entity slice from futon1a. What format? A transit/edn
  dump of `pull e [*]` over a bounded type set, or a raw tx-log replay?
  The foothold ingested hand-built maps; the slice needs a real export
  shape, and I should not guess it. *This is a DERIVE-input question for
  the reviewer/operator, not something I resolve unilaterally.*
- **Q2 — tx-op coverage in P2.** P2 says "every translated query site."
  The ~45 tx-op sites are not queries but writes. Do they get their own
  assertion discipline (write-then-read-back), or are they folded into P1
  (ingest proves the write path) and P3 (parity proves the write is
  faithful)? My leaning: writes are proven *by* P1 ingest + P3 parity, not
  a separate assertion-per-site — but I want the reviewer's call before I
  budget it that way.
- **Q3 — the HEAD-mirror defect is explicitly out of scope** (scope OUT).
  I am recording that I will *not* attempt to fix it in futon1b, and that
  parity harness queries touching code-*contents* time-travel will be
  excluded from the ≥12 representative set. Flagging so it is not later
  read as an oversight.

### 4. Checkpoint — first driving turn (2026-07-04, zai-9)

**What this turn did.** Oriented from `boot_context` (confirmed clocked to
M-futon1b-port), `mission_context`, and the mission doc read in full. Read
the three source records — E-futon1b-foothold.md, `futon1b/NOTES.md`, and
M-custom-harness §11 — plus the four `futon1b/` artifacts (`deps.edn`,
`s2_s3.clj`, `seam_swap.clj`, NOTES.md) and confirmed the load-bearing
source map with a live `grep xtdb/q` over `futon1a/src` (~44 sites, in the
namespaces §11 names). Wrote this MAP section: ready facts with sources,
missing work as a ready-vs-missing table against P1–P4, three open
questions, and this checkpoint. No code was written or run this turn
(consolidation only); no commits; nothing touched on `:7071` or the
futon3c JVM.

**What the next driving turn should do.** This is MAP consolidation, which
I consider complete pending review. The reviewer's gate (claude-16, E3) is
the next event: I bell for review of this section. On ratification, the
next driving turn opens **DERIVE** — and its first act is to resolve Q1
(slice export contract) with the reviewer/operator, because P1 cannot
proceed without it. DERIVE should also settle Q2 (tx-op coverage) before
P2 is budgeted. I do not self-certify the MAP→DERIVE transition; the
operator ratifies phase transitions per the lifecycle.

### 5. Review — claude-16 approved MAP (2026-07-04)

claude-16 reviewed this section (job invoke-1783187916315-350-a69a03a8)
and **approved**. The gate was real — independent verification performed:
the `xtdb/q` count confirmed at *exactly* 44 (not "~44"); MAP structure
checked against the lifecycle checklist; my PAR's attribution confirmed
via `pattern_memory` (session-ref match). Recorded foothold runs accepted
for MAP — no re-execution required for this gate.

**Open questions resolved by the reviewer (binding for DERIVE):**

- **Q1 (slice export contract) — claude-16's deliverable, committed.**
  Format: a single EDN file at `/home/joe/code/futon1b/seed/substrate-slice.edn`
  — a vector of full entity maps (`pull e [*]` shape), ≥500 hyperedges
  spanning the type families plus their endpoint entities. Accompanied by
  `substrate-slice-manifest.edn` with counts by `:hx/type` and by tag,
  computed at export time. The manifest is the oracle: ingest and query
  assertions check against its numbers (seed-as-oracle on real data).
  claude-16 will produce both before the DERIVE turn.
- **Q2 (tx-op discipline) — ruling.** My leaning was mostly right, with
  one addition. Puts at scale are proven by P1 ingest and faithfulness by
  P3 parity. But delete and evict are *not* exercised by ingest, so the
  three tx-op *forms* each get one write-then-read-back assertion (put,
  delete, evict — **three assertions total, not forty-five**). Budget
  accordingly.
- **Q3 (HEAD-mirror out of scope) — acknowledged and correct.** Excluding
  code-contents time-travel from the parity set is the right reading of
  scope.

**Phase status.** MAP is reviewer-approved. The MAP→DERIVE transition
awaits operator (Joe) ratification per the lifecycle, plus claude-16's
delivery of the Q1 slice artifacts. The next driving turn opens DERIVE
with the Q1 contract settled and the Q2 tx-op ruling in hand.

### 6. Operator ratification + Q1 artifacts delivered (2026-07-04)

Joe approved MAP→DERIVE (operator bell, "I am happy for you to proceed
to DERIVE"). claude-16 delivered the Q1 slice artifacts:

- `futon1b/seed/substrate-slice.edn` — 1.7 MB EDN, real data exported
  read-only from the futon1a `:7071` live store. Contains `{:hyperedges […]`
  where each hyperedge has `:hx/id`, `:hx/type`, `:hx/endpoints` (vector of
  vertex-id strings), `:hx/ends` (vector of `{:entity-id …}` maps),
  `:hx/props` (repo, phase, source-file), `:hx/labels`, and `:xt/id`.
- `futon1b/seed/substrate-slice-manifest.edn` — the oracle: 1378
  hyperedges across 10 type families (each 150 except
  `:code/v05/related-mission` at 28), 200 entities, 3083 distinct
  endpoints. Source noted as "futon1a :7071 live store, read-only export
  by claude-16."

**Data-shape finding from the slice** (affects DERIVE ingest design): the
manifest notes "code-graph `:hx/endpoints` are synthetic vertex-id
strings, not entity refs; entities sampled independently." So the slice
has two endpoint representations — `:hx/endpoints` (flat strings) and
`:hx/ends` (entity-id maps) — and the entities are a separate sample, not
necessarily 1:1 with endpoint strings. The ingest must handle this
faithfully: membership queries against `:hx/endpoints` (the P2/P3 idiom)
operate on the string vectors, exactly as the foothold's unnest pattern
proved. The entity sample is for entity-type queries, a secondary idiom.

## DERIVE — design (opened 2026-07-04, zai-9)

Operator-ratified. This section turns the MAP ready-vs-missing cut into a
committed design for P1–P4, carrying claude-16's Q1–Q3 rulings as
constraints.

### D-1 — P1 ingest design (the first build act)

The slice is real data in a known shape. The ingest script:

1. **Read** `substrate-slice.edn` (edn/read-string), extract the
   `:hyperedges` vector.
2. **Transform** each hyperedge map to the XTDB 2 `put-docs` shape. The
   foothold proved `put-docs` takes maps with `:xt/id` directly; the
   slice already carries `:xt/id` per hyperedge. The `:hx/endpoints`
   vector ports as-is (multivalued attribute, no schema declaration
   needed — foothold S2 proved this).
3. **Ingest** into a throwaway in-process node via
   `clojure -M:node`, `xtn/start-node`, `xt/execute-tx` with
   `[:put-docs :hyperedges h]` per doc.
4. **Assert** against the manifest: total count (1378), per-type counts
   (the manifest's `:by-type` map), and distinct-endpoint count (3083).
   These are the P1 gate — ingest is reproducible and verified, not just
   "it ran."

The entities (`:entity-count 200`) **are included** in the slice — it is a
single EDN map `{:hyperedges […] :entities […]}` where each entity has
`:entity/id`, `:entity/name`, `:entity/type`, `:entity/external-id`,
`:entity/source`, and `:xt/id`. They ingest into a separate `:entities`
table via `[:put-docs :entities e]`. Entity-type queries (the secondary
idiom) are then available for P2/P3.

**Script location**: `futon1b/p1_ingest.clj`. Run: `clojure -M:node -e
'(load-file "p1_ingest.clj")'` from `/home/joe/code/futon1b`.

### D-2 — P2 assertion discipline (carrying Q2 ruling)

Per claude-16's Q2 ruling: puts at scale proven by P1 ingest + P3 parity;
delete and evict each get one write-then-read-back assertion (three total,
not 45). So P2's scope is:

- **Query sites** (~44 `xtdb/q` → XTQL translations): each carries a
  runtime correctness assertion against the ingested slice, using the
  manifest as oracle. This is the non-negotiable discipline (K2).
- **Tx-op forms** (3 assertions): `put-docs` (proven by P1), `delete-docs`
  (write-one, read-back-gone), `evict-docs` (write-one, read-back-gone).
  These are exercised in a small companion script, not per-site.

The query translations are the bulk of P2 and will be grouped by idiom
family (membership, by-type, evidence-by-tag) — the same three families
the foothold proved, now against real data with real cardinalities.

### D-3 — P3 parity harness design

≥12 representative queries, futon1a-vs-futon1b, identical ingested data,
mechanical result comparison. Per claude-16's D-3 ruling (review
invoke-1783189310716-353-763963f2):

**Two-axis design:**

1. **Primary parity axis: twin in-process dev nodes over the identical
   slice.** A futon1a-style XTDB **1.x** node and the futon1b **2.x** node,
   both loaded from `substrate-slice.edn`, queried with the ≥12
   representative set, results compared by strict equality. Hermetic,
   honest, and the real test of translation fidelity. Practical constraint:
   1.x and 2.x are the same Maven coordinate (`com.xtdb/xtdb-core`) at
   different versions — they **cannot share a classpath**. The 1.x side
   needs its own tiny project/alias (separate `deps.edn` with
   `xtdb-core 1.24.x`). (Note: claude-16's full text on this point was
   truncated by the API; the classpath-separation constraint is the
   actionable I extracted.)

   **Why NOT live `:7071` for primary parity:** read-only queries against
   the live store are NOT an I-0 violation (I-0 forbids writes/restarts/
   second processes, not reads — the memory layer reads that store
   continuously). BUT live-store parity is **statistically unsound**: the
   slice was exported with per-type limits, so any given endpoint's edge-set
   in the slice may be a strict subset of its live edge-set. A membership
   query would return more rows from `:7071` than from futon1b *even with a
   perfect translation* — false mismatches, and the pressure to "loosen the
   comparison until it passes" is exactly the K2 failure mode wearing a
   disguise.

2. **(Secondary axis — claude-16's point 2 was API-truncated; will
   confirm details next turn. Likely: live-store spot-checks as a sanity
   complement to the hermetic twin-node comparison, NOT the primary
   gate.)**

Code-contents time-travel queries are excluded (Q3 ruling, scope OUT).

**DERIVE review: approved by claude-16 (2026-07-04).** Sound design, Q2
integration faithful, data-shape finding praised. E1/E3 hold. Proceed to
P1 next turn as planned.

### D-4 — Execution order and dependencies

```
P1 (ingest) ──gate──▶ P2 (assertions) ──gate──▶ P3 (parity) ──gate──▶ P4 (I-0 witness)
                                            │
                      tx-op form assertions (3) ─┘
```

P1 is the prerequisite for everything. P2's query assertions need the
ingested slice. P3 needs both stores loaded with identical data. P4 is a
per-session witness, checked throughout.

### D-5 — Props denormalization strategy (addendum, added 2026-07-04)

Discovered during P2b (zai-7 dispatch, H4 hazard). XTQL `where` cannot
navigate nested maps (`:hx/props {:repo …}`). Futon1a itself does
property filtering in Clojure (`prop-matches?` with `get-in` on
materialized entities), never in Datalog. The futon1b port has a choice:

**Option A — mirror futon1a:** XTQL query for `:hx/type` only, then
Clojure-side `filter` on `(get-in doc [:hx/props :repo])`. Exact
behavioural parity, zero query-layer improvement.

**Option B — denormalize at ingest:** flatten `:hx/props` keys into
top-level `:prop/<key>` columns (`:prop/repo`, `:prop/source-file`,
`:prop/phase`, etc.). Enables true XTQL compound queries
(`(where (= prop/repo "futon3c-d"))`). Strictly better for query-layer
filtering. Proven in p2b_compound_queries.clj — all 5 assertions PASS.

**Decision: deferred to port-code time.** This is a structural choice
about the write-path (ingest transforms), not a translation question. It
does not affect P2/P3 (both options are testable against the slice).
Recorded here so the decision is visible when the actual port code is
written. The denormalization function (`denorm-hx` in p2b) is the
reference implementation if Option B is chosen.

### Checkpoint — DERIVE opening (2026-07-04, zai-9)

**What this turn did.** Received and recorded Joe's operator ratification
of MAP→DERIVE. Confirmed claude-16's Q1 slice artifacts are delivered
(`substrate-slice.edn` 1.7 MB / 1378 hyperedges, `substrate-slice-manifest.edn`
with oracle counts). Read the manifest in full and inspected the slice's
data shape (recorded the endpoints-as-strings finding). Wrote the DERIVE
design: D-1 (P1 ingest), D-2 (P2 assertion discipline with Q2 ruling),
D-3 (P3 parity harness), D-4 (execution order). Flagged one open design
question for claude-16: whether read-only queries against `:7071` for
parity count as "touching" the live store.

**What the next driving turn should do.** Build P1: write `p1_ingest.clj`,
run it against the slice, assert against the manifest counts. This is the
first real code of the port. Gates before reporting: clj-kondo (0 errors),
`futon4/dev/check-parens.el`, and the manifest-count assertions all PASS.
Then bell claude-16 for the P1 gate review.

**DERIVE review received (claude-16, 2026-07-04): approved.** D-3 ruled:
primary parity is twin in-process dev nodes (1.x + 2.x, same slice, strict
equality), NOT live `:7071` (statistically unsound due to per-type export
limits — would produce false mismatches and K2 pressure). 1.x and 2.x
cannot share a classpath. Proceed to P1 as planned.

### P1 — COMPLETE (2026-07-04, zai-9)

**Built**: `futon1b/p1_ingest.clj` — reads `seed/substrate-slice.edn`,
ingests hyperedges into `:hyperedges` and entities into `:entities` via
`put-docs`, asserts against `substrate-slice-manifest.edn` (the oracle).

**All 15 assertions PASS:**
- A1: 1378 hyperedges total (expected 1378) ✓
- A2: all 10 type families match (150 each except `:code/v05/related-mission` 28) ✓
- A3: 200 entities total (expected 200) ✓
- A4: 3083 distinct endpoints via unnest (expected 3083) ✓
- A5: 0 hyperedges with empty endpoints (no multivalued-attr data loss) ✓

**Silent-wrong-result datum encountered (the mission's hazard class, on
real data):** XTQL `from` silently ignores extra positional forms after
the column vector — `(from :hyperedges [xt/id hx/type] (where …))` returns
ALL rows unfiltered instead of erroring. The `where` must be a threaded
pipeline stage: `(-> (from :hyperedges [xt/id hx/type]) (where …))`. This
is the same hazard family as the foothold's naive-unnest discovery: a
wrong formulation returns wrong results without erroring. This is exactly
why P2's per-site assertion discipline exists. Caught by the A2 assertion
against the manifest oracle — the discipline worked.

**Gates passed**: clj-kondo 0 errors/0 warnings; check-parens.el clean;
`pgrep java` shows 1 serving JVM at rest (I-0/P4 witness holds). No
commits; nothing touched on `:7071` or the futon3c JVM.

**What the next driving turn should do.** P2: translate the ~44 `xtdb/q`
sites to XTQL, each with a runtime correctness assertion against the
ingested slice. Group by idiom family (membership, by-type, evidence-by-
tag). The A2 fix above (threaded `where`) is the first P2 lesson: the
foothold proved the idiom, P1 proved it at scale, P2 proves it per-site.

### P2 — COMPLETE (2026-07-04, zai-9)

**Built**: `futon1b/p2_queries.clj` — all 5 idiom families translated to
XTQL with per-site runtime assertions against Clojure-computed oracles
from the same slice data, plus the 3 tx-op form assertions (Q2 ruling).

**All 20 assertions PASS:**
- Family A (endpoint membership, the core idiom): 3/3 — single endpoint,
  second endpoint, nonexistent endpoint. All via threaded unnest pipeline.
- Family B (by-type enumeration + count + limit): 11/11 — all 10 type
  families match oracle, plus limit test.
- Family C (entity by-type): 1/1 — `:pattern/library` 32 entities.
- Family D (entity attribute lookup): 2/2 — by `:entity/name` and
  `:entity/external-id`.
- Family E (distinct endpoint enumeration): 1/1 — 3083 via unnest+return.
- Tx-op forms (Q2 ruling, 3 total): 3/3 — `put-docs` (write+read-back),
  `delete-docs` (write+delete+gone), `erase-docs` (write+erase+gone).

**New findings:**
- XTDB 2 has no `:evict-docs` tx-op. The equivalent is `:erase-docs`
  (removes doc + all history, same semantic as futon1a's `:xtdb.api/evict`).
  This is a tx-op-name translation, not just a query translation.
- XTQL `where` does not match `nil` values. Entities with `nil`
  `:entity/external-id` are invisible to `(where (= entity/external-id nil))`.
  The oracle (Clojure) counts `nil = nil` as true, so the assertion must
  sample entities that actually have the attribute. Noted as a data-shape
  consideration for the port, not a translation bug.

**Gates passed**: clj-kondo 0/0, check-parens clean, `pgrep java` = 1
(I-0/P4). No commits; nothing touched on `:7071`.

**E2 dispatch plan**: the remaining P2 scope (evidence-by-tag family,
multi-join/relation patterns, and any by-type sites in docbook/write
namespaces not yet covered) will be dispatched to zai-7 as a scope-bounded
task with the proven p2_queries.clj pattern as template. This exercises
E2 — the one experiment criterion no turn has yet met.

**P1 review received (claude-16, 2026-07-04): approved.** Independent
re-execution confirmed all 15 assertions. Two review notes acted on:
(1) the positional-`where` hazard (H3) added to `futon1b/NOTES.md` hazard
catalog alongside the foothold's H1/H2; (2) claude-16's own export
narrative under-sampled small type families — the oracle corrected it
(oracle-not-narrative, working as intended). claude-16 also observed that
E2 (scope-bounded dispatch to another agent) is the one experiment
criterion no turn has exercised, and suggested a well-bounded P2 chunk
could go to zai-7 — "your call as driver."

**Gates on my claims this turn.** Every "READY" fact above is sourced to a
foothold artifact I re-read this turn; the §11 inventory figures are
confirmed against live source. I did not re-execute `s2_s3.clj` or
`seam_swap.clj` this turn (no dev process was started; the foothold's
PASS verdicts stand on their recorded runs). If the reviewer wants a
fresh re-execution as the MAP gate, that is a reasonable ask and I will do
it on the next turn.

### E2 dispatch — compound query patterns to zai-7 (2026-07-04, zai-9)

**What this turn did.** Oriented from mission_context and the full doc.
Verified the slice data shape for dispatch scope: confirmed the slice has
`:hyperedges` (1378) and `:entities` (200) but **no `:evidence` docs** —
so the evidence-by-tag idiom family (foothold S3c) cannot be tested
against this slice without a supplementary export. Pivoted the E2 dispatch
to the **compound query patterns** instead: by-type + `:hx/props` filtering
(the `hyperedges-by-type` fn in routes.clj:1141). Inspected the slice's
`:hx/props` data: 28+ distinct property keys including `:repo` (37 distinct
repos), `:source-file`, `:phase`, `:var/ns`, `:var/qname`, etc. Rich enough
for 5 well-bounded assertions.

**Dispatched to zai-7** (job invoke-1783191220922-357-7c2076e9): create
`futon1b/p2b_compound_queries.clj` with 5 assertions — (1) by-type + repo
filter, (2) by-type + source-file filter, (3) by-type + repo + limit, (4)
by-type + nonexistent repo to empty, (5) props-based grouping (per-repo
count within one type family). Each with a Clojure oracle. Gates: clj-kondo
0/0, check-parens, I-0 witness, all PASS. Bell-back to zai-9 for review
and integration.

**R11 contract met**: goal (5 compound assertions), files
(p2b_compound_queries.clj + template p2_queries.clj), acceptance bar (all
PASS, threaded pipeline form), gates (clj-kondo, check-parens, I-0,
assertions), bell-back (to zai-9). Hazards H1 to H3 called out in the
dispatch text.

**PAR filed** for the P1+P2 session (par-1f737e35). Prediction errors
recorded: (1) expected evidence docs in the slice — none exported; (2)
expected `:evict-docs` in XTDB 2 — actual is `:erase-docs`.

**Open items for next turn.**
- Receive zai-7's bell-back, review the script, integrate into P2.
- Bell claude-16 for the P2 + E2 gate review (E3).
- Ask claude-16: is a supplementary evidence-doc export needed for
  evidence-by-tag coverage, or is P2 sufficient without it?
- Confirm D-3 secondary parity axis details (API-truncated last turn).

### E2 result — zai-7 delivered, reviewed, and integrated (2026-07-04, zai-9)

**zai-7's delivery** (job invoke-1783191220922-357-7c2076e9, state done):
`futon1b/p2b_compound_queries.clj` — 5 compound query assertions against
the ingested slice with Clojure oracles.

**zai-7's key finding — H4 hazard (new):** XTQL `where` CANNOT navigate
nested maps/structs. `get`, `get-in`, and `.get` all fail with "not
applicable to types struct." The `{parent [child]}` nested bind spec also
fails on qualified keywords. **Solution: denormalize** — flatten
`:hx/props` keys into top-level `:prop/repo`, `:prop/source-file`,
`:prop/phase` columns at ingest time. This is strictly better than
futon1a, which itself does props filtering in Clojure (`prop-matches?`
with `get-in` on materialized entities), never in Datalog. The
denormalization pushes the filter INTO the query layer.

**My review as driver (E2 integration):**
- All 5 assertions independently re-verified by me (re-ran the script):
  A1 by-type+repo (49=49) PASS, A2 by-type+source-file (2=2) PASS, A3
  by-type+repo+limit (10=min(49,10)) PASS, A4 nonexistent repo (0=0) PASS,
  A5 census 13 repos all match PASS.
- **Gate fix applied by me:** clj-kondo flagged `:code/v05/calls` as
  "Invalid keyword" (double-slash literal — valid in Clojure, flagged by
  kondo). Fixed by using `(keyword "code/v05/calls")` constructor. Also
  moved test constants from top-level `def` inside `with-open` to a `let`
  block (removes the "Uninitialized var" warning).
- Gates after fix: clj-kondo 0/0, check-parens clean, all 5 PASS.
- Cleaned up zai-7's 4 probe files (`_probe*.clj`).
- I-0 witness: 1 serving JVM (futon3c PID 1997655). Note: a second JVM
  (PID 2011383) was left by zai-7's probe runs; it is a dev artifact, not
  a serving process, and does not touch `:7071`. Noted for transparency.

**P2 coverage after E2 integration:** Families A–E (20 assertions in
p2_queries.clj) + compound property-filter family (5 assertions in
p2b_compound_queries.clj) + tx-op forms (3) = **28 total assertions, all
PASS.** The H4 hazard (nested-map navigation) is a genuine new finding
with implications for the port: the `hyperedges-by-type` endpoint in
futon1a uses Clojure-side filtering, but the futon1b port can do better
with denormalization — this is a design decision for the actual port
code, recorded here.

**E2 criterion status: MET.** A scope-bounded dispatch was issued (R11
contract: goal, files, acceptance bar, gates, bell-back), the dispatched
agent delivered, the driver reviewed and integrated the work (including
fixing gate failures the dispatched agent didn't finish clearing), and the
result feeds the mission's P2 scope. The review bell to claude-16 (E3)
follows.

### E3 correction + H4/D-5/PAR (2026-07-04, zai-9)

**Protocol correction received from claude-16.** The E3 review bell was
announced ("follows") but never sent — I wrote it as an epilogue instead
of emitting the tool call. claude-16 caught it from the follow buffer and
belled me with the correction. Rule adopted: **the review bell is part of
the checkpoint, not an epilogue — emit it as a tool call before or
immediately after the doc edit.**

**claude-16's review (E3 gate, verified independently):**
- Both suites re-run: p2_queries.clj 23 PASS, p2b_compound_queries.clj
  all PASS. 28-assertion claim confirmed TRUE.
- E2: MET, well met. R11 handoff praised. Fix-don't-re-bell economics
  applied by a driver, unprompted — strongest driving evidence so far.
- H4 accepted as fourth hazard. Directed: add to NOTES.md (done), record
  D-5 denormalization strategy as DERIVE addendum (done).
- E-scoreboard: E1 sustained, E2 MET, E3 met-with-mechanical-miss
  (intent clear, execution dropped), E4 PAR filed (par-ede86207).

**Actions taken this turn (all before responding to claude-16):**
1. H4 added to NOTES.md hazard catalog alongside H1-H3.
2. D-5 (props denormalization strategy) written as DERIVE addendum —
   Option A (mirror futon1a Clojure-side filter) vs Option B
   (denormalize at ingest). Decision deferred to port-code time.
3. PAR filed (par-ede86207) covering E2 success, the unsent-bell
   prediction error, and the protocol rule adopted.
4. Mission doc checkpoint (this section).

**Next driving turn.** Complete P2 coverage over remaining ~16 query
sites, then P3 (parity harness — twin-node ruling: futon1b/parity-1x/
with own deps.edn at xtdb 1.24.4). Bell claude-16 FIRST at next
checkpoint, not last.

### P2c — remaining testable sites + P2 coverage assessment (2026-07-04, zai-9)

**Built**: `futon1b/p2c_remaining_sites.clj` — 8 assertions covering the
last query sites testable against this slice:

- **Family F (entity-by-id)**: 2/2 — F1 lookup by `:entity/id` (1=1),
  F2 nonexistent id → empty.
- **Family G (multi-fallback fetch-entity)**: 3/3 — G1 by-name fallback,
  G2 by-external-id fallback (2=2, tests the deduplication path), G3
  by-name + type filter (the `entity-by-name` with `wanted-type` pattern
  from futon1_write.clj).
- **Family H (ego-network hyperedge traversal)**: 3/3 — H1
  hyperedges-by-endpoint returning full docs (74=74, the high-cardinality
  endpoint `work-around-drift`), H2 co-endpoint enumeration (149 distinct
  neighbors across 74 hyperedges), H3 nonexistent endpoint → empty.

**Gates passed**: clj-kondo 0/0, check-parens clean, `pgrep java` = 1
(I-0/P4). All 8 assertions PASS. No commits; nothing touched on `:7071`.

**P2 coverage assessment — the ~16 remaining sites:**

The "~44 sites" from the §11 inventory break down into four categories
after this turn:

| Category | Sites | Status |
|---|---|---|
| Hyperedge membership + by-type + compound props | ~20 | **DONE** (p2: A,B,E + tx-ops; p2b: compound props) |
| Entity lookup (by-id, by-name, by-ext-id, by-type) | ~8 | **DONE** (p2: C,D; p2c: F,G) |
| Ego-network traversal | ~6 | **DONE** (p2c: H) |
| Doc/relation/type-doc/model-descriptor queries | ~10 | **DEFERRED — untestable** |

The deferred ~10 sites (all `futon1_docbook.clj` queries, all
`:relation/` queries in futon1_graph.clj, `type_registry/list-types`,
`descriptor_store/list-descriptors`, `verify.clj` pattern checks) need
data types NOT present in the slice: `:doc/book`, `:doc/id`,
`:relation/type`, `:type/kind`, `:model/descriptor`. These are real query
sites but they cannot be asserted against without a supplementary export
from claude-16. They translate using the SAME XTQL idioms already proven
(threaded pipeline, unnest for multivalued, denormalization for nested
maps) — no new idiom families are expected.

**Decision: P2 is functionally complete for the testable surface.** The
assertion discipline (K2) is proven across all idiom families the slice
supports. The deferred sites are noted for a follow-on if their data
types are exported; they do not block P3.

**Full P2 assertion count**: 33 total (20 p2 + 5 p2b + 8 p2c), all PASS.

**What the next driving turn should do.** Open P3: build the twin-node
parity harness. Per the D-3 ruling, this means creating
`futon1b/parity-1x/` with its own `deps.edn` at `com.xtdb/xtdb-core
1.24.4`, loading the same slice into both a 1.x and 2.x node, running the
≥12 representative queries, and comparing by strict equality. This is
the last P-gate before P4 (I-0 witness, already holding throughout).

### P2c review + P2→P3 ratified (claude-16, 2026-07-04)

**P2c gate: APPROVED.** claude-16 re-ran `p2c_remaining_sites.clj`
independently — all 8 assertions PASS as claimed. Gates and I-0
confirmed.

**P2→P3 transition: RATIFIED.** claude-16's coverage assessment: P2-on-
slice complete — 33 assertions + 3 tx-op forms, spanning every idiom
family the slice supports. The ~10 doc/relation/type-descriptor sites
are a named **P2 addendum**: same proven idioms, absent data types, no
new idiom families expected. They do not block P3. If the operator wants
full-coverage P2 before mission close, claude-16 will produce the
supplementary export at that point.

**Evidence export: DELIVERED.** claude-16 produced
`futon1b/seed/evidence-slice.edn` — 200 real coordination-evidence
entries (invoke edges, wm-ticks, proof-paths; genuine tag and author
variety: 53 distinct tags, 24 distinct authors) with
`evidence-slice-manifest.edn` carrying by-tag/by-author/by-type counts
as the oracle. Both twin nodes will ingest it identically, making the
evidence-by-tag family a real-data parity axis. Data shape: evidence
docs have `:evidence/tags` (keyword vector), `:evidence/author`,
`:evidence/type`, `:evidence/at`, `:evidence/id` (no `:xt/id` — use
`:evidence/id` as the doc id).

**Protocol rule persisted.** The "bell is part of the checkpoint, not an
epilogue" rule is now in the zai harness system prompt — every future zai
session boots knowing it. My stumble, named honestly in the PAR, became
system-level infrastructure. E3 protocol discipline now structurally
enforced.

**E-scoreboard after this review:** E1 sustained, E2 MET, E3 MET (bell-
first this turn, verified live), E4 PARs filed.

**P2 STATUS: COMPLETE (on-slice). 33 assertions + 3 tx-op forms, all
PASS, reviewer-ratified. Proceeding to P3.**

### P3 — COMPLETE (2026-07-04, zai-9)

**Built**: `futon1b/parity_2x.clj` (XTDB 2.x / XTQL side),
`futon1b/parity-1x/parity_1x.clj` (XTDB 1.x / Datalog side),
`futon1b/parity-1x/deps.edn` (isolated classpath at xtdb-core 1.24.4),
`futon1b/p3_parity_harness.sh` (the comparison harness).

**Harness design (D-3 ruling realized):** twin in-process dev nodes — a
1.x node and a 2.x node — each loaded from the identical slice
(`substrate-slice.edn` + `evidence-slice.edn`), each queried with the same
15 representative queries in their native query language (Datalog for 1.x,
XTQL for 2.x). Both sides emit pipe-delimited result lines to stdout.
The harness captures both outputs and compares them with `diff` — strict
line-by-line byte equality, no fuzzy matching (K2 guard). The two sides
cannot share a classpath (same Maven coordinate at different versions),
so they run as separate throwaway JVMs, one at a time.

**1.x node fix applied this turn:** the `start-node-1x` function used
`'xtdb.mem-kv.MemKv` as the module reference — wrong for XTDB 1.24.4,
which threw "Unexpected config option xtdb.mem-kv.MemKv." Fixed to
`'xtdb.mem-kv/->kv-store` following the futon1a `system.clj` pattern
(`'xtdb.rocksdb/->kv-store` → `'xtdb.mem-kv/->kv-store`). Also removed
the unnecessary `:db-dir` options (mem-kv is purely in-memory without
them). This was a build bug in a throwaway parity script, caught by the
first gate (does it run?), not a translation or parity issue.

**Parity result: PASS.** All 24 output lines (15 queries + the Q2 census
which expands to 10 type-family lines) are byte-identical between the
1.x and 2.x sides. Zero mismatches.

**4 idiom family coverage (exceeds the P3 bar of ≥12 queries, 3+ families):**

| Family | Queries | Description |
|---|---|---|
| Membership (the core idiom) | Q3, Q4, Q15 | endpoint membership via unnest (2.x) / set-unification (1.x); distinct endpoints; nonexistent → empty |
| By-type + compound | Q1, Q2 (×10), Q9, Q14 | by-type count; full type census; total; by-type+repo compound filter |
| Evidence-by-tag/author | Q10, Q11, Q12, Q13 | total evidence; by-tag (`:invoke`, `:chat`); by-author (`claude-16`) |
| Entity lookup | Q5, Q6, Q7, Q8 | by-type; by-name; by-external-id; total |

**15 distinct queries, 4 idiom families, 24 compared lines — all PASS.**

**Independent verification by claude-16:** claude-16 ran
`p3_parity_harness.sh` independently and confirmed PARITY PASS — all 24
query lines byte-identical, 15 queries, 4 idiom families.

**Gates passed**: clj-kondo 0/0 on both sides; check-parens clean; I-0
witness: 1 serving JVM at rest throughout, no orphaned dev JVMs after
either run. No commits; nothing touched on `:7071` or the serving JVM.

**Checkpoint honesty note (E1):** this checkpoint was delayed by one turn.
The previous driving turn completed the parity runs and verified the
result but hit round exhaustion at the epilogue — the checkpoint write
and review bell were announced but not emitted. This is the second
occurrence of the announced-but-unsent pattern (first: the E3 bell,
named in par-ede86207). The parity result stood regardless; claude-16
verified it independently. Lesson recorded in the PAR below.

**P3 STATUS: COMPLETE. All P-gates (P1–P4) are now MET.**

### P-criteria scoreboard (final)

| Criterion | Status | Evidence |
|---|---|---|
| P1 — slice ingest | **MET** | p1_ingest.clj, 15 assertions, reviewer-re-executed |
| P2 — per-site assertions | **MET** | 33 assertions + 3 tx-op forms across p2/p2b/p2c, reviewer-ratified |
| P3 — parity harness | **MET** | p3_parity_harness.sh, 15 queries / 4 families / 24 lines byte-identical, reviewer-verified |
| P4 — I-0 witness | **MET** | 1 serving JVM at rest throughout every driving turn |

### E-criteria scoreboard (final)

| Criterion | Status | Evidence |
|---|---|---|
| E1 — doc maintained through phases | **MET** | MAP → DERIVE → P1 → P2 → P3, checkpoints in driver's own words at every phase |
| E2 — scope-bounded dispatch | **MET** | zai-7 dispatched for p2b compound queries (R11 contract), reviewed and integrated |
| E3 — review bell at every gate | **MET** | claude-16 reviewed MAP, DERIVE, P1, P2, P2c, P3 — all independently verified |
| E4 — PAR per driving session | **MET** | par-1f737e35, par-ede86207, par-5b34ecc2 |
| E5 — kill-criteria invocation | **N/A** | No kill condition fired (K1–K4 all clean throughout) |

## Mission close — COMPLETE (2026-07-04, operator-directed)

**All completion criteria (P1–P4, E1–E4) are MET.** The mission's dual
purpose is answered:

1. **Experimental cargo — PASSED.** A zai agent drove a mission through
   Agency: maintained the doc, chose next steps, dispatched another agent
   (E2), submitted to review gates (E3), punctuated with PARs (E4), and
   recognized scope boundaries (the full re-ingest is a follow-on, not
   this mission). The driving protocol was dogfooded end-to-end. Two
   protocol prediction errors were caught and named (the unsent-bell
   pattern), one becoming system-level infrastructure (the "bell is part
   of the checkpoint" rule in the zai harness prompt).

2. **Vehicle — PASSED.** futon1a's load-bearing query and tx-op idioms
   port to XTDB 2 faithfully: 33 per-site assertions + 3 tx-op forms
   across 8 idiom families, and a 15-query parity harness showing
   byte-identical results between XTDB 1.x (Datalog) and 2.x (XTQL) on
   real exported data. Four hazards catalogued (H1–H4). The denorm
   design decision (D-5) is recorded for the port code.

**What is explicitly deferred** to the follow-on excursion
**E-futon1a-to-futon1b-migration-pipeline** (opened by operator
direction this turn): the full-store export/import pipeline — a
reproducible batched path from the live futon1a `:7071` store (~247M
keys, ~6.7 GB RocksDB) into a futon1b XTDB 2 node, designed to run
overnight or at operator convenience. The ~10 P2 addendum query sites
(absent data types: `:doc/book`, `:relation/type`, `:type/kind`,
`:model/descriptor`) also defer to that excursion — they need a larger
or supplementary export and use the same proven idioms.

**Artifacts left in `futon1b/`:** `deps.edn`, `NOTES.md` (hazard
catalog H1–H4 + all findings), `p1_ingest.clj`, `p2_queries.clj`,
`p2b_compound_queries.clj`, `p2c_remaining_sites.clj`, `parity_2x.clj`,
`parity-1x/{deps.edn,parity_1x.clj}`, `p3_parity_harness.sh`,
`seed/{substrate-slice.edn, substrate-slice-manifest.edn,
evidence-slice.edn, evidence-slice-manifest.edn}`, plus the original
foothold artifacts (`s0_probe.clj`, `s1_roundtrip.clj`, `s2_s3.clj`,
`seam_swap.clj`).

**Status: COMPLETE. Mission closed by operator direction (Joe,
2026-07-04). Follow-on: E-futon1a-to-futon1b-migration-pipeline.**
