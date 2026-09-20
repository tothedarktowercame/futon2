# Matched observation evidence adapter — 2026-09-20

Implementation: ec556c0d. Standalone namespace
`futon2.aif.matched-observation-evidence`; no judge/tick/existing-F wiring,
no click, no serving reload. No existing A/C/D/Q code or declarations changed.

Authority read: mathlib4 `DarkTower/WarMachine/MatchedObservationEvidence.lean`
(782d9b05da) and `PolicyRollout.lean` predictedOutcome definition.
The adapter uses the exact finite sum of mixture observation rows multiplied
by the single retained rollout belief. Matching reads the resulting retained
`:predicted-outcome`; it does not perform another rollout or prediction.

All records, including invalid inputs, carry model declarations:
`:parameter-basis :synthetic`, `:calibration-authority :none`,
`:z-semantics :per-step-redraw`. Exact probability arithmetic precedes the
floating approximation of Real.log. Positive rational underflow does not
become a contradiction (2^-2000 control is finite and approximately 2000 ln 2).

Registered warrant:
`test-registry-15ff71e5ee7bd4913ce1a41d6528ca2c7dc65a28be42ad2296627e1832afb5d4`.
Subject: `wm-f/matched-observation-evidence`.
4 tests / 52 assertions, zero failures/errors, exit 0. Spec, stdout/stderr,
runner log and closure retained alongside this note.

Constructor probes, separately:

- Missing: `{:option :none}` returns `:constructor :missing`, `:status :ok`,
  with NO `:value` key. It is not a numerical zero.
- Contradiction: received `#{:y}` (also `#{:x :y}`) has exact probability 0;
  returns `:constructor :contradiction`, `:status :refused`, retaining that
  observation and probability, with NO numeric value.
- Value: received `#{:x}` has probability 3/4 and value 0.2876820724517809;
  received `#{}` has probability 1/4 and value 1.3862943611198906.
  Both use the retained distribution `{#{:x} 3/4, #{} 1/4}` at step 1 after
  the real synthetic transition produces :x. A separate certain observation
  at step 0 returns constructor :value with zero surprisal, distinct from missing.

The complete four-outcome carrier checks both directions of contradiction
iff probability zero and value iff positive probability, observation identity,
surprisal equality and nonnegativity. Invalid option/model/prior probes remain
`:status :invalid`, with no mathematical constructor and declarations retained.
The rollout-call counter asserts exactly one invocation.

Gates: clj-kondo clean; check-parens passed for source and test. Focused tests
passed before committing, then the registry executed the committed content
once to mint the durable warrant. No Lean edits or Lean suite execution.
