# Delete the outer-selection code (Joe, 2026-10-28)

Joe's ruling: there are no inner policies until a task has been selected; the
outer loop is the same loop with a different policy, and its policies are
task-SELECTION policies drawn from Cascade Live (see
`futon3/library/cascades/select-tasks-from-cascade-live.flexiarg`). Code that
selects a task by building and scoring tactical families for every eligible
target, or that runs a separately built "outer" scoring machine, is wrong.
Joe: "we're not going to build a new solution until that garbage has been
deleted, because otherwise we might get confused and imagine that the old
garbage is actually a viable solution." Seats that ran this code
(war-machine, wm-full-loop, wm-author, wm-reviewer, wm-repair-reviewer) are
removed with it. Git history keeps everything; nothing here is archived in
the tree.

## Namespaces to delete (src + their tests)

Selection by scoring tactical families across targets:
- futon2.aif.meta-live-outer-selector — builds families for all eligible targets, seals the menu, scores it
- futon2.aif.wm.family-selection — task-local families used as outer menu entries
- futon2.aif.outer-canonical-g, futon2.aif.outer-canonical-selection
- futon2.aif.selected-outer-cascade-policy — handoff from that selector
- futon2.aif.q2-construction, futon2.aif.q8-frontier — Q2/Q8 coverage of that selector's menu

Separately built outer scoring / belief machinery:
- futon2.aif.outer-policy-objective, outer-policy-evidence, outer-policy-evidence-provider, outer-policy-provider-runtime
- futon2.aif.outer-target-score, outer-target-belief, outer-learning-runtime
- futon2.aif.outer-objective-certificate, outer-objective-sut
- futon2.aif.outer-cascade, outer-loop, outer-task-selection
- futon2.aif.outer-selection-trace

Six-slot META templates (never based on Cascade Live):
- futon2.aif.meta-outer-policy, meta-policy-constructor, meta-adapter-discovery, meta-decision-manifest
- futon3 library/meta/meta-outer-policy-cascade.edn and scripts/check-meta-outer-policy.clj

Dependents that exist only to serve the above (delete, after checking each):
q9-factorization, q10-arrangement-census, q8-frontier-certificate,
integrated-controlled-sut, assessed-policy-basis, wm.frozen-cascade-steps,
scripts/wm_frozen_selection_step.clj, scripts/rank_with_readings.clj,
scripts/meta_slot_census.clj, scripts/extended_four_numbers.clj,
scripts/diamond_four_numbers.clj, scripts/consumption_four_numbers.clj,
scripts/wm_run_facts.clj (if it only reads selector receipts).

Lean (mathlib4 DarkTower): OuterCascadePolicyObjective.lean,
Proof2/OuterSelectionCanonicalG.lean, and the Requirements.lean clauses that
equate a menu of tactical families with compared policies (Q2/Q4/Q8 outer).

## Kept

- The inner loop: full-loop-runner, cascade execution, cascade-decision's
  inner part, flight code, the shared G (`cascade-coapply-g`, Lean
  `GOverCascades`, `CascadeEFE*`). Inner G is right; it was used at the wrong level.
- Repair: meta-injury-*, meta-self-heal-*, meta-medic-*, meta-repair-coordinator, meta-terminal-injury.
- Readers of the task field: meta-field-observation, ticket-readiness, mission-registry.
- meta-pipeline-selector and outer-learning-manifest: KEPT for now, flagged for
  the next pass (the first reads the pipeline cascade snapshot, which may be the
  Cascade Live input; the second admits run records). Decide after deletion.

## After deletion

The full loop has no outer selector. A click that needs one must refuse with a
typed absence (`{:absent :no-task-selection-policy}`), not fall back to anything.
The task to work on is supplied by the caller until the Cascade Live selection
pattern exists.

## Narrowed 2026-10-28

claude-11 narrowed the deletion scope after reviewing retained inner callers:

1. Keep `futon2.aif.wm.family-selection` and its inner-path tests. Keep
   `select-over-families`, `full-eligible-target-field` and `field-census`
   where used for the selected task's reading families. After removing the
   outer consumers, delete only functions with no retained caller in futon2
   `src`/`scripts` or futon3c `src`; verify the candidate dead-function list
   by searching rather than assuming it.
2. Keep `q10-arrangement-census` and its route test: they check the selected
   task's constructed policies.
3. Keep `scripts/wm_run_facts.clj`. Remove only outer-selector/outer-receipt
   sections; retain Q4, Q6, interpretation-order and absence diagnostics.

For other mixed namespaces, retain functions used by a retained inner-path
caller and remove functions whose callers are all being deleted. A conflict
between this rule and deletion of the outer machinery must be reported before
changing the contract.

Q10 moved from keep to delete: its helpers have no independent inner caller;
its certificate and route fixtures depend on the deleted outer Q2/Q8 receipts.

Implementation boundary notes:
- Retain only Q2's `construction-census` and its helpers: cascade-decision
  still records the selected task's inner construction. The outer certificate,
  record verifier and outer-actions producer are deleted.
- Ticket readiness still needs field inputs. Its observation-only helpers move
  from the deleted selector to `ticket-readiness-inputs`; they do not select.
- Delete `target-reading-preparation`: after removing the pre-choice batch in
  runtime it has no retained production caller. Keep selected-target reading
  repair for the supplied task.
- Historical receipt readers used by retained learning/terminal consumers stay;
  they neither construct a menu nor select a task. The two futon3c browser
  entrypoints return the typed no-task-selection-policy absence.
