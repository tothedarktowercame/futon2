# C349 — receipt-validated writer-fence capability

## Result

`writer_fence_capability.clj` is the single conversion from transported fence
material to an event-freedom capability. It requires both an identifier and a
receipt, validates the receipt, and re-runs `writer_fence_evidence.py` against
the live world. Its successful value carries a private in-process token;
copying or hand-authoring `{:status :observed-held}` cannot satisfy
`observed-held?`.

The three acceptors now consume that capability:

- `wm_workspace_gate.clj` and `contract_authority_current.clj` emit a
  fence-conditional verdict only for an observed capability;
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

Result: 10 tests, 46 assertions, zero failures/errors. One shared fixture proves
that a fabricated capability yields `:event-free? :unverified` at all three
acceptors while a receipt-validated capability yields `true` at all three.

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
