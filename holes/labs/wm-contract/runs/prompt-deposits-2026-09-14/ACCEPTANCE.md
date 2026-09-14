# Independent review acceptance — limb-evidence deposit prompts

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 9b2d905f
(prompt construction + controls), 395edfc2 (receipts).
Verdict: ACCEPTED.

Checked:

- File scope: full_loop_runner.clj prompt builders + tests + receipts
  only; no close/admission/manifest logic touched (diff read in full).
- All four builders (author, reviewer, revision-author,
  revision-reviewer) carry the deposit block via one shared
  evidence-deposit-instruction fn; the attempt evidence directory is
  computed once under the cohort? guard with the exact path the
  admission slice reads, and threaded via prompt-opts to all four
  dispatch sites (initial + revision rounds — the revision round was
  easy to miss and is covered).
- Role split enforced and TESTED: receipts + revision pair named to
  authors only; the standing decision to reviewers only, with
  :decided-by/:implementation-author assignment spelled out and the
  self-decided refusal warned; flat-files-only and
  invalid-deposit-refuses warnings in all four prompts; absent
  evidence-dir (non-cohort) omits the block entirely.
- Receipts: fresh-JVM full runner namespace 158 tests / 848
  assertions exit 0 at 9b2d905f; kondo baseline-delta 0/0;
  full-driver parens; clean first pass.

The qualifying-close machinery is COMPLETE: contract records
(limb_evidence, + reviewer order fix), close admission (916ff630,
TOCTOU-closed), and now the instructions that put deposits in front
of the seats. Next: serving-JVM reload, one cohort-51 run with
deposits, second blinded observation.
