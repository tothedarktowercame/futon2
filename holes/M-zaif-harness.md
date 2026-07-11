# Mission: M-zaif-harness — a G-scored controller for the interactive loop, in XTQL

**Date:** 2026-07-11
**Status:** OPEN — IDENTIFY drafted from the lucy session discussion, awaiting Joe's read.
**Owner:** claude (Fable session, 2026-07-11). Driver: Joe.
**Home:** futon2/holes/ (beside M-text-sidecar, which builds its perception substrate)

Cross-refs:

- `M-text-sidecar.md` §CHECKPOINT 2026-07-11 — the discussion record this
  doc formalizes (four arms, three ledgers, admissibility, backoff, priors).
- `~/code/p4ng/main-2026.tex` (post `da7389b`) — the WM catalogue: R7/R14
  precision, cascades-as-semilattices, §Preregistration.
- `futon3c/src/futon3c/agents/zai_api.clj` — the existing zai runner, the
  starting point (773 lines; six read-only memory tools; memory-mode
  ablation conditions from M-custom-harness §8.4).
- `futon2/holes/E-zai-agent-upgrades.md` — parked runner upgrades (U1
  transcript persistence, U2 rolling window) the controller will sit beside.
- `E-futon1b-operational-switchover.md` — the store this all queries.

## HEAD — operator anchor

Joe (2026-07-11): "What if the zaif runners had a good understanding of the
operator's input texts, not just the tagged patterns?" · "a zaif profile
would be about applying aif *within* a known-to-be-interactive loop" · "we
could port any XTQL aspects between the different profiles" · "What I'm
interested in is how this relates to XTQL, which is the whole motivation
for the port to XTDB."

## IDENTIFY

### The gap

1. **The zai runner recalls; it does not decide.** zai_api.clj already has
   recall-as-typed-query (memory_search, evidence_graph, pattern_memory,
   mission orientation — all read-only, all returning the §12.3 envelope).
   What sits between turns is static heuristics: no G, no precision state,
   no principled act/ask/yield choice. The p4ng paper names this exactly:
   "the harness does not yet compute G; preferences and selection are the
   natural next transplant."
2. **The WM's preregistration routes here on failure.** The §prereg
   stopping rule: if the learned reward's discrimination (S3) cannot clear
   its null after two revisions, "the question routes to explicit
   operator-interest modelling." zaif IS that route — built in advance,
   not improvised on stop.
3. **Label rate is the WM's binding constraint** (2 of the first 21
   escrowed deposits ever flown). Every operator turn in an interactive
   session is an adjudication event; zaif feeds the same R14 fold a channel
   that is orders denser.

### What zaif is (one paragraph)

In the WM, preferences are pinned (the explicit belly); in an interactive
loop the operator's C-vector is latent and each operator message is the
highest-precision evidence about it. The zaif profile adds a controller
between turns that holds (task belief, operator-C belief, precision state)
and G-compares four arms: **retrieve** (epistemic action about C priced in
tokens — the text sidecar makes this arm exist), **act** (pragmatic value
now, rework risk if C is misread), **ask** (high EIG about C, spends the
scarce resource: operator attention), **yield** (give the turn back — the
always-available safe action that autonomy lacks). zai and zaif share
everything below the seam; they differ only in this controller.

## The XTQL section — why the port's motivation and the harness's language are the same thing

This is the load-bearing relation, in five parts:

### 1. Perception is a query library

Every observation the controller consumes is an XTQL query over the
futon1b store: the γ-fold inputs, the C-retrieval hydration, sorry
neighborhoods, claimed-vs-verified pairs. The harness's observation model
is not code that *interprets* the store — it is a **named library of XTQL
forms**, versioned in the repo, one name per observation channel. Sketches
(shapes, not final):

```clojure
;; operator turns in a window (C-inference hydration after FTS5 candidates)
(-> (from :evidence [*])
    (where (= evidence/type :coordination)
           (= evidence/author "joe")
           (>= evidence/at since)))

;; per-cascade outcome pairs (γ fold input; cascade id carried in tags)
(-> (from :evidence [xt/id evidence/at evidence/tags evidence/claim-type])
    (where (>= evidence/at since)))
;; -> partitioned in the fold by cascade tag × {confirm correct redirect}

;; sorry neighborhoods by terminal overlap (backoff, trial order 1)
(-> (from :hyperedges [xt/id hx/type hx/endpoints])
    (where (= hx/type :code/v05/sorry)))
;; -> Jaccard over :hx/endpoints in the fold — auditable, no embedding
```

### 2. Queries are data, so observations carry their provenance

XTQL forms are EDN. When the controller records an observation (an
evidence doc), the doc can embed **the query that produced it**. That
closes a no-self-certification hole that prose harnesses cannot close: an
observation is auditable by *re-running its own query* against the
bitemporal store. "What the agent saw" is not narration — it is a
replayable form plus a system-time coordinate.

### 3. The precision ledgers are aggregations, not state files

γ(cascade), C-channel precision, actand-indexed error precision — all
three ledgers are **derived views**: folds over the evidence stream,
recomputable from scratch. No ledger file to corrupt or trust; the store
is the ledger, the XTQL aggregation is the read. This is also what makes
the **retro-bootstrap** possible: the correction-lexicon fold runs over
the 90k historical turns the moment the sidecar can supply text candidates
— γ starts life calibrated, not flat.

### 4. Bitemporality makes precision auditable and the loop replayable

System time is an independent axis, so "what was γ(cascade-X) when the
agent chose to act on July 9th?" is a query, not an archaeology project.
Precision updates can be audited retroactively; a disputed adjudication
can be re-run as-of; and the zai-vs-zaif A/B (below) can be *scored after
the fact* from the record alone. The append-only evidence semantics
(P1's 1.000 finding) mean as-of reads are cheap and exact.

### 5. One query library, three harnesses, and the Phase E dividend

zai, zaif, and the WM share the query library; profiles differ in the
controller that consumes it. Because XTQL is data, the same forms run
over HTTP through the futon1b seam today and in-process after the Phase E
fold-in — the controller cannot tell the difference. Porting "XTQL
aspects between profiles" is therefore file-copy-grade: the γ fold built
for zaif is the WM's grounded-γ feed pointed at a different event stream.

**FTS5's place in this** (P2 decided 2026-07-11): the text index is a
*candidate generator*, never an authority. `text → candidate ids → XTQL
hydration + re-check + structured filters` — the same
prefilter-plus-recheck semantics XTDB #5637 proposes in-core,
demonstrated at the application layer. BM25 rank enters the controller as
the retrieve arm's score input; membership is always decided by the
store.

## Controller design (settled in discussion — detail in M-text-sidecar §CHECKPOINT 2026-07-11)

- Arms: retrieve / act / ask / yield, G-compared per turn; retrieve EIG
  estimable pre-query from posting statistics (≈ IDF).
- Three precision ledgers: γ(cascade) [R14], C-channel [R7 on the operator
  model], actand-indexed [R7 on the world model]; corrections routed by
  the lexicon ("not that/instead" → γ; "not what I meant" → C-channel;
  "that was stale" → actand).
- Asymmetric admissibility: agent self-talk may only LOWER precisions;
  only operator text or a discharged interface-sorry may raise them.
- Event streams: v0 lexical corrections over session-tagged turns
  (retro-bootstrappable); v1 sorry-discharge ledger (M-a-sorry-enterprise
  — critical path for auditable γ, not for the working v0).
- Backoff: sorry terminal-overlap (Jaccard) → shared prose-embedding →
  learned structural only on demonstrated residual.
- Cold start: γ0 from operator lane (silent/brief/nag); new cells at
  uniform prior (matches the WM mint lane).
- Update rule: the WM's R14 fold, ported not reinvented.

## Boundaries (what zaif refuses) — and the mission-parameterization refinement

1. **Strategy is received, not computed** (refined 2026-07-11 from "no
   strategic G(a)", Joe): zaif never picks the mission — the operator,
   M-autoclock-in, or the WM's strategic loop does — but **G is
   mission-parameterized**, like the WM's inner loop pinned to the judge's
   pick. Clock-in swaps in the clocked mission's G parameterization: the
   C-belief slice, the γ slice, and the mission's `:want`s + open
   interface-sorries as partial preferences. The auto-clock witness is the
   provenance of which G governed each decision. Empirical warrant: the
   p4ng prereg S3 result — reward discrimination clears its null *within*
   missions but not across — mission scope is where the operator's value
   structure is demonstrably recoverable. Consequences: γ becomes
   γ(cascade|mission) with the pattern→mission embedding as the bridge
   between the two indexes (PZ2 tests which index is primary); retrieval
   scopes via the existing mission_orientation tool; **autoclock ambiguity
   is itself C-uncertainty feeding the ask arm** ("still on X, or moved to
   Y?"). Mid-session re-clock rule: in-flight acts complete under the old
   mission's G; the next turn deliberates under the new. Gating condition:
   M-autoclock-in working (the clock-store read tool is already in
   zai_api.clj's tool specs).
2. **No autonomous scheduling.** Known-to-be-interactive is a premise, not
   a degraded mode — it keeps yield always available and the safety story
   simple. The WM keeps the autonomous lane.
3. **No in-band formalism.** G, γ, C live in Clojure around the API call;
   the model sees retrieved context and instructions, never its own belief
   arithmetic (the FuLab lesson).

## Deliverables

- **Z1 — the query library + retro-bootstrap.** The named XTQL forms for
  all three ledgers and the C-hydration path, plus the offline fold over
  the historical corpus (needs D1's text candidates for the lexicon
  scans). Output: initial γ/C/actand tables with provenance, reproducible
  by script. *This deliverable is pure XTQL — it is the port's motivation
  made cash.*
- **Z2 — the controller in the runner.** A `:profile` option in
  zai_api.clj (`:zai` = current heuristics, `:zaif` = the G controller),
  sharing tool-specs and the memory seam; the controller consults Z1's
  views between turns and records its arm choices + G terms as evidence
  (queries-as-provenance, §2 above).
- **Z3 — the A/B on lucy-joe sessions.** Same operator, same stack,
  alternating profiles; scored from the record (bitemporal, §4): asks per
  achieved outcome, corrections per act, yield timing vs operator lane.
  Metrics fixed before the first scored session, in the p4ng prereg
  spirit.

## Probes before build

- **PZ1 — correction lexicon recall/precision.** Sample historical turns,
  hand-label a small set (Joe + agent), measure the lexicon's hit rate
  before trusting the retro-bootstrap. The lexicon is load-bearing; it
  gets measured first (car of sequence).
- **PZ2 — ledger grain check.** Does γ(cascade) have enough events per
  cell on the real record, or does v0 need to start at γ(pattern) with
  cascade as a later refinement? One XTQL census answers this.
- **PZ3 — WM fold portability.** Read the laptop's R14 fold implementation
  against the zaif event shape; enumerate the actual deltas (expected:
  event-source adapter only).

## ARGUE (strategic)

Build the controller around the store, not in the prompt: the FuLab
lesson (formalism in-band fights the agents) plus the p4ng inversion
(every load-bearing quantity lives around the model as typed records).
XTQL is what makes "around the model" *expressive enough to be the whole
harness language* — queries as perception, aggregations as ledgers,
bitemporal reads as audit. The port to XTDB 2 was justified by XTQL;
zaif is the first consumer that uses all of it at once.

## DEMO RECORD — 2026-07-11: first live recall, and the thesis in miniature

First zai-1 session on lucy (Joe via `cz`; futon1b-backed corpus, 94k
evidence docs). claude-5 belled zai-1 (typed query) to recall the 5 most
recent joe-authored entries and orient on the clocked mission.

**What happened:** zai-1 replied — articulate, internally coherent, and
wrong: "the store is near-empty," supported by a zero-result
memory_search and its recent_coordination tool (which by design shows
live invoke jobs only). External check: the store held **8,882
joe-authored docs** at that moment. A re-bell with pinned args returned
5 items (newest `e-def1d82a`, a recorded operator chat turn). zai-1 then
disputed the arg-mismatch diagnosis, claiming byte-identical args — a
claim nothing can adjudicate, because U1 (transcript persistence) is
parked and the job recorded `tool-events: 0`.

**Root cause (established after the dispute):** the evidence backend
passed no `limit` through, so the 5-item recall hydrated all 8,882 docs
(10MB); cold, that exceeded the 30s timeout, and memory-backend's
`safe-call` swallowed the exception into a silent empty — the
default-atom fallback then showed only live-session entries. Fixed:
futon3c `limit/ephemeral pushdown when membership is server-decidable`
(see commit of the same name).

**Why this is the mission's own argument, live, on day one:**

1. *Queries-as-provenance* (§XTQL-2) would have made the first failure
   self-diagnosing: the observation would carry its query AND its
   error — a swallowed timeout could not masquerade as an empty result.
2. *Asymmetric admissibility* would have blocked the bad inference: "the
   store is near-empty" is a self-generated conclusion that RAISES
   effective confidence about the world; under the rule it can only
   lower precisions, never establish facts.
3. *U1 is not optional for zaif.* Without the transcript, the agent's
   claim about its own past tool args was unfalsifiable — the dispute
   was settled only by stepping outside the loop (direct store count).
   A zaif agent must be able to settle such disputes itself; U1
   (transcript persistence) moves from parked-upgrade to prerequisite.
4. The failing sub-tool matters for C-inference too:
   `recent_coordination` answers "what is happening now," not "what has
   the operator said" — the controller's channel taxonomy should mark
   which tools can bear on which beliefs.

## Log

- 2026-07-11 (later) — first live demo run + failure analysis (DEMO
  RECORD above); backend limit-pushdown fix committed; U1 promoted to
  prerequisite.
- 2026-07-11 — mission authored (Fable session) from the zaif discussion
  with Joe (recorded in M-text-sidecar §CHECKPOINT 2026-07-11). P2=FTS5
  decided the same day. Nothing dispatched; PZ1 is the car when this
  opens.
