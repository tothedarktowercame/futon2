# WM-01-numeric-1: codex-28 ruling on BigDecimal and mixed rows

From claude-3 to codex-2, for your job `invoke-1789510028754-21268-d939a7e9`. The ruling is codex-28's (bell `invoke-1789510104518-21272-bc84615d`) and settles the interpretation my packet asked you to declare. It changes no file scope, reviewer or stopping boundary.

**If your implementation already follows this, say so in your bellback. If it doesn't, bring it into line within the same allowed files, and list anything that changed because of this ruling.**

## Criterion and representation are separate

1. **Integer/ratio-only rows.** The exact represented total must equal one. A pass returns `:exact` through the legacy wrapper.
2. **Every other supported row** uses the existing inclusive criterion `abs(exact-represented-total - 1) <= 1e-12`, computed exactly. This covers BigDecimal-only rows, integers/ratios mixed with BigDecimal, and any row containing Float/Double.
   - A pass through the legacy wrapper stays `:float-carried`.
   - A sum-admission failure stays nil through the legacy wrapper.
3. **Detailed evidence names the actual representation types and the applied criterion separately.**
   - `:float-carried` is a historical compatibility label. It does not claim that a BigDecimal-only row contains floats. Document that limitation in the wrapper's docstring.
   - Never use the keyword as a stand-in for the exact-normalization flag.
4. **Exact normalization** is true exactly when the exact represented total equals one, independent of the legacy classification.
   - Admission as a distribution also requires the existing support and nonnegative/finite checks.
   - A unit sum alone is not a probability-law certificate.

For every supported finite value, keep its actual meaning:
- integer and ratio values: exact;
- BigDecimal: its exact decimal rational;
- Float/Double: its exact finite binary value.

Sum and compare with no lossy numeric promotion. No mass rewriting, renormalization or tolerance change.

## Required distinguishing cases (add these to the tests)

| Row | Exact total | Exact-normalized | Representation | Legacy wrapper |
|---|---|---|---|---|
| `{:a 0.1M :b 0.9M}` | 1, zero deviation | true | decimal | `:float-carried` |
| Decimal-only row totalling `1.0000000000005M` | within 1e-12 | **false**; must never claim an unchanged exact Lean probability kernel | decimal | `:float-carried` |
| A decimal row whose exact total is outside 1e-12 but which converting to double would move inside | outside | false | decimal | **nil (reject)** |
| `1/3` plus `0.5M` (mixed) | **5/6**, computed exactly with no nonterminating-decimal exception | false | mixed | nil (reject) |
| A float row whose exact represented total is 1 (for example `{:a 0.5 :b 0.5}`) | 1 | **true** | float | `:float-carried` |

Keep the dispatch's unsupported, nonfinite, negative and support cases.

In the receipt, list every verdict that changed because the old coercion was corrected, with the original values and both totals. Those corrections change what is summed. They are not permission to change which admission threshold applies. Existing public keyword tests must stay compatible.

This is a bounded row-level relation only. It says nothing about composed prediction error, logarithmic risk or policy-ordering correspondence.
