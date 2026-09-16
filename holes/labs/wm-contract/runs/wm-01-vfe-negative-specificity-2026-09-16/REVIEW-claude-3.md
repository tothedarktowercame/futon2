# WM-01-vfe-negative-specificity-1: claude-3 independent review of c88fca8f

**Reviewer verdict: ACCEPT** futon2 `c88fca8fc6826e832a79995e202f84bcf97dcda9` (author codex-7) at bounded source scope, against the packet at p4ng `f59731b` (`WM-01-vfe-negative-specificity-1.md`, sha256 `c1cb0311…`). It resolves F1 of review `68c2cbc1`:

- the weakened-positive control now passes only with a valid unmodified baseline, a real elaborating weakening, and exactly `[:positive-source-drift]`;
- baseline or setup failure is reported separately (exit 1) from a non-specific mutation result (exit 2).

This is gate correctness only. It is not WM-01 closure, R4 admission or a normalization claim.

Released by codex-28 (bell `invoke-1789581584629-21667-25890c4a`) after the WM-09 final verdict.

## Scope and pins

- **Commit scope.** `c88fca8f` changes `checks/variational_free_energy_witness.clj`, adds `checks/variational_free_energy_witness_control_test.clj`, and adds receipt files under this directory. No later commit touches the wrapper, its test or the shared validator, and the worktree equals the commit for all three.
- **Pins.** Every `IMMUTABLE-PINS.json` entry (reference fixture, positive receipt, `VariationalFreeEnergyWitness.lean`, `VariationalFreeEnergyNegative.lean`, `Holes.lean`) and every `SOURCE-PINS.json` entry (wrapper, test, `positive_proof_receipt.clj`, packet) matched before and after my runs (`pins-before.txt`, `pins-after.txt`). The canonical receipt, fixture and validator were last changed before this commit (`c7a6f24f`).
- **mathlib4.** The VFE Lean files and `Holes.lean` are clean. Other uncommitted mathlib4 edits (`InteroceptivePolicyPosteriorFinite`, `MachinePolicyPosteriorWitness`, `PolicyPosterior`) belong to other work and are untouched here.

## Diff reading

- The old weakened branch (a bare `(not (:pass? receipt-report))`, which passed on any failure, including a stale baseline) is removed from the other modes. `other-main` keeps the positive, value/type-negative and unrelated-edit logic unchanged.
- `weakened-control` checks, in order:
  1. baseline `receipt/validate` with no overrides must be `pass? true`, `failures []`, and fixture = expected, else `:baseline-failed`;
  2. the real witness must elaborate, else `:baseline-elaboration-failed`;
  3. the mutation must change the source, else `:mutation-unchanged`;
  4. the mutated source must elaborate, else `:mutation-elaboration-failed`;
  5. the overridden report must be `pass? false` with failures exactly `[:positive-source-drift]`, else exit 2 `:unexpected-mutation-result`.

  No validator or canonical byte is weakened, and the mutation lives only in memory and a deleted temp file.
- **`-main` guard.** Running `-main` is now guarded by `(= *file* (System/getProperty "babashka.file"))`, so the namespace can be required. A skipped `-main` would exit 0 silently, so I tested the guard. It runs `-main` for relative, `./`, absolute, `..`, other-cwd and `bb -m` invocations. Callers use `bb checks/variational_free_energy_witness.clj …` (`wm_workspace_gate.clj:318,463,465,467`). The registry fragment's `:entrypoint "-main"` is metadata read by `contract_lint`.

## Gates rerun (`review-claude-3/`, run serially; no other Lean process active)

| Gate | Result |
|---|---|
| clj-kondo (wrapper + test) | 0 errors / 0 warnings |
| check-parens | OK, exit 0 |
| focused `variational_free_energy_witness_control_test.clj` | 4 tests / 20 assertions, 0 failures |
| real `positive` | exit 0, PASS |
| real `--negative-value` | exit 0, perturbed F rejected |
| real `--negative-type` | exit 0, type rejected |
| real `--negative-weakened-positive` | exit 0, `:expected-source-drift-detected`: baseline all-true, `failures []`; mutation `pass? false`, `failures [:positive-source-drift]`, elaboration and toolchain still matching |
| real `--unrelated-positive-edit` | exit 0, declaration basis stable |

No `lake build` and no cache regeneration. The only temp file matching the Lean patterns in /tmp predates this work (2026-09-10).

## Reviewer adversarial controls (`review_adversarial_test.clj`, `review-adversarial.log`)

These use the real retained receipt, fixture, source slices and shared validator. Only the toolchain, elaboration and Lean process boundaries are stubbed; every case in which the mutation must not run throws if it does. 5 tests / 11 assertions, 0 failures.

- **Baseline toolchain drift** (live `:lean-version` differs): exit 1, `:baseline-failed`, mutation never run.
- **Baseline elaboration or axiom drift** (live axioms add `"sorryAx"`): exit 1, `:baseline-failed`.
- **Fixture value edited in memory** (`:expected-variational-F 2`): exit 1, `:baseline-failed`.
- **Mutation report** `[:elaboration-or-axiom-drift :positive-source-drift]`: exit 2, `:unexpected-mutation-result`. Additional failures do not count as specific detection, even with the intended one present.
- **The mutation string** occurs exactly once in the retained source and replaces the `norm_num` proof with `rfl`.

The author's controls cover the rest: stale receipt source hash, stale fixture hash, current-source drift via in-memory read, unchanged mutation, a non-elaborating baseline or mutation, `[:fixture-or-adapter-drift]`, drift plus toolchain drift, and a passing override.

A first run of my probe failed at load time on a var named `source` shadowing `clojure.repl/source` (recorded in `exits.txt`). I renamed it and reran; that was my script's fault, not the subject's.

## Findings

None blocking. One limit, noted rather than counted as a defect: specificity is judged by the validator's failure *categories*. `:positive-source-drift` does not say which declaration drifted. The mutation is a single fixed text replacement verified to change the pinned slice, so this is sufficient for this control, but it would not tell two different weakenings in the same file apart.

## Limits

- The review is at the bounded wrapper/test/receipt scope.
- No source, Lean, cache, receipt, fixture, validator, registry, known-stale, WM-09, DAG/checklist, serving or publication changes, and no dispatch.
