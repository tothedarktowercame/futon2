# WM-09-predecessor-history-1 K9 correction: claude-3 review of 9d7ab9a5

**Reviewer verdict: ACCEPT source and test correctness of futon2 `9d7ab9a5`** (implementation `96d5b5e5`) against p4ng `6882a3a` (K1–K9), for the bounded predecessor-discovery scope. Both blocking findings from review `58e17030` (on `8d5b569d`) are fixed:

- identical copies at different paths now refuse only as target-relative ambiguity;
- regular files named `attempt-*` no longer count as hidden attempts.

One open contract question (Q1) needs a coordinator ruling. No retained record triggers it today.

**This is not operational readiness.** On the retained roots, receipt-mode discovery still refuses every target because of B2 (`wm-outer-loop-43-v1/attempt-053`) and the K8-b differing-identity collisions for archived `wm-outer-loop-40-v1` attempts 002–024. Both are recorded under AUTH-history-disposition, and no recovery is claimed.

Requested by codex-28 (bell `invoke-1789579093163-21621-8938730c`). The later commit `276a8dc1` (K9R3), which changes the same source and test again, was **not** reviewed. This acceptance does not transfer to it.

## Scope and pins

- **Diff.** `96d5b5e5` changes three places in `receipt_construction.clj`:
  - `auxiliary!` refuses a name starting with `attempt-` only when the entry is a directory;
  - `distinct-history-identities!` refuses only differing checkpoint sets (`:history-identity-collision`) and returns the rows;
  - the equal-time ambiguity refusal in `previous!` now carries the requested target, both paths, the identity/hash records and `:identical-checkpoint-sets?`.

  The test changes replace the global `"M-fresh"` assertion with target-relative cases and add 25 regular `attempt-NNN.edn` files to the positive coverage control. `9d7ab9a5` adds only receipt files.
- **Pins.** `K9R2-SOURCE-PINS.json` matches: source `275f120e…`, test `a7a24cc8…`. The nine `7e738d53` diagnostic files are unchanged.
- **Callers.** The caller and classifier files are unchanged since `fb849b22`.
- **How checks were run.** Every check used `git show 9d7ab9a5:` copies first on the classpath, in short-lived JVMs.

## Gates rerun (`review-k9r2-claude-3/`)

- **receipt-construction-test:** 25 tests / 305 assertions, 0 failures. Matches the author.
- **Focused callers:** 2 tests / 16 assertions, 0 failures.
- **Full `interpretation-job-test` namespace:** 14 tests / 467 assertions, 0 failures, exit 0.
- **Author's hermetic classification script:** typed discovery refusal → `:interpretation/invalid-receipt` → `:environmental-hold`; injected NPE → `:untyped-failure` → `:machine-failure`.
- **Negative control:** the `ae721644` source with the `9d7ab9a5` tests gives 1 failure and 2 errors, so the new controls reject the over-broad version.
- **Stores.** The production stores held 465 files before and after all runs.
- **Lint.** clj-kondo 0/0 and check-parens OK on the reviewer probe.

## Reviewer controls (`k9r2-probe.clj`, `.out`)

These use temporary stores and the author's fixtures. The requested target is `"M-history"`.

| Case | Expectation | Result |
|---|---|---|
| C1: identical live+archive copy of unrelated `M-other`, plus valid matching predecessor | carry valid; both copies excluded | carried `:valid`; 2 `:producer-recorded-different-target` exclusions ✓ |
| C2: same copy, fresh target | initial cascade; both excluded | `:first-attempt-no-admissions`; 2 exclusions ✓ |
| C3: identical copy is the latest matching history | refuse with copy evidence and both paths | `:ambiguous-previous-construction`, `:identical-checkpoint-sets? true`, 2 paths ✓ |
| C4: same identity, differing content, unrelated target | global structural refusal | `:history-identity-collision`, `identical? false` ✓ |
| C6: auxiliary subtree with regular file `attempt-013.edn` | covered and excluded | `:complete-no-attempt-no-close-coverage` ✓ |
| C8: attempt `evidence/attempt-identity.edn` (regular file) | coverage passes | initial cascade ✓ |
| C9: older identical matching pair, then a newer valid predecessor | newer valid carried; pair not latest | carried `:newer` ✓ |
| C10: identical matching pair closed **after** the action time, older valid predecessor | future pair ignored; older valid carried | carried `:older` ✓ |
| C11: identical copy of a **manifest-bound** not-reached attempt | (Q1) | refuses `:non-construction-manifest-binding-missing` at the copy |
| C12: manifest-bound unrelated construction, alone vs with identical copy | (Q1) | alone: excluded ✓; with copy: refuses `:relevance-manifest-binding-missing` at the copy |

The author's controls for a hidden attempt directory, a hidden `007-closed.edn`, an unreadable subtree and links/escapes remain negative in the namespace.

## Open question for the coordinator

**Q1. An identical copy of a manifest-bound record refuses globally.**
- A close manifest binds its checkpoint sources by absolute `:source-path`. A byte-identical copy at another path carries the same manifest, so its own checkpoint files are not bound. Both `relevance-evidence!` and `non-construction!` then refuse the copy before any target is compared (C11, C12), which blocks every target.
- That fits K5/K9 as written ("broken present identity-integrity binding refuses"). But it means the owner's control "identical pair + other target ⇒ successful unrelated exclusions with both paths" holds only for records without a manifest.
- The retained identical pair (`wm-outer-loop-40-v1/attempt-001`, July) has no manifest, so no retained record triggers this. A future archive copy of any modern (manifest-bound) attempt would.
- **Ruling needed:** either
  - (a) a manifest whose entries match the copy's bytes by SHA-256 but name the original paths counts as a *contradicted* binding (keep the refusal); or
  - (b) identical-copy evidence may let the copy rely on its twin's verified binding for relevance exclusion only.

  I don't treat this as blocking, because the current contract text supports the refusal.

## Retained-data consequence under this successor

- Archived and live `attempt-001` are now target-relative: ambiguity for `:sorry/pudding-g1-arrow-witness-binding` only.
- The archive's `derived-projections/morning-brief-items/attempt-NNN.edn` files no longer refuse.
- The global blocks that remain are the differing sets for attempts 002–024 (author's bounded diagnostic confirms 002 and 024 at this source; my earlier census counted 23) and B2.

## Limits

- The review covers `9d7ab9a5` exactly; `276a8dc1` and any uncommitted edits were not reviewed.
- No production reads in place and no store writes. Classification was checked through the author's hermetic script at the pinned caller.
- No source edits, recovery, serving, successor, checklist or DAG changes, and no dispatch.
