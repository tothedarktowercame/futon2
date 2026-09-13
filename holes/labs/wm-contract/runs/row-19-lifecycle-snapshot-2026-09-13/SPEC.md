# Atomic lifecycle snapshot producer boundary

The new producer owns one lock, generation and exact six-provider registry.
Every participating writer must mutate through that boundary. Capture holds the
same lock, reads every provider, checks provider revisions before and after,
requires exact schema/generation/scope/owner provenance, and emits the exact
bytes and digests consumed by reconciliation.

Its job/trace universe is only a coverage subject. The producer deliberately
sets completeness-authority absent. An external independently configured
resolver must accept the exact generation, six digests and universe; the
producer cannot approve its own census.

This mechanism is isolated-fixture-only. Current hot-ledger, turn-queue,
execution, delivery and deferred writers do not participate in a common lock,
and no production adapter is claimed. Eventual integration must route each
writer mutation through one boundary, derive the closed ingress controller
record from the installed controller, and publish the external acceptance
separately. Until then production construction refuses and no atomic real
snapshot exists. Every derived result remains restart-authorized false.
