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

## HANDOFF LOG (claude-5 driving via checked handoffs to zai-1 — Joe, 2026-07-11)

Protocol: each handoff is a :have→:want packet, pre-flighted against five
checks (capability / closure / external adjudicator / size / blast radius)
before the bell; outcomes adjudicated externally by claude-5 and recorded
as tagged evidence — each adjudication is itself a claimed-vs-verified
pair, i.e. the γ-ledger's event stream, generated while building its
consumer.

### H1 — PZ1 slice 1: correction-lexicon sample scan (dispatched 2026-07-11)

**:have** — the futon1b evidence API on 127.0.0.1:7074 (read-only GETs;
windowed; author/type/claim-type/since/before/limit params); bb on PATH;
the v0 lexicon (in the packet, three ledger routes: :gamma policy markers,
:c-channel intent markers, :actand world-model markers); operator chat
turns identifiable as type=coordination, claim-type=question, author=joe,
tags incl. chat/turn/user.

**:want** — two files under `futon2/holes/labs/M-zaif-harness/`:
`pz1_lexicon_scan.clj` (bb script, args: --since --max-docs, pages the
API by :at cursor, extracts all string values from :evidence/body, scans
case-insensitively, no writes elsewhere) and `pz1-sample-scan.edn`
(`{:sample {…params :docs-scanned n} :counts {marker→n per route}
:hits [{:id :at :route :marker :snippet≤120ch} …]}`) from a run with
--since 2026-06-01 --max-docs 2000.

**Acceptance (external):** `bb pz1_lexicon_scan.clj --since … --max-docs
50` exits 0 rerun by claude-5; EDN parses; ≥5 random :hits ids re-fetched
from the store and the marker verified present in that doc's body; counts
= hits aggregation; `git status` shows only the two files. **No commits**
(operator-directed commits only).

**Pre-flight:** capability PASS (shell+files+HTTP GET only) · closure
PASS (endpoint, params, lexicon, shapes all in-packet) · adjudicator PASS
(rerun + spot re-fetch, independent of zai's report) · size PASS (one
script + one run) · blast radius PASS (read-only API; writes confined to
two named lab files).

**Adjudication (2026-07-11): PASS — verified, not believed.** Claimed:
1,437 docs scanned, 153 hits (γ 109 / C-channel 24 / actand 20), 0
errors, two files. Verified externally: independent rerun exits 0 (50
docs → 4 hits, sane); EDN parses and per-marker counts equal the hits
aggregation (4 zero-count markers explicitly present — good practice);
5/5 randomly sampled hit ids re-fetched from the store with the claimed
marker confirmed in the body; writes confined to the two named files; no
commits. First data point for PZ1: the v0 lexicon fires on ~10.6% of
operator turns (153/1437), γ-route dominant — next slice is hand-labeling
a hit/miss sample for precision/recall before trusting the
retro-bootstrap. This adjudication is also the first recorded
claimed-vs-verified pair of the γ event stream.

### H2 — PZ1 slice 2: labeling sheet (dispatched 2026-07-11)

**:have** — `labs/M-zaif-harness/pz1-sample-scan.edn` (153 hits, H1-verified);
the evidence API (H1's paging pattern); bb. **:want** — `pz1-labeling-sheet.edn`
(`{:items [{:id :at :kind :hit|:probe :route-claimed :marker :context≤600 :label nil}…]}`):
60 hit items stratified by route proportionally (each firing marker represented
where possible) + 60 probe items sampled uniformly from H1's window with ids
NOT among the hits; plus `pz1-labeling-sheet.md` (readable table). **Acceptance:**
EDN parses; 120 items; every hit id ∈ H1 hits; ≥5 spot-checked probe ids
in-window and not-in-hits via the store; stratification counts verified; writes
confined to the two files; no commits. **Pre-flight:** all five PASS (same
profile as H1 — read-only API + two lab files).

**Adjudication (2026-07-11): FAIL — remediable.** Hits half fully valid
(60 items, 43/9/8 routes, all ids ∈ H1, 18/18 markers, contexts intact).
Probes half REJECTED: **45/60 sampled outside the specified window**
(back to 2026-02), despite the packet's "same window (since 2026-06-01)".
The worker's own summary carried the tell — "48 distinct days" cannot fit
a ~40-day window — and its report was confident anyway. Also one scope
note: a third file (`pz1_build_labeling_sheet.clj`, the build script) was
written against the two-files rule — benign, kept, and the rule is now
amended to allow the build script explicitly. Second consecutive case
where external adjudication caught what the worker's report obscured.
First negative event in the γ stream. Remediation: H2b (regenerate probe
half in-window; hits half retained).

**H2b adjudication (2026-07-11): PASS.** Full re-verification (worker's
"verified locally" given no weight): 120 items; hits half byte-stable
(43/9/8, all ∈ H1, 18/18 markers); probes 60/60 in-window
(2026-06-01T10:41 → 2026-07-10T09:36, 17 days); disjoint from hits; 5/5
store spot-checks VERIFIED. Worker's root-cause account (missing :since
param; fix adds param + defensive local filter + assertion) is consistent
with the observed failure mode. Sheet committed. H3 dispatched: zai-1
labels blind from :context only; claude-5 labels independently; gold pass
to Joe on disagreements + random slice.

Plan to PZ1 close: H3 = independent blind labels (zai-1 by handoff, claude-5
directly), mechanical agreement, disagreements + random slice (~20) to Joe as
the gold pass (operator labels decide; agent labels propose — admissibility
applied to the measurement). Then precision/recall/routing-accuracy and the
PZ1 verdict.

### H3 — PZ1 slice 3: independent blind labels + agreement (2026-07-11)

**H3a (zai-1, by handoff): PASS.** `pz1-labels-zai1.edn` structurally
verified before claude-5 labeled (count 120, ids in sheet order, all
`:correction?` booleans) — per-item values NOT inspected until claude-5's
labels were frozen, preserving mutual blindness. zai-1: 35 corrections
(22 γ / 11 C / 2 actand); only 4/60 probes marked.

**H3b (claude-5, direct): DONE.** Labeled from `:context` only, ignoring
`:marker`/`:route-claimed`; judgment rule recorded in
`pz1_write_claude5_labels.clj` (correction = pushes back on / negates /
redirects something the agent did, claimed, planned, or believed inside
the loop; γ = approach redirect, C = intent-model repair, actand =
world/artifact-state correction). claude-5: 34 corrections (19 γ / 9 C /
6 actand); duplicate docs labeled consistently.

**Mechanical agreement (`pz1_agreement.clj`):** 101/120 (84.2%) on
`:correction?`, Cohen's κ = 0.614; mutual trues 25, route agreement
22/25. The disagreement structure is itself a finding: zai-1 said yes on
10 hits claude-5 judged non-corrections (marker-anchored over-credit:
"revert"/"stale" firing inside expository prose), while claude-5 marked
6 probes zai-1 missed (unmarked corrections — e.g. "G is supposed to be
defined over policies not actions" — exactly the recall blind spot the
probe half exists to expose). Gold pass built: 32 items = 19
correction-disagreements + 3 route-disagreements + 10 seeded-random
agreed calibration items, mixed unmarked in `pz1-gold-sheet.md` (contexts
only — neither agent's labels shown, no markers). Joe's labels decide;
both agents' labels propose. After the gold pass: score both agents +
the raw lexicon against gold → precision/recall/routing-accuracy → PZ1
verdict.

### H4 — PZ1 slice 4: provisional score from agreed labels (dispatched 2026-07-11)

Gold pass still pending with Joe; the agreed 101 items support a provisional
measurement now, with disputed items as bounds. **:have** — the three frozen
label artifacts (`pz1-labeling-sheet.edn`, `pz1-labels-zai1.edn`,
`pz1-labels-claude5.edn`, all positional/id-aligned) + H1 population
constants (N=1,437 operator turns in-window; H=153 hits; NH=1,284 non-hits;
sheet = 60/153 hits route-proportional + 60 probes uniform from non-hits).
**:want** — `pz1_provisional_score.clj` (bb) + `pz1-provisional-score.edn`
computing, for each basis ∈ {zai1, claude5, agreed}: lexicon precision
(true hits / labeled hits; agreed basis restricts the denominator to agreed
items), estimated recall = (precision × 153) / (precision × 153 +
probe-true-rate × 1,284), and routing accuracy (among true hits:
route-claimed == labeled route; agreed basis = mutual-true hits with agreed
route). Plus disputed-all-true / disputed-all-false bounds on the agreed
basis. **Acceptance:** numbers must match claude-5's fully independent
recomputation (script written without reference to the worker's); writes
confined to those two lab files; no commits. **Pre-flight:** all five PASS
(pure mechanical computation over frozen committed inputs; zai-1's own
labels are an input but no judgment is exercised, so self-scoring bias
doesn't arise; blast radius two files).

**Adjudication (2026-07-11): FAIL — remediable.** All three bases and the
upper bound match the adjudicator's pre-locked independent computation
exactly (futon2 `c40cb05`, committed before the worker reported). The
LOWER bound is wrong: the worker re-reported the agreed basis (20/48,
5/53, recall .3448) instead of recomputing all-disputed-false over the
full denominators (correct: precision 20/60 = .3333, probe-rate 5/60 =
.0833, recall 51.0/158.0 = .3228). The worker's own script documents the
reasoning error ("agreed already = lower bound") — assigning disputed
items false places them in the denominators; excluding them does not.
Notable: this time the worker's REPORT was faithful to its artifact (both
carry the same defect) — the failure is in the work, not the reporting.
Fourth adjudication, second FAIL; both FAILs caught by external
recomputation. Remediation: H4b (fix lower bound only; everything else
byte-stable).

**H4b adjudication (2026-07-11): PASS.** Delivered EDN verified against
the adjudicator's pre-locked computation: 14/14 values match to 1e-9
(three bases + upper bound unchanged; lower bound now full-denominator:
precision 20/60 = .3333, probe-rate 5/60 = .0833, recall .3228). Ledger
stands at H1 PASS · H2 FAIL→H2b PASS · H3 PASS · H4 FAIL→H4b PASS.

**PZ1 provisional result (adjudicated; final pending Joe's gold pass):**
lexicon precision .42 agreed-basis (bounds .33–.53); recall .32–.34
agreed-basis (.20 on claude-5's labels, .37 on zai-1's — the gold pass
decides this axis); routing accuracy 1.00 on agreed trues (.87–.90 per
labeler). Reading: the correction lexicon is a high-routing-fidelity,
low-recall, mediocre-precision detector — a cheap γ-stream seeder, not a
measurement instrument. Incidental finding during H4: futon1b JVM heap
OOM stopped XTDB ingestion (node survives, reads fail with
system-time-after-tx); restart + ~60s replay recovered, no data loss;
see futon1b README `da65915`.

## Log

- 2026-07-11 (later still) — **retry PASSED on the original unpinned
  ask** after the limit-pushdown fix was hot-swapped (Drawbridge reload +
  fresh backend instance; no JVM restart, zai-1's session preserved):
  5 joe-authored entries, correctly ordered, coherent types/tags, and a
  correct reading of the null mission target ("nothing is clocked").
  Query verified on the windowed path: include-ephemeral=false&limit=5,
  4.4s. Demo closed green.
- 2026-07-11 (later) — first live demo run + failure analysis (DEMO
  RECORD above); backend limit-pushdown fix committed; U1 promoted to
  prerequisite.
- 2026-07-11 — mission authored (Fable session) from the zaif discussion
  with Joe (recorded in M-text-sidecar §CHECKPOINT 2026-07-11). P2=FTS5
  decided the same day. Nothing dispatched; PZ1 is the car when this
  opens.
