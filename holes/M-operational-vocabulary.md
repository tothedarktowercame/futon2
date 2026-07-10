# Mission: Mine the operational vocabulary — give the WM a memory of previous actions (M-operational-vocabulary)

**Date:** 2026-06-25 (created + driven through one session; lifecycle reconstructed after the fact)
**Status:** **INSTANTIATE** — the FORWARD mining pipeline is BUILT and CPU-validated end-to-end; the GPU **box run** (joint mine over ~6.4k turns) is the at-scale instantiation, **pending commissioning** (prereg, §5). The **backward/sorry half** and the **promotion paths** (overlay→library/move-set/substrate-2) are chartered as the next steps. *Name provisional — alternatives: M-spin-the-scattered · M-concentration.*
**Owner:** Joe + claude-1
**Repos:** futon6 (`scripts/{mission_structure_embed, mission_mine_moves, meme_mine_runner, meme_mine_joint, meme_consume, mission_concept_tag, meme_target_sample}.py`, `scripts/linode-meme-mine.sh`, `holes/{meme-mine-runner-spec, meme-mine-preregistration}.md`, `data/meme-mine/`) · futon2 (`aif/rollout.clj`, `report/cascade_lane.clj`, `aif-wiring-explainer.html`, this doc, `mission-mining-readiness.html`, [[E-mine-mission-transitions]]) · futon3c (Evidence Landscape `src/futon3c/evidence/*`, the WM pilot, [[M-autoclock-in]]) · futon3a (`resources/notions/minilm_*_embeddings.json`, the meme/arrow store) · futon1a (`:7071` substrate-2)
**Cross-ref:** [[M-aif-wiring]] (this closes its R16 **criterion (2)** — the rollout path — and feeds **R14** γ; the diagram now carries the *belly* this work motivated) · [[M-wm-policies]] (CLOSED — continues its **Track 2** forward-model coverage) · [[M-a-sorry-enterprise]] (the **backward/goal dual** — sorries; §3b) · [[M-autoclock-in]] (the `turn→mission` link this consumes; §6) · [[M-populate-substrate-2]] (our run *produces* its empty PROOF-layer content; §3d) · `the-woven-form.flexiarg` + `futonic-logic.flexiarg` (the root metaphor + vocabulary) · `futon6/holes/proofcheck-readiness.html` (the pipeline template ported here)

---

## 1. IDENTIFY

The War Machine **abstains** on every served mission (act-gate `:abstain-missing-leg`) because its rollout `G(π)` is `nil` — the mission has **no rollout path**. As Joe put it: *the agent literally can't imagine acting on these missions.* The fix is to **give it a memory of previous actions** and relate the current possible actions to past ones.

Deeper diagnosis: today's scopes (`IDENTIFY` / `<patternname>`) are a coarse **output-layer overlay**; the real **operational vocabulary** (futonic-logic's 鹵/皿/部/鹽/咅/香/應…) is latent in the **turns = the hidden layers**, of which substrate-2 is the compiled *output*. So the mining must reach the hidden layer: **memes** ← human→agent asks · **sorries** ← agent→human (應-voice) · **concepts** ← 香-tagging · **structure** ← the scope-tree.

**Why a new mission:** this produces the WM's **transition model** by mining the stack's own history — upstream of M-aif-wiring's R16/R14 (downstream consumers: coverage→ΔG; verdict history→γ) and the home M-wm-policies punted its Track-2 coverage to.

---

## 2. MAP / DERIVE — the pipeline

The **port of the proofcheck pipeline (proofs→missions)**: deterministic-where-possible, LLM-only-for-the-residue, an incremental monoid over **three corpora** (mission docs · patterns · turns). Tracker: `mission-mining-readiness.html`.

**The JOINT architecture (the key DERIVE move — Joe's correction).** Sending only turns wastes the model and leaves grounding to weak CPU reconstruction. We *have* all three objects, so: **CPU = retriever** (top-K candidate missions+patterns per turn, via concept-tag/co-embedding), **GPU = joint reasoner** (`meme_mine_joint.py`): (a) ground endpoints to real ids; (b) characterize which patterns the turn instantiates (`pattern_apps` = PSR); (c) compose a **cascade**; (d) **write new patterns** when no candidate fits (**R17** niche-construction). The pedagogy/NPT symmetry (Joe): forward = train-the-trainer (T3, the propagating method); backward = local-trainee (T1, the ground goal).

---

## 3. ARGUE — the design decisions

**(a) Forward/backward (policy/preference) duality.** Everything mined so far is *forward*: meme/turn/pattern = `have→want` (modus ponens, A→B) = the WM's **methods**. A **sorry** is *backward*: `want ⊣ have` (goal, method absent, B→A) = the WM's **goals**, hierarchical. They are two halves of one arrow (`:open` = sorry; `:constructed` = meme). **AIF reading:** memes/patterns are the *policy* half of EFE, sorries are the *preference* half — `G = risk(distance-from-goal=sorry) + ambiguity(forward)`; you cannot compute ΔG without both. This motivated the **belly** added to `aif-wiring-explainer.html` (v0.4 *mens, manus, venter*): the missing preference organ (Friston's C-vector / hara 丹田) feeding R5's risk — and it explains *why R16/R14/R15 were the gaps*: the forward loop was wired, the **setpoint** was not.

**(b) Sorries are a separate, backward effort — NOT co-embedded into the first run.** They are hierarchical (oriented by the capability-star-map; discharged to the Pudding-Prover "eat it / n≥1" standard; chartered in [[M-a-sorry-enterprise]]). **Audit (293 `:sorry` entities, `:7071`):** 143 open = **110 real** (devmap/excursion/technote/structural-law goals, 50 with next-steps) + **33 templated boilerplate** ("mission at unclosed phase"). Trust filter = `status=open ∧ not-boilerplate`. Held out of the box run; their content is the **belly's** content for the dual effort later.

**(c) substrate-2 — we are a PRODUCER, not just a consumer.** The empty PROOF layer (`constructs`/`closes`/`depends-on-sorry`) is exactly what the joint run emits (memes=arrows, `pattern_apps`=PSR, `new_patterns`, cascades). So we don't wait for it to land — we help fill it; the missing piece is the **promotion path overlay→store** ([[M-populate-substrate-2]]). The graph-wide *structure* embedding genuinely waits — there are no relation edges to embed until that layer exists.

**(d) WM-I4 / arming is exogenous, not an AIF criterion** (meta layer); the held R16 arc is held by *missing ΔG* (an AIF gap), not by permission.

---

## 4. VERIFY — built + validated this session (CPU, end-to-end)

- **Structure embedding** (`mission_structure_embed.py`): 198 missions, 161-d; same-class NN-match **0.79 vs 0.38** text; structural provenance verified end-to-end.
- **Mined-move generator** (`mission_mine_moves.py`): **177/177** abstaining missions flip `:abstain`→ non-nil ΔG (median −7.7e-4). Coverage done; **provenance quality** (63% same-class) is the frontier.
- **MEME-MINE** (`meme_mine_runner.py`): empirical op-vocabulary **~10 classes vs 3** hand-coded; endpoint-resolution tiers **contextual 59% / named 23% / unsupported 18%**; dedup key `(have,want,op)`. Stub-validated; openai-ready.
- **Consume + floor/cert** (`meme_consume.py`): endpoint-identity bridge meme→move; **186 structure-borrowed · 12 island** (needs-a-foothold) → `action-cert.json`.
- **Targeted sampler** (`meme_target_sample.py`): **82/198** rollout missions are meme-groundable from this corpus; full chain validated on **M-war-machine** (real ask → meme → move → **ΔG −0.0036**, real provenance).
- **Concept-tag** (`mission_concept_tag.py`): 1307-surface gazetteer; **32/300 turns route to a mission** via shared concept (model-free turn→mission, complements the weak autoclock).
- **Joint runner** (`meme_mine_joint.py`): retrieval + the 4-part schema (memes · pattern_apps · cascade · new_patterns) stub-validated (6/8 candidate recall). **`linode-meme-mine.sh`** (one-command box run) bash-clean; consume tail wired.

---

## 5. INSTANTIATE — the box run (pending commissioning)

The GPU run mines ~6.4k human→agent asks **× retrieved missions × patterns** on a 4-GPU Linode (vLLM `mark4-70b`), one command: `scripts/linode-meme-mine.sh` (tunnel-mode keeps transcripts on dev). **Preregistration:** `holes/meme-mine-preregistration.md` — predictions P1 (tiers ~59/23/18) · P2 (bounded op-vocab) · P3 (≥40/198 missions meme-grounded) · P4 (provenance beats guess) · P5 (joint retrieval lifts grounding) · P6 (new-pattern proposals = the R17 signal); data/privacy posture; decision rule. **Landscape ready to *receive*** (files + prereg analysis + readiness map); **promotion** of what we learn (new_patterns→library · cascades→move-set · meme-moves→substrate-2 PROOF) is the deliberate post-run step.

---

## 6. Completion criteria + relations

1. Served/abstaining missions get a non-nil ΔG from **real provenance** — ✓ mechanism (M-war-machine); full at box scale.
2. A turn-grounded move beats a structure-borrowed prior — ✓ shape (war-machine real-ask move); the box run is the n-scale witness.
3. Honest island-handling — ✓ (12 islands surfaced as needs-a-foothold).
4. The R14 (γ) feed — pending (post-box; needs accumulated verdict history).

**[[M-autoclock-in]] (the recursion):** this session was itself mis-clocked there — a live instance of the weak `turn→mission` link it exists to fix (and the reason this mission was spun up). Until it hardens, we route **turn→pattern→mission** (concept-tag + co-embedding).

**Deferred / post-box:** `SELECT-PER-MISSION` (runtime selection) and `GAMMA-FEED` (R14 γ) need a running loop — not buildable as shells now. The **backward/goal half is the sequel [[M-goals-and-holes]]** (mine goals/holes — spoken sorries + *unspoken* absence-holes), serving [[M-a-sorry-enterprise]].

**DRAFT:** `the-woven-form.flexiarg` (root metaphor; the §3 "commitments" are claude-1's synthesis for Joe's review).
