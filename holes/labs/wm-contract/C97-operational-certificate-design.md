# C97 — operational topology certificate

Date: 2026-08-31. Status: checker and fixtures delivered; no live certificate claimed while C95 has no completed tick.

The certificate is an independently generated post-run verdict over three inputs: exact `TickRunRecord` bytes, an external resource-status receipt, and the declared topology. The runner does not generate or approve it.

The topology authority is `p4ng/aif-control-map-paper.svg`, pinned at SHA-256 `568938d213c23c79f42a01a36547ced622deec64a3591f847c3e72a234b538ec`. Because the checker consumes the figure's C19-agreement data rather than parsing SVG geometry, it additionally pins `p4ng/empirics-futon/control-map-edges.edn` at `64485bb0165fe4abdaf799b59853c05efb0e09fbaf293bf9511d91e5098f509d`.

The certificate records:

1. explicit `:run/id` when present, otherwise the SHA-256 of exact record bytes, plus `:startedAt`;
2. both topology hashes and whether they match;
3. every run-record hop classified `:original`, `:measured`, or `:undeclared`;
4. separate undeclared traversal and declared-but-unexercised populations;
5. the external resource receipt and its effective clean verdict.

Pass requires a timestamped nonempty route, matching topology pins, zero undeclared hops, command exit zero, zero `pids.events:max` delta, and no native-thread-exhaustion marker. Missing resource evidence writes a failing certificate. Partial coverage is permitted but visible.

The current run evidence has exactly the required hop granularity: nine ordered hops with endpoints, implementation `:via`, and timestamp. It lacks an explicit run ID and resource status. C95 should add the former; C91's wrapper must supply the latter. Neither absence is silently filled.

Canonical positive:

```
bb -cp . checks/wm_operational_certificate.clj \
  --run holes/labs/wm-contract/tick-run-record-2026-08-30.edn \
  --resource test/fixtures/wm-operational-certificate/resource-clean.edn \
  --certificate /tmp/wm-operational-certificate.edn
```

Append `--negative` and use a distinct output path for the undeclared-hop falsifier. It exits 0 only when the emitted certificate has `:verdict :fail`; a falsely certified mutation exits 2. Normal failures exit 1 after writing the certificate.

Gate results: checker positive exit 0; undeclared-hop negative exit 0 with a written failing certificate; 3 focused tests / 9 assertions green; strict contract qualification PASS/exit 0 at the contract's own `.source["git-sha"]` authority; futon2 1,027 tests / 6,166 assertions green. The unchanged futon3 suite was last run at 248 tests / 1,518 assertions, green, immediately before this futon2-only delivery.
