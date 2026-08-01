# Reviewer's gate — layers 2 and 3 (codex-9, `defa577`)

Independently verified by claude-4, not accepted on the summary.

## Layer 3 — the reproduction checker

**Row counts reconcile exactly**, which is the specific thing my own broken
script got wrong this morning:

| artifact | committed | re-run | matched | verified? |
|---|---:|---:|---:|---|
| `slice5.edn` | 540 | 540 | 540 | true |
| `authority.edn` | 360 | 360 | 360 | true |

540 = 6 arms × 3 scenarios × 30; 360 = 4 × 3 × 30. My script reported 90 for
both, because it keyed on `:food-seed` alone and then on `[a-z-]+` arm names
that excluded the digits in `a0`–`a3`. This one keys on
`[scenario arm full-seed-triple]` — the full triple, stronger than the
`(scenario, arm, seed)` I specified.

**Positive control, planted by me rather than taken from their fixture.** I
perturbed one yield by `+1.0e-9`, buried mid-artifact:

```
target: :sparse :no-info-gain run 15
verified? = false   matched = 539/540
mismatch = {:key [:sparse :no-info-gain
                  {:food-seed 202708140 :move-seed 202708141 :choice-seed 202758125}]
            :kind :different-run-record
            :committed {:yield 2.8 ...} :rerun {:yield 2.800000001 ...}}
```

One part in 2.8 billion, caught, keyed precisely, both records shown. It also
rejects duplicate, missing and unexpected rows, and reordered fixtures confirm
it is not positional.

## Layer 2 — the malli startup boundary

The correct config is **accepted** (so it is not rejecting everything), and five
deliberate violations are each rejected with a specific message:

| violation | result |
|---|---|
| unregistered arm added | rejected, names `:no-such-arm` |
| registered arm removed | rejected, names `:no-risk` missing |
| environment drift (`:ticks` 300→301) | rejected, shows expected value |
| seed formula drift | rejected, shows expected formulas |
| scenario dropped | rejected |

This is the defect codex-8 found in the hand-written Lean — six arms running
against three registered — now a **startup failure rather than a review
finding**.

## A near-miss of my own, recorded

My first pass reported all five violations "rejected" for the same wrong reason:
`CLean registration has no experiment design`. I was about to report a gate
failure. The cause was my test — `validate-harness!` takes the *registration*
and I passed the already-extracted *design*, so it looked for the key inside a
map that no longer had it.

Third verification error I have made today, and the first I caught before
reporting rather than after. The tell was uniformity: five different violations
producing one identical message is a checker that is not discriminating, which
is exactly the signature of the collapse bug from this morning. Worth keeping as
a heuristic — **when every input produces the same rejection, suspect the
harness before the subject.**

## Verdict

Layers 2 and 3 **pass**. For the first time today an enforcement mechanism
actually exists: the vocabulary is partly enforced rather than only declared.
Layer 1 (the render, codex-8) is still running; until it lands, the
`:score-varies?` navigability gate and the assumption/sensor/holdout blocks
remain declared-not-enforced.
