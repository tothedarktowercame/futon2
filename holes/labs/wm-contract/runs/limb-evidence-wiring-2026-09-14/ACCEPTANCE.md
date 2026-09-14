# Independent review acceptance — limb-evidence admission wiring

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 916ff630
(runner + controls), caa7e76b (receipts). Verdict: ACCEPTED.

Checked:

- File scope: full_loop_runner.clj + its tests + receipts only.
- All four anchors: attempt-scoped evidence/ listing (bounded to the
  attempt's own directory, filename-sorted); strict UTF-8 decode with
  REPORT actions + strict one-form EDN + limb-evidence/validate-record
  per deposit; evidence entries appended after the six sibling events
  with the agreed id format and per-entry read-time admitted-at;
  absent/empty directory preserves the six-entry behaviour exactly;
  refusals (:evidence-not-single-edn, :evidence-edn-invalid,
  :source-unavailable) propagate before close persistence, with the
  outer runner boundary's typed passthrough extended to
  :limb-evidence/refusal.
- A hardening the packet did not ask for, worth naming: bytes are
  read ONCE at validation and the same captured bytes feed the
  manifest digest (read-bytes consults the capture first), so
  validation and admission cannot diverge on a file mutated between
  the two reads — the time-of-check/time-of-use gap is closed.
- Controls: end-to-end run with the three record kinds deposited
  MID-ATTEMPT through the delivery-qa seam (realistic pre-close
  timing) -> 9-entry manifest, evidence ids after siblings in
  filename order, block ids = manifest ids, per-entry digests
  independently recomputed; three refusal cases (self-decided
  standing decision, two EDN forms, trailing garbage) each with the
  typed code and NO 007 file.
- Receipts: fresh-JVM full runner namespace 157 tests / 824
  assertions exit 0 at 916ff630; runner kondo baseline-delta 0/0;
  full-driver parens; clean first pass.

Note (not a defect): a stray SUBDIRECTORY under evidence/ would
refuse the close as :source-unavailable (readAllBytes on a directory
throws). Fail-closed and acceptable; the prompt-update packet should
tell depositors that evidence/ holds flat .edn files only.

The qualifying-close machinery is now complete in code: a cohort-51
attempt whose author/reviewer deposit limb receipts, a standing
decision, and a revision pair during the attempt will close with
those artifacts admitted pre-cutoff. Remaining: the prompt-update
packet instructing depositors, then a real run, then the second
blinded observation.
