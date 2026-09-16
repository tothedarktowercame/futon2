# WM-08-09-review-1: claude-3 review

Assessment author codex-3, commit `a5923dce`. **Per-fix reviewer verdicts:**

| Fix | codex-3 | claude-3 |
|---|---|---|
| `c4130ba5` interpretation failure → environmental hold | ACCEPT | **LIMITED ACCEPTANCE** (downgraded) |
| `706ca3e8` legacy predecessor reset | REJECT | **CONCUR: REJECT** |
| `983d4c49` recorded acting order | LIMITED ACCEPTANCE | **CONCUR: LIMITED ACCEPTANCE** |

Accepting a historical diff is not deployment. No serving, click or checkbox claim is made.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-08-09-review-1.md` (sha256 `0ff615f9…`), elected by codex-28 in `invoke-1789562930424-21302-dc2e5d01`.
- Author job: `invoke-1789563053706-21304-309de600`. Handoff: `../handoffs/WM-08-09-review-1-to-codex-3.md` (sha256 `e26450f9…`).

## What I checked

1. **Scope.** `a5923dce` writes only this receipt directory; no `src/` or `test/` edits.
2. **Diffs read myself:** `c4130ba5`, `706ca3e8`, `983d4c49`. The `c4130ba5` hunk is present verbatim in the current runner.
3. **Attribution — and a correction to my own handoff.** I told codex-3 the runner had one later commit (`cae5f6d9`); I took that range from `983d4c49` instead of `c4130ba5`. There are two: `24dc6068` and `cae5f6d9`. codex-3 found and corrected this. `cae5f6d9` touches interpretation code in one measurement-guard line only, not the classifier.
4. **Tests re-run in isolated processes:** `interpretation-job-test` 10 tests / 184 assertions, `receipt-construction-test` 7 / 31, both exit 0.
5. **codex-3's probe re-run exactly as recorded**, output to `/tmp` so author evidence was not overwritten: exit 0, 3 tests / 48 assertions. Outcomes reproduced: `:legacy-predecessor-no-ruled-admissions`, `:first-attempt-no-admissions`, `:carried-from-previous-occurrence`, `:previous-manifest-source-mismatch`, `:previous-occurrence-mismatch`. Probe lint 0/0, parens OK.

## `c4130ba5` — LIMITED ACCEPTANCE (downgraded from ACCEPT)

**What holds.** Genuine interpretation content failures (invalid receipt, no relevant pattern, nothing firing, changed source, genesis required) are typed failures that open an environmental hold, with no author or build dispatch and no successful construction. codex-3's evidence for this is sound.

**What does not.** The classifier maps the **entire `interpretation` keyword namespace** to `:environmental-hold`. And the interpretation job's `catch Exception` (`interpretation_job.clj`) assigns **`:interpretation/invalid-receipt` to any exception that matches none of five specific patterns** — including a typed machine failure such as `:build-failed`, since only `:agent-budget-expired` is carried through. It rethrows with that `:failure-kind`, which the runner's `explicit-failure-kind` takes from the outermost cause. So a real code fault inside the job no longer opens a machine repair.

**Demonstrated, not only read.** My control ran the real receipt-mode path via `interpretation-job-test/run-case` inside `hermetic-repair-fixture/with-hermetic-stores`:

| Injected into `receipt-construction/construct!` | Recorded failure kind | Repair class |
|---|---|---|
| `(ex-info … {:failure-kind :build-failed})` | `:interpretation/invalid-receipt` | **`:environmental-hold`** |
| `NullPointerException` | `:interpretation/invalid-receipt` | **`:environmental-hold`** |

Production store file counts were unchanged across that run (465 before and after).

**Attribution.** The catch-all fallback already existed at `c4130ba5`, so from that commit any untyped fault in the interpretation job stops opening a machine repair. `24dc6068` later moved `construct!` inside the same `try`, which extends this to construction faults. The runner itself (comment near `full_loop_runner.clj:4830`) calls exactly this kind of downgrade of a typed machine failure to environmental hold a fail-open.

**Missing case:** an exception inside the interpretation job that is not a recognised interpretation content failure — typed machine failure or untyped — must still classify as `:machine-failure`. Two further kinds in the declared vocabulary also look like invariant breaches rather than content gaps and are routed to a hold: `:interpretation/job-already-dispatched` and `:interpretation/attempt-identity-mismatch`. I have not demonstrated a fault through those two; I record them as questions.

## `706ca3e8` — CONCUR: REJECT

`:legacy?` is only `(not (contains? judgment :receipted-construction))`, and that branch is taken **before** the `:else` branch that validates the close manifest. A modern construction that has lost that one key therefore resets as legacy without validation. There is no positive legacy marker (version or epoch). The committed tests cover a genuine old-shaped record and a mismatched occurrence, but not a deleted modern carrier or missing files. Missing construction or close files becoming "first attempt" predates this fix, as codex-3 notes.

## `983d4c49` — CONCUR: LIMITED ACCEPTANCE

Reuse of the validated predecessor's recorded order is sound: `previous!` validates occurrence, data root, epoch, start hash, manifest agreement and diff digest, and rejects unsealed byte edits and occurrence substitution. Not established: that the recorded order is what was actually enacted. No enactment record is consulted, so a digest-consistent wrong order is accepted and reused.

One detail I checked: reuse keys on `(identical? c (:cascade previous))`. In `cascade_policy/organise`, `after` is a fresh map literal, so the after arm is always re-simulated and only the before arm reuses the recorded order — the current caller passes the previous cascade straight through. Correct as wired; it would silently fall back to re-simulation if a future caller passed a copy.

## Process incident during this review

My first control called `run-case` from a `/tmp` probe **outside** the hermetic fixture. It wrote three ~103 MB trip reports into the production trip store (`futon2/data/wm-tripwires/trips`, 13:08–13:16Z) — one T10 loaded-code drift in the probe JVM and two T8 livelock trips computed over the real repair store but triggered by a non-serving process. No repair obligations were written and nothing referenced the trip ids. I moved (not deleted) them to `/home/joe/quarantine/claude3-probe-trips-20260916/` with hashes, provenance and a restore command. The store is back to 275 trip files. The demonstration above is from the corrected, hermetic run.

## Remaining WM-08 / WM-09 obligations

codex-3's list stands, plus the new item:
- **WM-08 C1–C3:** ordinary problem-derived query/retrieval, complete pinned context, actual source reading and task-bound interpretation in ordinary use.
- **WM-08 C4:** independent F2 expectations and independent F4 designation; the nonvacuity decision remains unresolved.
- **WM-08 C5–C6:** opaque-interface refinement, running finder evidence, downstream useful use, retained empty/unhelpful search findings; serving activation and an ordinary receipt-mode run.
- **WM-09 C1–C3:** a trustworthy before-state and admission history boundary (the `706ca3e8` reset cases), all four independently attributed inputs, authored-relation preservation, nonvacuous no-bootstrap checks.
- **WM-09 C4–C5:** independently grounded same-occurrence acting consequences, exact delivered policy identity, and selection/interpretation/enactment distinctions.
- **WM-09 C6:** connect the replacement to the legacy `organise` obligation, plus serving activation, an ordinary constructed family and a qualifying click receipt.
- **New, from `c4130ba5`:** machine faults inside the interpretation job must keep machine-failure classification.

No repair was attempted and no successor was chosen.
