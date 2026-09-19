# Lane 5: nil selection receipt

Finding: `repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure` (schema 3).

The retained finding records selection failure, Java Named.getName null, nil selected-entry and nil failure-data. It does not retain the identity of the nil carrier. Historical commit `b30d915ecc9a0b4547724658d4035a6d381b2e0b` documents a live reproduction of this cohort-57 attempt-002 exception at war_machine.clj:4754: the producer omitted `:r12-admission` from the renderer map. That commit already repaired both the handoff and the renderer's nil case. Current `scripts/futon2/report/war_machine.clj:4758` consumes the receipt and line 7172 hands it over. This packet does not claim a new production fix.

Regression commits `b8a9399d75c9f71f1ec312fe3579ad0f825fa50f` and `1fcef69d127bc9c0f7205f9cc2824df039e981ad` add `test/futon2/report/selection_nil_receipt_test.clj:14`: the exact missing receipt renders NOT WIRED rather than coercing nil with name; admitted and refused receipts flow from calibration through the actual generator to the renderer (line 23). A valid action is consumed by the actual runner selected-entry (line 46). Scan ports and policy judgement are stubbed: this warrants the receipt handoff and selection consumption, not policy scoring.

One CLI registration, subject `wm-selection/typed-nil-refusal`, succeeded: 2 tests, 11 assertions, zero failures/errors, exit 0. Warrant: `test-registry-1aee489d4d02247ab846f8c62a070eb655ddcb914765609bf72b829b307637a7`. Spec, stdout/stderr, exit, runner log, closure and static gate output are under `runs/typed-nil-selection-2026-09-19/`. clj-kondo: zero errors/warnings; check-parens: OK. No clicks, shared-JVM reload, or finding mutation during this stage.

Independent review and any subsequent implementation recording are receipted below after completion.
