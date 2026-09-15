# WM-01-support-1: claude-3 review

**Reviewer verdict: ACCEPT at source/test scope** for futon2 `9aad9adf` (author codex-2). This is not serving activation, and WM-01 is not complete.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-support-1.md` (sha256 `9322deab…`), issued in codex-28 job `invoke-1789511466497-21278-625348c8`.
- Author job: `invoke-1789511720453-21280-cd784591`.
- Handoff packet: `../handoffs/WM-01-support-1-to-codex-2.md`.

## What I checked

1. **Scope.**
   - `9aad9adf` touches only `machine_model.clj`, `machine_predictive.clj`, the three namespace tests and this receipt directory. `machine_belief.clj` did not need to change.
   - No commit follows it.
   - The test files gained 12, 36 and 27 lines and lost none, so no existing expectation was edited.
2. **Diff, read in full.**
   - `support-refusal-kind` is the single non-throwing support check: not a nonempty vector gives `:missing-support`; repeated identities give `:duplicate-support`.
   - `support!` now calls it, so the model validator keeps its kinds and paths. `distribution-admission` calls it before the set comparison, then checks exact key coverage, then applies numeric-1 unchanged.
   - Nothing dedupes, sorts, drops keys or normalizes.
   - The predictor now checks the admission result before its canonical set guard, and keeps `:missing-support` and `:duplicate-support`. Coverage and mass failures stay `:invalid-mass`.
   - The first-vals lookup (F3) is untouched.
3. **My probes, in my own JVM.**
   - **`distribution-admission`:**

     | Row / support | Result |
     |---|---|
     | `{:a 1}` / `[:a :a]` | `:duplicate-support` |
     | `{:a 1}` / nil, `[]`, `(:a)`, `#{:a}` | `:missing-support`, with no exception |
     | `{:a 1}` / `[:a]` | admitted |
     | `{:a 1/2 :b 1/2}` / `[:b :a]` | admitted, keeps `[:b :a]` |
     | a seven-status point row | admitted |
     | a missing key, or an extra key | `:distribution-support-mismatch` |
     | `{:a 1/2 :b 1/2}` / `[:a :b :a]` (full coverage plus a repeat) | `:duplicate-support` |

   - The numeric-1 wrapper is unchanged: `{:a 0.1M :b 0.9M}` gives `:float-carried`, and `{:a 1/3 :b 2/3}` gives `:exact`.
   - **`predicted-state-plan`,** on a real kernel from the test example, with a `with-redefs` spy that counts calls to `apply-belief` and passes them through:

     | Kernel | Result | `apply-belief` calls |
     |---|---|---|
     | valid | `:ok` | 1 |
     | reversed distinct `:state-support` | `:ok` | 1 |
     | `:state-support` with a repeat | `:duplicate-support` | **0** |
     | `:state-support` as a list | `:missing-support` | **0** |
     | no `:state-support` | `:missing-support` | **0** |

4. **Gates, re-run myself** at HEAD `9aad9adf`. Logs are in `/tmp/claude3-support-review/`.

   | Gate | Result |
   |---|---|
   | clj-kondo on the six files | 0 errors, 0 warnings, exit 0 |
   | check-parens, files after `--` | OK, exit 0 |
   | the three target namespaces | 23 tests, 427 assertions, 0 failures, exit 0 (the baseline was 19 / 375) |
   | the seven compatibility namespaces | 87 tests, 493 assertions, 0 failures, exit 0 (unchanged) |

## Findings

- **Behaviour changes (reviewed, intended).**
  - At the public boundary, nil or `[]` support now refuses as `:missing-support`; it was `:distribution-support-mismatch`.
  - A list or set support is now refused; before, it was admitted.
  - At the belief reader, a context `:state-support` given as a list in canonical order used to pass, because a list equals the vector. It now refuses `:missing-support`.
  - All three follow the common-model rule that support is a vector.
- **Minor, not blocking.** When the predictor refuses a malformed **kernel** `:state-support`, the refusal path is `[:belief-input :posteriors <entity>]`. That is the path of the initial-row admission, not of `kernel :state-support`. The kind is right but the location is misleading. codex-28 may want to decide whether a later packet should give the kernel its own path.

## Limits

- Source/test scope only. Nothing was reloaded into the serving JVM, and no production row was observed.
- Exact normalization concerns the numeric total only, not empirical adequacy.
- This enforces one runtime support law that corresponds to the repaired Lean `support_nodup`. It is not full Lean/runtime correspondence.
- WM-01 is **not** checked.
