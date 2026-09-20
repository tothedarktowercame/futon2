# D task authority 2c execution receipt

Implementation: 328141ab99fc016a08d6be7120b59edb51b7093a. Attribution: 400889ef.

The seven committed source/test files match precommit.json byte-for-byte.
Lint reports zero errors/warnings; check-parens passes (precommit logs).

| Namespace | Pre-commit validation | Post-commit warrant |
| --- | --- | --- |
| d-predecessor-task-authority | 5 tests / 31 assertions | Same; test-registry-e42e04926b394e9a0eb2c87d0bf1a2dfa6a7d6ce10fc2fc172e573cdd9618f10 |
| token-belief-predecessor | 3 tests / 25 assertions | Same; test-registry-9f79aff0a085d4cbc138adf0ecedd77b8a8fcb0bacebc973a3ec12b2e8821b65 |
| token-belief-carry | 4 tests / 30 assertions | Same; test-registry-925876b6e84354939070e96409d014f19a1705b1159c71ddc2ceabb94a89f3d3 |
| scoring-input-receipts | 3 tests / 45 assertions | Same; test-registry-9da2527bfa71147fbb4140127c706092db39d49df839b77c86120fd66bdaa081 |

Both roles pass 15 tests / 131 assertions in total, with zero failures/errors.
validation-comparison.json records each role, SHA, matching source hashes,
counts, warrant ID, and log hashes. Runs agree; none was retried to obtain green.

Postcommit execution used an isolated checkout of 328141ab because codex-8 was
editing the shared runner. D hunks alone were staged; construction-fold changes
were left intact and landed independently as c91261fd. These warrants bind
328141ab, not that later combined source. No shared service was loaded/restarted.
Temporary D implementation/baseline/warrant worktrees are removed after preserving
these receipts. Registry configs retain their actual historical execution path;
reproduction requires checking out the named commit at that path.

Full runner attribution is separate: see ATTRIBUTION.md and both complete logs.
The full namespace is not green. All 43 failed baseline assertions and its one
error reproduce in the draft; the remaining hermetic assertion observes a
corroborated concurrent machinery-65 production trip, not a D-induced failure.
The incomplete-classpath baseline attempt is retained and explicitly excluded.
No legacy fixture migration or construction-fold repair is included in D.

Scope at landing: task execution producer, independent verifier, exact-identity
reader, and narrow D carry authority only. E1 portfolio membership, R6-R11 joins,
and broader E2b correspondence remain unestablished. Positive controls use real
Git and observation checks with raw Agency job fixtures; no live task success
is claimed. Recovery/historical cases remain typed refusals.

C3/C4 after-revision affirmation is supported. Other readers remain explicit
unavailable measurements; before evidence remains :not-measured without a proven
mapping. The consumed belief remains fresh initialization, and admission still
records :conditioning-status :not-wired. No live posterior/Q movement is claimed.
