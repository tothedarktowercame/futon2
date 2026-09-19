# Ordinary selection precedence — 2026-09-19

Authority: `RULING-selection-precedence-2026-09-19.md`. Implementation `8b6827da` removes automatic repair-entry pre-emption, judgement-transform suppression, posterior-discrimination suppression, and controller-decision omission. Existing repair-entry helper and historical revalidation execution branch remain. Historical candidate probing now occurs only for an explicitly selected historical action; ordinary author prompts do not carry the former repair-only stop-line instruction list.

The selection checkpoint retains `:open-stop-lines {:count n :ids [...]}` from the exact repair-open observation; the durable tick record copies that evidence, including an empty observation. This observes all obligations returned by the existing reader (including awaiting-validation), not just the former pre-emption-eligible first member. Existing external repair/store verbs and production click budgeting are unchanged. No click or shared-JVM reload was performed.

A single CLI registration bound subject `wm-runner/selection-always` to warrant `test-registry-27e7b793e5c88d371700844297e6c3efb71787b26cf9d04b6231df86ad690f5e`. Namespace `futon2.aif.selection-always-test`: 2 tests / 19 assertions / zero failures/errors, exit 0. Static gates: clj-kondo zero errors/warnings; check-parens OK. Spec, runner log, closure, stdout/stderr and exit are in `runs/selection-always-2026-09-19/`.

Regression scope: real runner selection, discrimination, transform, selection checkpoint and durable record, with two open obligations vs none. Controlled policy judgement from the existing runner fixture; execution intentionally ends at construction before dispatch. Hermetic repair and trace fixtures protect production stores. This is not a full production run or policy-generation test. Existing tests which assert the superseded automatic diversion have not been rerun or claimed green; the old repair branch remains available for explicit callers.

Independent review is commissioned separately and appended below.
