# M-custom-harness — EXPLORE: futonic agent harnesses and memory substrates

*Question:* if a model endpoint such as Z.AI provides no native filesystem access,
no built-in memories, and no automatic project bootstrap, what should a Futon-native
agent harness provide instead?

This is not a mission to clone Codex or Claude Code. The interesting opportunity is
to design a harness whose agency comes from Futon: evidence, missions, social state,
clocking, graph memory, and local tools. Files are one substrate, but not the only one.

**Status (2026-07-04, end of day): VERIFY COMPLETE (§14) — the §8.4
three-condition comparison supports the §9 hypothesis: graph bootstrap was
3–4.5× faster with the best citations and calibration, while files proved
a viable slower fallback. DERIVE (D-1…D-11, ratified), ARGUE, slices 1–3
(§13.5a–d), the §13.5c product fixes (2 of 3), and the E-futon1b-foothold
excursion are all done and live-verified. Remaining: DOCUMENT. Parked:
subject-ref linking, B2 flight-native follow-on, A1/A2 triggers.** The
`zai-repl.el` harness works end-to-end — the Emacs client calls tools through
the Agency invoke-stream — and the MAP record (§6–§11) surveyed the territory
against verified source. §12 turns §7's leanings into decisions D-1…D-10
(operator-ratified 2026-07-04, with the staging framing recorded at the top
of §13), specifying the v1 memory-and-orientation tool layer concretely
enough to implement and deferring the A2/A3/B2 options with named re-entry
triggers. §13 is the ARGUE synthesis: pattern cross-reference (which fed one
revision, R-1, back into §12.3), coherence, trade-offs, and the
plain-language argument. Next: VERIFY (the §8.4 three-condition comparison
is the designated spike), then slice-1 wiring.

*(§1–§5 are the original EXPLORE framing, kept intact; §6 onward supersedes where
they differ — notably §1–§2's picture of a bare harness.)*

---

## 1. Initial Observation

The Z.AI API is fast and capable as a model endpoint, but the bare API has no local
situation:

- it does not read `AGENTS.md`;
- it does not know the current repo conventions;
- it has no persistent memory unless the harness supplies one;
- it cannot inspect files, run commands, talk on IRC, or query the JVM without tools;
- it has no built-in notion of missions, clock targets, Agency identity, or evidence.

The current `zai` harness begins to close the execution gap by adding tool calls for
filesystem, shell, JVM reflection, and IRC. That makes it agentic in the local coding
sense. The next question is deeper: what kind of *memory and orientation* should a
futonic harness have?

## 2. Design Opportunity

A custom harness can decide what the agent wakes up knowing, what it may ask for, and
where durable context lives.

Possible boot context:

- Agency identity: agent id, session id, cwd, clock target, surface.
- Repo contract: `AGENTS.md`, local README fragments, active branch, dirty files.
- Mission state: current C-/M-/E- target, nearby mission notes, outstanding tickets.
- Social state: recent IRC, recent bells/whistles, active agents, current commitments.
- Memory state: relevant evidence records, prior decisions, PSRs/PURs/PARs, tool traces.

The boot packet should probably be small and opinionated. The rest should be available
through tools, so the model can ask for context rather than being flooded by it.

## 3. Memory As Graph, Not Just Files

The exciting alternative to "read a bunch of markdown" is to treat XTDB/evidence as
the agent's memory substrate.

Candidate memory tools:

- `memory_search`: semantic or structured search over evidence records.
- `mission_context`: summarize evidence and notes for a C-/M-/E- target.
- `evidence_graph`: traverse claims, replies, corrections, proof paths, and decisions.
- `recent_coordination`: recover recent inter-agent turns and commitments.
- `pattern_memory`: retrieve PSRs/PURs/PARs relevant to the current task.
- `tool_history`: show what this agent or nearby agents actually did recently.

This would make memory durable, inspectable, and shared across agents. It also avoids
the bad version of "memory" where every agent maintains a private text blob that cannot
be audited or joined with the rest of the system.

## 4. Questions To Keep Open

- What belongs in the always-on boot packet versus an explicit tool?
- Should `AGENTS.md` be injected verbatim, summarized, or exposed as a tool result?
- What is the right unit of memory retrieval: evidence entry, turn, mission, proof path,
  pattern, or graph neighborhood?
- How should the harness prevent stale memories from sounding authoritative?
- Can the agent cite evidence ids or mission files when it uses memory?
- Should memories be ranked by recency, graph proximity, mission clock, semantic match,
  or prior usefulness?
- What should be private to one agent session versus shared across Agency?
- How do we distinguish operator preferences from one-off historical accidents?

## 5. First Experiments

1. Add a `boot_context` tool or prelude for Z.AI that reports agent id, cwd, session id,
   clock target, repo root, and presence of `AGENTS.md`.
2. Add a read-only `read_agents_md` or `repo_contract` tool and observe whether the model
   asks for it naturally.
3. Add an evidence-backed `memory_search` tool with strict limits and returned evidence ids.
4. Add `mission_context` for the active clock target, using evidence plus mission markdown.
5. Compare three conditions on the same task:
   - no memory, tools only;
   - file bootstrap via `AGENTS.md` and mission notes;
   - graph bootstrap via XTDB/evidence retrieval.

The evaluation should look for groundedness, reduced repeated explanation, better mission
continuity, and fewer false claims about what the agent has inspected or done.

## 6. Substrate survey (2026-07-04)

What exists today, verified against source. The harness has two halves:

- **Client:** `futon3c/emacs/zai-repl.el` — Emacs UI; direct stateless API mode
  plus an Agency mode that streams `/api/alpha/invoke-stream` events (text,
  tool_use, tool_result, done).
- **Server loop:** `futon3c/src/futon3c/agents/zai_api.clj` — OpenAI-style tool
  loop, max 8 tool rounds per turn, 14 tools (read/list/search/edit/write,
  run_shell/run_readonly, 5× JVM reflection, irc_recent/irc_send), delegating to
  `futon3c.peripheral.real-backend`. Conversation history is an in-memory atom
  inside the invoke-fn closure; only the session id is persisted. System prompt
  is four sentences.

Backing surfaces for the candidate memory tools:

| Candidate tool | Existing surface | Readiness |
|---|---|---|
| `boot_context` | `agency/registry.clj` (id, session, peripheral, hop-stack — **no cwd**; cwd lives in backend config) + `agency/clock_store.clj` (`current-clock`, `record-tool-use!`) + git | needs a thin tool |
| `repo_contract` | `AGENTS.md` files are 1.2–5 KB (measured: workspace 3.4K, futon2 1.3K, futon3c 5.1K) | trivial |
| `memory_search` | `evidence/store.clj query` — structured filters only (`:query/subject :type :claim-type :author :since :tags :limit`); HTTP `/api/alpha/evidence`. **No free-text search exists anywhere in the stack**; futon3a embeddings (`notions/search-embeddings`) are the only semantic option and need the python venv server | structured: wire a wrapper; semantic: phase 2 |
| `mission_context` | `enrichment/query.clj enrich-file` (composes missions+patterns+vars+coverage from :7071) + `peripheral/mission_backend.clj` (`tool-obligation-query`, `tool-mission-spec-get`) + mission markdown | works, needs tool wrapper |
| `evidence_graph` | `evidence/threads.clj` (`project-thread`, `thread-forks`, `thread-conjectures`) + `store/get-reply-chain`, `get-forks`; futon1a `hyperedges-by-end` for graph-neighborhood traversal (always pass `&limit`) | works in-JVM, needs tool wrapper |
| `recent_coordination` | invoke-jobs ledger (`/api/alpha/invoke/jobs`, public view incl. caller, surface, state, events, bellback-of) + `/coordination/edges`, `/coordination/threads` (open bells, who-owes-whom, crossings) | **live HTTP today** |
| `pattern_memory` | write side done: `real_backend` has `:psr-search :psr-select :pur-update :par-punctuate` tools and proof-paths land as tagged evidence. **None of these are exposed in zai_api's tool-specs.** No dedicated PSR/PUR/PAR *read* API — retrieval = evidence query by tag | expose + wire a read fn |
| `tool_history` | `clock_store/record-tool-use!`/`record-edit!` + invoke-job `:events`; the backend itself keeps no trace | needs a read tool |

Adding a tool costs: one `tool-*` fn + one `case` clause in `real_backend.clj`
(if new), plus one `tool-specs` entry + one `execute-tool` case in `zai_api.clj`.
The cheapest first win is exposure, not construction: the PSR/PUR/PAR discipline
tools already exist behind the backend and are simply not offered to the model.

Two real gaps, named so they don't masquerade as small: (1) no free-text or
semantic search over evidence/hyperedges — all retrieval is by exact
type/tag/author/id/endpoint; (2) no dedicated pattern-memory read API.

**What was checked (provenance).** `zai-repl.el` and `zai_api.clj` read in full;
an Explore sweep over futon1a routes, the Agency registry/ledger/clock-store,
evidence store/threads, futon3b gate shapes, and futon3a notions — every
readiness cell above cites a specific namespace/fn. Re-verified against source
2026-07-04 (reviewer pass): `real_backend.clj` defines
`:psr-search :psr-select :pur-update :par-punctuate` (≈ lines 642–646) and none
of them appear among `zai_api.clj`'s 14 tool-specs (the loop bound is
`loop [remaining 8`), confirming the exposure gap; AGENTS.md sizes measured at
3.4K / 1.3K / 5.1K.

## 7. Design considerations (2026-07-04)

Considerations against §4, taking the survey as ground — **leanings, not
decisions.** MAP produces facts, not commitments; the items below are candidate
positions for a later DERIVE to weigh and the operator to ratify, phrased as
"lean toward X because…" rather than "X is chosen."

**Boot packet vs tool.** Lean: the boot packet holds what the agent needs to
*ask well*, not the answers themselves. Concretely: agent id, session id, cwd,
surface, clock target (id + one-line description), branch + dirty files, and the
repo's `AGENTS.md` verbatim. Everything in §3 stays behind tools. Estimated
packet: 2–7 KB, dominated by AGENTS.md.

**AGENTS.md: lean verbatim.** They are 1–5 KB — summarizing saves nothing and
introduces drift; tool-only exposure risks the model never asking (experiment 2
below would test that empirically; absent that evidence the leaning is inject).

**Unit of retrieval — lean: evidence entry as atom, thread as molecule.** `memory_search`
returns entries; `evidence_graph` returns `project-thread` projections and
reply-chains. Every result carries its evidence id — ids are the citation
currency.

**Staleness.** Every memory result is stamped with its timestamp and author, and
the tool result frame says "recorded, not necessarily current." The system
prompt extends the existing honesty clause: memory results are *quotes from the
record*, to be cited by id, not restated as present-tense fact. Live state
questions (what is running, what is dirty) should go to live tools, not memory.

**Citation.** Lean yes, and it's mechanically cheap: tools return ids; the prompt requires
"when you rely on a memory result, cite its evidence id or mission file." This
is the same move as the existing "do not claim to have inspected anything
unless you used tools" line — provenance discipline, extended from actions to
memories.

**Ranking.** Lean structured-first: clock-target match > graph proximity
(endpoint traversal from the mission/thread node) > recency. Semantic ranking
only when the futon3a embedding server is up, and marked as such in results.
"Prior usefulness" ranking waits until tool_history accumulates usage records —
don't design the prior before the data exists.

**Private vs shared: lean toward nothing private.** All durable memory writes
would go through the evidence store — auditable, joinable, shared across Agency —
leaving only the conversation history session-private. This would make the §6
hypothesis into policy (the agent remembers through the system, not a private
blob), which is exactly the kind of commitment DERIVE should ratify, not MAP. (Corollary worth fixing: zai_api's history atom dies with the
JVM closure; durable turn memory already exists in the invoke-jobs ledger, so a
session could rehydrate from there rather than growing a private persistence
mechanism.)

**Operator preference vs historical accident.** Preferences are *recurrent* and
should live as patterns (PSR/PUR-backed) or explicitly tagged
`:operator-preference` evidence; one-off decisions stay ordinary evidence
entries. The distinguishing signal is recurrence plus an explicit claim-type —
never infer a standing preference from a single entry.

## 8. Candidate experiment slices

Options for a later DERIVE/INSTANTIATE, not a committed sequence — ordered by
leverage; each independently landable.

1. **Expose what exists** — add `boot_context` (registry + clock_store + git) and
   `repo_contract` to zai_api tool-specs; expose the already-built
   `:psr-search :psr-select :pur-update :par-punctuate` backend tools to the
   model. Pure wiring, no new backend code except boot_context.
2. **Graph memory, read side** — `memory_search` (wrapper over
   `evidence/store query`, strict `:limit`, ids in every result),
   `evidence_graph` (`project-thread` + reply-chain), `recent_coordination`
   (invoke-jobs + coordination/threads). Add a `pattern_memory` read fn =
   evidence query filtered to PSR/PUR/PAR/proof-path tags.
3. **`mission_context`** — clock target → mission markdown + `enrich-file` +
   obligation query, summarized with ids.
4. **The three-condition comparison** (§5.5) — same task under (a) tools only,
   (b) file bootstrap, (c) graph bootstrap. Score groundedness (claims backed by
   ids), repeated-explanation count, and false claims of inspection. Run only
   after slices 1–2 so condition (c) is real.
5. **Semantic search** — futon3a embeddings behind `memory_search` when the
   embed server is up; degrade to structured search otherwise. Last because it
   has an infrastructure dependency the others don't.

## 9. Working Hypothesis

A futonic harness should not merely give a model local tools. It should give the model
a situated way to ask, "what is already known here, what is currently live, and what
evidence supports that?" XTDB/evidence can become the shared memory layer, while files
remain the executable artifact layer.

The aim is an agent that remembers through the system rather than through private lore.

## 10. Status & candidate next step (2026-07-04)

MAP consolidating; DERIVE not yet entered. §4's design questions are *explored*
in §7 as considerations, and §8 lays out candidate experiment slices. The
harness is further along than §1–§2 imply — `zai_api.clj` already delegates to
`real-backend`, whose PSR/PUR/PAR discipline tools are *built* but simply not
offered to the model.

**Candidate next step (offered, not committed): slice 1 (§8), "expose what
exists".** Nearly mechanical: one `boot_context` backend fn plus tool-spec
entries, and the existing PSR/PUR/PAR tools added to `zai_api.clj`'s
`tool-specs` + `execute-tool` case. No new retrieval infrastructure. It would be
the cheapest first win and unblocks slice 4's graph condition (c). Small enough
to do in-place rather than bell out (Codex quota out until 2026-07-18 anyway) —
ready to wire on Joe's word, once DERIVE opens.

*Reconstruction note:* §6–§9 were authored by Fable on 2026-07-04; the session
hit its usage limit on the follow-up "make the doc up to date" step, so this
status section, the §6 provenance paragraph, the status banner, and the
14-tool count fix (Fable's draft said 15) were reconstructed from the session
transcript and re-verified against source by the Claude owner.

## 11. Options map — query substrate & flight unit (2026-07-04, MAP)

Staying in MAP mode: options as territory, facts not decisions. Asking "what
does *futon-native* mean beyond giving the model tools" surfaced two design
axes. Both are laid out as ready-vs-missing; neither is chosen here.

### Axis A — how the harness queries memory

Verified query-substrate status (details in §6 and `M-typed-holes-MAP.md` §6):
the whole stack is **XTDB 1.24.x / Datalog**; XTQL is **XTDB 2.x**, absent, no
migration note. Three options:

- **A1 — finish the 1.x query layer.** Add recursive Datalog `:rules` (1.x
  supports them; none exist in `futon1a/src` today) and expose the
  prefix-sweep + `db-as-of` reads over the `:7071` HTTP API (they work already,
  Drawbridge-only). *Brings:* server-side multi-hop / `callers-of` /
  transitive-deps / code-structure time-travel without the `enrich-file` N+1
  HTTP fan-out. *Cost:* query-layer work only — no engine change, no re-ingest,
  does not touch the one serving JVM (I-0). *Does not close:* no SQL surface;
  code-*contents* time-travel still blocked upstream by the HEAD-mirror data
  model (`E-substrate-2-timetravel`).
- **A2 — migrate to XTQL / XTDB 2.x.** A 1.x→2.x engine change (columnar/Arrow).
  *Brings uniquely:* **SQL as a model-writable query surface** (off-the-shelf
  LLMs write SQL, not Datalog); a **composable query algebra** matching the
  `queryComb` / queries-as-scopes reading (a query as a composed probe);
  bitemporality as a first-class language clause. *Cost:* engine migration,
  re-ingest of ~5,400 commits of substrate-2, touches the load-bearing `:7071`
  store (I-0). Unnecessary for a fixed-tool-menu harness.
- **A3 — query-IR at the store-agnostic layer** (the D2 gap in
  `M-typed-holes-MAP.md`). A small intent representation the harness builds
  (e.g. from a partial hyperedge) that compiles to 1.x Datalog now and *could*
  target XTQL/SQL later. *Brings:* keeps A1 and A2 both open; is the seam where
  "adopt XTQL" would slot in; aligns with query-as-composed-probe. *Cost:*
  designing the IR; risk of abstracting before the real query shapes are known.

**Cross-axis fact.** A2's SQL value scales with exactly one question: *does the
model author queries, or pick from typed tools?* Fixed menu → 1.x is fine.
Model-as-query-author (the "solving is querying" daydream) → a language the
model already knows starts to earn its keep, and 1.x has no SQL.

**A2 instantiation — sibling repo (`futon1b`), non-destructive.** Bud a
`futon1b` on XTDB 2.x, port futon1a's model to it, leave the live `:7071`
futon1a untouched — the stack's bud-a-sibling pattern (futon3b reads futon1a
read-only; futon7b built clean). *De-risks the operational half:* I-0 safe,
reversible, A/B-comparable. *Two facts on cost:* (i) the futon1→futon1a
precedent does **not** predict it — that port was XTDB-1.x → XTDB-1.x (both on
`xtdb-core 1.24.x`, via `scripts/migrate_futon1.clj`), same Datalog dialect and
tx-ops, so query/tx code copied; **1.x→2.x drops Datalog** (2.x is SQL/XTQL), so
the surface is a *rewrite*: ~44 `xtdb/q` sites, ~45 `:xtdb.api/put|delete|evict`
tx-ops, and 34 multivalued-endpoint unification idioms (`[e :hx/endpoints ep]`)
across ~24 namespaces (much of it one-off scripts; the load-bearing core is
`routes.clj` + `compat/{futon1_graph,futon1_write}` + `core/xtdb` + `model/*`).
(ii) Two offsets: the rewrite is the natural place to build the A3 query-IR for
free, and 2.x's first-class bitemporal SQL would clean up `commit_ingest`'s
valid-time stamping. *First spike to settle the question:* translate one
`:hx/endpoints` membership query to XTQL — the most Datalog-idiomatic pattern in
the store — and confirm it reads cleanly. *Not de-risked:* the code-graph
HEAD-mirror defect (`E-substrate-2-timetravel`) is a data-model choice, not an
engine limitation, so 2.x does not fix it for free.

### Axis B — the harness's native unit: turn vs flight

- **B1 — turn-native (status quo).** The current harness: OpenAI-style loop,
  ≤8 tool-rounds/turn, in-memory history atom that dies with the JVM (§6). Unit
  = request→response. *Brings:* simple; matches every off-the-shelf harness.
  *Missing (futon-native lens):* no prediction / measurement / learning
  structure; durable memory only as a side effect; forces **extractive
  re-mining** (a 70B over every turn to recover the sorries/memes/C-entries the
  agent minted implicitly) — capture-after-the-fact.
- **B2 — flight-native.** Unit = a *flight* (`flight.spec.edn:12`: "a flight is
  the trace of ONE step" of the WM operator; each organ slot is a term carrying
  its ground or a typed sorry carrying its reason — field-read → velocity →
  prediction → gated act+witness → measurement → self-record). A flight can
  contain N turns. *Brings:* **R15** (temporal depth — the harness predicts,
  acts under the WM-I4 consent gate, measures, learns) and **R11** (a flight can
  span agents; the mesh is boot context + participant, over the already-live
  coordination surface — `recent_coordination` / invoke-jobs /
  coordination-threads); structure **declared mid-flow** (the "line of flight")
  instead of re-mined; flight records are hyperedges → the flight log *becomes*
  the queryable memory. *Cost:* the durable unit becomes a persisted flight
  record on `:7071` (not an in-memory atom); the loop grows prediction +
  measurement slots; the consent gate applies to the act step. Architectural,
  not slice-1 wiring.

**B2 corollary — the flight record as observability (2026-07-04).** If a flight
captures its *artifacts* as endpoints — files written, figures generated
(`cascade-N-<mission>.png`), notebook entries logged to
`p4ng/sequel-notebook.org`, bells dispatched, a transcript pointer — then
"*which session did X happen in?*" becomes a **substrate-2 query**, not a
tmux/transcript archaeology dig. Live proof-of-need: locating one remembered
"cascade figures + notebook logging" thread this session took a scored scan
across ~12 session transcripts and resolved to **three** different sessions
(cascade figures `34ac5690`, today, via the dataviz skill; flight-log→notebook
`3af7ac9c`, 07-02; notebook + AIF-loop figures `e7a6a78e`, today) — exactly the
scatter that per-flight logging into substrate-2 collapses to one query. This is
the same move as §7's "remembers through the system, not private lore," applied
to the *work products* of a flight, not just its claims.

### Where the axes couple (the finding)

The three threads are one design axis seen from three sides:

- A flight record is a hyperedge (or a few) → **B2 makes the flight log the
  store the A-axis queries.**
- Queries-as-scopes: a new flight *is* a partial hyperedge with a goal-hole, and
  **boot context = filling it against the flight log** → A3 (query-IR as a
  composed probe) and B2 (flight unit) are the same mechanism from two ends.
- Model-as-query-author (A2's SQL trigger) and flight-native (B2) reinforce: if
  the agent declares goals and composes its own retrieval mid-flight, both pull
  toward a model-friendly query surface.

### Ready vs missing (this slice)

| READY (no new code) | MISSING (the work each option implies) |
|---|---|
| 1.x Datalog engine + uniform `hx/` schema | recursive `:rules`; HTTP-exposed prefix / `db-as-of` (A1) |
| cheap query grain fast — count-pushdown, by-type, by-end (§6) | SQL / model-writable surface — 2.x only (A2); the query-IR (A3, unbuilt) |
| coordination surface live (invoke-jobs, coordination-threads) | R11 wired into a flight unit (B2) |
| `flight.spec.edn`, WM rollout, WM-I4 consent gate all exist | the flight as the harness's loop unit — today it is a turn loop (B2) |

*MAP exit for this slice:* options are on the table; nothing is decided. The
coupling above is the finding — the query substrate, the flight unit, and the
"other agents" surface are not three problems but three faces of one.

## 12. DERIVE — design (2026-07-04)

Entered on operator handoff (Joe → Fable, 2026-07-04). This section turns
§7's leanings into decisions and §8's candidate slices into a committed v1,
with the §11 axes explicitly settled-or-deferred. Verified against source
same day: `zai_api.clj` tool loop (14 specs, `loop [remaining 8`,
four-sentence system prompt, in-memory `!messages` atom), `real_backend.clj`
discipline-tool dispatch (lines 642–646), `evidence/store.clj`
`query`/`get-reply-chain`/`get-forks`, `evidence/threads.clj`
`project-thread`/`thread-forks`/`thread-conjectures`, `clock_store.clj`
`current-clock`/`record-tool-use!`/`record-edit!`, `flight.spec.edn` header.

**Scope of v1:** a memory-and-orientation layer on the *existing* turn loop —
boot packet, eight read-side memory tools, four already-built discipline
tools exposed, ledger rehydration. No new durable entity types, no new write
paths, no engine work. The three-condition comparison (§8.4) is the
evaluation gate and belongs to VERIFY as the spike.

### 12.1 Decisions

**D-1 — Boot packet: orientation, not answers.**
IF the agent needs situation to ask well, HOWEVER flooding the context
defeats the tools-first design and §2 warned against it, THEN the boot
packet is exactly: agent id, session id, cwd, surface, clock target (id +
one-line description), git branch + dirty-file list, and `AGENTS.md`
verbatim — everything in §3 stays behind tools, BECAUSE the packet's job is
to make the first tool call well-aimed, not to preempt it. Hard cap 8 KB
(INV-4); measured AGENTS.md sizes (1.3–5.1 K) leave room.

**D-2 — AGENTS.md verbatim.**
IF summarizing saved tokens it might earn its keep, HOWEVER the files are
1–5 KB and summaries drift from source, THEN inject verbatim, BECAUSE at
this size the summary costs more (in drift risk) than it saves. Experiment
§5.2 (tool-only exposure) remains a VERIFY-time probe; absent that evidence,
verbatim is the decision, not a leaning.

**D-3 — Fixed typed tool menu; Axis A settles to "no A-axis work in v1."**
IF a model-writable query surface (A2's SQL) is valuable, HOWEVER its value
scales with model-as-query-author and v1 is a fixed menu of typed tools
(§11 cross-axis fact), THEN v1 wraps existing in-JVM fns and requires no
query-layer work at all — not even A1, BECAUSE `memory_search` /
`evidence_graph` / `recent_coordination` all have live backing surfaces
already (§6). *Named triggers for re-entry:* A1 opens when `mission_context`'s
`enrich-file` fan-out is measurably the bottleneck or a multi-hop/`callers-of`
tool is wanted; A2's first move stays the §11 spike (translate one
`:hx/endpoints` membership query to XTQL in a `futon1b` bud); A3 is *not*
built in v1, BECAUSE abstracting the query-IR before real query shapes
accumulate is the risk §11 itself named — v1's tool call-logs are exactly
the corpus that de-risks A3 later.

**D-4 — Unit of retrieval: evidence entry as atom, thread as molecule.**
`memory_search` returns entries; `evidence_graph` returns thread projections,
reply-chains, forks, or a bounded graph neighborhood. Every returned item
carries its evidence id — ids are the citation currency (D-9).

**D-5 — Staleness: envelope + live/recorded split.**
IF memory results read as present-tense facts the agent will repeat them as
such, HOWEVER the record is by construction historical, THEN every
memory-family result is wrapped in a provenance envelope (each item stamped
`:id :at :author`; the frame text "recorded, not necessarily current"), and
live-state questions (what is running, what is dirty, who is registered) are
answered only by live tools, never memory tools, BECAUSE the honesty clause
already in the system prompt extends naturally from actions to memories:
memory results are *quotes from the record*.

**D-6 — Ranking: structured-first.**
Clock-target match > graph proximity (endpoint traversal from the
mission/thread node) > recency. Semantic ranking is slice 5 (deferred): only
when the futon3a embedding server is up, and results so obtained are marked
`:ranked-by :semantic`. "Prior usefulness" ranking waits for `tool_history`
data to exist — do not design the prior before the data.

**D-7 — Nothing private; rehydrate from the ledger.**
IF the agent needs cross-session continuity, HOWEVER a private persistence
mechanism (serializing the `!messages` atom) would create exactly the
unauditable text blob §3 rejects, THEN durable memory writes go only through
the evidence store, the conversation atom stays a session-scoped working
cache, and session resume rehydrates from the invoke-jobs ledger, BECAUSE
durable turn memory *already exists* there (§6) and the agent should
remember through the system. Rehydration spec: on `make-invoke-fn` start
with an existing session id, query the ledger for that session's jobs, take
the last 10 turns, reconstruct user/assistant *text* messages only, and
prefix the reconstruction with one marker message: `[rehydrated from
invoke-jobs ledger: tool detail elided]`. IF full-fidelity replay were
attempted, HOWEVER tool messages must pair with tool-call ids and would
bloat the window, THEN text-only with an elision marker, BECAUSE the point
is continuity of commitments — tool *facts* should be re-fetched live (D-5).

**D-8 — Axis B: turn-native retained; flight-native deferred to a follow-on.**
IF B2 (flight-native) is where the futon-native ambition points — prediction,
consent-gated action, measurement, flight records as queryable hyperedges —
HOWEVER B2 is architectural (new durable unit on :7071, consent gate on the
act step, loop restructure) and the §8.4 comparison should run on the cheap
version first, THEN v1 keeps the turn loop and B2 becomes a named follow-on
(working title `M-flight-native-harness`, to be scoped against
`flight.spec.edn` and WM-I4), BECAUSE POC-plus-follow-on beats scope creep,
and v1's `tool_history` + ledger records are the data B2's flight records
would formalize — nothing built here is thrown away. The §11-B2 corollary
(artifacts as endpoints) transfers to that mission intact.

**D-9 — Citation is required, mechanically supported.**
Tools return ids (D-4/D-5); the system prompt adds: when you rely on a
memory result, cite its evidence id or mission file. Same shape as the
existing "do not claim to have inspected" line.

**D-10 — Operator preference ≠ historical accident.**
Preferences are recurrent and explicitly typed: PSR/PUR-backed patterns or
evidence tagged `:operator-preference`. `pattern_memory` surfaces only
those; a single untyped entry is never presented as a standing preference.

**D-11 — Storage-coupling discipline: the envelope is the portability seam.**
*(Added post-ratification, 2026-07-04, in response to Joe's storage-coupling
question; ratified same day with the note: agent turns are already logged
into the Evidence Store — true for Zai too — which is an existing write
path behind the same store boundary, migrated with the store; it does not
tighten harness coupling or make porting harder.)*
IF the harness must not get deeply integrated into XTDB 1 before a possible
futon1b/XTDB 2 port, HOWEVER building v1 *on* a fresh XTDB 2 store would
invert the ratified staging — the record v1 exists to expose (evidence,
substrate-2, missions, ledger) lives in the 1.x store today, so a fresh
store is empty exactly where the memory hypothesis needs it full; it pulls
A2's ~5,400-commit re-ingest forward as a prerequisite for a wiring-week
project; and a second serving store collides with I-0 (new engine deps in
the one JVM mean a restart, or a second serving process) — and raw `.edn`
logs address a *write* side that v1 deliberately does not have, THEN:
(i) **read-side confinement** — all store access lives in one backend
namespace (a map of per-tool read fns); the §12.3 envelope is the only
contract `zai_api.clj` sees; no Datalog, XTDB types, or store-specific query
shapes leak past it. Port cost to any future store = re-implement those read
fns; harness, prompt, and client change zero. This is deliberately a
*namespace discipline*, not a query-IR — A3's "don't abstract before the
query shapes are known" still holds.
(ii) **write-side rule, prospective** — any new durable write (none in v1;
flight records in the D-8 follow-on are the first expected case) lands as an
append-only `.edn` event log first, with store ingestion as a projection —
ingestable into 1.x or 2.x as we see fit, and still passing the normal
canonical-id write gate on ingestion.
(iii) **the XTDB 2 foothold proceeds, decoupled** — as a bounded excursion
(`E-futon1b-foothold`) extending the §11 spike: stand up a temporary
futon1b, translate the `:hx/endpoints` membership query to XTQL, and
re-implement *one* tool's read fn (`memory_search`) against a mirrored
evidence slice, proving the (i) seam actually swaps. A natural early
dogfooding task for Zai-on-v1 (§13.4),
BECAUSE the operator's actual requirement is cheap portability — which the
seam buys now at near-zero cost — while putting a new engine *under* v1
would make the working model hostage to the port it was supposed to precede.

### 12.2 Entity and relation types

**Deliberately zero new durable types in v1.** The boot packet is ephemeral
(assembled per session, never stored); memory-tool results are projections
of existing evidence entries, threads, hyperedges, ledger jobs, and
clock-store records. The first new durable type (the flight record) belongs
to the D-8 follow-on. This is itself a design decision: the mission's
hypothesis is that the *existing* record, properly exposed, is the memory.

### 12.3 Tool specifications

All new tools follow the existing pattern: a `tool-*` fn (+ `case` clause)
in `real_backend.clj` where new backend behavior is needed, plus a
`tool-specs` entry and `execute-tool` case in `zai_api.clj`. Existing 14
specs unchanged (INV-5). Memory-family results share the envelope
`{:frame "recorded, not necessarily current" :query {...} :items [...]}` —
each item carrying `:id :at :author` (or `:file :mtime` for markdown
sources), and `:query` echoing the filters, mode, and ranking that produced
the result set (`:ranked-by :structured` or `:semantic`). *(The `:query`
echo is revision R-1, added during ARGUE from
`aif/declare-the-conditioning` — see §13.1.)*

| Tool | Params (required*) | Backing | Returns |
|---|---|---|---|
| `boot_context` | — | registry (id, session, surface, hop-stack) + backend-config cwd (registry has no cwd — thread it from `make-invoke-fn` opts) + `clock_store/current-clock` + git (branch, porcelain dirty list) | the D-1 packet minus AGENTS.md |
| `repo_contract` | `repo` (default: cwd root) | resolve `AGENTS.md` at repo root | `{:path :size :mtime :content}` |
| `memory_search` | `subject`, `type`, `claim_type`, `author`, `since` (ISO), `tags`, `limit` | `evidence/store.clj query` (structured filters, exactly its `:query/*` keys) | envelope of entries |
| `evidence_graph` | `id`*, `mode`* ∈ thread \| reply-chain \| forks \| neighborhood, `limit` | `threads/project-thread`, `store/get-reply-chain`, `store/get-forks` + `threads/thread-forks`; neighborhood = futon1a `hyperedges-by-end` in-JVM (requiring-resolve), **always with limit** | envelope of the projection |
| `mission_context` | `target` (default: current clock target) | mission markdown (status banner + latest checkpoint) + `enrichment/query.clj enrich-file` + `mission_backend` obligation query | envelope: summary + ids + file paths |
| `recent_coordination` | `limit` | invoke-jobs public view + `/coordination/threads` (open bells, who-owes-whom, crossings) | envelope of jobs/threads |
| `pattern_memory` | `tags`, `limit` | evidence query filtered to PSR/PUR/PAR/proof-path tags (the *usage record* — distinct from `psr_search`, which searches the futon3a pattern *library*) | envelope of discipline records |
| `tool_history` | `agent` (default: self), `limit` | `clock_store` tool-use/edit records + invoke-job `:events` | envelope of tool events |

Plus **exposure only** (backend fns exist at `real_backend.clj:642-646`):
`psr_search`, `psr_select`, `pur_update`, `par_punctuate` get tool-specs and
`execute-tool` cases delegating to the existing `:psr-search` etc. keys.

Defaults: `limit` 20, max 100, enforced server-side (INV-2). The 8-round
loop bound stays; memory tools are cheap reads and no evidence yet says the
bound binds — revisit with `tool_history` data.

### 12.4 System-prompt additions (verbatim)

Appended to the existing four sentences; boot packet injected at session
start as part of the system message (IF injected per-turn the history would
need mid-conversation system edits, HOWEVER that breaks the message-log
shape, THEN static-at-boot with the `boot_context` tool for mid-session
refresh, BECAUSE dynamic state belongs to live tools anyway — D-5):

> Your boot context below is a snapshot from session start; refresh with
> boot_context if you need current state. Memory tools (memory_search,
> evidence_graph, mission_context, recent_coordination, pattern_memory,
> tool_history) return quotes from the record — recorded, not necessarily
> current. When you rely on one, cite its evidence id or mission file. For
> live state (what is running, what is dirty, who is active) use live tools,
> never memory. Do not present a single memory entry as a standing operator
> preference; standing preferences arrive tagged as such.

### 12.5 Invariants (checkable)

- **INV-1 provenance envelope:** every memory-family result item carries
  `:id :at :author` (or `:file :mtime`), and the envelope carries the
  `:query` echo of what produced it (R-1). Check: schema assertion in the
  tool-fn return path.
- **INV-2 bounded retrieval:** every memory-family tool enforces `limit`
  (default 20, max 100); any futon1a neighborhood traversal passes a limit.
- **INV-3 read-only memory family:** the eight §12.3 tools mutate nothing.
  (`pur_update`/`par_punctuate` are discipline tools, a separate family,
  already gated by their own preconditions.)
- **INV-4 boot packet ≤ 8 KB**, assembled fresh per session, never cached
  across sessions.
- **INV-5 additive change:** the existing 14 tool-specs and the invoke-fn
  contract (`prompt`, `session-id` in; `:result :session-id :error` out) are
  byte-identical in behavior for turns that use no new tool.
- **INV-6 I-0 respected:** all changes land via Drawbridge reload; no JVM
  restart; no new serving process.

### 12.6 Data flow

```
zai-repl.el ──invoke-stream──▶ zai_api.clj turn loop (≤8 rounds)
                                  │ execute-tool
                                  ▼
                        real_backend.clj ──┬─▶ fs / shell / JVM / IRC   (existing)
                                           ├─▶ evidence store query/threads   (memory_search, evidence_graph, pattern_memory)
                                           ├─▶ registry + clock_store + git   (boot_context, tool_history)
                                           ├─▶ invoke-jobs ledger + coordination threads   (recent_coordination; D-7 rehydration)
                                           ├─▶ mission markdown + enrich-file + obligations   (mission_context)
                                           └─▶ futon1a :7071 in-JVM, always &limit   (neighborhood mode)
```

No futon5 exotype diagram for v1, recorded per the lifecycle checklist:
v1 adds no loop (no prediction/measurement/learning structure) — it is a
read-side exposure layer on an existing turn loop. The D-8 follow-on *is*
a loop and must sketch its diagram during its own DERIVE.

### 12.7 View/UI

Minimal: `zai-repl.el` already streams text/tool_use/tool_result events, so
the new tools are visible with zero client work. One addition: a
`zai-repl-show-boot` command displaying the session's boot packet (fetch via
`boot_context`), so the operator can audit what the agent woke up knowing.
Non-intrusive display (reuse the existing result rendering; never steal
cursor).

### 12.8 Fidelity contract (GF-lite)

This extends existing behavior, so a light contract applies: **preserve**
all 14 existing tools and the invoke-fn contract unchanged (tripwire: a
scripted turn exercising read/search/edit behaves identically before and
after); **add** the §12.3 surface; **drop** nothing. Full GF matrix not
required — no donor is being replaced.

### 12.9 Build order (committed, replaces §8's "options" framing for v1)

1. **Slice 1 — expose what exists:** `boot_context` + `repo_contract` +
   system-prompt/boot-packet injection + the four discipline-tool
   exposures. Pure wiring except one thin backend fn.
2. **Slice 2 — graph memory, read side:** `memory_search`,
   `evidence_graph`, `recent_coordination`, `pattern_memory`,
   `tool_history`, with INV-1/INV-2 enforced.
3. **Slice 3 — `mission_context` + D-7 ledger rehydration.**
4. **VERIFY spike — the three-condition comparison (§8.4):** same task under
   (a) tools only, (b) file bootstrap, (c) graph bootstrap; score
   groundedness (claims backed by ids), repeated-explanation count, false
   inspection claims. Runs after slices 1–2 make condition (c) real.
5. **Deferred:** semantic search (§8.5, infra-dependent), A1/A2/A3 per D-3
   triggers, B2 per D-8, `E-futon1b-foothold` per D-11.iii (runnable any
   time after slice 2; independent of the VERIFY spike).

### 12.10 PSRs

#### PSR-1: `realtime/structured-events-only` applied to memory results

- Pattern chosen: realtime/structured-events-only
- Candidates: structured-events-only, ad-hoc result strings (status quo for
  existing tools)
- Rationale: the provenance envelope (INV-1) is this pattern applied inward —
  a memory result may not assert without carrying its witness (id, timestamp,
  author). Same move flight.spec.edn makes for flight records, applied to
  retrieval results.
- Confidence: high — the pattern is established in the futon3c library and
  the envelope is cheap.

#### PSR-2: `realtime/authoritative-transcript` applied to session rehydration

- Pattern chosen: realtime/authoritative-transcript
- Candidates: authoritative-transcript (ledger as source of truth),
  serialize-the-atom (private blob persistence)
- Rationale: D-7 — the invoke-jobs ledger is already the authoritative
  transcript of turns; rehydrating from it avoids a second, private,
  unauditable persistence mechanism and makes the harness remember through
  the system.
- Confidence: medium-high — text-only rehydration is a fidelity trade
  (elided tool detail), accepted deliberately per D-7's BECAUSE clause.

### 12.11 Exit-criterion self-check

Implementable from this section alone? The tool table names each backing fn
and its file; envelope, defaults, prompt text, and rehydration behavior are
specified; the two genuinely new behaviors (boot-packet assembly, ledger
rehydration) carry their data sources and edge handling. Open items are
*named as deferred with triggers* (D-3, D-8), not silently pending.

*Ratified by Joe, 2026-07-04* — see §13 header for the staging framing that
came with the ratification.

## 13. ARGUE — synthesis (2026-07-04)

**Ratification framing (Joe, 2026-07-04):** D-1…D-10 accepted as-is, with
the staging made explicit — v1 is deliberately a fairly *generic* harness;
the more futonic (flight-native) harness comes later as a migration, with
the working v1 as the instrument. And a new idea recorded for the D-8
follow-on: **Zai itself as a candidate co-builder of its own successor**
(dogfooding) — see §13.4.

### 13.1 Pattern cross-reference

Structured survey of `futon3/library/` against the §12 design (headers of
every cited pattern read 2026-07-04). The two PSR'd patterns (§12.10) are
included for completeness; the rest were surfaced by this pass.

| Pattern | Where in the design | How |
|---|---|---|
| `aif/no-self-certification` | INV-1 provenance envelope | A memory result may not move the agent's claims on evidence it could have manufactured: id/timestamp/author are stamps the model cannot fake, and the citation rule (D-9) counts only stamped results. Same move `flight.spec.edn` makes for flight records, applied to retrieval. |
| `aif/declare-the-conditioning` | §12.3 envelope — **fed back as revision R-1** | "Every valuation surface must declare what slice of the situation it consumed." A result set without its query is a lambda, not a value. The envelope now carries a `:query` echo (filters, mode, `:ranked-by`), so downstream readers can tell a structured hit from a semantic one and a narrow query from a broad one. |
| `agent/evidence-over-assertion` | D-9 citation clause | Extends the harness's existing inspection-honesty line from actions to memories: a claim resting on the record names its evidence id. |
| `agent/state-is-hypothesis` | D-5 live/recorded split; D-7 atom-as-cache | Memory results are hypotheses about the present, framed as quotes from the record; live state is re-sensed through live tools. The conversation atom is a working cache, never ground truth. |
| `agent/trail-enables-return` | `tool_history`, clock-store records, ledger | The trail already exists (clock_store, invoke-jobs); v1's contribution is making it *readable by the agent that left it*. |
| `agent/handoff-preserves-context` | D-1 boot packet + D-7 rehydration | The receiver is usually the agent's own future session: the packet orients it, rehydration restores its commitments, and the elision marker keeps the restored context honest about its fidelity. |
| `agent/scope-before-action` | D-1 (packet as orientation), INV-2 (bounded retrieval) | Declare the territory before entering: the packet names the mission/clock target; every retrieval is limit-bounded rather than open-ended. |
| `agency/self-attribution` | envelope `:author` stamps; rehydration marker | Memory results attribute claims to their original authors and are never re-voiced as the harness's own; rehydrated turns are marked as reconstructions from the authoritative ledger, not fresh utterances. |
| `realtime/structured-events-only` (PSR-1) | INV-1 | No bare strings from memory tools; every result is a structured event carrying its witness. |
| `realtime/authoritative-transcript` (PSR-2) | D-7 | The invoke-jobs ledger is the one transcript of record; session continuity derives from it rather than from a private copy. |

One pattern *revised* the design (R-1); the rest confirmed it — which is
what a design assembled from §7's leanings should expect, since those
leanings were themselves formed inside this pattern culture.

### 13.2 Theoretical coherence

The IDENTIFY-era anchoring (§1–§3, §9) was: a futonic harness gives the
model a situated way to ask "what is already known here, what is live, and
what evidence supports it?", with XTDB/evidence as shared memory and files
as the artifact layer. The §12 design serves this directly — every §3
candidate memory tool now has a §12.3 row — but the ratification sharpens
the theory in one respect: **"futon-native" is reached by migration from a
working generic harness, not by first build.** That is the stack's own
bud-a-sibling precedent (§11's futon1b instantiation, futon3b, futon7b)
applied to harness design: build the cheap working organism, let it
generate the data (tool call-logs for A3, tool_history for D-6's usefulness
prior, §8.4 metrics for the memory hypothesis itself), then grow the
successor against real evidence. The §11 coupling finding is not abandoned
by deferral — it is preserved as the D-3/D-8 triggers, and v1 is precisely
the apparatus that will trip or retire them.

### 13.3 Trade-off summary

Gave up, knowingly: **semantic recall** (structured filters only until the
futon3a embed server is a dependency worth taking — D-6); **full-fidelity
session replay** (text-only rehydration; tool facts re-fetched live — D-7);
**the flight unit now** (v1 keeps B1's known weakness — implicit structure
that must be re-mined — accepting it for one more generation because the
comparison experiment needs the cheap version as baseline — D-8);
**model-authored queries** (fixed tool menu; the model asks through typed
slots — D-3); **any new durable types** (§12.2). Got in exchange: zero new
write paths (I-0 risk ≈ nil), wiring implementable this week, an evaluation
gate *before* architecture, and records that feed every deferred option
rather than fight it.

### 13.4 Generalization notes

- **Endpoint-agnostic memory layer.** Nothing in §12.3 is Z.AI-specific:
  the tools mount on any OpenAI-style loop, and the envelope/citation/
  live-recorded discipline transfers to Claude peers and Codex harnesses
  unchanged. If the memory layer proves out, it is Agency infrastructure,
  not a Zai feature.
- **Boot packet as generic Agency orientation.** D-1's packet is what *any*
  registered agent should wake up knowing; only the AGENTS.md resolution is
  repo-relative.
- **Dogfooding the successor (Joe, at ratification).** The D-8 follow-on
  (`M-flight-native-harness`) can be built *with* Zai running on v1 — the
  replacement harness's construction becomes the live workload for the §8.4
  comparison: does mission_context/rehydration measurably reduce
  re-explanation and false-inspection claims across the build sessions?
  This closes a loop the mission itself proposes: the harness that
  remembers through the system, exercised on the task of designing the
  harness that predicts through it.

### 13.5 Plain-language argument

We're giving the Z.AI coding agent two things: a short orientation note
when it wakes up (who it is, where it is, what it's working on, the house
rules), and lookup tools over the group's shared record — past decisions,
ongoing missions, recent conversations between agents. Everything it looks
up arrives stamped with who recorded it and when, and the agent has to
quote those sources instead of presenting old notes as fresh fact. It keeps
no private notebook: anything worth remembering goes into the shared
record, where anyone can check it. We build this simple version first,
measure whether it actually makes the agent more grounded and less
repetitive, and only then design the more ambitious harness — possibly with
the Z.AI agent itself helping to build its own successor.

### 13.5a Checkpoint — slice 1 landed (2026-07-04)

Wired by the Claude owner in-place per §10 (Codex quota out; §12.9 item 1).
Uncommitted as of this checkpoint; changed files:

- **New:** `futon3c/src/futon3c/peripheral/memory_backend.clj` — the D-11.i
  seam namespace: `boot-context`, `repo-contract`, `boot-packet-string`
  (8 KB cap enforced, INV-4). Registry/clock-store reached via
  `requiring-resolve` (no load-order dependency on Agency).
- **Edited:** `futon3c/src/futon3c/agents/zai_api.clj` — six new tool-specs
  (`boot_context`, `repo_contract`, `psr_search`, `psr_select`,
  `pur_update`, `par_punctuate`) + `execute-tool` cases; system prompt
  extended with the §12.4 clauses (trimmed to name only tools that exist in
  slice 1); boot packet injected into the system message lazily on first
  turn; `tool-opts` gains `:cwd` and `:session-id-atom`. Existing 14 specs
  untouched (INV-5); `real_backend.clj` untouched (discipline tools were
  already wired there).

**Gates:** clj-kondo 0 errors / 0 warnings on both files;
`check-parens.el` OK. Loaded into the live JVM via Drawbridge `:reload`
(no restart, INV-6/I-0).

**Verification (live JVM):** `boot-context` returns identity + registry
block (`:agent/*` keys — one fix over the HTTP view's flattened names) +
clock + git (branch, 61 dirty) + AGENTS.md provenance; `repo-contract`
returns the 5,116-byte futon3c AGENTS.md verbatim with mtime; boot packet
rendered at 5,786 chars < 8,192. **End-to-end:** a fresh invoke-fn built
from the reloaded code ran one real Z.AI turn — the model called
`boot_context` and `repo_contract` unprompted-in-form and answered with the
correct branch (`agency-fixes-2026-06-11`), correct dirty count (61), and
the correct first heading of AGENTS.md ("Inter-agent handoffs in futon3c"),
each verified against ground truth.

**Operational note:** the already-registered `zai-5` picks up the new
tool-specs and dispatch via var indirection on its next turn, but its old
closure lacks the boot-packet injection and the new `tool-opts` entries
(nil-safe: `boot_context` falls back to `user.dir` and a nil session id).
Re-register the zai agent to get full slice-1 behavior.

#### PUR-1: `realtime/structured-events-only` (slice 1, partial)

- Pattern: realtime/structured-events-only
- Actions taken: boot_context and repo_contract return structured maps with
  provenance (`:as-of`, `:path`/`:mtime`) rather than bare strings.
- Outcome: partial — the full §12.3 envelope (`:frame` + `:query` echo)
  applies to the memory family proper and lands with slice 2; slice 1's two
  tools are live-state tools, which by D-5 carry `:as-of`/`:note` instead
  of the recorded-not-current frame.
- Prediction error: one — the in-JVM registry record uses namespaced
  `:agent/*` keys where the HTTP roster view flattens them; `select-keys`
  on the flat names returned `{}` on first live test. Fixed same session.

### 13.5b Checkpoint — slice 2 landed via zai dogfooding (2026-07-04)

**Built by zai-7 itself** (the §13.4 move, first exercise), belled by
claude-16, who reviewed, fixed, reloaded, and verified. Three attempts:

1. **Attempt 1 exhausted all 8 tool rounds on spec/source reads and was cut
   off before writing anything** — the cleanest live datum yet that the
   turn-native round bound binds (B1's weakness, §11). Consequences: round
   bound raised to 24 in `zai_api.clj` (the "revisit with data" note in
   §12.3 is hereby exercised), and the retry prompt inlined the envelope
   spec — a context-economy lesson for handoffs to 8-32k-round-budget
   harnesses.
2. Attempt 2 died to a reviewer-side fault (claude-16 called
   `/agents/:id/rebind`, which hardcodes `:claude`, silently replacing the
   zai invoke-fn; repaired via `POST /api/alpha/agents/restore`).
3. **Attempt 3 succeeded**: 4 of 5 tools (`memory_search`, `tool_history`,
   `evidence_graph` with thread/reply-chain/forks + neighborhood TODO,
   `pattern_memory`), `recent_coordination` deliberately skipped per the
   stop-cleanly instruction. Zai ran both gates itself and reported clean;
   it even attempted `par_punctuate` unprompted (rejected by PAR shape
   validation — the discipline tools are being *reached for*, which is the
   §12.4 prompt clause working).

**Review findings (fixed by the reviewer per the fix-don't-re-bell rule):**
- `pattern_memory` returned raw evidence entries — INV-1 item-shape
  violation and full-body leak; now projects via `evidence-item`.
- JSON-sourced `:ref/type` arrives as a string but store subject-refs are
  keyword-typed — thread-mode queries would silently match nothing; now
  coerced in `evidence-graph` and `memory_search`.
- `memory_search` lacked the §12.3 `:subject` filter; added.
- Verified-correct zai claims worth noting: `resolve-backend` does fall
  back to the default store on nil (its comment was right), and
  `thread-forks` takes the projection.

**Verification:** clj-kondo 0/0 + check-parens OK (reviewer-run); Drawbridge
reload; all four tools called directly in the live JVM (envelope, `:query`
echo, INV-1 item stamps confirmed against real entries — the store's
contents are the mesh's own coordination evidence, pleasingly); one live
end-to-end zai turn called `memory_search` + `tool_history` through the
JSON round trip and reported ids matching ground truth exactly.

**Observability infrastructure landed alongside** (operator request, Joe):
bell-seeded turns previously ran invisibly and completed silently. Fixed:
`run-invoke-job!` now installs a composing ledger sink recording text/tool
events per job; `auto-bellback-recipient-types` widened to include `:zai`;
new `futon3c/emacs/agent-follow-mode.el` replays out-of-REPL turns into the
agent's REPL buffer with tool-line coloring and a live "invoking" progress
line. This is the §11 B2-corollary observability need served at the turn
grain — flight-grain capture remains the D-8 follow-on's job.

**PAR-rejection mystery resolved (post-checkpoint, same day):** zai's
failed `par_punctuate` was traced to the reviewer's *own* slice-1
tool-spec, which declared `prediction_errors` as an array of strings while
`futon3.gate.shapes/PAR` requires `[:vector map?]` — zai followed the spec
it was given and was correctly rejected; its "unrelated to my code changes"
guess was right. Fixed: spec now declares object items and the
`execute-tool` case coerces stray strings to `{:description s}`; verified
live (PAR persists). Two side-findings recorded while tracing: (1)
`real_backend.clj` requires `futon3.gate.shapes` — resolved from *futon3b*'s
source tree, but the require still names futon3, an I-5 smell worth a
follow-up look; (2) minimal PARs (nil what-didnt, empty vectors) always
passed, so the discipline loop was only broken for *substantive* PARs — the
worst kind of partial breakage, invisible until an agent tries to say
something real.

**Slice 2 COMPLETE (same day, second zai handoff):** zai-7 built
`recent_coordination` (compact job/edge items via the mesh_qa precedent;
correct backing ns `futon3c.social.coordination-ledger`), landed a
substantive PAR on retry (`par-3c6d2378…` / `e-pp-path-0d796241…`), and —
taught the bell protocol — **closed the loop with an explicit
`agency_send.py` bell to claude-16**, the harness's first agent-initiated
Agency speech act. The bell protocol is now also in the zai system prompt
for future sessions.

Review of the final delivery surfaced two deep findings, fixed by the
reviewer:
- **Two evidence stores:** discipline/proof-path evidence lands in the
  durable XTDB store (`futon3c.dev/!evidence-store`); memory tools queried
  only `store.clj`'s in-process default — zai's PAR was invisible to
  `pattern_memory` while its landing claim was *true*. The seam now reads
  both (`evidence-query` merges, deduped; `graph-call` prefers durable
  with default fallback).
- **`:query/tags` is AND semantics** (verified live), so `pattern_memory`'s
  four-tag default could never match; now queries per-tag and merges
  (ANY-of).
After the fixes, `pattern_memory` live-returns the actual discipline
record: claude-9's PSR/PURs and zai-7's own PAR — the harness reading the
Agency's pattern history across stores, which is §3's hypothesis working.

**Remaining:** `evidence_graph` neighborhood mode (TODO). Then slice 3
(`mission_context` + D-7 rehydration), then the §8.4 comparison (VERIFY
spike).

**Slice 3a landed through a JVM crash (2026-07-04, later):** zai-7 built
`mission_context` + the `evidence_graph` neighborhood mode but hit round
exhaustion a third time, mid-edit (paren-broken tree). The serving JVM then
crashed (peripheral fault, unrelated) and rebooted **from the working
tree** — so all landed harness code compiled in at boot (event capture,
`:zai` bellback, 24 rounds, memory tools), and the registry restore brought
all agents back with real invoke-fns. The reviewer completed zai-7's
unfinished wiring (missing `mission_context` execute-tool case, `:end_id`
pass-through) and fixed three bugs: `find-mission-file`'s empty-string
truthiness + missing globstar; the ring-response unwrap on
`hyperedges-by-end`; and a single-deref on `!f1-sys` (var→atom→map needs
two). Both tools live-verified: `mission_context` returns the mission file,
status banner, and last checkpoint; neighborhood mode returns real
code-graph hyperedges through the seam. The `enrich-file` composition is
formally DROPPED from v1 `mission_context` (file-oriented, not
mission-oriented) — recorded as a deviation from §12.3.

**D-7 rehydration just acquired its motivating incident:** the crash
destroyed both zai agents' in-memory conversation histories — zai-7 lost
its slice-2/3 working context, zai-8 its excursion context — exactly the
loss the invoke-jobs-ledger rehydration is designed to absorb. Rehydration
(the remaining slice-3 half) is next, built by the Claude owner.

**E-futon1b-foothold OPENED (2026-07-04, on Joe's word):** the D-11.iii
excursion is live at `futon2/holes/E-futon1b-foothold.md`, owned by
**zai-8** (a second runner, registered for this purpose so zai-7 stays
clocked to this mission), dispatched S1–S3 by claude-16. Bounded: throwaway
in-process XTDB 2 node, synthetic seed only, never touches `:7071`, kill
criteria named. Its S3 verdicts feed the D-3/A2 trigger; S4 (the seam swap)
follows after review. The build sessions double as live workload for the
§8.4 comparison.

### 13.5c First condition-(c) dry run — graph bootstrap, scored (2026-07-04)

After the crash destroyed zai-7's session memory, it was dispatched to
reorient using ONLY the memory tools (no file reads) and answer five
ground-truth-checkable questions. Score: **4 of 5 correct with citations;
the one wrong answer is the run's chief product.**

- **Correct:** mission identity/status/checkpoint via `mission_context`;
  the day's full bell graph via `recent_coordination` (including crash
  evidence: two failed auto-bellbacks); own fresh-session state via
  `boot_context`; and a next-work analysis that *derived D-7 rehydration as
  top priority from tool evidence alone* (null `tool_history` vs. its own
  pre-crash coordination traces found via `memory_search` by author — the
  asymmetry proves the memory gap).
- **Wrong, instructively:** "my PAR never landed." It did
  (`e-pp-path-0d796241…`, present post-crash). Two tool defects compounded
  into a confident false claim: (1) **provenance-stripping projection** —
  PAR proof-paths land authored `gate-pipeline`, and the compact item shape
  strips the body where the session-ref lives, so an agent cannot recognize
  its own punctuation in the shared record; (2) **checkpoint-staleness** —
  `extract-last-checkpoint` serves the last checkpoint-*headed* section
  (§13.5b, which describes the PAR *rejection*), while the later
  resolution lives in non-checkpoint-headed paragraphs. The tools
  corroborated each other's staleness — exactly the groundedness failure
  mode the §8.4 comparison exists to measure, caught on the first run.
- **Third product finding (zai-7's own):** `evidence_graph` and
  subject-scoped `memory_search` return empty for the mission because
  mission evidence is not subject-ref linked — "the richest mission data
  lives in markdown, unreachable by graph tools." Subject-ref linking is
  the graph condition's missing substrate.

Fixes queued from this run: carry punctuator identity into discipline
evidence items; make checkpoint extraction date-aware rather than
heading-anchored; subject-ref linking for mission evidence. zai-7's
first successfully-landed self-recognized PAR: `par-63c9a10a…`.

**Fix outcomes (same day, zai-7 dispatch + reviewer write-side):** two of
three closed. (1) Punctuator identity: zai-7's read-side extractor was
correct against the shape it found — but that was the *in-pipeline* shape;
the bridged evidence body strips gate records to ids, so the reviewer
completed the write side (`persist-proof-path!` now stamps
`:evidence/session-id` + body `:session-ref`/`:discipline-kind`).
Round-trip verified: a fresh PAR returns from `pattern_memory` with its
session-ref and kind — agents can now recognize their own punctuation.
Pre-fix punctuations remain identity-less (honest limit). (2) Checkpoint
staleness: `content-after-checkpoint?` flag + inline supersession note,
no date parsing. (3) Subject-ref linking: still open (infrastructure-sized).
**D-7 observation:** this was zai-7's first dispatch under rehydration and
its first to complete cleanly in budget with gates + explicit bell — no
round exhaustion, no re-reading of known context. One datum, not proof,
but it points where §8.4 will look.

### 13.5d Checkpoint — D-7 rehydration live; slice 3 complete (2026-07-04)

Built by the Claude owner (invoke-path surgery, reserved per §13.5b's
division of labor). **One honest deviation from the DERIVE spec:** D-7
assumed the ledger held turn text; in fact *nothing persisted the caller's
prompt* (ledger jobs have no prompt field; invoke evidence entries are
coordination edges) and assistant text survives only as `result-summary`
plus (since today) captured events. So v1 rehydration is **commitment
summary, not conversation replay** — per prior turn: when/who/surface,
prompt (marked "[prompt not recorded]" for pre-capture turns), and the
result summary — with the gap fixed forward: `run-invoke-job!` now records
the raw caller prompt as a ledger event, so future sessions rehydrate with
prompts.

Mechanics: `session-turns` + `rehydration-string` in the memory seam
(terminal jobs only; bellback echo jobs filtered by agent-id; 6 KB cap);
injected into the system message on first turn alongside the boot packet,
marked as reconstruction. Gates 0/0 + OK; loaded surgically (no ns reload
of serving http).

**End-to-end proof:** zai-7 restored (fresh invoke-fn), then belled a
no-tools memory check — it answered from the rehydrated block alone:
correct prior-turn count and caller attribution, correct identification of
its latest completed turn (the §13.5c orientation exercise). The crash
scenario that motivated D-7 (§13.5b addendum) is structurally closed.

Slices 1–3 are now ALL COMPLETE. Remaining on the mission: the §8.4
three-condition comparison proper (condition (c) dry-run done, §13.5c),
the three §13.5c product fixes (punctuator identity in projections,
date-aware checkpoint extraction, subject-ref linking), and DOCUMENT.

### 13.6 Exit-criterion self-check (ARGUE)

Inevitable-given-constraints? Each §12 decision now has at least one
library pattern behind it, the one pattern that pushed back produced a
recorded revision rather than a silent accommodation, and the trade-offs
are enumerated with what was bought. Communicable? §13.5 uses no term of
art. ARGUE complete; VERIFY's designated spike is the §8.4 three-condition
comparison, which requires slices 1–2 to exist first.

## 14. VERIFY — the §8.4 three-condition comparison (2026-07-04)

**Design.** Three fresh zai agents, identical task, one condition each,
run concurrently. Conditions via a new `:memory-mode` on `make-invoke-fn`
(`:none` = no boot packet, no orientation/memory tools — condition (a);
`:files` = boot packet + file/shell tools, no memory family — (b);
`:full` = everything — (c)). Task: four orientation questions about this
mission with checkable ground truth (status/checkpoint, discipline records,
recent coordination, next open work), citation required per claim,
"unverified" preferred over guessing.

**Metrics** (jobs `…343/344/345`, scored by claude-16 against ground truth):

| | (a) tools-only | (b) file bootstrap | (c) graph bootstrap |
|---|---|---|---|
| wall time | 2 m 20 s | 3 m 24 s | **45 s** |
| tool rounds | 6 | 12 | **4** |
| Q1 status/checkpoint | correct | correct | correct |
| Q2 discipline records | correct, via raw proof-path EDN files on disk | correct, but sourced from the mission doc's own *narrative* (secondhand) | correct, with store-citable evidence ids |
| epistemics | good | good | **best: cross-checked tool results against the file, and flagged the claude-9 PSR/PUR subject-linkage as "unverified"** — the exact honesty behavior the design asked for |
| false claims | none observed | none observed | none observed |

**Verdict: the §9 working hypothesis is supported.** Graph bootstrap was
3–4.5× faster, used the fewest rounds, produced the only store-citable
ids, and exhibited the best calibration — while the file substrate proved
richer than MAP assumed: condition (a) independently discovered the raw
proof-path EDN files on disk and extracted correct ownership from them.
Files and graph are complementary layers (artifacts vs. shared memory),
exactly as §9 framed; the graph layer is the faster, better-cited route
to the same truths, not the only route.

**Limitations, recorded honestly:** n = 1 per condition, same model, one
task family (orientation); "reduced repeated explanation" is proxied by
round counts, not measured across sessions (though §13.5d's
first-clean-dispatch-under-rehydration observation is corroborating);
the ledger's 2000-char text-event trim clipped the Q3/Q4 tails in the
observability record (capture limitation, noted for the ledger design;
full results delivered via bellback).

**Completion-criteria check (§5's evaluation criteria):** groundedness ✓
(citations checked out; (c) strongest); repeated explanation ✓ (round
economics + §13.5d datum); mission continuity ✓ (D-7 no-tools memory
check, §13.5d); fewer false claims ✓ (zero observed; (c) uniquely
self-flagged unverifiable linkage). **Decision-log entries:** no DERIVE
revisions required by VERIFY; `:memory-mode` added to the harness as
spike apparatus (kept — it is also the natural switch for future
ablations); condition agents `zai-cond-a/b/c` deregistered after the run.

VERIFY exit criterion met. Remaining phases: DOCUMENT (plus the parked
follow-ons: subject-ref linking, B2 flight-native, A1/A2 triggers).

### 14.1 Field note — the wedged-search incident (2026-07-04, evening)

Real-world testing (Joe driving zai-8 on exploratory work) surfaced a
harness-tool robustness defect no staged test had: `tool-grep` walked
directory trees unboundedly (dot-*files* filtered, dot-*directories*
descended), so a `search` defaulting to the workspace root slurped
gigabytes of binary — caught live mid-decode of a 279 MB CUDA `.so` from
`.venv`, ~50 minutes into the call. Fixed same evening: dot/vendor/store
directory pruning, 2 MB file cap, 30 s budget with interrupt check,
truncation reported as an explicit sentinel result.

Three mission-relevant observations:
1. **The observability stack earned its keep diagnostically:** the
   follow-mode progress line surfaced the symptom ("using search", stuck),
   the registry showed the 50-minute invoke, thread stacks pinned
   `tool-grep`, and `lsof` named the file. "What is my agent doing right
   now?" was answerable end-to-end.
2. **Turn-native has no in-flight brakes:** the wedged call could not be
   stopped without a JVM restart — no per-tool budget, no cancellation
   point, and the agent's turn drainer is serialized behind it, making the
   agent unavailable until the tool returns. More accumulated motivation
   for the B2/flight-native follow-on, where the act step is gated and
   budgeted by design.
3. **Tool implementations are part of the harness's safety surface:** the
   §12.5 invariants bounded the *memory* tools (INV-2 limits) but the
   inherited file tools had no equivalent discipline. Lesson: bounded
   retrieval is a harness-wide invariant, not a memory-family one.

### 14.2 Field note — the orphaned-JVM incident (2026-07-04, second crash)

The serving JVM was SIGTERM'd twice today, both times coinciding exactly
with zai `run_shell` invocations of `clojure -M:node` (a second JVM).
Root cause found in the harness tool layer, sibling of §14.1:
`run-command`'s timeout path called `.destroyForcibly` on the outer bash
only, ORPHANING the `clojure`→`java` tree underneath — a real defect,
fixed below. **Attribution correction (same evening, from syslog):** the
orphan theory was WRONG for the crashes themselves — at OOM time exactly
one java existed. Both JVM deaths were kernel OOM events (17:15 and
20:27 local, `Out of memory: Killed process (2.1.201)`) whose primary
victims were **warm-pouch Claude CLI processes ballooned to 12 GB and
16.4 GB anon RSS** respectively (firefox at 22 GB as standing load); the
serving JVM died in the exhaustion cascade each time. The systemic
finding is therefore about M-kangaroo, not run_shell: **long-lived warm
pouches grow without bound** and need RSS-based recycling (see
E-zai-agent-upgrades for the parked zai analogs; the pouch version is
urgent by two data points in one day).

Fixes, live and verified: (1) timeout now kills the whole process tree,
descendants first (verified: a timed-out grandchild `sleep` is reaped —
and the verification itself tripped the pgrep-matches-own-wrapper
footgun before the instrument was corrected); (2) the futon1b `:node`
aliases are capped `-Xmx1g -XX:MaxDirectMemorySize=1g`.

Lesson, extending §14.1's: **bounded retrieval AND bounded subprocess
lifecycle are harness-wide invariants.** A tool that can start a process
must own the whole tree's death. Second entry in the "real use finds
what staged tests cannot" series; both found by operator observation of
live agents within hours of the capability existing.
