# C138 — status populations have distinct sources

Date: 2026-08-31

The C132 report mislabeled the contract-lint classification of eleven contract
holes as `:=sorry-classification`.  That field described the right values under
the wrong population name.  The report now renders both populations:

```text
holes=11 source=contract_lint-live-report
hole-classification={conformant 7, refused-implementation 3, witnessed 1}
:=sorry-terms=6 source=lean_sorry_category_check
sorry-classification={DELIBERATE IMPLEMENTATION REFUSAL 3,
                      PERMANENT EXTERNAL ATTESTATION 3}
```

Five contract holes have proved, witnessed bodies, so eleven holes and six
literal `:= sorry` terms are compatible facts rather than rival counts.

`lean_sorry_category_check` now emits `:sorry-category-counts` over only the
declarations containing `:= sorry`; its broader `:category-counts` continues to
include witnessed-obligation labels on proved declarations.  The renderer does
not derive either population from the other.

The source-independence control is:

```sh
make status-control
```

It accepts a contract-lint hole population and a Lean-checker sorry population,
then injects the exact C132 defect by substituting the hole population for the
sorry population.  The substitution is rejected.  The workspace gate runs
this control as `c138-status-population-sources`, so it is not a dormant check.

The demonstrated full status still rendered all sections and exited 1 for its
named red components.  Futon3 remained green.  The first C138 futon2 run
overlapped unrelated full-loop work and honestly reported five failures; a
settled green suite receipt is recorded separately from that finding rather
than overwriting it.

