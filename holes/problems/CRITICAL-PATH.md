# War Machine critical dependency path

Current as of 2026-08-31 after C167. This is a dependency graph, not a
schedule. Decision questions remain authoritative in
`DECISIONS-PENDING.md`; implementation and evidence details remain in the
linked delivery records.

## Verified graph

```text
C167 channel-priority repair                                  COMPLETE
  └─ diagnostic tick completes                               COMPLETE
      ├─ post-v18 support shadow emitted (2 records)          COMPLETE
      │   ├─ C108 shadow→live decision becomes eligible       DECIDABLE NOW
      │   └─ C130 empirical basis becomes nonempty            PARTIAL ONLY
      └─ first v20 record emitted                             COMPLETE
          ├─ all trace readers consume v20                    COMPLETE
          ├─ resource-clean operational certificate passes   COMPLETE
          └─ Joe's production run is no longer the first
             exercise of schema/readback/certificate          TRUE, WITH SCOPE
```

The proposed C171 chain was correct through emission, but it arrived after C167
had already completed that prefix. There is no remaining technical block from
the nil `:gap` bug.

The final branch needs a scope qualifier: C167 used
`stub:first-ranked-authorized-mission`, stamped
`:trigger :diagnostic-run-tick-once`, and set `:live-wire? false`. It exercised
schema v20, trace readers, topology evidence, resource evidence, and the
certificate. It did **not** exercise the production selector or live outward
actuation. Joe's production run is therefore no longer the first test of the
evidence envelope, but it remains the first current production-path exercise.

Sources: [`C163`](../labs/wm-contract/C163-diagnostic-tick-findings.md) records
the original pre-emission failure; [`C167`](../labs/wm-contract/C167-channel-priority-repair.md)
records both post-repair ticks, v20 readback, and certificate; the concrete
readback and certificate are
[`C167-v20-readback.edn`](../labs/wm-contract/C167-v20-readback.edn) and
[`C167-v20-operational-certificate.edn`](../labs/wm-contract/C167-v20-operational-certificate.edn).

## What tick completion made measurable

The canonical C108 census now reports:

```text
54 files · 803 records · corpus SHA-256 544b6fdf…729a9e
2 records with presence provenance and support shadows
288 classifiable/shadowed candidates with absent channels
0 incomparable-support pairs
0 candidate rank changes · 0 winner-changing records
```

This is enough to satisfy C108's former **existence** precondition: the
shadow-to-live switch is now a decision Joe can make, so it moves into
`DECISIONS-PENDING.md`. It is not enough to claim representative behaviour:
the denominator is two diagnostic records through one stub selector seam.

The proposed chain overstates what this measurement does for C130. The shadow
observes support-sensitive score comparison; it does not simulate the A/B
semantics for missing prediction triples, strategic-mode inference, sorry
pressure, rollout refusal/exclusion, fulab temperature, or belief aggregation.
Those seven questions were already normatively decidable, and now have a
nonempty adjacent observation corpus, but their option-specific ranking impact
remains unmeasured. Measuring that impact requires a shadow or forward run for
each chosen semantics; the live switch must not be inferred from the zero rank
changes above.

Source: [`C108`](../labs/wm-contract/C108-support-typed-scoring-shadow.md) defines
the shadow contract and its former zero-coverage boundary. Canonical current
measurement: `bb -cp . checks/absence_scoring_counterfactual.clj`.

## What is blocked on Joe

All **11** entries in [`DECISIONS-PENDING.md`](DECISIONS-PENDING.md) are
decidable today, with different evidence strength:

```text
StrategicOutcome decision
  └─ strategic carriers ──> canonical G_S/E_S ──> strategic mission selection

R16 authored outward binding
  └─ bounded armed operation + independent readback + next-belief observation

R5 hard-guard authority
  └─ named avoided-range vetoes (if any); diagnostic already works without it

seven C130 absence decisions
  └─ seven live coercion removals; option-specific ranking effects not measured

C108 shadow-to-live authority
  └─ equal-support enforcement at live selection; first evidence now exists
```

Deferral does not break the current machine. It preserves the explicit
baseline/red/refusal at each boundary: the strategic layer remains an
engineering surrogate, R16 remains construction-only, avoidance remains
diagnostic, seven coercions remain lint-red, and support typing remains
shadow-only.

Sources: [`C166`](../labs/wm-contract/C166-strategic-outcome-stop.md),
[`C78`](../labs/wm-contract/C78-outward-act-refusal.md),
[`C113`](../labs/wm-contract/C113-avoidance-unknown-safety-design.md),
[`C130`](../labs/wm-contract/C130-absence-decisions.md), and
[`C108`](../labs/wm-contract/C108-support-typed-scoring-shadow.md).

## What is blocked on neither

- Additional bounded diagnostic ticks can accumulate v20 support-shadow
  evidence without changing scoring, ranking, selection, or actuation.
- Trace compatibility/readback and operational-certificate checks can continue
  over each new diagnostic record.
- The absence lint, workspace gate, correction-index check, and other existing
  controls remain independent of the eleven semantic choices.
- Preparing option-specific **shadow measurements** is independent where it
  does not select or actuate; switching any option live is not.

The operator-triggered production run is not classified as independent work:
it requires Joe's trigger by definition, even though the C167 bug no longer
blocks it. It also remains distinct from the eleven design/authority decisions.
