# C182 — immediate measurements for two C130 absence options

Date: 2026-08-31. Authority: measurement only. No scoring, belief, ranking,
policy, selection, or actuation behavior changed.

## Boundary and corpus

This measurement answers only what changes at the immediate fallback-selector
or belief-aggregation boundary. **Downstream ranking effects are unmeasured and
require sequential replay.** A zero below must not be restated as “option A
changes nothing.”

Canonical corpus at measurement time:

```text
54 trace files · 803 records
SHA-256 544b6fdf149662bdbf0942e1c8b30d4f2820b397040f1f0663b23331ae729a9e
2 current tagged-envelope records
288 ranked candidates (144 in each record)
```

The 288 candidates are exposure within **two selector decisions**, not 288
independent decisions. Both records are diagnostic ticks through the stub
selector, so a third record from Joe's production-path run would improve the
evidence materially: it would add the first current production selector
population. It would not, by itself, measure later-tick consequences.

## Missing sorry pressure

Both records have `:sorry-count-norm {:variant :absent :reason
:source-field-missing}`. At the immediate `default-mode-select` boundary:

| option | record 1 | record 2 |
|---|---|---|
| A — abstain/return control | `:abstain :unknown-sorry-pressure` | same |
| B — continue through branches not inspecting sorry pressure | selects `:learn-action-class/:survey-mission`, recorded rank 144 | same |

Thus the immediate result differs in **2/2 records**: A abstains and B selects.
This does not say which is safe, and it does not say what the live strategic
selector would rank later. It measures exactly the branch-local consequence
over the retained candidate populations.

Source loci: `src/futon2/aif/policy.clj:120-172` and the persisted
`:observation-envelope` / `:ranked-actions` fields in
`src/futon2/aif/trace.clj:429-462`.

## Belief aggregation

Both records contain eight numeric prediction-error entries, but the envelope
shows that `:sorry-count-norm` was honestly absent before it was coerced. The
current driver and option A's observed-support-only driver are:

| timestamp | current, 8 coerced channels | A, 7 observed channels | delta A − current | sign change | B |
|---|---:|---:|---:|---|---|
| `20:32:24.972954701Z` | -0.058013863604 | -0.071218020273 | -0.013204156669 | no | refuse incomplete collection |
| `20:35:51.187865882Z` | -0.062775435448 | -0.075183402264 | -0.012407966817 | no | refuse incomplete collection |

Option A changes the immediate aggregate in **2/2 records** and its magnitude
by about 0.0124–0.0132, without changing its sign. Option B refuses both
collections. The current driver reconstruction agrees exactly with the final
recorded micro-step driver (error ≤ `1e-12`), so the comparison is against the
quantity the tick actually used rather than a parallel interpretation.

This still does **not** reconstruct the alternative three-step belief
trajectory. Only the final weighted-error collection is retained; the
per-step collections that produced earlier micro-steps are not. Consequently
“option A changes the immediate aggregate in 2/2 records” is supported;
“option A changes a later ranking” is not.

Source loci: `src/futon2/aif/belief.clj:1017-1052`, prediction-error production
at `scripts/futon2/report/war_machine.clj:4360-4444`, and trace persistence at
`src/futon2/aif/trace.clj:429-462`.

## Reproduction and current gates

```sh
bb -cp . checks/c130_immediate_option_measurement.clj
bb -cp . checks/preemptive_absence_coercion_lint.clj
make workspace-gate
```

The measurement command exits 0 only when current records exist, both target
populations are nonempty, and the independently reconstructed current
aggregation agrees with the recorded final micro-step within `1e-12`.

The absence lint remains honestly red at **7** decision sites. The workspace
gate was run as found; its result is recorded in the delivery report rather
than worked around while concurrent `Holes.lean` work is in flight.
