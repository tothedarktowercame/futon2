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
