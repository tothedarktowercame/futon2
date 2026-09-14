# Acceptance — close-failure containment (10090021..578ee155, receipt ebdb7997)

Reviewer: claude-15, per the coding-handoff protocol. Author: codex-24
(job invoke-1789421829469-20865-d22954a6).

VERDICT: **ACCEPTED**.

What I checked (diffs dece1589..578ee155 read via git; receipts validated,
not re-run):

- Containment sits at TWO boundaries: `close!` now wraps `close-core!` in a
  try/catch that writes a durable typed 007 (via `term` + the cohort
  writer), and the outer run catch, when `@closing?`, produces the same
  typed close instead of rethrowing. The receipt's retained failing run
  (`tests.out`) documents WHY both exist: a close invoked from inside an
  existing catch cannot be caught by that same catch — the outer boundary
  is the one that fires for failures raised by `close-core!` invoked from
  catch blocks.
- Typed refusals carry through: :limb-evidence/refusal /
  :evidence-manifest/refusal / :close-retention/refusal / :failure-kind, in
  that order, with :close-exception as the arbitrary-Throwable fallback;
  the exception class and ex-data are retained in the 007 payload. The
  payload keeps the cell contract's judgment+ground shape
  (:ground {:kind :full-loop-close-failure}) and fabricates NO retention
  block.
- A repair obligation is minted at containment via
  `record-system-failure!`; its `write-new-or-identical!` on
  obligation-id(attempt-id, failure-kind) keeps one-failure-one-finding in
  the ordinary path. If the containment writer itself fails differently, a
  second, differently-typed finding is possible — recorded as a note: that
  IS a distinct failure (the containment failed), not a duplicate.
- Orphaning assertions flipped exactly as required:
  manifest-unreadable, digest-mismatch, and the invalid-evidence doseq now
  assert 007 exists with the SAME refusal kind they previously caught
  in-flight; new `arbitrary-close-throwable-produces-typed-close` pins the
  IllegalStateException → :close-exception + exception-class path.
  `delivered-commit-cannot-close-without-field-desk-qa` now expects a typed
  :delivery-qa-gate-failed close — an in-scope contract change (the QA
  throw previously escaped the close).
- No backfill: zero changes under data/; cohort-53 attempt-001 remains the
  retained orphan counterexample, cited in both containment docstrings.
- Receipt: honest multi-round trail (5 commits; boundary + three final gate
  runs, failures preserved). Final: kondo 0/0, parens PASS, fresh-JVM
  `:nses` runner suite 167 tests / 906 assertions, 0/0, exit 0.

Non-blocking note: the two containment sites duplicate ~50 lines of
finding/sorry construction; a shared helper would be a mechanical cleanup
for a future small packet — not worth a round-trip now.
