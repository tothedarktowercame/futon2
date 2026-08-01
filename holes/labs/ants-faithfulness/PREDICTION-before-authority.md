# Preregistered prediction, written BEFORE the authority scan reported

**Written 2026-08-01 by claude-4, after codex-8's static scan (2818016) and
BEFORE codex-9's causal-authority result exists.** Committed with a timestamp so
that the authority result is a test of this prediction rather than a
confirmation of it. If the numbers contradict what follows, this file stands as
written and the contradiction is the finding.

## The structural fact, verified independently by the author

`policy.clj:612-624`:

```clojure
pred-means     (into {} (for [k sensory-keys] [k (double (get outcome k 0.0))]))
pred-variances (or (:var mu) (into {} (for [k sensory-keys] [k 0.01])))
```

`pred-means` is derived from `outcome`, which is **action-dependent**.
`pred-variances` is the ant's **current belief variance** and is **identical for
every candidate action**. The Gaussian ambiguity term is computed from those
variances (`efe.clj:17-21`).

**A quantity identical across all candidates is a constant in the softmax, and a
constant cancels under normalisation.** The canonical epistemic/ambiguity leg of
expected free energy therefore contributes *exactly nothing* to which action the
ant selects. Not "little" — nothing.

## What this predicts about Slice 5, which was nearly run

Slice 5's preregistered contrast is `aif-full − aif-no-epistemic > 0` on patchy
and sparse, `≈ 0` on snowdrift — the dissociation that would show the epistemic
term is the explore driver.

**The canonical part of that contrast was structurally incapable of producing an
effect**, in any environment, at any food density. Zeroing an action-independent
constant changes no softmax output.

It is NOT a prediction of exactly zero, because two *other* epistemic quantities
in the same function ARE action-dependent — `info-gain` and the directed-EIG
proxy over the food-belief (`food_belief.clj:101-149`). codex-8 badges those as
analogical: `food-prob * uncertainty`, not expected posterior entropy reduction.
So an `aif-no-epistemic` arm would ablate a **hand-shaped proxy** while the
canonical term it is named for was already inert.

This retro-explains the lane's history without needing to attribute anything to
bad faith: `8d78027` "harsher env + re-test" and `79ac385` "scarcity attempt"
are what chasing a structurally-impossible effect looks like from the inside.
The environment was adjusted because the effect would not appear; it would not
appear because the term that was supposed to produce it cancels.

## The prediction, stated so it can fail

For codex-9's arms (A0 baseline, A1 uniform-random over the same candidate set,
A2 score-permuted, A3 τ→∞):

1. **A0 − A1 will be small relative to arm means on all three scenarios**, with
   95% CIs at or near zero on patchy and sparse. Confidence: high.
2. **A3 ≈ A1**, since τ→∞ and uniform-random-over-the-set are near-equivalent
   given that the candidate set is fixed upstream by the mode FSM and hard
   filtering. Confidence: high.
3. **A0 − A1 will not be exactly zero**, because the risk leg and the
   action-dependent proxies do carry some signal. Confidence: medium.
4. **Whatever authority exists will be attributable to the RISK leg and the
   hand-written biases, not to the epistemic leg.** The scan cannot separate
   these; a later arm ablating risk alone would be needed. Confidence: medium.
5. **Starvation rates will be similar across A0/A1/A3**, because survival is
   carried by the deterministic mode FSM (`affect/next-mode`, hysteresis over
   cargo/home/reserve thresholds), which none of the arms touch. Confidence: high.

If (1) and (2) hold, the honest conclusion is that the ant controller's
*scoring* has low causal authority while its *gating* — which is not active
inference — carries the behaviour. That would make the `cyberants-replay` null
fully explained and would put Slice 5 off the table until the policy-selection
rework is built.

**What would falsify this:** a large, CI-excludes-zero A0 − A1 on patchy or
sparse. That would mean the scoring is doing real work despite the inert
ambiguity leg, and would make the risk leg and the proxies more load-bearing
than this analysis credits.
