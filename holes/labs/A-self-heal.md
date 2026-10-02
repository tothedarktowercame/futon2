# A-self-heal — repair one War Machine click under its debugger

**Kind:** approved-algorithm candidate.  **Status:** observed procedure, written
down 2026-10-02.  **Scope:** War Machine control-path injury; not ordinary
mission implementation.

## What this records

This is the algorithm actually used during the controlled click
`2026-10-02-afd0e890-46e7-4152-a0e6-68b78759e248`.  The click was attached to
the restart debugger, stopped at typed failures, and left paused while each
failure was diagnosed.  A separate agent produced one bounded repair; the
controller independently inspected its evidence before hot-loading or using a
restart.  The procedure was already acting as self-heal, but was not available
to the META selector as an A item.

The injury was not “one mission failed.”  It was:

> A War Machine click cannot yet complete while preserving a reason-bearing
> selection, exercised-or-explained loop-node receipts, an enacted result, a
> trace, and a terminal run record suitable for review.

Observed manifestations included `:required-loop-node-unexercised`, lost
selection/redecision context, completed work remaining selectable, absent
outer-selection receipts, and terminal `:universe-not-admitted` abstentions.
The debugger stop added in `7bc98c8ee` retains the latter rather than allowing
it to look like a successful no-op.

## META slot filling

```clojure
{:task-kind :algorithm
 :target "A-self-heal"
 :next-move :run-algorithm
 :repairs-capability :wm-click-completes-with-reviewable-receipts

 :entry-maneuver
 {:inspect :debugger-stop-registry
  :zero-stops :permit-one-click-launch
  :one-stop :adopt-existing-run-as-current-patient
  :multiple-stops {:result :refuse
                   :reason :self-heal-debugger-frames-stacked}
  :launch-precondition :debugger-stop-registry-empty}

 ;; The observed campaign did not declare numeric time or token budgets.  Do
 ;; not invent them retrospectively: the runtime must bind both before this
 ;; template becomes a fully filled policy.
 :resource-envelope
 {:time-budget-ms :bind-at-launch
  :token-budget :bind-at-launch
  :click-budget 1
  :repair-width :one-bounded-commit-per-stop
  :author-seat "codex-16"
  :reviewer-seat "codex-10"}

 :evidence-channel
 {:source {:run-id "2026-10-02-afd0e890-46e7-4152-a0e6-68b78759e248"
           :click-id "wm-click-72d1881d-4c21-4ca3-8774-49d757e9e8a4"
           :attempt "attempt-002"}
  :locator {:kind :debugger-stop-and-terminal-run-record}
  :repair-evidence [:explicit-commit :focused-gates :independent-review]}

 :stopping-rule :debugger-stop
 :exit-maneuver
 {:restart :abort
  :cardinality :exactly-once-per-unresolved-stop
  :postconditions [:terminal-run-record-present
                   :run-absent-from-debugger-stop-registry]}
 :rearm-observation
 {:kind :counted-controlled-click
  :count 1
  :requires
  [:terminal-outcome
   :outer-task-selection-present
   :selection-receipt-present
   :loop-nodes-present-or-reasoned-bypass
   :selected-enacted-identity-present
   :trace-written
   :terminal-output-summary-present]
  :forbids
  [:required-loop-node-unexercised
   :required-checkpoints-missing
   :universe-not-admitted
   :unknown-outcome
   :successful-no-op]}}
```

The two `:bind-at-launch` values are deliberate typed holes.  The historical
procedure was bounded by one paused click and one repair commit at a time, but
did not meter elapsed time or tokens.  META construction must supply positive
numeric values from the current resource state; until then this algorithm is
available as a filler but no concrete policy instance is admissible to G.

## Algorithm distilled from the run

0. Before attachment or launch, read `futon2.aif.wm.debugger/stopped`.
   - With zero stops, the algorithm may proceed to its one-click launch.
   - With exactly one stop, launch nothing: adopt that run as the current
     patient and continue at step 3 using its retained condition.
   - With multiple stops, refuse as `:self-heal-debugger-frames-stacked` and
     require explicit operator disposition; do not guess which frame owns the
     machine.
   A previously stopped run may be repaired and retried, or explicitly
   aborted.  In either case a new click is forbidden until the registry has
   been observed empty.
1. If step 0 found no stop, attach `futon2.aif.wm.debugger` before launching
   the one authorised click.  Record the run/click identity, verify the
   attached state, and immediately re-check that the stop registry is empty.
2. Run until either the click reaches the re-arm observation or a typed
   debugger condition stops it.  A bypass without an exercised branch must
   carry a specific rationale; `:unknown` is itself a stop.
3. Preserve the stopped condition, selected target, decision, receipts and
   restart choices.  Do not start another click and do not turn the stopped
   task into a hand-written success.
4. Diagnose from the retained evidence.  Distinguish a code defect from a
   source-lifecycle defect and from a missing semantic decision.
5. Dispatch exactly one bounded repair to the author seat.  Require an
   explicit commit and focused positive/adversarial gates.  The repair may not
   launch a replacement click.
6. Have the reviewer seat inspect the diff and rerun a proportionate gate.
   Hot-load canonical namespaces only after the repair is sound.
7. Choose a debugger restart explicitly:
   - `:retry` repeats only the repaired phase in the same attempt;
   - `:abort` preserves the ordinary terminal failure and discharges the
     current debugger frame;
   - no restart is taken when semantics are genuinely undecided.
8. Repeat from step 2.  Each new stop is a new observation and may select a
   different repair pattern; it is not evidence that the previous repair was
   useless.
9. Before returning, handing off, or permitting another click, deliver
   `:abort` exactly once to every unresolved stop owned by this algorithm.
   Wait for its terminal run record, then verify its run ID is absent from
   `futon2.aif.wm.debugger/stopped`.  An undecided semantic issue may prevent
   `:retry`, but it must not leave a suspended debugger frame behind.  Failure
   to discharge a frame is `:self-heal-debugger-frame-undischarged`.
10. Re-arm ordinary M/E/T support only when one controlled click satisfies
   every required receipt in `:rearm-observation`.  Otherwise leave the
   machine injured with a terminally recorded failure.

## Receipts from the observed procedure

- `c7a834c1e` made unexercised required loop nodes stop the line;
  `54539af85`, `e1814f68b`, and `f2979dc78` retained increasingly precise
  decision and abstention context.
- `92a930316` retained post-ask redecision failure evidence instead of
  collapsing it to `:required-checkpoints-missing`.
- `eaa94a82f` carried the selected target and ask into automatic repair
  findings; `20db9fb2e` repaired the exercise-receipt projection for the real
  C1 decision.
- `ca36d83b9` excluded completed requisitions from current support and
  `3f7b3cf45` retained the outer-selection receipt in terminal records.
- `7bc98c8ee` made terminal selection abstention a restartable debugger stop.
- The first stop then exposed a lifecycle defect in `E-close-S6`; its source
  was marked completed in futon5a `e4cb486110d5eacabad96973f607a89cc164dda3`
  and the same attempt was retried.  The second stop selected
  `E-apm-halftime-pre-go-live-D`; because its acceptance prose had no
  authoritative completion observation, the stop was aborted rather than
  retried.  The resulting terminal record has SHA-256
  `dfac8e2be03d0ed865bc1dabf4858aa4e9eed9587f166f33a29bdb278cb6a87e`,
  and the debugger stop registry was verified empty.

These receipts establish the algorithm's provenance.  They do **not** satisfy
its re-arm condition: no post-repair click has yet completed with the complete
receipt set.

## Failure and escape conditions

- No matching A item: META selection returns typed
  `:no-matching-self-heal-algorithm`; it must not fall back to M/E/T while the
  injury remains active.
- No numeric resource binding: refuse the concrete candidate before G.
- Repair cannot be reviewed or hot-loaded: retain the stop and end the
  algorithm as `:typed-blocker`.
- Re-arm probe fails: keep `:wm-click-completes-with-reviewable-receipts`
  injured and use the new receipt as the next self-heal observation.
- Abort delivery or terminal recording fails: refuse algorithm completion as
  `:self-heal-debugger-frame-undischarged`; do not start another click on top
  of the suspended frame.
- The entry check finds multiple existing stops: refuse as
  `:self-heal-debugger-frames-stacked`.  Preserve every stop and require an
  explicit operator choice; starting a new self-heal click is forbidden.
