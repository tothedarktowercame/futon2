# WM-13 terminal-support drift adjudication

The apparent drift is fourteen attempt-close outcomes versus twelve terminal
dispositions. The two excluded kinds are:

- `:historical-verification-awaiting-validation`
- `:historical-verification-refused`

They are declared as valid attempt-close outcomes by
`src/futon2/aif/full_loop_cohort.clj:28-38`, but are explicitly subtracted by
`non-disposition-outcomes` before the machine-C organization support is built
(`src/futon2/aif/ruled_outcome_c.clj:26-36`). The first records an admitted
historical repair waiting for validation, not a predicted flight result: the
runner constructs that administrative transition and throws its typed outcome
at `src/futon2/aif/full_loop_runner.clj:4092-4148`. The second is emitted when
that historical-verification port or transition is absent/malformed
(`full_loop_runner.clj:4092-4123`).

**Adjudication: their absence from terminal-disposition C is correct, not a
support gap.** Both remain legal close-checkpoint classifications, but neither
is a predicted terminal flight disposition and neither receives a preference
mass. The organization carrier and seed must therefore remain the derived
`outcome-kinds − non-disposition-outcomes` set of twelve, while close validation
continues to recognize all fourteen. The machine-model contract already states
this distinction and the observed `14 − 2 = 12` count
(`holes/labs/wm-contract/CONTRACT-machine-model-v1.md:42-54`).

The executable controls now pin the distinction rather than comparing the
twelve-wide seed directly with the fourteen-wide close vocabulary:
`holes/labs/wm-contract/f10_runtime_fold_check.bb:63-116` derives both sets,
checks the two exclusions by name, and compares the Lean carrier with the
disposition set. The focused source test independently asserts disjointness
between the two administrative outcomes and the seed support
(`test/futon2/aif/ruled_outcome_c_test.clj:33-42`).

This adjudication does not revive the foreclosed processual/terminal bridge and
does not authorize either administrative historical-verification state as a
disposition-model outcome.
