# C54 — goal-outcome risk replay inputs

Delivered 2026-08-31 by `wm-verbs`.

Each scored candidate now carries `:goal-outcome-replay-inputs`, version 1. It
records the open C entries actually scored (`:outcome-ref`, status, weight,
preferred outcome, and resolved current outcome), the relevant capability-graph
projection, the durable-join lookup projection, and one typed q-sat per entry.
An available probability is always `{:status :present :value n}`; in particular,
zero is not used to encode absence. Missing graph or durable-join inputs are
explicit `:absent` variants with reasons.

The score and evidence are produced by the same pure evaluation. The existing
hinge and KL entry points delegate to it, and the production scorer consumes its
`:score`, so the trace cannot silently describe a second calculation. Trace
schema version 15 persists the evidence. This changes neither the selected mode
nor any scoring formula.

## Prior calibration evidence retained

- Real-belly round trip: 411/455 entries were `:becomes`; at T=0.1 the unmet KL
  lane was approximately 9.6 times the hinge lane.
- No scalar T in [0.02, 2.0] matched hinge dispersion; at T=2 the uniform KL
  dispersion was still 20.3 times hinge.
- E6 changed the winner, moved all 110 candidate ranks, and had Spearman
  rho approximately 0.841. That shadow also changed the channel-risk lane, so
  the result cannot be attributed solely to goal-outcome risk.

These historical measurements are context, not assertions. Future comparisons
must consume the per-candidate replay payload from a pinned trace population.

Canonical verification:

```sh
clojure -M:test -m cognitect.test-runner \
  -n futon2.aif.c-vector-test -n futon2.aif.trace-test
clj-kondo --lint src/futon2/aif/c_vector.clj src/futon2/aif/efe.clj \
  src/futon2/aif/trace.clj test/futon2/aif/c_vector_test.clj \
  test/futon2/aif/trace_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el \
  --eval '(arxana-check-parens-cli)' -- --no-defaults \
  src/futon2/aif/c_vector.clj src/futon2/aif/efe.clj \
  src/futon2/aif/trace.clj test/futon2/aif/c_vector_test.clj \
  test/futon2/aif/trace_test.clj
```
