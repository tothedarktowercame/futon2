# M2l: selected outer target interpretation ask

Implemented on branch codex-11/m2l, based on 53a201cfa, worktree /home/joe/worktrees/futon2/m2l. No click, reload, canonical source edit, or live-store write.

## Premises and change

selected_outer_cascade_policy.clj:168–171 carries the sealed entry's action into [:decision :action] and names the outer route in the certificate. Historical run records instead project it to :decision :chosen. The M2h fixture attaches mission wants before sealing; this test uses that real producer and the recorded target projection (README explains the historical record predates M2h).

full_loop_runner.clj:2115 now admits one refusal-shaped ask input for a selected outer cascade-candidate with available declared-wants and nonempty want. It does not decide which wants are unproduced. The unchanged ask boundary at :6956 limits asks to ordinary clicks (nil :flight); :6988 redecides once only on publication. With no publication judgement0-squares is retained. :7263–7265 retains the ask on the judgment cell. The earlier helper reader at :6855 merely delays terminal refusal where an ask can discharge it; selected decisions already have no terminal selection refusal.

## Execution evidence

Red: JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/home/joe/notes/m2l-tmp clojure -X:test :nses '[futon2.aif.m2l-outer-selection-ask-test]'
M2l-red.log: 3 tests, 14 assertions, 2 failures, 0 errors. The selected fixture had no ask calls and retained :selection-complete-no-ask-required.

Green: same command, M2l-green.log: 3 tests, 14 assertions, 0 failures/errors. Cases: selected outer ask once + retained nonpublication; unavailable declaration no ask; old abstained refusal still asks.

Worktree verification requires test-only canonical-runner-bytes binding to this checkout's real bytes; otherwise the production source drift guard correctly refuses the differing worktree. No change to the production guard. Initial combined run hit this guard. M2l-run-tests.clj reruns only the two named namespaces with that isolated binding.

Existing namespace on changed runner: 17 combined tests / 80 assertions, 23 failures, 0 errors (M2l-green-isolated.log). Unchanged base: clojure -X:test :nses '[futon2.aif.click-interpretation-ask-test]', 14 tests / 66 assertions, identical 23 failure sites, 0 errors (M2l-existing-base.log). Publication fixtures return :rejected rather than :published on both; no new failure sites. This regression namespace is NOT green.

clj-kondo: 0 errors, 2 pre-existing warnings (redundant let, unused on-prod-history-checkpoint), no new warnings. check-parens OK. Logs and isolated runner script copied into evidence/m2l-outer-selection-ask/.

This only requests missing interpretations. It does not join effects, produce token prediction inputs, or heal M2 by itself.
