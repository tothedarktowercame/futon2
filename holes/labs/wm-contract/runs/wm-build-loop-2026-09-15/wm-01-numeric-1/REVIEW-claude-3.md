# WM-01-numeric-1: claude-3 review

**Reviewer verdict: ACCEPT at source/test scope** for the combined change futon2 `ac821857` plus `7f546b63` (author codex-2). The one conflict with codex-28's ruling, in `ac821857`, is corrected in `7f546b63`. That is what I accept. This is not serving activation, and it does not complete WM-01.

- Dispatch: codex-28 `invoke-1789509835504-21264-d64055e8`.
- Ruling: codex-28 `invoke-1789510104518-21272-bc84615d`, forwarded as `invoke-1789510179031-21273-4054f2f0`.
- Author jobs: `invoke-1789510028754-21268-d939a7e9` (implementation) and `invoke-1789510179031-21273-4054f2f0` (ruling correction).
- Handoff packet and forwarded ruling: `../handoffs/WM-01-numeric-1-to-codex-2.md` and `../handoffs/WM-01-numeric-1-decision-to-codex-2.md`, committed by the inbox-zero promotion `1d0959ae`.

## What I checked

1. **Scope.**
   - Across `ac821857` and `7f546b63`, every changed path is one of `src/futon2/aif/machine_{model,belief,predictive}.clj`, their three test files, or this receipt directory.
   - The four callers outside scope are untouched: `machine_q_risk`, `categorical_ambiguity`, `machine_parameters` and `work_target_tick`.
   - The only later commit, `1d0959ae`, adds my two handoff files and touches nothing in scope.
2. **Diff, read in full.**
   - `numeric-row-admission` is the single numeric result.
   - `row-sum-admission` is a one-line projection of its `:admission`.
   - `distribution-admission` adds only the support check. `distribution!`, the belief reader and every predictor step (initial and produced) consume it.
   - Float/Double values go through `(rationalize (BigDecimal. (double v)))`; there is no `rationalize` on a raw double. BigDecimals are rationalized directly. Totals use `+'`.
   - `float-row-tolerance` is unchanged at `1e-12M`. Nothing renormalizes. Consumer rows are passed on as the identical object.
3. **The ruling, checked in my own JVM against the committed code.**

   | Row | Representation | Exact total | Normalized? | Wrapper result |
   |---|---|---|---|---|
   | `{:a 0.1M :b 0.9M}` | `:exact-decimal` | 1 | true | `:float-carried` |
   | `{:a 0.5M :b 0.5000000000005M}` | | `2000000000001/2000000000000` | false | `:float-carried` |
   | `{:a 1/3 :b 0.5M}` | | `5/6` | false | nil (no exception) |
   | `{:a 0.5 :b 0.5}` | `:ieee-floating` | 1 | true | `:float-carried` |
   | `{:a 1/3 :b 2/3}` | `:exact-rational` | 1 | true | `:exact` |
   | ratio row with deviation `1/2000000000000` | | | false | nil (exact rule kept) |
   | `{:a 1/3 :b 1/6 :c 0.5M}` | `:mixed-exact` | 1 | true | `:float-carried` |

   - A Float32 row and a row containing `-0.0` are both admitted as `:ieee-floating`.
   - Every result is equal after an EDN round-trip.
   - The wrapper docstring says `:float-carried` is a historical compatibility label.
4. **Independent arithmetic** (Python `Fraction`, exact on floats; it never calls the Clojure code).
   - Seven-entry production row: total `36028797018963969/36028797018963968`. The deviation is exactly `1/2^55`, the same in reverse order, and within the bound.
   - `1/3 + 0.6666666666666666` = `27021597764222975/27021597764222976`.
   - `0.1M + 0.9` (double) = `45035996273704961/45035996273704960`.
   - `0.1M + 0.9M` = 1, and `1/3 + 0.5M` = 5/6.
   - Near-unit decimal deviation: `1/2000000000000`.
   - The row whose verdict flips, `{:a 0.01M :b 0.990000000001000001M}`:
     - old coerced total `576460752303999931/576460752303423488`, within the bound;
     - exact total `1000000000001000001/1000000000000000000`, outside it.
     - Both match the receipt.
   - The at-bound row `{:a 0.01M :b 0.990000000001M}` has deviation exactly `1e-12`, and is admitted inclusively.
5. **Gates, re-run myself.** The tree was at HEAD `1d0959ae`, and the six files' sha256s match codex-2's `after.sha256`. Logs are in `/tmp/claude3-numeric-review/`.

   | Gate | Result |
   |---|---|
   | clj-kondo on the six files | 0 errors, 0 warnings, exit 0 |
   | check-parens, files after `--` | OK, exit 0 |
   | `clojure -X:test`, the three target namespaces | 19 tests, 375 assertions, 0 failures, exit 0 |
   | the seven compatibility namespaces | 87 tests, 493 assertions, 0 failures, exit 0 |

6. **Controls 4 and 5, by reading the tests.**
   - **Reader.**
     - The reader's `:numeric-admission` equals a fresh `distribution-admission` of the same row, and the posterior is `identical?` to the input.
     - Permuted values keep the total but change the evidence; a changed support order is visible.
     - Wrong or extra support, negative, NaN and both infinities are rejected, as are unsupported types, each by refusal kind.
     - The seven-entry row is admitted as `:float-carried` with normalized false, and the result round-trips through EDN.
   - **Predictor.**
     - Every step carries `(:model kernel)` and the kernel support.
     - Each step's admission equals `distribution-admission` recomputed on that step's own row. Step 1's values differ from step 0's, so a reused initial admission would fail, and the terminal row equals the last step's values.
     - A bad row produced mid-plan (a `with-redefs` of `apply-belief`) is refused at `[:steps 1]`.

## Findings

- **F1: resolved.** `ac821857` applied the exact rule to decimal-only and mixed-exact rows, contrary to codex-28's ruling. The ruling was queued behind that job, because Agency cannot deliver a message into a running job. `7f546b63` corrects it, and the checks above confirm the correction.
- **F2: reviewed behaviour change, not a defect. The wrapper is now stricter for every caller, including the four outside scope.**

  | Input | Before | Now |
  |---|---|---|
  | `{:a -1 :b 2}` | `:exact` | nil |
  | an `AtomicInteger` mass | `:float-carried` | nil |
  | NaN, infinity, nil or string | threw an exception | nil |
  | two `Long/MAX_VALUE` masses | overflowed (ArithmeticException) | nil |
  | `{:a 0.01M :b 0.990000000001000001M}` | `:float-carried` | nil (the corrected coercion) |

  - All four outside callers already treat nil as a refusal:
    - `machine_q_risk.clj:31` and `categorical_ambiguity.clj:45` use `(or (row-sum-admission …) <refusal>)`;
    - `machine_parameters.clj:51` checks `(nil? …)`;
    - `work_target_tick.clj:70` uses `(demand! (some? …))`.
  - Their compatibility tests pass.
  - At the reader, a nil or string mass now refuses as `:unsupported-numeric-type` rather than `:invalid-mass`.
  - codex-28 may want to record the stricter wrapper as a deliberate change to callers outside this scope.
- **F3: pre-existing, out of scope, unchanged.** `predicted-state-plan` takes its initial row as `(first (vals (:posteriors belief-input)))`, not keyed by the policy's entity. Belief input is single-entity only, and this change does not alter that.
- **Note on control 3.** A single-value decimal row near `1+1e-12` cannot cross the bound under double conversion: the midpoint to the next double lies below the bound, as I found by trying. A crossing needs at least two entries, whose rounding errors add up. codex-2's two-entry case does cross, as verified above.

## Limits

- Source/test scope only. Nothing was reloaded into the serving JVM, and no production row was observed through the new path.
- Mixed-type arithmetic inside transitions is unchanged and was not reviewed; codex-2 flagged it as outside the dispatch.
- I did not re-run codex-2's `capture.clj` or `extra_cases.clj`. I checked the before and after totals for the key cases independently instead.
- **Row-sum error is not a composed prediction or logarithmic risk/ordering error bound.**
- WM-01 stays open for three things: the formal numeric relation at other operations, rebinding dependent proofs and admissions, and consistent model identity at every actual consumer.
