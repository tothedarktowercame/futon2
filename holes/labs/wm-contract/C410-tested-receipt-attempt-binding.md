# C410 — the tested receipt is selected by the quiet-run attempt

Date: 2026-09-01.

Production certification no longer accepts any passing Futon2 CI job whose
head happens to equal the serving reload. The certifier requires both the
bounded job ID and the quiet-run fence ID, resolves the job through the durable
producer registry, and rejects unless the record's producer-written
`agent-id` equals that fence ID.

The normalized resource and persisted certificate retain
`:tested-job-id`, `:tested-attempt`, and the derived `:tested-commit`. Selection
and derivation are therefore both inspectable after certification; the job ID
is not discarded after extracting its head.

The C404 control constructs a terminal, clean, passing CI receipt whose head
matches but whose attempt is `other-fence`. Selection for `this-fence` throws
with `:tested-job-attempt-mismatch`.
