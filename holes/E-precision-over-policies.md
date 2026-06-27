# Excursion: precision over policies — the γ term (E-precision-over-policies)

**Date:** 2026-06-26 · **Status:** DELIVERED 2026-06-27 (driven by claude-11; gated; see §7).
**Authored by:** claude-10. **Driven by:** claude-11.

---

## 7. DELIVERED (claude-11, 2026-06-27)

γ built as a learned, bounded, trace-persisted scalar wired into selection;
reduction-safe (≡ today at γ=1.0); learning staged on R16's realized-outcome feed.

**ARGUE (§4) decisions ratified:**
- **§4.1 — γ scalar, default 1.0, reduction-safe.** ✓ Burn-in gate: γ holds at
  1.0 until ≥5 realized samples accrue.
- **§4.2 — learning signal.** Ratified a *divergence* from the charter's literal
  "1/rolling-variance" R7-mirror: γ uses a **signed, scale-free performance
  ratio** `perf = (expected−realized)/(|expected|+|realized|+ε) ∈ [−1,1]` over
  expected-vs-realized G (lower G = better ⇒ realized-below-expected = beat =
  perf>0). Transfer `γ = clamp(2^perf̄, 0.5, 2.0)`, neutral 1.0 at exact-match.
  Scale-free (no G-unit constant). *Two-step history:* first a symmetric relative
  error (vs variance, to avoid consistent-bias false confidence); then made
  **signed** after a 2026-06-27 demo showed a symmetric metric measures only
  CALIBRATION — a large over-delivery (a big mismatch) spuriously lowered γ —
  whereas the excursion's goal (HEAD: decisiveness earned by good outcomes)
  needs PERFORMANCE direction. Both confirmed by Joe. The realized term is R16's
  `:realized-outcome` (not R12 Beta-credit — a different quantity, as §4
  anticipated).
- **§4.3 — modulation `τ_eff = τ_spread / γ`.** ✓ (not a γ-only temperature).
- **§4.4 — staging.** ✓ Machinery + wiring + the trace seam built now; the rich
  signal is gated on R16 live-wiring enactment. **Interface paired with claude-10:**
  the realized term is the `:realized-outcome` trace contract
  `{:policy :expected-G :realized-G :tick}` — both legs the *same* EFE quantity
  (fold coverage→rollout ΔG over predicted vs enacted wiring); written by the
  enactor, read by γ next tick (async-clean). fold.clj stays expected-only.

**Exit conditions (§5):** all met. (1) bounded [0.5,2.0], trace-persisted
`:policy-precision`, default 1.0 ✓ · (2) γ updates from realized-vs-expected
history off the hot path ✓ · (3) selection reads γ — higher γ sharpens, lower
flattens, demonstrated in tests ✓ · (4) reduction-safe — γ=1 byte-identical;
`:realized-outcome` absent today ⇒ neutral ✓ · (5) burn-in observability note:
`README-gamma.md` ✓.

**Artifacts:** `src/futon2/aif/policy_precision.clj` (new γ learner) ·
`policy.clj` (`effective-temperature`, `select-action :policy-precision`) ·
`trace.clj` (`:policy-precision` + `:realized-outcome` seam) ·
`scripts/futon2/report/war_machine.clj` (judge: read prior γ, fold realized
outcome, feed selection) · tests for all · `README-gamma.md` · explainer
`holes/aif-wiring-explainer.html` (R14 → WIRED; openable ⟨⟩ interface nodes).

**Gates:** clj-kondo 0/0 · check-parens OK · full suite 395 tests / 1243
assertions / 0 fail · explainer headless-verified (2 iface nodes, 0 JS errors).

**Scope-out held (§6):** scalar-only (per-context γ deferred); R15 hierarchical
γ; R16 arming. Pending: the live `:realized-outcome` feed (R16 / claude-10).

---
**Parent / relates:**
[[M-aif-wiring]] (R14 is its γ criterion — this excursion delivers it; the R-contract text stays with M-aif-wiring) ·
[[E-C-vector-live]] (R19 — the belly; the same "wire the canonical AIF quantity, default to the prior, sharpen on burn-in" shape) ·
R7 (`futon2.aif.precision` — the channel-level precision this mirrors at policy scale) ·
R16 (close-the-loop — the realized-outcome feed γ learns from) ·
R12 (`futon2.aif.intrinsic-values` — the existing Beta-credit learner; a structural sibling).
**Repos:** futon2 (`aif/policy.clj` — the softmax/τ selection where γ enters · `aif/precision.clj` — the R7 sibling · `aif/trace.clj` — the cross-call state home · `aif/efe.clj` — G the policies are scored by).

---

## HEAD

The WM's R6 selection is `P(a) ∝ exp(−G(a)/τ)` with **τ derived from the EFE *spread* of the current candidate set** (`policy/adaptive-temperature`: high spread → low τ → sharp pick; tight → high τ → diffuse → abstain). That τ is a **within-the-moment heuristic** — it reacts to how separated *this* decision's options are, but it never learns from **how past commitments actually turned out**. So the agent is not self-calibrating in *decisiveness*: it cannot commit harder after a run of plans that paid off, or hedge after a run that didn't.

**R14 — precision over policies (Friston's γ)** — is exactly that missing quantity: `P(π) ∝ softmax(γ·(−G(π)))`, where **γ is the agent's confidence in its own decision-making**, inferred from the realized-vs-expected outcomes of the policies it chose. γ ≈ the inverse selection temperature (`γ ≈ 1/τ`). In the brain it's the same VTA/SN dopamine signal as R7's channel precision — one level up, over *policies* instead of *channels*.

### The question

**What makes γ — the precision over policies — a LEARNED, bounded quantity (mirroring R7's channel precision, but over the realized-outcome history of chosen policies) that modulates the selection temperature, so the WM's decisiveness is earned rather than hand-set?**

### Discipline this inherits (read first)

- **No background loops in the shared serving JVM** ([[feedback_no_perpetual_loop_shared_jvm]]) — γ updates ride the existing per-tick path / trace, never a new poll loop. (The E-arxana-clock incident, 2026-06-26.)
- **Default to the prior; sharpen on burn-in** (the R19-KL pattern): γ defaults to 1 (≡ today's τ behaviour) until realized-outcome history accrues — so the change is reduction-safe and improves over runs.
- **The trace IS the state store** (README-clicks-and-ticks): γ persists as a trace field, like R7's `:precision-state`, never a sidecar.
- Evidence-first; gates per AGENTS.md (clj-kondo, check-parens, tests); never restart the JVM.

---

## 1. IDENTIFY — the gap

- **τ is spread-derived, not outcome-learned.** `policy/adaptive-temperature` computes τ from the current candidates' G-spread only. There is no γ; decisiveness doesn't track past success.
- **R7 solved the sibling at channel scale.** `aif/precision.clj` already learns per-channel precision Π from prediction-error rolling-variance, bounded by floor/cap, persisted in the trace. γ is the *same shape, one level up* (over policies).
- **The realized-outcome feed exists in trickle.** R16's consent gate records a ΔF∧ΔG verdict per cycle — the expected-vs-realized signal γ needs. It's thin today (R16 abstains pre-arming) but it's the right channel.

## 2. MAP — what's already here

| Piece | Where | Role for γ |
|---|---|---|
| softmax + adaptive τ | `policy/softmax-weights`, `policy/adaptive-temperature` | the selection γ modulates (`τ_eff = τ_spread / γ`) |
| channel precision Π (R7) | `aif/precision.clj` (variance-component + need, floor/cap, trace-persisted) | the structural template for γ at policy scale |
| EFE G(π) | `aif/efe.clj` (`:G-total`, `rank-actions`) | what policies are scored by; the expected term |
| realized-outcome record | R16 consent gate ΔF∧ΔG per cycle; per-tick trace | the learning signal (expected-G vs realized) |
| trace store | `aif/trace.clj` (`:precision-state`, `:decision`, `:ranked-actions`) | γ's cross-call home (add `:policy-precision`) |
| Beta-credit learner (R12) | `aif/intrinsic-values.clj` | sibling pattern (learned hyperparam from outcomes) |

## 3. DERIVE — the design (framed; the driver owns it)

1. **γ representation** — a bounded scalar policy-precision `Π_π`, persisted in the trace (`:policy-precision`), defaulting to 1.0. (Scalar first, per canonical AIF; per-context γ is a later refinement.)
2. **The learning signal** — the policy prediction-error: the chosen policy's *expected* G vs its *realized* outcome (the R16 ΔF∧ΔG verdict / next-tick observed free-energy change). Low rolling error ⇒ plans pay off ⇒ γ rises (commit harder); high error ⇒ γ falls (hedge/explore). Mirrors R7's `variance-component = 1/rolling-variance`, at policy scale.
3. **Wiring into selection** — `τ_eff = adaptive-temperature(spread) / γ` (high γ ⇒ lower effective τ ⇒ sharper commitment). Reduces to today's behaviour exactly at γ=1.
4. **Bounds + safety** — γ floored/capped (like R7's `tau-floor`/`tau-cap`) so it can neither collapse to 0 (never commits) nor explode (overconfident). Default 1.0.
5. **The feed (staged)** — γ starts learning from the recorded ΔF∧ΔG trickle now; the rich signal arrives once R16 closes (armed actions return real outcomes). Burn-in, like R19-KL: machinery now, sharpens as data accrues.

## 4. ARGUE — decisions to ratify (open)

- **γ scalar, default 1, reduction-safe** — vs per-context/per-action-class γ (defer).
- **Learning signal = expected-vs-realized G rolling-error** (the R16 ΔF∧ΔG record) — vs reusing R12 Beta-credit (which is per-action-class follow-through = the KL `p`, a *different* quantity than decision decisiveness). Which signal, and is the ΔF∧ΔG record the right realized term?
- **Modulation form** `τ_eff = τ_spread / γ` — vs replacing the spread-τ entirely with a γ-only temperature.
- **Staging** — build the γ machinery + wiring + the trickle feed now; full learning gated on R16 closing. (Honest, like KL.)

## 5. Exit conditions (provisional until ARGUE)

1. γ is a **bounded, trace-persisted** quantity, default 1.0 (selection identical to today at the default).
2. γ **updates** from the realized-vs-expected-G history (a run of well-realized plans raises it; a run of misses lowers it), off the per-action hot path.
3. **Selection reads γ**: the effective temperature is `τ_spread / γ` — demonstrated: a higher γ sharpens the softmax (more commitment), a lower γ flattens it (more abstain/explore).
4. **Safe + reduction-safe**: γ clamped to [floor, cap]; with no history, γ=1 and behaviour is byte-identical to the current τ path.
5. A burn-in observability note (sibling to README-loss): what γ trajectory to watch.

## 6. Scope-out (named)

R7 channel precision (done); the hierarchical/deep-temporal precision where an upper level sets γ for a lower (R15); arming/acting on the sharpened selection (R16/WM-I4); the R14 contract text + A/B/C/D/E diff (M-aif-wiring). This excursion is **only** the learned, bounded, wired γ.
