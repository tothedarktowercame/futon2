# Q-B magnet-quality scorecard — E-have-want-pairs

**Probe by:** claude-6 · **Date:** 2026-07-01 · **For:** C-cascade-real E1/E2 gate (via claude-4).
**Charter:** `E-have-want-pairs.md` §Q-B. **Sim-only; 0 :7071 writes.**

**Question (verbatim):** *does the have→want magnet do what needs doing?* i.e. can a
**mechanically-derivable** magnet ψ produce qualitatively-good cascades on **real** data — or is
good cascade quality fundamentally dependent on **hand-curated** magnet text?

**F** = accuracy − λ·complexity (Bayesian Occam, λ=0.25; **F>0 = the expansion is accepted**).
Anchor / ceiling (`E-have-want-pairs.md` §1b, *hand-curated* meme): M-value-creation-loop
id-stem F=−0.19 → **hand-meme F=+0.90**. The question is whether any *mechanical* derivation gets there.

## Method

A **mechanical-magnet ladder** — every ψ is a *deterministic function of stored fields*, no hand curation:

| magnet | ψ derivation |
|---|---|
| `M_idstem` (baseline) | the live lane's weak feed — `cascade_lane/mission->psi`: strip `M-`, hyphens→spaces |
| `M_want` | tokenized **goal** text (capability id / **sorry `:title`** / mission name) — want only |
| `M_havewant` | reconstructed **have→want** = current mission state (phases / status) + goal — the real impl-#3 magnet |

Sources (319 items): **264 C-vector wants** (stated 139 = 110 sorry + 29 capability · incomplete 81 · mess 44)
+ **55 diffsub-moves** (sanity, each `:have`/`:want`/`:delta-g`). Scored in-process via
`futon3a` `construct_cascade` (MiniLM magnet). Artifacts: `magnet_probe_extract.bb` (EDN→JSON),
`magnet_quality_probe.py` (scorer), `data/c-vector/magnet-quality-scorecard.json` (full rows).

## Numbers

**Overall (n=319), by magnet:**

| magnet | F_mean | F_median | frac F>0 | size_mean |
|---|---|---|---|---|
| M_idstem | −0.151 | −0.45 | 0.207 | 2.21 |
| M_want | −0.072 | −0.452 | 0.260 | 2.64 |
| **M_havewant** | **−0.058** | **−0.249** | **0.295** | 2.86 |

**Lift M_havewant vs M_idstem (paired):** mean ΔF **+0.093**, **58.9%** improved, **48/319** crossed F≤0 → F>0.

**By channel (M_havewant, and its lift over idstem):**

| channel | n | idstem F_mean | havewant F_mean | havewant frac F>0 | mean ΔF | crossed 0 |
|---|---|---|---|---|---|---|
| **stated-sorry** | 110 | −0.121 | **+0.050** | 0.355 | **+0.171** | 21 |
| incomplete | 81 | +0.036 | +0.080 | 0.370 | +0.044 | 17 |
| diffsub | 55 | −0.135 | −0.008 | 0.382 | +0.127 | 10 |
| stated-capability | 29 | −0.360 | −0.360 | 0.138 | **0.000** | 0 |
| mess | 44 | −0.452 | −0.447 | 0.000 | +0.005 | 0 |

**diffsub F↔ΔG (mechanism check):** Spearman(F, `:delta-g`) = −0.19 / −0.07 / −0.07 (idstem/want/havewant).
**Caveat:** all 55 moves' ΔG lie in a 0.001 band (all marginal) → this tests "does F track ΔG *among
near-equal moves*", answer **no**; weak/inconclusive, not a clean negative. Semi-circular (charter §Q-B).

## Qualitative (eyeballed cascades — the proxy `on_topic` undercounts; trust the eyeball)

- **stated-sorry is the win, and it's genuinely on-topic.** The sorry `:title` is real stored NL text.
  `completion-full-layer-integration`: F −0.44 (size 1) → **+0.16** (size 4: futon-bridge-health,
  mission-interface-signature, error-layer-hierarchy — on-topic). `f0-f2-interface-agent-perception`:
  +0.02 → **+2.48** (size 11: sense-deliberate-act, umwelt-not-architecture, aif-as-environment —
  clearly on-topic even though the token proxy reads 0.00). The mechanical lift here *matches/exceeds
  the hand-meme ceiling* — because the stored want carries real words.
- **stated-capability = zero lift.** Bare slug (`cold-eoi-authored-outbox`) tokenizes identically to
  the id-stem; nothing to add; stays F<0. No title, no reconstructable have.
- **High F is not always on-topic.** `mission-coherence-patterns/derive` → F=+1.08 but pulls generic
  mission-method patterns (shape-first-identify, scope-bounded-handoff) — semi-circular, not topical.
  **F alone ≠ good cascade.**
- **incomplete: the "have" (phases) helps thinly** — lifts off-topic size-1 cascades toward
  generic-process patterns (M-archaeology-control −0.505 → +0.066), but stays thin; off-topic pulls
  (M-the-futon-stack-Q6 → jhana/ifr-* patterns, −0.75) are **library-coverage** failures, not magnet failures.
- **mess is dead** — generic "coherence wholeness rise" goal has no topical territory; size 1, F=−0.45, no variant moves it.

## Verdict (the C-cascade-real gate)

**There IS a mechanically-derivable magnet source that lifts F — and it is nameable: the want-side
stored natural-language text (sorry `:title`; weaker: mission phase-state), NOT the bare id-stem.**
Where that text exists, the mechanical have→want magnet produces genuinely on-topic F>0 cascades that
reach — and at the top end exceed — the hand-curated ceiling. So Q-B is **not** a flat "hand-curation-only."

**But it is a PARTIAL, conditional result, not a clean green line:**
1. The lift is **conditional on want-side text richness**. Rich want (sorry title) → real on-topic lift;
   bare-slug want (capability id) → **exactly zero** lift, stays F<0.
2. Even with the best mechanical magnet, only **~30%** of the corpus clears F>0; the median stays negative.
3. The remaining failures are **library coverage** (off-topic pulls on thin-territory missions) — the §1b
   binding constraint, re-confirmed — not the magnet mechanism.
4. F↔ΔG does not track (Spearman≈0, weak test): the magnet pulls a topical neighbourhood but its quality
   does not encode the move's gradient value.

**Disposition for E1/E2:** move from *"held on Q-B, unknown"* → **"held on Q-B, with a named mechanical
lever."** The generative signal is mechanically derivable **where the want carries stored words**; the
honest next levers (both mechanical, neither hand-curation) are: **(a) harvest want-side NL text for
bare-id wants** (sorry `:if`, mission HEAD text — the same move that made sorry titles work), and
**(b) grow library coverage** where missions sit in thin territory (E-library-coverage). This does **not**
license a SATISFIED ledger line now (we-do-discipline), but it converts the hold from a blank into a
quantified, mechanical, closable gap — the opposite of "good cascades need hand curation."
