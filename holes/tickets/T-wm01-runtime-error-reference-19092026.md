# WM-01 P-4b: width repair and runtime reference boundary

Author: codex-1, 2026-09-19. Partial delivery; not node acceptance.

The numerical test now keeps rational endpoints through subtraction, ordering,
and margin comparison. It removes the artificial 1e-300 width floor and asserts
positive widths for the three nonzero-remainder fixture intervals. The margin
multiple remains 1000; it is a test constant, not a runtime uncertainty bound.

The new narrow-enclosure control uses width 1/10^24 at 1. Its near pair has
one width of separation; its far pair has 2001 widths. Both endpoints round to
1.0. With the former double-before-subtraction width implementation injected,
the control reports 2 failures and 1 pass: the measured width is zero and the
near pair incorrectly passes. With exact subtraction, the namespace reports
7 tests, 105 assertions, zero failures/errors. This is fixture execution, not
a live-tick receipt.

Reproduce the negative control from the repository root:

```sh
clojure -M:test -e '(require (quote futon2.aif.wm01-numerical-operations-test)) (binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)] (with-redefs [futon2.aif.wm01-numerical-operations-test/enclosure-width (fn [lo hi] (- (double hi) (double lo)))] (clojure.test/test-vars [(var futon2.aif.wm01-numerical-operations-test/narrow-enclosure-margin-control)])) (prn @clojure.test/*report-counters*) (System/exit (if (pos? (:fail @clojure.test/*report-counters*)) 0 1)))'
```

## Stop at part 2: specify which exact value the bound concerns

The fixture's `cg-step` supplies `c-float` from `preference-distribution` to
`cascade-g/step-g`. That scorer converts the supplied binary64 C coordinates to
represented rationals. Its exact reference retains that row, including its
nonexact normalization.

The live `rank-cascade-actions` calls `horizon-g-sparse-cert`. In the nonzero
rates branch, `horizon-g-sparse*` instead obtains `utility-weights` and computes
log-C using `softplus`, `Math/exp`, and `Math/log1p`. It never consumes that
materialized C row. Thus the previously measured score-minus-reference values
are discrepancies between two computation paths with different represented C
inputs, not an isolated measurement of arithmetic error against one common
exact specification. Exact rational subtraction fixes the width test but does
not resolve this distinction.

Following the packet's instruction to stop when a part changes the picture,
no runtime bound or live instrumentation is claimed. To continue, define the
reference as the exact utility-derived C law (including declared tempering and
zeroed outcomes), and derive bounds for that scorer's actual operations; or
explicitly include the materialized-row discrepancy as a separate term. Do not
reuse the rational log remainder alone or call a fixture maximum a bound.

The shared conversion removal remains unimplemented because work stopped at
this reference boundary. Existing source and data records are unchanged.
