# Family theta coherence execution receipt

2026-10-24; author codex-1, reviewer codex-11. Branch family-theta-coherence.
Worktree /home/joe/worktrees/futon2/family-theta-coherence.
Base/test HEAD: 929e3d6a3584da3c0e744953a1d569451aa5517a (tested uncommitted changes identified below).
No merge, reload, click, learning-ledger writes or route activation performed.

## Scope and numeric decision

The family boundary checks the exact Coherent equation per declared occurrence using the counts passed to novelty. Only Clojure integers/ratios are supported; decimals, IEEE values (including finite doubles), nil and strings yield typed refusals. Positive count validation precedes division; arbitrary precision addition avoids integer overflow. Normal half-count and ratio producers and explicitly defaulted records are accepted. There is no epsilon, coercion, ledger reread, parameter replacement or new prior.

Incoherent candidates never enter raw scoring, and their typed findings remain in family failures. Snapshot conflicts still refuse the whole family. Certification checks independently before recomputation. The raw scorer remains unchanged, including its fixed-transition/variable-count arithmetic controls. Neither fresh observation admission nor runtime R20 same-click binding is claimed.

## Evidence hashes and environment

- source family_canonical_g.clj SHA256 b10544720a96da1658076cfa06cfe3e071e457319c519bce6cfb20348de43736
- new test family_theta_coherence_test.clj SHA256 0f32d0dd39695fca1edad355ba365b842a18a686bf8f43b31ce3baaceed82dc7
- deps.edn SHA256 e4d7c26ae92847e13092abc60cd0ecb2d1a0f89213ef9456cfb02c2bf41737e9
- Clojure CLI 1.12.5.1664; OpenJDK 21.0.12.1+1-1-24.04.4-Ubuntu
- Commands run from the worktree, own JVM. Only ledger/pattern-theta is stubbed in the new producer fixture to avoid live ledger reads; arrangement, family scoring, transition and certification are real.

Full logs committed beside this receipt in test/resources/family-theta-coherence/.

## Red, green, mutation

Command: clojure -X:test :nses '[futon2.aif.wm.family-theta-coherence-test]'
Pre-implementation: exit 1, 2 tests/26 assertions, 9 failures/0 errors (coherence-red.log).
Three failures establish the semantic defect: incoherent candidate still scored, no typed family finding, independently recomputed incoherent certificate accepted. Six additionally pin unsupported numeric types.

After implementation and test expansion: exit 0, 4 tests/45 assertions, 0 failures/errors.
After mutation restoration: same command, exit 0, 4/45, 0/0 (coherence-green.log).

Mutation command:
```sh
clojure -Sdeps '{:paths ["src" "test" "resources"]}' -M -e '(require (quote futon2.aif.wm.family-theta-coherence-test)) (binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)] (with-redefs [futon2.aif.wm.family-canonical-g/coherence-finding (constantly nil)] (clojure.test/test-vars [(var futon2.aif.wm.family-theta-coherence-test/same-posterior-family-boundary)])) (prn @clojure.test/*report-counters*) (System/exit (if (pos? (:fail @clojure.test/*report-counters*)) 1 0)))'
```
Exit 1: {:test 1, :pass 11, :fail 3, :error 0}. Only the guard was disabled in that JVM; source stayed fixed. Full log coherence-mutation.log.

Relevant regression commands, each once:
- clojure -X:test :nses '[futon2.aif.wm.family-selection-canonical-g-test]' — exit 0, 7 tests/49 assertions, 0/0 (coherence-family.log).
- clojure -X:test :nses '[futon2.aif.cascade-coapply-g-test]' — exit 0, 15 tests/88 assertions, 0/0 (coherence-raw.log).

## Gates

clj-kondo --lint src/futon2/aif/wm/family_canonical_g.clj test/futon2/aif/wm/family_theta_coherence_test.clj
Exit 0: linting took 14ms, errors: 0, warnings: 0.

emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' src/futon2/aif/wm/family_canonical_g.clj test/futon2/aif/wm/family_theta_coherence_test.clj
Exit 0: OK.

git diff --check — exit 0.

Read scripts/warrant_suite.py: it contacts the live registry and writes central storage/index. Not invoked under this packet's no-live-writes constraint; this committed receipt plus exact logs/hashes is supplied instead, not claimed as a registry warrant.

## Setup failures and command deviation (not semantic evidence)

Initial test fixture had invalid reader literal 3/4M: exit 1 before test execution. Corrected to 0.75M before the recorded semantic red run. An added test had one extra closing paren, caught by kondo (exit 3); fixed before green gates. First mutation command used an invalid escaped var quotation: exit 1 before execution; replaced with (var ...).

I mistakenly invoked clojure -M:test -e to load the new namespace. That alias runs the test runner; it began broader namespace discovery. Interrupted with Ctrl-C, exit 130, not counted as any passing gate. Output included load-time test prints and fold-turn QUARANTINED notices under futon6/data; I cannot attest that those unrelated load-time effects were read-only. No further broad run or attempted cleanup was performed. Worktree git status after interruption showed only the intended source and test changes, no generated tracked/untracked additions. This deviation is disclosed rather than represented as a focused test.
