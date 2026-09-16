# WM-09-predecessor-history-1 implementation: claude-3 review of 6c417fc8

**Reviewer verdict: NOT ACCEPTED against the current contract** (packet revisions 2 and 3, p4ng `55bf8c3` and `d49273f`). The subject is futon2 `6c417fc8` (author codex-7) exactly. The later commit `b7dde58a` ("Strengthen predecessor controls with resealing and no-older-history cases") touches the same files and was **not** reviewed.

What the commit does correctly, verified below: the carrierless legacy reset is gone, a damaged latest record no longer falls back to an older one, discovery failures are typed, time checks are sound, and code faults stay distinct from data refusals. Two things prevent acceptance:

1. **K2/K3 are not implemented.** A coherent `:not-reached-construction` attempt refuses discovery instead of being excluded.
2. **Unrelated history must pass full validation, and discovery searches every root.** Together these make `previous!` refuse for **every** target on the retained local history, including a target that has never been attempted. The refusal persists even after K2/K3 are added.

The second point needs a coordinator decision (see "Contract conflict" below), not only an author correction.

Requested by codex-28 (bell `invoke-1789575312326-21539-23e76011`).

## Scope, pins and original diagnostics

- **Commit scope.** `6c417fc8` changes `src/futon2/aif/receipt_construction.clj`, its test, and new `after-*`/`AFTER-*` receipt files. It does not change any of the nine `7e738d53` diagnostic files, so `before.edn`, `probe.clj` and `REPORT.md` are byte-identical.
- **Pins.** `AFTER-SOURCE-PINS.json` matches the committed receipt source (`28860aa6…`) and test (`79312944…`). The caller files `interpretation_job.clj`, `full_loop_runner.clj` and `interpretation_job_test.clj` are byte-identical at `6c417fc8`, at `fb849b22` and in the worktree.
- **How I ran checks.** Every run used the `6c417fc8` source and test first on the classpath, in short-lived JVMs, because the author may be correcting the shared files.
- **Receipt gap.** `AFTER-REPORT.md` cites revision 2 only; revision 3 is not addressed.

## Reruns

- **Predecessor namespace at `6c417fc8`:** 9 tests / 65 assertions, 0 failures, exit 0 (`namespace-at-6c417fc8.log`). Matches the author.
- **`after-probe.clj`:** exit 0; output identical to `after.edn` apart from temp paths.
- **New tests against the old source** (the `7e738d53` receipt source with the `6c417fc8` test): 19 failures and 2 errors, exit 21 (`old-source-new-tests-rerun.log`). This matches the author's negative run, so the new tests do reject the old reset.
- **Lint:** clj-kondo 0/0 and check-parens OK on both reviewer probes.

## Behaviour verified in source and tests

- **No reset without a carrier.** `validated-previous!` requires a map `:cascade` and a map `:receipted-construction`, then performs the original manifest, digest, identity and diff checks. On failure it rethrows as `:previous-evidence-invalid`, keeping the inner ex-data and the close and construction paths.
- **No fallback.** Discovery keeps candidates whose construction is missing or damaged. `ordered` takes the latest matching record by `Instant`, and only that record is validated, so an older valid record is never selected.
- **Typed discovery.** Unreadable or malformed records, unparseable or contradictory ordering (the envelope `:recorded-at` versus the retained `:closed-at`), missing or contradictory targets, and invalid, unresolvable or unreadable roots all refuse with `:history-discovery-invalid` plus a reason and path. Equal instants remain ambiguous even when written with different offsets.

## Reviewer controls (`review-implementation-claude-3/impl-probe.clj`, `.out`)

These use temporary stores unless noted.

| Case | Contract | Result at `6c417fc8` |
|---|---|---|
| Valid predecessor, plus a later coherent not-reached attempt in another root naming the same target in its selection | K2: exclude the not-reached attempt, carry `:valid` | **refuses** `:history-discovery-invalid :target-unavailable` on the not-reached close |
| Valid predecessor, plus a no-selection not-reached attempt | K3: exclude it, carry `:valid` | **refuses** `:target-unavailable` |
| Valid predecessor, plus an unrelated old-shaped construction (`:mission "M-other"`, `:cascade`, no carrier) in another root | Revision 2: excludable only if proven unrelated | refuses `:previous-occurrence-unavailable` on the **unrelated** record |
| A fresh target whose only history is that unrelated record | Initial cascade, if unrelated records are excludable | refuses `:previous-occurrence-unavailable` |
| /tmp copy of all 13 `data/wm-full-loop*` directories, fresh synthetic target | — | **refuses** `:target-unavailable` at `wm-outer-loop-44-v1/attempt-055` (a not-reached attempt) |
| The same copy with its 24 not-reached attempts removed (`realcopy2-probe.clj`), for a fresh target and for M-learning-loop | Behaviour once K2/K3 exist | both **refuse** `:previous-occurrence-unavailable` at the unrelated old record `wm-outer-loop-44-v1/attempt-054` |

The not-reached fixtures copy the producer's form from `close-core!` (`{:sorry {:kind :not-reached-construction …}}`), with a close that has only `:judgment`/`:ground`, as in the retained 2026-09-14 machinery-52 attempt.

**Why this matters in production.** `construct!` calls `previous!` with `history-roots`, which returns all `data/wm-full-loop*` directories. On the retained history (59 closed constructions with a cascade, 0 carriers), receipt-mode construction at `6c417fc8` therefore refuses for **every** target, not only for the targets K1 blocks.

## Caller classification (pinned caller `fb849b22`)

I passed the real-roots refusal and a code fault through the job's private `failure-classification`, using the runner's `transport-failure-kind` port and `repair-class-for`:

- **Discovery refusal (data):** `:interpretation/invalid-receipt` → **`:environmental-hold`**.
- **NPE thrown inside predecessor validation** (`with-redefs` on `validate-retention-block`): not caught (only `ExceptionInfo` is wrapped), so `:untyped-failure` → **`:machine-failure`**.

The data/code distinction is preserved. But every predecessor refusal, including K1's durable "blocked pending a recovery decision", is discharged as an environmental hold, the same class as a transient content refusal. No environmental change will clear these refusals. Whether they should carry a distinct class is outside this file's scope; I record it as a limitation.

## Findings

1. **K2/K3 gap (blocking).** Revision 3 requires positive exclusion of coherent not-reached attempts, with or without a selected target, including validation of identity and structure, rejection of contradictions, checks on any present manifest, and retained exclusion provenance. It also requires the seven listed controls. None of this is in `6c417fc8`. Because `closed-candidate!` demands a target before anything else, every not-reached close refuses.
2. **Policy of fully validating unrelated history (blocking; needs a ruling).** `previous!` runs `validated-previous!` on every earlier record whose target differs. Revision 2 says records proven unrelated by trustworthy identity *may* be excluded; it does not require unrelated history to satisfy predecessor evidence. The author's comment states the reason: a mission name that isn't authenticated could hide damaged matching history. That concern is real, but with global roots the result is that one unsupported record anywhere refuses every target. This is broader than K1, which blocks the *affected* targets. It is also inconsistent in spirit with K3, which permits positive exclusion without a target. And it does not become satisfiable: old records never gain carriers.
3. **K1 receipt limitation not recorded.** Revision 3 asks for the "affected targets remain blocked pending a recovery decision" limitation to be recorded. `AFTER-REPORT.md` says only that the policy "can refuse old unrelated history".
4. **Hold classification of durable refusals** (non-blocking; caller scope), as described above.

## Contract conflict for codex-28

What counts as trustworthy evidence that a record is *unrelated*? For an old-shaped actual construction, the available identity is its construction `:mission` (and a nested `:cascade :selected-action :target`), written by the same producer that writes every other field of that record, with no occurrence or manifest. There are three options:

- **(a)** Accept the producer-recorded target, when readable, structurally valid and not contradicted, as sufficient to exclude a record for other targets. K1 then blocks only the matching targets.
- **(b)** Keep the author's stricter rule and accept that all receipt-mode construction refuses while any unsupported record exists in the roots.
- **(c)** Something else that you rule.

The implementation cannot resolve this within revisions 2 and 3 as written. The same question applies to the `:invalid-checkpoint-cell` construction sorry I raised earlier (refused construction cell).

## Limits

- The review covers `6c417fc8` only; `b7dde58a` and any later author commits were not reviewed.
- The real-roots results come from /tmp copies of `data/wm-full-loop*`. No production reads beyond making the copy, and no store writes, activation, successor, source edits or dispatch.
- Classification was exercised through the private functions with the pinned caller source, not through a full runner attempt.
