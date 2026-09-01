# C281 — live-artifact format-boundary lint

Date: 2026-09-01

`checks/live_artifact_format_boundary_lint.py` is the executable prevention for
defect class 6a. It reads exactly the eight generators declared in
`p4ng/empirics-futon/README-live-artifacts.md` and pairs every unsafe source
shape with the explicit presence/population proof that retires it.

## Live result

The C278 census found seven exposed generators. During implementation,
`gen_model_coverage.py` independently added a nonempty-row check and required
per-row fields. The lint therefore reports the current state, not the stale
census:

```text
6 findings / 8 generators
clean: defect-tally, model-coverage
finding: live-topology / classification-counts-before-format
finding: lane-campaign / blank-active-identities
finding: q-interface / missing-as-of-to-empty
finding: variable-situation / unreconciled-cell-population
finding: war-room / default-zero-and-null-metrics
finding: workflow-report / missing-lane-to-idle
```

Canonical invocation:

```sh
python3 checks/live_artifact_format_boundary_lint.py
# exit 1 while findings exist

python3 checks/live_artifact_format_boundary_lint.py --negative-control
# exit 0
```

The negative control presents the same `%d` sink twice: without a presence
proof it is flagged; after a same-scope `(assert (some? ...))` it is not. This
prevents the lint from degenerating into “all format calls are forbidden.”

## Honest boundary

This is a bounded contract lint, not interprocedural type inference. It can
prove direct validations and expected-population reconciliations.

**2026-09-01 C284 amendment:** comments and `FORMAT-PROOF` markers are not
evidence and are never accepted. A proof hidden in a helper remains
review-required until this lint learns that executable shape. This supersedes
the original marker-plus-control rule: a marker could be added without its
claimed control and therefore made the lint self-certifying.
Categorical reconciliation remains generator-specific: the lint can demand
that pointer-status rows reconcile, but it cannot infer the intended vocabulary
from a generic `filter`.

Missing generator files are `UNAVAILABLE`/exit 2, not clean. Findings are exit
1. A clean fully proved population is exit 0.

No live-artifact generator or generated paper artefact was edited in C281.
