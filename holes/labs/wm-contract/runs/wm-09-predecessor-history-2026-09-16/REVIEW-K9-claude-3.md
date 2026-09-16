# WM-09-predecessor-history-1 K7–K9 successor: claude-3 review of 8d5b569d

**Reviewer verdict: NOT ACCEPTED — return for two bounded corrections.** The subject is futon2 `8d5b569d` (source implementation `ae721644`), reviewed against p4ng `6882a3a` (K1–K9).

The K7 targetless exclusion, archive traversal, differing-identity collision refusal, overlap deduplication and link/escape guards all work. Two discovery rules are broader than K9 and turn records that should be excluded into structural refusals for **every** target:

- **F1.** A byte-identical copy at a distinct path refuses before relevance is checked. K9 keeps an ambiguity refusal only for the target the copy matches.
- **F2.** The auxiliary-coverage scan refuses any *entry* named `attempt-*`, including regular files. K9 tests only `attempt-*` *directories* and `007-closed.edn`. The retained archive contains exactly such files.

This verdict does not rely on the other retained records that already block discovery everywhere (B2, the K8-b collisions). Each finding is judged on hermetic fixtures where nothing else blocks.

Requested by codex-28 (bell `invoke-1789578912290-21615-78a8ae16`).

## Scope, pins, diagnostics

- **Commits.** `615a9987` (K7), `858aa7f7` (K8), `135b931a` (K8 archive diagnostic), `ae721644` (K9 source/test) and `8d5b569d` (K9 archive diagnostic) change only `receipt_construction.clj`, its test and receipt files.
- **Pins.** `K9-SOURCE-PINS.json` matches: source `9a16668d…`, test `227f700b…`, packet `b355dd8e…` (= p4ng `6882a3a`).
- **Original diagnostics.** All nine `7e738d53` files are byte-identical.
- **Callers.** The caller and classifier files (`full_loop_runner`, `interpretation_job` and its test, `full_loop_cohort`, `close_retention`, `evidence_manifest`, `forward_model`, `interpretation_evidence`) are unchanged since `fb849b22`.
- **How checks were run.** The shared worktree already held further uncommitted author edits to the receipt source and test at review time, so every check used `git show 8d5b569d:` copies first on the classpath, in short-lived JVMs.

## Gates rerun (`review-k9-claude-3/`)

- **receipt-construction-test:** 25 tests / 301 assertions, 0 failures. Matches the author.
- **Focused callers:** 2 tests / 16 assertions, 0 failures.
- **Author's hermetic classification script:** typed discovery refusal → `:interpretation/invalid-receipt` → `:environmental-hold`; injected NPE → `:untyped-failure` → `:machine-failure`. Exit 0.
- **Negative control:** the `858aa7f7` source with the `8d5b569d` tests gives 10 failures and 1 error out of 25 tests, so the K9 controls reject the pre-K9 implementation.
- **Full `interpretation-job-test` namespace:** 14 tests / 467 assertions, 0 failures, exit 0.
- **Stores and temp directories.** The production stores held 465 files before and after, and no temporary directories remain.
- **Lint.** clj-kondo 0/0 and check-parens OK on the reviewer probe.

## Reviewer controls (`k9-probe.clj`, `.out`)

These use temporary stores and the author's producer-shaped fixtures. The requested target is `"M-history"` unless stated.

| Case | K9 expectation | Result at `8d5b569d` |
|---|---|---|
| C1: identical live + archive copy of an **unrelated** (`M-other`) attempt, plus a valid matching predecessor | Copies are not merged; unrelated for this target; carry the valid predecessor | **refuses `:distinct-path-identical-history`** |
| C2: the same identical unrelated copy, fresh target | Initial cascade with both copies excluded | **refuses `:distinct-path-identical-history`** |
| C3: identical copy of a **matching** attempt | Unresolved refusal, with both paths and copy evidence | refuses `:distinct-path-identical-history` ✓ |
| C4: same identity, different content, unrelated target | K8-b collision refusal | refuses `:history-identity-collision` ✓ |
| C5: overlapping roots (parent and archive child) over one attempt | Deduplicated by canonical path | initial cascade; one exclusion ✓ |
| C6: archive-group auxiliary `derived-projections/morning-brief-items/attempt-013.edn` (**regular file**) | Excluded after complete coverage | **refuses `:unsupported-history-layout`** at that file |
| C7: same subtree, file named `item-013.edn` | Excluded | `:complete-no-attempt-no-close-coverage` ✓ |
| C8: attempt `evidence/` holding a regular file `attempt-identity.edn` | Coverage passes (no directory, no close) | **refuses `:unsupported-history-layout`** |

## Findings

**F1. Identical distinct-path copies are refused for every target (blocking).**
- `distinct-history-identities!` runs over every discovered close before `closed-candidate!` and relevance checks. It refuses any identity group of size > 1, including identical checkpoint sets, with `:distinct-path-identical-history`. The author's test `distinct-path-checkpoint-copies-remain-unresolved` asserts this for requested target `"M-fresh"`, so the broadening is pinned in the tests.
- K9 on K8-a: "retain ambiguity refusal for distinct physical records … not silently merged into one occurrence … It supersedes any reading of K8 that all equal-time ambiguity must be eliminated". That text keeps the refusal where the copies would be the ambiguous latest matching history. It does not make an identical copy a structural block for unrelated targets. K9's structural refusal is stated for K8-b (differing sets) only.
- **Correction:** identical sets at distinct paths should go through the normal per-record discovery path:
  - excluded (each path with its own provenance, plus identical-copy evidence) when the target differs;
  - refused as unresolved ambiguity, with both paths and copy evidence, when they are the latest matching history.

  Differing sets (K8-b) stay a structural refusal. Replace the `"M-fresh"` assertion with controls C1–C3.
- **Retained data:** live and archived `wm-outer-loop-40-v1/attempt-001` (target `:sorry/pudding-g1-arrow-witness-binding`) is this case. Under K9 it should block only that target.

**F2. Regular files named `attempt-*` are treated as hidden attempts (blocking).**
- In `auxiliary!`, `scan!` refuses whenever `(.getName entry)` starts with `"attempt-"` or equals `"007-closed.edn"`, and it applies this to files and directories alike. K9: "no attempt-* directory and no 007-closed.edn at any depth"; "Regular ancillary files do not become attempt directories".
- The retained archive group has `derived-projections/morning-brief-items/attempt-002.edn … attempt-024.edn`: 23 regular files. The whole archive root therefore refuses discovery for every target (C6).
- The same rule covers the recognised attempts' `evidence/` subdirectories (C8). No retained evidence file currently starts with `attempt-`, but nothing in the producer contract guarantees that.
- **Correction:** refuse only on a *directory* named `attempt-*` or a *file* named `007-closed.edn`; keep the unreadable, link and escape refusals. Add a control with regular files named `attempt-NNN.edn`.
- **Diagnostic limit:** `K9-ARCHIVE-REPORT.md` copied only the 27 attempt directories, not `derived-projections/`, so its bounded diagnostic could not show F2. As the coordinator noted, it is not an all-root census. With F1 corrected, its `attempt-001` result would also change from structural refusal to target-relative handling.

## Verified correct

- **K7:**
  - `:no-op` is accepted without `:target`/`:target-class`.
  - `:learn-action-class` is accepted only with a `:target-class` in `forward-model/action-types` minus `:no-op`/`:learn-action-class`.
  - A present `:selected-mission` must equal the rendered label.
  - For targetless actions, `:selected-mission` is dropped from target evidence and no target is inferred.
  - Malformed actions and non-targetless types still need a real target.
- **K8:**
  - root/cohort/attempt and root/archives/group/cohort/attempt layouts are traversed, with searched-layout provenance;
  - a close outside an attempt refuses `:misplaced-closed-history`;
  - symlinks and root escapes refuse;
  - recognised attempts without a close are not predecessors;
  - canonical-path deduplication removes traversal overlap only (C5).
- **K8-b:** different checkpoint sets under one identity refuse `:history-identity-collision` with both paths and per-slot SHA-256 (C4 and the author's archive diagnostic for attempts 002/024).
- **Typed data vs code fault:** unchanged and correct at the pinned caller.

## Operational note (not a finding against source)

B2 (`wm-outer-loop-43-v1/attempt-053`) and the K8-b collisions for archived attempts 002–024 still block receipt-mode discovery for every target on the retained roots. That is correct under K7/K9 and recorded under AUTH-history-disposition. Correcting F1 and F2 removes two *unjustified* global blocks; it does not make the roots operationally usable.

## Limits

- The review covers `8d5b569d` exactly; uncommitted worktree edits were not reviewed.
- No production reads in place and no store writes. Classification was checked with the author's hermetic script at the pinned caller.
- No source edits, recovery, serving, successor, checklist or DAG changes, and no dispatch.
