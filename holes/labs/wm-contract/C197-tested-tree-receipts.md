# C197 — Tested-tree suite receipts

Date: 2026-08-31

## Decision

Suite evidence is current by content identity, not wall-clock order. A bounded
receipt records the repository basis before and after its command: `HEAD` tree
SHA, SHA-256 of `git diff HEAD --`, and dirty/readable flags. Readiness accepts
the receipt only when both observations are stable, clean, and equal to the
current clean repository basis.

This makes an old receipt for an identical tree current and a newer receipt for
a different tree `UNVERIFIED`. Any dirty state remains `UNVERIFIED`: a commit
tree plus a changing worktree is not a stable identity for what was tested.
Legacy receipts have no tested-tree provenance and are not backfilled.

## Controls and invocations

- `make run-readiness-tree-control` constructs an old same-tree receipt and a
  recent different-tree receipt. The former must pass and the latter must be
  rejected as `tested-tree-differs`; `0=pass, 2=control-slipped`.
- `make run-readiness` reads real receipts and reports each suite basis and its
  content-currentness. It is read-only and does not launch a tick or suite.
- Receipt producer smoke check:
  `python3 scripts/bounded_test_job.py --receipt <tmp>/receipt.json --output <tmp>/out.log --cwd <repo> true`
  from `futon3c`.

The commissioner refresh `bounded-1788212679658-futon2-bounded-refresh`
finished green but was launched before the receipt producer carried repository
basis fields. It remains valid historical execution evidence and is
`UNVERIFIED` for current-tree readiness; inventing its tested tree afterward
would defeat the provenance change.
