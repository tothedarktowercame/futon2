# M2d execution receipt

The implementation shares read-only injury selection across diagnosis, carried bindings and route. A recorded other-injury cause defers the old transaction and prepares that injury. Eligible deferred transactions restore in injury-id order. Moves check the selected source byte SHA and refuse changed sources or occupied destinations. Retry-budget and coordinator code are unchanged.

Live ledger and transaction fixtures are verbatim, pinned in the new test namespace. The before run caught the wrong diagnosis and work order (04e14314 rather than cfbcbad0). The final positive runs real route!, prepare and coordinator through claim, with an author port that records the work order and throws to stop further effects. Restoration/negative route tests stop at a stub coordinator boundary. Test 7b calls actual readiness in an isolated process and asserts unchanged file positions/SHA; it does not claim to run the full runtime.

Final focused result: 7 tests / 36 assertions, no failures or errors. Lint 0/0; check-parens OK. The initial implementation's focused run passed 7/35; test 1 was strengthened to exercise the actual coordinator/author port, then the final focused run passed 7/36 (an intermediate test edit had an EOF syntax error, fixed before this run).

Requested existing namespaces: 99 tests / 756 assertions initially produced four failures. The new context-path mismatch was fixed and its original test replayed successfully (1/8). The remaining three failures reproduce on unchanged base source: old diagnosis test expects the now-bounded request to include :injury-record; harness expects claimed rather than reopened after author failure; harness subprocess checkpoint verification exits 1. They are retained as baseline failures, not claimed green. No changes were made to those tests or their contracts.

Amendment 2: restoration in a clean tracked fixture passes actual adapter readiness. The same fixture with tracked dirt returns :refused with [:tracked-worktree-dirty], leaving deferred bytes unchanged. No futon3c edit. prepare-acceptance is unchanged: it consumes the selected diagnosis observation as before.

Dirty deferral question: YES, under the existing checks. meta_self_heal_adapters.clj readiness lines 62–87 recognizes the old transaction's author receipt at the normal path and permits its tracked dirt. The new route's defer branch moves that transaction, then calls coordinator/prepare for the blocking injury and enters coordination without rechecking git cleanliness. This is code-path evidence, not a live dirty-repair execution.

No reload, click, merge or live-store write. After tests, the live ledger and transaction SHAs still equal the pinned snapshots (72f37625… and 6f271b09…). The branch is for review; three baseline regression failures remain explicitly recorded.
