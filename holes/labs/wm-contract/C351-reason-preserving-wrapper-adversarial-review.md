# C351 — adversarial review of C342 reason-preserving wrappers

Date: 2026-09-01. Reviewer: `wm-evidence`. Reviewed repair `8349ebf`.
Assessment only; no wrapper or fixture was changed in the workspace.

## Verdict

The new three-valued exit contract works, but the positive Lean baseline is
vacuous in ten of twelve wrappers. C342 therefore prevents an already-invalid
**data fixture** from masquerading as a successful mutation rejection, but does
not establish the positive Lean witness it says it establishes.

The stated population is also arithmetically wrong: twelve wrappers expose
**sixteen** distinct negative modes, not fifteen. Cascade has four, observation
kernel has two, and the other ten wrappers have one each. There are no “other
three” within a fifteen-mode population to classify; C342 does not enumerate a
separate three-mode remainder.

## Exit-contract controls

All controls ran in temporary shared clones with the real workspace untouched.

- **Intended rejection:** all sixteen current negative invocations returned
  exit `0` and their named rejection message.
- **Invalid baseline:** changing each authoritative fixture's schema before
  invocation made all sixteen return exit `1` and `BASELINE-INVALID (control
  reason not established)`. None printed its named negative-control pass.
- **Mutation slipped:** neutralising each wrapper's in-memory mutation while
  retaining its valid baseline made all sixteen return exit `2` and `mutation
  slipped`.

Thus exits 0, 1, and 2 are independently reachable and distinguished at every
repaired negative mode. The original C325 failure—an unrelated invalid data
baseline returning the named negative success—is closed.

## Counterexample: empty positive Lean witness

Ten wrappers define their Lean-positive baseline only as exit 0 from `lake env
lean FILE`. An empty Lean file exits 0. Redirecting only the positive witness
path to an empty file, while leaving the fixture and intended negative mutation
unchanged, produced exit `0` and the named negative PASS for every affected
mode:

| Wrapper | Affected negative modes |
|---|---:|
| `ambiguity_witness.clj` | 1 |
| `belief_state_witness.clj` | 1 |
| `channel_witness.clj` | 1 |
| `expected_free_energy_witness.clj` | 1 |
| `expected_information_gain_witness.clj` | 1 |
| `log_multivariate_beta_witness.clj` | 1 |
| `observation_kernel_witness.clj` | 2 |
| `predictive_outcome_risk_witness.clj` | 1 |
| `fold_witness.clj` | 1 |
| `have_want_arrow_witness.clj` | 1 |

That is ten wrappers and eleven modes. The data mutation is still rejected for
the intended data reason, so the exit-0 sentence is not false about that narrow
predicate. It is false about C342's stronger ordered baseline claim that “the
positive Lean witness passes”: absence of all Lean witness content is accepted
as passage. This is the same empty-population acceptance shape the repair was
meant to exclude, one layer deeper.

The two wrappers not exposed to this counterexample are:

- `ablation_exact_dyadic_witness.clj`, whose declared baseline is data-only;
- `cascade_diff_witness.clj`, whose baseline validates the data predicate and a
  pinned source digest rather than compiling a positive Lean file.

For `fold` and `have-want-arrow`, the exact guarded negative Lean fixtures still
reject correctly; the missing part is positive-witness presence. For the other
eight affected wrappers, exact fixture predicates independently preserve the
data-mutation reason, but the compiled Lean side is still not witnessed by a
named declaration or content pin.

## Population accounting finding

C342 says “twelve wrappers” and separately describes cascade's four modes and
observation's two. Those facts imply sixteen unique modes:

```text
10 single-mode wrappers + 4 cascade modes + 2 observation modes = 16
```

Aliases such as `--negative-control` for cascade O2 or observation
normalisation do not create additional modes. No reading produces fifteen, and
C342's phrase “the rest of the audited controls” supplies no names from which a
three-mode untouched population can be independently reconstructed.

## Inventory

The delivery inventory command is:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
