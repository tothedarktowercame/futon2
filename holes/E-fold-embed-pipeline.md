# Excursion: the serious GPU embedding pipeline for the fold's phase 2 (E-fold-embed-pipeline)

**Date:** 2026-07-01 · **Status:** CHARTERED (design) · **Owner:** claude-4 (end-to-end).
**Parent:** [[M-fold-ansatz]] — this is its **phase 2** (`cascade → SORRY`, the substrate-2 interface),
carved out as an excursion because it is a *real ML build*, not a mission's worth of open specification.
**Modelled on ("something like this," Joe):** `futon6/holes/pre-superpod-pipeline-readiness.html` ·
`futon6/holes/proofcheck-readiness.html` · `futon2/holes/mission-mining-readiness.html` (staged, gated,
GPU where it earns it).

## Why this excursion exists (no quick fixes)
Phase 2 = predict a mission's real **substrate-2 sorry endpoints** from its **cascade**. Three *shallow*
numpy embeddings (raw-SVD / PPMI / code-graph spectral) all **lost to popularity/degree** — but that only
shows the *cheap* version fails, not the ansatz. The honest test needs a **trained, GPU-scale, multi-
relational** embedding done properly. That is this excursion. A flat result *here* (a real pipeline, fair
baselines) is a genuine verdict; a flat result from an afternoon hack is not.

## The prior that shapes the design (futon history — heed it)
A past superpod run: **BGE text-embedding beat R-GCN structural for retrieval; hard negatives essential**
([[feedback_superpod_embeddings]]). So the pipeline must:
- treat **BGE-text as a first-class baseline AND a node-feature** (not assume structure wins);
- settle **text vs structure vs hybrid** by ablation, fairly;
- mine **hard negatives** (near-miss endpoints), not random ones — random negatives are why AUC looked
  "fine" while recovery was popularity-flattered.

## The pipeline (staged · gated · GPU where it earns it)

**Stage A — Graph extraction → a typed heterogeneous graph (CPU, laptop).**
Pull the real substrate-2 multi-relational graph (NOT the cascade slice): nodes = `code/v05/var` (125,730),
`namespace` (7,053), `pattern/library`, `mission/doc`, `sorry`; typed edges = `calls` (89,304),
`contains` (73,628), `coverage` (10,690), `test` (29,802), `edits` (191,485), mission↔pattern (1,783),
sorry↔mission (`:related-missions`). **Establish the mission↔code linkage** (the one real unknown — via
`edits` (agent/session→var) + `mined-move` + `sorry :related-missions`; time-boxed investigation, output
a documented edge set). Artifact: `graph.npz` (typed edge index) + `node-catalog.json`. *Gate:* every
node has a type and ≥1 edge; the linkage covers ≥N missions with known code endpoints.

**Stage B — Ground truth + hard-negative mining (CPU).**
Post-hoc-mine the **empirical sorries** for a corpus of missions (Joe's method: doc + code + git + XTDB;
autoclock-in already sealed as the template) → `(cascade, {have,want endpoints})` labelled pairs.
**Hard negatives:** for each mission, the top-degree/most-popular non-endpoints + endpoints of
*sibling* missions (near-misses). Artifact: `pairs.jsonl` (train/val/test split BY MISSION, no leakage).
*Gate:* ≥30 missions with sealed empirical sorries; hard-negative ratio checked.

**Stage C — Embedding training (GPU / superpod). The heart.**
Node features = BGE(text of the node) ⊕ type-onehot. A **relational GNN** (R-GCN / GraphSAGE over the
typed edges) produces structural node embeddings; train with a **link-prediction / endpoint-recovery**
objective (cascade-centroid → endpoint), hard-negative sampling, degree-correction in the loss.
**Ablations (this is the actual experiment):** `text-only (BGE)` vs `struct-only (GNN)` vs `hybrid` —
so we finally answer BGE-vs-structure *fairly* on THIS task. Artifact: `embeddings.npy` + `model.pt`.
*Gate:* val endpoint-recall stops improving (early stop); the run is checkpointed
([[feedback_capture_before_decommission]] — capture artifacts before the box is released).

**Stage D — Evaluation (CPU).**
Endpoint-recovery on the **held-out missions**: recall@k, MRR, degree-corrected, vs **three baselines —
popularity/degree, BGE-text-only, random**. Report the *distribution*, not n=1. Artifact: `scorecard.md`
+ per-mission recovery. *Gate:* the verdict is stated either way (beats baselines ⇒ ansatz has legs;
ties them ⇒ measured "structure adds nothing here, and here's the ceiling").

**Stage E — Laptop export (CPU) — preserves the ansatz's point.**
Freeze the trained node embeddings → the **fold at runtime is laptop-only dot-products** (train once on
GPU; infer on the laptop, sub-second). This is how "laptop-only" and "GPU pipeline" reconcile: training
is a one-time GPU cost, inference is the cheap laptop fold. Artifact: `laptop-index.npz` + a
`fold_phase2(cascade) → ranked endpoints` function.

## Success criteria (Joe/owner ratify)
1. The **hybrid** (or the ablation winner) **beats popularity AND BGE-text-only** at endpoint-recovery on
   held-out missions — *or* a rigorous "it doesn't, here's the measured ceiling and why" (real result).
2. Laptop inference confirmed sub-second (the efficiency claim), from GPU-trained embeddings.
3. Plugs into E-close-the-loop's interface as the phase-2 realizer of `fold(cascade, circumstance)`.

## Reuse (Joe: reuse the mission-mining / pre-superpod / proofcheck pipelines)
- **mission↔code linkage (Stage A.2) — must be BUILT** (verified unbuilt: turn/commit/mission edges = 0,
  and the readiness-doc's `backfill_turn_commit_mission_bestguess.py` is NOT on disk). Build via **commit
  `:subject` → mission-id text match** (commits often name their mission) + clock/clocked-on valid-time
  (author≈agent, timestamp) fallback. `code/v05/edits` (191,485, commit→var) makes commit→code solid.
- **`mission_structure_embed.py`** — mission structure embedding (Stage C.2 component; sibling of the
  CLean `clean_structure_embed`).
- **`diffsub_emit.py`** — the JAX soft-adjacency embedding scaffold ([[M2 of M-fold-ansatz]]).
- **`mission_mine_moves.py` / `mission_scope_detect.py`** — Stage B extraction. The BGE embedder + the
  pre-superpod staging/gating harness for C. Card tracker: `E-fold-embed-pipeline-readiness.html`.

## Compute + boundaries
- **Compute (Joe commissions):** local prep (A/B/D/E) + one **4-GPU Linode** for training C (right-sized
  for a 125k-node GNN — not superpod). Prep-locally / train-on-box / **capture-before-release**
  ([[feedback_capture_before_decommission]]). Dependency: the Linode spun up — the one thing enabled,
  not designed.
- **In scope:** phase 2 only (`cascade → substrate-2 sorry`). **Out:** phase 1 (cascade — done); phase 3
  (`sorry → wiring`, CLean/DarkTower — stage ⑧ `clean_structure_embed`, a *separate* build).

## GFN scope + compute (Joe, 2026-07-01) — two paths, one Stage B
- **Embedding path (Stage C):** phase-2 *selection*, GNN over the 125k-node graph → wants the **4-GPU Linode**.
- **GFN path:** a *generator* of the **full fold** (sorry+wiring), a small policy net over a bounded
  fold-construction + the **0-sorry reward** — **plausibly laptop-only** (reuses the CPU cascade-sampler),
  so it can sidestep the Linode and may return first.
- **Stage B is SHARED** by both (Joe: "same mining either way"): the (cascade, sorry-endpoints, wiring)
  ground truth feeds the embedding's pairs AND the GFN's full-fold examples. Build B once.

## Stage B grain — settled (2026-07-01, built + measured)
A.2 linkage (doc-sha-citations: 495 commits / 93 docs → +subject-match) → mission→code via `edits`.
Endpoint-grain measured on 123 missions:
- **var-footprint** (all touched vars): median **784** — the code footprint, NOT the interface.
- **namespace-level:** median **29** (50 missions ≤25) — usable, coarse.
- **used-var** (Joe's refinement — external vars actually CALLED via the `calls`-graph = static reflection):
  median **110** — better than footprint, still coarse because `V_m` = whole cited-diff footprint. The true
  ~6-endpoint interface needs added-vars-only (not in `edits`) or focused mining.
**DESIGN (settled):** coarse used-var/ns = **bulk TRAIN signal** (123 missions, cheap); **focused empirical
sorries** (autoclock-in template, hand-mined) = the **gold held-out EVAL**. Train coarse, evaluate focused —
the honest interface-recovery test stays on the gold set; the bulk gives the GNN/GFN training volume.
Artifacts cached: `/tmp/{commits,edits,calls_all}.tsv`. Next: package `pairs.jsonl` + hard negatives.

## STATUS: LINODE-READY (2026-07-01) — Stages A+B built, C+D authored
- **A+B (laptop, DONE):** `futon6/scripts/fold_embed/mk_dataset.py` → reproducible bundle
  `futon6/data/fold-embed/` = nodes.jsonl (62,967: 62,297 var + 361 pattern + 309 mission) ·
  edges.jsonl (257,932: calls 89,321 · contains 73,644 · uses-pattern 1,783 · mission-touches 93,184) ·
  pairs.jsonl (110; 89/10/11 train/val/test by mission, hard negatives) · manifest.json.
- **C+D (authored, runs on box):** `futon6/scripts/fold_embed/train_fold_embed.py` — lean torch SAGE
  (minimal deps: torch + sentence-transformers), the **text / struct / hybrid ablation** + endpoint-
  recovery (recall@20, MRR) vs popularity + random. Parses OK; torch absent locally so it runs on the box.
- **To run (Joe commissions):** `futon0/README-linode.md` → `g2-gpu-rtx4000a4-s` (RTX4000 Ada ×4) via
  StackScript 2142757 → `rsync futon6` → `python scripts/fold_embed/train_fold_embed.py --mode {text,struct,hybrid}`
  → three scorecards → the fair BGE-vs-structure verdict. Then Stage E laptop export.

## STATUS UPDATE (2026-07-02) — the used-var bundle was the WRONG OBJECT; A-next gold corpus is the target
**The LINODE-READY bundle above is retired as ground truth.** Gating it before spend
(`futon6/scripts/fold_embed/check_fold_embed_gates.py`, torch-free, on the laptop —
`library/data-mining/{gates-as-code,smoke-before-the-paid-run}`) caught two showstoppers:
- **empty cascades:** all 110 pairs had `cascade: []` (a mission-id key mismatch in `mk_dataset.py:83` —
  canonical `<repo>-d/mission/<id>` vs bare doc-leaf), so every arm would score an identical global-mean
  ranking — a **null ablation**. Fixed the join (0→100% cascade coverage) but the honest corpus caps at
  ~23–33 missions (few missions have *both* a mined cascade AND commit-cited code endpoints).
- **wrong target:** `pos` = "top-60 external-called used-vars" is a **coarse proxy**, not the mission's
  real sorry endpoints. The experiment (f1: `cascade → substrate-2 sorry`) needs the **empirical sorries**.

**Resolution (Joe):** used-vars are meaningful but need *contextualising* into the typed have/want
interface — which is exactly A-next. The ground truth is now the **A-next gold corpus: 10 real
`(cascade, sorry, wiring)` triples**, each sorry recovered post-hoc from doc+code+git+XTDB, holes honestly
graded (`:discharged`/`:open`/`:research`). Canonical pin: `futon2/holes/labs/A-next-gold-corpus.md`;
triples in `futon2/holes/labs/A-next-*/`. This is the eval set / few-shot golden / gate fixtures.

**Open decision (Joe, 2026-07-02):** with this improved gold, either (a) **seed the GFlowNet locally**
(n=10 is the flywheel's known operating point; laptop, no box, tests the *mechanism* on honest data), or
(b) **the GPU mining run** (70B batch-recover triples over ~200 missions, primed+gated by the gold — but
many missions are IDENTIFY-stage, so it would mostly add `:open` sorries). Recommended sequence: (a) first
(cheap, high-info); (b) only if the local run shows the mechanism works but is data-starved.

**DECIDED (a) — GFN seed dispatched (2026-07-02, Joe + claude-11).** Joe confirmed the GFN path; the v0
seed build is belled to claude-4 (the excursion owner) with a pinned design: endpoint-SET selection over
the pooled gold refs (own gold = positives, the other 9 missions' endpoints = the B.2 hard negatives),
exact-k Choices env (s4 reuse), STEEP 0-sorry reward (`eps + exp(β·coverage)`, range ≥ ~100× — the flat-C
lesson), one TB GFN per mission × 10, untrained-vs-trained probe, distribution reported, reduced-pool
fallback rung to separate state-space-size from mechanism failure. Code → `futon6/scripts/fold_embed/`,
verdicts → `futon6/data/fold-embed-gfn/`. v0 tests the MECHANISM (env + reward + trainability), not
cascade→sorry generalization — the conditional/cross-mission policy is v1, and (b) stays the data
escalation if v0 says "works but starved". (First bell mis-sent to codex-1 — a bare shell; superseded.)
