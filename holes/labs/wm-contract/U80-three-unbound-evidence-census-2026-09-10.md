# U80 — evidence census for the three unbound current attestations

Date: 2026-09-10

This is a read-only census of committed evidence. It does not run the War
Machine, refresh an audit, change an owner disposition, or claim absence beyond
the checked contract sources, witness registry, checks, retained run evidence,
and rulings. The category checker currently reports these declarations because
their Lean docstrings cite no checker and `checks/witness-registry.edn` has no
row keyed by their declaration name.

## `enactedEqualsSelectedWhenRankOneGated`

The declaration remains `:run-gated / :not-ready` in the dated U27 audit. Its
required observation is one persisted record joining a rank-1 selection that
passes its own act gate to the action actually enacted. U27 records that the
antecedent has never occurred and that the inspected 2026-09-01, 2026-09-02,
and S4 traces contain zero `:realized-outcome` fields. S-stage shadow records
cannot establish enactment by construction.

There is nearby executable and recorded evidence, but it does not discharge
this proposition. `test/futon2/report/war_machine_test.clj` exercises ranking,
selection, gate, and policy paths; C527 records the F9 stepped-run test as
green. The retained S5 and 2026-09-04-re5 traces are digest-pinned by
`runs/F2-run4-readiness/READINESS.edn`. Neither supplies the missing enacted
half. The concrete repair path is therefore run-gated: after an authorized run
produces the stated joined event, add a declaration-keyed checker and registry
binding with a frozen receipt and a negative fixture that changes the enacted
action after the rank-1 gate passes. Existing component tests cannot be cited
as that event.

## `policyPrecisionIsGammaFromBeta`

Joe's J10 disposition and U47 correction are recorded in
`C501-h3-h4-falsifier-correction.md`. The declaration remains
`:run-gated / :not-ready`. The production wiring and focused unit coverage
exist: `variational-temperature-opts-carry-beta-and-its-source-test` in
`test/futon2/report/war_machine_test.clj` checks that variational mode carries
beta and its provenance. This proves the local option construction, not the
required persisted event.

The retained evidence explicitly misses that event. U47 reports 18
`:tau-source` values in `data/wm-trace/wm-trace-2026-09-01.edn`, all
`:selection-gain-only`; the S3 artifact is documented as a replay, and its one
live tau=beta tick used the write-suppressing preflight in
`run8_s3_preflight.clj`. The concrete repair path requires a writes-enabled,
authorized tick under `FUTON_WM_TAU_MODE=variational-beta-gamma`, persisted
with both `:tau` and the carry-beta `:beta-source`, followed by a frozen-record
checker, a mutation that breaks that provenance join, and a declaration-keyed
registry entry. No current fixture can honestly be rebound as that record.

## `policyPosteriorImportsPolicyF`

This declaration already has retained positive evidence on the flagged path.
J10/U47 corrected its falsifier and classified it
`:run-gated / :witnessed-under-flag`. The frozen
`runs/2026-09-01-s4/wm-trace-s4.edn` record has three of four ticks with
`:f-pi-posterior {:status :present, :applied? true}` and per-candidate F values;
the fourth records typed incomplete coverage. Focused tests in
`test/futon2/report/war_machine_test.clj` cover default-off behavior, complete
identity joins, incomplete and ambiguous coverage refusal, and flagged-path
preconditions.

The owner disposition still requires a default-path persisted record carrying
the term, or a separate ruling that makes the flag default-on. The existing S4
record must therefore remain identified as flagged-path evidence. A truthful
near-term binding can register a checker over S4 only if its evidence scope is
explicitly `flagged-path witness, held open`; its negative fixture should
remove or misjoin the per-candidate F payload. Such a binding would remove the
checker-corpus citation gap while leaving the Lean proposition open. Closure
still depends on the default-path run event or the already-described ruling.

These are three different states: one lacks the enacted event, one has wiring
but lacks a persisted beta-derived-tau event, and one has a persisted flagged
event but lacks its default-path closure event. None calls for relabelling a
declaration from the evidence inspected here.
