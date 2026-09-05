# C532 — F8 refusing convergence checker

Date: 2026-09-05

`convergence_check.bb` checks the adopted ledger rather than trusting its
labels.  It re-reads the eighteen equation identities from
`aif-equations.edn:74-200`, verifies all 54 copied fields, resolves all 36
licences by suffix across futon2, p4ng and mathlib4, checks the fixed F6 rung
order, recomputes leading legs, validates both certificate forms, and derives
accepted-run status from `data/wm-step/w1/pin/pin.edn:1`.

## Run-identity decision

Both predicates are computed.  The F6 predicate at
`scripts/generate_variable_situation_accounting.bb:450-462` sees only a
top-level map or top-level sequence of maps and finds zero run identities in
the licences at witnessed-or-higher rungs.  The checker licenses evidence using
a run identity anywhere in the artifact tree, while separately printing the
F6 count.  This admits the F7 decision whose identity is deliberately nested at
`runs/F7-cascade-policy/f7-cascade-policy-decision.edn:146`, without confusing
it with a fixture: the same record must also satisfy the quantity-specific
policy-set predicate (its schema and at least two constructed candidates).
Thus nested identity alone is insufficient.

## Certificates and acceptance

Artifact certificates and trailing-leg certificates are disjoint checked
forms.  A non-green or non-accepted artifact certificate must explain why; a
trailing certificate must name `:spec`, `:impl`, or `:both` and carry both its
reason and licensing condition.  Only a green artifact whose nested run id is
present in the accepted-step authority can support `:converged? true`.
Declared `:accepted-run?` is compared with that computation, not trusted.
The three R5 simulations remain valid artifact certificates but correctly
support zero convergence because `runs/F3-node-sim/00-r5-pilot.edn` has no run
identity.

All four ledger caveats are pinned: Box-5 staleness, the R5 certificate limit,
the R17 class-(b) divergence, and the retired scalar F/flag-gated F-pi status
(`CONVERGENCE.edn:7-12`).  Section 14 of the shared negative controls plants
each refusal on temporary copies and leaves the committed ledger unchanged.

No registry, source, decision, certificate, or ledger was changed.  No live
tick, run lock, generator, or publish was used.
