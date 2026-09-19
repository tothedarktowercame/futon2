# Lane 5: nil selection receipt

Finding: `repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure` (schema 3).

The retained finding records selection failure, Java Named.getName null, nil selected-entry and nil failure-data. It does not retain the identity of the nil carrier. Historical commit `b30d915ecc9a0b4547724658d4035a6d381b2e0b` documents a live reproduction of this cohort-57 attempt-002 exception at war_machine.clj:4754: the producer omitted `:r12-admission` from the renderer map. That commit already repaired both the handoff and the renderer's nil case. Current `scripts/futon2/report/war_machine.clj:4758` consumes the receipt and line 7172 hands it over. This packet does not claim a new production fix.

Regression commits `b8a9399d75c9f71f1ec312fe3579ad0f825fa50f` and `1fcef69d127bc9c0f7205f9cc2824df039e981ad` add `test/futon2/report/selection_nil_receipt_test.clj:14`: the exact missing receipt renders NOT WIRED rather than coercing nil with name; admitted and refused receipts flow from calibration through the actual generator to the renderer (line 23). A valid action is consumed by the actual runner selected-entry (line 46). Scan ports and policy judgement are stubbed: this warrants the receipt handoff and selection consumption, not policy scoring.

One CLI registration, subject `wm-selection/typed-nil-refusal`, succeeded: 2 tests, 11 assertions, zero failures/errors, exit 0. Warrant: `test-registry-1aee489d4d02247ab846f8c62a070eb655ddcb914765609bf72b829b307637a7`. Spec, stdout/stderr, exit, runner log, closure and static gate output are under `runs/typed-nil-selection-2026-09-19/`. clj-kondo: zero errors/warnings; check-parens: OK. No clicks, shared-JVM reload, or finding mutation during this stage.

Independent review and any subsequent implementation recording are receipted below after completion.

## Independent review and implementation recording

Codex-23 **APPROVED** in `REVIEW-typed-nil-selection-2026-09-19.md`, commit `8f8f14820856942ca16bc3b4dc6d9dd66a982e30`. Review job `invoke-1789845496196-22483-2150813f` was independently observed through the live Agency API as done, executed true, eight tool/command events. Reviewer consumed the warrant via POST test-registry/check without executing tests; the note explicitly attests the bounded regression witness.

The schema-3 predicate at `src/futon2/aif/repair_obligation.clj:112` was read before assembling `implementation-input.edn`. Git independently confirmed full package SHA `1fcef69d127bc9c0f7205f9cc2824df039e981ad`, its parent `b8a9399d75c9f71f1ec312fe3579ad0f825fa50f`, and ancestry into observed HEAD `8f8f14820856942ca16bc3b4dc6d9dd66a982e30`. Its 19:15:31Z commit falls between author job start 19:13:14Z and observation at 19:20:32Z (author job still running). Review note names exactly that package. These observations ground fresh-author, descendant, in-author-window, corroboration and no-disagreement; raw reduced observations are in `runs/typed-nil-selection-2026-09-19/observations.json`.

One `record-implementation!` call accepted the finding, with distinct attempt `typed-nil-selection-2026-09-19`. **Runner-eligible: 5 → 4**. Status is **:awaiting-validation**. Durable record:

`/home/joe/code/futon2/data/wm-repair-obligations/implementations/repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure.edn`

The packet retains a byte copy as `implementation-record.edn`, exact input, stdout/stderr and review commission. No store contract refused. An initial temporary recording script had an unmatched delimiter before any store call; its reader error is retained as `record-script-read-error.stderr`. Correcting that script did not rerun the registered suite.

No resolve! call: production-successor validation remains open. No other finding mutated, no clicks, no reload. Production fix remains the historical `b30d915e`; new commits warrant and review that repair rather than claiming a second implementation of it.
