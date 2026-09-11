# Locked reconciliation gate and corrected disabled qualification plan

4921fd85 places reconciliation before persisted-terminal advancement inside
the existing controller JVM/OS lock. Independent retained interleaving now
resolves repair-057 before series completion. Updated race expectations name
series-terminal explicitly (the interleaved request already wrote trial-terminal)
and assert the repair store is empty. No failure is filtered or suppressed.
Result: 1 test / 25 assertions, zero failures/errors. The ordinary U88 fixture
retains its original trial-terminal/succeeded default expectation.

Packet commands in 77eb0901 were invalid for Futon2: its :test alias has exec-fn,
not main-opts; clojure -M:test:test-all -n ... exits 1 trying to open file -n.
Corrected both commands to clojure -M:test -m cognitect.test-runner -n NS.
Executed both exact commands: runner 130 tests / 617 assertions and repair store
9 tests / 36 assertions, zero failures/errors. Producer validate-plan! and all
four source hash checks pass. The disabled admission packet now pins plan SHA
23cea4e01492e412a4e2f76416fb8e91b9ae6a32f63b75d5133adf2d8692f9ce.
No historical execution receipt has been manufactured: these command validations
are not qualification/independent-review/admission records. The preparation
continues to say execution not-performed and successor unmaterialized.

Remaining preparation: actual qualification producer receipt and independent
review, immutable verification record, then disabled receipt-derived admission
configuration. A successor link must bind the eventual canonical admission and
execution identity. Existing failed attempts and capacity remain untouched;
this turn changes tests and disabled preparation only. No restart or launch.
Lint, parens and scoped diff checks pass for changed Clojure fixtures.
