# Why `serves-a-way` had no pick on click 4 (2026-11-06)

Scope: read-only reconstruction of click `wm-click-ca5cf289` from
`data/wm-runs/selection-record-2026-11-06-wm-click-ca5cf289-ccf4-43eb-a747-a15a2d7472a8.edn`
and Giraffe run `20261106T093804.173Z-a1acae93`.  The selection record is
single-line EDN, so retained-record citations are to line 1.

## Result

There is no parser, identifier, stale-pin, or replay defect in this click.
The 53 rows that are `status=open`, `action=work`, and have a non-`none` way
have **zero intersection** with the click's 223 admitted ready targets.
Consequently `serves-a-way` correctly reduced 223 to zero.

The placement source pinned in the record is:

* TSV `/home/joe/code/futon3c/holes/excursions/ant-reads/A1-placements.tsv`,
  SHA-256 `eda97d6ebfc722cbf1272bc3eadfb960141801bc88b03303006b38bb06abec55`;
* classes `/home/joe/code/futon3c/holes/excursions/ant-reads/classes.tsv`,
  SHA-256 `f4283b2d5e3493523f07981a95db2d2bcf1cfaf76d1c96b8ceb91cc9487e5ca0`.

The selection record retains both original paths, hashes, retained-store
paths, 108 per-stem rows, one class row, and `:status :pinned`
(`selection-record-...edn:1`).  The current TSV hashes to the retained hash,
and the retained copy at
`data/wm-runs/eda97d6e....placement-source` has the same hash.  Runtime replay
first accepts the retained bytes only when their hash equals the pin, then
falls back to the current path only under the same equality check
(`src/futon2/aif/full_loop_runtime.clj:1373-1385`); it reloads the resolved
paths without substituting current bytes (`full_loop_runtime.clj:1387-1403`).

## Exact join

I loaded the retained click field, the pinned Giraffe `pass.edn`, and the
pinned placement bytes through `placement-adapter/load-placements`.  The
counts are:

| population | count |
|---|---:|
| outer field admitted before readiness | 273 |
| admitted and Giraffe `:ready` | 223 |
| TSV rows `open` + `work` + nonempty parsed ways | 53 |
| intersection of the previous two rows | **0** |

Of the 223 ready targets, 222 received exactly
`{:status :absent :reason :no-placement-yet}`.  The remaining target,
`E-ukrn-paper-v2.4-comments`, received its real per-stem placement row:
`status "open"`, `action "work"`, but `ways []`, because its TSV `way` value
is `none`.  Thus all 223 fail `placed-workable?`, which requires string
`"open"`, a nonempty `:ways`, and `"work"` or `"cluster:*"`
(`src/futon2/aif/outer_score.clj:106-120`).

This also rules out the suspected shape bugs.  TSV columns are split by the
header and ID is retained as the same string used by the field
(`src/futon2/aif/placement_adapter.clj:52-57,67-71`).  `none` and blank ways
become `[]`; comma lists become vectors
(`placement_adapter.clj:59-62`).  Status and action intentionally remain
strings, matching `placed-workable?`.  Exact target lookup returns the row or
the typed absent fact (`placement_adapter.clj:97-120`), and annotation attaches
that fact to every observation fact (`placement_adapter.clj:122-131`).

The 53 apparently usable placement rows break down against the Giraffe/current
outer field as follows:

* 40 are excluded as `:unread-new`;
* 10 are not present in this Giraffe readiness population;
* one (`M-typed-holes-mathlib-handoff`) is `:cancelled`;
* two are Giraffe-ready but excluded before scoring:
  `T-jvm-provenance-before-multi-jvm` for `:relation-unrecognised`, and
  `T-outer-belief-separate-machine-failure` for
  `:completion-definition-not-found`.

The three examples in the request illustrate those categories rather than a
join defect.  `M-patterns-done-right` is retained as `:unread-new`;
`M-typed-holes-mathlib-handoff` is cancelled; and
`E-outer-selection-landscape` is absent from this Giraffe readiness
population (`/home/joe/code/storage/giraffe/20261106T093804.173Z-a1acae93/pass.edn`;
corresponding outer
exclusions are in `selection-record-...edn:1`).

The scored field is partitioned before cascade scoring
(`src/futon2/aif/outer_select.clj:366-395`), then every cascade is run against
that same admitted field (`outer_select.clj:397-426`).  So neither of the two
ready-but-excluded placed targets was silently available to
`serves-a-way`.

## Is there a code defect or a red test?

No code defect is evidenced, so no defect regression test is proposed.  A
test asserting that unrelated placement rows must force a nonempty gate would
be wrong.  Existing behavior is internally consistent: the exact pinned bytes
are parsed, string/vector shapes match the predicate, and the join is empty.

There is instead a **coverage/data-state fact**: the placement authority's 53
actionable rows cover none of the current scored ready field.  That may merit
refreshing placement observations or Giraffe readings, but a refresh must be
grounded in current evidence; changing IDs, bypassing unreadness, or treating
`none` as a way would fabricate eligibility.

An audit test worth adding only as telemetry—not as a pass/fail validator—would
retain counts for `ready`, `placed`, and their intersection beside the gate
trace.  The current trace already records `223 -> 0`; retaining the population
breakdown would make this diagnosis direct.

## Would a corrected fact form a second prediction class?

There is no erroneous fact to correct among the 53 rows, so the honest answer
for the observed click is **not applicable**: no existing actionable placement
can be admitted into the cascade merely by correcting parsing or replay.

One bounded counterfactual is informative.  The sole admitted ready target
with a placement row, `E-ukrn-paper-v2.4-comments`, would survive if—and only
if—there were genuine evidence for a non-`none` way.  Since it would then be
the only placed target, `serves-a-way` would pick it.  The retained per-task
rating gives it prediction digest
`7659e0cb371085ff9d249225b29a9ce47c75229c1e1ff2959d6457354ffd7158`,
different from `E-queries-and-scopes`'s
`a7d102689d9abfed76680bf1e5c7dc59c3873b52d633f9f045f1780994fc9df8`
(`selection-record-...edn:1`).  It would therefore form a second prediction
class.  This is not a recommendation to edit the TSV: the missing way must be
established independently.

Likewise, admitting either of the two placed Giraffe-ready targets requires
repairing its real outer exclusion first.  Their present relation/completion
evidence is insufficient to compute an honest selectable prediction, so the
probe must not claim that either would repair Q2.
