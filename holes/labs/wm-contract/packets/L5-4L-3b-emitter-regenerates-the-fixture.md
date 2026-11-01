# L5-4L-3b — The witness emitter emits the L5-4L-3 grounding shape, and regeneration reproduces the fixture (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `1338ffeed`; mathlib4 darktower `72af54b01a` (your L5-4L-3 merged: typed
`ExclusionReason`, `GroundedPolicy.class`, class-grain G4, `OuterFactorizedPreference.lean`). In
L5-4L-3 you hand-adapted `SelectionGroundingWitness.lean`; the rule for that file is "generated,
do not hand-edit", so the emitter must now produce it.

## Behaviour

`futon2/holes/labs/wm-contract/witnesses/emit-selection-grounding-lean.bb`:
1. Exclusions emit `.excluded <task> .unreadNew <pin>` (the constructor you named) instead of the
   numeric reason; any drift member whose reason is not `giraffe/unread-new` refuses the emission by
   name (no other reason exists in the pinned pass today; assert it).
2. Each `GroundedPolicy` emits its `class` id = first 15 hex of sha256 of `(pr-str [prediction-digest
   g-units])` exactly as `outer_select.clj`'s `prediction-classes` keys the class (same digest of
   the prediction outcomes, same 10^6 units), so the fixture's class ids and production class ids
   agree for the same prediction and G. Document the function in the header.
3. `witnessRun`'s `policyG` keyed by class id with one row per class (the two ready-gated cascades
   share a class if their predictions and G agree; state which classes the witness has).
4. Idempotent; print the file's sha256.

## Acceptance

Regenerate `mathlib4/DarkTower/WarMachine/SelectionGroundingWitness.lean` on a mathlib4 branch
`codex11/l5-4l-3b-regenerated-witness` from `72af54b01a`; the existing theorems
(`witness_field_considered`, `witness_scored_from_terms`, `witness_agrees`,
`witness_prioritised_unverifiable`, `witness_c1_pick`, `witness_c3_pick`) must still prove, by
`native_decide`, with no hand edits. Diff the regenerated file against the hand-adapted one and
explain every difference. Build the two grounding modules green; zero sorry; log + sha in
`~/notes/l5-4l-3b/`. Emitter commit on a futon2 branch `codex11/l5-4l-3b-emitter`. No merges.
Bell claude-2 with both shas, the diff summary, and the class ids.
