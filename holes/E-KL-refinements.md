# E-KL-refinements

**Date:** 2026-07-03 · **Status:** CHARTERED (debt registered; owner TBD — Joe assigns)
**Parent:** `holes/M-evaluate-policies.md` §12 (D5a built dark, `f6451ba`; refinements
deliberately deferred — Joe: "we can come back to the KL issues after [E6]").
**Consumers:** M-evaluate-policies (any future `:risk-mode :kl` flip) · W1/claude-4
(E-C-vector-live §11, contract at line 230) · the R18 badge on `:G-risk`.

## Charter

`futon2.aif.preferences/kl` (Gaussian-Q vs range-density) is badged
`:principled-approximation` **in-source** with named gaps. This excursion is the ledger
of exactly what would raise it — and of the calibration questions E6 exposed. It is
prototyping-forward debt (`:kind :prototyping-forward`), not silent debt.

## The items

1. **Truncate Q to [0,1].** The closed form takes E_Q over ℝ while C is supported on
   [0,1]; untruncated tail mass makes the "KL" a divergence score (can dip below 0 for
   wide σ). Repair: renormalise Q on [0,1] (two Φ terms) in `expected-hinge` + the
   entropy term → a true KL between densities on the same support → badge candidate
   `:derived-from-FEP` (under the Gaussian channel model).
2. **Channel-weight parity.** `:kl-channel-weights` defaults to uniform; the hinge path
   uses `free_energy`'s pragmatic weights. E6's ×22 risk-dispersion mixes functional
   change with weight change. Repair: a `:kl-channel-weights :pragmatic-parity` preset
   sourcing the same weight map, so E6-style comparisons isolate the functional.
3. **Temperature calibration.** `default-c-temperature` 0.1 was documented, not fitted:
   at T=0.1 an 0.1 out-of-range excursion costs ~1 nat, and E6's winner flip is partly
   a T-choice. Candidate: fit T so that KL-risk's within-tick dispersion matches the
   hinge's (behaviour-anchored), or derive per-channel T from observed channel σ.
   The E4 lesson binds: calibrate on *within-tick* dispersion, never corpus σ.
4. **Negative-divergence guard.** Until item 1 lands: decide clamp-at-0 vs report-raw
   (currently raw; fine dark, wrong live).
5. **Bernoulli path exercised end-to-end.** The `:becomes` Bernoulli form is built +
   unit-tested but has no live consumer yet; W1's first real use (claude-4) should
   round-trip it and feed back any contract friction.

## Cross-references

- Mission: `M-evaluate-policies.md` §8.6 (design), §12 (build + E6 evidence, deferral
  note), §11 (W1 boundary contract).
- Evidence: `holes/labs/M-evaluate-policies/e6-shadow.edn` (winner flips; dispersion
  ×22/×554); `e4-renorm-sweep.edn` (why naive rescaling is not a repair).
- Badge: `data/r18-badges.edn` `:G-risk` note points here.
- Contract: `E-C-vector-live.md:230` (the four consumer requirements).

**Exit:** items 1–4 landed (or explicitly declined with IHTB) + item 5 round-tripped ⇒
the `:G-risk` badge re-audited under the dcbe021 rules. None of this blocks
M-evaluate-policies' DOCUMENT/close; a `:kl` flip before item 1+2 land would.
