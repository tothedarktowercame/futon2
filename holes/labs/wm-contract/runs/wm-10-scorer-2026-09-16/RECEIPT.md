# WM-10 canonical scorer: first direct implementation

Author: codex-28; no dispatched agents. Implements closure-plan D2/C4's finite scorer and rational numeric enclosure at source/test scope. Agreed plan: p4ng a727b02, SHA256 27644d7612a4be243f8be2b1042d5c5d374aa3380cf914162c26a7ea5de93543.

## Result

New `futon2.aif.cascade-g/step-g` constructs the joint and marginal from supplied q/A and computes risk plus conditional ambiguity. `total-g` sums an explicitly supplied schedule, enforcing shared model, supports, policy and occurrence. The alternative expression is calculated from the same joint. All six numerical components enclose an independent 90-digit Python reference; example G is approximately 0.52162817912052683675.

The logarithm series gives exact rational upper/lower bounds, not a guessed tolerance. See CONTRACT.md for derivation, limits and interface binding. Binary64 values retain their exact represented rational values; approximate normalization is not converted to exact normalization.

## Checks executed

- clj-kondo: 0 errors, 0 warnings, exit 0.
- check-parens: OK, exit 0.
- New scorer plus existing ambiguity/risk namespaces: 13 tests, 67 assertions, 0 failures/errors, exit 0.
- `git diff --check`: exit 0.
- Commands, exit statuses and stdout retained in gates.json and corresponding text files.

An initial test reader error (ratio literal with an invalid N suffix) was corrected before the successful gates. No production or shared-JVM actions occurred.

## Not established

Independent authorship review, Lean proof of the interval implementation, real WM-05/06 inputs, empirical A, firing/history correspondence, candidate coverage, delta-G gate and WM-11 selection consumption remain outstanding. Neither WM-10-delivery nor the whole checkbox is marked complete. No existing authority validator, model source or selection code changed.
