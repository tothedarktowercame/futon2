# Durable archive scope

The hot invoke-job ledger still applies D13 unchanged: transcript detail is
trimmed after 24 hours and unreferenced terminal jobs expire after seven days.
Immediately when a job crosses that deletion boundary,
`compact-invoke-jobs-ledger` moves only its immutable R9 projection into the
top-level `:request-commission-archive`: exact normalized commission and
request digest, agent/caller/surface, artifact and trace ids, timestamps,
terminal state/code, execution summary, delivery, and invocation model. The
archive record has its own digest and refuses conflicting reuse of a job id.

Archive entries have no clock expiry. They remain until a future explicitly
reviewed archive-retention operation removes them; this packet defines no such
operation. This preserves the small evidence needed to replay R9 joins without
retaining full transcripts or protecting hot jobs indefinitely. The read API
validates both the archive-record digest and the original request digest.

The test uses three jobs more than seven days old. The requested job is not the
newest sentinel, disappears from `:jobs`, survives a disk write and in-memory
reset, and reads back from `:commission-archive` with its trace and artifact
join fields. Deleting its archive refuses `:invoke-job-missing`; mutating the
archive refuses `:request-commission-archive-tampered`.

This projection does not authenticate trace-to-job indices or an external
operator root. The later genesis packet must join the archived `:job-id` and
`:job-join :trace-id` to its independently retained trace authority and must
verify the external operator-root plus delegated canonical-branch acceptance
required by lead policy `9f37f503`. No anchor was fabricated here.
