# C107 — what `clojure -T:build ci` reaches

Date: 2026-08-31

## Source-defined gate

`build.clj/ci` calls `build.clj/test`, deletes `target`, copies `resources` and
`src`, compiles `ants.clj-ants-aif`, and builds the uberjar.  `test` constructs
the `:test` basis and runs:

```sh
java ... clojure.main -m cognitect.test-runner
```

The `:test` alias adds `test` and Cognitect test-runner.  There is no explicit
War Machine check list in `build.clj`; reachability is entirely through test
namespace discovery.

## Check namespaces reached today

The suite directly requires these nine namespaces:

| Check namespace | What the suite actually establishes |
|---|---|
| `contract-lint` | Snapshot, live structural invariants, shape falsifiers, and strict-verdict semantics on a synthetic binding. It does **not** run live `--strict`. |
| `control-map-lint` | Snapshot, live 21-edge baseline, endpoint logic, and negative fixtures. |
| `obligation-ledger-reconciliation-check` | Live positive and stale-row mutation. |
| `preemptive-repair-lint` / `preemptive-repair-suite` | Five hard classes plus reporting-only absence coercion, and a gate-consumption mutation. |
| `r2-channel-contract` | Live era-aware corpus invariants, snapshot, mutation, and generated Lean text shape. It does not invoke Lean on the generated file. |
| `r8-f-contract` | Live era/disposition invariants, snapshot, and semantic mutations. |
| `r9-independence` | Live pinned census and false-only checker mutation. |
| `wm-operational-certificate` | Certificate logic against a committed run and clean resource fixture, including dirty-resource and undeclared-hop controls. It is not an operator-triggered live certificate. |

The six preemptive wrapper scripts are reached through the shared suite logic,
not launched as six subprocesses.  Absence coercion remains reporting-only by
the announced C81 policy.

The completed C107 gate receipts were green: futon2 1,031 tests / 6,180
assertions and futon3 248 tests / 1,518 assertions.  They ran concurrently in
the bounded testing slice, peaked at 990 and 1,020 tasks respectively, and had
zero resource events.  C107 does not change this gate.

## Checks not reached

### Candidate workspace gates, but not safe to add today

| Check | C107 positive result | Disposition |
|---|---:|---|
| live `contract_lint --strict` | 0 | Add to an announced workspace gate; suite coverage is not the live verdict. |
| `holder_check` | 0 | Add; ownership is live contract state. |
| `control_map_figure_agreement_check` | 0 | Add to a cross-repository/paper gate where the SVG and PDF checkout is declared. |
| `control_organization_check` | 1 | Do not add: its stored map SHA is `827af5d2…`, current map SHA is `64485bb0…`. This is a real stale pin requiring ORG reclassification/re-pin review. |
| `hyper_edge_exemplar_check` | 0 | Add to the cross-repository schema gate. |
| `hyper_edge_domain_range_check` | 0 | Add to the cross-repository schema gate; its 13 unknown domains remain loud findings, not failures. |
| `lane_registry_check` | 1 | Do not add during active work: it correctly found wm-nouns' completed C105 still recorded as holding. Run at dispatch/closure boundaries. |
| `fold_turn_quarantine_check` | 0 with `bb -cp src:.` | Add after recording the correct classpath; the bare canonical invocation cannot load production code. |
| `preference_stack_witness_shape_check` | 0 | Add; it is a pure committed-artifact gate. |
| `preference_stack_binding_check` | 1 | Do not add: Babashka cannot analyse the production `AtomicMoveNotSupportedException` path. It needs a JVM invocation or a narrower dependency boundary. |
| `r9_proof_receipt_check` | 1 | Do not add: it reports source-content, source-git, and import-git drift. Rebind separately; do not refresh inside gate wiring. |
| `wm_route_conformance` | 0 | Add for the committed run fixture; retain the live certificate separately. |
| `wm_runs_once_witness` | 1 | Do not add: its default expects absent `tick-run-record-2026-08-31.edn`. Decide the run-selection contract first. |

The gate would therefore become red for five independently named reasons if
these were wired wholesale.  C107 records rather than absorbs them.

### Deliberately manual or event-triggered

- `wm_operational_certificate` live CLI: requires an operator-triggered run and
  its matching resource receipt. CI tests its logic; CI cannot manufacture the
  operational event it certifies.
- `r17_generator_disposer_check`: C25 makes activation conditional on a live
  production path reaching R17. Until that trigger, its green result means
  dormant, not safe-live.
- `absence_scoring_counterfactual`: diagnostic measurement whose result is
  explicitly `:unknown`; it is not a pass predicate.
- Historical witness replayers: `ablation_exact_dyadic_witness`,
  `belief_update_check`, `belief_variance_inputs`, `cascade_diff_witness`,
  `expected_free_energy_witness`, `expected_information_gain_witness`,
  `generative_model_witness`, `log_multivariate_beta_witness`,
  `r19_stack_witness`, `r2_pinned_snapshot_witness`, and
  `r8_pinned_snapshot_witness`. Their consumers are pinned registry bindings;
  rerun them on source/fixture change and rebind in a separate commit. The live
  R2/R8 predicates are already in the suite.
- Negative modes for every check remain falsifiers for check development and
  review; a production gate runs positive modes while dedicated tests prove
  the negative controls reject.

## Single reviewer command

The only truthful all-green gate command today is:

```sh
clojure -T:build ci
```

It gates the repository test/build boundary described above, not every War
Machine operational claim. There cannot yet be one honest command for all
otherwise gateable checks: five candidate checks are currently red, two require
cross-repository checkout contracts, and the operational certificate requires
a run/resource pair produced outside CI. Chaining them while ignoring exit
codes would be a facade; chaining them normally would announce a migration that
has not been triaged.

The smallest next gate migration is a separate `wm-workspace-gate` command,
added only after the five red cases above are resolved or explicitly accepted.
It should run CI, live strict qualification, holder, figure/schema/organization,
lane-at-boundary, fold quarantine, preference binding, proof receipt, route,
and run-once checks, collecting every failure rather than stopping at the first.
The live operational certificate remains a subsequent operator gate consuming
that run's resource receipt.
