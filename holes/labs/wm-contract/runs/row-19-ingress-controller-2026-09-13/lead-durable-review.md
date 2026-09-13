# Independent durable recovery review

Reviewed full source/tests at 30b7de55 and receipts 13f64ac1. Both pinned files match current bytes and the source commit exactly (lead-durable-source-pins.sha256). Retained raw focused output shows 7 tests/25 assertions, zero failures/errors; induced failure reports one failure and exit 1; kondo zero warnings/errors and actual explicit parens OK. Passing tests were not rerun.

The single-captured-state verification response and explicit restart-authorized=false repair hold. Required store outside test-only mode, projection separation from entrant tokens, and simple restart/ack recovery hold for the tested payloads and sequential single-owner usage.

Two isolated executed counterexamples block broader recovery acceptance:

1. defer-resume! accepts a payload containing a Java Object, persists pr-str #object text, and returns the ID. A fresh controller then refuses deferred-edn-invalid, so even an earlier valid pending resume cannot be recovered. Validate supported payload schema and strict EDN round-trip before publication, preserving previous disk and memory bytes on refusal.
2. Two controllers can open the same store, each holding a distinct in-memory lock and stale projection. After the first durably accepts A, the second durably accepts B by replacing the file without A. Recovery contains only B. Enforce store ownership for the controller lifetime (with explicit close/release) or a correctly synchronized versioned transaction protocol; a label or documented intention is insufficient. Initialization must also use atomic no-overwrite under that ownership, not exists-then-replace.

Command: `clojure -M /home/joe/code/futon2/holes/labs/wm-contract/runs/row-19-ingress-controller-2026-09-13/lead-recovery-counterexamples.clj` from futon3c. Exit 0 with asserted counterexamples; exact stdout and empty stderr retained. New script kondo has zero errors/warnings and actual explicit-path parens returns OK. Only fresh temporary stores were touched.

Next bounded packet: serialization/ownership/atomic initialization repairs and meaningful same-JVM/cross-process exclusion, release/recovery and unsupported-payload controls. Keep HTTP wiring, lifecycle reconciliation, local control listener and first-installation fence open. No serving mutation, deployment, restart readiness or admission.
