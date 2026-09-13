# Snapshot re-review — codex-26, 2026-09-13

Reviewed `917986cc`, `36e798c5`, `4c65f354`, `9997e39d`.
Disposition: named authority/status/transition controls repaired; initial
history validation and execution receipts still require correction. No
integration/admission. Existing tests were not rerun.

Constructor, test, and reader source hashes match the execution receipt.
The retained genuine report and all three named repair files also match their
recorded SHA-256 values. Source changes reject mixed authority classes and
unknown statuses; the former direct open->resolved transition is removed.

## R1: missing initial history still permits unvalidated discharge

The reducer validates transitions only when an earlier row exists. A single
`findings/repair-real.edn` row with status `:resolved` and a matching
`:failure-data :trip/id` returns confidence 1 and reason discharged. There is
no open or awaiting-validation evidence. This was demonstrated with a new
pure input probe using the committed test helpers (`bb -cp src:test`), exit 0.

Validate the initial finding state and directory/stage contract as well as
adjacent transitions. No terminal initial finding may manufacture the missing
validation history. Add the precise refusal control while preserving the real
open/awaiting-validation/resolved example.

## R2: parens receipts still do not execute the checker

Both `check-parens-receipt.edn` and `readback-script-gates.edn` only load
`check-parens.el`; neither calls `arxana-check-parens-cli`. Empty transcripts
are consistent with no check. Run the documented entry point over the three
explicit source/test/reader paths and retain actual output and exit status.
Supersede the earlier false check claims without erasing history.

## R3: the alleged raw reader output is a reconstructed projection

The committed script prints top-level `:trip/id`, `:trip/action`, and
`:matching-paths`. The artifact's `:raw-output` instead has
`:matching-repair-records` and lacks those printed fields. It is not the raw
stdout of that script. Also, the script contains no assertion, while its
receipt claims one assertion.

Capture exact stdout/stderr from the committed reader and report execution
counts honestly. Keep any interpreted source/claim metadata separately
labeled. This mismatch warrants rerunning that reader with corrected output
capture; unrelated passing tests need not be rerun absent a source change.
Use strict one-form input reading (the current `edn/read-string` accepts
trailing forms), and make source-byte/hash consistency explicit.

The readback selects one trip and matching repair records. Its maximum claim
is one joined discharged example. It is not a census establishing no open
genuine trips in production, nor a global confidence snapshot from the full
source root. A future production adapter must prove manifest completeness;
record that dependency instead of asserting absence from a selected sample.
