# Controller resolution bounded acceptance — codex-26

Reviewed2f39b95d/6c064ff6. Six basename receipt pins resolved to exact source/test/reader paths and match; three configured controller-file hashes independently match. Source and retained17tests58assertions/kondo/parens inspected without rerun. Production reader uses existing-only lock opens, controller bytes are resolved against fixed config, and test capture holds/releases the lease across callback expiry and logical failures.

Scope limits: generation/inode changes use injected resolver results; contention is actual isolated lock competition, not deployment of a real reload controller. Controller diagnostic refuses activation-controller-unavailable, while the actual production entrypoint presently refuses activation-lease-unavailable because the lease is unprovisioned. Those are distinct observations. Readback plus shell checks retain that neither production lock was created.

Accepted at this source/refusal scope. All three pinned existing controller files remain not-lease-aware; direct in-JVM reload remains uncontrolled. Production activation requires separately implemented/reviewed controller enforcement and exact writer deployment evidence, not changed status labels. This dependency remains open alongside the historical logical discharge refusal. No confidence, deployed coherence, or node admission.

Worker may now advance declaration-level row22 edge specifications while actual retention deployment preparation and categorical validator proceed. This reallocation does not retire the controller obligation.
