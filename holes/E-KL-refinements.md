# E-KL-refinements

**Date:** 2026-07-03 · **Status:** ✅ **CLOSED** (Joe, 2026-07-03 — "if the work is
done the excursion can close"; closed at the §15 `:kl` production flip. Exit criteria
were met the same day — owner: claude-5, Joe-assigned 2026-07-03.) All five items resolved same-day: 1+2 built claude-10
`0f8d5c6` (reviewed-PASS, §10 row E); 4 discharged-by-1; 3 apparatus built + measured
claude-10 `22b0024` (reviewed-PASS, row F — headline: NO T matches hinge dispersion,
the gap is structural, ×20.3 uniform / ×2.3 parity; reviewer addendum: ~×13 of the
uniform gap is weight MASS, 13×1.0 vs pragmatic Σ=1.0); 5 round-tripped claude-4
`eb06565` (reviewed-PASS, row G — no contract friction; W1-side scale ×9.6 named).
Badge re-audited per the exit clause: in-source `kl` = `:derived-from-FEP` under the
declared Gaussian channel model; production `:G-risk` stays `:analogical` while the
live lane is the hinge — Σw·KL is joint-KL only at UNIFORM weights, so the canonical
flip config is uniform; `:pragmatic-parity` is a comparability tool. The `:kl` flip is
UNBLOCKED and is the operator's decision (evidence: e6-shadow.edn + e6-shadow-post-KLref.edn
+ t-calibration.edn). Excursion close = Joe's read.
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

1. **Truncate Q to [0,1].** ✅ **DONE (claude-10, 2026-07-03, `0f8d5c6`).** New
   `preferences/kl-gaussian-range`: Q~ = N(mu,s2) truncated+renormalised to [0,1]
   (closed-form in Φ/φ), so it's a true KL on the shared support ⇒ ≥ 0.
   **Verified against Riemann quadrature** (`truncated-kl-matches-quadrature`, the
   load-bearing test, tol 1e-3 over a 144-cell sweep) — claude-5's algebra
   confirmed, not trusted blind. Non-negativity (incl. the named regression case
   mu 0.5/s2 1.0/[0,1]/T 1.0 = +6.8e-4, was < 0) + monotonicity tested. M clamped
   1e-12 (mu-far-outside regime, documented); result `(max 0.0 …)` = numerical clamp
   only. HONESTY block + docstring updated; **badge candidate `:derived-from-FEP`,
   re-audit is the owner's step (not claimed).** Bernoulli branch untouched.
2. **Channel-weight parity.** ✅ **DONE (claude-10, 2026-07-03, `0f8d5c6`).**
   `efe/compute-efe`: `:kl-channel-weights :pragmatic-parity` resolves to
   `pref/pragmatic-weights` with MISSING channels weighted 0.0 (parity = zero-weight,
   not 1.0). Map form + uniform default byte-identical. Tested
   (`kl-pragmatic-parity-preset`): parity ≡ pragmatic-over-0-default, ≠ uniform, and
   an absent channel (`:mathematics-pct`) proven to contribute 0.
3. **Temperature calibration.** ✅ **APPARATUS BUILT + MEASURED (claude-10, 2026-07-03,
   `22b0024`).** `scripts/wm_t_calibration.clj` (SIM-only, one-shot; per-channel-T
   plumbing in `efe/compute-efe`, scalar path byte-identical) + artifact
   `holes/labs/M-evaluate-policies/t-calibration.edn`.
   **Headline finding (single tick, days=14):** hinge within-tick risk sd = **0.1063**;
   **NO T in [0.02, 2.0] matches it** — kl-sd *floors* at **×20.3 (uniform)** / **×2.3
   (:pragmatic-parity)** even at the softest T. The gap is **STRUCTURAL** — as T→∞,
   C→uniform and KL→ −H[Q~], whose spread across candidates is T-independent — so
   dispersion-matching is **not a T-choice**. So E6's winner flip is NOT (mainly) a
   T-artifact: the KL functional is intrinsically ~20× more dispersed than the hinge
   (parity's channel-zeroing cuts that to ~2.3×). Per-channel-T proposal
   (`T_ch = anchor·σ_ch/σ̄`) emitted but ILLUSTRATIVE (no exact T* to anchor). E4
   lesson honoured (within-tick only; single-tick caveat stated; re-runnable on later
   ticks). **`default-c-temperature` stays 0.1 — the flip decision is the operator's,
   now informed by this data.**
4. **Negative-divergence guard.** ✅ **DISCHARGED BY ITEM 1 (claude-10, 2026-07-03).**
   Moot once truncation landed: the score is now a true KL ≥ 0 by construction, so
   there is no negative divergence to guard. The residual `(max 0.0 …)` is a numerical
   clamp against ~1e-7 erf-approximation noise only (documented as such), not a
   semantic clamp on a divergence score.
5. **Bernoulli path exercised end-to-end.** ✅ **DONE (claude-4, 2026-07-03, `eb06565`;
   reviewed-PASS claude-5).** W1 landed a real DARK consumer: `c-vector/kl-risk-of` +
   `predictive-goal-outcome-risk-kl` (production path untouched; no private C — imports
   preferences; bb-load verified by reviewer, kl-at-p 2.5841 reproduced exactly).
   Live round-trip on the real belly (411/455 `:becomes`): **no contract friction** —
   shapes, nats, temperature semantics, [] ⇒ 0.0 floor all hold; kl-unmet 6.0000 nats
   = weight·1/T contract-exact. Three feedback items recorded in the §12 note of
   E-C-vector-live.md, the load-bearing one: **KL lane ≈ ×9.6 the hinge at T=0.1 ⇒
   any W1-side flip ALSO waits on item 3's calibration** (named flip-blocking in the
   dark twin's docstring); plus the :becomes-keyword→target-1 asymmetry ("prefer NOT
   x" inexpressible from W1's entry shape, noted) and the deliberate, visible
   unit-mixing of range-hinge with Bernoulli-KL in the dark twin.

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
