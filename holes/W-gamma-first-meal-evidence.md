# W-gamma-first-meal — verdict and evidence (2026-07-06)

## Verdict: **RESOLVED — gamma has cargo.**

Three gated missions with replayable deposits AND ΔF > 0 exist. The gamma
feed wiring (live since d05dd35, `*gamma-escrow-feed?*` ON) would ingest
them on the next live tick where one is the top-ranked :pass. No enactment
was performed (per the dispatch rules).

## The qualifying deposits

Offline check over all 13 loaded deposits (act-gate = ΔF ∧ ΔG):

| Deposit | Mission | ΔF (F-free-energy) | ΔG (coverage) | Gate |
|---|---|---|---|---|
| ft-evaluate-policies-009 | futon2-d/mission/evaluate-policies | **3.999** | −0.615 | **:pass** |
| ft-legacy-sorry-cleanup-001 | futon2-d/mission/legacy-sorry-cleanup | **1.113** | −0.571 | **:pass** |
| ft-aif-faithfulness-001 | futon2-d/mission/aif-faithfulness | **0.296** | −0.556 | **:pass** |

All three have positive ΔF and negative ΔG (escrow-sourced). The act-gate
verdict for each is `:pass`.

## The offline proof (ft-evaluate-policies-009)

Command: `clojure -M -e '(load-file "/tmp/gamma-meal-proof.clj")'` in futon2.

```
ACT-GATE:
  delta-F: 3.999
  delta-G: -0.6153846153846154
  delta-G/source: :fold-escrow
  has :fold-escrow: true
  VERDICT: :pass

GAMMA FEED LEG:
  gamma-fold-of returns escrow fold: true
  gamma-expected-G: -0.6153846153846154
  gamma-expected-G matches coverage ΔG: true

=== VERDICT ===
GAMMA FIRST MEAL: WOULD FEED ✓ — ft-evaluate-policies-009 qualifies
```

The path verified:
1. `act-gate-from-lane-entry` with escrow replay ON → ΔG sourced from
   `:fold-escrow` (the pinned deposit, not the classical fold).
2. `preview-verdict` → `:pass` (both legs present and correctly signed).
3. `enact/gamma-fold-of` → returns the escrow fold (source-consistent,
   `*gamma-escrow-feed?*` ON, `:delta-G/source` = `:fold-escrow`).
4. `:delta-g` of the gamma fold = −0.615 (non-nil) → γ would receive a
   real expected leg, not hold.

F stability verified: re-running the constructor on the evaluate-policies ψ
yields F=3.999 (identical to the deposit's recorded value).

## Flags (all in the correct position)

- `*escrow-replay?*` = true (2g arming)
- `*classical-fold-dG?*` = false (L4 ruling)
- `*gamma-escrow-feed?*` = true (d05dd35 repair)

## Why gap 1 did not block this

Gap 1 (the ΔF last inch) is about **M-first-flights** specifically — its
best lane ψ draws F=−0.024, 0.024 short of the gate. But the gamma feed
needs *any* gated mission with ΔF > 0, not first-flights specifically.
Three other missions have crossed the gate. The campaign doc's "blocked
exactly by gap 1" was written when no mission had ΔF > 0; the overnight
and morning deposit runs (2026-07-06) changed that.

## What remains

- A **live tick** is needed to actually enact one of these and feed γ.
  The tick has not run since 2026-07-05 (disk-full: "No space left on
  device" on the evidence store). The feed wiring is proven correct
  offline; enactment requires the disk issue resolved and the tick
  running.
- The gamma feed would then need burn-in + ~24 fed ticks to evaluate
  card 7's pre-registered thresholds (paying-lane γ ≥ 1.10, futile-lane
  γ ≤ 0.90). This evidence is the first-meal precondition, not the full
  card 7 evaluation.
