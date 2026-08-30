# P-R8 — Two nouns under one number: F (present fit) and g (gain reading)

Problem record (delivery-lifecycle v2). Node R8 (BELIEVE, support). Lane 3 of the R-node build.
Opened 2026-08-30 by claude-15 on Joe's go. Owner: claude-15. Source: `futon2/holes/labs/wm-contract/R8-glossary-formalisation.md` (carry §3–§5 forward).

## S1

**problem.** "R8" names two things (worksheet §0): **F**, the per-tick present-fit
`½ · mean_k (Π_k · (o_k − μ_k)²)` — theory-defined as a formula over stack-defined μ, o — and **g**, the gain
reading `clamp(2^(gain·mean perf), 0.5, 2.0)` with `τ_eff = 1/g` — an engineering controller that was
called a precision and fed from L1 pairs only. Every trace record examined in the census holds G under the key
`:free-energy` and no F (the census counted 88 *realized outcomes*; the record count is the number of
top-level forms across the 54 files of `futon2/data/wm-trace/` and is for D1 to establish — amended
2026-08-30 after claude-20 checked the corpus: 54 files by `ls`, not 88 of anything). g never moved: pinned at 1.0 with 0 samples through five deliveries whose
guarantees were each defensible and whose composition was seven weeks of silence (§4 e1–e5: corpus-wide
throw on one rejected deposit; `nil` outside a 4-entry map; `(catch Throwable _ judgement)`; no-op on
absence; idem-key from a different clock). R8 is the node where the July facade was most complete: the
number was present, named, and inert.

**now.** `GainChain.lean` states threadedIdentity, inhabitedHandle, durableBeforeFold, declaredDomain,
typedAbsence — and lacks the L2 constraint the glossary demands. The record schema ≥ 7 carries
`:prediction-errors` and `:precision-state`, so F is recomputable from the record. The outcome space for g
does not exist (spine: `P-validated-R5` §2a).

**solved:**
1. **F, definable now:** `F : Belief → Observation → Precision → ℝ` and the record contract
   `∀ tick, tick.variationalFreeEnergy = F tick.muPre tick.observation tick.precision`, checked over the
   trace with the reader loop. **Falsifier (amended 2026-08-30 after R8-D1 — codex-12 refused the one-direction form and claude-20
   reproduced the refusal; claude-15 reproduced the counts independently):** the checker assigns every
   top-level form one of three dispositions and the acceptance is the *census*, not a uniform fire —
   (a) `:missing-F-computable`: no stored F, inputs present → the checker recomputes F and fires;
   (b) `:stored-F`: `:variational-free-energy` present → the checker recomputes and compares
   (equal within ε or fires `:stored-F-mismatch`);
   (c) `:insufficient-inputs`: no `:precision-state` (or no `:prediction-errors`) → typed absence, never
   a pass and never a fire.
   **Expected values, stated before D2:** 755 / 32 / 5 over 792 forms in 53 files (D1, codex-12; claude-20
   reproduced); claude-15's own loop first gave 793 forms in 54 `.edn` files with 6 lacking `:precision-state`
   — the extra one is the dotfile `.lane-futility-index.edn` (one form, not a trace). **Filter, stated:**
   `data/wm-trace/wm-trace-*.edn`, dotfiles and `wm-shadow-step.json` excluded → 53 files, 792 forms.
   D2 uses that filter and reports the file and form counts; a checker that fires on all 792 or passes all 792 is
   wrong in one of two directions.
   "operational alarm" and no more — no claim that F is the variational free energy of a generative model.
2. **g, honestly named:** `expectedCoverage`, `realized`, `perf`, `g`, `τ_eff` as an engineering controller
   with its five deliveries typed (`Delivery` fields §4: `atomic-with`, `receipt`, `guarantee`, `idem-key`),
   and the **L2 refusal** stated as a type: g may be updated only from pairs whose `realized` the model did
   not produce. Refusing witness: the whole corpus (all L1). **Blocked** for anything that calls g a
   precision or π a policy — that half waits for the spine (Outcome, Policy).

**facades:** a key named `:free-energy` that holds G; the 18/18 fixture test (−0.6 → −0.6); 88 outcomes on
one policy offered for "any mission"; tests on `with-redefs` producers; τ "repaired" by renaming γ→g.

**status.** open.
**holder.** claude-20 (tech lead) → codex-12 (R8-D1)  
**parent.** BUILD  *(fifth precept, §0.10 — added 2026-08-30)*

## Edges (overlap points)
`R7→R8` (drawn), `R8→R5` (drawn), `R10→R8` (drawn), `R1→R8`, `R2→R8`, `R8→R14` (derived — the gain chain,
from the glossary). Payload `R8→R5`: `{tick, F}`; `R8→R14`: `{tick, g, τ_eff, n-samples, layer}` — the
`layer` field is what R9 checks. Schemas fixed in `P-control-map-lint.md`'s fixtures.

## deliveries
- **R8-D1 — discovery, no code.** Census over the trace: schema versions; which records carry
  `:prediction-errors`/`:precision-state`; recompute F for each and compare with whatever sits under
  `:free-energy`/`:variational-free-energy`; the `:selection-gain` state across ticks (expected: 1.0
  throughout, 0 samples). ≤ 200 lines, tick ids and file:line. Refusal permitted.
- **R8-D2 — build (after review).** The F checker whose acceptance is the three-disposition census of
  *solved* (1) — expected 755 `:missing-F-computable` / 32 `:stored-F` / 5 `:insufficient-inputs` over
  792 forms in 53 files — with the evidence on the recomputed-vs-stored comparison over the 32 (difference
  distribution reported, ε chosen from it and justified, any difference beyond float noise a
  `:stored-F-mismatch` with its tick id); kondo/parens; deterministic. *(Rewritten 2026-08-30 after
  claude-20 found this bullet still carried the refuted "fires on all 88" form; the amended `solved`
  governs.)*
- **R8-D3 — build (after D2).** The five deliveries e1–e5 written as `Delivery` records with schema +
  fixture each, and the L2 constraint as a stated type with its refusing witness — no g update code.
- **R8-D4 (blocked on spine).** g over the Outcome carrier.

## log
- 2026-08-30 record written (claude-15).
- 2026-08-30 amended (claude-15): '88 trace records' was the outcome count, not the record count.
- 2026-08-30 R8-D2 delivery bullet rewritten to match the amended falsifier (claude-20 caught the stale text and quoted it as superseded rather than reconciling it).
- 2026-08-30 amended (claude-15): falsifier rewritten as three dispositions after R8-D1's refusal (the packet's one-direction form was wrong in two directions: 32 forms already carry F; 5–6 cannot compute it). Owner gate on R8-D1: PASSED.
