# C349 — receipt-validated writer-fence capability

## Result

`writer_fence_capability.clj` is the single conversion from transported fence
material to an event-freedom claim. It requires both an identifier and a
receipt, validates the receipt, and re-runs `writer_fence_evidence.py` against
the live world. C363 superseded the original private-token implementation:
Clojure privacy is not an integrity boundary, so no reusable bearer capability
is now issued.

The three acceptors now consume that shared verification path:

- `wm_workspace_gate.clj` and `contract_authority_current.clj` emit a
  fence-conditional verdict only after observed verification;
- `mutable_read_set.clj` no longer accepts caller-declared `:held` maps;
- `wm_preflight.clj` uses the same verifier instead of retaining a parallel
  implementation.

`run_workspace_gate_bounded.py` remains transport-only. It carries both
`FUTON_WRITER_FENCE_ID` and `FUTON_WRITER_FENCE_EVIDENCE`, and rejects a lone
member of the pair; it derives no claim.

## Controls and canonical invocations

```sh
clojure -M:test -m cognitect.test-runner \
  -n writer-fence-capability-test -n wm-preflight-test -n mutable-read-set-test
```

Historical C349 result: 10 tests, 46 assertions, zero failures/errors. C363
supersedes the synthetic-capability control with subprocess-bound controls;
see its note for the current invocation and counts.

```sh
bb checks/contract_authority_current.clj --writer-fence fabricated
```

Exit 0 for the independent content claim, but explicitly
`PASS-CONTENT-ONLY (event-free unverified)`; the fabricated ID earns no event
claim. The distinction is intentional: failure to prove a fence does not make
the contract content false.

```sh
FUTON_WRITER_FENCE_ID=fabricated \
  python3 scripts/run_workspace_gate_bounded.py --command true
```

Exit 125 with `WRITER_FENCE_ID_AND_EVIDENCE_REQUIRED_TOGETHER`; no bounded job
is submitted.

```sh
clj-kondo --lint writer_fence_capability.clj \
  checks/contract_authority_current.clj checks/wm_workspace_gate.clj \
  checks/mutable_read_set.clj scripts/wm_preflight.clj \
  test/writer_fence_capability_test.clj test/wm_preflight_test.clj \
  test/mutable_read_set_test.clj
```

Exit 0, no errors or warnings.

Delivery inventory:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
