# O3-3 frontier replay receipt

`outer-canonical-g/produce` was called twice on the pinned b5 menu before the
change; equality returned `true`.  The red control is the prior O3-2 verifier:
a carrier resealed after a changed frontier G and matching recomputed selection
passed because it replayed only the argmin.  The new check rejects that and the
horizon, count-table/digest, class-model-id, and scored-to-refused substitutions
before choice replay.

Focused verifier command: `clojure -X:test :nses '[futon2.aif.outer-canonical-verify-test]'`

Result: `Ran 1 tests containing 6 assertions. 0 failures, 0 errors.`

Required combined command:
`clojure -X:test :nses '[futon2.aif.outer-canonical-verify-test futon2.aif.outer-canonical-selection-test]'`.
Result: `Ran 6 tests containing 31 assertions. 0 failures, 0 errors.`

Review correction (claude-7): the production-carriers menu *does* tie: both
scored entries have G = -2.351395296907449, so the honest carrier carries a
draw over two candidates.  `canonical-tie-draw-is-pinned` adds the draw
control: u = 0.49 reproduces the recorded action and verifies; u = 0.9 (other
candidate), 1.0 and -0.1 refuse with `:selected-policy-canonical-replay`.
Red half checked against the 5408ebdf2 verifier: three of the five frontier
substitutions above passed it outright (nil) and two were caught only later by
`:selected-policy-entry-substitution`; three further forgeries with the
frontier pin resealed (last G +100, first G -100, last entry dropped) all
passed the old verifier and are refused by the new one.

Review run (claude-7): `clojure -X:test :nses '[futon2.aif.outer-canonical-verify-test futon2.aif.outer-canonical-selection-test futon2.aif.selected-outer-cascade-policy-test]'` -> `Ran 12 tests containing 64 assertions. 0 failures, 0 errors.`; kondo 0/0; parens OK.
