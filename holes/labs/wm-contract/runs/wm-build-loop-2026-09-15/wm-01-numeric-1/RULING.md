# Codex-28 numeric criterion ruling implemented

Ruling: `invoke-1789510104518-21272-bc84615d`, relayed by claude-3 in
`invoke-1789510179031-21273-4054f2f0`. Author: codex-2; reviewer: claude-3.

The first implementation (`ac821857`) did **not** follow this later ruling:
it required exact equality for decimal-only and mixed-exact rows. This
follow-up corrects that choice. Only `machine_model.clj`, its existing test,
and this receipt directory changed. `pre-ruling.sha256` records the prior
six-file state; `after.sha256` records the final state.

## Contract

- Integer/ratio-only rows: exact sum one, legacy `:exact`.
- All other supported rows, including decimal-only and mixed-exact: inclusive
  exact deviation <= 1/1000000000000, legacy `:float-carried` when admitted.
- Representation, criterion, and exact-normalization flag remain separate.
  The wrapper docstring now explicitly says that `:float-carried` is a
  historical compatibility label, not evidence of floating representation.
- Values and their exact total/deviation computation did not change in this
  follow-up. No mass normalization, coercion, or tolerance expansion was added.

## Changes against ac821857

| Original values | Previous result | Corrected result | Exact normalization |
|---|---|---|---|
| `{:a 0.1M :b 0.9M}` | `:exact` | `:float-carried` | true |
| `{:a 1/3 :b 1/6 :c 0.5M}` | `:exact` | `:float-carried` | true |
| `{:a 0.1M :b 0.9000000000005M}` | nil | `:float-carried` | false |
| `{:a 1/3 :b 1/6 :c 0.5000000000005M}` | nil | `:float-carried` | false |

These restore the pre-dispatch legacy keywords on the same examples. They do
not restore the old coercion: totals still describe the original exact values.

## Correction that changes a verdict versus the pre-dispatch function

Original row: `{:a 0.01M :b 0.990000000001000001M}`.

- Old double-coerced total: `576460752303999931/576460752303423488`.
  Deviation `576443/576460752303423488` is within 1e-12; old keyword
  `:float-carried`.
- Correct represented total: `1000000000001000001/1000000000000000000`.
  Deviation `1000001/1000000000000000000` is strictly above 1e-12;
  new keyword nil, `:exactly-normalized? false`.

This is the only ordinary finite nonnegative probe whose admission verdict
changes relative to the original pre-dispatch function. Other invalid-input
changes are retained in RECEIPT.md and extra-cases.edn. before.edn and after.edn
contain the full original values and both calculations; independent.py and its
Fraction outputs independently confirm the corrected total/deviation.

Added controls also cover the exact inclusive boundary, `1/3 + 0.5M = 5/6`
without a nonterminating-decimal exception, and a unit-sum float row that keeps
`:float-carried` while reporting exact normalization. All representation cases
now assert the applicable criterion separately.

## Gates

Commands are retained verbatim in commands.txt. Final logs and immediate exit
statuses are in clj-kondo.*, check-parens.*, tests.*, compatibility.*:

- clj-kondo: 0 errors, 0 warnings; exit 0.
- check-parens: OK; exit 0.
- Boundary namespaces: 19 tests, 375 assertions; no failures/errors; exit 0.
- Compatibility namespaces: 87 tests, 493 assertions; no failures/errors; exit 0.
- Independent calculation and before/after capture: exit 0.

No registered witness dependency changed; no Lean, serving or held-P1 work
was performed. Row-sum error is not a composed prediction or logarithmic
risk/ordering error bound. No blocking conflict remains; independent code
review is still required.
