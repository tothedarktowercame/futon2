# Independent review acceptance — semantic retention

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 723fa30e
(mechanism + runner + prompts + controls), 8f053434/2f68d3e4 (test
pinning), 26934674 (receipts). Verdict: ACCEPTED.

Checked:

- All three gaps closed as anchored: (1) receipts gain OPTIONAL
  :stdout-file/:stderr-file flat companion names (path separators and
  dot-names refuse :output-file-invalid); the pure record validation
  stays IO-free while validate-limb-receipt-outputs verifies the
  companion bytes' digests via injected reads, refusing
  :output-digest-mismatch; (2) the standing decision REQUIRES a
  review-grade :explanation (>= 80 chars, refusing
  :explanation-invalid, checked before and after the exact-map so a
  missing key reads as the explanation refusal); (3) the
  direct-approve build cell now retains :review-text, pinned by test.
- Runner admission hardened beyond the packet: every evidence/ file
  must now be either a valid limb record or a receipt-NAMED companion
  — a stray file refuses (its parse error, or :schema-mismatch),
  closing the loophole where arbitrary files rode into the manifest
  as unclassified evidence. Companion bytes are the captured bytes
  (single read; digest and verification cannot diverge). A named but
  absent companion refuses :output-file-invalid.
- Existing-record note: the two historical exercise closes are not
  re-validated (records, not schemas in flight); optionality of
  companions preserves receipts without them.
- Controls: companion happy/mismatch/nested-name; missing and short
  explanations; prior suites green. Receipts: fresh-JVM 165 tests /
  880 assertions exit 0; runner kondo baseline-delta 0/0; full-driver
  parens.

Next: serving-JVM reload; cohort-51 attempt-002 (the cohort's last)
runs exercise 3 with full semantic retention; observation #3.
