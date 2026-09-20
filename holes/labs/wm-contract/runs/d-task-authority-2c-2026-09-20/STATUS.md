> Landed: 328141ab, with matching postcommit warrants. See [EXECUTION.md](EXECUTION.md) and [ATTRIBUTION.md](ATTRIBUTION.md). The original draft report follows as history.

> Superseded gate status: full attribution is complete; see [ATTRIBUTION.md](ATTRIBUTION.md). The historical draft report below is retained.

# D task authority: draft held at the runner regression gate

The narrow scope ruling has been implemented as a draft, not landed source.
`draft-location.json` identifies the isolated worktree, baseline and source
hashes. `draft.patch` reproduces the tested tree. Canonical source edits were
restored only after matching every draft source hash in the worktree; unrelated
workspace files were left alone. No live actuation or shared-JVM reload occurred.

## Implemented and exercised

- Named `:d-predecessor-task-authority-v1`, certifying executed-with-artifacts
  only; E1/E2a/E2b are untouched. Minted occurrence chain retained; optional
  R6-domain ID retains `:candidate-to-minted-join :not-established`.
- Shared artifact and review checks extracted from the runner rather than
  duplicated. The runner keeps its API wrappers.
- Pre-dispatch declaration snapshots and source pins; explicit revision pair
  with `:before-evidence :not-measured`; task grain and declared-kernel authority.
- Agency prompt binding includes the digest of the minted dispatch record.
  The verifier independently fetches the exact author/reviewer jobs, checks
  that binding, verifies review-to-commit/repository identity, reruns the shared
  freshness check against Git, and replays the after-token observations.
- Immutable claim writer, exact minted-action-ID reader, trace context, final
  runner record, and D carry admission connected. Recovery and historical
  cases remain distinct typed refusals. Historical old receipts remain replayable.

The positive test uses real Git commits, real occurrence minting, real C3
observation, the producer, persistence and reader. External Agency reads are
fixture ports returning raw jobs; the production verifier itself is not
stubbed and receives no asserted admission flag. This is not evidence of a
successful live production execution.

Current reader coverage is C3/C4 revision-pair affirmation. Other readers and
other repositories are explicit unavailable measurements, never false facts.
No pre-measurement mapping is asserted. Admission requires an actual affirmative
result. This limitation is visible in the draft and needs review before landing.

The admitted D branch still consumes fresh initialization and explicitly records
`:conditioning-status :not-wired` / `:conditioning-consumption-not-wired`.
No active posterior or Q update is claimed. The scope of this draft is task
production, verification and admission, not completed live Bayes consumption.

## Gate results

Pre-commit targeted runs:

| Namespace | Tests | Assertions | Failures | Errors |
| --- | ---: | ---: | ---: | ---: |
| d-predecessor-task-authority | 5 | 31 | 0 | 0 |
| token-belief-predecessor | 3 | 25 | 0 | 0 |
| token-belief-carry | 4 | 30 | 0 | 0 |
| scoring-input-receipts | 3 | 45 | 0 | 0 |
| full-loop-runner | 180 | 966 | 44 | 1 |

Lint: zero errors/warnings. Parentheses: pass. Full commands, source hashes and
logs are retained. No implementation commit or post-commit warrant is claimed.
An earlier focused authority run passed 3 tests / 23 assertions before further
controls and integration were added; the table is the later complete draft run.

## Baseline diagnosis

Two failing vars were tested using the committed pre-change runner:
`terminal-recovery-job-transitions-to-machine-repair` and
`historical-verification-action-commits-without-author-dispatch`.
They reproduce **12 failures and one EDN reader error** (unknown `#object`).
The full 44 failures have not all been established as pre-existing.

The first baseline attempt correctly stopped at source drift: an old loaded
runner differed from the modified canonical checkout. The second isolated JVM
pinned the source authority path to the exact git-extracted baseline file.
The loaded-versus-authority byte equality check remained in force; it was not
replaced by a success flag. Commands and the baseline blob hash are retained
in `baseline-pinned-source.json`. No branch code entered the shared JVM.

The recovery fixtures expect automatic diversion from ordinary selection when
an open repair is present. The committed runner explicitly selects ordinarily
instead (`RULING-selection-precedence-2026-09-19.md`); the sampled historical
fixture likewise never selects its intended historical action. These fixtures
need a reviewed migration to explicit selected repair/historical actions, or
another declared validation boundary. Do not reinstate automatic diversion,
skip the failures, or label the draft warranted.

## Handoff

Request a separate fixture-migration decision before broadening this D change
into legacy runner test repair. The draft remains available at
`/home/joe/code/futon2-d-task-authority`, branch `codex6/d-task-authority`.
Do not live-load it. After the gate issue is resolved, complete the remaining
integration controls and review the stated coverage/conditioning limits before
landing and registering the post-commit executions.
