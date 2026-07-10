# Cascade structural metrics (post-hoc, arm-labeled)

| deposit | batch | arm | boxes | seq | copar | copar% | depth | meets | chain? | holes |
|---|---|---|---|---|---|---|---|---|---|---|
| ft-chipwitz-corps-012 | 1 | gfn | 1 | 0 | 0 | 0.0 | 1 | 0 | single | 5 |
| ft-futonzero-generative-012 | 1 | gfn | 5 | 2 | 3 | 0.6 | 3 | 3 | lattice | 4 |
| ft-learning-loop-012 | 1 | gfn | 5 | 3 | 2 | 0.4 | 3 | 5 | lattice | 4 |
| ft-chipwitz-corps-013 | 2 | gfn | 5 | 3 | 2 | 0.4 | 4 | 4 | lattice | 5 |
| ft-chipwitz-corps-014 | 2 | incumbent | 9 | 3 | 5 | 0.62 | 3 | 5 | lattice | 6 |
| ft-futonzero-generative-013 | 2 | gfn | 5 | 2 | 2 | 0.5 | 3 | 3 | lattice | 5 |
| ft-futonzero-generative-014 | 2 | incumbent | 1 | 0 | 0 | 0.0 | 1 | 0 | single | 6 |
| ft-learning-loop-013 | 2 | gfn | 5 | 1 | 3 | 0.75 | 2 | 1 | lattice | 5 |
| ft-learning-loop-014 | 2 | incumbent | 3 | 2 | 1 | 0.33 | 2 | 3 | lattice | 5 |
| ft-autoclock-in-002 | 3 | gfn | 5 | 2 | 3 | 0.6 | 3 | 4 | lattice | 5 |
| ft-autoclock-in-003 | 3 | incumbent | 1 | 0 | 0 | 0.0 | 1 | 0 | single | 5 |
| ft-canon-fingerprint-store-002 | 3 | gfn | 5 | 2 | 3 | 0.6 | 3 | 3 | lattice | 5 |
| ft-canon-fingerprint-store-003 | 3 | incumbent | 1 | 0 | 0 | 0.0 | 1 | 0 | single | 5 |
| ft-state-snapshot-witness-002 | 3 | gfn | 4 | 2 | 2 | 0.5 | 3 | 4 | lattice | 5 |
| ft-state-snapshot-witness-003 | 3 | incumbent | 7 | 4 | 3 | 0.43 | 4 | 5 | lattice | 6 |

## Per-arm means

```
{
 "gfn": {
  "boxes": 4.44,
  "seq": 1.89,
  "copar": 2.22,
  "copar_frac": 0.48,
  "depth": 2.78,
  "meets": 3.0,
  "holes": 4.78,
  "n": 9,
  "chains": 0
 },
 "incumbent": {
  "boxes": 3.67,
  "seq": 1.5,
  "copar": 1.5,
  "copar_frac": 0.23,
  "depth": 2.0,
  "meets": 2.17,
  "holes": 5.5,
  "n": 6,
  "chains": 0
 }
}
```

## Arm comparison — the honest reading (2026-07-11)

Raw means mislead (GFN copar_frac 0.48 vs incumbent 0.23): the gap is driven by
the incumbent's SINGLE-PATTERN cascades, which have no structure to draw.
Multi-pattern only (boxes >= 3): GFN 0.54 (n=8) vs incumbent 0.46 (n=3) — both
arms fold as lattices when they compose at all. ZERO pure chains in either arm.

So "semilattice vs list" does NOT differentiate the arms — the folder (same
blind runner per batch) found co-application structure wherever there was
anything to wire. The visible differentiator is COMPOSITION-SIZE DISTRIBUTION:
  gfn        8 lattices + 1 single   (consistently mid-sized: 4-5 boxes)
  incumbent  3 lattices + 3 singles  (bimodal: minimal or large)
A 1-box wiring is the degenerate case nearest the "list that folds empty" —
no meets, nothing to chain. The incumbent's greedy coverage-saturation stop
produces that degenerate case half the time on these psis; the GFN never
stopped below 4 boxes except once.

Caveats: n=15; wiring is authored by the folder from the pattern set (same
folder per batch, blind to arm — folder bias controlled, but this measures
what the pattern set AFFORDS, filtered through one folder's judgment).
