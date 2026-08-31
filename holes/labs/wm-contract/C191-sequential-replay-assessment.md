# C191 — sequential replay assessment for C130 downstream effects

Date: 2026-08-31. Assessment only. No tick or replay was run and no behavior or
trace schema changed.

## Answer

A sequential diagnostic replay could answer a bounded question:

> Holding a recorded exogenous input sequence fixed, when does option A versus
> option B first change the core ranked population, the fallback result, or the
> carried belief state?

It cannot answer that question from today's trace. The records preserve enough
state to demonstrate carry (`mu-post[t] = mu-pre[t+1]`) and enough outputs to
check a replay, but not the complete pre-ranking inputs needed to recompute the
counterfactual chain. Nor can a stub-selected chain establish production
selection effects.

Serial dependence is therefore necessary but not sufficient. It gives the
replay its recurrence relation; it does not make an output log into a replay
input log.

## Propagation depth

The first possible divergence is **the same tick (lag 0)**, not three ticks
later:

- belief aggregation occurs before `wm-state` is ranked, so an omit/refuse
  branch may alter the same tick's belief, EFE scores, and ranking
  (`war_machine.clj:4360-4458`);
- the sorry-pressure choice is inside the exception fallback for the same
  tick's controller selection (`war_machine.clj:4573-4584` and
  `policy.clj:120-172`). If invoked, abstain versus continue diverges there.

If rankings do not diverge at lag 0, the altered belief can carry into the next
tick and remain latent. There is **no finite guaranteed depth** in the current
model: no contraction bound, finite influence horizon, or theorem says a
nonzero belief difference must either change a selection by tick `k` or never
do so. A replay can report “first divergence within horizon H” or “none through
H”; it cannot infer “none exists” from a finite clean prefix.

The existing two-record chain supplies only one between-tick propagation edge.
Even with complete inputs it could test lag 0 and one next-tick carry, not a
general propagation depth.

## What the trace has and what it drops

Retained and useful:

- observation plus lossless observation envelope;
- `mu-pre`, `mu-post`, prediction errors, precision state, micro-step summary;
- compact ranked actions, decision, selection-gain and habit-prior sufficient
  state;
- code/config provenance in `:wm-version`.

Missing for faithful sequential replay:

1. **Pre-coercion per-micro-step prediction inputs.** Only the final weighted
   error collection and summaries of prior micro-steps survive. An alternative
   aggregation at step 0 changes the belief used to predict step 1, so the
   baseline step-1 output cannot be reused (`trace.clj:429-449`).
2. **The persisted ranking snapshot.** The in-process `!last-wm-inputs` atom
   holds `wm-state`, enriched candidates, mission registry view, scan id and
   structural grounding for reranking, but it is not written to the trace
   (`war_machine.clj:688-735`, `4505-4511`).
3. **Raw candidate/forward-model inputs.** `strip-ranked-action` deliberately
   drops the nested prediction as “recoverable by re-running”; that recovery
   would read today's registries unless their inputs were pinned
   (`trace.clj:76-130`). Reusing the baseline ranked outputs after changing
   belief would be circular.
4. **Fallback invocation provenance.** The trace retains the final strategic
   decision after its action is overwritten by the strategic seam; it does not
   say whether `policy/select-action` succeeded or the exception fallback ran
   (`war_machine.clj:4573-4640`, `trace.clj:132-141`). C182's sorry result is
   consequently a conditional branch counterfactual, not an observed fallback
   rate.
5. **Pinned exogenous scan inputs.** The trace persists the derived observation,
   not the complete `scan-data` and registry/evidence snapshots from which
   observation, candidates, structural pressure, anticipation and missions
   were produced (`war_machine.clj:4213-4259`, `4454-4511`).

A replay delivery would need a once-per-tick, content-pinned **replay envelope**
containing these exogenous inputs and the pre-ranking snapshot. This is the
same persistence-boundary lesson as C66/C104: store the rich evaluation object
once, rather than reconstructing it later from compact outputs. The envelope
must also distinguish exogenous inputs from endogenous state, so branch B does
not accidentally consume branch A's future outputs.

## What a diagnostic replay would establish

The core ranking path before strategic selection is shared, so a diagnostic
replay can validly establish that an option can (or did within H) change:

- belief state;
- controller scores or rank order;
- admissible population;
- the three-member scheduler-habit ordering supplied to a selector.

It cannot establish the production-selected mission. The diagnostic seam is a
deterministic function that chooses the first scheduler member
(`run_tick_once.clj:57-89`); the production selector may consume memory,
relations, calibration and other evidence. A divergence under the stub proves
possible propagation in the shared upstream ranking. No divergence under the
stub does not rule out a production-seam divergence, and a different stub
winner need not imply a different production winner.

The strongest design is therefore two reported endpoints: **core rank
divergence** (selector-independent) and **selected-action divergence by named
selector seam**. Production selection requires production-seam records or a
pinned, independently executable production selector.

## The three declarations, in order

1. **Estimand first.** Recommended shapes that the machine can record are
   first-divergence lag, winner divergence, rank displacement, and belief-driver
   delta over a fixed replay horizon. Choosing which one answers the governance
   question is a commissioner/operator decision; it is not derivable from the
   existing two records. The records can show feasibility and baseline ranges.
2. **Material-effect threshold second.** For winner identity, “any divergence”
   is a natural executable threshold. A material numeric driver delta or rank
   displacement is a policy judgement because it determines which changes are
   ignored; Joe owns it where ranking or safety is affected. Existing records
   can calibrate scale but cannot choose materiality.
3. **Stopping rule last.** Once the estimand and threshold exist, observed
   propagation/autocorrelation can inform a horizon or sequential stopping
   rule. Desired confidence and tolerated missed effect remain operator choices.
   Today there is one transition, so the data cannot estimate a propagation
   distribution or justify a horizon.

Thus the first required decision is the **estimand**. A bounded replay can be
useful before a statistical sampling plan—“did either branch diverge within
H?”—but H must be reported as a bound, not disguised as proof of eventual
equivalence.

## Current gates

No replay or tick ran. Canonical unchanged checks:

```sh
bb -cp . checks/c130_immediate_option_measurement.clj
bb -cp . checks/preemptive_absence_coercion_lint.clj
make workspace-gate
```

Absence remains seven live decision sites. Gate status is reported as found in
the delivery result.
