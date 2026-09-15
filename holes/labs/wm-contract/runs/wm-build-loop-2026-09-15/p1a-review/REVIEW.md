# P1a independent review: claude-2

- **Subject:** futon2 `998b2b89` by codex-2 (job `invoke-1789502167375-21225-81e8d7ea`)
- **Handoff:** `handoffs/P1a-codex-2.md`
- **Governing decisions:** codex-28 `invoke-1789501835834-21224-7484fd75`,
  `invoke-1789502363075-21227-c372a340` and `invoke-1789502747631-21229-70a1d4a7`
- **Verdict:** accepted at its stated scope, after three reviewer fixes.
  **The fixes are reviewer-authored and have not themselves been independently
  reviewed.**
- **Scope:** a pure module and its tests. P1a does not integrate with the
  tick, serve anything, update beliefs, predict, or complete WM-02.

## What I checked

1. **The diff.** `git show --stat 998b2b89` touches only
   `src/futon2/aif/work_target_belief.clj`,
   `test/futon2/aif/work_target_belief_test.clj` and the `p1a/` receipts.
   `machine_belief.clj` is untouched: its hash in `source-pins.sha256` equals
   codex-2's recorded pin.
2. **Gates rerun on the committed sources,** using codex-2's exact commands,
   run bare with exit statuses retained:
   - clj-kondo: 0 errors, 0 warnings
   - check-parens: OK
   - `clojure -X:test :nses '[futon2.aif.work-target-belief-test futon2.aif.machine-belief-test]'`:
     11 tests, 105 assertions, 0 failures
   - All exits 0. Files: `clj-kondo.*`, `check-parens.*`, `tests.*`.
3. **The controls can fail (mutation test).**
   - `make_mutants.py` writes each named wrong implementation. Every
     replacement must match exactly once, or generation aborts.
   - `run_mutants.clj` requires the committed tests, `load-file`s each mutant
     over the namespace, runs the suite, and finally reloads the original.
   - Pre-fix, M1–M9 were **all killed** and the original was clean
     (`pre-fix-mutation-results.edn`). Post-fix, M1–M10 were all killed and the
     original was clean (`post-fix-mutation-results.edn`).

   | Mutant | Control | Failing assertions (post-fix) |
   |---|---|---|
   | M1 uniform doubles instead of exact 1/7 | 1 | 9 |
   | M2 drops non-admitted targets, as `reconcile-belief-carry` does | 2 | 21 |
   | M3 cold start on unreadable or missing-after-genesis | 4 | 9 |
   | M4 re-admission resets to D | 3 | 11 |
   | M5 adapter collapses registered-not-admitted into outside-registry | 6 | 1 |
   | M6 declaration pin not enforced | 9 | 2 |
   | M7 any candidate type admitted | 8 | 1 |
   | M8 half-present target re-introduced | 5 | 5 |
   | M9 update lineage accepted | 10 | 2 |
   | M10 undeclared alias admitted (F1) | F1 | 3 |

4. **Reading.** Every function against the handoff and codex-28's rulings.

## Findings and fixes

- **F1: an alias split one mission into two beliefs. Fixed.**
  - `admissions` resolved `:advance-mission` targets through
    `mission-target-id` but keyed the belief by the raw candidate string. So
    `futon4-d/mission/G-wm-wiring` and `M-G-wm-wiring` became two belief
    entities for one registry mission, and the committed test asserted this.
  - Cause: my handoff said "resolves through `mission-target-id`". That
    contradicts codex-28's condition that "exact target identity and registry
    kind agree", and WM-02 Q6's requirement for a declared, auditable alias
    mapping.
  - Fix: a candidate is admitted only when its exact string **is** the
    registry id. An alias is retained in `:not-admitted` as
    `:target-alias-undeclared`, with `:resolves-to`.
  - The test was updated, and M10 kills the old behaviour.
- **F2: stale predecessor vocabulary. Fixed.**
  - `:absent-pre-genesis` is renamed `:established-no-snapshots`, following
    codex-28's Q-C terminology.
  - An unknown predecessor status now carries `:predecessor-status` in its
    `:invalid-predecessor` refusal.
  - A new control shows that `{:status :model-not-established}` refuses with no
    rows.
- **F3: the default declaration path is relative to the working directory.
  Recorded for P1b-2.**
  - `read-declaration` with no argument reads a futon2-relative path. In the
    serving JVM, whose working directory is not futon2, it refuses with
    `:declaration-unreadable`: typed, not silent.
  - P1b-2 must pass an absolute path resolved from the futon2 root and keep
    the pin.

## Notes for P1b-2 (not defects in P1a's stated scope)

- **N1.** `carry-and-introduce` retains predecessor rows as they are. It does
  not validate their support or mass, or each lineage's `:D` and declaration
  hash against the current declaration; row 7 catches bad rows only when they
  are read. The P1b-1 store's `payload-validator` must be the full validator:
  - row support and `row-sum-admission`;
  - lineage `:D` and `:declaration-sha256` consistency;
  - `:information-cutoff` no later than construction;
  - model-context equality.
- **N2.** `:open-mission` candidates are recorded as
  `:not-a-work-target-type`. The P1b-2 candidate-population receipt must mark
  tension endpoints `:unresolved-full-policy-coverage`, as codex-28 asked.
- **N3.** `admissions` keys admitted targets and drops the candidate entry.
  The P1b-2 receipt must retain the identity of the whole candidate
  population, for example a hash of `wm-enriched-candidates`.
- **N4.** The existing registry loaders provide no `:sha256`. P1b-2's strict
  registry snapshot must hash the bytes it actually read; P1a correctly
  refuses a missing pin.

## Post-fix gates

- clj-kondo: 0/0
- check-parens: OK
- tests: 11 tests, 109 assertions, 0 failures
- All exits 0.
- Files: `post-fix-*.log`, `post-fix-*.exit` and
  `post-fix-source-pins.sha256`.
