# Mission: Goals and holes — the WM's setpoints, stated & unstated (M-goals-and-holes)

**Date:** 2026-06-25
**Status:** **IDENTIFY** (standard mission — run by the lifecycle; the **backward/goal dual** of [[M-operational-vocabulary]]).
**Owner:** Joe + claude-1
**Repos:** futon1a (`:7071` substrate-2 — the `:sorry` entities) · futon3c (Evidence Landscape, agent→human turns) · futon5a ([[M-a-sorry-enterprise]], stack-geometry) · futon0 ([[M-capability-star-map]] — the goal hierarchy) · futon7 ([[C-pudding-prover]] — the discharge standard) · futon2 (`aif/efe.clj` risk; `aif-wiring-explainer.html` — the belly; this doc)
**Cross-ref:** [[M-operational-vocabulary]] (the **forward** dual — methods/policy) · [[M-a-sorry-enterprise]] (the sorry-driven-organisation this serves) · [[M-aif-wiring]] (R5 risk · R14 γ · R15 hierarchy · R16 — the backward criteria; the **belly** this fills) · [[M-populate-substrate-2]] (the `:sorry` corpus + the empty PROOF layer) · [[M-capability-star-map]] + [[C-pudding-prover]] (orientation) · `futonic-logic.flexiarg` (the 應 should-voice)

---

## HEAD (Joe, 2026-06-25 — verbatim sense)

We've talked a lot about different ways of tracking *goals* — ranging from the original devmaps to missions, excursions, and a proof-is-in-the-pudding reality-based detection system. We've also talked a lot about different ways of understanding and tracking *holes* — ranging from the "gap" that is intrinsic to a mission at the IDENTIFY stage to a typed hole in a formal proof (or, similarly, a typed hole in our model of a software system). One useful example is the capability-star-map: it lists *known* capabilities that we would like to gather, but implicitly it also *leaves out* some latent capabilities that we are likely also gaining but haven't made an explicit model of yet.

In contrast to the domain of goals-and-holes, we have a comparatively *good* understanding of the forward direction — build missions, follow advice in patterns, and so on. There's nothing wrong with building in a forward aggregative/accretive mode, but unless we model the goals-and-holes we risk not building *towards* valued things even when we know what they are. Accordingly, we need to think through the "backward" direction to the same level of rigour as we have the "forward" direction. This is also attested to by the AIF model (Friston's C-vector).

---

## 1. IDENTIFY — name the gap

**Motivation.** The **forward** direction is rigorous ([[M-operational-vocabulary]]: methods/policy mined from history). The **backward** direction — the *goals/holes* the building should serve — is not. The cost is precise: **forward-accretive building can accrue capability without building *toward* what we value.** In AIF terms the act-gate has no **setpoint** — EFE's risk has nothing to measure distance against; the **belly** (Friston's C-vector) is empty. This mission brings the backward direction to the same rigour as the forward.

**The miss this exposes (Joe, 2026-06-25).** The R1–R18 contract (`futon2/docs/futon-aif-completeness.md`) enumerates the recognition + policy machinery (A→R3, B→R4, the EFE/selection/precision loop) but has **no first-class preference criterion** — **C** (prior preferences over outcomes, one of the five canonical generative-model matrices A/B/C/D/E) appears only *implicitly* inside R5's risk and *mislabeled* as static hyperparameters in `aif.preferences`. So C is a **silent miss** — both the [[E-aif2-partB]] failure mode (the hand-grown checklist skipped a canonical component) and the R18 faithfulness fault (a load-bearing quantity present-but-unnamed). The R14–R18 round was a literature-diff but diffed for policy-*depth*, never against A/B/C/D/E systematically. **This mission is what surfaces and fills C; it should become contract criterion R19** (and a matrix-systematic A/B/C/D/E diff should check D and E too).

**Theoretical anchoring — the C-vector, pinned (lit. refs).** In discrete active inference the generative model is `{A: likelihood P(o|s) · B: transitions P(s′|s,π) · C: prior preferences · D: initial-state prior · E: policy prior}`. **C is the agent's prior preference over OUTCOMES** — a categorical distribution (log-preferences per outcome modality) encoding goals as the observations it expects/prefers to obtain. Expected free energy decomposes **`G(π) = ambiguity (epistemic / information-gain) + risk`**, where **risk = the KL divergence between a policy's predicted outcomes and C**; policies leading to C-preferred outcomes are more probable. The load-bearing pin: **with C ignored, `G` reduces to pure information-gain** (intrinsic motivation) — a policy with no preferences explores but builds *toward nothing* (the "hand with no belly"). C may be fixed, learned, or set top-down by a higher hierarchical level (deep / sophisticated inference → R15). **So this mission = model the WM's C: the goals/holes are its preferred-outcome entries, and `hole/latent` = preferred outcomes not yet encoded (C's negative space).**

*References (standard literature):* Da Costa, Parr, Sajid, Veselic, Neacsu & Friston, **Active inference on discrete state-spaces: a synthesis**, *J. Math. Psych.* (2020) — the A/B/C/D/E formulation + the risk/ambiguity split; Friston, FitzGerald, Rigoli, Schwartenbeck & Pezzulo, **Active inference: a process theory**, *Neural Computation* (2017); Parr, Pezzulo & Friston, **Active Inference: the FEP in Mind, Brain, and Behavior**, MIT Press (2022); Smith, Friston & Whyte, **A step-by-step tutorial on active inference**, *J. Math. Psych.* (2022); and, for *how* C is set (a DERIVE-phase concern): "Prior preferences in active inference agents: soft, hard, and goal shaping" (arXiv 2512.03293). Plus the in-stack duality/belly: [[M-operational-vocabulary]] §3, [[M-aif-wiring]]. Other anchors: backward-chaining / hierarchical goals (logic); self-discrepancy (ideal vs actual).

**Shape-first (the gap is a *family*, not a one-off).** The gap is "the WM's setpoints are unmodelled." A setpoint/hole comes in sibling instances under one shape — `hole/<flavour>`:
- `hole/stated` — an explicit goal/sorry (spoken: 應-voice, devmap/star-map entry, a `:sorry`);
- `hole/incompleteness` — a *known* goal not done (started-but-not-run, raised-but-unaddressed, declared-but-empty, asked-but-unanswered);
- `hole/latent` — an *unnamed* goal (the star-map's negative space — capability accruing with no model). The deepest sibling.
- `hole/mess` — a *named, run, but low-coherence* goal: a mission scored **messy** by Salingaros `L=T·H` (high mess `C=T·(10−H)`) — not unfinished, not unnamed; it exists and runs, but is tangled, and tidying it *is* a goal (Joe). The implicit hole the other three siblings don't name.

Naming the family up front (per `shape-first-identify.flexiarg`) is the IDENTIFY move; whether all three are real *kinds* (vs one collapsing into another) is a MAP/DERIVE finding, not assumed here.

**Scope in:** model the goal/hole family (all three siblings) into the WM's preference set — the backward input to EFE's risk / the belly. The forward↔backward **join** (which method discharges which goal = substrate-2's PROOF layer) is the shared deliverable with M-operational-vocabulary.
**Scope out:** the forward method-mining (M-operational-vocabulary); building the substrate-2 PROOF *store* ([[M-populate-substrate-2]]); arming/acting (M-aif-wiring / Car-3, gated).

**Completion criteria (testable; provisional until ARGUE):**
1. A trusted **`hole/stated`** open-goal set, attached to missions via the star-map.
2. ≥1 **`hole/incompleteness`** detector surfaces a *meaningful* hole the stated channel misses, distinguished from normal WIP by orientation.
3. ≥1 **`hole/latent`** probe surfaces a goal the star-map *leaves out* (its negative space).
4. Goals feed EFE's risk (the belly → R5), joining the forward methods (the join = substrate-2 PROOF).
5. Discharge held to the Pudding-Prover standard (a hole closes on a real witness, not an assertion).

**Relationships:** forward dual [[M-operational-vocabulary]] · serves [[M-a-sorry-enterprise]] · AIF home [[M-aif-wiring]] · corpus/store [[M-populate-substrate-2]] · orientation [[M-capability-star-map]] + [[C-pudding-prover]].
**Source material:** the 293 `:sorry` entities (`:7071 entities/latest?type=sorry`); `sorrys.edn` (14); agent→human turns (應-voice); the devmaps; the star-map graph; Mission-Control coverage/tensions.
**Owner/deps:** Joe + claude-1; repos as above.

> **IDENTIFY exit criterion:** Joe reads this and agrees the gap is real and the scope is right (and the `hole/{stated,incompleteness,latent}` family is the right shape). **← pending.**

## 2. MAP — survey questions (research; facts, not design — to answer next)

*Don't design the detectors yet. MAP reads code, calls APIs, counts things.* Questions to answer:
- **Q1 — inventory the goal/hole-tracking infra + its grain.** devmaps · missions/excursions/campaigns · star-map · Pudding-Prover · the 293 `:sorry` · M-a-sorry-enterprise's B→A mining · Mission-Control coverage/tensions. What does each already detect, and at what grain?
- **Q2 — the `:sorry` corpus, exactly.** status/maturity/source/name-type distributions + the trust-filter yield. *(Preliminary, from the M-operational-vocabulary audit: 143 open → 110 clean + 33 boilerplate; confirm + extend.)*
- **Q3 — run-evidence.** How is "a mission was *run*" detectable (turns clocked to it · commits touching it · PARs · evidence entries)? What's the signal separating *started* from *run* (the `hole/incompleteness` input)?
- **Q4 — the star-map's negative space.** How could a `hole/latent` even be *detected* — what signals "capability accruing with no model" (co-application clusters / recurring PSRs with no declared cap)?
- **Q5 — Mission-Control already generates tensions.** Does it already surface some of these holes? Overlap vs gap with this mission.
- **Q6 — the goal↔method join.** Anything already wiring goals to the methods that discharge them? *(Preliminary: substrate-2's PROOF layer is empty — confirm.)*

**Deliverable:** a **ready-vs-missing** table (column "ready — exists, no new code" vs "missing — the actual work"), every Q answered with concrete findings.
> **MAP exit criterion:** every Q has a concrete answer; the ready-vs-missing table is complete.

### MAP findings — what can feed C (2026-06-25)

**Headline:** "what can feed C" splits by the three hole-flavours, and the supply gradient matches the difficulty — **stated is over-supplied (ready), incompleteness is partial, latent has no feed at all.**

| Candidate C-feed | channel | status | facts |
|---|---|---|---|
| capability-star-map (`futon0/.../M-capability-star-map.graph.edn`) | stated | **READY** | 37 caps w/ status — 23 satisfied · 13 held · 1 active · 12 frontier; **14 not-satisfied = open goals**, 12 frontier = summits. The cleanest, most-structured feed (explicit preferred capabilities). |
| 293 `:sorry` entities (`:7071 entities/latest?type=sorry`) | stated + incompleteness | **READY** | 143 open → **110 clean** + 33 boilerplate (boilerplate = incompleteness at low-res). |
| devmaps (`futon{0-7}.devmap`, ~10) | stated | **READY** (via sorries) | prototype completion goals; already harvested into the sorries (50 devmap-type). |
| `sorrys.edn` (14, curated) | stated | **READY** | hand-curated WM sorries. |
| agent→human **應-voice** turns | stated / incompleteness | **PARTIAL** | present (~20% raw fire a should/sorry marker); needs a recognizer to extract structured holes. |
| run-evidence (`turn-commit-mission-backfill.json`) | incompleteness | **PARTIAL** | 1653 commit→mission + 639 turn-attributed (best-guess, choppy); seeds a started-but-not-run detector (scopes present ∧ few/no attributed commits). |
| substrate-2 declared-but-empty layers | incompleteness (structural/meta) | **PARTIAL** | PROOF/CLAIM/ARXANA empty (substrate-2 explainer) — a structural hole signal, queryable. |
| Salingaros mess/liveness (`futon6/data/mission-wholeness.edn`) | **mess** | **READY (+discharge)** | 194 missions scored `L=T·H` / `C=T·(10−H)`; **43 mess-class** = implicit holes (tidy them). Uniquely, the discharge already exists — the `:centre-mess` move-class + the mission-coherence cascade — so this channel's `discharged-by` join is partly closed. |
| **latent** / star-map negative space | latent | **MISSING** | no existing source; candidate signal = co-application clusters / recurring PSRs with no declared cap. All to-build — the hardest. |
| the **C-vector object** + the R5-risk join | assembly | **MISSING** | nothing assembles a preference vector from these feeds and wires it to EFE's risk (the join = substrate-2's empty PROOF layer). |

**Q-answers:** Q1 inventory+grain — done (star-map=cap-grain · devmaps=prototype · sorries=typed-hole · MC=status). Q2 sorry distributions — 143 open / 110 clean / 33 boilerplate. Q3 run-evidence — partial (1653 best-guess commit→mission). Q4 latent — no source (candidate: no-declared-cap co-application clusters). Q5 MC tensions — **no hole-emitter found** (MC = status/coverage, not hole-detection). Q6 goal↔method join — empty (PROOF layer; confirmed).

**MAP verdict:** C's **stated** entries can be populated *now* (star-map + sorries + devmaps); the **unstated** channels are the real work — *incompleteness* is buildable on partial signal (run-evidence + empty-layers), *latent* has no feed at all and is the hard frontier. The C-vector object + the R5 join are missing across all three.
> **MAP exit:** every Q answered; ready-vs-missing table complete. **← ready for DERIVE on Joe's nod.**

## 3. DERIVE — the C-vector model (2026-06-25)

**Central decision — what space is C over? (IF/HOWEVER/THEN/BECAUSE).** **IF** C must be AIF-faithful — prior preferences over *outcomes the agent can observe* (Da Costa et al.) — **HOWEVER** the goals/holes (star-map caps, sorries) are structured objects, not values of the existing 13–14 observation channels, so projecting them onto those channels is lossy — **THEN** extend the outcome space: **C is over (observation channels ∪ goal-satisfaction observations)**, where each tracked goal/hole contributes one *goal-satisfaction outcome* (queryable — a cap's `:status`, a sorry's open/closed, a phase's run-evidence) that the agent prefers satisfied; **risk = Σ divergence(predicted outcomes, C)** across both sub-spaces. **BECAUSE** goal-satisfaction *is* observable, so a specific cap/sorry becomes a first-class preferred-outcome the risk term scores against — faithful to C-over-outcomes, and it *reuses R5's existing risk machinery* (extended from 14 channels to 14 + N goal-outcomes) rather than inventing a parallel one.

**Entity types.**
- `C-entry` — one preferred outcome: `{id · flavour: stated|incompleteness|latent · outcome-ref: channel | goal(cap-id / sorry-id / mission-phase) · preferred: satisfied | value-range · weight · status: open|met|foreclosed · provenance · witness?}`. **The C-vector = the open C-entries.** *Derived* from its source, never authored.
- Sources are existing entities (star-map caps, `:sorry`, devmap goals) — C-entries point back to them.

**Relation types.**
- `derived-from` : C-entry → source (provenance).
- `ascends-to` : C-entry → capability/summit (star-map orientation → weight).
- `discharged-by` : C-entry → method (a forward meme/move/cascade that moves its outcome toward *met*) — **the join to [[M-operational-vocabulary]]** = substrate-2's empty PROOF layer (`:closes`/`:constructs`).
- `risk-contributes` : C-entry → R5 (the belly→R5 arc; EFE risk reads the open entries).

> **Two C's — resolved by a relabel.** Friston's **C-vector** = this mission's subject (the preferences). Salingaros's metric (formerly a bare `C=T·(10−H)`) is now **coherence (H) / liveness (L=T·H)** in the code — relabeled exactly to avoid this clash. It is a *source* that **feeds** Friston's C: a low-coherence (messy) mission → the preferred-outcome "raise its coherence/liveness."

**Producers (the four detectors, over the MAP feeds).**
- **stated** (READY): star-map not-satisfied caps (13 held + 1 active + 12 frontier) + 110 clean open sorries + devmap goals (via sorries) — all CPU reads — **+ 應-voice (the LINODE companion):** a *backward* `meme_mine_joint` pass over `role=assistant` turns paired with the human reply — the only GPU-mined backward channel, **hot-swapped onto the standing forward-run box**; yields `flavour ∈ {reach (unstated goals), correction (human override of an agent action — the cleanest C-signal)}` — VERIFY decision-log. → `C-entry{flavour, preferred}`.
- **mess** (READY + discharge exists): `mission-wholeness.edn` (Salingaros coherence H / liveness L=T·H). Two C-entries:
  - **(i) per-mission hole** — the **43 low-coherence (mess-class)** missions → `C-entry{flavour=mess, outcome-ref=that mission's coherence, preferred=coherence-rises}`. A messy mission *is* a hole (Joe); discharge already exists (`:centre-mess` / mission-coherence cascade) — the one channel whose forward-method is built.
  - **(ii) a STANDING global-coherence entry** → `C-entry{flavour=mess, outcome-ref=stack/total coherence, preferred=stays-high, weight=high}` — not a hole to close but a **regularizer**: it makes *every* policy's risk penalize actions that *lower* coherence, so the agent fixes one thing without making a bigger mess (see ARGUE — the safety property).
- **incompleteness** (PARTIAL): *started-but-not-run* = mission has scope-phases ∧ ~no run-evidence (backfill: few/no attributed commits) → `C-entry{flavour=incompleteness}`; + raised-but-unaddressed (sorry age × still-open) + declared-but-empty layers. Gated by I4.
- **latent** (MISSING — frontier): co-application clusters / recurring PSRs with no declared cap → *candidate* `C-entry{flavour=latent, status=conjectural}` — surfaced, never asserted.

**Invariants (checkable).**
- **I1 provenance** — ∀ C-entry ∃ `derived-from` (no fabricated preference; the 間 discipline).
- **I2 orientation-weight** — weight = critical-path distance to a high-value summit by backward induction over **(star-map ∪ devmap hierarchy)** (the star-map's 37 caps cover only a minority of holes; devmaps are the second goal-tree — ARGUE W2); un-oriented entries take a **low default weight** (surfaced, de-prioritised), never the entry's local text.
- **I3 earned closure** (Pudding-Prover) — `status` flips open→met only with a `witness` (n≥1 real outcome), never by assertion.
- **I4 meaningful-absence** — an incompleteness C-entry surfaces if it `ascends-to` a non-backwater goal in (star-map ∪ devmap); the un-oriented surface at default-low weight (never silently dropped — that would re-hide the hole).

**Data flow.** the 3 CPU detectors read the MAP feeds → emit C-entries (with provenance + orientation-weight) → assembled as the C-vector → `risk-contributes` to R5 → `discharged-by` joins to M-operational-vocabulary's mined methods (the PROOF layer). Sim-only; no substrate writes until the join is promoted ([[M-populate-substrate-2]]).

**Wiring.** the belly (R19) → R5-risk arc already exists in `aif-wiring-explainer.html`; this DERIVE specifies *what flows on it* (open C-entries) and *what joins it* (`discharged-by`, the PROOF layer).

### PSR — declare-don't-guess (futon-theory / M-wm-policies seam)
- Pattern chosen: declare-don't-guess (a C-entry's outcome-ref + provenance is *declared from its source*, never inferred from free text — the seam that stamped `:advances-cap` on mined moves).
- Candidates: declare-don't-guess vs infer-from-embedding.
- Rationale: a fabricated preference is worse than a missing one — the audit's lesson (33 boilerplate sorries) and the AIF risk: a wrong C silently steers the whole loop.
- Confidence: high — same seam already proven on the forward (mining) side.

> **DERIVE exit:** implementable from this section alone (entity/relation types, the 3 producers, invariants I1–I4, data flow, the C-over-outcomes decision). **← for Joe's check.**

## 4. ARGUE — does it stand up? (2026-06-25)

**Pattern cross-reference (the design instantiates established library patterns — it is not ad-hoc).**
| DERIVE move | library pattern(s) | how it applies |
|---|---|---|
| I1 provenance / declare-don't-guess | `ukrns/coding-provenance` · `vsatelier/decision-provenance` · `futon-theory/honest-map-over-flattering-counter` | every C-entry cites its source; an honest *missing* beats a flattering fabricated preference |
| I2 orientation-weight | `structure/backwards-induction` | weight a hole by backward-induction from the summit it ladders to |
| I3 earned-closure (witness) | `invariant-coherence/{subsumption-witness, state-snapshot-witness}` · `math-informal/construct-an-explicit-witness` | a C-entry flips met only on a real witness (the Pudding standard) |
| `discharged-by` (hole→method) | `system-coherence/bind-open-questions-to-closure-mechanisms` | bind each goal/hole to the method that closes it (= the PROOF-layer join) |
| the hole-family shape | `invariant-coherence/{shape-first-identify, protocol-family-naming}` | `hole/{stated,incompleteness,latent}` as sibling instances |
| a hole = a committed goal | `futon-theory/derive-exits-on-a-minted-sorry` · `war-machine/ideal-actual-gap` | a sorry / ideal-actual gap IS a goal with the method absent |

**Theoretical coherence.** The design serves the IDENTIFY anchor (C = prior preferences over observable outcomes): goal-satisfaction *is* observable, so C-over-(channels ∪ goal-outcomes) is faithful, and risk = divergence(predicted, C) is the canonical term — reusing R5, not inventing one. **One coherence nuance (W1 below):** risk-scoring a goal-outcome requires the *forward model* to predict goal-progress under a policy — which is the `discharged-by` join (the empty PROOF layer). So stated-C *assembles* now, but risk-*closure* over goal-outcomes waits on the join.

**Stress-test — where it bends:**
- **W1 — staging dependency (not fatal).** Per above: assembling C is independent of the join; risk-scoring goal-outcomes depends on it. Honest sequence, not a flaw — VERIFY should spike the channels-only risk first (already works) then the goal-outcome extension behind the join.
- **W2 — orientation coverage (→ DERIVE revision).** I2/I4 orient by the star-map — but it has only **37 caps**, and most holes (the 110 sorries are devmap/excursion/technote) **aren't cap-linked**, so `ascends-to` is undefined for the majority → I4 can't filter them. **Fix (folded into DERIVE):** orientation = star-map *∪* the **devmap hierarchy** (devmaps double as a goal-tree — IDENTIFY already noted this) *∪* a low default-weight for the un-oriented (surfaced but de-prioritised, never dropped). This is the one place the DERIVE was under-specified; now patched.
- **W3 — latent is a conjecture, not a detector.** "no-declared-cap co-application cluster" is a *weak proxy* for an unmodelled goal. Honest scope: the latent producer emits **surfaced candidates for review**, status `conjectural` — not asserted goals. It's the frontier (MAP: MISSING); fine if never dressed up as more.
- **W4 — scale (tunable).** Combining goal-outcome-risk + channel-risk needs a commensurable weight so N goal-outcomes don't swamp 14 channels (or vice-versa). A normalisation/weight, set in INSTANTIATE; minor.
- **W5 — a missed channel, now folded (Joe).** ARGUE/DERIVE omitted the **Salingaros mess** channel: a messy mission (low `L=T·H`) is an implicit hole — *tidy it*. It is the **second READY channel** (43 mess-class scored in `mission-wholeness.edn`) and, uniquely, its **discharge already exists** (`:centre-mess` / mission-coherence cascade). Now `hole/mess` (IDENTIFY) + a 4th producer (DERIVE) + a MAP row. A *strengthening*, not a weakness — and it gives VERIFY a clean target: **mess is the one end-to-end-ready channel** (scored holes ∧ a built discharge ∧ an observable outcome L), so the C→risk→discharge loop can be spiked there *without* waiting on the W1 join.

**Trade-offs.** We give up: (a) a *complete* C now — only the stated channel is immediately populatable; (b) automatic latent-goal detection — it stays human-reviewed; (c) immediate risk-closure — it waits on the join. We keep: faithfulness (real C-over-outcomes), honesty (provenance + witness + surfaced-not-asserted), and reuse (R5's existing risk).

**Generalization — the coherence preference is a safety regularizer (Joe).** The mess channel does double duty. As a *hole* it says "tidy this messy mission." But its **standing global-coherence entry** (DERIVE mess-ii) generalizes far beyond that: because lowering coherence raises EFE risk, **an agent with this C will not make a big mess in the process of "fixing" something** — it can't trade a local fix for a global tangle, because the tangle is penalised in the same `G` it's minimising. That is a genuine **safety property**, and it bears directly on **R16 arming**: a coherence-preferring agent is *safer to let act*, because the failure mode of "agent kludges X working while wrecking Y" is priced out by its own preferences, not bolted on as an external guardrail. (This is also why C cannot be just a list of goals-to-reach — it must include preferences over *how the whole stays*, not only *what gets done*.)

**Plain-language argument.** The agent can act (it has hands) but has no *belly* — no model of what it's hungry for. This mission gives it one: it reads the goals we've **stated** (the capability map, the open sorries) and the holes we've left **unsaid** (work started but never run), and turns each into something the agent prefers to be true. Each preference is tagged with where it came from (never invented), weighted by how much it matters (by what big goal it ladders up to), and counts as "met" only with real evidence, not a claim. Then the planner can finally ask *"am I getting closer to what we want?"* — which, before, it couldn't, because nothing told it what we wanted.

**Verdict: it stands up** — the spine (C-over-outcomes, faithful, pattern-grounded, R5-reusing) holds; ARGUE forced **one DERIVE revision** (W2 orientation = star-map ∪ devmap ∪ default), **added a missed READY channel** (W5 — Salingaros mess, with its discharge already built), surfaced one **known staging dependency** (W1), and **honestly bounded** the latent channel (W3). Not "merely possible" — it instantiates six established patterns + the canonical C-vector, and now has **two ready channels** (stated + mess). → ready for VERIFY, best spiked on the **mess** channel (the only end-to-end-ready one).

## 5. VERIFY (2026-06-25)

**Spike — the mess channel end-to-end on real data (CPU, no Linode).** Read `mission-wholeness.edn` → formed **43 mess C-entries** `{flavour=mess, outcome-ref=coherence L, preferred=alive-median L=45.9}` → risk = distance(preferred, current L) is non-trivial (0–35.9, median 22.5) and **ranks the messiest highest** (M-editorial-assistant, L=10 → risk 35.9); discharge exists (`:centre-mess`). **Validates the riskiest DERIVE commitment** — *does C-over-a-goal-outcome slot into R5's risk shape?* **Yes for an observable outcome:** the **static** risk (distance from preferred coherence) is computable now (the spike); the **predictive** risk (coherence under a policy) is the W1-gated extension. So the C→risk→discharge loop is proven on one channel without the join.

**Completion-criteria pre-check.** C1 stated — designed. C2 incompleteness — designed (started-but-not-run on the backfill). C3 latent — designed as conjectural/surfaced (honest). C4 feed-EFE-risk — **spiked (mess)**; stated-channel extension W1-gated. C5 pudding-discharge — designed (I3); mess's discharge is *built*. + mess channel — spiked.

**Decision log — the Linode-companion scope (Joe's VERIFY question → a DERIVE revision).** *Did DERIVE scope this as a Linode-mining companion to M-operational-vocabulary?* Under-scoped — and the spike shows **why it's mostly CPU**: the backward **goals are already structured** (star-map, sorries, wholeness, devmaps — pre-computed), whereas the forward **methods had to be mined from unstructured prose** (GPU). That is the genuine asymmetry; the mess spike just ran a whole channel CPU end-to-end. **One true Linode companion does exist — the 應-voice channel:** mining agent→human turns (`role=assistant`) is the exact dual of the forward run's mining of human→agent turns for memes, and it is the **same machinery** — a **backward pass of `meme_mine_joint` over the assistant turns** (the forward run already reads them as thread-context; the backward pass keys on them as the subject). **Two corrections from Joe (2026-06-25) refine the scope:**

- **Not one session — a hot-swap.** The box is *already* running the forward (human→agent) pass; the backward pass is a **code hot-swap onto the standing box** (no teardown, if we're quick), not both directions in one run.
- **Not just unstated goals — corrections.** The 應-voice channel carries TWO C-signals: (i) **reach** — unstated goals the agent orients toward; and (ii) **correction** — an agent turn that draws a human override (*"not only"*, *"not that — this"*) reveals a preference the agent's C *lacked*. The correction is the **cleanest C-signal**: Friston's C *is* preference-over-outcomes, and a human correcting an agent's just-produced outcome is exactly that, directly — the delta (agent-did → human-redirected) *is* the C-entry. It is **relational** — read from the (agent-turn, human-reply) pair, not one role. **Worked example, live:** scoping this channel as "unstated goals" drew Joe's *"not only"* → that correction is itself a `C-entry{flavour=correction}`.

**Revision (folded to DERIVE):** the 應-voice producer = the Linode companion — a backward joint pass over `role=assistant` turns *paired with the following human reply*, **hot-swapped onto the standing forward-run box**, emitting `C-entry{flavour ∈ {reach, correction}}`. The other channels (stated / mess / incompleteness / latent) are CPU. **correction** is added as a 5th channel flavour (a *revealed* preference, alongside the 4 IDENTIFY flavours).

**Unification (Joe, 2026-06-25) — `correction` ↔ `mess` are one preference at two timescales.** A mission that proceeds *without discipline* drifts: **correction** catches the drift per-turn (leading indicator, *strong* supervision); **mess** (low Salingaros coherence) is the same drift *accumulated and un-caught* (lagging indicator, *weak/absent* supervision). Both feed the **same C-entry — "stay disciplined / coherent"** — so `{mess, correction}` are two **observation modes of one preference**, not two preferences. Consequence for EFE: a policy's predicted risk includes *both* "will it draw a correction" and "will it leave a mess"; following the patterns minimizes both at once.

**Telos — weak supervision via a pattern-harness.** Why this matters for the WM: the goal is eventual operation under **weak** (not absent) supervision — the **patterns act as an end-user-supplied harness**. Mechanism = preference-internalization: corrections observed today (strong, per-turn operator signal) are **mined into patterns / C-entries** (the 應-voice channel's dual purpose), and those patterns then enforce discipline tomorrow with the operator out of the per-turn loop. Safety does *not* come from removing the gate — **R16/Car-3 stays operator-gated for *acting*** — it comes from the harness + the **standing global-coherence regularizer** (DERIVE's STANDING C-entry; ARGUE's "safety regularizer"): an agent with these preferences *won't make a big mess while trying to fix something*. So correction-mining IS the training signal for the harness. *(Folded to DERIVE: the `mess` and `correction` producers share one C-entry target.)*

> **VERIFY exit:** riskiest commitment spiked (mess: C-over-observable-outcome → R5 risk, real data); Linode-companion scope resolved (mostly CPU + the 應-voice GPU pass mining reach+correction); correction↔mess unified into one coherence preference → the weak-supervision/pattern-harness telos; DERIVE revised. **← for Joe.**

## 6. INSTANTIATE (2026-06-25)

Tracked in **`goals-holes-readiness.html`** (the INSTANTIATE card-tracker, backward dual of `mission-mining-readiness.html`). First build wave — the all-CPU static loop + the GPU backward producer:

- **C-ENTRY (built)** — the shared record, one constructor used by *both* producers: `futon6/scripts/c_vector.bb` `c-entry` (CPU) and `c_mine_joint.py` `to_c_entry` (GPU). Enforces I1 (provenance required), I2 (weight carries its `:basis`; un-oriented → default-low 0.3), I3 (status→met only with a witness).
- **C-MESS + C-RISK (built, CPU)** — `c_vector.bb` promotes the VERIFY spike to a durable producer: **43 per-mission mess C-entries + 1 standing global-coherence regularizer**, preferred = alive-class median L = 45.9; `risk-of` = weight·distance ranks the messiest highest (M-editorial-assistant L=10, top), discharge = the existing `:centre-mess`. Writes `data/c-vector/c-entries.mess.edn`. Static risk works now; predictive is W1-gated. *(The C-less EFE drops exactly this term — Da Costa et al.)*
- **C-STATED (built, CPU)** — `c_vector.bb` `produce-stated` reads `:7071`: **29 unmet capabilities** (13 `:held` w=0.6 + the rest frontier/under-propertied w=0.4; the 2 literal-meta placeholders "capabilities"/"capability" dropped, nil `:capability/id` falls back to the entity name) **+ 110 clean open sorries** (143 open − 33 templated-`:if` boilerplate = the audit split) → `C-entry{flavour=stated, preferred=:becomes attested|closed}`. The backward asymmetry in practice: pure reads, no mining.
- **C-INCOMPLETE (built, CPU)** — `c_vector.bb` `produce-incomplete` over the 211 scope-trees: **81 started-but-not-run** missions (identify/map present, no verify/instantiate/document) → `C-entry{flavour=incompleteness, preferred=:run}`, default-low weight (I4: surfaced, never dropped). M-editorial-assistant appears here *and* in mess — a real cross-channel signal.
- **C-STORE (built, CPU, sim-only)** — `c_vector.bb` `->substrate-entity` maps each C-entry to the substrate-2 entity shape `{:id :name :type :props}` (cf. the `:7071` cap/sorry entities), relations inlined in props (`:c-entry/derived-from` = I1 provenance, `:c-entry/discharged-by` = the PROOF-layer join). Idempotent ids keyed off the source's unique substrate id → **263/263 with a derived-from link, 0 collisions**. Writes `data/c-vector/c-store-overlay.edn`. **ZERO `:7071` writes** — promotion gated on [[M-populate-substrate-2]].
- **Assembled C-vector readout** — `c_vector.bb` prints the belly: **263 open preferences across 3 channels** (stated 139 · incompleteness 81 · mess 43), all carrying provenance (I1) and a weight basis (I2). Derived, never authored.
- **BACKWARD-JOINT + C-REACH + C-CORRECTION (built, stub-validated; GPU run pending)** — `c_mine_joint.py`, the 應-voice fork of `meme_mine_joint.py`: `read_pairs` reads each agent→human turn paired with the *following operator reply* (inter-agent bells / plumbing filtered out — the belly is **Joe's** preferences), shared retriever + vLLM backend + null-norm + 200-checkpoint. Stub end-to-end: **40 pairs → 45 C-entries (40 reach + 5 correction, 28 grounded** to real missions). Real reach/correction classification needs `--backend openai`, driven by the runner **`scripts/linode-goals-and-holes-mine.sh`** (built — mirrors `linode-meme-mine.sh`, tunnel-first, resolves repo/venv on dev *and* box) — **hot-swappable onto the standing forward-run box** (same model, no teardown). Only the ops decision (hold the box vs separate commission) remains.

**Still to build:** the live `compute-efe` join (C-RISK into the rollout — *but note:* static C-vector risk is a per-policy constant, so it only **steers** selection once predictive risk exists → W1-gated; static risk is diagnostic now) and the GPU 應-voice box run (hot-swap). C-LATENT deferred; W1-JOIN needs breakdown; HOT-SWAP is the live-box decision. Promotion of the C-STORE overlay to `:7071` is gated on [[M-populate-substrate-2]].

> **Future work (Joe, 2026-06-25):** inter-agent comms (`Caller: claude-N`/`codex-N` exchanges) *are* interesting as a preference signal but don't fit the operator-belly scope here — filtered out of the 應-voice pass for now; revisit as a separate channel later.

## 7. DOCUMENT (accreting)

**The C→EFE wiring caveat (load-bearing — don't mistake "risk computes" for "belly steers").** The static C-vector risk is a **per-policy constant**, so wiring it into `compute-efe` is **diagnostic** now (it reads out *what the agent wants*, ranked) but does **not steer** policy selection — argmax over policies is invariant to a constant offset. The belly only *steers* once **predictive** risk exists: a C-entry's outcome evaluated *under a candidate policy*, so a policy that discharges a preference lowers its risk. That predictive join is **W1-gated** (the goal↔method bridge = substrate-2's empty PROOF layer). Until then: the C-vector is a faithful, derived, provenance-carrying *readout* of preferences (265 open across 3 channels), not yet a controller.

## Phases ahead
**INSTANTIATE (continuing)** — C-STORE next (substrate-2 overlay, sim-only) + the GPU 應-voice box run (hot-swap, while the forward run has headroom) → **DOCUMENT**. Three CPU channels (stated/incompleteness/mess) + the GPU producer are built; the C-vector assembles (265 open preferences). Checkpoints append as each wave completes.
