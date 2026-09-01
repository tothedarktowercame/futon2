# C356 — non-vacuous positive Lean witnesses

## Population and repair

The authoritative population is **12 wrappers / 16 distinct negative modes**:

- nine single-mode Lean wrappers;
- `observation_kernel_witness.clj` has two modes;
- `cascade_diff_witness.clj` has four modes;
- `ablation_exact_dyadic_witness.clj` has one mode.

Ten wrappers (eleven modes, counting both observation-kernel mutations) formerly
defined their Lean-positive baseline as only `lake env lean FILE` exiting zero.
An empty file therefore passed. They now share
`checks.lean-positive-witness`, which requires the source to be readable,
nonempty after comments, declaration-bearing, and free of the standalone Lean
`sorry` term before invoking Lean.

Ablation already avoids the defect because its declared baseline is an exact
dyadic data predicate, not a Lean file. Cascade already avoids it because its
baseline is the data predicate plus the pinned source digest. Both remain
unchanged.

All sixteen modes are covered; none remains unrepaired. The exit-2 mutation-
slipped branches are unchanged and were independently shown reachable for all
sixteen in C351. This repair changes only positive-baseline admission.

## Controls and invocations

With `FUTON_POSITIVE_LEAN_OVERRIDE` pointing to a real empty temporary `.lean`
file, all eleven exposed modes exit 1 with
`BASELINE-INVALID (control reason not established)`. With their real positive
sources, all eleven exit 0 with the named mutation-rejection verdict.

The unaffected modes also remain positive:

```sh
bb -cp . checks/ablation_exact_dyadic_witness.clj --negative
bb -cp . checks/cascade_diff_witness.clj --negative-o1
bb -cp . checks/cascade_diff_witness.clj --negative-o2
bb -cp . checks/cascade_diff_witness.clj --negative-o3
bb -cp . checks/cascade_diff_witness.clj --negative-o4
```

All exit 0. Source-boundary controls:

```sh
clojure -M:test -m cognitect.test-runner -n lean-positive-witness-test
```

Result: 1 test, 5 assertions, zero failures/errors. It rejects empty,
comment-only, and `sorry`-bearing sources, accepts a completed declaration, and
does not mistake the `.sorryCountNorm` constructor for the `sorry` term.

```sh
clj-kondo --lint checks/lean_positive_witness.clj \
  checks/ambiguity_witness.clj checks/belief_state_witness.clj \
  checks/channel_witness.clj checks/expected_free_energy_witness.clj \
  checks/expected_information_gain_witness.clj \
  checks/log_multivariate_beta_witness.clj \
  checks/observation_kernel_witness.clj \
  checks/predictive_outcome_risk_witness.clj checks/fold_witness.clj \
  checks/have_want_arrow_witness.clj test/lean_positive_witness_test.clj
```

Exit 0, no errors or warnings.
