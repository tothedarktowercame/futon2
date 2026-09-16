# WM-09-predecessor-history-1 final successor: claude-3 review of 276a8dc1

**Reviewer verdict: ACCEPT source and test correctness of futon2 `276a8dc1c31954dcc5125205e164ffa69c1846de`** against p4ng `6882a3a` (K1–K9), for the bounded predecessor-discovery scope. This is the exact acceptance subject. My acceptance of `9d7ab9a5` (`69c5139c`) is not carried over: I re-verified the full set of gates and controls at this commit.

**Not operational readiness.** On the retained roots, discovery still refuses every target because of B2 (`wm-outer-loop-43-v1/attempt-053`) and the K8-b differing-identity collisions for archived `wm-outer-loop-40-v1` attempts 002–024, recorded under AUTH-history-disposition. No recovery, serving or checklist state is claimed.

Open items that don't block: Q1 from the previous review (still unruled) and N1 below.

Requested by codex-28 (bell `invoke-1789579241651-21627-d62e3acd`).

## Delta from 9d7ab9a5

- **Source.** A single change in `previous!`: when the two latest earlier matching records close at the same instant and their checkpoint sets are identical, the refusal kind is `:distinct-path-identical-history`. Otherwise it stays `:ambiguous-previous-construction`. Payload unchanged: requested target, sorted paths, identity/hash records, `:identical-checkpoint-sets?`.
- **Tests.** The same-target identical-copy case now expects `:distinct-path-identical-history` and checks both close paths. The unrelated case checks that both paths appear in exclusion provenance. A new test, `identical-copies-respect-the-latest-earlier-boundary`, checks that older and future identical pairs do not block a unique later valid predecessor.

## Scope and pins

- **Pins.** `K9R3-SOURCE-PINS.json` matches: source `8594d8ba…`, test `9ac29300…`. HEAD and the worktree have the same receipt source and test.
- **Original diagnostics.** The nine `7e738d53` diagnostic files are unchanged.
- **Callers.** The caller and classifier files are unchanged since `fb849b22`.
- **How checks were run.** Every check used `git show 276a8dc1:` copies first on the classpath, in short-lived JVMs.

## Gates rerun (`review-k9r3-claude-3/`)

- **receipt-construction-test:** 26 tests / 313 assertions, 0 failures. Matches the author.
- **Focused callers:** 2 tests / 16 assertions, 0 failures.
- **Full `interpretation-job-test` namespace:** 14 tests / 467 assertions, 0 failures, exit 0.
- **Author's hermetic classification script:** typed discovery refusal → `:interpretation/invalid-receipt` → `:environmental-hold`; injected NPE → `:untyped-failure` → `:machine-failure`.
- **Negative controls with the `276a8dc1` tests:**
  - `ae721644` source: 1 failure, 3 errors, matching the author's `K9R3-NEGATIVE.json`;
  - `9d7ab9a5` source: 1 failure, so the delta's refusal-kind assertion is load-tested.
- **Stores.** The production stores held 465 files before and after.
- **Lint.** clj-kondo 0/0 and check-parens OK on both reviewer probes.

## Reviewer controls

**`k9r3-probe.clj`** (the same cases as the `9d7ab9a5` review, rerun at this commit):

| Case | Result at `276a8dc1` |
|---|---|
| C1: identical copy of unrelated attempt + valid predecessor | carried `:valid`; both copies excluded ✓ |
| C2: identical unrelated copy, fresh target | initial cascade; 2 exclusions ✓ |
| C3: identical matching copy is latest | **`:distinct-path-identical-history`**, identical true, 2 paths ✓ |
| C4: same identity, differing content | global `:history-identity-collision` ✓ |
| C6/C8: regular `attempt-*` files in auxiliary subtree / attempt `evidence/` | covered; initial cascade ✓ |
| C9: older identical matching pair, newer valid predecessor | carried `:newer` ✓ |
| C10: future identical matching pair, older valid predecessor | carried `:older` ✓ |
| C11/C12: identical copy of a manifest-bound record | refuses at the copy (`…manifest-binding-missing`); Q1 unchanged |

**`k9r3-delta-probe.clj`** (specific to the delta):

| Case | Result |
|---|---|
| D1: two distinct-identity matching attempts closing at the same instant | `:ambiguous-previous-construction`, identical false ✓ |
| D2: identical copy pair **plus** a third distinct matching attempt, all at the same instant | refuses `:ambiguous-previous-construction`, identical false, **2 of 3 paths** (N1) |

## Findings (non-blocking)

- **N1. Ambiguity provenance lists only two contenders.**
  - The refusal is built from `(take-last 2 ordered)`. With three or more matching records at the latest instant (D2), it names two of them. Depending on sort order, it can omit the identical-copy pair's evidence and report `:ambiguous-previous-construction` where one pair is an identical copy.
  - The outcome is still a refusal, so nothing is admitted wrongly. But the retained provenance is incomplete for the rare case of more than two equal-time contenders. Collecting every record that shares the latest `:closed-at` would complete it.
- **Q1 (from review `69c5139c`, still open).** An identical copy of a manifest-bound record refuses every target, because the copied manifest binds the original absolute paths. That is consistent with K5/K9 as written. No retained record triggers it (the `attempt-001` pair has no manifest). It needs a coordinator ruling before any modern-attempt archive copy exists.

## Limits

- The review covers `276a8dc1` exactly.
- The author's archive diagnostic stays pinned to `96d5b5e5` and was not rerun here. Under this source the identical `attempt-001` pair would report `:distinct-path-identical-history` for its own target only.
- No production reads in place and no store writes. Classification was checked with the author's hermetic script at the pinned caller.
- No source edits, recovery, serving, successor, checklist or DAG changes, and no dispatch.

## Supplement: classifier coverage for the identical-copy refusal

Requested by codex-28 (bell `invoke-1789579297854-21632-eaa84095`). The subject is still `276a8dc1`; there are no source changes. See `identical-copy-classification.clj` and its `.log`.

The refusal was produced by the real `previous!` on a hermetic live+archive identical copy of the latest matching attempt.

- **Its ex-data:**
  - keys `:construction/refusal :distinct-path-identical-history`, `:interpretation/refusal :interpretation/invalid-receipt`, `:files` (2 paths), `:records`, `:requested-target`, `:identical-checkpoint-sets? true`;
  - no `:failure-kind`, no `:outcome`, no cause.
- **Classifier key.** `:distinct-path-identical-history` is a diagnostic label only. The job classifier ignores `:construction/refusal`. It keys on `:interpretation/refusal :interpretation/invalid-receipt`, which is in the closed vocabulary, and does not consult transport because a refusal is present.
- **Direct classification** (job `failure-classification` with the runner's `transport-failure-kind`, then `repair-class-for`): `:interpretation/invalid-receipt` → `:environmental-hold`.
- **Through the real receipt-mode `run-case`**, inside both hermetic namespace fixtures, with that exact exception thrown from the `construct!` port:
  - runner failure kind `:interpretation/invalid-receipt`;
  - repair class `:environmental-hold`;
  - recorded interpretation failure `:interpretation/invalid-receipt`;
  - calls `["interpreter"]`, no legacy constructor calls.
- **Control.** An injected NPE on the same path gives `:untyped-failure` → `:machine-failure`, with recorded kind `:interpretation/machine-failure`.
- **Stores.** Production stores held 465 files before and after, and no temporary directory remains.

The hold class is routing only. It grants no authority to discharge, reset or recover the copied history: the refusal persists on every later discovery until a separately justified disposition resolves the copy.
