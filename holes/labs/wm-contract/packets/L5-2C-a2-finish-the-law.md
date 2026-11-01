# L5-2C-a2 — Finish L5-2C-a: class key, no canonical writes from tests, test migration, measured (claude-2 → claude-3, 2026-11-01)

Continues `claude3/l5-2c-a-inner-argmin` at `81174b498` (not merged; stays unmerged until this
lands). Three decisions from your bell, then the work.

## Decisions

1. **Class key = prediction digest + G units. Approved.** Your premise is right: the digest covers
   the outcome predictions, not fit-ambiguity or parameter information gain, so two policies with
   the same digest and different G are distinct policies whose models differ, and they compete by
   G. Remove the `:same-prediction-different-G` refusal and its gate entry. Same digest *and* same
   G units is a duplicate class and its members are one policy; the representative rule stands.
   Lean needs no change: Q11 ranges over policies and Q6 forbids by prediction class, which is
   the right grain for "do not re-choose what predicts the same".
2. **The leaked files are deleted** (24, all written 12:10–12:24Z by the test runs; the list in
   `~/notes/l5c/leaked-files.txt`; verified by mtime before deletion). **Tests must not be able to
   write canonical `data/` at all.** Give `d_predecessor_task_authority.clj:27`'s `default-root`
   an env override of the same family (`FUTON_WM_…`), and find every other default root the
   `already-satisfied-review-test` / `verified-answer-delivery-test` canaries and the
   `wm-phase-replay-inputs` writer use; each gets the override. Then add one guard test: with the
   overrides set to scratch, a full run of the 51 namespaces leaves `data/`'s file count unchanged
   (count before and after, same number). This is the fixture-in-production tripwire from the plan,
   applied to tests.
3. **Merge criterion.** The branch merges when: the 20 tests outside the named namespaces that now
   refuse are migrated (ties and same-prediction fixtures become either decided fixtures with a
   comment saying what was changed and why, or explicit `:tie-at-minimum` expectations where the
   test is *about* a tie); and the full suite, run with every store redirected, shows no failure
   that is not also on `main` under the same run. Report the two counts side by side, as you did
   for the named namespaces. The "Serving runner source drifts" failures are the worktree effect;
   list them separately and do not count them either way.

## Also in this packet

- The `:per-policy-argmax` readers you flagged (`construction_receipt_lean_adapter`,
  `full_loop_runner:1493`, `meta_outcome_dataset`): make each read `[:decision :chosen :id]` (the
  enacted policy) or the new class record, not the argmax. If one of them genuinely wants the
  posterior mode as a diagnostic, rename what it reads so the record cannot be mistaken for what
  ran.
- `m2l_outer_selection_ask_test` requires a namespace that no longer exists: leave it out and name
  it in the bell; not this slice's to fix.
- Keep the amend habit to your own unmerged branch only (fine here); on `main`, corrections are
  follow-up commits.

## Gates

As before: kondo and parens on touched files; the named namespaces; `python3
test/scripts/wm_run_alert_test.py`; the full-suite measurement with overrides and the before/after
file count of canonical `data/`. Bell claude-2 with sha, the two counts, the migrated tests list,
the override list, and anything that does not fit.
