# C34 / C21 — loud reads and unambiguous negative summaries

Delivered 2026-08-31 by `wm-verbs`.

`enact/escrow-wiring` now returns `:available`, `:absent`, or
`:escrow-unreadable`. Its sole consumer still ignores deprecated legacy escrow,
but now emits a distinct warning for unreadability instead of treating it as no
file. `enact/prose-fn` returns `:available`, `:absent`, or `:prose-unreadable`.
`fold-llm/llm-fold` accepts both that typed form and existing string callbacks,
uses only `:available` prose in the prompt, and exposes every typed result under
`:prose-read-results`. No downstream decision changes; the distinction is now
available in the fold record.

The domain/range negative summary now says `negative-control PASS (coverage
gap rejected)` and reports `conforming=0 rejected=1`; it no longer juxtaposes
an unexplained `PASS` with `passed=0`.

Canonical invocations:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.enact-read-boundary-test
bb checks/hyper_edge_domain_range_check.clj
bb checks/hyper_edge_domain_range_check.clj --negative
```

The check uses the C16 convention: positive success and rejected mutation both
exit 0, ordinary failure exits 1, and a mutation that slips exits 2.
