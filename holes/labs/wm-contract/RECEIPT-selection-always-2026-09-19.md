# Ordinary selection precedence — 2026-09-19

Authority: `RULING-selection-precedence-2026-09-19.md`. Implementation `8b6827da` removes automatic repair-entry pre-emption, judgement-transform suppression, posterior-discrimination suppression, and controller-decision omission. Existing repair-entry helper and historical revalidation execution branch remain. Historical candidate probing now occurs only for an explicitly selected historical action; ordinary author prompts do not carry the former repair-only stop-line instruction list.

The selection checkpoint retains `:open-stop-lines {:count n :ids [...]}` from the exact repair-open observation; the durable tick record copies that evidence, including an empty observation. This observes all obligations returned by the existing reader (including awaiting-validation), not just the former pre-emption-eligible first member. Existing external repair/store verbs and production click budgeting are unchanged. No click or shared-JVM reload was performed.

A single CLI registration bound subject `wm-runner/selection-always` to warrant `test-registry-27e7b793e5c88d371700844297e6c3efb71787b26cf9d04b6231df86ad690f5e`. Namespace `futon2.aif.selection-always-test`: 2 tests / 19 assertions / zero failures/errors, exit 0. Static gates: clj-kondo zero errors/warnings; check-parens OK. Spec, runner log, closure, stdout/stderr and exit are in `runs/selection-always-2026-09-19/`.

Regression scope: real runner selection, discrimination, transform, selection checkpoint and durable record, with two open obligations vs none. Controlled policy judgement from the existing runner fixture; execution intentionally ends at construction before dispatch. Hermetic repair and trace fixtures protect production stores. This is not a full production run or policy-generation test. Existing tests which assert the superseded automatic diversion have not been rerun or claimed green; the old repair branch remains available for explicit callers.

Independent review is commissioned separately and appended below.

## One reconciliation following independent review

Codex-15 review `b760e5ab` / job `invoke-1789847623191-22503-b9ce22fc` requested changes: the original selection-only patch still let queue-first recoverable memory read/reuse an old job downstream, and explicit historical B could execute queue-first A. Its original warrant was valid but insufficiently scoped to these behaviors.

Reconciliation `420faeb0` binds singular `stop-line` only when the enacted entry is a repair action, matching its selected target against the observed queue. Thus ordinary recovery reads, reuse, supersession, and discharge-contract fallback see nil; separately authorized successor validation remains unchanged. The retained historical helper validates admission identity and actors before execution; the original transition checks remain.

Corrected namespace executed once: 4 tests / 31 assertions / zero failures/errors, exit 0. New tests reach ordinary fresh dispatch with an unrelated running recovery job and assert no job read; select historical B with A first and assert execution receives B; mismatched admission refuses before execution. Hermetic production-file-set assertions passed. Refreshed bound warrant: `test-registry-f352e8317f8dd879d2ab5364e09527a3384f344bc2750bcf307c1f12a36f58f7`. Corrected spec, stdout/stderr, runner log, closure and clean gates: `runs/selection-always-2026-09-19/reconciled/`. No production clicks.
