# improve-2a: record-only preference audit

Branch: `fix/narrative-improve-2a`, based on `04e34c8b`.
Implementation: `5d3124021561b3a4ac57c27ec1eecf12baa6e6e5`.

The serving selector attaches `:wm/preference-audit-v1` to the selection certificate after selection. The scoring-input validator replays it when present; older receipts without it retain their existing validation. The new namespace is registered with load identity and listed in required sources. No scorer, G, E, habit or preference law changed.

The receipt identifies the target-qualified token powerset, the global deduplicated source inventory (unit **source entry**, not outcome), projected source/outcome counts, normalized budget before projection, projected/consumed utility totals, every fallback token and its lam/want-count derivation, schedule, and full powerset normalizers at every step. The pre-projection budget is explicitly **derived from the retained normalization law**. Raw source-weight totals were not retained and are marked held; 468 is never represented as the raw-weight divisor. Fallback derivation is held when its required inputs are absent or the retained preference has nonzero mu; it does not invent a decomposition.

Selected-target odds retain token locators and mean present versus absent with other tokens fixed. Missing target/locator/domain evidence is held. Ruled-zero exclusions also hold that odds comparison because an explicit other-token context is needed; the full normalizer still accounts for the exclusions. The narrative adds one sentence naming the source-budget domain and each wanted token's odds (latest updater: **1.001307 : 1**).

## Frozen replay

| Run suffix | Retained live utility | All consumed utility | G spread | Fallback wants |
|---|---:|---:|---:|---:|
| 1789964661 | 78/19483 | 19951/116898 | 0.00130598185312556 | 1 |
| 1789952479 | 3143/58449 | 87367/779320 | 0.003130934660986501 | 7 |

Both inventory counts are 468. Latest F2 fallback is exactly 1/6. All 27 candidates replay through the actual horizon model within 1e-8; complete normalizers agree with the model within 1e-12. Candidate-score and complete selection-law `pr-str` bytes are identical before/after attachment; removing the audit returns the original decision exactly. EDN roundtrip validates. Removing fallback rows, replacing source count 468 by 5, or corrupting log Z is rejected by the scoring-input validator with `:preference-audit-mismatch`.

Historical tick records omit the top-level selected action. The test supplies the recorded per-policy winner for these two fixtures only. A separate read-only comparison with each D-task `[:dispatch :occurrence :action/value]` returned `true` for exact action equality (actions a5d2326d-a73a-447c-8bc7-7f77c8bbc187 and 357124fc-c58f-4a84-984a-6dc277cdf39b). Production never infers selection from argmax: missing selection binding yields held odds.

## Validation, 2026-09-21

Own process/worktree, Ubuntu Java 21.0.11, Clojure CLI 1.12.5.1664. No serving-JVM evaluation, live clicks, or rewriting frozen records. Runner tests use their hermetic fixtures.

Baseline: copied the new acceptance namespace into a detached worktree at `04e34c8b` and ran `clojure -M:test -m cognitect.test-runner -n futon2.aif.preference-audit-test`. Exit 1:

> Could not locate futon2/aif/preference_audit__init.class, futon2/aif/preference_audit.clj or futon2/aif/preference_audit.cljc on classpath.

This is an absent-namespace baseline failure; no assertion evaluated on main.

For each namespace below, ran `clojure -M:test -m cognitect.test-runner -n <namespace>` in the implementation worktree:

| Namespace | Tests | Assertions | Failures/errors |
|---|---:|---:|---:|
| futon2.aif.preference-audit-test | 4 | 79 | 0/0 |
| futon2.aif.scoring-input-receipts-test | 3 | 44 | 0/0 |
| futon2.aif.token-observation-initialization-test | 6 | 50 | 0/0 |
| futon2.aif.run-narrative-test | 21 | 110 | 0/0 |
| futon2.aif.load-identity-test | 3 | 12 | 0/0 |
| futon2.report.cascade-decision-test | 13 | 94 | 0/0 |
| futon2.aif.full-loop-runner-test | 185 | 1063 | 0/0 |
| Total | 235 | 1552 | 0/0 |

The existing grounded two-tick fixture now asserts the audit is recorded and replay-valid in the retained selection checkpoint; its grounded-change and `closed-execution` assertions also passed.

`clj-kondo --lint` on all seven changed Clojure files: 0 errors, 0 warnings (one existing redundant-str info in war_machine). `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- <the seven changed files>`: all OK. `git diff --check`: clean.

Scoped registry warrant: `test-registry-7b0851e48c021a93785ea37cdfa9a8643c08af9d2f90a6ffb3c844643726557d`. Registered with `clojure -M -m futon3c.test-registry run /tmp/improve-2a-registry.edn`, then `check /tmp/improve-2a-registry-check.edn`: `:warrant? true`, `:outside-closure []`. It pins the five changed production files and the new acceptance test, rerun **4 tests / 79 assertions / 0 failures / 0 errors** at implementation commit. The runner-suite result above is separately reported, not claimed as covered by this scoped warrant.
