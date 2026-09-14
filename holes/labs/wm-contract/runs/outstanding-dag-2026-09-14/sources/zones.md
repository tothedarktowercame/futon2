# M-capability-zones: a Voronoi partition of Futon City, so learning has ground to stand on

**Type:** Mission
**Status:** INSTANTIATE (S1) — chartered 2026-07-19 (claude-9 draft from the morning-after
design conversation with Joe). Operator scope-pin + dimensional amendment 2026-07-19; S1
belled to codex-10 the same day (spec: `M-capability-zones-S1-handoff.md`). Paper pattern
landed in `p4ng/main-2026.tex` (sibling of R17′′).
**Owner:** claude-9 (ground control); claude-3 holds the owner seat for S1 dispatch/review
(session of 2026-07-19) — authors per slice via the coding-handoff protocol
**Home-repo:** futon0 (cross-repo coordination home, per `single-locus/mission-home` and the
M-capability-star-map precedent; the successor mission to that CLOSED mission)
**Discharges:** `repair-attempt-036-policy-nondiscrimination` and
`repair-attempt-037-artifact-binding-mismatch` (both discharge against slice 2's concrete spec —
attempt-037 proved a vague repair spec cannot be authored; this document is the concrete one)

**Cross-references (read-only intake):**
- `futon0/holes/missions/M-capability-star-map.md` — CLOSED predecessor; proved EFE-over-the-
  capability-graph is trustworthy. This mission gives the graph a *partition* and the partition
  a seat in G.
- `futon5a/holes/missions/M-learning-loop.md` — owns operator-side capability mining (hinge-log →
  capability graph in futon1b, WebArxana render). This mission is its machine-side sibling; it
  consumes the same graph substrate and does not fork the vocabulary.
- `futon3c/src/futon3c/live_efe_map.clj` (M-live-efe-map, ALL LIVE) — the coordinate field
  (BGE mission embeddings `futon3a/resources/notions/bge_mission_embeddings.json` + carpet
  positions `futon6/data/mission-carpet-pos-embed.json`) and the live traffic layer (agent
  saucers, operator rocket with the honest-absence rule).
- `futon2/holes/E-feature-constellation` + `powerbi-tui/skills-expanded-v2.md` §"The research
  programme, computed" — Figure 5: 172 missions retracted to the above-median evidential core;
  node size = enacted magnitude, edge width = summed turn-attestation, zero hand-typed rows.
  **Load-bearingness is already computed there.**
- `futon2/src/futon2/aif/action_proposer.clj` (`gap-actions`), `intrinsic_values.clj`
  (per-class Beta posteriors + `wm-hyperparameter-update` hyperedge persistence),
  `efe.clj` (`:G-efe = risk + ambiguity`), `full_loop_runner.clj` (`selection-discrimination`).
- `M-substrate-metric` (via C-substrate-completion) — **owns the ground metric and node
  granularity. Consume, don't fork**: slice 1 uses plain BGE cosine as the interim distance,
  marked `:interim-metric`; the Wasserstein/Fisher–Rao upgrade lands there, not here.
- `p4ng/main-2026.tex` — catalog pattern **"Capability Zones over the Embedded Landscape
  (staged)"** (sibling of R17′′), landed 2026-07-19. This mission is that pattern's build-out;
  the pattern's **dimensional note is normative here**: operative partition in a 3-D reduction,
  2-D carpet as its projection, high-D nearest-seed as diagnostic only, R9-spirit acceptance
  invariant (accepted object = operative object).
- `futon2/docs/futonzero-alphazero.md` — the honest R2 caveat ("the prior isn't trained from
  outcomes"). The retro-mined corpus this mission produces is the training data R2 was waiting
  for; R2 itself stays in its own lane (futon6 M-differentiable-substrate).

---

## Corpus scope (operator-pinned, Joe 2026-07-19, recorded by claude-3)

- **Construction corpus (S1 harvest):** the FUTON stack — the `~/code/futon*` family
  (17 repos; worktree clones like `futon3c-index-check` excluded). Same landscape whose
  missions feed `futon3c.live-efe-map` and that was fair game for M-capability-star-map.
- **Demonstration stratum (distinct, NOT in the S1 harvest):** `~/code/apm-lean`,
  `~/code/ukrn-services-simulation`, `~/code/mathlib4/DarkTower`, and future work under the
  tithe plan (20% outward bucket, `futon7/holes/M-futon-forward-model.sequencing-v1.md`
  §tithe). Joe: these are "demonstrations of capability moreso than loci for building
  capability." Kept distinct because:
  1. honest coverage — S1's per-repo coverage note stays within the construction corpus;
  2. demonstrations are prime evidence for the DERIVE attachment-mode question — they are
     the edge-attachment cases (the charter's own example, mathematical reasoning, is
     exactly what apm-lean/DarkTower demonstrate);
  3. the Beta-update semantics for demonstration evidence (capability exercised in foreign
     territory) may legitimately differ from construction evidence — that is a design
     point for review when the stratum is harvested, not a hand-tuned constant.
- **Follow-on:** harvest the demonstration stratum after S1 review lands, same machinery,
  items tagged with `:demonstration` provenance in the artifact.

## HEAD

### The question

When the War Machine's leading candidates are all "learn a capability I don't have," how should
it tell them apart — without inventing discrimination it hasn't earned?

### The finding that forced the question (2026-07-19 night)

After four consecutive fully-gated groundings (attempts 032–035), attempt-036's top-5 were all
`:learn-action-class` candidates at bit-identical `:G-efe 17.470403…`, and the
`selection-discrimination` tripwire refused to pick arbitrarily. Diagnosis (verified live): the
14 gap-classes' intrinsic-value posteriors have **zero recorded updates** — all at the
Beta(1,1) prior. The identical G was *epistemically honest ignorance*: the machine has never
observed anything about any capability gap. Both the estimator and the tripwire behaved
correctly. What's missing is the loop that lets evidence accumulate — and the ground it would
accumulate over. Attempt-037 then proved that a repair without a concrete spec cannot be
authored (`:artifact-binding-mismatch`; same signature as 014/021 — spec-vagueness, not
pinning, is the variable).

### The three principles (from the design conversation, 2026-07-19)

1. **Stigmergic, not biographical (Joe: "I've simply been the ant building this anthill").**
   Capabilities live in the built structure, not in agent biographies. No agent-resolved
   attribution; a capability exists where the structure bears load through it. The months of
   git history are described as *which load paths got built*, never as "who learned what."
2. **Earned, not invented.** Discrimination between gap classes must come from evidence
   (retro-mined history, live traffic, recorded outcomes) flowing through the *existing*
   posterior machinery. The tripwire is untouched; it starts passing when the posteriors
   genuinely separate. No hand-tuned per-class constants, ever.
3. **Morphogenetic attachment; hyper-prior recovered empirically.** Sometimes new capability
   attaches preferentially to existing mass (futon1→1b XTDB, ants→R-numbers→War Machine; the
   Self-Improving Loops and Live Invariants clusters); sometimes at the edge (mathematical
   reasoning; "the JVM into a ClojureVM", built the night before this charter). Neither mode is
   preferred a priori. The Gypsy-Jazz/novelty dial is NOT a slice-1 or slice-2 parameter — it
   is a DERIVE-phase question answered from history (see §Derive), defaulting to neutral until
   then.

### The mechanism in one paragraph

Embed each action-class into the same BGE space that Futon City already lives in (seed = the
class's text now; the centroid of its exercised commits once mined — seeds drift with the
anthill). The Voronoi partition over those seeds assigns every mission, feature, commit, and
live agent position a **zone** — the **operative membership computed in a 3-D reduction of the
embedding space** (amended 2026-07-19, per the paper pattern's dimensional note — see
cross-refs: print-flat 2-D silently misfiles boundary features through adjacency distortion;
raw high-D suffers distance concentration, leaving every nearest-seed margin uniformly thin and
the partition uninspectable; three dimensions keep the Voronoi cells compact and the
tessellation walkable), rendered on the 2D carpet as a projection *from the 3-D partition*,
with near-boundary margins reported honestly as mixed. The high-dimensional nearest-seed
reading is retained **only as a boundary-distortion diagnostic**. The load-bearing reason is an
acceptance invariant in the spirit of R9: **the partition the operator visually accepts (S1.5)
is the same object the machine's preferences run on (S2)** — the machine must operate in a
space a human can walk. Then three signals per zone, all read from surfaces that
already exist: **load** (enacted magnitude + attestation summed over the zone — the Figure-5
quantities), **demand** (live-map traffic: saucer/rocket time-in-zone, with clock-lineage for
history), and **posterior variance** (the per-class Beta posteriors, finally fed). G's prior
preference reads load, its ambiguity leg reads variance, demand weights both. A gap class whose
zone holds heavy mass or traffic but no machine capability is the learning target — and the
036 tripwire passes because the zones genuinely differ.

## Slices

### S1 — seeds, membership, and the retro harvest
- Embed the full action-class vocabulary (all classes, not only the current 14 gaps) into BGE
  space; `zone-of` membership fn (high-dim cosine, nearest seed, margin reported).
- Retro-mine the git corpus into capability terms: map commits → action-classes exercised
  (three grains, best-first: attempt items + feature cards where they exist; mission docs +
  pattern references; raw commit paths/messages for the long tail). Write
  `wm-hyperparameter-update` records through the existing `intrinsic_values` machinery — the
  posteriors leave Beta(1,1) on real observation counts.
- Zone seeds recomputed as evidence centroids after the harvest; drift is expected and logged.
- **Acceptance:** posteriors show non-uniform counts across ≥ half the vocabulary; `credit-for`
  returns non-default values; a written honest-coverage note stating what fraction of history
  resisted description (no silent caps).
- *Amendment (2026-07-19, dimensional note):* S1's high-D `zone-of` is **diagnostic-grade**;
  harvest assignments made in raw BGE space are **provisional** and are re-computed in the
  accepted 3-D reduction before any deposit to the live posterior store (the S1 handoff's
  artifact-only deposit gate already enforces this ordering — nothing live is touched until
  after S1.5). Belled to codex-10 2026-07-19 (`invoke-1784453722209-864-0b5d57a9`) under the
  pre-amendment reading; the deliverables survive unchanged, only their finality labels shift.

### S1.5 — the partition made visible before it is used
- Build the **3-D reduction** of the BGE space (deterministic, versioned; method recorded —
  this is a *projection for operability*, distinct from the ground metric `:held` on
  M-substrate-metric). Operative `zone-of` runs in this space.
- Re-assign the S1 harvest in the 3-D partition; the high-D vs 3-D **disagreement set is part
  of the acceptance material** (it is the boundary-distortion diagnostic made concrete).
- Render the zones on the live map (WebArxana layer beside the existing saucers/rocket) — the
  2-D carpet layer is a projection *of the 3-D partition*, not an independent 2-D tessellation.
- *Surface pin (Joe, 2026-07-19):* S1.5 ships the **2-D projection** — that is the acceptance
  surface for today. Navigable 3-D exploration of Futon City (using the `~/vsat/` tooling) is
  a **reach goal, held** for a later excursion; reasonable, but not what is needed to get the
  War Machine running again. Priority order: S1.5 acceptance → S2 discharge of 036/037.
- **Operator visual acceptance is the gate** (Field Desk principle: feature-acceptance before
  machine use). Joe walks the zones; boundary complaints are fixed or recorded as accepted
  distortion BEFORE slice 2 wires anything into G.
- On acceptance, the partition object (seeds + reduction parameters + version) is **frozen**;
  the posterior deposit (re-assigned records) happens against this accepted object.

### S2 — a seat in G (discharges 036/037)
- **Reads only the S1.5-accepted partition object, version-pinned** (R9-spirit invariant: the
  accepted object IS the operative object; no recomputation drift between what Joe walked and
  what G reads).
- Prior preference: zone load enters C for `:learn-action-class` candidates.
- Ambiguity: per-class posterior variance replaces the class-flat term.
- Demand: zone traffic weight (live + lineage-derived).
- Invariants: `selection-discrimination` untouched; `:G-core = risk + ambiguity` untouched
  (new terms enter through the existing augmentation/preference channels with named keys, per
  the B-2a score-provenance discipline); retries=0-style regression — with an empty posterior
  store, behavior is byte-identical to today.
  - *Superseded in part (2026-07-21, S2a review):* the Native-Currency Discrimination
    pattern's degradation clause replaces byte-identity-at-empty-store for learn-actions
    with **refusal-equivalence** (uniform predictive variance 0.25, zero C shift, tripwire
    refuses exactly as 036); non-learn actions remain byte-identical (tested).
- **Acceptance:** a replayed attempt-036 selection discriminates (distinct-G ≥ 2 among top-5)
  *purely from harvested evidence*; both repair obligations discharge through a normal
  full-loop click with card + executed review + grounding.

### Walk verdict (Joe, 2026-07-19 evening)
- **Accepted for unsticking.** "Getting it unstuck is the key thing… that's fine, as long as
  we are getting distinct numbers out." Distinct numbers verified: 12/12 gap classes separate
  on in-memory replay (0.9375–0.9992). The partition object is **frozen as `pca3-v1`** +
  `harvest-2026-07-19-3d.edn` per the R9-spirit invariant.
- **Boundary questions recorded as accepted distortion pending refinement** (not blockers):
  no-op zone holds 51/269 missions, 25 of them high-D disagreements; disagreement set is
  79% at commit grain. UI complaints (disagreement toggle, guidance note) fixed same evening
  (futon6 `7d62d61`).
- **037 resolved by this mission's existence** (operator judgment): the concrete spec that
  attempt-037 proved unauthorable now exists and carried two slices. Formal discharge of
  036/037 still goes through the replayed-036 full-loop click.
- **Follow-up mission required (Joe):** refine the scores *as the machine runs* — the S2
  load/demand channels and score-shape refinements belong there; this mission's remaining
  critical path is deposit → replay-036 → discharge. Winding the machine back to 036 and
  running from there is acceptable.
- **Machine start: NOT yet armed.** Joe: "I'm not prepared to start it yet, but we could
  sanity check that it's ready to go soon." Deposit and arming remain Joe-gated.

### Meta-pattern layer (Joe idea, 2026-07-19 late — charter material for the follow-up mission)
Seed a **second, layered partition** from the paper's R-catalog patterns (`p4ng/main-2026.tex`)
as representative meta-patterns: each WM feature organizes (and competes for) the patterns and
work in its surrounding region. Rationale (Joe): the 14 action-classes are potentially ad hoc;
the catalog is well-theorised and definitively represents the WM's capabilities, largely
reflective of the stack's. Design constraints agreed in conversation:
- **Layer, don't replace.** The action-class partition keys the R12 posteriors and the 036
  unstick path — untouched. The meta-pattern zones bridge via a per-zone *distribution over
  action-classes* (shared vocabulary, no fork).
- Catalog prose = seed texts (fixes the "Exercise X by…" seed-template bias).
- Long game: the action vocabulary itself becomes learnable — R17/BMR merges/splits of
  action classes proposed from pattern-grounded evidence.
- Arrives as a **versioned v2+ generation** with its own operator walk; `pca3-v1` stays frozen.
- **First probe (cheap, no state change):** embed the catalog patterns, transform through the
  existing pca3-v1 matrix, overlay on the map — see which zones the paper's patterns claim.

### Steady-state (post-S2 design note, Joe 2026-07-19 — "refine without the big embed")
The 5-hour S1 embedding was one-time backfill, not a recurring cost. The continuous path:
- **Trickle embedding:** new commits (tens/day) embed in seconds; ride `daily_reembed.sh`.
  Prereq fix: re-key the prepare checkpoints by content/sha instead of chunk position, so
  appends don't invalidate earlier chunks.
- **Live evidence needs no embedding:** once S2 is wired, enacted attempts feed the
  posteriors through the normal R12 `wm-hyperparameter-update` loop in real time — the
  retro harvest is the bootstrap, not the diet.
- **Seeds/reduction:** centroids are streaming means; PCA refit is seconds. Full re-embeds
  recur only on embedding-model change (rare; GPU pass ≈ 1 min if needed).
- **Drift vs the R9-spirit pin:** the accepted partition is version-frozen, so refinement
  lands as *discrete accepted generations* — background drift, then a short operator
  re-walk and a new pinned version when drift crosses a visible threshold. No silent
  mutation under G. The lumpy v1 partition is acceptable precisely because of this path.

- **Hypergraph route (Joe, 2026-07-19 evening):** term/hypergraph text indexing
  (XTDB#5637 lane; demo impls on-laptop: `futon1bi/src/futon1bi/text_index.clj`,
  futon1b `textprobe_*` + `fts_oracle.clj`) is the witness-bearing complement to
  embedding classification — postings are auditable term-by-term (R9-friendly) and
  incremental at millisecond cost. Candidate demo: classify the 1,918 resisted items
  and cross-check index-vs-embedding agreement on the confident 4,892 (disagreement =
  a third diagnostic stratum). Lives in the 5637 lane as its demo corpus, not as an
  obligation here; Joe-gated there (D2 packet).

### DERIVE (later, empirical — not blocking)
- The attachment-mode hyper-prior: for each historical capability emergence, measure its birth
  adjacency (attached-to-mass vs edge) and correlate with subsequent load-bearingness. If a
  circumstance-conditional pattern exists, it becomes the learned dial; until then the residual
  ant-preference stays neutral. (This is FutonZero R2's corpus doing double duty.)

## Sorries / held elsewhere
- Ground metric + granularity: `:held` on M-substrate-metric (interim = BGE cosine, named).
- R2 (train the prior from outcomes): stays in futon6; this mission only produces the corpus.
- Operator-side capability mining: stays in M-learning-loop; shared vocabulary, no fork.

---

### Checkpoint 1 — 2026-07-19 (S1 landed)
**What was done:** S1 belled to codex-10; job killed at the 30-min Agency cap mid-embedding;
claude-3 reviewed the on-disk delivery per the handoff review gate, fixed findings directly
(checkpointed batch embedding + honest text cap in `capability_zones_prepare.py`; repo-key
type fix in the coverage table), re-ran the compute under systemd-run (~5 h on CPU), ran the
harvest, and landed everything: futon2 `56df8e5` (NOTE: on branch `M-propagators-ant-gate`),
futon0 `6c11b2b`.
**Numbers:** 6810 items; 4892 assigned / 1918 resisted (28%, margin < 0.01 in raw BGE space —
provisional per the dimensional amendment); 101 monthly Beta records; 11/14 classes with
evidence (zero: `open-mission`, `survey`, `pursue`); seed drift 0.26–0.34 cosine; live :7073
store untouched (108 `wm-hyperparameter-update` hyperedges before and after — deposit waits
for S1.5 re-assignment in the accepted 3-D partition).
**Test state:** 3 tests, 13 assertions, 0 failures; clj-kondo 0/0; check-parens OK.
**Next:** S1.5 — build the 3-D reduction, re-assign the harvest, render the 2-D projection
layer, Joe walks and accepts (disagreement set included in acceptance material).

### Checkpoint 2 — 2026-07-19 (S1.5 built and reviewed; awaiting Joe's walk)
**What was done:** codex-10 delivered S1.5 in-window (futon2 `9a57a61`, futon3c `fb7c31a`,
futon6 `22aacfc` — the maintained frontend is `mission_efe_field.py`, hence the extra repo —
futon0 `953d2d2`); claude-3 ran the full review gate: all four diffs read, gates re-run
(futon2 5 tests/28 assertions, futon3c 1/6, kondo 0/0 both, parens OK), determinism verified
independently (transform + zone-of-3d double-call; codex's byte-identical refit sha),
3-D artifact replayed in-memory (134 records, 14/14 classes non-default), store count read
directly (:7073 = 108 exact, unchanged — closing the one gap codex reported honestly when the
store timed out under load), screenshot + legend/toggle inspected.
**Numbers:** pca3-v1 explains 16.0% of variance (8.7/4.5/2.9); re-assignment: 6810 items,
6130 assigned, 680 mixed (p10-of-margins threshold — marks exactly 10% by construction),
**all 14 classes have evidence** (134 records); **disagreement set = 5401/6810 (79%)** vs the
high-D diagnostic, top flows apply-cascade→survey (804), apply-cascade→advance-mission (584),
advance-mission→survey (362). Mission-level zone census (269 rendered): survey 82, no-op 51,
close 39, pursue 23, close-hole 20, apply-cascade 19…
**Test state:** 6 tests, 34 assertions, 0 failures across futon2+futon3c.
**Next:** Joe walks the zones (toggle on the mission-efe-field map; acceptance doc:
`M-capability-zones-S1.5-acceptance.md`). Walk questions on record: is the 3-D partition
semantically walkable at 16% variance, or does v2 (UMAP-3) get built? Is the no-op zone
holding 51/269 missions meaningful or a seed artifact? Deposit stays locked until acceptance.

### Checkpoint 3 — 2026-07-21 (deposit live; first clicks; contract-shape finding)
**What was done:** Joe accepted-for-unsticking and commissioned up to 5 clicks. Deposit
executed (store 108→242; 12/12 gap classes distinct). Attempt-038 selected the open 036
repair via stop-line preemption (correct), passed selection/construction, then was
operator-interrupted (agent reap of codex-8/9; hot-swap to codex-1/2). zai-4's orphaned
authoring produced futon2 `63fcdb3` — a proposer change deduplicating tied gap-actions —
which FAILED owner review (invents discrimination at the uniform prior via alphabetical
representative; shadows omitted classes from evidence; violates principle 2) and was
REVERTED (`909c2ac`, tests green 7/16). Attempt-039 `:build-failed` (`:not-reached-build`).
**Finding (the predicted new failure mode):** the repair discharge contract requires
`:distinct-repair-commit` bound in-attempt — it assumes repairs are code-shaped. THIS
repair was data-shaped (the deposit); its commits predate any attempt, so authors either
fail to bind or manufacture code changes. The contract shape, not the machine, is the gap.
**Test state:** proposer 7/16 green post-revert; machine PAUSED (unit stopped) pending
Joe's direction on discharge path.
**Next:** discharge 036/037 honestly — candidate paths: (a) operator-lane discharge citing
the mission's deposit commits + a replayed discriminating selection as grounding; (b) an
in-attempt commit that *documents* the data repair (bindable, honest) + normal grounding;
then the production-shaped successor click shows live discrimination. Contract-shape fix
(data-shaped repairs) belongs to the follow-up mission.

### Checkpoint 4 — 2026-07-21 morning (the tripwire is measuring the right thing)
**What was done:** OOM recovery; rehydrate-on-startup defect found and fixed (futon2
`662d159` — no runner code ever called the documented bootstrap-replay); successor cohort
`wm-outer-loop-41-v1` preregistered + activated (target 10; validator generalized `feecd36`);
attempt-id global-uniqueness defect found and fixed (`94b1cf2` — cohort-41's attempt-001
collided with cohort-40's in the global morning-brief store); crew woken by whistle;
obligations 036/037/039 + initialization carry implementation records (`:awaiting-validation`).
**The finding (attempt-041):** with posteriors FED and DISTINCT in the candidates
(iv 0.9615–0.9992 visible in the ranked field), `:G-efe` for gap classes is still
bit-identical — intrinsic credit enters only `:controller-score` via effective-risk
(B-2a purity), and `selection-discrimination` reads the CORE, by design ("must not hide a
flat estimator"). The estimator core IS still flat: the deposit was necessary but not
sufficient. The tripwire has now correctly refused twice for two different reasons.
The principled fix is exactly S2's ambiguity item: per-class posterior variance in the
ambiguity leg — which separates the core itself, leaving the tripwire untouched.
**Open design question (needs ARGUE before code, Joe's verdict):** the SIGN. Naive
variance-into-ambiguity makes unexplored classes HIGH-G (repulsive) — anti-epistemic.
And R17′ quarantines posterior spread OUTSIDE G-efe as a named bonus (not EIG); whether
R12 per-class Beta variance is "the ambiguity of the learn-action outcome" (belongs in
the leg) or "posterior spread" (must stay a named augmentation) is a live question the
paper's own honesty rules force us to answer, not assume.
**State:** machine paused; `repair-attempt-041-policy-nondiscrimination` open (blocking,
honestly); 036/037/039 await a grounded production-shaped click for `:resolved`.

### Checkpoint 5 — 2026-07-21 (RESOLVED: the arc closes)
**What was done:** S2a (native-currency legs, futon2 `b3b0e47`, codex-1) and S2b (contract
artifact shapes, `03d341d`, codex-2) and S3 (readiness-that-wakes, `200a850c`, codex-1) all
landed, owner-reviewed, gates green. Pattern "Native-Currency Discrimination (the R17′–R12
edge)" published (p4ng `718e7a3`); R16 + R9 revised against the recorded failure aggregate
(`8827c85`). Validation clicks in cohort wm-outer-loop-41-v1: production selections passed
`selection-discrimination` on the fed core (distinct-g 4) in attempts 042–044;
**attempt-046 grounded** (repair of 043, author codex-1, reviewer codex-2, commit `9210242`);
the runner auto-validated the canary obligation, and
**036, 037, 039, 041, and the initialization obligation are all `:resolved`** against
attempt-046 as the production-shaped successor.
**The through-line:** attempt-036 refused because nothing was known; the harvest described
six months of history; the deposit fed the posteriors; attempt-041 refused because credits
aren't the core; the pattern put the evidence in the core's own legs; and the tripwire —
untouched throughout — passed when, and only when, the estimator had earned it.
**Still open (normal machine life, not this mission):** 044-artifact-binding-mismatch,
047-review-execution-evidence-missing, 043 awaiting validation; 10 legacy
awaiting-validation obligations continue draining one per grounded click.
**Test state:** futon2 aif suite 677 tests / 4492 assertions, 0 failures (S3 run).
**Next:** S1.5 remains formally acceptable-as-accepted (walk verdict recorded); the
follow-up mission (score refinement as the machine runs; load/demand channels;
data/spec failure-kind mapping for contracts; meta-pattern layer) awaits its charter.
