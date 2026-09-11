# Historical repair revalidation proposal — not adopted

The ordinary `:repair-machine-failure` action cannot truthfully revalidate a
historical implementation: `fresh-artifact-binding` requires a changed HEAD,
the prompts attribute that change to the author, and `ground-commit!` creates a
new implementation entity. No existing action carries all required semantics.

Proposed action: `:revalidate-historical-repair`. Its immutable input binds:

- repair `repair-attempt-057-untyped-failure` and the exact finding digest;
- implementation commits `9ab503bd61be1d63e7a24731e8e8aa285a9e44da`
  through `3bdc381e76518e69f90077397fa46495da98e61c`, with verified Git ancestry
  and both commits ancestors of the reviewed source HEAD;
- a fresh verification attempt, independent reviewer and executed review job;
- a new verification/discharge identity, never the existing implementation
  entity ID;
- an explicit witness produced by running the pinned qualification battery.

Admission to `:awaiting-validation` requires all bindings, a passing fresh
review, nonempty executed checks, unchanged source/finding bytes, distinct
actors, and immutable absence of the verification identity. It does not resolve
the repair. `repair/resolve!` remains reserved for a later, distinct,
production-shaped successor.

Reusable mechanisms are Git ancestry checks, strict immutable finding reads,
Agency review execution evidence, the grounding witness shape, immutable
repair-store writes, and the existing awaiting-validation/successor split.
Fresh-author binding and `ground-commit!` are deliberately not reused because
their semantics are false for a historical commit.

The adjacent qualification script checks the proposed finite envelope only.
It is not imported by the runner and is not an action implementation.
