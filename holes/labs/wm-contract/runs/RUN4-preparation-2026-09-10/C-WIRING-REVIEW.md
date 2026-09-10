# Independent RUN4 C wiring review

Verdict: **ACCEPT** commit `34fb1758d65cf383aae8b1abe7a31402791dbab5` as a
reviewed, opt-in wiring change. This is preparation evidence only. It is not
authority to launch RUN4, take a run lock, or write run data.

Reviewer: `codex-11` (independent of the author, `codex-17`). Review date:
2026-09-10.

## Recorded-commit review

I reviewed the tree recorded at `34fb1758`, rather than relying on the current
working tree. Its scope is the proposed RUN4 sheet, the wiring README and
certificate/proof, two pinned resources, the resolver, the judge boundary,
the EFE scorer, trace projection, and one focused test namespace (11 paths;
351 insertions and 5 deletions). `git diff --check 34fb1758^ 34fb1758` passes.

The proposed sheet's relative paths resolve beside the sheet. Rehashing the
recorded resource bytes reproduces both pins exactly:

- `resources/run4/seeded-c.edn`:
  `1459952e2d3e4534760ba8512482fc3cb3aa4020d2c3464b2056efaff6112e1f`
- `resources/run4/checkpoint-kernel.edn`:
  `39996c267ba36fc7cfbfb53a5821f304a4ff21089ff53bff031090e8f23e90d0`

The recorded `runtime-mass-binding.lean` hashes to
`305222dea73f92c92bf89166be7423e12d1eae04eb3b40e5067b338911fc6659`,
matching the certificate, and the certificate's changed `efe.clj` pin
(`5158ec4b...`) also matches the recorded scorer bytes. No frozen-statement or
placeholder-count gate is asserted by this wiring packet.

## Behavioral findings

- The resolver consults `FUTON_WM_RUN_CONFIG` only when the caller did not
  explicitly supply `:ruled-outcome-c-enabled?`. Explicit `false` therefore
  bypasses the sheet and artifact readers. Absence also returns the input opts
  unchanged. The focused tests verify byte-identical (`pr-str`) legacy scorer
  output and absence of trace provenance in both cases.
- In the enabled lane, the resolver checks the selected seed and kernel names,
  verifies both source digests, verifies the parsed seed against the canonical
  ruled-outcome seed, and constructs the named constant-checkpoint adapter.
  Missing, malformed, unknown, and pin-mismatched configurations refuse with
  typed `ExceptionInfo` reasons.
- The actual stepped judge path calls `configured-fold-efe-opts` before both
  real `efe/rank-actions` passes. That helper resolves the run sheet and passes
  `:seeded-c`, `:disposition-kernel`, enablement, and provenance into
  `compute-efe`. Enabled scores carry the provenance; `strip-ranked-action`
  preserves it together with `:G-ruled-outcome-c` and
  `:predicted-disposition-risk`. The new focused test exercises the real
  diagnostic judge-options helper, this materialization helper, the real
  scorer, and the real trace projection, while deliberately not executing a
  full judge scan or live tick.
- The constant kernel yields `log 2` and is explicitly marked constant across
  policies, so this packet does not claim selection discrimination or close
  the observation-model bridge.

## Independently reproduced checks

Each test namespace ran in its own Clojure process:

```text
clojure -M:test -m cognitect.test-runner -n futon2.aif.c-fold-config-test
  3 tests, 16 assertions; 0 failures, 0 errors

clojure -M:test -m cognitect.test-runner -n futon2.aif.efe-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.policy-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.trace-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.disposition-risk-test
clojure -M:test -m cognitect.test-runner -n run-tick-once-test
  aggregate: 153 tests, 661 assertions; 0 failures, 0 errors

bash ../futon4/dev/check-parens.sh <five touched Clojure files>
  OK

clj-kondo --lint <five touched Clojure files>
  0 errors, 1 warning
```

The sole lint warning is the known pre-existing unused private var
`futon2.aif.efe/ambiguity`; it was reproduced and not suppressed.

No code fix, live tick, run lock, run-data write, shared-JVM load,
registry/worklist edit, sealed-holdout read, or Claude call was performed.
