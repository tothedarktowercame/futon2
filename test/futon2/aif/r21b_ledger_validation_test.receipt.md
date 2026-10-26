# R21b durable-ledger validation execution receipt

- Source commit tested: `61601b216f722655ff466c06a74248c6fa054443`
- Worktree: `/home/joe/worktrees/futon2/r21b-ledger-validation`
- Bounded job: `bounded-1793010757742-r21b-ledger-validation-final`
- Command: `clojure -X:test :nses '[futon2.aif.meta-self-heal-production-test futon2.aif.meta-injury-ledger-test futon2.aif.meta-injury-store-test futon2.aif.meta-medic-store-test]'`
- Output: `/tmp/futon-bounded-tests/bounded-1793010757742-r21b-ledger-validation-final.log`
- Machine receipt: `/tmp/futon-bounded-tests/bounded-1793010757742-r21b-ledger-validation-final.receipt.json`
- Repository basis: clean and stable at start and finish.
- Resource result: clean; peak 57 pids, no native-thread markers, and
  `pids.events:max` delta 0.
- Test result: **failed**, 86 tests, 694 assertions, 14 failures, 0 errors.
  Thirteen failures reproduce in the unchanged production-test namespace on
  canonical `main`; the remaining failure is the unchanged
  `legacy-terminal-observation-route-remains-compatible` assertion in
  `meta_injury_store_test.clj`. The new medic-ledger verifier-count, tamper,
  and projection assertions pass in the focused readiness test: 1 test, 11
  assertions, 0 failures, 0 errors.
- Static gates: clj-kondo 0 errors / 0 warnings; check-parens `OK` on all three
  changed Clojure files.
- Real-ledger timing, same 45-entry source pin `4afd33d7…` and seven-scope
  authority: canonical pre-change `diagnosis-context` 48,097.8 ms; rebased
  worktree 27,136.0 ms. Both returned a loaded ledger, computed projection,
  and one active injury.

This receipt does not claim that the required four-namespace gate is green.
It records the stable, resource-clean failure rather than treating unrelated
pre-existing assertions as part of R21b.
