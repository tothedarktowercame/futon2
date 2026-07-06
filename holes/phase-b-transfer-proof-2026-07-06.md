# Phase-B transfer proof — M-first-flights policy-grade G(s,π)

**Date:** 2026-07-06
**Author:** zai-5 (ground-control demo dispatch from codex-1)
**Mission:** M-first-flights
**Sorry:** `:sorry/first-flights-phase-b-policy-grade-G`
**Purpose:** determine whether Phase B still belongs as open work under M-first-flights, or whether it has a named successor owner and should be closed/transferred. No closure edits — evidence only.

## Verdict: TRANSFERABLE

Phase B's obligation — "flight records must carry derivations, ghosts, and warrants" at policy grade G(s,π) — has a named successor: the **fold-realized → γ calibration seam** (`futon2.aif.fold-realized`), live-loop III.1. The obligation is partially discharged (the realized-outcome record exists and produces calibration samples) and partially held (the `*live-wire?*` flag is off — enactment is not yet wired into the pilot, which is Joe's call).

## What successor machinery owns the obligation

**`futon2.aif.fold-realized`** is the direct successor:

1. **Expected vs realized G** — `realized-outcome-of` produces `{:policy :expected-G :realized-G :tick}`, where both legs are the SAME coverage→rollout ΔG quantity. This is the "policy-grade G(s,π)" shape: a policy (the fold), an expected G (the gate's decision-time ΔG), and a realized G (post-enactment re-observation). The mission's Phase B exits 6-8 (prediction organ's policy-grade slot, plan-vs-realised, cascade scored as policy) are all framed in terms of this expected/realized pair.

2. **Zero-coverage semantics** (claude-16 ruling 2026-07-06) — an executor that constructs 0 boxes against N obligations produces a measured realized-G of 0.0, not nil. This is the "derivations, ghosts, and warrants" requirement made machine-checkable: the ghosts (policy-holes) are typed, the warrant (the coverage computation) is recomputed, and the derivation (expected→realized signed error) is a calibration signal, not a bare number.

3. **The γ calibration pair** — the realized-outcome feeds γ (R14), which is the loop-closing demonstration Phase B exit 5 asks for (at action grain today, policy grain when enactment is live-wired).

**What remains held:** `*live-wire?*` defaults false — enactment (`apply-cascade!`) is not wired into the pilot. This is Joe's call (the 2026-06-26 incident discipline). The PURE path is available and tested; the LIVE path awaits operator consent.

**The arming condition revision (checkpoint 22):** the mission itself revised Phase B's arming from "rollout engine lands" to "terms constructed + metric understood" — and the terms ARE constructed (fold_realized.clj) and the metric IS understood (coverage→rollout ΔG, same quantity on both legs). The revised arming condition is met in the PURE/tested sense; the LIVE wiring is the remaining gate.

## How the sorry registry should be marked

**Recommendation: mark `:addressed`** (not `:foreclosed`, not stay `:open`).

**Rationale:**
- `:addressed` is the correct status because the obligation has a named successor that implements the core requirement (expected/realized G pair with typed ghosts and recomputable warrants). The machinery exists, is tested (fold_realized_zero_coverage_test.clj — ALL TESTS PASS), and produces real calibration samples over qualifying deposits.
- NOT `:foreclosed` because the obligation is genuinely discharged into live machinery, not abandoned or ruled out of scope. The successor owner (`futon2.aif.fold-realized` + the γ calibration seam) is alive and under active development (live-loop III.1).
- NOT stay `:open` because the sorry's own rationale ("flight records are lists of numbers and nulls — judgments without derivations") is no longer true at the fold-realized grain: the realized-outcome record carries policy, expected-G, realized-G, and tick — a derivation, not a bare number. The bare-scalar problem remains for the SUBSTRATE debt (homed on M-substrate-metric, explicitly out of scope per §4), but the Phase-B obligation as stated ("derivations, ghosts, and warrants") has a concrete owner.
- The `*live-wire?*` flag being off is a consent gate, not a design gap — the mission's own §4 boundary says "proposal-mode stays a typed absence, this mission makes it renderable, not fillable." The live-wiring is Joe's call, same as it was at checkpoint 22.

## Tests/commands run

```
$ cd /home/joe/code/futon2 && clojure -M test/fold_realized_zero_coverage_test.clj
=== TEST 1: zero-construction enactment → realized 0.0 ===
  4/4 PASS
=== TEST 2: devmap-coherence fixture → realized >0 boxes ===
  3/3 PASS
=== TEST 3: anti-fake-calibration guard ===
  3/3 PASS
=== TEST 4: full gamma sample (expected + realized) end-to-end ===
  ft-aif-faithfulness-001: expectedG=-0.556 realizedG=0.0 signed-error=0.556
  ft-evaluate-policies-009: expectedG=-0.615 realizedG=0.0 signed-error=0.615
  ft-legacy-sorry-cleanup-001: expectedG=-0.571 realizedG=0.0 signed-error=0.571
  6/6 PASS
=== BONUS: nil/empty wiring → realized-G nil ===
  2/2 PASS
ALL TESTS PASS
```

## Summary

Phase B is TRANSFERABLE. The successor is `futon2.aif.fold-realized` (the fold-realized → γ calibration seam, live-loop III.1). The sorry should be marked `:addressed` — the machinery exists, is tested, and produces the derivation/ghost/warrant shape the obligation demands. The `*live-wire?*` flag (enactment wiring into the pilot) is Joe's consent gate, not a design gap. Mission-close remains the operator's call.
