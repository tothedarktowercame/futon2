# M-wm-policies — DOCUMENT: a real run of the WM, new features at work (2026-06-24)

*What this is (Joe's DOCUMENT ask): real output from the **live** WM/AIF apparatus showing each M-wm-policies
feature firing, and **the difference it makes** vs the pre-mission behaviour. Captured against the running futon3c
JVM (served judgement `/api/alpha/war-machine` + live `futon3c.portfolio.*` / `futon2.aif.rollout` /
`futon2.report.cascade-lane` calls). Not a fixture — the served snapshot is a real scheduler tick.*

---

## 1. Track 1 — off-map demotion (R5: corrected pragmatic EFE term)

Live `ranked-actions` (served judgement, 107 candidates):
```
top-3:  (1) learn-action-class   (2) address-sorry sorry/pattern-measure-never-target   (3) address-sorry …
cursor (M-emacs-cursor-peripheral) rank: 55, 63
```
**Difference.** Pre-mission the cursor was **#1** (G = −9.25) — the long-noted silly pick, because off-map work
scored a free `0.0` while on-ascent work was taxed by a whole-mission body penalty. With the off-map penalty +
leaf-aware + status-aware corrections live, the cursor sits at **rank 55/63** and on-ascent / concrete work
(learn-action-class, address-sorry) tops the ranking. *The headline ranking bug is gone, on live data.*

## 2. Track 3 reactive — niche-construction instead of freeze (R6 + R17 + real EIG)

Same **flat field** (τ=0.3 → abstain; gap/stall/blocked = 0.9), old vs new selection:
```
abstain? true
OLD behaviour:  :wait        (effect :none — the WM freezes; the operator must notice)
NEW behaviour:  :acquire-patterns
   :acquire-patterns  G = −0.22   (epistemic/EIG = 0.429)
   :wait              G = −0.15   (epistemic = 0.0)
```
The new action wins the abstain branch because its **real expected-information-gain** (0.429 = normalized Shannon
entropy of the action-posterior × low-confidence) dominates — not a hand-tuned constant.

What `:acquire-patterns` then **does** (the effect, on a real stalled mission):
```
execute-effect! :acquire-patterns  (|ψ = M-substrate-metric>)
  → proposed cascade: math-formalization/metric-cauchy-convergence
     wholeness = 0.311   F-free-energy = −0.506
  note: "proposed cascade for |psi=M-substrate-metric> — 1 patterns, wholeness=0.311"
```
**Difference.** Stuck → *build a candidate policy and diagnose it* instead of stuck → *freeze → escalate to the
operator*. And the act-gate is honest: F-free-energy = −0.506 < 0 says this candidate's coverage doesn't yet pay
for its complexity — i.e. the WM reports "this really is a thin pattern-class (seed here)," rather than pretending.

**No-hijack (the τ-gate works):**
```
calm + low-τ (abstain, but field NOT flat)  → :wait     (acquire's EIG → ~0, nothing to gain)
confident field (τ ≥ 0.55, no abstain)      → :review   (a pragmatic action wins)
```
So `:acquire-patterns` only fires when the WM is genuinely stuck on a flat field — it cannot hijack normal operation.

## 3. Track 3 proactive — defensive-driving gap-map (R17, served `:pattern-gaps`)

Live `:pattern-gaps` lane on the served judgement (open missions whose best cascade fails the AIF accept test):
```
M-bayesian-structure-learning   F-free-energy = −0.01   gap? = true    (acc 0.32 < λ·complexity → seed here)
M-canon-fingerprint-store       F-free-energy = +0.005  gap? = false   (just net-positive)
```
**Difference.** The WM now **scans the horizon** and flags the *classes it has no good-enough patterns for*
(here the futon6 math/Bayesian missions — independently the VWM's gap cluster) — "seed patterns before charging
in," rather than only reacting once already stuck in a mission. *Honest edge:* both sit right at the F=0 knee, so
the verdict is ψ-sensitive at the boundary (the clear cases — rich size-5–7 vs thin size-1 — are robust).

## 4. Cascade lane — the visible non-degenerate policy (R13 semilattice grain)

Live `:cascade-policies` (the on-the-fly Alexander pattern-language per circumstance, scored by wholeness):
```
M-canon-fingerprint-store        wholeness = 0.458   size = 1
M-bayesian-structure-learning    wholeness = 0.832   size = 2
```
**Difference.** Before, the WM only ranked single next-steps; now a *scored multi-pattern policy* (a semilattice,
not a sequence) is surfaced per mission, with the AIF **F-free-energy = accuracy − λ·complexity** as its
Bayesian-Occam act-gate leg (replacing the Salingaros-`C` analogy).

## 5. R13 — multi-step `G(π)` beats greedy (the rollout, reproduced live)

`futon2.aif.rollout` on a constructed 2-step case (`root → bridge → agency`, where step-1 unlocks step-2's `have`):
```
greedy-one-step:  G = −0.20   policy = [root->bridge]                  (can't see past the bridge)
best-rollout(2):  G = −1.01   policy = [root->bridge, bridge->agency]  (unlocks the high-value cap)
```
**Difference.** The WM can now value a move that is **bad alone but good as step-1 of a policy** — the
field→path-integral generalization that single-step EFE structurally cannot express. This is the witness that
created R13.

---

## Summary — feature × difference × R-criterion

| Feature | Old behaviour | New behaviour (live) | R |
|---|---|---|---|
| Off-map demotion (Track 1) | cursor #1 (G=−9.25) | cursor rank 55/63; on-ascent work tops | R5 |
| Niche-construction on stall (Track 3) | `:wait` / freeze / escalate | `:acquire-patterns` → propose+diagnose a cascade | R6, R17 |
| Real EIG epistemic | flatness-count proxy | action-posterior entropy (0.429) — wins the abstain branch | R5 (info-gain) |
| Defensive-driving gap-map (Track 3) | none | `:pattern-gaps` flags pattern-poor classes (F≤0) | R17 |
| Cascade lane / `F-free-energy` | (Salingaros-`C` analogy) | scored semilattice-policy + real ΔF act-gate | R13, R18 |
| Multi-step `G(π)` rollout | single-step EFE only | best −1.01 vs greedy −0.20 | R13 |

**Net:** every M-wm-policies feature fires on live/real data, and each makes a concrete difference vs the
pre-mission WM. The one thing *not* shown acting is Car-3 (the WM autonomously **applying** a chosen policy) —
deliberately HELD for operator arming (the R16 loop-closure + consent gate). Everything above is propose/score/
display, read-only.
