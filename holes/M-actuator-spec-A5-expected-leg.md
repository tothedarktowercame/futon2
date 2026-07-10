# Spec: A5 two-sided expected leg (γ can commit, not just hedge)

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **Extends:**
`futon2/src/futon2/aif/fold_realized.clj` (`realized-outcome-grounded`). **Pays down:** R14
(γ gets a signal that swings both ways). **Distinct from** the R12/R13 re-ranking known-gap
(Claude-5's thread) — this is γ *calibration*, not policy re-ranking.

## The problem (demonstrated, not hypothetical)
`realized-outcome-grounded` currently emits `:expected-G 0` — a fixed "fully discharge" target.
So `perf = (0 − realized)/…` is only ever `0` or negative: pushing the 5-sample set walked γ to
**0.66** (mean-perf −0.6) and it can only fall to the floor. Nothing can beat perfection, so γ
can hedge but never **commit**. We need the expected leg to be a real *forecast* a build can beat
*or* miss.

## The fix — expected = the fold's own coverage forecast, in endpoint-count units

Keep `:realized-G = (- bound inhabited)` (the substrate dial, unchanged). Replace the fixed
`:expected-G 0` with the **fold's predicted remaining**:

```
predicted-coverage = fold-eval/coverage over the DEPOSIT's fold wiring   ; folded/(folded+holes) ∈ [0,1]
:expected-G        = (Math/round (* bound (- 1.0 predicted-coverage)))   ; predicted remaining, endpoint-count
```

Then `perf = (expected-G − realized-G)/(|expected-G| + |realized-G| + ε)` swings **both ways**:
- build inhabited **more** than the fold forecast (`realized-remaining < predicted-remaining`) ⇒ perf > 0 ⇒ **commit**
- build inhabited **less** ⇒ perf < 0 ⇒ **hedge**

Same SCALE-MATCH discipline: both legs endpoint-count, never mixed with coverage-ΔG.

### Source & fallback (label it)
- If the deposit carries a fold plan (a `:turn/:answer` wiring or `:wiring` with boxes+holes so
  `fold-eval/coverage` is computable): use it. Set `:expected-source :fold-coverage`.
- If not (a hand-authored CLean with no fold prediction — e.g. the done-mission candidates):
  fall back to `:expected-G 0`, `:expected-source :perfection-target` (honestly one-sided). Do NOT
  silently pretend a forecast exists.

Keep `:realized-source :substrate-dial`, the `:dial` map, the anti-tautology flag, and the
`*live-wire?*` gating exactly as they are.

## Acceptance (tests)
1. **Two-sided proof (the point):** a deposit whose fold forecast is LOW (high predicted-remaining)
   but whose build inhabited HIGH (low realized-remaining) → `perf > 0` (commit). And the mirror
   (fold forecast high, build low) → `perf < 0`. A fixed-`0` expected could never produce the first.
2. **Fold-coverage is actually used** when available: `:expected-G` ≠ 0 for a fold-deposit mission
   with partial coverage, and `:expected-source :fold-coverage`.
3. **Fallback labelled:** a hand-authored-CLean mission (no fold plan) → `:expected-G 0`,
   `:expected-source :perfection-target` — not a silent perfect-forecast.
4. Units stay endpoint-count; anti-tautology flag and `*live-wire?*` gate preserved; the old
   coverage producer (`realized-outcome-of`) still intact.

## Gates
clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green. Reuse `fold-eval/coverage`
and `box-match-snapshot`; no `:7071` restart. Bell **claude-4** back with commit sha + a
two-sided example (one commit-side sample, one hedge-side sample).
