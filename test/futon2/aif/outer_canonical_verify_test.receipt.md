# O3-3 frontier replay receipt

`outer-canonical-g/produce` was called twice on the pinned b5 menu before the
change; equality returned `true`.  The red control is the prior O3-2 verifier:
a carrier resealed after a changed frontier G and matching recomputed selection
passed because it replayed only the argmin.  The new check rejects that and the
horizon, count-table/digest, class-model-id, and scored-to-refused substitutions
before choice replay.

Command: `clojure -X:test :nses '[futon2.aif.outer-canonical-verify-test]'`

Result: `Ran 1 tests containing 6 assertions. 0 failures, 0 errors.`

The production-carriers menu has no canonical tie, so the separate malformed
draw control is not represented here.  A changed draw with a correspondingly
changed tied action is an honest distinct recorded draw and is not meant to
refuse.
