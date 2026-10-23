# M2y-4a producer receipt

Worktree `/home/joe/worktrees/futon2/m2y4a`, branch
`m2y4a-retain-effect-revisions`; test HEAD/base
`af54ac62b690bfd37f602c669be99b13ea066de0`. Own Clojure JVM, worktree cwd.

The close adapter retains the packet's sealed receipt bodies verbatim alongside
their digest vector. An optional grounding revision must be a 40-hex string and
have a nonblank string repo. Absent revision is still valid; present nil is not.
The review prompt requests the inspected commit and distinguishes its 40-hex
SHA from the existing 64-hex grounding digest. JSON-decoded keyword keys and
string enum values remain supported: the existing :verified/"verified" status
check is unchanged; revision and repo are strings, not enum keywords. No
inference of a commit from a content digest and no observer implementation.

## Fixtures

`legacy-continuation-close.edn` is the existing live close
`data/wm-cascade-continuation-closes/7a4af8ece478ab9561bf37d2977a8a87598b9607a94107cd74470351e9512fd8.edn`
copied verbatim with a trailing newline added. Live source SHA256:
`f7a2acc9fddfa842c3d05f8a2f8766582d165a36330d97a351254cb7febab5aa`;
fixture SHA256 `c7a64781f4299abc60076fc788d1285c2dca5ffc977a15ba4d635005aaf4b70b`.
The fixture test checks its original content digest, four receipt digests and
absence of receipt bodies. No migration or live data mutation is performed by
the implementation. New effect inputs in tests are explicitly synthetic,
sealed by the real executor; they are not a claimed live observer fixture.

## Commands/results

- `clojure -X:test :nses '[futon2.aif.m2y4a-effect-retention-test]'` before source
  changes: `red.log`, exit 1, 4 tests / 24 assertions / 14 failures / 0 errors.
- `clojure -X:test :nses '[futon2.aif.wm.cascade-continuation-test futon2.aif.m2y4a-effect-retention-test]'`:
  `green.log`, exit 0, 9 tests / 41 assertions / 0 failures / 0 errors.
  A test-source grep for cascade-effect-review-prompt/cascade-effect-author-prompt
  found only the new namespace, which pins revision/repo/40-hex/inspected wording.
- Drop only `:effect-receipts` from the persisted close. Run the explicit new
  namespace: `mutation-retention.log`, exit 1, 4 tests / 24 assertions /
  2 failures / 0 errors (body equality and retained revision).
- Restore retention; bypass only revision-valid? with `(or true ...)`. Same
  namespace: `mutation-revision.log`, exit 1, 4 tests / 24 assertions /
  8 failures / 0 errors (four malformed revisions and four invalid repos).
- Restore the check, same namespace: `restored.log` records the final result.
- `clj-kondo --lint src/futon2/aif/wm/cascade_continuation.clj src/futon2/aif/full_loop_runtime.clj test/futon2/aif/m2y4a_effect_retention_test.clj`:
  `kondo.log`, 0 errors / 0 warnings.
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' src/futon2/aif/wm/cascade_continuation.clj src/futon2/aif/full_loop_runtime.clj test/futon2/aif/m2y4a_effect_retention_test.clj`:
  `parens.log`, OK. `git diff --check`: clean.

Test-command error: an initial retention mutation used `:vars` alone instead
of `:nses`. It selected the requested test but loaded other test namespaces,
emitting top-level realized-G checks and futon6 fold-turn QUARANTINED messages.
The process had already exited 1 (1 test / 4 assertions / 2 expected mutation
failures) before a stop signal could reach it. Its full log is retained as
`aborted-vars-discovery.log` (name reflects the attempted stop, not actual
termination). This is not claimed as an isolated gate or proof of no external
side effects. All subsequent mutation commands explicitly restrict namespaces.
No click or reload was requested or performed by this packet.

Validated source SHA256s:

- cascade_continuation.clj: `b236304a55515dc775701f84d2519445645be038ebc44971e14ed4c73708af0d`
- full_loop_runtime.clj: `d0baa032aa8559c50a452817d153e34cdfb21366db3edb5059cd779b028fcbe8`
- m2y4a_effect_retention_test.clj: `923a00d2056872b688ec15e044c1179460aef696397c1b928046e62e7b0c0f5d`
