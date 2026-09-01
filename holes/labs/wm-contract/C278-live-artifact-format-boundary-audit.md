# C278 — live-artifact format-boundary audit

Date: 2026-09-01

Scope: eight generators named by
`p4ng/empirics-futon/README-live-artifacts.md`. Review and focused existing
negative controls only; no generator, source registry, generated snapshot, or
published paper was changed.

## Summary

| Generator | Missing/renamed value at output boundary | Classification |
|---|---|---|
| `gen_live_topology.bb` | missing classification counts print `null`; missing node label and stage print an empty label | **plausible/invalid output, exit 0** |
| `gen_lane_campaign_table.bb` | missing dispatch key is loud; explicit nil is rendered `---`; blank non-nil holding/job renders an empty measured cell | **one format-boundary gap** |
| `gen_model_coverage.py` | missing referenced keys raise loudly; an empty `rows` population renders a plausible all-zero table | **vacuous zero population** |
| `gen_q_interface_table.bb` | required interface/remediation population is checked; missing `:as-of` renders a blank date | **visible but plausible gap** |
| `gen_variable_situation_table.bb` | a row with absent/unknown pointer status disappears from both pointer columns and their total | **plausible lower counts, exit 0** |
| `gen_war_room_tetrahedron.bb` | missing repair statuses become zero; workflow fields can print `null` | **plausible/invalid output, exit 0** |
| `make_defect_tally_figure.py` | all rendered identities, statuses, evidence, population sizes, and as-of are validated | **clean for audited boundary** |
| `gen_workflow_report.bb` | absent lane rows become idle/zero; downstream totals can be absent despite a generated report | **plausible zeroes; table withdrawn** |

One of eight is clean under the requested mutation class. One has an otherwise
strong explicit-absence contract but accepts blank values. Six can publish a
plausible or syntactically valid artefact from incomplete semantic input.

## Per-generator evidence

### 1. Live topology — unsafe

`gen_live_topology.bb:68-78` validates endpoints, organization membership, and
edge count. It does **not** validate the rendered
`:classification-counts`. Lines 170-174 pass those values to `%d`; a missing or
renamed count becomes Java/Babashka's literal `null`, exactly the reported
format-boundary defect. Likewise `node-svg` renders `(or label stage)` through
`(str x)`; if both are absent, the node receives an empty second label while
the generator exits 0.

The existing unknown-endpoint negative control remains sound:

```text
bb empirics-futon/gen_live_topology.bb --negative
exit 0; unknown endpoint rejected
```

It tests graph shape, not rendered-field presence.

### 2. Lane campaign — explicit absence mostly sound; blank strings unsafe

`gen_lane_campaign_table.bb:27-51` requires all four lanes and every dispatch
key. A renamed or missing key fails loudly. It also requires the four dispatch
values to be either all nil or all non-nil; nil is deliberately rendered as
`---` alongside `IDLE (explicit)`, so the dash is truthful rather than a
measurement.

However, non-nil is weaker than present data. Empty strings for `:holding` or
`:job-id` satisfy the active-row test and render as empty cells
(`gen_lane_campaign_table.bb:88-96`). Timestamps are protected by parsing, but
holding and job identity are not asserted nonblank. This is a format-boundary
gap, not a missing-key gap.

### 3. Model coverage — field access loud; empty population vacuously zero

Python dictionary indexing makes missing `rows`, authority, as-of, area,
content status, pointer status, and live contract SHA loud. Unknown categorical
values also exit. The accounting assertion proves every supplied row is
classified.

It does not require `rows` to be nonempty or match an expected declaration
population (`gen_model_coverage.py:56-88`). An empty registry satisfies the
accounting assertion and renders every area and Total as zero. Those zeroes
look like measurements, not missing input. This is the C269 empty-contract
vacuity at the paper boundary.

### 4. Q-interface table — checked population, unchecked stamp

The generator runs `q_interface_completeness_check.clj` first. That check
requires seven definition IDs, six interface IDs, and strings for blocker and
next action on every gap (`checks/q_interface_completeness_check.clj:90-119`).
Its focused missing-remediation control rejected:

```text
bb checks/q_interface_completeness_check.clj ... --negative-control
exit 0; gap without next action rejected
```

The generator nevertheless formats `(:as-of data)` without asserting it
(`gen_q_interface_table.bb:116`). Clojure string conversion of nil produces an
empty string, yielding the plausible sentence `as of .` and exit 0. Missing
definition symbols render the explicit word `unmapped`, which is visible and
truthful rather than a silent default.

### 5. Variable-situation table — unsafe lower counts

Top-level shape, rows vector, content-axis population, and both contract pins
are checked (`gen_variable_situation_table.bb:34-55`). Per-row status vocabulary
is not. Cells count only exact `:resolves` and `:drifted` pointer statuses
(`:58-66`). A row whose pointer-status is absent, renamed, or unknown is counted
in neither column and disappears from that content row's displayed total. The
generator exits 0 with plausible lower counts. There is no assertion that the
sum of table cells equals `(count rows)`.

### 6. War-room tetrahedron — broadest unsafe boundary

The generator requires a nonempty variable population, 61 repair instances,
and four lane keys. It does not validate the categorical populations it
formats:

- repair counts use `(get st :repaired 0)`, `:partial 0`, and `:open 0`;
  missing or renamed statuses therefore lower the figure to plausible zeroes;
- lane state defaults unknown lane lookup to idle in `chip`;
- the uncertified workflow branch formats `:frontier` and
  `:pending-decisions` without checking them; `%d` prints `null` for the missing
  numeric value (`gen_war_room_tetrahedron.bb:96-123`).

This is the generator that can turn a schema rename into `null/38 ...` while
still writing a valid SVG and exiting 0. Its `:certified` branch is not the only
exposure: the deliberately uncertified branch still formats unchecked workflow
totals.

### 7. Defect tally — clean for this boundary

`make_defect_tally_figure.py:23-51` requires counting-rule version, nonempty
as-of, exactly nine classes, exact per-class instance populations, class
identity and name, unique non-null instance IDs, enumerated status, nonblank
evidence, and total population 61. Every numeric field rendered by the SVG/PDF
is derived from those validated enumerations rather than read through a
default.

Focused control:

```text
python3 empirics-futon/make_defect_tally_figure.py --negative-control
exit 0; evidence-less repaired row rejected
```

No missing-to-zero, missing-to-dash, or missing-to-empty rendering path was
found for a reader-facing measurement.

### 8. Workflow report — unsafe internal diagnostic

The paper table is permanently withdrawn, which bounds publication exposure,
but the generated EDN still feeds the tetrahedron. The generator validates the
registry version but not the four-lane population. `holdings` is built from
whatever rows exist; `per-lane` then iterates the hard-coded four lanes and
uses absent holdings as `:idle`, `:open-holding 0`, and `:none`
(`gen_workflow_report.bb:212-232`). A missing lane row therefore becomes a
plausible idle lane.

The report itself derives its totals, so its own current rendering is less
exposed to renamed total keys. The boundary reappears when the tetrahedron
consumes those totals without validating the workflow schema. Withdrawal of
the table mitigates paper publication, not the live interior's input contract.

## Conclusion

The recurring unsafe shape is not `%d` specifically. It is **aggregation or
formatting before proving population and field presence**:

1. defaults (`get ... 0`) turn absent categories into measured zero;
2. filters silently omit rows whose category is absent;
3. `str` turns nil into an empty field;
4. Java formatting turns nil into `null` while succeeding.

The permitted visible-gap form must be authored and labelled (`IDLE
(explicit)`, `unmapped`, `STALE-PIN`), not an incidental rendering of nil.

