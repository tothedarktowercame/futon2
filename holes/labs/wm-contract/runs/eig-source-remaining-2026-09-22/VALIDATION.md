# EIG remaining-task admission

The active mission has two open tasks without an activation gate: the typed Q(o|pi) consumer boundary and the default-off EIG shadow/calibration packet. The later decision to replace the live proxy remains Joe's. Neither reading claims that missing model or held-out data already exists.

The declaration retains the updater pattern, receipt and candidate. It adds singleton routes through `contracts/every-entry-has-a-falsifier` (typed-Q payloads and executable falsifiers) and `aif/two-layer-calibration` (separate internal shadow checks and held-out outcome calibration). Both require the observed completed updater and forbid their own already-completed task. Both retain the family context, schedule, beta and scales. Receipts pin the actual library bytes and state applicability and limits.

The new fixture is the mission at futon2 f4b390324774ee308efb920ff00357f4a9a0c644, SHA256 3bdc0ef28322d178ce5f55df118418fd992326c5464fc17e78d0c10cb0e9bdaa. Git observation IO uses frozen historical/current bytes; the actual C4 matcher, pinned-pattern admission, assembly, scoring and selection run in the test. No live mission file, serving JVM, WM click or production store is used. The outcome test now freezes its original updater-only declaration rather than accidentally combining a historical mission with a changing live declaration.

## Evidence

Before extending the declaration, the new namespace failed: 3 tests, 16 assertions, 8 failures, 0 errors. In `remaining-work-reaches-real-joint-selection`, `(is (= 2 (count actions)))` failed with actual `(not (= 2 0))`. No two new first actions were admitted.

After extension:

- `clojure -M:test -m cognitect.test-runner -n futon2.report.eig-source-remaining-test`: 3 tests, 20 assertions, 0 failures/errors.
- `clojure -M:test -m cognitect.test-runner -n futon2.aif.token-outcome-test`: 3 tests, 17 assertions, 0 failures/errors.
- `clojure -M:test -m cognitect.test-runner -n futon2.aif.cascade-sources-test`: 8 tests, 33 assertions, 0 failures/errors.
- `clojure -M:test -m cognitect.test-runner -n futon2.aif.interpretation-construction-test`: 8 tests, 55 assertions, 0 failures/errors.

The new namespace checks both first actions survive joint selection, each also survives independently at T=2, the completed updater is absent from admitted outputs, and mutating either pattern SHA refuses with `:interpretation-source-hash-mismatch` before selection.

`clj-kondo --lint resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn test/futon2/report/eig_source_remaining_test.clj test/futon2/aif/token_outcome_test.clj`: 0 errors, 0 warnings.

`emacs --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --files resources/wm/cascade-sources/M-aif-policy-conditioned-eig.edn test/futon2/report/eig_source_remaining_test.clj test/futon2/aif/token_outcome_test.clj`: OK.

`git diff --check`: clean. All commands ran from `/home/joe/code/futon2-eig-source-remaining` in separate CLI processes. The scoped registry run is recorded separately after committing its source scope.

## Scoped warrant

Evidence ID: `test-registry-feb34f71eed9922f01c2e527dd241292c6532dfff9d0c0854e501a60fa3f096d`. Registered at source HEAD `d4f5c9fd8c18d857f3fd2ffb98d8bf6a8492d4ab`: 3 tests / 20 assertions, 0 failures/errors, matched postcheck, `:warrant? true`. The subsequent check also returned `:warrant? true`, `:outside-closure []`. Full results, execution log and loaded-file closure are in `registered/`.

Commands (own CLI JVM, working directory `/home/joe/code/futon3c`):

```
clojure -M -m futon3c.test-registry run /home/joe/code/futon2-eig-source-remaining/holes/labs/wm-contract/runs/eig-source-remaining-2026-09-22/registry.edn
clojure -M -m futon3c.test-registry check /home/joe/code/futon2-eig-source-remaining/holes/labs/wm-contract/runs/eig-source-remaining-2026-09-22/check.edn
```

The registry requires its logical `clojure -M:test -n <namespace>` command and instruments that command with its own runner; the initial explicit `-m cognitect.test-runner` form was refused before execution. The committed config uses the supported logical form. This warrant covers the new admission namespace and declared/loaded scope, not the separately executed historical outcome test. Pattern bytes were checked by the real admission loader during the run; external library files are not claimed as part of the registry's repo-relative scope. Registry evidence append is the only external write, explicitly requested for this packet. No WM production store was changed.
