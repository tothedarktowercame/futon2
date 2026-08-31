# C186 — can diagnostic ticks grow the C182 evidence base?

Date: 2026-08-31. Assessment only; no tick was run.

## Answer

Diagnostic ticks can add **distinct sequential states**, so Joe's production
run is not the only way to grow immediate-boundary evidence. They do not add
independent selector trials, and they cannot cover the production selector
seam. Raw tick count is therefore the wrong `n`.

The stub is deterministic conditional on its input. `stub-selection` takes the
first member of `:scheduler-habit-ranking` and introduces no random choice
(`scripts/futon2/run_tick_once.clj:57-89`). But its input is live and stateful:

- candidates come from the current mission, pattern, sorry, anticipation, and
  evidence views (`war_machine.clj:4454-4490`);
- belief and precision are folded forward from the preceding trace record
  (`war_machine.clj:4327-4368`);
- the scheduler list is derived from the current admissible ranking
  (`war_machine.clj:4594-4605`).

Thus “same stub” does not imply “same observation.” It means repeated inputs
produce repeated outputs, while changed corpus or carried state can change the
ranking the stub receives.

## What the two records actually did

Pinned corpus: 54 files, 803 records, SHA-256
`544b6fdf149662bdbf0942e1c8b30d4f2820b397040f1f0663b23331ae729a9e`.

| field | diagnostic 1 | diagnostic 2 | same? |
|---|---|---|---|
| observation SHA-256 | `7be1a6cc…61d26` | `34ac6221…963c` | no |
| pre-belief SHA-256 | `46094aa6…60cb` | `45ff125f…cb7` | no |
| post-belief SHA-256 | `45ff125f…cb7` | `4580d4ac…37b4` | no |
| precision-state SHA-256 | `9a009ba3…1fb8` | `b115b944…42c` | no |
| ranked-actions SHA-256 | `26ee36bc…d0c` | `132f3536…50cd` | no |
| stub input ordering | AIF-policy, WM-compliance, shared-memory | WM-compliance, shared-memory, AIF-policy | no |
| selected mission | `M-aif-policy-conditioned-eig` | `M-wm-aif-policy-grain-compliance` | no |

The exact equality of diagnostic 1's post-belief and diagnostic 2's pre-belief
is direct evidence of dependence, not merely a common timestamp or shared
corpus. The records are two distinct selector decisions, but not two
independent draws.

For C182 specifically, both records still had the same branch-relevant absence
(`:sorry-count-norm :source-field-missing`), the same 144-candidate population
size, the same option-B fallback action, and similarly signed aggregation
deltas. They show the mechanisms firing across two evolving states; they do not
estimate a population frequency.

## What more diagnostic ticks can and cannot add

They can add:

- new observation, belief, precision, and ranked-population fingerprints as
  the evidence corpus or carried state changes;
- repeated immediate driver deltas, including evidence about their magnitude
  and whether a sign change ever occurs;
- new branch-relevant absence reasons if producers change.

They cannot add:

- an independent sample merely by being rerun;
- evidence about the live selector, because `run_tick_once` explicitly stamps
  `:selector-seam stub:first-ranked-authorized-mission`, trigger
  `:diagnostic-run-tick-once`, and `:live-wire? false`
  (`run_tick_once.clj:93-102`, `204-213`);
- downstream ranking causality without sequential option replay.

Joe's production run therefore adds something diagnostics cannot: the first
current production-selector-seam observation. It is a new evidence **stratum**,
not simply a third member of the same sample.

## What `n` would be enough?

No defensible fixed raw `n` exists yet. The project has specified neither a
target effect size nor a sampling model, and sequential beliefs violate the
independence assumption behind treating tick count as sample size. Declaring
“10” or “30” would manufacture acceptance after seeing two records.

Until an estimand and stopping rule are named, report three denominators:

1. raw tick records;
2. unique input fingerprints `(selector seam, observation, pre-belief,
   precision state, ranked population)`;
3. selector-seam strata represented.

Current values are **2 raw records · 2 unique sequential-state fingerprints ·
1 selector-seam stratum · 0 independent-trial claim**. The evidence should
remain labelled **directional**. The minimum qualitative improvement is one
production-seam record plus additional diagnostic states only when their input
fingerprints differ; identical reruns add zero effective evidence. Moving from
directional to an effect estimate requires a predeclared outcome (for example,
driver-delta distribution or abstention incidence), a material-effect
threshold, and a stopping/power rule. None exists today.

## Reproduction and gates

The fingerprint census was read-only over the same corpus C182 pins. Canonical
commands remain:

```sh
bb -cp . checks/c130_immediate_option_measurement.clj
bb -cp . checks/preemptive_absence_coercion_lint.clj
make workspace-gate
```

Absence remains seven live decision sites. C182's implementation and measured
numbers are unchanged; only their evidential strength is relabelled.
