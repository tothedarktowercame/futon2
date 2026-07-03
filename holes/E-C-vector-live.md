# Excursion: a LIVE C-vector — the WM's preferences, kept current (E-C-vector-live)

**Date:** 2026-06-25 · **Status:** DELIVERED through §12 (live C §9 · predictive risk §10 · durable-join steps 1–4 + reconcile §11–12); full PROOF-store promotion stays D4-gated.
**Authored by:** claude-2 (scoping only — not the driver).
**Parent / relates:**
[[M-goals-and-holes]] (the backward/goal dual — this excursion *delivers* the live C that its R19 / C-vector needs; it does NOT own the R19 contract text) ·
[[M-operational-vocabulary]] (the forward dual — methods/policy; its box run produced the one-time mined C-entries this excursion's snapshot step reads) ·
[[M-aif-wiring]] (R5 risk · R14 γ · R15 hierarchy · the **belly** / venter this fills) ·
[[M-populate-substrate-2]] (the now-LIVE bitemporal substrate the goals/holes live in — and the source of the **freshness-guard discipline** below) ·
[[E-mine-mission-transitions]].
**Repos:** futon2 (`aif/preferences.clj` — the static C to make live; `aif/efe.clj` — `compute-efe`/`:G-risk` the consumer; `aif-wiring-explainer.html` — the belly) · futon1a (`:7071` substrate-2 — the `:sorry` corpus + goal/hole entities) · futon0 ([[M-capability-star-map]] — the goal hierarchy) · futon7 ([[C-pudding-prover]] — the discharge standard).
**Lit anchors (the C-vector):** Da Costa, Parr, Sajid, Veselic, Neacsu & Friston, *Active inference on discrete state-spaces: a synthesis* (J. Math. Psych. 2020) — A/B/**C**/D/E + the risk/ambiguity split; Friston et al., *Active inference: a process theory* (2017); Parr, Pezzulo & Friston, *Active Inference* (MIT 2022); Smith, Friston & Whyte, *A step-by-step tutorial on active inference* (2022); *Prior preferences in active inference agents: soft, hard, and goal shaping* (arXiv 2512.03293) — **how** C is set.

---

## HEAD

The War Machine's preference component **C** — Friston's prior preferences over outcomes, the "belly" feeding EFE's risk — exists today only as a **static, hand-set `def`** (`futon2.aif.preferences/preferences`, a map of per-channel preference ranges that `efe/compute-efe` reads as `:G-risk`). M-goals-and-holes diagnosed this: C is a *silent miss / mislabel* (a canonical generative-model matrix present-but-frozen as hyperparameters). The forward+backward mining runs on Linode produced C-*entries*, but a one-time assembly of them is a **snapshot** — and Joe's requirement is explicit: **the C-vector must be LIVE.**

Goals and holes change continuously — sorries open and close, missions complete, the capability-star-map grows, `hole/latent` negative space gets named. A snapshot C means the WM computes risk against **stale setpoints** — it builds toward what we valued *then*, not now. With C ignored or stale, `G(π)` collapses toward pure information-gain: the agent explores but **builds toward nothing** (the "hand with no belly").

### The question

**What makes the C-vector live** — continuously derived from the current goal/hole corpus and kept fresh — and wired into `efe`'s risk so the WM's preferences track present goals, not a frozen mining snapshot?

### The hard-won discipline this excursion inherits (read first)

This session, substrate-2's code/history layer **silently froze for 5 weeks** because a live model had **no freshness alarm** — nobody noticed the data had stopped tracking reality (M-populate-substrate-2 D0/D7a). **The C-vector is the same shape of object: a derived view that must track a moving source.** So: **build the live C WITH a freshness guard from day one** — C is stale the moment the goal/hole corpus changes after C was last derived; an alarm (the D7a `invariant-queue-freshness`/`futon3c.watcher.freshness` pattern: derived-stale-vs-source) must say so loudly. Do not ship a live-C that can silently freeze. This is non-negotiable and is an exit condition.

---

## 1. IDENTIFY — the gap

- **C is static + mislabeled.** `aif/preferences.clj` is a hand-authored `def`; `efe.clj:296` reads `(get pref/preferences ch)` per channel for `:G-risk` (gap-from-preference on the predicted mean). Nothing derives it from goals; nothing updates it.
- **The mining gave a snapshot, not a setpoint.** M-operational-vocabulary's run characterized C-entries; useful for shape, but frozen.
- **Goals/holes are the real C.** M-goals-and-holes' `hole/{stated,incompleteness,latent,mess}` family ARE the preferred-outcome entries (and `hole/latent` = C's negative space). They live in substrate-2 (now live + bitemporal) + the star-map + the sorry corpus (293 entities; 110 real per claude-1's audit).
- **Cost of staleness is precise:** `risk = KL(predicted-outcomes ‖ C)`; a stale C measures distance to the wrong target. The WM accrues capability without building toward currently-valued things.

## 2. MAP — survey (facts to gather first; don't design yet)

- Read `aif/preferences.clj` (current C shape: which channels, what ranges) and `aif/efe.clj` `compute-efe` / the risk term (exactly how C enters; per-channel range → `max(0, gap)`).
- Read M-goals-and-holes (the hole family + completion criteria) and its `goals-holes-readiness.html`.
- Inventory the goal/hole sources: `:sorry` entities (`:7071 entities/latest?type=sorry`; use the new **`/api/alpha/census`** count endpoint for populations, not a timeout-prone scan), the star-map graph, devmaps, Mission-Control coverage/tensions, the mined C-entries from the box run.
- How does the WM observe? The 13–14 channel observation schema (`aif/observation.clj`) — C is a preference over *these* outcome modalities, so the goal/hole→C mapping must land on these channels.

## 3. The snapshot-first scaffold (Joe's "get a sense")

Step one is legitimately a **one-time belly assembler**: assemble C once from the mined C-entries + current goal/hole corpus, print the per-channel preference it implies. This is the IDENTIFY/MAP scaffold — it shows the *shape* of a derived C and validates the goal/hole→channel mapping **before** building the live loop. Flag it loudly as a throwaway snapshot, not the deliverable.

## 4. DERIVE — the open design (the driver owns this; framed, not decided)

1. **C representation** — keep the per-channel preference-range shape `efe` already consumes (least wiring), or a richer log-preference distribution? (Lit: C is log-preferences over outcomes; `efe` currently uses ranges.)
2. **goal/hole → preference mapping** — how a `hole/stated` (a sorry / star-map goal) becomes a preferred-outcome entry on the observation channels. `hole/latent` (negative space) = the interesting case.
3. **Live-update trigger** — re-derive C when the goal/hole corpus changes. Reuse the substrate-2 watcher's drainer/probe pattern (debounced, off-cycle) rather than recomputing per WM tick.
4. **Freshness guard** (mandatory, §HEAD) — a `futon3c.watcher.freshness`-style check: C stale iff the goal/hole corpus changed after C's last derivation; loud on transition.
5. **`efe` wiring** — make `compute-efe` read the live-derived C instead of the static `def` (ideally a 1-line indirection: `pref/current-C` fn over the live structure, default-falling-back to the static map so the change is regression-safe).

## 5. ARGUE — decisions to ratify

- **Live, with snapshot as scaffold** (not snapshot-as-deliverable) — Joe's call, ratified.
- **Derive-and-cache, not recompute-per-tick** — the WM tick reads a maintained C; derivation is off-cycle (watcher/probe), so the hot path stays cheap (mirrors why commit-ingest is decoupled — D0.2).
- **Augment, don't rip out** — keep the static `preferences` as the fallback/floor so a derivation failure degrades to the current behavior, never to an empty belly (an empty C ⇒ G = pure info-gain ⇒ builds toward nothing).
- **R19 stays with M-goals-and-holes** — this excursion delivers the live-C *mechanism*; the contract criterion R19 (and the A/B/C/D/E systematic diff) is M-goals-and-holes' to formalize. Don't edit M-aif-wiring's R-contract here; feed it.

## 6. Exit conditions (testable; provisional until ARGUE)

1. C is **derived** from the current goal/hole corpus (not a static `def`), landing on the observation channels.
2. C **updates** when the corpus changes (a sorry closes / a goal is added → C shifts), off the WM hot path.
3. **`efe`'s `:G-risk` reads the live C** — demonstrated: a goal/hole change shifts a preference, which shifts `G-risk`, which **re-ranks a policy** (the belly actually steering selection).
4. A **freshness guard** flags a stale C loudly (the D7a discipline) — verified by simulating a corpus change without re-derivation.
5. Degrades safely: derivation failure → falls back to the static preference floor, never an empty C.

## 7. Scope-out (named, not hidden)

Forward method-mining (M-operational-vocabulary); the substrate-2 PROOF store / forward↔backward join (M-populate-substrate-2 D4); arming/acting on the resulting EFE (WM-I4 / M-aif-wiring Car-3, gated); the R19 contract text + the full A/B/C/D/E faithfulness diff (M-goals-and-holes). This excursion is **only** the live, fresh, wired C — the belly that beats.

## 8. Disciplines

Never restart the JVM; reload via Drawbridge `load-file`. Use `/api/alpha/census` (per-type, count-pushdown) for substrate-2 populations — never a high-limit `?type=` scan (it times out; that footgun is why the census endpoint exists). Evidence-first (specific counts/files). Gates per AGENTS.md (clj-kondo, check-parens, tests). **Ship the freshness guard with the live C, not after** — §HEAD.

> **NB (claude-10, 2026-06-26):** the `/api/alpha/census` route on the running :7071 returned `:not-found` (the JVM predates it — a live freshness smell, logged not fixed; never restarted). The deliverable therefore reads populations via the working `entities/latest?type=…` endpoint, exactly as `c_vector.bb` does.

---

## 9. DERIVE + INSTANTIATE — the live mechanism (claude-10, 2026-06-26; driven by Joe directly, Codex on vacation)

**Decisions ratified by Joe (the §4/§5 open forks):**
1. **Goal-outcome risk = a separate additive term**, not folded into the channel risk — *"fine as long as we don't forget predictive-risk later."* So: STATIC risk now (distance of the current corpus from C); the **predictive** term (KL of a policy's predicted goal-outcomes ‖ C — the W1 join) is a **named, un-removable seam** (`goal-outcome-risk` :TODO), not a silent drop.
2. **The live C lives in an atom** (`futon2.aif.c-vector/c-state`).

**Delivered (all in futon2):**
- `src/futon2/aif/c_vector.clj` (new) — derives the **stated** channel LIVE from :7071 (caps not-yet-attested + clean open sorries, kept shape-identical to `c_vector.bb/produce-stated`); atom-backed `c-state`; `refresh!`/`maybe-refresh!` (off-cycle, derive-and-cache — never the WM tick); `corpus-signature-of` + `stale?`/`freshness-check` (the mandatory guard, loud on *err*; injectable arity for hermetic tests); `goal-outcome-risk` (mean-normalised, W4); `merge-entries` (the extension point for the mess/incompleteness/應-voice overlays — *not* duplicated here).
- `aif/preferences.clj` — `current-C` indirection seam (returns the static map today; channel-liveness can override without touching consumers).
- `aif/free_energy.clj` + `aif/efe.clj` (`survival-cost`) — read preferences through `current-C` (behaviour-identical floor).
- `aif/efe.clj` `compute-efe` — additive `:G-goal-outcome` term reading the live C (`[]` ⇒ 0.0 ⇒ reduces exactly to the pre-change behaviour); `:goal-outcome-weight` / `:goal-outcome-entries` opts.
- `test/futon2/aif/c_vector_test.clj` (new) — 8 tests / 34 assertions mapping to §6 exits.

**Exit conditions — met (verified, auditable):**
1. **Derived, not static** ✓ — live smoke: `refresh!` produced **139** C-entries from :7071 (31 caps, 293 sorries).
2. **Updates off the hot path** ✓ — `maybe-refresh!` re-derives only when `stale?`; the WM tick reads the atom only.
3. **efe's risk reads the live C** ✓ — live `:G-goal-outcome = 0.3396` on a no-op; hermetic test shows satisfying a goal lowers the term and re-ranks heavy-vs-light. *(Per-policy re-ranking in full is the predictive W1 extension; the wiring that turns a goal-outcome difference into a re-rank is done + tested.)*
4. **Freshness guard fires loudly** ✓ — hermetic + live: fresh right after derive, `fresh?=false`/`stale?=true` after a simulated corpus drift, warning emitted to *err*.
5. **Degrades safely to the floor** ✓ — empty/unreachable corpus ⇒ `[]` ⇒ `G-goal-outcome 0.0` ⇒ identical to pre-change EFE; `refresh!` never clobbers a good belly to empty.

**Gates:** clj-kondo 0 errors (1 pre-existing warning in `infer-mode`, untouched); check-parens OK on all changed files; `clojure -X:test` — c-vector 8/34 pass, regression on efe/free-energy/preferences 45 tests / 153 assertions, 0 failures.

**Known follow-ups (named, not hidden):** (a) ~~the **predictive-risk** seam~~ → **landed, see §10**; (b) **unify** the two `produce-stated` implementations (futon2 ns vs `c_vector.bb`) — deliberate duplication today so futon2 needs no futon6 files; (c) fold the **mess / incompleteness / 應-voice** channels via `merge-entries` over the `c_vector.bb` overlays; (d) a cheaper freshness probe once the `/census` route is live on :7071.

## 10. Predictive-risk — the action-dependent term (claude-10, 2026-06-26; Joe: "push ahead")

The static term (§9) is a constant offset across policies, so it cannot re-rank. **Predictive-risk** = `risk = divergence(π's PREDICTED goal-outcomes ‖ C)` — the canonical EFE term — *is* action-dependent and re-ranks. Built in its **in-memory** form (the durable PROOF store stays M-populate-substrate-2 D4's):

- **The discharge link, reusing existing structure** (no parallel mechanism): an action's *advanced outcome ids* = its `:target` ∪ (for `:open-mission`) the star-map mission's `:produces` caps ∪ an explicit `:advances-outcomes` override seam. `c-vector/advanced-outcome-ids`.
- **`predictive-goal-outcome-risk`** — entries the action advances are predicted satisfied (a deterministic point-mass forward step) ⇒ contribute 0; the **denominator stays the current open-count** (fixed), so advancing a goal can only *lower* risk, never raise it via a shrinking mean. Reduces to the static term exactly when the action advances nothing (`:no-op`).
- **`efe/compute-efe`** now uses the predictive form (passing `action` + `capability-graph`); `:no-op` → identical to before, so all prior tests hold.

**Verified:** clj-kondo 0/0, check-parens OK; tests now **56 / 203 assertions, 0 failures** (3 new predictive tests incl. a through-`compute-efe` per-policy re-rank). **Live:** against the real 139-entry belly, `risk(:no-op)=0.33957` vs `risk(pursue ai-passes-prelims)=0.33525` → the goal-advancing policy is preferred (re-ranks ✓).

**Still deferred (named):** the durable `discharged-by` PROOF join (M-populate-substrate-2 D4) replacing the in-memory id-match; a **probabilistic** predicted outcome (advance-with-probability-p — the full KL) rather than the point-mass flip.

## 11. R19-PROOF-JOIN — breakdown (the durable discharged-by join)

Predictive risk (§10) uses an **in-memory** id-match (action `:target` ↔ C-entry `:outcome-ref`). The durable form — a persisted goals↔methods join the forward model reads to predict goal-progress under a policy — is **specified here, gated on M-populate-substrate-2 D4** (the substrate-2 PROOF layer) + M-operational-vocabulary (the mined method arrows). Decomposed into steps:

1. **Persist C-entries** — promote the sim-only `c-store-overlay.edn` (452 `:c-entry` entities) to :7071 as first-class entities. *(Gated: M-populate-substrate-2 promotion path, D4.)*
2. **Persist `discharged-by`** — each C-entry already carries `:discharged-by` (e.g. `:centre-mess`, `:adopt-redirect`); write it as a substrate-2 **relation** `c-entry --discharged-by--> method`. *(The empty `:closes`/`:depends-on-sorry`/… relation family in the substrate-2 explainer is exactly this layer.)*
3. **Method arrows** — join `discharged-by` to M-operational-vocabulary's mined forward methods (the `meme.arrow` constructs) so a method/mission is a queryable node the join can reach.
4. **Forward-model read** — replace `advanced-outcome-ids`' in-memory token-match with a query over the persisted join: "which C-entries does this policy's method discharge?" → the predictive set, now durable + promotable + auditable.
5. **Reconcile** — the join is the bidirectional bridge: backward (goals/holes, M-goals-and-holes) ↔ forward (methods, M-operational-vocabulary). This is the substrate-2 PROOF store's reason for being.

**Dependencies (why it's not built here):** D4's promotion path + the two owning missions must land first; building the join before them would invent relation content this excursion explicitly scoped out (§7). The in-memory match (§10) is the working stand-in until then.

> **STEPS 1+2 (+2b) DONE — 2026-07-02, Joe-ratified (claude-11).** `futon2/scripts/promote_c_entries.bb`
> (dry-run default, idempotent name-keyed upserts, x-penholder api, run-write! pipeline):
> **454 `c-entry` entities** durable on :7071 (455 live entries, one exact-duplicate pair deduped;
> full EDN fidelity in `:props :entry-edn` + flat convenience props), **3 `method/class` entities**,
> **62 `:discharged-by` relations** (c-entry → method/{centre-mess,adopt-redirect,preserve-coherence})
> and — step 2b, what makes the join graph-queryable — **190 `:outcome-ref` relations** from c-entries
> to CANONICAL mission nodes (`<repo>-d/mission/<stem>`, resolved via the 472 mission/doc entities;
> 5 unresolved, reported). Names follow README-conventions (entity `:name` = the identifier =
> relation endpoint). Prerequisite fixed the same day: the in-memory join's vocabulary-correlation
> bug (futon2 `2fd9022` — 0/110 → 55/110 actions advancing entries). Steps 3–5 (mined-move join,
> forward-model reads the DURABLE join, reconcile) remain — step 3 dispatched to a helper agent
> with the mining-run landing spec (the deferred M-fold-ansatz batch-recovery now has a place to land).

> **STEP 3 DONE — the durable join, queried (2026-07-02, claude-11; claude-3's step-0 finding:**
> the HTTP census/type endpoints don't count relation-kind docs, and `:relation/type` is stored
> KEYWORDIZED — the join runs via Drawbridge datalog, not the HTTP scan.) **Coverage:** of the
> 189 `:outcome-ref` relations, **119 c-entries reach ≥1 `code/v05/mined-move`** through the
> shared canonical mission node (83 distinct missions; head: autoclock-in ×5, the-futon-stack ×4,
> buyer-discovery ×4); 354 mission nodes carry moves. So the durable join has real signal for the
> mission-directed slice (119/454 ≈ 26% of all c-entries); the token-match stays the fallback for
> the rest. The query (reusable, read-only, Drawbridge):
> ```clojure
> (let [db (xtdb.api/db (:node @futon3c.dev/!f1-sys))
>       rels (xtdb.api/q db '{:find [f t] :where [[r :relation/type :outcome-ref]
>                                                 [r :relation/from f] [r :relation/to t]]})
>       mm (into #{} (map first) (xtdb.api/q db '{:find [ep]
>                                                 :where [[e :hx/type :code/v05/mined-move]
>                                                         [e :hx/endpoints ep]]}))]
>   (filter (fn [[_ m]] (mm m)) rels))   ; ⇒ the c-entry ↔ mined-move join pairs
> ```
> Remaining: ~~step 4 (forward-model reads THIS join for the covered slice) · step 5 (reconcile)~~
> → **landed, see §12** · the mining pilot (claude-3, fresh cycles) growing `:discharged-by`
> toward the uncovered mission-directed entries (43 as of 2026-07-03, §12).

> **PROOF-MINE SWEEP LANDED + coverage re-measured (2026-07-03, claude-11 verify).** The gold-gated
> GPU run (proof-mine-runner-spec; 218 missions, D5 bands passed 0.54/0.70/0.70) landed **43
> git-validated `:discharged-by` relations** (verbatim witnesses only; 18 prose + **8 hallucinated
> shas REJECTED by git-validation** — the verification design paying for itself), name-keyed
> `commit` entities (`sha/<7>`), source `proof-mine`, reversible. **Coverage delta:** mission-directed
> c-entries covered 119/189 (mined-moves) → **138/189 by either axis (+19)**; the uncovered tail
> shrank **70 → 51**; 103 missions now carry discharge evidence (total `:discharged-by` 106).
> Open follow-ons: c-entry-grain attribution (landing is mission-keyed); **rule-candidates from the
> run are mined but NOT yet landed** (fold_engine PRs — the R16-EXEC-REACH/R14-variance channel).

## 12. STEPS 4+5 DONE — the forward model reads the durable join; the bridge reconciled (claude-4, 2026-07-03; Joe: "drive the excursion")

**Step 4 — the durable read (all in futon2, live-loaded via Drawbridge `load-file`, JVM never restarted):**
- `c_vector.clj` gains a **durable-join section**: `fetch-durable-join*` reads the persisted
  `:outcome-ref` + `:discharged-by` relations and each c-entry's `:entry-edn` outcome-ref from the
  in-JVM XTDB node via `requiring-resolve` (relations have NO HTTP read route — the claude-3
  finding — and the WM scheduler cohabits the serving JVM with the store, so the same
  dynamic-resolve discipline as `credit-satisfy-prob` applies: resolves live, nil in bb/futon2's
  test JVM ⇒ degrade to token-match). Injectable via the `!durable-join-fn` atom seam.
- `build-durable-adv` (pure, hermetically tested) compiles the fetch into the forward-model lookup
  `{advancer-token → #{c-entry ref tokens}}` from TWO edge families: mission `:outcome-ref` edges,
  and c-entry→`method/*` `:discharged-by` edges — the literal "which C-entries does this policy's
  method discharge?" read. Mission→`sha/*` rows are past-discharge EVIDENCE, deliberately NOT
  compiled (they feed the §5 reconcile numbers via `durable-join-stats`).
- `refresh!` caches the compiled map in `c-state` under `:durable-adv` (+ `:durable-join-meta`) —
  derive-and-cache, off the WM hot path, last-good preserved on fetch failure. NB the corpus
  signature does not observe relation-only changes; join staleness is bounded by the refresh
  cadence (any corpus change / the 5-min `ensure-belly-fresh!` debounce), documented in-line.
- `advanced-outcome-ids` expands its base set through the cached join: lookup keys = base tokens ∪
  the action's method/`:type` tokens; nil join ⇒ exactly the pre-durable behaviour (the uncovered
  slice keeps the token-match fallback). `predictive-goal-outcome-risk` is signature-unchanged.
- **Blank-token guard:** `id-tokens` now yields NO tokens for blank ids — a correction entry's
  empty `:ref-id` would otherwise compile to `""` and every blank-id entry would match every
  expansion (silent over-match, caught while writing the hermetic fixture).

**Gates:** clj-kondo 0 errors / 0 warnings on both changed files; check-parens OK;
`clojure -X:test` **414 tests / 1322 assertions, 0 failures** (4 new deftests: pure compile,
expansion arities, join-only re-rank, blank-guard).

**Live verification (real belly, real join):** `refresh!` → 455 live entries (141 stated + 314
overlay), join meta `{:outcome-ref 189, :disch-entry->method 62, :disch-mission->commit 43,
:advancer-tokens 351}`. **190/455 live entries are reachable through the durable join.** The
method-class read is pure new signal: `{:type :centre-mess}` advances **75 entries the token-match
cannot reach at all** (its base set is empty) and re-ranks live — risk 0.27939 vs no-op 0.29636;
a covered mission action also re-ranks (0.29525). The join additionally covers 4 alias-gap
missions token-match missed (e.g. `web-arxana-missions`, whose entries only carry the `M-` form).

**Step 5 — reconcile (the bidirectional bridge, measured fresh from the store 2026-07-03):**
backward (goals/holes → 189 outcome-ref'd c-entries over 115 canonical mission nodes) meets
forward (methods) on three axes: **119 via mined-moves · 51 via method-class · 36 via commit
witnesses ⇒ 146/189 (77%) covered by ≥1 axis, uncovered tail 43** (down from 51 — the
method-class axis had not been counted in the "either axis" figure before). The bridge is no
longer only *queryable* — since step 4 the WM's predictive risk **consumes it on every scored
tick**, so a newly landed `:discharged-by` relation changes policy ranking at the next belly
refresh with no code change. Full durable-join *promotion* (persisted C-entries as the canonical
source replacing the in-memory derive) stays gated on M-populate-substrate-2 D4, per §7.

**Open follow-ons (named, not hidden):** the 43-entry uncovered tail (mining pilot);
c-entry-grain attribution for the mission-keyed commit evidence; rule-candidates from the GPU run
still unlanded (fold_engine PRs); unify the two `produce-stated` producers (§9 follow-up (b),
untouched here); a relation-aware freshness probe if relation landings ever outpace the refresh
cadence.

> **BOUNDARY AGREED with M-evaluate-policies D5a (claude-4 ↔ claude-5 bell, 2026-07-03):**
> `c-distribution` (per-channel normalised preference DENSITY over [0,1], uniform-on-[lo,hi]
> + exponential tails, temperature param) lives in `futon2.aif.preferences`, **owned by
> M-evaluate-policies D5a**; W1 (this excursion's remaining probabilistic-KL upgrade, §10
> deferred item) is a CONSUMER — neither mission builds a private C. W1's consumer
> requirements, accepted as the shape contract: (1) a **Bernoulli/point-mass form for
> `:becomes` (binary) goal-outcome entries** — Q is the policy's predicted satisfaction
> prob (the R12 credit p already sourced in `credit-satisfy-prob`); don't force binaries
> through the continuous density; range channels (`:>=`/`:<=`, `frac`-normalised) use the
> continuous density directly. Expose `(log-preference dist x)` + `(kl q dist)` for both
> Gaussian and Bernoulli Q. (2) **KL in nats, densities normalised over [0,1]**, stated in
> one place, so D5a's Σ Πch·KL and W1's mean-normalised goal-outcome term add honestly
> into G-total (W4). Temperature: higher = softer; temp→0 = hard hinge/point-mass; default
> documented. (3) **Degrade-safe**: flag off / density absent ⇒ exactly the current hinge
> behaviour (augment-don't-rip-out, §5). (4) **bb-loadable**: pure Clojure or
> dynamic-resolve — `c_vector.clj` must keep loading in babashka (R19-UNIFY). Faithfulness
> via the dcbe021 badge layer + argue-exhibit pp. 8–9 formulas, per claude-5's note.

> **ITEM-5 BERNOULLI ROUND-TRIP — CLEAN (claude-4, 2026-07-03; E-KL-refinements item 5,
> requested by claude-5 bell …ed2f2ac5).** W1 now has a real consumer of the D5a density:
> `c-vector/kl-risk-of` (one open `:becomes` entry → weight · `pref/kl` {:kind :bernoulli
> :p q-sat} ‖ `pref/c-distribution` {:becomes 1}) and the DARK twin
> `predictive-goal-outcome-risk-kl` (advanced ⇒ q-sat = R12 credit p, non-advanced ⇒ 0.0;
> range entries keep the hinge; production path untouched — no flip). NO private C: both
> fns import `futon2.aif.preferences` (pure ns, no cycle, bb-loadable preserved).
> **Live round-trip (real belly, 411 `:becomes` of 455 entries):** entry
> `ai-passes-prelims` (cap, weight 0.6), real credit p = 0.5 (Beta prior burn-in) →
> kl-unmet **6.0000 nats** (= weight·1/T at T=0.1, exactly the contract's stated
> semantics), kl-at-p **2.5841**, hinge 0.6; belly-wide dark twin no-op **2.8480** vs
> advance **2.8405** — re-ranks in the same direction as the hinge lane (0.29636 vs
> 0.29570). **Contract verdict: arg shapes, nats units, temperature semantics
> (higher=softer, T→0 point-mass), and the [] ⇒ 0.0 floor all hold at the real call
> site — no shape friction.** Three feedback items for the owner: (1) **scale** — the KL
> lane runs ≈ ×9.6 the hinge at T=0.1, so any flip needs the item-3 temperature/weight
> calibration first (named in the dark twin's docstring as flip-blocking); (2) W1's
> `:becomes` values are domain KEYWORDS (:attested/:closed) that coerce truthy → target 1
> — correct today, but "prefer NOT x" is inexpressible from W1's entry shape (fine,
> noting the asymmetry); (3) range entries cannot take the KL form from W1's lane (their
> Gaussian Q lives in efe's channel model), so the dark twin unit-mixes deliberately and
> visibly. Gates: clj-kondo 0/0, check-parens OK, `clojure -X:test` 427/1635, 0 failures
> (2 new deftests: `kl-risk-of-roundtrip-test`, `predictive-goal-outcome-risk-kl-test`).
