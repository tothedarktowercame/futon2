# C363 — subprocess fence integrity boundary

## Decision and repair

Arbitrary code in the same Clojure process cannot be excluded from private
vars: `ns-resolve`, `var-get`, and `with-redefs` make namespace privacy a
convention, not an integrity boundary. C363 therefore removes the bearer-token
model rather than attempting a stronger private marker.

The sanctioned API is now `writer-fence-capability/assess`. It accepts the
transported fence ID and receipt path, launches the fixed
`writer_fence_evidence.py` subprocess itself, and immediately derives the
event claim. It returns no reusable authority. The three downstream acceptors
pass evidence coordinates, not caller-authored status maps. The public dynamic
runner test seam and the private token no longer exist.

This closes accidental and ordinary in-process forgery. It does **not** claim
security against arbitrary code already able to redefine functions in the
consumer JVM; Clojure provides no such boundary. The production subprocess is
the sanctioned observation boundary.

## Receipt integrity

Before live re-observation, the prior receipt must now contain nonempty
`classification.observed.start` and `.finish` populations and a well-ordered
observation interval ending no more than 300 seconds ago. Empty JSON, missing
observations, and replayed old intervals cannot establish a claim. Attestation
expiry and fresh live-world agreement remain independently required.

Synthetic test output is no longer capable of minting a production result:
there is no rebindable runner. Tests cover the rejecting paths; the genuine
fenced path remains the real `writer_fence_evidence.py` integration exercised
by the C337 fence rehearsal.

## Focused gates

```sh
clojure -M:test -m cognitect.test-runner \
  -n writer-fence-capability-test -n wm-preflight-test \
  -n mutable-read-set-test
```

Result: 11 tests, 34 assertions, zero failures/errors. Controls establish:

- `ns-resolve` finds neither `capability-token` nor `*run-evidence*`;
- a lone fabricated ID remains `:unverified`;
- an empty receipt is unavailable;
- an ancient observation interval is rejected as
  `:prior-observation-interval-stale`.

```sh
clj-kondo --lint writer_fence_capability.clj \
  checks/contract_authority_current.clj checks/wm_workspace_gate.clj \
  checks/mutable_read_set.clj scripts/wm_preflight.clj \
  test/writer_fence_capability_test.clj test/wm_preflight_test.clj \
  test/mutable_read_set_test.clj
```

Exit 0, no errors or warnings (one informational redundant-boolean notice).

Delivery inventory:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
