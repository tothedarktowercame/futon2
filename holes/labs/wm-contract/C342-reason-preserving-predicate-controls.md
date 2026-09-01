# C342 — reason-preserving standalone predicate controls

Date: 2026-09-01

C325 demonstrated that ambiguity, belief-state, and channel-vocabulary
negative controls exited 0 and named their intended mutation even when an
unrelated defect had already invalidated the baseline. The workspace gate's
separate positive invocation prevented a composite false green, but each
standalone sentence overclaimed.

## Population audit

The unsafe structure was audited wrapper by wrapper. Twelve wrappers required
repair, covering the predicate/data controls plus two guarded-Lean wrappers
that likewise skipped their positive witness in negative mode:

- `ablation_exact_dyadic_witness.clj`;
- `ambiguity_witness.clj`;
- `belief_state_witness.clj`;
- `cascade_diff_witness.clj` (four mutation modes);
- `channel_witness.clj`;
- `expected_free_energy_witness.clj`;
- `expected_information_gain_witness.clj`;
- `log_multivariate_beta_witness.clj`;
- `observation_kernel_witness.clj` (two mutation modes);
- `predictive_outcome_risk_witness.clj`;
- `fold_witness.clj`;
- `have_want_arrow_witness.clj`.

The first ten mutated a compound data predicate and treated any false result as
the named rejection. `fold` and `have-want-arrow` validated their data fixture
but skipped the positive Lean witness while running the guarded negative Lean
fixture. The rest of the audited controls already compute their positive
baseline in negative mode, use exact `#guard_msgs`, or compare an independently
typed negative result; they do not share this escape.

## Outcome contract

Every repaired negative invocation now establishes, in order:

1. the unmutated data predicate is valid;
2. the positive Lean witness passes, where one exists;
3. the intended in-memory mutation makes the named data predicate false, or
   the exact guarded negative Lean witness passes.

Outcomes are distinct:

- exit 0: baseline valid and intended mutation rejected;
- exit 1 with `BASELINE-INVALID (control reason not established)`: the control
  cannot attribute rejection to its mutation;
- exit 2: baseline valid but the intended mutation slipped.

The three C325 counterexamples were repeated against temporary malformed
fixtures. Wrong ambiguity schema, wrong belief-state basis, and wrong channel
schema each returned exit 1 and the `BASELINE-INVALID` sentence, never the
named negative-control PASS. All repaired positive and intended-negative
invocations returned exit 0.

Standalone exit 0 for this population is now reason-preserving. No wrapper in
the audited population needs the fallback disclaimer “standalone exit is not
evidence”; baseline-invalid invocations state that limitation themselves.
