# U80 current `sorry` census and category boundary

Date: 2026-09-10

`checks/lean_sorry_category_check.clj` now requires the complete first
`·`-delimited docstring clause to equal a declared category. Prefixes,
suffixes, negations, and unknown wrappers are rejected. Category words in
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

The U27 lifecycle audit is a separate, dated source: `:as-of 2026-09-08`,
against contract git SHA `bf79f988b3131701ff9ed257d371cc45dd5eea5b`. The
report exposes that date and the complete recorded authority rather than
presenting its joined fields as a fresh audit. Among the same nine contract
declarations it records three `:pre-run-closable` and six `:run-gated`; its
readiness counts are six `:not-ready`, two `:witnessed-and-held-open`, and one
`:witnessed-under-flag`. The checker emits both fields per declaration and
does not translate one vocabulary into the other. In particular,
`dirichletAccumulationImportAbsent` remains currently labelled as a permanent
external attestation while that dated U27 audit types it `:pre-run-closable`.
The current witness registry separately binds the declaration to
`checks/dirichlet_accumulation_import_absence.clj`, its persisted report, and
the synthetic rejecting control; the registry row records `:result :passed`
at `2026-09-08T00:00:00Z`. The checker reports this current registry evidence
and its file-presence checks alongside the old lifecycle provenance. It does
not turn U27's stale not-bound prose into a new owner decision.

The dependency-first account is also narrower than U80's old wording. The one
current implementation refusal is `organise`; worklist row F12 carries its
construction and remains blocked. `C` is currently a deferral, carried by F10,
and `find` is carried by F11 but is not one of the nine current `sorry`
declarations. No Lean label, registry row, worklist row, or owner ruling is
changed by this correction.

The checker remains red for three concrete current-label obligations:
`enactedEqualsSelectedWhenRankOneGated`,
`policyPrecisionIsGammaFromBeta`, and
`policyPosteriorImportsPolicyF` are currently labelled permanent external
attestations but have neither a docstring checker citation nor a passing
registry witness with an executable check and rejecting control. The other
three current permanent attestations have executable witness evidence. This note therefore does not
claim six established not-retiring bases, and it does not recreate the stale
claim of seven. Establishing or changing a not-retiring disposition requires
an owner ruling or the missing independently checked evidence.

Unknown first-clause categories and first clauses containing two recognized
categories are rejected. The negative-control runner now requires the finding
specific to its mutation; a pre-existing red finding can no longer make an
unrelated mutation appear detected.
