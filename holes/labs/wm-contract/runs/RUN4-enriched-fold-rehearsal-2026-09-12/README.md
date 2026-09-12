# RUN4 enriched-fold rehearsal — 2026-09-12

This is the positive-direction runtime rehearsal requested after commissioning
the enriched construction gate. It uses the genuine U88 v4 production
selection and construction as its frozen G/cascade input, then gives that
construction to a real Zai fold turn (`zai-5`, Agency job
`invoke-1789228389623-20416-1cb6bc77`). The response in `fold-output.edn` is
verbatim; it was not repaired after the turn.

The runtime results are green:

- `validate-fold-output-v1`: enriched, no findings;
- `validate-fold-correspondence`: no findings;
- `construction-wiring-result`: `:wired`;
- the cohort construction checkpoint validator, applied to the genuine v4
  construction cell with the persisted fold result: no errors.

The record deliberately does **not** claim that U88 completed. The fold exposes
mission review/closure and live adoption as obligation-bearing holes. Nor does
this rehearsal claim Lean validation or Lean attestation: it is runtime
validation of persisted records only.

Replay:

```sh
clojure -M -e '(require (quote clojure.edn) (quote futon2.aif.fold) (quote futon2.aif.full-loop-runner)) (let [o (clojure.edn/read-string (slurp "holes/labs/wm-contract/runs/RUN4-enriched-fold-rehearsal-2026-09-12/fold-output.edn")) c (clojure.edn/read-string (slurp "holes/labs/wm-contract/runs/RUN4-enriched-fold-rehearsal-2026-09-12/selected-cascade.edn"))] (prn {:fold (futon2.aif.fold/validate-fold-output-v1 o) :correspondence (futon2.aif.fold/validate-fold-correspondence o (:shown c)) :runner-gate (select-keys (futon2.aif.full-loop-runner/construction-wiring-result {:shown (:shown c)} (constantly o)) [:status])}))'
```

The exact source checkpoint and artifact digests are in `run-record.edn`.
