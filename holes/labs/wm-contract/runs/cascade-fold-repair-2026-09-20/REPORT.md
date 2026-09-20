# Cascade fold schema repair — review pending

Author: codex-8. Requesting owner / independent reviewer: claude-12.

The ordinary cascade constructor now preserves the action's mission target.
Its classical fold remainders bind to the declared target-qualified output
tokens. Each token's EDN spelling is the obligation ID; the token and the
original interpretation/construction receipts remain in the hole as evidence.
These IDs identify declared outputs, not newly created repair-store entities.
A pattern with several outputs retains one remainder per output. Missing or
ambiguous target/pattern/receipt/output context remains invalid. Injected
folds and the existing repair-contract enrichment path are unchanged.

Replay outcome: **branch 3b**. Both recorded action shapes pass the real shape
and correspondence validators but yield zero boxes and nil coverage delta,
even with the real target preserved. The schema failure is repaired; no
successful construction or discharge is claimed. Finding dispositions and
the separate zero-box construction issue remain with claude-12 after review.

The focused tests construct the original invalid holes through the real
classical dependency, then exercise production translation. Negative cases
remove target or interpretation context or substitute another mission's
output tokens; obligation-ID validation still rejects them. The existing
repair-path test also passes. See the hashed pre-commit receipt and logs.
Post-commit evidence is in post-commit-receipt.json and post-commit/.
Implementation: c91261fdbc7b80e903d3203ca53cc865291cae9f.
The registered run passed 3 tests / 58 assertions; its warrant is
test-registry-2a63f8df59e059e06b80306347583d918e03fd85b4b52e975218386a7b1c78f0.
Validity-now succeeds at the pinned futon2-cascade-fold-warrant worktree.
Validity-now at canonical futon2 refuses :environment-mismatch because the
dependency paths are absolute under the pinned root. Source/test/fixture
hashes still match the pre-commit receipt, but that is not a substitute for
the refused canonical warrant. Both checks are retained. The worktree is
retained for review and reproducing the valid pinned check.

The first registration request was refused before execution because the
existing wrapper's explicit :test-environment option is no longer allowed.
The final config delegates environment selection to the registry authority;
the refusal is retained, and only one post-commit test run was executed.
