# R5 round 2 — answer to R14
## What R14 asked for that I can supply

- Stable candidate identity can be built from each action's `:type`, `:target`,
  and `:target-class`; policy already selects those fields for its ranking audit
  (`src/futon2/aif/policy.clj:226-232`). This is not yet a declared identity.
- I already supply rank and controller score on every included entry, computed
  under one `state` and one `opts` value per `rank-actions` call
  (`src/futon2/aif/efe.clj:844-862`).
- I can expose the existing score decomposition rather than only the aggregate.
  The current result explicitly separates G-core from controller augmentation
  (`src/futon2/aif/efe.clj:726-735`), but a stable schema/version still needs to
  be declared.
- I can make empty admitted input a tagged absence before R14 receives it. The
  current function already produces `[]` for empty input
  (`src/futon2/aif/efe.clj:844-862`); the missing work is preserving the cause:
  no candidates, all unsupported, or scoring failure.

## What R14 asked for that I cannot supply

- I cannot currently guarantee every score is finite. Search for `finite?`,
  `isNaN`, `isInfinite`, and `Double/is` over `efe.clj` found no validation;
  `rank-actions` simply sorts `compute-efe` results by `:controller-score`
  (`src/futon2/aif/efe.clj:853-862`). A fail-closed numeric validator must be
  added before that guarantee is honest.
- I cannot supply a scoring-configuration identity or idempotency key. `opts`
  is threaded into every computation but is not named or digested
  (`src/futon2/aif/efe.clj:853-862`).
- I cannot presently deliver support exclusions to R14. `rank-actions` stores
  them only as vector metadata (`src/futon2/aif/efe.clj:855-862`), while the
  production runner maps and filters the vector before `select-action`
  (`scripts/futon2/report/war_machine.clj:4488-4518`), producing new vectors
  that do not preserve that metadata.

## Where R14's picture of me is wrong

R14 says it can report whether R5's ordering selected the live action or was
only counterfactual, then cites `:habit-authority :counterfactual-only`. That
field describes the habit/temperature-adjusted ordering, not R5's controller
order. In the live strategic branch, `chosen` is the first non-no-op entry of
my already sorted vector (`src/futon2/aif/policy.clj:247-253`), while the
temperature-dependent order is stored separately. The honest receipt is:
`:controller-order-authority :live` and `:temperature-order-authority
:counterfactual-only`.

I accept R14's need for a sensitivity witness, but R5 cannot provide it alone:
R14 owns the gain and temperature functions. R5 can provide one immutable
score field; R14 should evaluate that exact field at two gains and report
whether distribution, ordering, abstention, or nothing changes. In today's
strategic branch the expected finding is that the temperature-dependent
distribution/order changes while the selected action does not.
