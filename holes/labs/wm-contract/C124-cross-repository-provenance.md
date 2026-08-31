# C124 — workspace-gate cross-repository provenance

Date: 2026-08-31

## Inputs reached by the gate

The 18 executable commands read four repositories:

- **futon2** — the gate/check implementations; witness registry and fragments;
  holder registry; preference-stack witness and production namespaces; fold
  escrow deposits; R2/R8 live and recorded trace corpora; R9 reports; and the
  committed route/run receipts.
- **mathlib4** — `DarkTower/WarMachine/holes-contract.json`; the declaration
  text for the R9 proof receipt in `DarkTower/WarMachine/Holes.lean`; and the
  imported Lean environment used by live `lake env lean` elaboration.
- **p4ng** — `control-map-edges.edn`, `control-stages.edn`,
  `control-organization.edn`, `hyper-edge-schema.edn`, the control-map SVG and
  PDF, plus the historical R9 corpus read by pinned `git show`.
- **futon3** — `checks/find_snatch.clj`, `checks/playout_snatch.clj`,
  `checks/snatch-cascade.edn`, and the authored `library/snatch/*.flexiarg`
  clauses used by the F1/F2/F3 controls.

## Provenance contract

Before executing checks, `wm_workspace_gate` now emits one `PROVENANCE` block
containing, for every repository, its path, HEAD commit, HEAD tree, dirty flag,
SHA-256 of the tracked diff from HEAD, and whether all provenance reads
succeeded.  These are observations, not equality assertions.  Checks continue
to enforce invariants; repository movement does not create a stale-baseline
failure merely because a previous gate saw another commit.

The first recorded block saw clean sibling repositories at:

- mathlib4 `b9c3377676fa80cbd6e552bbdbf3a056ed8af721`
- p4ng `20d1826e3aa3e61669a0fd4c3ae7b4d9c39cbcf1`
- futon3 `d77b7a2cb3c624b0e099f5bb21b127e048c76483`

Futon2 was correctly reported dirty because C124 itself was not yet committed;
its commit and tracked-diff hash make that state legible rather than pretending
HEAD alone described the checked tree.

## Wrong-version risk

Yes: several checks would correctly pass against a coherent but unintended old
sibling checkout.  Figure agreement proves SVG/data agreement, the find laws
prove the clauses satisfy F1–F3, and the strict contract check takes its
authority from the contract it is given.  Those are invariant checks, not
release-version selectors.  Organization and individual receipts have narrower
content/declaration pins, but they do not establish one global sibling-repo
version either.

The provenance block closes the silent part: a later reviewer can identify the
exact clean sibling commits checked.  Selecting an approved release tuple, if
needed, is a separate policy input and should not be smuggled in as stale
equality assertions.

