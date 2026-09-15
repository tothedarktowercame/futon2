# WM-01-numeric-1 receipt

Author: codex-2; reviewer: claude-3. Dispatch:
`invoke-1789509835504-21264-d64055e8`; handoff:
`invoke-1789510028754-21268-d939a7e9`. The governing dispatch hash was checked:
`b61a65fcd37c0a723ba3a4802c876b2a57a8e767da4b8859ead4bffe35303b76`.

Baseline commit: `baseline-head.txt`. The six authorized source/test SHA-256s
before and after are in `before.sha256` and `after.sha256`.

## Numeric contract

`machine-model/numeric-row-admission` is the single numeric implementation.
`row-sum-admission` projects its `:admission` keyword; it does not validate again
through another algorithm. `distribution-admission` adds the existing support
check; the model's `distribution!`, belief reader and predictor all use it.

| Representation | Exact interpretation | Admission rule / revision |
|---|---|---|
| `:exact-rational` | integer/ratio values | exact total = 1; `exact-represented-v1` |
| `:exact-decimal` | BigDecimal values as exact decimal rationals | same exact rule |
| `:mixed-exact` | integers/ratios/BigDecimals, no IEEE value | same exact rule |
| `:ieee-floating` | finite Float/Double represented binary values | absolute deviation <= 1/1000000000000; `v1.1` |
| `:mixed-floating` | preserve each exact value and each IEEE value individually | same unchanged absolute criterion |

Exact totals use rational arithmetic. IEEE conversion goes through
`BigDecimal(double)`, never `rationalize(double)`. BigDecimals are rationalized
directly. Integer summation promotes rather than overflowing. Negative,
nonfinite and unsupported masses refuse; unsupported types carry a typed name,
not an opaque object. A unit-sum IEEE row still reports `:float-carried`, with
`:exactly-normalized? true`; an approximately admitted row reports false.

Every supported finite nonnegative result retains keyed `:values`, per-key
`:representations`, the row representation class, `:exact-total`,
`:exact-deviation`, criterion/revision and exact-normalization flag. Refused
non-unit rows also retain that evidence. Invalid/nonfinite/unsupported inputs
return typed refusals without pretending to have finite totals.

**Float EDN encoding:** raw `(float 0.1)` prints as `0.1`, whose EDN readback
is a different Double value. In evidence only, Float is widened exactly to
Double (`0.10000000149011612`), and its `:float32` identity is retained per key.
Thus evidence values remain `=` to input values and round-trip through EDN.
The original consumer row remains the identical object, including its Float
values. No mass is rounded or renormalized. `extra-cases.edn` records this check.

## Before/after evidence and behavior changes

`cases.edn`, `before.edn`, and `after.edn` contain all normal probes.
`baseline_row_sum.clj` is the exact old function extracted from the recorded
baseline commit, in a separate tooling namespace. `capture.clj --before`
reproduces old outputs, including for added controls; without that argument it
records the new result. `extra_cases.clj` / `extra-cases.edn` cover invalid
inputs, overflow and Float encoding. Existing tested keyword results stay the
same. Newly declared cases with changed wrapper behavior are explicit:

| Input | Old keyword | New keyword / result |
|---|---|---|
| `{:a 0.1M :b 0.9M}` | `:float-carried` | `:exact`; represented total 1 |
| `{:a 1/3 :b 1/6 :c 0.5M}` | `:float-carried` | `:exact`; represented total 1 |
| `{:a 0.1M :b 0.9000000000005M}` | `:float-carried` | nil; exact-only non-unit row |
| `{:a 1/3 :b 1/6 :c 0.5000000000005M}` | `:float-carried` | nil; exact-only non-unit row |
| `{:a -1 :b 2}` | `:exact` | nil; `:invalid-mass` (full model/reader already rejected negatives) |
| `{:a (AtomicInteger. 1)}` | `:float-carried` | nil; `:unsupported-numeric-type` |
| two `Long/MAX_VALUE` masses | ArithmeticException | nil; exact total 18446744073709551614, non-unit refusal |
| NaN / positive Double infinity / negative Float infinity | NumberFormatException | nil; `:invalid-mass` |
| nil / string mass | NullPointerException / ClassCastException | nil; `:unsupported-numeric-type` |

The exact-only near-unit examples have deviation 1/2000000000000. They are
inside the IEEE tolerance but are deliberately not granted that tolerance.
The numeric interpretation for decimal-only and mixed-exact rows is a declared
choice in this packet, raised for review in the bellback.

Examples where the keyword is unchanged but the represented calculation is
corrected:

- `{:a 1/3 :b 0.6666666666666666}`: old coerced total is
  `0.999999999999999944488848768742172978818416595458984375M`;
  new exact total is `27021597764222975/27021597764222976`. Both admit as
  float-carried; the ratio is no longer silently replaced by its IEEE image.
- `{:a 0.1M :b 0.9}`: old coerced total is
  `1.0000000000000000277555756156289135105907917022705078125M`;
  new exact total is `45035996273704961/45035996273704960`.
- The seven-entry retained production float row keeps all masses and remains
  `:float-carried`. Its exact total is `36028797018963969/36028797018963968`,
  deviation `1/36028797018963968`; exact-normalization flag false. The old
  ordinary floating sum's spelling must not be mistaken for this exact total.

Before, the reader discarded admission and predicted steps kept only a keyword.
Now `:numeric-admission` accompanies the unchanged reader `:belief-input`, with
reader model/entity identity. Every predicted step retains its actual row,
model identity, old keyword and detailed numeric result. The detailed result's
`:support` retains the consumer's order, and keyed values retain state identity.
The reader and test kernel use different support orders for the same named row;
those orders remain visible. Swapping values changes the evidence even when the
sum does not change. No consumer or model identity is silently substituted.

## Independent calculation

`python3 independent.py` uses `fractions.Fraction` on exact decimal/ratio
literals and on IEEE values exposed through `float.hex`, reconstructed with
`float.fromhex`. It does not call the Clojure implementation. `independent.json`
retains each literal, IEEE hex where applicable, exact rational, total and
deviation. `independent.edn` supplies the independently generated expectations
used by the tests. The examples cover exact thirds/non-unit rows, the real
seven-entry row, exact zero floating deviation, and mixed/decimal cases both
inside and outside the unchanged IEEE tolerance. Reversing row entries leaves
the calculated total/deviation unchanged.

## Gates and scope

All commands ran from `/home/joe/code/futon2`, in tooling processes. No gate
output was piped: stdout/stderr were redirected to `.log`, and each immediate
exit status was retained in `.exit`.

- `clj-kondo --lint` on the six authorized files plus the three receipt Clojure
  scripts: 0 errors, 0 warnings, exit **0** (`clj-kondo.*`).
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval
  "(arxana-check-parens-cli)" -- <same nine files>`: OK, exit **0**
  (`check-parens.*`).
- `clojure -X:test :nses '[futon2.aif.machine-model-test futon2.aif.machine-belief-test futon2.aif.machine-predictive-test]'`:
  **18 tests, 305 assertions**, no failures/errors, exit **0** (`tests.*`).
- `clojure -X:test :nses '[futon2.aif.work-target-belief-test futon2.aif.machine-q-risk-test futon2.aif.categorical-ambiguity-test futon2.aif.machine-parameters-test futon2.aif.work-target-tick-test futon2.aif.efe-machine-q-test futon2.aif.trace-test]'`:
  **87 tests, 493 assertions**, no failures/errors, exit **0** before and after
  (`baseline-compatibility.*`, `compatibility.*`). No baseline failures.
- Capture/independent commands and their `.exit` files are retained; all exit 0.

Witness dependency search: `rg -n 'machine[_-](model|belief|predictive)' checks`
finds no references. Inspection of `machine_vocabulary_witness.clj` and its
registry/gate entries confirms that it checks unrelated reference fixtures and
Lean vocabulary files, not these Clojure namespaces. No affected registered
witness negative modes were identified; no Lake invocation was run.

## Limits and open questions

**Row-sum error is not a composed prediction or logarithmic risk/ordering error
bound.** This is represented-row admission evidence only. No empirical authority,
new A/B/C, global numeric migration, Lean correspondence or WM-01 completion is
claimed. Other consumers keep the keyword API. Existing transition arithmetic
is unchanged: mixed-type multiplication/addition and accumulated prediction
error are outside this dispatch, including Clojure's ratio/BigDecimal arithmetic
limitations. The new admission does not establish those operations' correctness.

No serving reload, click, persistence or held-P1 edit occurred. No blocking
compatibility conflict was found; the declared behavior changes above require
the independent review requested in the dispatch.
