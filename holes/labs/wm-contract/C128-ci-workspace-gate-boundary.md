# C128 — CI versus workspace-gate decision

Date: 2026-08-31

## Decision

The workspace gate remains **deliberately separate** from
`clojure -T:build ci`.  It is mandatory at the War Machine pre-merge/reviewer
boundary through:

```sh
make pre-merge
```

That target first runs hermetic futon2 CI and then the four-repository workspace
gate.  `make workspace-gate` is the narrower reproducibility command.  A
missing mathlib4, p4ng, or futon3 checkout is a loud failure; there is no skip
path.

## Why not put it in `ci`?

Putting the workspace gate in CI would maximize routine reachability and avoid
having a useful check that nobody runs.  Now that all 19 results are green,
red migration is no longer an objection.

But `ci` is the repository build boundary.  The workspace gate invokes Lean,
reads paper artefacts in p4ng, and runs futon3 code and authored library clauses
at absolute sibling paths.  Isolated clones, package builders, and ordinary
futon2 CI legitimately lack those repositories.  Making their absence fail
ordinary CI would train maintainers to bypass or ignore CI, while silently
skipping them would make the top-level gate claim more than it checked.

The separate required pre-merge target preserves both truths: futon2 CI remains
hermetic and independently useful; a War Machine review is incomplete unless
the cross-repository gate runs.  This is execution wiring, not merely a note:
the named reviewer target composes both commands and propagates either exit.

## Boundary semantics

- `make ci`: futon2 tests and build only; no sibling assumptions.
- `make workspace-gate`: all committed-artifact War Machine checks, with
  four-repository provenance; sibling absence fails loudly.
- `make pre-merge`: the required reviewer gate, in that order.

The live operational certificate and lane registry remain deliberately manual
for their previously recorded reasons.  Absence coercion remains reporting-only
under the C81 policy.  This decision changes no `clojure -T:build ci` semantics.

