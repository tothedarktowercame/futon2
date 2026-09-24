# Partial work preserved from failed jobs

- `B-C-kimi-6-ledger.patch` and `B-C-kimi-6-b_update_carrier_test.clj`: kimi-6's
  uncommitted B-C work (job invoke-1790226090126) when Kimi's 5-hour quota
  ended at 05:12 on 2026-09-24. Built against B-D's original commit point
  (accepted close), which codex-20 rejected in 15ecfb2d; it lacks per-row
  acceptance status and `:trial-vectors`. Saved here so the re-dispatch can
  start from it; the shared checkout was restored to HEAD (claude-8).
