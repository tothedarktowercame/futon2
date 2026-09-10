# U80 current `sorry` census and category boundary

Date: 2026-09-10

`checks/lean_sorry_category_check.clj` now reads the first `·`-delimited
docstring clause as the declaration's current category. Category words in
later clauses are retained as historical mentions and cannot classify the
declaration. This matters for `wmRunConformsToWiring`: its current clause is
`OPEN, RUN-GATED`; the later occurrence of `PERMANENT EXTERNAL ATTESTATION`
records the interpretation that its owner refused.

The live contract has 205 parsed declarations and nine literal `:= sorry`
bodies. Their current declaration categories are:

| Current declaration category | Count |
| --- | ---: |
| `DEFERRAL UNDER ORGANIZED DISCOVERY` | 1 |
| `DELIBERATE IMPLEMENTATION REFUSAL` | 1 |
| `OPEN, RUN-GATED` | 1 |
| `PERMANENT EXTERNAL ATTESTATION` | 6 |

The U27 lifecycle audit is a separate source. Among the same nine contract
declarations it records three `:pre-run-closable` and six `:run-gated`; its
readiness counts are six `:not-ready`, two `:witnessed-and-held-open`, and one
`:witnessed-under-flag`. The checker emits both fields per declaration and
does not translate one vocabulary into the other. In particular,
`dirichletAccumulationImportAbsent` remains currently labelled as a permanent
external attestation while U27 types it `:pre-run-closable`. That disagreement
is visible rather than resolved here.

The dependency-first account is also narrower than U80's old wording. The one
current implementation refusal is `organise`; worklist row F12 carries its
construction and remains blocked. `C` is currently a deferral, carried by F10,
and `find` is carried by F11 but is not one of the nine current `sorry`
declarations. No Lean label, registry row, worklist row, or owner ruling is
changed by this correction.

The checker remains red for four concrete current-label obligations:
`enactedEqualsSelectedWhenRankOneGated`,
`dirichletAccumulationImportAbsent`, `policyPrecisionIsGammaFromBeta`, and
`policyPosteriorImportsPolicyF` are currently labelled permanent external
attestations but name no executable checker path. The two other current
permanent attestations name existing checkers. This note therefore does not
claim six established not-retiring bases, and it does not recreate the stale
claim of seven. Establishing or changing a not-retiring disposition requires
an owner ruling or the missing independently checked evidence.

Unknown first-clause categories and first clauses containing two recognized
categories are rejected. The negative-control runner now requires the finding
specific to its mutation; a pre-existing red finding can no longer make an
unrelated mutation appear detected.
