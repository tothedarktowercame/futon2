# L5-4L-3b — generated class-grain witness

Bases: futon2 1338ffeed; mathlib4 72af54b01a. No merges or live operations.
The emitter imports action-identity for production's canonical prediction digest.
Its g-units mirrors outer-select's BigDecimal(double), six-place HALF_EVEN.
Class digest is plain SHA256(pr-str [prediction-digest g-units]), exactly as
prediction-classes computes it. My hand-adapted fixture incorrectly used
action-identity/digest for the outer pair. This corrects that identity mismatch.

## Classes

- giraffe-order and repair-click: digest
  c159fe5a90a6004311f80c08b56c9c99ad1f2b227aa666b82d8659758d2ac04a,
  Lean ID 870777911698415620, G units 3484093.
- uncertain-design: digest
  abc155c5358c07c626a0139606cbf61ef46b7d244875a1f9b969baa0194a94d5,
  Lean ID 773516722280382588, G units 2994922; chosen.
- policyG has two rows. All 626 task entries, picks, steps, prediction IDs,
  terms, exact rational G values and six theorem statements are preserved.

## Exhaustive regenerated-file diff versus hand adaptation

1. Header becomes generated/do-not-hand-edit, removes manual adaptation
   instructions, and records actual input witness sha 392e9151... instead of
   old original-generation sha 4a3461d7... plus supplemental provenance.
2. Header states precise production prediction/class digest functions and the
   two-class membership. Typed .unreadNew note remains.
3. c1/c2 class IDs change from 461015774054426131 to 870777911698415620;
   c3 from 303582455414473496 to 773516722280382588, correcting the hash function.
4. witness.chosen and witnessRun.currentDecision use corrected c3 class ID.
No other differences; no generated-file hand edits.

## Reproduction and checks

Run bb holes/labs/wm-contract/witnesses/emit-selection-grounding-lean.bb with:
- WITNESS_DIR: holes/labs/wm-contract/witnesses
- PASS_EDN: /home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5/pass.edn
- OUTPUT: /home/joe/worktrees/mathlib4-l5-4l-3b/DarkTower/WarMachine/SelectionGroundingWitness.lean

Second emission to ~/notes/l5-4l-3b/second.lean is byte-identical (cmp exit 0).
Output: 65,611 bytes; SHA256
7bd9b6ecd7db087df383cbac9146470f5d66a979957375a19d1a3e3616489181.

Both grounding modules built (exit 0); all six required theorems remain
native_decide proofs; zero sorry. Real copied .lake, not a symlink.
Build log ~/notes/l5-4l-3b/build.log SHA256:
8843e1d28bcb637a698350918acb96d6c71b7677e3920a08c61db96bf09e0c6b.

Production parity: ~/notes/l5-4l-3b/parity.bb reads/evaluates the exact g-units
and prediction-classes defn forms from outer_select.clj using real action-identity,
asserting both IDs. Exit 0; production-parity.log retains full digests/memberships.
Loading the whole outer-select namespace under bb first failed on missing
clojure.data.json; the pure-form check avoids unrelated runtime dependencies.
This is not a full namespace load test.

Negative: a scratch witness substituting :all-policies-already-satisfied for
:giraffe/unread-new fails emission by name before output creation (negative.log).
No registered wrapper negative modes exist for this emitter in workspace-gate/
census. Existing grounding negative theorems compile with the module.
Pass sha and exact 609+17 partition remain checked.

Term differences from recorded double G: c1/c2 +2.410226107585345E-8,
c3 -1.4128440417593424E-7. No numeric calibration changes.
