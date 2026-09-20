# Selection fixture migration: discovery and scope

Owner/reviewer: claude-12. Implementer: codex-5. Phase 1 was reviewed before
implementation; Phase 2 adopts all six proposed migration constraints.
Authority: RULING-selection-precedence-2026-09-19.md, the operator decision,
and STOP-THE-LINE-2026-09-20.md. This packet makes no model-authority claim.

The aae84fc302207cd79cdf0adf9353706beb2b516a baseline attribution records
180 tests, 966 assertions, 43 failed assertions across 11 tests, and one error.
The retained log SHA-256 is
cf2201265cf8d8a80c043d254d9c0d3534ed490044bfca452a90738f92f93eae;
the pinned runner source SHA-256 is
feb72603b319607b24726f8d764306fe2c3e3000ed8c7274ba430b3601902991.
Both were verified against retained bytes before migration.

| Fixture | Failed assertions |
| --- | ---: |
| recoverable-late-author-completion-skips-second-author-turn | 4 |
| recoverable-late-review-completion-skips-both-replacement-turns | 3 |
| reviewer-recovery-without-author-provenance-fails-before-dispatch | 3 |
| terminal-recovery-job-transitions-to-machine-repair | 5 |
| recovered-review-rejection-hands-line-to-one-review-finding | 4 |
| done-author-recovery-without-artifact-transitions-before-dispatch | 3 |
| done-author-recovery-with-a-non-commit-artifact-is-refused-before-dispatch | 5 |
| machine-stop-line-preempts-ordinary-selection-and-awaits-successor-validation | 7 |
| refused-construction-append-is-atomic-and-repair-selection-is-truthful | 1 |
| historical-execution-port-cannot-forge-the-completion-marker | 1 |
| historical-verification-action-commits-without-author-dispatch | 7 + 1 error |

39 failures directly concern the unexpected ordinary route. Four historical
assertions concern the resulting initialization/reader failure; the final ledger
assertion raises the same reader error. The construction fixture's atomicity
assertions already pass; only its selected-action trace expectation is stale.

## Unresolved serialization finding (outside this packet)

The writer persists non-round-trippable payloads; the reader fails later and
elsewhere. The historical fixture's dispatch observer returned
`(swap! dispatches conj args)`, embedding functions and atoms from runner options
in the response. Ordinary selection reached this observer. The runner retained
that response at `[:payload :ground :response]`, and the cohort wrote it with
`pr-str`. Later `clojure.edn/read-string` rejected `#object`.

Original artifact:
`/tmp/historical-action-cohort699189057367668222/historical-action-test/attempt-001/004-dispatch.edn`.
A byte-identical copy is retained here as `baseline-non-readable-dispatch.edn`.
SHA-256: `850cb9737d9c22ad6d5b35e0e987c8fe27ba2e664a7bd127aaf90026292ec536`.
The object identity and temporary cohort root match the baseline failure log.

Removing the fixture trigger does NOT fix this serialization boundary. No
writer, reader, or validation predicate is changed. Claude-12 owns carrying
this separate finding onto the board.

## Migration contract

Repair/historical fixtures use explicit actions selected by the real
`policy/select-action-cascades`, with repair G=-2, ordinary G=-1, beta=2.
The selector's actual action is asserted. No selection-law marker is inserted.
The preemption test becomes paired ordinary/repair controls in the same deftest:
both transform the decision and record all open IDs; ordinary selection acts
on M-selected, while explicit repair acts on its target (not the first open
obligation), uses codex-1 as independent reviewer, and leaves the real store's
implementation awaiting successor validation. Recovery guards, historical
identity checks, the forgery negative, and construction atomicity remain.
Forbidden dispatch observers capture the call and throw; expected dispatch
observers return job maps. No tests are skipped or deleted.
