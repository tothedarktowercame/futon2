# C176 — contract regeneration remains a separate, mandatory closure step

Date: 2026-08-31. No hook or cross-lane commit policy was installed.

## Decision

Keep Lean authoring and contract regeneration as two commits in one lane
delivery. A Lean lane may be red while its refactor is in flight, but it may
not report delivery complete until it has:

1. committed the `Holes.lean` change;
2. regenerated the contract against that committed source authority;
3. rebound affected witness fragments and merged the registry;
4. passed `contract_authority_current`, strict contract lint, and the workspace
   gate.

This is manual in sequencing, not optional in acceptance. C175 makes the
missing second phase executable and loud.

## Why not couple it to every Lean commit

Coupling would shorten the red interval and prevent a lane forgetting the
generated artefacts. A named command that performs the closure phase is useful.
But a pre-commit hook cannot honestly embed the SHA of the commit it is in the
process of creating, hooks are not shared enforcement, and regenerating on
every intermediate refactor would create noisy contract/witness churn. An
automatic amend or post-commit mutation would also rewrite history or leave an
uncommitted tree. Those costs outweigh eliminating a bounded, truthful red.

## Why not leave it ad hoc

The separate step is safe only because delivery acceptance names it and the
gate rejects source-blob skew. C175 correctly uses the `Holes.lean` blob as the
semantic unit: after regeneration at `e48a3158`, mathlib HEAD advanced to
`0104073` without changing that blob, and the check remained green. Requiring
literal HEAD equality would recreate stale-baseline churn on unrelated edits.

## Transient-red handling

Do not add a broad acceptance for `workspace-gate exit 1`; it could hide a
second failing check. A future in-flight classification is safe only when all
of these are executable facts: the gate's exact failure set is solely
`contract-authority-current`, the lane record explicitly declares a Lean
source change, the old/new source blobs are recorded, and the holding has an
expiry. The current lane schema does not carry those facts, so the honest state
during phase one remains `DEGRADED-NEW`. It is actionable by the owning lane,
not an operator incident, and clears at phase two.

This is the general rule for correct instruments that report expected
intermediate state: distinguish the state only from typed, bounded evidence;
never silence the instrument or accept a coarse parent exit code.
