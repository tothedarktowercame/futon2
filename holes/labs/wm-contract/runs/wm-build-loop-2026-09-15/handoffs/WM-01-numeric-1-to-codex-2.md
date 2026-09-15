# WM-01-numeric-1: preserve numeric meaning through row admission

From claude-3 to codex-2. **Author: codex-2. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28 dispatch `invoke-1789509835504-21264-d64055e8`, under Joe's standing continuation authority (`/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/STANDING-AUTHORITY.md`). The full dispatch text is `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-numeric-1.md` (sha256 `b61a65fcd37c0a723ba3a4802c876b2a57a8e767da4b8859ead4bffe35303b76`). **Read it first; it governs.** This packet adds the facts I checked and the traps I found. It does not widen the dispatch.

## TODO clause and cascade

WM-01: "Record State/Action/Outcome, model revision and numeric semantics consistently". Numeric/runtime correspondence is still open after the historical repair acceptance. Cascade WM-01 K3–K5 is `/home/joe/code/p4ng/wm-walkthroughs/cascades/wm-01/README.md`; K4 is "numeric admission is not exact equality". This change is one necessary part of that correspondence; it does not close WM-01.

## The one behaviour to build

Build **one shared numeric admission result**, produced in `machine_model.clj` and carried through the two existing consumers:
- the belief reader in `machine_belief.clj`;
- each predicted-state step in `machine_predictive.clj`.

The result must be stable, EDN-readable data, with no opaque Java objects. It must contain:
- the original values, unchanged;
- the representation class;
- the exact represented row total;
- the exact absolute deviation from one;
- the criterion applied, with its revision;
- whether the represented masses satisfy exact normalization.

Support identity must be kept at the consumer record, so that permuting values cannot pass as the same distribution.

The existing keyword API (`row-sum-admission` returning `:exact`, `:float-carried` or nil) becomes a **compatibility wrapper over that same result**, not a second validator.

## What I checked in the current source (futon2 `main`, HEAD `7ace7293`)

- `src/futon2/aif/machine_model.clj:46-59`, `row-sum-admission`:
  - All-integer/ratio rows need `(== 1 sum)`.
  - Otherwise every value goes through `(BigDecimal. (double v))`. That is exact for Float/Double, but it silently replaces ratios and BigDecimal decimals by their double images.
- `machine_model.clj:61-69`, `distribution!`: the support-set match, finite and nonnegative checks, then the admission check. Reuse these; don't reimplement them.
- `src/futon2/aif/machine_belief.clj:57-69`: the same checks, then admission is checked and discarded. The `:ok` output (`:model`, `:context`, `:state-support`, `:belief-input`) carries no admission.
- `src/futon2/aif/machine_predictive.clj:39-58`: only the keyword is kept per step (`:admission`). The initial row is `(first (vals (:posteriors belief-input)))`.
- **Callers outside your edit scope.** They must keep working unchanged through the wrapper; do not edit them:
  - `machine_q_risk.clj:31,55-56`
  - `categorical_ambiguity.clj:45`
  - `machine_parameters.clj:51`
  - `work_target_tick.clj:70`
- Existing assertions on the keyword:
  - `work_target_belief_test.clj:57,116`
  - `machine_model_test.clj:114-121`, which includes the retained seven-entry production-shaped float row at lines 110-113.
- **Witness wrappers.** `grep` of `checks/` finds no wrapper that references these three namespaces; the only `machine` hit in `checks/wm_workspace_gate.clj` is `machine_vocabulary_witness`, which is unrelated. Confirm this. If a registered wrapper does depend on a changed file, run only its negative modes, one at a time (futon2 `AGENTS.md`, "Changing a witness").
- Your three allowed source files and `test/futon2/aif/` are clean in the working tree. futon2 is a **shared worktree** with about 189 other dirty paths that are not yours.

## Traps (verified with `clojure -M -e` on this machine)

- **`rationalize` on a double is not exact.** `(rationalize 0.1)` gives `1/10`, because it goes through the decimal string. The exact IEEE value is `(rationalize (BigDecimal. 0.1))`, which gives `3602879701896397/36028797018963968`. If you compute totals as ratios, convert Float/Double through `(BigDecimal. (double v))` first.
- **Ratio + BigDecimal throws** when the result has no terminating decimal: `(+ 1/3 0.5M)` fails with "Non-terminating decimal expansion". Mixed exact rows need rational arithmetic, such as `rationalize` of each BigDecimal (which is exact), not BigDecimal arithmetic.
- **The old path hides an exact decimal row.** `{:a 0.1M :b 0.9M}` sums to exactly 1 as decimals. Through the old path it becomes `1.0000000000000000277555756156289135105907917022705078125M`, which is admitted as `:float-carried` although the represented values are exactly normalized. This is the kind of before/after evidence control 3 asks for.
- EDN round-trips both ratios and BigDecimals (`1/3`, `1.5M`). Test that the result survives `(clojure.edn/read-string (pr-str result))` with equal content. NaN is never `=` to itself, so test NaN refusal by refusal kind, not by value equality.

## One interpretation you must declare, not settle silently

The dispatch fixes two cases:
- integer/ratio rows need an exact sum of one;
- finite Float/Double rows are admitted under the existing `1e-12M` criterion, and are not thereby exactly normalized.

It does not say which admission rule applies to a row made only of BigDecimal values, or to a mixed row. Declare each representation case in code and in the receipt, and give unsupported numeric types a typed refusal.

The wrapper's keyword must not change for any input in the existing tests. For any new case where your rule gives a different verdict or keyword from the old function, list the input with both verdicts in the receipt under "behaviour changes", and raise it in your bellback. Do not decide it quietly either way. Whatever the admission verdict, the exact-normalization flag and the exact total/deviation must report the truth about the represented values.

Do not broaden the tolerance, renormalize, change masses, infer empirical authority, or claim any bound for composed predictions, KL/logarithmic risk or ordering.

## Allowed edits

- `futon2/src/futon2/aif/machine_model.clj`
- `futon2/src/futon2/aif/machine_belief.clj`
- `futon2/src/futon2/aif/machine_predictive.clj`
- Their existing tests: `test/futon2/aif/machine_model_test.clj`, `machine_belief_test.clj` and `machine_predictive_test.clj`. A narrowly scoped test helper is allowed only if genuinely required.
- Receipts under `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-numeric-1/`.

Not allowed: persistence, a new service, belief-domain extension, fabricated observations, new A/B/C, the scorer, migrating other consumers, serving reload, clicks, Lean edits, or any held P1 code. If compatibility needs an edit outside these files, stop and report the exact conflict first.

## Acceptance controls (from the dispatch)

1. **Exact rows.** Exact rational thirds pass as exact; an exact non-unit row fails. Compute the total independently and show the original row is unchanged.
2. **The seven-entry float row** (`machine_model_test.clj:110-113`).
   - Keep its masses.
   - Show approximate admission is distinct from exact normalization.
   - Show the total and deviation are order-independent.
3. **Mixed and decimal rows.** Mixed ratio/float and decimal cases must expose the old calculation's silent coercion without declaring those conversions exact. Test rows inside and outside the tolerance, and exact zero deviation in a floating representation.
4. **The full model/reader boundary** rejects wrong or extra support, negative, NaN and infinite mass. Show that permuting the support with identity preserved differs from permuting the values.
5. **Actual consumption.** Through the existing belief-reader and state-predictor APIs, show the detailed evidence accompanies the actual row consumed at each step, with model and support identity. An unused helper doesn't count.
6. **Receipt.** Record the numeric interpretation and an independent calculation of the totals, done outside the code under test. For example, Python `fractions.Fraction` over `float.hex` values for the seven-entry row. State explicitly: **row-sum error is not a composed prediction or logarithmic risk/ordering error bound.** Claim no correspondence beyond the exact relation checked.

## Gates: run them, keep commands and exit statuses, and don't pipe gate output

```sh
cd /home/joe/code/futon2
clj-kondo --lint src/futon2/aif/machine_model.clj src/futon2/aif/machine_belief.clj src/futon2/aif/machine_predictive.clj \
  test/futon2/aif/machine_model_test.clj test/futon2/aif/machine_belief_test.clj test/futon2/aif/machine_predictive_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)" -- <the same six files>
clojure -X:test :nses '[futon2.aif.machine-model-test futon2.aif.machine-belief-test futon2.aif.machine-predictive-test]'
# compatibility: callers you must not break
clojure -X:test :nses '[futon2.aif.work-target-belief-test futon2.aif.machine-q-risk-test futon2.aif.categorical-ambiguity-test futon2.aif.machine-parameters-test futon2.aif.work-target-tick-test futon2.aif.efe-machine-q-test futon2.aif.trace-test]'
```

The files for check-parens go after `--`; without it the tool prints "No target files." and exits 2. Test JVMs are short-lived tooling, which futon2's `CLAUDE.md` allows. Do not start a serving process and do not reload the running JVM.

If a compatibility test already fails on HEAD before your change, record that failure against HEAD rather than fixing it.

## Commit and receipt

- Commit only the authorized paths, staged by explicit path. Never `commit -a`, never amend, and check `git log -1` before committing.
- `RECEIPT.md` in the receipt directory should record:
  - the source sha256s of the files before and after;
  - the declared representation cases and criterion;
  - before/after boundary evidence, including the old-function outputs on the mixed and decimal cases;
  - the independent calculation;
  - the gate commands and exit statuses;
  - behaviour changes, if any;
  - limits.

## Stop and bellback

Stop once the change, tests and receipt are committed. **Bell claude-3 back** with:
- the commit shas;
- the numeric contract as implemented;
- before/after evidence;
- gate exit statuses;
- behaviour changes;
- unresolved findings.

Anything you discover beyond this scope is a question in the bellback, not a task.
