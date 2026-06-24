# E-aif2-partB — can we trust the AIF specification? (a held deep-research excursion)

**Date:** 2026-06-09
**Status:** **FIRST-PASS DONE (2026-06-24), full adversarial round still optional.** A first-pass triangulation
(from the in-hand deep-research AIF∩morphogenesis report + the R-contract) was run by claude-2 in a clear window
and folded into `futon2/docs/futon-aif-completeness.md` §"Are R1–R13 enough?" — R13 written in, and
**R14–R18 added as first-class tracked-open criteria** with definite status + checkable forms (R14 γ-over-policies
· R15 hierarchical/temporal depth · R16 closed action–perception loop · R17 structure-learning/niche · R18
faithfulness). The **full** fan-out
deep-research round (§4) remains an optional hardening pass; the charter below is retained for it.
*(Originally: HELD — charter only; run in a clear usage window, not mid-collaboration.)*
**Owner:** claude-1 (charter) → a future single-agent deep-research pass.
**Relation:** follow-on to `M-aif2` (the R1–R12 boundary-tier work) and its **2026-06-09 retrospective**
(why the degenerate-policy loop passed R1–R12 → recommended `R13-policy-adequacy` + a degeneracy/sniff-test
criterion). Sibling-in-method to **E-the-dark-tower-2** (written by a prior deep-research round — the
precedent for this excursion's mode).

---

## 1. The tension (Joe, 2026-06-09)

> "Even with R13 — can I really trust the specification of AIF we are working to? How do I know there
> aren't still-more missing components?"

R13 patches *one* gap we found *only because it bit* (the cursor #1 / degenerate single-step policy). The
worry is the meta-level: the `R1…Rn` checklist is itself a fallible, hand-grown artifact. How many more
gaps are lurking, invisible until they bite?

> **CORRECTION (Joe, 2026-06-24):** §2's "you cannot certify the spec complete from inside it (Gödel / frame
> problem)" is **wrong for this context.** We are implementing a *known, formalized theory* (AIF/FEP) with a
> *finite canonical component set* — completeness is a checkable diff against the literature with a **reachable
> endpoint**, not a self-referential paradox. R13 was missed for a mundane reason (introspecting our checklist
> instead of diffing against the theory), and the diff *closes* that. The valid residue of §2 is narrower:
> *surprise → update* is a useful **early-warning / faithfulness cross-check** (it caught R13 before any diff,
> and catches "present-but-unfaithful" components), and the canonical component set must be *surveyed correctly*
> (the deep-research round) — but neither makes completeness unattainable. See `futon-aif-completeness.md`
> §"Are R1–R13 enough?" for the corrected framing. The text below is kept as written for provenance.

## 2. The honest answer (the reframe to carry into the research)

**You cannot certify the spec complete from inside it.** A finite checklist can never prove "there are no
more missing R's" — structurally (Gödel / the frame problem): the list can't enumerate its own blind spots.
So "trust a *complete* spec" is unattainable; it's the wrong bar.

**The move is to trust a gap-surfacing *process*, not a complete *artifact* — and that process is Active
Inference itself, applied recursively.** AIF never assumes its model is correct: it runs, and *surprise*
(prediction-error) is the signal to update. R13 was found exactly this way — the *live* WM produced an
output surprising to operator common sense, and we updated. The spec's incompleteness is handled the same
way the model handles any inadequacy: **surprise → update.** (M-aif2's "the modeller inside the model" *is*
this — the apparatus modelling its own adequacy, emitting gaps as prediction-errors.) So you trust the
**loop** to surface inadequacy *as surprise*, provided it is (a) live, (b) observed by a critic with a
sniff-test, (c) willing to revise.

**The real danger is the *silent* gap.** Cursor #1 was loud. The dangerous inadequacy is plausible-but-wrong
output that nobody flinches at. So trust scales with how hard **surprise-detection** works — operator
sniff-tests, an adversarial "what's missing?" completeness-critic, and *combining methods so their
disagreement is the signal*. Invest there, not in "finishing the checklist."

## 3. The concrete move — the deep-research round (this excursion's deliverable)

**Triangulate `R1–R12(+R13)` against the canonical free-energy-principle / active-inference decomposition in
the literature.** Convert "unknown unknowns" into a *checkable* set of known-unknowns. The canonical
component checklist to diff against (at minimum):
- generative model (priors + likelihood) · recognition/approximate-posterior density
- **EFE** with its full **pragmatic (risk) + epistemic (ambiguity / info-gain)** decomposition
- **policies** as the unit of evaluation (already R13's territory)
- **precision** (and precision *over policies* — the `γ` term; candidate missing R)
- **hierarchical depth** (nested generative models) and **temporal depth** (deep/“sophisticated” inference)
- the **action–perception loop** (closing the cycle; the R10 "live operation" but as a loop, not a snapshot)
- learning: state/parameter/**structure** learning timescales

**The tell that motivates this:** policy/temporal **depth is a named, canonical AIF component** (Friston's
deep temporal models / sophisticated inference) — so R13 was *findable in the literature all along*; our
R-list under-weighted it because it was grown from our own build, not diffed against the external reference.
The audit will likely surface the *next* R13-like candidates (precision-over-policies? hierarchical depth?
the full epistemic-value term? action-perception-as-loop?) **before** they bite.

**Deliverable:** a cited candidate-missing-components report (canonical decomposition × our `R1–R13`,
gap-by-gap), each gap classed `present / partial / absent`, with a checkable form for each absent one — the
input to the next R-contract revision. (It will **not** prove completeness — nothing can — but it earns the
most trust actually available: a concrete, audited list of known-unknowns.)

## 4. How to run it (when unheld)

Use the `deep-research` harness (the mode that produced **E-the-dark-tower-2**): fan-out web/literature
search on the FEP/AIF component decomposition (Friston, Parr, Da Costa, Pezzulo et al.), adversarially
verify the canonical-component list, then diff against `futon2/docs/futon-aif-completeness.md` (R1–R12) +
the M-aif2 retrospective (R13). **Single-agent, focused — run in a clear window, not while coordinating the
live build.**

## 5. Hold note

**HELD by operator (Joe, 2026-06-09):** do not run mid-collaboration; pick it up when there's a clear usage
window. This file is the charter so the thread isn't lost.
