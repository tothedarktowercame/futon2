# Row 22 E5: R15 slow state to R6 candidate shaping

The verifier reads five independently pinned, strict one-form UTF-8 EDN
sources: a fixed expected context, the complete unshaped occurrence domain,
slow-state and weight-table authority, claimed shaped output, and independent
R13 depth authority. All
sources must share model revision, run, tick, and isolated scope.

The fixed context binds the typed model/revision/run/tick, complete ordered
unshaped occurrences/actions, slow mode, exact table, and table authority. The
authority has the canonical `strategic-modes` id and a meaningful revision.
The weight table must be nonempty and equal the actual
`temporal-hierarchy/mode-prior-weights` result for a recognized resolved mode.
Weights must be finite and positive. Every unshaped occurrence has an explicit positive prior and
finite step cost. The verifier calls the actual `apply-slow-prior` function and
requires EDN value equality with the claimed full output. This is not a claim
that alternate EDN serializations have identical bytes; each input's retained
SHA-256 separately pins its serialized bytes. For recognized
classes, `prior' = prior * weight` and `delta' = delta - ln(weight)`; the base
delta is retained. Unrecognized classes remain unchanged, matching production.
IEEE `double` and `Math/log` semantics are claimed, not exact real arithmetic.

Occurrence order and identity, complete actions, and semantic duplicates are
preserved. Depth is resolved separately and must be repeated unchanged by the
shaped record. Changing slow mode with fixed depth can change shaping; changing
depth with fixed mode cannot change the shaping table. This edge does not
derive R13, score/select a candidate, use policy-precision gamma, or implement
E6 forward/feedback.

The future runtime seam is immediately after the complete R6 occurrence domain
and R15 slow state are captured, and before R6 scoring. This packet does not
wire that seam. Production refuses until those four sources have independently
owned production retention and pins.
