# C451 — six drawn edges with no equation across them

**Date:** 2026-09-01  
**Scope:** TN-edge-review query 3a; discovery only

The two run records have identical endpoint populations. Each stores all nine
hops under `:route` and repeats six under `:route-verdict :unmapped-hops`
(`tick-run-record-2026-08-30.edn:1` and
`tick-run-record-2026-08-31.edn:1`). “Neither” below means absent from both
fields in both records.

| Drawn edge | Running-code reader / `:via` | Recorded traversal | Reading supported by the code |
|---|---|---|---|
| R8→R5 | Route instrumentation names `futon2.aif.efe/rank-actions` (`scripts/futon2/report/war_machine.clj:4536-4537`). A reader of R8's F at R5 is **not found**: `variational-free-energy` is computed at `:4450-4452`, while `rank-actions` receives `wm-state`, candidates and options at `:4536`; it calls `compute-efe` for each candidate (`src/futon2/aif/efe.clj:903-920`). The F value is only later copied to the report at `war_machine.clj:4753`. | In `:route`; not in `:unmapped-hops`, in both records (`tick-run-record-2026-08-30.edn:1`; `tick-run-record-2026-08-31.edn:1`). | **Plumbing misfiled among equation boxes.** The recorded hop is sequential control-flow instrumentation, not an F→G data dependency. R5 does not use R8's F in its score; it recomputes its EFE-core/controller score from state, action and options. This is not evidence for a departure from the formalism. |
| R2→R3 | Direct reader **not found**. `judge` reads the observation at R2 (`scripts/futon2/report/war_machine.clj:4220-4223`), forms prediction errors at R8 (`:4375-4379`), updates/attaches R7 precision (`:4382-4388`), synthesises belief events from the weighted error (`:4392-4428`), then R3's `apply-arena-belief-events` consumes those events (`:4429-4431`; function at `:204-210`). | Neither, in both records. The recorded neighbouring path is R2→R7 in `:unmapped-hops`, then R7→R3 in `:route` (`tick-run-record-2026-08-30.edn:1`; `tick-run-record-2026-08-31.edn:1`). | **Implementation/drawing shortcut for a theory path through other boxes.** R3 does not read `o` directly. The drawing shortens the implemented `o → ε → Π-weighted error → belief event` path; the implementation does not establish a direct R2→R3 edge. |
| R13→R14 | **Not found.** The only live horizon/depth value here, `wm-horizon-steps`, is derived from anticipation events (`scripts/futon2/report/war_machine.clj:4482-4487`) and passed into EFE options (`:4516-4522`), not into temperature or selection gain. | Neither, in both records. | **Plumbing/misfiled drawing, with no realised plumbing found.** No depth-to-temperature reader exists in the inspected live path, so there is no evidence for a formalism variant. |
| R14→R16 | `futon2.report.war-machine/judge` reads the result of `invoke-strategic-selection`: selected mission ids and authorization fields become `wm-decision` (`scripts/futon2/report/war_machine.clj:4596-4663`). But this is selection-result/authorization plumbing, not a read of τ, and `judge` records a decision rather than enacting it (`:4784-4790`). A τ-output reader at grounded actuation is **not found**. | Neither, in both records. The recorded edge out of R14 is R14→TRACE, in `:route` and `:unmapped-hops` (`tick-run-record-2026-08-30.edn:1`; `tick-run-record-2026-08-31.edn:1`). | **Plumbing misfiled among equation boxes.** The live seam carries a reviewed selection and authorization into a decision record. It does not establish the formal τ→action edge at R16, nor a precision-modulated-temperature variant. |
| R6→R13 | **Not found.** Candidate selection happens after `wm-horizon-steps` has already been derived from anticipation and supplied to scoring (`scripts/futon2/report/war_machine.clj:4482-4522`); the ranked candidates then enter `policy/select-action` at `:4568-4584`. No selected-policy/candidate output is read to set depth. | Neither, in both records. The recorded SELECT hop is R6→R14, in `:route` and `:unmapped-hops`, not R6→R13 (`tick-run-record-2026-08-30.edn:1`; `tick-run-record-2026-08-31.edn:1`). | **Plumbing/misfiled drawing, with no realised plumbing found.** The live dependency points the other way in phase order: horizon config affects scoring before R6 selection. No departure from the formalism is evidenced by this edge. |
| R7→R14 | **Not found.** R7 channel precision is updated as `precision-state` (`scripts/futon2/report/war_machine.clj:4382-4388`). R14 instead reads a distinct `selection-gain` state from the prior trace and folds an R16 realised outcome (`:4332-4361`); only `selection-gain-value` is supplied to `policy/select-action` (`:4573-4581`). `effective-temperature` uses G spread and selection gain (`src/futon2/aif/policy.clj:46-80`). The selection-gain module explicitly says this is not variational policy precision (`src/futon2/aif/selection_gain.clj:1-15`). | Neither, in both records. R6→R14, not R7→R14, is recorded in `:route` and `:unmapped-hops` (`tick-run-record-2026-08-30.edn:1`; `tick-run-record-2026-08-31.edn:1`). | **Plumbing/misfiled drawing.** The drawn edge conflates two different precision-like quantities. τ is modulated by the engineering selection gain and optionally G spread, not by R7's channel Π. Therefore these records do **not** support a precision-modulated-temperature departure requiring a new citation/choice for R7→R14. |

## Result for ruling

- One drawn edge is traversed: R8→R5, but only as route sequencing; F is not an
  input to the R5 score.
- R2→R3 is a shortened drawing of an implemented path through prediction error
  and precision; there is no direct observation reader at R3.
- Four SELECT-side edges appear in neither run. R14→R16 has identifiable
  selection/authorization plumbing but no grounded actuation in this path; direct
  readers for R13→R14, R6→R13 and R7→R14 are not found.
- In particular, R7 Π does not feed τ. The live temperature uses the separately
  named engineering `selection-gain`, whose source explicitly disclaims
  variational policy precision.

No registry or paper artefact was changed by this discovery.
