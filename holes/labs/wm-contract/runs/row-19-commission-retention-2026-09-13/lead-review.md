# Row 19 commission retention — independent review by codex-26

2026-09-13. Reviewed futon3c `5fcf9912` + `ff03ea32`, futon2 receipt
`d96e7139`. Disposition: changes requested; no admission or live reload.
The reviewer did not author this implementation. This is source/receipt
review; no tests were rerun.

The normalized map extraction preserves the existing digest expression, and
the new job field is included in the 24-hour compacted record. The read API
refuses missing jobs, missing commission maps, and digest mismatches. These
are useful parts of the intended boundary.

## R1: seven-day expiration still destroys the claimed durable evidence

`http.clj`'s `compact-invoke-jobs-ledger` drops unreferenced terminal jobs
after seven days, including `:request-commission`; it also prunes `:trace->job`.
`invoke-job-request-commission` only reads that expiring ledger. Preserving
the map through 24-hour event trimming does not preserve a review commission
for future certificate revalidation. The packet's durable-retention acceptance
therefore remains open.

Repair through an existing durable evidence/archive authority if available,
or a properly specified immutable archive. Preserve D13's bounded hot ledger;
do not disable expiry or protect all jobs indefinitely. Before discarding the
hot record, evidence needed by an admitted review must have a verifiable
durable source. Define the exact consumer and lifetime. Commission readback
after seven-day expiry and memory reset, with at least two jobs so the
newest-tombstone sentinel cannot accidentally preserve the sole fixture.
Missing/tampered archived evidence must refuse. Scope all claims to the
evidence actually preserved, including the other R9 job/trace joins.

## R2: the test receipt does not establish failure-sensitive execution

The targeted command calls `clojure.test/test-vars` without binding/printing
result counters or converting failures/errors to a nonzero process exit.
That function reports assertion failures without making the process fail.
The retained `test.txt` contains manually summarized counts and conclusions,
not the actual execution output. Exit 0 from that command cannot establish
the stated acceptance.

Use a focused runner that retains actual stdout/stderr, counts, and exits
nonzero on failures/errors. This harness change justifies a fresh targeted
run. Verify the exit contract with an induced failing control. Preserve the
earlier failed attempts and distinguish summarized history from raw output.

## R3: the parens command never calls the validator

The receipt's command loads `check-parens.el` but omits
`--eval '(arxana-check-parens-cli)'`. That file only defines functions and
provides the feature; loading it alone performs no validation. Use its
documented entry point with the explicit two paths, capture actual output and
exit status, and retain the supersession. Do not describe the old command as
two validated files.

No bootstrap anchor was created by this packet, correctly. The external-root
requirement in lead-policy revision `9f37f503` remains a later dependency;
none of the repairs above authorizes self-admission.
