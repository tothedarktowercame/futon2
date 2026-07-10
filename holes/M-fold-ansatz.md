# Mission: the embedding fold as an ansatz (M-fold-ansatz)

**Date:** 2026-07-01 · **Status:** MAP COMPLETE (claude-4 — the scatter is mapped; see §MAP M1–M4; awaiting Joe's ARGUE steer). IDENTIFY complete below.
**Authored by:** claude-4. **Card:** expands **FOLD-ANSATZ** (`futon2/holes/aif-wiring-explainer.html#FOLD-ANSATZ`).
**Cross-refs (card-IDs, not transclusions):** FOLD-ANSATZ · FOLD-GAE · FOLD-GFLOWNET · FOLD-JAXREFINE ·
FOLD-EXOTYPE · (contrast) FOLD-MINILM.

**Parent / relates:**
[[E-close-the-loop]] (nailed the fold INTERFACE + impl #1; **explicitly scoped impl #3 OUT** — this
mission is that scoped-out frontier, and it plugs into the socket E-close-the-loop defined) ·
[[E-fold-engine]] (impl #1, classical, futon3a) · [[E-llm-fold]] (impl #2, LLM-turn, futon3a — the
realization that **already works**, so #3 is a bet on *efficiency*, not on *possibility*) ·
[[E-cascade-sampler-sampler]] (the existing GFlowNet lab — `holes/labs/E-cascade-sampler-sampler/s4/`
has `train_cascade_gfn.py`, `probe_trainability.py`, gflownet-entries — **reuse, don't restart**) ·
[[E-have-want-pairs]] (the (cascade→wiring) corpus impl #3 must learn from; Q-B measured its magnet) ·
`ct-wiring-explainer.html` (the DarkTower CT target schema — TypedHole/Fill/Comb/Discharge).
**Repos (MAP will confirm):** futon3a (embedding/stores) · futon2 (`aif/fold`, the sampler lab) ·
futon6 (JAX/GFlowNet scatter) · DarkTower/CLean (the CT bridge).

---

## HEAD — the gap (as described on the FOLD-ANSATZ card)

The fold IS the interface (`futon2.aif.fold`): `(cascade, circumstance) → {:wiring :delta-g :policy-holes}`.
**impl #2 (LLM-turn) already realizes it** — cascades→wiring need *not* go through embeddings at all.
So **impl #3 (the embedding fold) is an ANSATZ**: a bet that the same process can be made *efficient*, or
decomposed into efficient subprocesses (wiring-as-graph in embedding space — GAE link-prediction,
GFlowNet generation, JAX refine, exotype encodings, with DarkTower's CT primitives as the target schema).
It is **not** the fold. Therefore **JAX + GFlowNet are speculative implementation details, deliberately
deferred** under build-to-the-interface rigor.

**The honest question this mission opens (why it is a mission, not a task):** *is the ansatz worth
building at all, and if so what is the smallest test that would tell us?* This is genuinely open —
- **FOLD-GAE's one test FAILED** (lift −0.058) — but that result is **stale** (predates the substrate-2
  uplift + CLean), so it must be **re-tested before it is trusted or dismissed**;
- the prior art that *works* (Rob: embed **Lean-code-as-graphs** in pgvector to reuse proof structure)
  is on the wrong **grain** — it embeds *code*, not missions/abstractions; a Clojure-code-as-graphs
  replication is *different, and Clojure ≠ Lean*, so substrate-2 structure-embedding is a **plausible
  but distinct bet, not a replication**;
- the near, non-speculative piece is **CLean** (Joe's `.clean.edn` Lean-compatible outline → DarkTower
  TypedHoles via `clean_to_lean.py`, 0-sorry-gated; `MissionExample` already typed) — the **outline→CT
  bridge**, not yet ported to missions/code.

So IDENTIFY's job is to frame the bet honestly: what would make the embedding fold **beat impl #2 on
efficiency without losing soundness/honesty-of-policy-holes**, and what is the cheapest experiment that
discriminates "worth it" from "no." We do **not** assume the ansatz pays off; we test it.

## MAP-phase note (Joe, 2026-07-01) — the scatter to sweep first

There are **~43 files mentioning GFlowNet and ~77 mentioning JAX** across futon2/3c/6. MAP must:
1. **Sweep and consolidate** those scattered references — most are likely stale experiments, seeds, or
   notes; find the live ones and the dead ends.
2. **Dedup against the existing lab** — `E-cascade-sampler-sampler/s4/` already trains a cascade
   GFlowNet + has a trainability probe; establish what is reusable vs what was a different target
   (that lab samples **cascades**; this mission folds **cascade→wiring** — adjacent, not the same).
3. **Re-run the stale FOLD-GAE test** on today's substrate-2 + CLean before trusting the −0.058.
4. Confirm the repo map above.

## Scope — this mission is impl #3 ONLY

**In:** the embedding realization of the fold, plugged into E-close-the-loop's interface; the
GAE/GFlowNet/JAX/exotype sub-pieces (FOLD-* siblings); the DarkTower CT target; the CLean outline→CT
bridge; the re-test of the stale GAE result.
**Out (named):** the fold *interface* (done — E-close-the-loop); impl #1/#2 (E-fold-engine / E-llm-fold);
the (cascade→wiring) *corpus* build (E-have-want-pairs / the un-built E-wiring-diagram-corpus — this
mission *consumes* it, does not build it); cascade *sampling* (E-cascade-sampler-sampler).

## Exit criteria (draft — for Joe/driver to ratify)

1. A **verdict on the bet**: evidence-backed "the embedding fold can/can't beat impl #2 on efficiency
   without losing soundness," from the smallest discriminating test — *or* an honest "still open, here is
   the one experiment that would decide it and why it's not run yet."
2. The stale **FOLD-GAE −0.058 re-tested** on current substrate-2 + CLean (trusted or retired, not left
   stale).
3. The **scattered JAX/GFlowNet references consolidated** into this mission (or explicitly retired), so
   the ~120-file scatter stops being folklore.
4. Whatever is decided, it **plugs into E-close-the-loop's interface** (same socket) and is honest about
   `:policy-holes` (the fold must not hide what it couldn't wire).

*Register (from the card): #2 already works, so nothing here is load-bearing for closing the loop — this
is the efficiency frontier. Keep the honest "we do not claim to have solved it" tone; a well-argued "the
ansatz is not worth it yet, here's why" is a full success for this mission.*

---

## MAP — the territory (claude-4, 2026-07-01, in progress)

*Method: three parallel read-only sweeps (GFlowNet · JAX · FOLD-GAE/DarkTower/CLean), synthesized here.
Goal: turn the ~120-file scatter into a live map — what exists, what's stale, what impl #3 would reuse
vs build.*

### M1 — GFlowNet landscape
~29 files (the "~43" included dupes + data artifacts). **The only live GFlowNet code is the
cascade-SAMPLER, not a cascade→WIRING folder.** `E-cascade-sampler-sampler/s4/train_cascade_gfn.py`
trains a trajectory-balance GFN to sample budget-6 pattern-sets (cascades); it **trained** (1200 TB
steps, seed 20260612) and the flight-grounding probe showed +0.21 lift (n=1) — proving the R2
flight→reward→policy channel is real. **But it FAILS the realized-G yardstick**: greedy-ε beats it and
the TB policy stays near-uniform (114–125 distinct/128), because the reward landscape (C-wholeness /
metric-C v1) is **too FLAT to concentrate a policy**.
- **No GFlowNet code targets wiring generation (impl #3 / FOLD-GFLOWNET) — confirmed none exists.**
- **Reusable for impl #3:** the config machinery, the proxy-adapter pattern (state→reward decoupling),
  the trajectory-balance loss (trains *when signal exists*), and the flight-grounding probe. **Not
  reusable:** the contest harness + the C-wholeness proxy (cascade-specific).
- **The strategic insight (load-bearing):** *sampling is not the bottleneck for folds — SIGNAL is.* The
  sampler's flat-reward failure is the same wall an embedding fold would hit, **unless the embedding
  space provides steeper signal than symbol-level scores** — which is *the* untested bet.

### M2 — JAX landscape
~77 mentions resolve to **3 live load-bearing clusters + scatter** (no impl-#3 JAX exists; `FOLD-JAXREFINE`
is a card, not code):
- **M-differentiable-substrate** (`futon6/scripts/diffsub_emit.py`) — policy-prior moves via JAX autodiff
  on a **soft-adjacency** matrix + band-satisfaction loss; production, wired to rollout. **This is the
  closest impl-#3 exemplar** — soft-adjacency optimization over an embedding metric is exactly an
  embedding-fold building block.
- **M-differentiable-code** (`futon6/scripts/code_diff_jax_pilot.py`) — the same pattern on the
  code-graph (PoC; seeds the above).
- **M-aif2 WM-shadow** (`futon2/scripts/wm_shadow_pilot.py`) — differentiable belief (Fisher-Rao on the
  simplex); closed pilot whose finding *relocates the gap to perception* (the WM sees only aggregate
  channels). Tangential but relevant if the fold needs per-entity signal.
So the JAX scaffold impl #3 would reuse (soft-adjacency autodiff) **already exists and works** — the
ansatz is not greenfield on the machinery, only on the target + corpus.

### M2 — JAX landscape
_(pending sweep — what JAX is load-bearing for vs experimental; any embedding/GAE-fold JAX.)_

### M3 — FOLD-GAE re-test readiness + DarkTower CT + CLean bridge

**FOLD-GAE (the stale −0.058) — doubly weak, so the failure carries little weight.** Code:
`futon5/tools/embed/jax-learning/it2_link_prediction.py` (commit `2b31d2a`, 2026-06-03). The −0.058
"GCN < heuristics" was a GAE (GCN encoder 32→16) vs link-prediction heuristics **on a TOY stochastic
block model** (60 nodes, 3 communities) — **not real wiring data at all.** So the negative result is
stale *and* toy-corpus; it says almost nothing about the actual bet. Re-testing needs a **real graph
corpus** (mission/code wiring), which doesn't exist yet (see the blocker below).

**DarkTower CT target — PRODUCTION, not design-only (the strong leg).** Real Lean at
`~/code/mathlib4/DarkTower/`: `TypedHole/Fill/Comb/Discharge/Coverage/BV` all implemented + proven
against Mathlib, with **0-sorry worked examples** (`MissionExample`, `PaperExample` in `Examples.lean`).
So impl #3's target schema — *a wiring is a BV-copar of typed-holes* — is **well-specified and real**,
not a hand-wave. This is the most mature piece.

**CLean bridge — works for math proofs, not yet missions/code.** `futon6/scripts/clean_to_lean.py`
(36K) converts `.clean.edn` outlines → `mathlib4/DarkTower/CLeanProofs.lean` (0-sorry gated); spec
`futon6/holes/excursions/E-clean.md`. It works for ~10 hand-lifted APM (arXiv-math) proofs. **NOT yet
ported to missions/code** — that port is the sibling [[E-darktower-wiring]], and it is the thing that
would *produce the (cascade→wiring) corpus* impl #3 must learn from.

**THE SEQUENCING BLOCKER (the load-bearing MAP finding).** impl #3 cannot even be *tested* until two
upstream pieces land: **(i)** CLean ported to missions/code → a real wiring corpus (E-darktower-wiring /
E-wiring-diagram-corpus), and **(ii)** the substrate-2 **(have→want) join** the fold's magnet conditions
on ([[M-populate-substrate-2]] D4 + [[E-have-want-pairs]] Q-B — and Q-B just measured that magnet at
~30% mechanical). Both are live front-line work, not greenfield. So the ansatz is **corpus-blocked**,
not idea-blocked — which is exactly what makes it correctly *deferred*.

### M4 — Synthesis: the verdict-shape and the smallest discriminating experiment

**The MAP verdict: "deferred" is correct — but the reason is now precise.** The ansatz is
**corpus-blocked, not idea-blocked**, and behind the corpus sits a deeper risk the sweep surfaced.

Three facts line up:
1. **Target is real and strong** (M3): DarkTower CT is production Lean; impl #3 has a well-specified
   target (`:wiring` = BV-copar of typed-holes). Not a hand-wave.
2. **Scaffold exists** (M1+M2): the JAX soft-adjacency autodiff (M-differentiable-substrate) and the
   GFN config/proxy/flight-probe machinery are live and reusable. Not greenfield.
3. **But the corpus doesn't** (M3): impl #3 must learn from a **(cascade→wiring) corpus** that only
   exists once **CLean is ported to missions/code** (E-darktower-wiring) and the substrate-2
   **(have→want) join** is populated (M-populate-substrate-2 D4; Q-B just measured that magnet at ~30%).

**The deeper risk (from M1):** even with a corpus, *signal — not sampling or machinery — is the
bottleneck.* The GFN sampler already failed on flat symbol-level reward. So the whole ansatz reduces to
one empirical question: **does the embedding space carry steeper signal for cascade→wiring than
symbol-level heuristics do?** The stale FOLD-GAE −0.058 does *not* answer this — it ran on a toy SBM,
not real wiring.

**The smallest discriminating experiment (this is the deliverable):**
1. **Seed a tiny real corpus:** hand-lift ~10 mission/code wiring diagrams through CLean (as was already
   done for ~10 APM proofs) → a small but *real* (cascade→wiring) set.
2. **Re-run FOLD-GAE link-prediction on THAT corpus** (not the SBM): does embedding-space link-prediction
   recover held-out wiring edges **better than the symbol-level heuristics** (common-neighbour /
   Adamic-Adar)?
3. **Read:** positive lift on real wiring ⇒ the ansatz has legs (build FOLD-GFLOWNET/JAXREFINE on the
   existing scaffold). Flat/negative on real wiring too ⇒ **the embedding fold is not worth it**; impl
   #2 (LLM-turn) stays the realization, and that is a full, publishable success for this mission
   (a measured "no", echoing the honest register of the sequel paper).

**So the honest one-line verdict:** *the embedding fold stays deferred until CLean→missions produces a
real wiring corpus; the one test that decides it is GAE-link-prediction-on-real-corpus vs symbol-level
heuristics; and the prior from the GFN result is that signal, not machinery, is the risk.*

---
**MAP complete.** Owner still TBD. Feeds the sequel paper's honest-hole story
([[project_sequel_paper_closing_the_loop]] — FOLD-ANSATZ is the notebook's deferred-🗓 card).

## ARGUE — why *now* ≠ the −0.058 attempt, and what we test at which scale (Joe + claude-4, 2026-07-01)

**The resource delta (why the earlier attempt doesn't bind).** The −0.058 GAE ran on a **toy SBM**, no
real corpus, no pattern-use history. What we now have (verified live):
- **turn→pattern** correspondence; **session→pattern** = **pùtuàn** diagrams per mission (the *realized
  weave*: `thread-orbits.edn` / `session-threads.json` / `reflow/*/orbit.edn`);
- **mission scopes ingested on demand**; a **well-populated substrate-2**;
- **C-cascade-real pattern-use history: 1783 mission↔pattern edges over 185 missions**, tagged
  `applied` vs `candidate` + cosine — *which patterns were actually used per mission and per cluster*;
- working folds **#1 (classical) and #2 (LLM)** that turn a cascade→sorry→wiring — so we can **generate**
  wiring diagrams on demand.

**So the corpus is GENERATABLE — two independent sources** (this is the crux, and it defuses the
circularity worry):
- **(S) synthetic:** run #2 over real (have,want) pairs (from the 1783 `applied` edges) → (cascade→wiring)
  pairs. This is [[E-wiring-diagram-corpus]]. *Caveat: a #2-distilled corpus teaches #3 to mimic #2.*
- **(R) realized:** the **pùtuàn/thread orbits** = the wiring that *actually wove* in real sessions —
  ground truth **independent of any fold**. This is what resolves the circularity (claude-10's
  thread↔cascade duality: realized weave vs predicted wiring).

**The hypotheses, separated — which part, at which scale, and what each would show.** Do NOT conflate
them; they nest (cheap gate before expensive train).

| H | claim | corpus / scale | read |
|---|-------|----------------|------|
| **A. Generation feasible** | we can mint valid (cascade→wiring) pairs at volume | S + R; **seed 10–50** first, validate each is **0-sorry** via DarkTower CT, then scale to hundreds | can we produce valid wiring at throughput? (mostly de-risked — folds work; measures validity-rate + cost). Tests the *operation*, not the ansatz. |
| **B1. Symbol-graph signal (NOW)** | the substrate-2 **embedding predicts real substrate-2 relations** better than similarity/popularity heuristics | the **live substrate-2 symbol graph** — 1783 mission↔pattern edges (use `applied` as ground truth; `candidate` was cosine-generated ⇒ circular), + arrows/clusters. **No new corpus needed.** | GAE link-prediction vs common-neighbour/Adamic-Adar/cosine. **This is the literal −0.058 re-run, but on the REAL substrate-2 graph, not a toy SBM.** Flat ⇒ the embedding can't predict the graph it was built from ⇒ **strong negative prior, ansatz likely dead**. Signal ⇒ *necessary-not-sufficient* green light for A→B2. **RUN FIRST — it's free.** |
| **B2. CT-wiring signal (the real target, corpus-blocked)** | the embedding predicts the **CT abstract wiring** (CLean/DarkTower typed-hole/comb structure) — the fold's actual `:wiring` output | needs **A** (only ~10 APM CLean diagrams today; none on missions) | link-prediction / structure-recovery on the CT wiring graph. The real thing the fold emits. Gated on A generating the corpus. |
| **C. Efficiency ansatz** | impl #3 **distills #2 cheaper** (fewer LLM calls / faster), **0-sorry-preserving** | S-corpus at **training size (100s)**; only if B passes | fidelity-to-#2 on held-out + cost/latency vs #2 + CT type-checks. *Tests distillation-efficiency — the ansatz's actual claim, NOT independent discovery.* |
| **D. Realized-weave validation** (optional, deepest) | #2's predicted wiring matches the **pùtuàn realized weave** | R-corpus (sessions with thread-orbits) | does the fold predict what actually wove? Validates S against reality; the honest ground-truth test. |

### ARGUE §2 — design for information gain, not kill-gates (Joe, 2026-07-01)

**The principle.** Don't frame an experiment as "kill or continue" unless the argument backs it. Design
each experiment for *expected information gain about THE DECISION* (build impl #3, or not), and label its
epistemic status honestly:
- **falsifier** — a negative result substantially lowers P(ansatz worth it);
- **confirmer / demo** — we're fairly sure it succeeds; it builds the positive case (paper) + often
  *enables* later tests; low surprise, high value;
- **prior-shifter** — moves belief but decides nothing on its own.
An experiment whose outcome we can already predict has low info-gain *unless it unblocks something*.

**What we are CONFIDENT about (foundations → run as confirmers/demos, lead with these):**
- the (cascade→CT-wiring) mapping **exists and is computable** — #1 (classical) and #2 (LLM) already do
  it; so "is it learnable at all?" is *not* the open question;
- we can **generate** the corpus (S via #2) and have **independent ground truth** (R via pùtuàn);
- the CT target is **real** (DarkTower production Lean).

**What is genuinely UNCERTAIN (where info-gain lives):**
- **(ii)** is the *embedding space* a good substrate for cascade→CT-wiring — or is the structure
  fundamentally *symbolic*? (Both the −0.058 and the GFN flat-reward whisper "maybe symbolic.")
- **(iii)** can an embedding fold be *cheaper than #2* without losing soundness?

**Reclassifying the experiments by status (this replaces the "B is the kill-gate" framing):**
- **A-small (generate ~10–30 real cascade→CT-wiring pairs via #2)** — **CONFIRMER + ENABLER**, run FIRST.
  We're pretty sure it works; its value is that it (a) *demonstrates the generation capability* for the
  paper, (b) validates each diagram **0-sorry** via DarkTower, (c) **unblocks B2**. Not a gate.
- **B2 (is the generated CT wiring cheaply learnable / embeddable?)** — **the real FALSIFIER**, and the
  high-info experiment; gated on A-small. On the ~10–30 corpus: does a cheap model (embedding retrieval
  / GAE) reproduce #2's wiring *beyond a dumb baseline*? A clean negative here IS decision-relevant.
- **B0 dumb-baseline (value-floor, cheap, high-info on (iii)):** does *nearest-cascade retrieval →
  reuse its wiring* already reproduce #2? If **yes**, efficiency is trivially achievable without a fancy
  fold (a cheaper positive — the ansatz "wins" boringly); if **no but learnable**, that gap is exactly
  where the embedding fold earns its keep. Bounds the ansatz's value from below.
- **B1 (substrate-2 symbol-graph link-prediction)** — **DEMOTED to an optional prior-shifter**, NOT a
  kill-gate. Honest: B1-flat does *not* kill B2 (pattern-*use* ≠ CT-*wiring*). It only reads whether
  substrate-2 embeddings carry *any* relational signal — a weak prior on (ii). Cheap; worth a look;
  decides nothing.

**Revised plan (high-info, no overclaimed kill-gate):** lead with **A-small** (confident + enabling +
a paper demo), then **B0** (value-floor) and **B2** (the real fork) on that corpus. B1 is an optional
side-probe. The honest success conditions are symmetric and both high-value: **B2 shows learnable
embeddable structure beyond B0** ⇒ build the fold; **B2 flat on a *real* corpus** ⇒ a measured,
publishable "the embedding ansatz isn't worth it" (evidence about the real problem, unlike the toy
−0.058). Neither outcome is a disappointment; both are information we don't have today.

*Later phase (deferred, Joe): "flow" cascades to OPTIMIZE them (GFlowNet/flow layer) before folding —
improves the input cascades, orthogonal to A/B. Not in A-small.*

## A-small — the first experiment card (GO, 2026-07-01)

**Goal:** produce **~10–30 mission-grain `(cascade → CT-wiring)` pairs**, each **structurally 0-sorry**
(the wiring type-checks as a DarkTower BV-copar of typed-holes; the mission's real gaps remain *typed*
holes — that's the honest point, not a failure). This is the first port of CLean to missions, and the
corpus B0/B2 run on.

**Tooling — VERIFIED runnable this session:**
- gate: `lean` 4.16.0 (elan) + `futon6/.venv`; `clean_to_lean.py --mode standalone` **compiles 0-sorry
  with core lean (exit 0)** — the cheap structural check, no mathlib build; `--mode real` renders the
  full proofs. Beachhead already exists: `mathlib4/DarkTower/FirstFlightsExample.lean`.
- fold→CLean bridge: `futon2/src/futon2/aif/fold_clean.clj`; classical impl #1 `fold_engine.clj`
  (`E-fold-engine-wiring-GENERATED.edn` shows it already emits wiring); #2 = an LLM turn authoring the
  `.clean.edn`.
- CLean format (from `futon6/holes/clean/*.clean.edn`): `{:clean/seq [methods…] :clean/boxes [{:id
  :method :text :consumes :produces :hole {:kind :sorry :discharge :satiety :wanted}}] :clean/wires […]}`.

**Pipeline (per pair):**
1. **Cascade** — take a mission's **`applied` patterns** as the realized cascade (the pùtuàn — from
   `/api/alpha/cascade-real/graph`, avoids the `cascade_construct` venv and the #2-circularity), *or*
   construct one via the magnet (S-source) where preferred.
2. **Fold → `.clean.edn`** — method-spine of typed boxes with `:consumes/:produces` wiring + typed
   `:hole`s (impl #1 `fold_engine` for classical, or an LLM turn for #2).
3. **Gate** — `clean_to_lean.py` render → `lean` compile → structural **0-sorry** (wiring well-typed);
   content holes preserved as typed sorries.
4. **Collect** `(cascade, .clean.edn, gate-result)` → the A-small corpus (input to B0/B2).

**Scale:** **n=1 pilot FIRST** (one real mission → applied-patterns cascade → author `.clean.edn` →
standalone gate) to prove the pipeline end-to-end and mint the first mission-grain wiring diagram; then
10–30. **Owner:** pilot by claude-4; scale-out TBD. Flag if a pair won't gate (a real finding about
fold-#2 output validity, not just an error).

### A-small pilot RESULT — autoclock-in ✅ (claude-4, 2026-07-01)

**PASS. The first mission-grain CLean CT-wiring exists and gates 0-sorry.**
- Artifact: `futon6/holes/clean/autoclock-in.clean.edn` — folded from the mission's live 10-pattern
  cascade into a 6-box wiring. Faithful to the *real* (messy) construction: `s3` produces a **spoiled**
  single-active guard (order-dependent RAM retract) and `s4` is a **Gu (iching/hexagram-18) REPAIR** box
  that consumes the spoiled state and produces order-independence — the "complex update" Joe flagged,
  represented honestly, not hidden.
- Gate: `clean_to_lean.py --mode standalone --only autoclock-in` → `lean` → **clean compile, exit 0**.
  The wiring is well-typed; `holeType s3/s4 = Obligation` (the two genuine holes), all else `Empty`;
  `discharges s6 = exitCriterion2Reconstitution`; BV congruence (spine reassoc + reading copar) checks.
- **A genuine A-small finding + fix:** the converter emitted `namespace CLeanProof_autoclock-in` — a
  hyphen is illegal in a Lean identifier, and the APM ids never had one but **every mission id does**.
  One-line fix in `clean_to_lean.py` (sanitize `pid` hyphens→underscores for the namespace). This is
  exactly the kind of gap the first mission-grain port was meant to expose.

**What the pilot proves for the programme:** the "confirmer + enabler" landed — the generation pipeline
(cascade → fold-by-LLM-turn → CLean → 0-sorry gate) **works end-to-end on a real mission**, produces a
valid CT wiring, and is now a paper demo. It unblocks the scale-out and thereby B0/B2.

### A-small SCALE-OUT — 10-mission corpus ✅ COMPLETE (claude-4 + 3 folding agents, 2026-07-01)

**The corpus B0/B2 run on now exists: 10 real (mission-cascade → CT-wiring) pairs, all 0-sorry-gated
(independently batch-gated by claude-4; author≠folder).** Folded from each mission's live cascade +
doc; typed holes correspond to *genuine* gaps, not fabrication; strong macro-shape diversity.

| # | leaf (`.clean.edn`) | boxes | holes | macro-shape |
|---|---|---|---|---|
| 1 | autoclock-in | 6 | 2 | build-then-repair-then-discharge (Gu repair) |
| 2 | stepper-calibration | 6 | 3 | gate-then-triage-then-discharge |
| 3 | patterns-done-right | 5 | 2 | graduation-ladder-then-discharge |
| 4 | state-snapshot-witness | 5 | 1 | linear-build-then-discharge |
| 5 | agency-rebuild | 6 | 3 | build-then-repair-then-discharge (split-brain repair) |
| 6 | invariant-queue-unstuck | 5 | 4 | build-and-discharge |
| 7 | single-entry-point | 5 | 2 | audit-derive-then-discharge (folded as a *derivation*, open fork) |
| 8 | f6-ingest | 6 | 3 | fan-out-then-aggregate |
| 9 | pattern-ingest | 6 | 4 | shape-gate-then-enrich-then-demote-authority |
| 10 | a-sorry-enterprise | 6 | 4 | active-inference-closure-loop |

~56 typed boxes, 28 typed holes, **8 distinct macro-shapes** — the structural variety B2 needs to have
something to discriminate. The fold handled honest edge cases well: two missions carry *repair* boxes
(the construction was corrected mid-flight), and two not-yet-landed missions were folded as *derivations*
with open `:queryAnswer` forks rather than faked as complete.

**A-small is DONE.** Next: **B0** (dumb nearest-cascade-retrieval baseline — does it already reproduce
the wiring?) and **B2** (embedding link-prediction on this corpus vs B0) — the real fork, now unblocked.

## A-next — the 3-PART fold: cascade → SORRY → wiring (Joe, 2026-07-01)

**The concern (right, and important):** A-small folded **cascade → wiring directly**, conflating the
*local* holes inside a construction with **the sorry that defines the INTERFACE the wiring is built to
fill**. The genuine lifecycle is **cascade → sorry → wiring**: the substrate-2 *sorry* is the typed-hole
interface (ports known, construction absent — a DarkTower `TypedHole`), and the wiring is its
*fill/discharge*. Skipping sorry-identification leaves the wiring's interface implicit/invented.

**CORRECTED model of the sorry (Joe, 2026-07-01 — the logged `:sorry` entries are *useful* but not
this).** The right model is `E-fold-engine` (`futon3a/holes/labs/M-memes-arrows/E-fold-engine-{sorry,wiring}.edn`):
- **The sorry = the interface, as a TYPED SIGNATURE over real substrate-2 endpoints.** Shape:
  `{:want-signature "fold : cascade-dict -> (Wiring,[PolicyHole])" :hungry-for <codomain>
    :endpoints [{:role :have|:want :ref <substrate-2 entity> :type <t> :in-map bool}] :kind <hole-typology>}`.
  The have-endpoints are the existing inputs; the want-endpoint is the codomain to pin. It is a
  `code/v05/sorry` *hyperedge over real endpoints*, not a prose title.
- **The wiring = the construction, `:for-sorry <that sorry>`**, and the **ACCEPTANCE CHECK (Joe): the
  wiring's `:terminals {:in :out}` = the sorry's mapped (have, want) endpoints.** Each box cites its
  cascade-pattern warrant.
- **sorry + wiring = the mission's formal spec** (interface/type + construction/term). A "pure" wiring
  with no sorry-interface is half a spec — untethered, liable to go wrong (exactly A-small's flaw).

**A-next fold (3-part), per mission — produce the PAIR (E-fold-engine template):**
1. **cascade → sorry** — mint the interface: a `:want-signature` + typed `:endpoints` (`:have` = the
   mission's real substrate-2 inputs, `:want` = the codomain), grounded (`:in-map` checked).
2. **sorry → wiring** — construct the wiring whose `:terminals = the sorry's endpoints`, boxes warranted
   by cascade patterns, undecidables surfaced as `:policy-holes` (not laundered into boxes).
3. **ACCEPTANCE** — terminals = endpoints (the checkable have/want match), *plus* the CLean 0-sorry
   structural gate on the wiring. The pair, not the wiring alone, is the artifact.

**Why this upgrades the experiment (not just faithfulness):**
- **B2 becomes better-posed** — the target is "predict the *fill* given a KNOWN typed sorry-interface,"
  constrained by `:endpoints`, not free-form wiring.
- **Two embedding probes, not one** — (a) cascade→sorry: does the embedding pick the right substrate-2
  (have, want) endpoints for the interface? (b) sorry→wiring: does it construct the fill? Probe (a) is
  pure substrate-2 retrieval — plausibly the *more* embeddable half.
- **A real checkable acceptance** — terminals = endpoints is a hard gate the "pure" wirings never had;
  the endpoints are *real substrate-2 entities*, so the fold is tethered to the live map, not free text.

**A-small is not wasted:** its 10 wirings are the construction half; A-next *completes the spec* by minting
the sorry-interface each was implicitly building toward and checking terminals=endpoints.

**Pilot A-next (NOT the drawbridge `:sorry` — Joe: useful, not canonical):** follow the E-fold-engine
template on a mission whose real substrate-2 (have, want) endpoints I can name. Candidate: **autoclock-in**
— have-endpoints are live (`clock_store`, the canonical `<repo>-d/mission/<id>` nodes, `clock-lineage`
ns); want-endpoint = the durable reconstitution (Exit-criterion-2). Mint its sorry (`:want-signature`
`autoclock-in : agent×session×target -> DurableLineage`, typed endpoints, `:in-map` checked), then check
its A-small wiring's terminals against those endpoints. That yields the first *complete* (sorry+wiring)
mission spec — a far stronger artifact than the pure wiring.

### A-next BLINDED STUDY (Joe, 2026-07-01) — the real probe-(a) experiment

**The move:** a done mission's interface can be recovered POST-HOC (doc + code + git before/after +
XTDB), giving **ground truth for cascade→sorry** — so we run a *blinded* study: how well does a
reconstruction from the **cascade alone** recover the **empirical** sorry?
- **Ground truth (sealed):** `labs/A-next-autoclock-in/autoclock-in-sorry-EMPIRICAL.edn` — recovered from
  the 7 commits (`8a23ec9`…`f409de9`), the code surface, XTDB endpoints, the doc. want-signature
  `clock-in : (agent,session,target) → DurableLineage`; `:have` = agent nodes / canonical mission nodes /
  `clock/clocked-on` / `clock_store` ns / `held/on-mission`; `:want` = the reconstitute output; typed-holes
  = order-independence (Gu), canonical-identity, repl-sync.
- **Blinded reconstruction (in progress):** a fresh agent, forbidden the doc/code/git/ground-truth, mints
  the sorry from ONLY the 10 cascade patterns → `autoclock-in-sorry-CASCADE.edn`.
- **Score (Phase 3):** endpoint overlap (have/want) + want-signature match + typed-hole recovery — *how
  much of the interface does the cascade determine?* Probe (a) with real ground truth; a genuine result
  either way (high recovery ⇒ cascades carry the interface; low ⇒ the interface needs more than the
  cascade — itself a finding about what a cascade under-determines).

## THE ANSATZ METRIC — speed + laptop-only compute (Joe, 2026-07-01, RE-CENTER)

**Stop scaling examples.** A-small (10 wirings) + A-next (the blinded sorry study) have made the fold's
*possibility and shape* clear — but they used a **cloud LLM (claude-4) as the fold**, which is exactly
what the ansatz proposes to REPLACE. Generating more LLM-folds does not test the ansatz; it re-proves
impl #2.

**The ansatz claim, precisely:** can a **laptop-only, fast** mechanism (CPU embeddings + JAX +
GFlowNet-or-similar — *no LLM API, no GPU cluster*) reproduce the fold that the LLM-turn does? The
success axis is **SPEED × laptop-only**, not more coverage. Quality/usefulness is secondary for now
(revisit later); the bet is *efficiency*.

**So the corpus is now FIXED (enough), and the work turns to the laptop mechanism.** Reframed experiments:
- **B0-laptop (the value-floor + first speed number):** embed each cascade (CPU MiniLM/BGE, ms) →
  nearest-cascade retrieval → reuse its wiring/sorry. Measure **latency/fold on the laptop** and
  fidelity vs the LLM-fold outputs we already have. If dumb retrieval is fast AND good-enough,
  efficiency is nearly free.
- **B2-laptop (the real bet):** a laptop JAX model (reuse the `M-differentiable-substrate` soft-adjacency
  autodiff + the GFN config scaffold from §MAP — both already CPU/laptop) that predicts the wiring's
  **grounded endpoints** given cascade + substrate-2 neighbourhood (the sharpened probe from the blinded
  study). Metric: **ms/fold on laptop** + endpoint-recovery vs the empirical sorries.

**Exit criterion 1 sharpened:** the verdict is "a laptop-only embedding fold can/can't reproduce the
LLM-fold FAST (target: ≤ seconds/fold, CPU-only)" — with the speed number and the fidelity, on the
FIXED corpus. A measured "laptop-only can't match it, here's the wall" is still a full success.

## CORRECTION (Joe, 2026-07-01) — the target is patterns→(sorry, wiring), NOT cascade-retrieval

**B0 is OUT.** "Finding cascades in an embedding of cascades" (nearest-cascade retrieval) is a lookup,
not the fold — not worth trying. And cascade-*induction* from a pattern embedding is **already shown**
(`cascade_construct`: patterns→cascade via the MiniLM magnet). So neither the input side nor a retrieval
shortcut is the open question.

**The open question = the two GENERATIVE maps out of patterns/cascades, laptop-only:**
- **f1 : patterns/cascade → SORRY** (substrate-2 sense) — predict the real substrate-2 **endpoints**
  the interface is over (the `:have`/`:want` nodes). This is **link-prediction over substrate-2**
  (embed cascade + candidate substrate-2 nodes → score which are the endpoints). Ground truth exists:
  the empirical sorries (A-next). **This IS the sharpened probe (a)** the blinded study pointed at —
  the cascade gives the interface TYPE; can embedding + substrate-2 supply the GROUNDED endpoints? — and
  it is the *meaningful* re-run of FOLD-GAE (on cascade→substrate-2 endpoints, not a toy SBM).
- **f2 : (cascade, sorry) → WIRING** (DarkTower sense) — generate the CT typed-graph (TypedHole/Comb/
  Discharge / BV-copar). The harder, generative half — GFlowNet/GAE structure generation.

**First real ansatz experiment = f1 (sorry-grounding), laptop-only:** CPU-embed the cascade + the
substrate-2 candidate endpoints → link-prediction → predicted `:have`/`:want` endpoints; score
endpoint-recovery vs the empirical sorries + report ms/fold. It's tractable, grounded, embeddable, and
it's the exact gap the blinded study measured (grounding, which the cascade under-determines). f2
(wiring generation) follows only if f1 shows the substrate-2 embedding carries endpoint signal.

## f1 — GO (Joe, 2026-07-01): structural embedding, "fix" not "kill" on a flat result

Prior bad luck with structural embeddings ≠ a reason not to try on the **now-much-richer** substrate-2
data. And crucially: **a flat f1 is a FIX criterion, not a KILL criterion** — structural embedding
demonstrably works (Rob's **pgvector-for-Lean** works very well), so a negative here means *wrong
configuration on this data*, to diagnose and fix, not the death of the approach. (Same spirit as the
earlier no-overclaimed-kill-gate discipline.)

**f1-v0 (numpy-only, laptop):** matrix-factorization SVD over the mission×neighbour incidence from
`/api/alpha/cascade-real/graph` (SVD *is* a structural embedding — the linear-GAE limit). Test: (a) bulk
held-out mission↔pattern link-prediction, AUC vs a popularity baseline; (b) autoclock-in endpoint
recovery — from its cascade, rank its true non-pattern endpoints (cluster `01-invariant-queue`, arrow
`→autoclock-in-argue`, the capability hole). Report recovery + latency. Upgrade to node2vec/GAE only if
v0 shows signal; if flat, diagnose (the fix, not the kill).

### f1-v0 RESULT ✅ (claude-4, 2026-07-01) — structural embedding recovers endpoints on rich substrate-2

Script `labs/f1_structural_embed.py` (numpy-only, laptop, **0.43s total / 67ms embed**):
- **(a) held-out mission↔pattern link-prediction:** **SVD-AUC 0.769 vs popularity-AUC 0.698** — real
  structural signal beyond popularity (231 missions × 550 items, 2333 edges).
- **(b) autoclock-in endpoint recovery:** from its cascade, its 3 true non-pattern endpoints rank
  **0 / 14 / 16 of 550** (cluster `01-invariant-queue` at rank 0) — **top-20 hit-rate 1.00**.

**Read:** unlike the stale FOLD-GAE −0.058 (a *toy SBM*), on the **now-rich real substrate-2** a plain
structural embedding **does** carry endpoint signal, laptop-only and sub-second. This is the meaningful
FOLD-GAE re-run, and it's POSITIVE — it validates f1=structural-embedding and vindicates Joe's "richer
data should work."

**Honest caveats (→ the fix/scale path, not a kill):** n=1 endpoint-recovery (3 endpoints); AUC lift over
popularity is modest (0.77 vs 0.70); these are the substrate-2 *graph* endpoints (cluster/arrow/hole),
not yet the full code-level interface (`clock/clocked-on`, `clock_store` ns) which aren't nodes in this
graph slice; SVD is a *linear* embedding. **Next:** (i) more empirical sorries via the post-hoc mining
(recovery *curve*, not n=1); (ii) add code/ns + hyperedge-type nodes so the code-level endpoints are
predictable; (iii) node2vec/GAE upgrade if the linear ceiling bites. Then f2 (GFlowNet + 0-sorry reward).

### f1-v1 CURVE RESULT — honest correction: naive embedding ~ties/loses to popularity (claude-4, 2026-07-01)

Turning n=1 into a curve **corrected the n=1 over-read.** Held-out non-pattern-endpoint recovery:
- *with* the mega-hole: SVD recall@20 **0.43** vs popularity **0.53** (pop wins).
- *dropping* the one degenerate hole (targets 202 missions) + degree-correcting: raw-SVD **0.26**,
  PPMI-SVD **0.26**, popularity **0.39** (recall@20); MRR ~tied (~0.16).

**So the naive structural embedding does NOT beat popularity for endpoint recovery.** The autoclock-in
n=1 (top-20 1.00) was *unrepresentative* — flattered by popular endpoints (its cluster/hole are
high-degree). Honest verdict: **weak structural signal on this graph-slice/target.**

**Per fix-not-kill — the diagnosis + fix path (NOT a kill):**
- **Why it's weak here:** the mission↔(cluster/hole/arrow/held) graph is *thin* and the endpoints are
  *popularity-dominated* — little residual structure for a *linear* (SVD/PPMI) embedding to find.
- **This converges with the Rob prior-art point:** pgvector-for-Lean works because it embeds **CODE-as-
  graphs** (rich structure); our thin mission-neighbour slice is the wrong graph. The fix is to embed the
  **richer code-level structure** — which is *also* what the real interface endpoints (`clock/clocked-on`,
  `clock_store` ns) need (they aren't nodes in this slice).
- **Fix directions (in order):** (i) enrich the graph with code/ns + more relation types (the mission→code
  map Joe noted is in XTDB) so the *real* endpoints are predictable; (ii) a deeper/nonlinear embedding
  (node2vec random walks / a real GAE) — the linear SVD is the shallow floor; (iii) only then judge f1.

### STOP-THE-LINE (Joe, 2026-07-01) — f1 was on the WRONG graph; the code-graph exists

Joe: "weird that substrate-2 didn't have a code-graph — I thought we had." **It does.** Per
`futon1a/README-census.md` + direct census: **`code/v05/var` 125,730 · `calls` 89,304 · `contains`
73,628 · `edits` 191,485 · `test` 29,802 · `namespace` 7,053** — a full call/containment/coverage graph.

**My f1 used `/api/alpha/cascade-real/graph` — the CASCADE slice only (patterns/clusters/holes/arrows,
~550 nodes). It never touched the code-graph.** So the f1-v1 "structural embedding ties popularity"
result is **RETRACTED as a claim about substrate-2** — it measured an impoverished slice, not the ~125k-var
code-graph. Joe did NOT hallucinate; I under-sampled.

**Consequence:** the *right* f1 (structural embedding over the real code-graph — the pgvector-for-Lean
setup, 125k vars + 89k call edges) is **UNRUN.** The autoclock-in interface endpoints (`clock_store` ns,
`clock_lineage` vars) live in THIS graph. The re-run needs the **mission/cascade ↔ code linkage** (likely
via `code/v05/edits` (agent/session→var) + `mined-move` (mission→mission) + `sorry :related-missions`) —
the first thing to establish. This is the genuinely rich structural test the ansatz deserves.

### code-graph f1 RESULT — third consistent negative (shallow embedding loses to degree)

Ran on the REAL futon3c call-graph (`labs/f1_codegraph.py`; sparse randomized-eigen, 229ms embed, 14s
total incl. 9560-var pull): **structural-embed AUC 0.591 vs degree/popularity AUC 0.775** — shallow
structural embedding **loses to degree** (recall@10 0.17). (The dense 9560² SVD hung a prior run; the
sparse fix works.)

**Consistent pattern across all three tests:** thin-slice SVD, thin-slice PPMI, and now the rich
code-graph spectral — **a *shallow* structural embedding does not beat popularity/degree** on
substrate-2's *degree-dominated* graphs (hub functions/patterns/holes get predicted by "popular").

**Fix-not-kill diagnosis (honest, bounded):** the CHEAP version of the ansatz (shallow numpy embedding)
is exhausted and insufficient. The *fair* test needs a **trained** embedding — node2vec random-walks /
a real GAE with negative sampling — which is what **Rob's pgvector-for-Lean actually uses**, and it
needs ML tooling (gensim/torch, currently absent) + real time (days), not a laptop-numpy afternoon. So
the honest finding is: *shallow structural embedding is insufficient on substrate-2; the ansatz's real
test is a trained code-embedding — scoped, motivated future work.* Per the mission's own exit criteria,
a well-argued "not proven yet, here's exactly what it needs" is a full success — and it stops the
ML-tuning rabbit-hole on the one-week clock.

### CORRECTION (Joe, 2026-07-01) — CLean is PHASE 3; phase 2 still needs substrate-2

My enthusiasm for stage ⑧ (`clean_structure_embed.py`, CLean→33-d structure vector→pgvector) was
**unwarranted for the problem at hand.** Placing it correctly in the 3-fold:
- **Phase 1** patterns → cascade — DONE (`cascade_construct`).
- **Phase 2** cascade → **SORRY** (the substrate-2 interface / real endpoints) — **THIS is f1, still OPEN.**
  My three negatives are all here. It needs **substrate-2** (the code-graph / endpoints). **CLean does not
  substitute for it.**
- **Phase 3** sorry → **WIRING** (CLean/DarkTower comb) — stage ⑧ is the phase-3 tooling (built:
  `iatc_to_clean` 8.1, `clean_structure_embed` 33-d 8.3, `clean_to_lean`+pgvector export 8.4 = the Rob
  ingestion contract). **But** even here the structure-embedding is weak on the mission corpus: ran it on
  the 10 A-small wirings + 7 APM (17 proofs, structure (17,96)) — mission-wirings' structure nearest-
  neighbour sims are ~0 (autoclock-in↔agency-rebuild, both build-then-repair, did NOT cluster; the APM
  proofs cluster better, e.g. cover-estimate pair 0.32). So phase-3 structure embedding needs work too.

**Net: phase 2 (cascade→substrate-2-sorry) remains the crux and is genuinely open** — shallow structural
embedding underperforms popularity (×3), and it's a substrate-2 problem, not a CLean one. Stop conflating
the phases. The fix (a *trained* substrate-2 embedding) is unproven and is the real next question.

## Pipeline cards ↔ experiment steps (crosswalk) + GFlowNet (Joe, 2026-07-01)

The serious build is [[E-fold-embed-pipeline]] (card tracker `E-fold-embed-pipeline-readiness.html`).
How its stages realize this mission's experiments:

| M-fold-ansatz experiment | E-fold-embed-pipeline card | status |
|---|---|---|
| **A-small** (LLM-fold → 10 CLean wirings) | template for B.1 pairs + eventual phase-3 (f2) targets | DONE |
| **A-next** blinded sorry (empirical sorry = ground truth) | **B.1** (post-hoc empirical-sorry mining; autoclock-in = sealed template) | DONE (n=1)→scale |
| **f1 = phase 2** (cascade → substrate-2 sorry) | the WHOLE pipeline **A→E** | building |
| **B0** dumb baseline (popularity/degree) | **D.1** baseline | folded in |
| **B2** embedding-vs-baseline (the real fork) | **C.3 ablation** (text/struct/hybrid) + **D.1** | the crux of C/D |
| the 3 shallow-embedding negatives | **superseded by C** (the fair, trained, hard-negative version) | done→retest |
| **f2 = phase 3** (sorry → wiring, CLean/DarkTower) | OUT of this excursion — stage ⑧ `clean_structure_embed` + GFlowNet | separate |

**GFlowNet — prep now (efficiency), deploy at f2 (Joe).** Add GFN readiness to the pipeline so it plugs
in without a restart: reuse `E-cascade-sampler-sampler/s4/` (config machinery, proxy-adapter, TB loss,
flight-probe — MAP M1). Its home is **f2 (phase 3 wiring generation)** with the **DarkTower 0-sorry gate
as the STEEP reward** (M1: the sampler failed on flat C-wholeness reward; 0-sorry is binary-steep — the
fix). Prep = a `G` card (parallel, laptop): stand up the GFN env + the 0-sorry reward adapter, so f2 is a
config-swap not a build.

**META (Joe, keep in mind — not first): GFlowNets can DESIGN experiments / meaningful folds.** Beyond
generating wiring, a GFN over the fold space could *sample meaningful folds* (diverse, reward-weighted by
a "meaningfulness" signal — e.g. terminals=endpoints acceptance + 0-sorry + on-topic). I.e. GFN not just
as the f2 generator but as an *experiment/fold designer* — a forward-looking direction, noted for when
phase-2 lands.

## The data flywheel (Joe, 2026-07-01) — pipeline feeds the GFN

The GFN fold-designer works (F1 0.69 leave-one-out, laptop) but is **data-limited at n=10** hand-authored
folds. The resolution: **the pipeline runner IS a fold-generation engine — its output is the GFN's
training data.** The two paths compose, not compete:
- **Pipeline (Stage A/B + the Linode embedding, and/or more LLM-folds over the 123 A.2-linked missions)**
  → more `(cascade → sorry/spine → wiring)` examples.
- → **GFN trains on the growing corpus** → a better fold-designer → (eventually) generates folds itself →
  more examples. A **data flywheel**.
- Grain note: the GFN learns at *pattern-spine* grain; Stage-B's coarse *var-endpoints* feed the
  embedding. To feed the GFN directly, the pipeline folds the 123 missions to spines (like A-small at
  scale) — the same runner, one more output head.
**Net:** n=10 → scale as the pipeline produces folds; the positive GFN result is a *floor*, not a ceiling.

## Checkpoint 2026-07-02 — the A-next gold corpus (phase-2 ground truth) + the used-var correction
**What happened:** building the E-fold-embed-pipeline bundle surfaced a category error — it targeted a
*coarse used-var proxy* with empty cascades (mission-id key mismatch), not the real sorry. Corrected: the
phase-2 ground truth is the **A-next gold corpus — 10 real `(cascade, sorry, wiring)` triples**, each sorry
recovered post-hoc (doc+code+git+XTDB), holes graded `:discharged`/`:open`/`:research`. Two triples caught
the LLM-fold wiring *over-claiming* its discharge (state-snapshot-witness, single-entry-point) — the value
of recovering the sorry independently of the wiring. Pin: `holes/labs/A-next-gold-corpus.md`.
Crosswalk update: **A-next** (was "DONE (n=1)→scale") is now **DONE (n=10, gold)**; **f1/B2** now runs
against these empirical sorries, not the used-var bundle.

**Open decision (Joe):** with the improved gold, (a) **seed the GFlowNet locally** — n=10 is the
data-flywheel's stated operating point, laptop-only, tests the *mechanism* on honest data; or (b) the
**GPU mining run** — 70B batch-recovers triples over ~200 missions primed+gated by the gold, but most
missions are IDENTIFY-stage so it would mostly add `:open` sorries. Owner lean: (a) first (cheap,
high-info, no box); escalate to (b) only if the local run shows the mechanism works but is data-starved.
