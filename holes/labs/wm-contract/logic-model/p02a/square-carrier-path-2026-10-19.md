# P02a — pre-choice missing-square carrier path

Static investigation at futon2 `1de83f63225800dd27970ce974c284206306a9d3`.
No serving JVM, click, publication, or Agency port was used.

## Finding

The ordinary strict-outer-selection path does **not** carry an outer
pre-choice missing-square family to
`[:decision :selection-certificate :target-policy-field :families]` before
phase 10.  P02's original chosen-action premise is not the reason: phase 10's
consumer can extract a need from a family failure without an action.  The
actual missing connection is the outer-policy-field to selected-outer-policy
projection boundary.

## Pinned chain

1. `src/futon2/aif/meta_live_outer_selector.clj`, `select-live`, lines
   602–616 forms `policy-field` with
   `family-selection/full-eligible-target-field`; its families include typed
   missing-square failures.  Lines 703–739 retain that field on the *outer
   receipt* as both `:policy-field` and `[:policy :family-census]`.
2. `scripts/futon2/report/war_machine.clj`, `generate-war-machine`, lines
   6653–6655 receives that outer receipt.  Lines 6792–6808 pass only
   `[:policy :selected-outer-cascade-policy]` to
   `cascade-decision`, explicitly bypassing the legacy inner selector for a
   sealed outer choice.
3. `src/futon2/aif/wm/cascade_decision.clj`, `cascade-decision`, lines
   1328–1341 calls `selected-outer-cascade-policy/decision` when that carrier
   exists; it does not project the outer receipt's `:policy-field`.
4. `src/futon2/aif/selected_outer_cascade_policy.clj`, `decision`, lines
   93–110 projects only the selected entry/action and a certificate containing
   selected-canonical-cascade, carrier, Q2/Q4/Q8.  It has no
   `:target-policy-field`.
5. `src/futon2/aif/full_loop_runner.clj`, phase selection lines 6385–6419
   retains that judgement; phase `:occurrence-square-authoring` lines
   6474–6479 calls `occurrence-square-authoring-step` with `judgement0-reading`.
   No intervening projection adds the missing field.
6. `src/futon2/aif/occurrence_square_authoring.clj`, `selected-need`, lines
   27–37 reads exactly the missing certificate path and can return a need from
   a family failure alone. `repair!`, lines 69–76, invokes it.  No chosen
   action is read.

All pins above are repository `futon2`, commit
`1de83f63225800dd27970ce974c284206306a9d3`.

## Constructed pair

`p02a-square-carrier-path-test` has two intentionally distinct fixtures.

- The direct consumer fixture places a missing-square family directly at the
  path `selected-need` reads.  It returns the target/excerpt/analysis need
  without an action.  This proves the consumer seam only.
- The projected control sends a selected carrier through the real
  `cascade-decision` → `selected-outer-policy/decision` branch (semantic
  verification is stubbed only to avoid manufacturing a sealed carrier).  Its
  result has no `:target-policy-field`, and `selected-need` returns nil.
  This is the actual projection seam, not an outer-selector integration run.

## Proposed one-turn repair handoff

P02 should add one explicit, integrity-checked carrier from the outer receipt's
pre-choice field/family census into the ordinary selected judgement before
`occurrence-square-authoring-step`, retaining target, excerpt digest and
analysis digest.  The acceptance check is a producer-path fixture: construct
an outer receipt with a missing-square family, run the ordinary selected
handoff projection, and assert phase-10 `selected-need` sees that exact
failure; a substituted target or digest must not cross.  The direct-consumer
fixture alone is insufficient.  Scope is a bounded first-family preparation
allowance, not all-target authoring.
