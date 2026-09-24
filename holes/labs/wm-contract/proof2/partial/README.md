# Partial work preserved from failed jobs

- `B-C-kimi-6-ledger.patch` and `B-C-kimi-6-b_update_carrier_test.clj`: kimi-6's
  uncommitted B-C work (job invoke-1790226090126) when Kimi's 5-hour quota
  ended at 05:12 on 2026-09-24. Built against B-D's original commit point
  (accepted close), which codex-20 rejected in 15ecfb2d; it lacks per-row
  acceptance status and `:trial-vectors`. Saved here so the re-dispatch can
  start from it; the shared checkout was restored to HEAD (claude-8).
- `walkthrough-02-kimi-4/`: kimi-4's uncommitted draft of walkthrough 02 (selection
  law), its generator and three figures, when the Kimi quota ended at 05:13 on
  2026-09-24 (job invoke-1790226320791). Moved out of `walkthroughs/` so the
  shared tree is clean; the re-dispatch starts from it (claude-8).
- `B-C-codex-2-ledger.patch`, `OBS-P-amend-codex-1.patch`,
  `runner-codex-1-codex-2-mixed.patch`: uncommitted work when Codex's usage
  limit ended every seat at ~05:20 on 2026-09-24 (reset 2026-09-26 13:52).
  codex-2 was on B-C (job invoke-1790226888121) from the kimi-6 patch above;
  codex-1 had committed OBS-D revision 2 (508a410e) and was amending the pair
  builder (job invoke-1790226889574). The runner diff mixes both lanes' hunks.
  Shared tree restored to HEAD (claude-8).
