# Offline accepted-job lifecycle reconciliation

The reconciler consumes six independently configured, exact-byte pinned EDN
snapshots: closed controller state, canonical hot ledger, accepted queue,
execution registry, final-delivery records, and deferred resumes. It does not
call the serving API or load the HTTP namespace, so it cannot initialize or
compact production state.

Every accepted job must occur in the hot-ledger census with exact job identity.
Nonterminal states must join their one and only lifecycle source. Terminal jobs
must join a complete delivery record at the same job and trace identity. All
active sources must name the controller's closed generation; waiting-writer
must be zero. Duplicate, omitted, extra, partial, unknown-state, missing-source,
pin-mismatch, trace-conflict and stale-generation inputs refuse.

Every snapshot now requires explicit isolated/production scope and named
producer provenance. IDs are nonempty strings in vectors or maps and duplicate
vectors refuse before set construction. UTF-8 decoding is strict. Deferred
resumes carry the same controller generation and their order, map keys, pending
status, payload map and requested-job-id must agree exactly.

A seventh independently configured completeness-authority snapshot binds the
controller generation, exact digests of all six data sources, and the complete
job/trace universe. Its universe must equal the hot ledger and each trace pin.
Only authority marked independent-fixture can support isolated positives.
Production scope refuses unconditionally until a genuinely independently owned
production completeness authority exists; a candidate boolean, matching count,
borrowed authority file or relabelled fixture cannot qualify.

Deferred resumes are reported separately and never counted as accepted jobs.
A complete census is evidence that this isolated snapshot has zero accepted
work in flight. It always states restart-authorized false: source authority,
loaded identity, first-installation fencing and temporal simultaneity remain
outside this pure join.

The real unavailable sources are an immutable controller-generation snapshot,
an accepted-queue snapshot, execution-registry snapshot, and final-delivery
snapshot taken atomically with the hot ledger. The current canonical hot file
alone does not contain that simultaneous cross-source cut, and reading it does
not prove which controller code is loaded. Therefore no real positive census
is claimed in this packet.

One read-only exact-byte observation of the canonical hot file found 8,307 jobs:
6,862 done, 972 failed, 348 cancelled, 108 delivered, 14 deduped and 3 running.
6,529 carried trace ids and none carried a request commission, consistent with
the known pre-retention serving code. The same observation pinned the durable
turn queue and roster files, but those files are not an atomic controller,
execution, and final-delivery cut. Exact measurements are retained in
real-source-discovery.out; their changing live nature makes them discovery,
not resolver inputs for a positive witness.
