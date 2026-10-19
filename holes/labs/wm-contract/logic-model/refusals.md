# WM click runner: refusals, abstentions and stops (selection path and neighbours)

## Reviewed update — 2026-10-19, codex-1

Chain 8 / P15: configuration injury uses explicit retained provider-absence
cause fields, including P10 `[:failure :causes :causes]`. Arbitrary decision
metadata is not causal evidence. Scored no-action and partial-frontier selection
are distinct non-injury observations; absent target reading alone is not proof
of missing machinery. No unknown overflow cause is inferred. Worker final
classifier tests passed; this is not a live ledger reclassification.

Chains 2–5/7 P10 (worker `afcd14896` + `a5d8ba5ba`, test `06f78ee44`): terminal
failure retains projected causes from supported refusal containers, original
failure/error-data and typed cause-chain data, with distinct cause counts and
two in-record entries. Overflow stores the complete supported projected cause
set in a digest-pinned sidecar outside the run-record filename namespace; failed
writes remain explicit absences. Counts are not a census of arbitrary judgement
trees. Original kind and close wrapper stay distinct. Synthetic boundary tests
pass; no live persistence claim.

Rows B/E P06 increment: policy-local objective absence is retained as a partial
strict verdict with exact upstream terms/score causes; integrity/global defects
still refuse. This improves cause retention, not continuation: ordinary-result
remains non-selected and the debugger gate is unchanged. Do not mark the original
strict-objective coverage blocker resolved by this measurement-only increment.

Evidence-join row B P05 (`845c427dd` + `afd1d2a17`): policy-local missing terms
or malformed priors now produce typed absent rows in a partial receipt instead
of atomic policy-evidence-incomplete refusal. Invalid global declaration/menu
integrity remains a refusal. This does not yet remove the downstream selector's
computed-only gate; no runtime continuation is claimed.

Chain 5 update (P00, `b4359d675`, reviewed worker `6fc4fadef`): scalar failure-detail remains lossy. Separately, `persist-run-record!` now copies existing `:reading-frontier` and `:loop-node-exercise` from the abstained sorry-cell certificate without requiring Q2 construction. It does not reconstruct omitted detail. The focused written-record test covers Q2 present/absent; no post-fix live click.

The original audit below remains a historical reading at its stated revision.

- futon2 commit read: `f76041cc7` (git rev-parse --short HEAD)
- date: 2026-10-19
- method: static reading only (grep and sed over `src/futon2/aif/**`, plus `scripts/futon2/report/war_machine.clj:1586` where one refusal is built). No code run.
- paths are relative to `/home/joe/code/futon2/src/futon2/aif/` unless they start with `scripts/` or `test/`.
- test column: file names under `test/futon2/` found by grepping the keyword (`:kind`), so a hit means the keyword appears in that file, not necessarily that the test constructs it through the raising function. NONE = no hit in `test/`.
- Disposition column: stop = throws or ends the click; record-and-continue = typed value returned and carried in the record, later code decides; default = a fallback value is substituted.
- Phase names are my reading, in click order: OUTER-FIELD (policy field from target sources) -> OUTER-MENU (sealed pre-choice menu) -> OUTER-EVIDENCE (provider) -> OUTER-OBJECTIVE -> OUTER-SEAL -> OUTER-LEARNING -> HANDOFF (cascade_decision receives the carrier) -> INNER (reading-family-decision) -> GATE (decision_gate/emit!) -> RUNNER-SELECTION (catch in full_loop_runner) -> READING-PREP (selected-target-reading phases) -> CLOSE/RECORD.

## Table

### A. Outer field and menu (wm/family_selection.clj, target_reading_registry.clj)

| kind | file:line | raising function | phase | missing/invalid input named | disposition | tests |
|---|---|---|---|---|---|---|
| `:no-current-target-reading` | target_reading_registry.clj:191 | `current-reading` | OUTER-FIELD | no stored reading for `target-id` (`:found-digests` empty) at `:expected-digest` (excerpt digest of the source) | record-and-continue: returned as `{:status :absent :kind ...}`; the family gets no policies (family_selection.clj:121 classes it `:absent`) | aif/outer_policy_field_boundary, outer_policy_runtime_selection, selected_target_reading, target_reading_preparation, target_reading_registry, wm/family_selection, wm/reading_family_decision, wm/selection_cascade_identity, wm/target_policy_field_decision, report/cascade_decision |
| `:stale-target-reading` | target_reading_registry.clj:191 | `current-reading` | OUTER-FIELD | readings exist for `target-id` but none at `:expected-digest` | record-and-continue (same as above) | aif/target_policy_family, target_reading_preparation, target_reading_registry, wm/family_selection, wm/q2_construction_route |
| `:invalid-target-reading` | target_reading_registry.clj:158, 162 | `publish!` (writer side) | READING-PREP | `:invalid-fields` (validator-version, request-excerpt-digest, source-text-template-only) or `(validation analysis)` not `:valid` | stop for that publication (returns `:status :refused`, nothing written) | aif/target_reading_registry |
| `:graph-path-not-supplied` | wm/family_selection.clj:145 | `families-for-field` | OUTER-FIELD | `graph-path` nil in the field config; every target then fails | record-and-continue: every family becomes `failed-field-family` (:100) | wm/target_policy_field_decision |
| `:target-source-path-absent` etc (`source-absent` value copied into `:kind`) | wm/family_selection.clj:214 (`:source-absent :target-source-path-absent`, set in full-eligible-target-field; copied into the family at :156-157) | `families-for-field` | OUTER-FIELD | task has no `:source :path` | record-and-continue | not grepped per kind (see Uncertain) |
| `:target-source-kind-unsupported` | wm/family_selection.clj:161-162 | `families-for-field` | OUTER-FIELD | `source-kind` not in `#{nil :head}` | record-and-continue | not grepped (Uncertain) |
| `:target-source-unreadable` | wm/family_selection.clj:192 | `families-for-field` (catch Exception) | OUTER-FIELD | any exception from digest/reading/squares lookup; `:message` kept | record-and-continue | not grepped (Uncertain) |
| `:no-canonically-bound-policy` | wm/family_selection.clj:360 | `sealed-pre-choice-menu` | OUTER-MENU | `entries` empty: no family has status `:computed` after canonical binding (this is what an all-absent reading field produces) | record-and-continue: returned as sealed-menu `{:status :refused :reason ...}`; becomes outer `:policy :reason` via meta_live_outer_selector.clj:638 | NONE (grep `canonically-bound` over test/ returns nothing) |
| `:canonical-binding-refused` | wm/family_selection.clj:356 | `sealed-pre-choice-menu` | OUTER-MENU | `(:refusals bound)` non-empty from `cascade-identity/bind-ranked` | record-and-continue (entries emptied) | wm/selection_cascade_identity |
| `:sealed-policy-menu-invalid` | meta_live_outer_selector.clj:638 | `select-live` cond | OUTER-MENU | sealed menu not `:sealed` and carries no `:reason` | record-and-continue (a default reason label) | NONE |
| `:policy-field-config-required` | meta_live_outer_selector.clj:636, 651 | `select-live` cond | OUTER-FIELD | `policy-field-config` not a map | record-and-continue | meta_live_outer_selector |
| `:meta-field-observation-unverified` | meta_live_outer_selector.clj:798 | `select` | OUTER-FIELD | `meta-field-observation` present but `field/verify` not `:verified` | record-and-continue (receipt built with refused selection) | meta_live_outer_selector |

### B. Provider, objective, seal, learning (outer selection after the menu)

| kind | file:line | raising function | phase | missing/invalid input named | disposition | tests |
|---|---|---|---|---|---|---|
| `:outer-policy-evidence-provider-required` | outer_policy_evidence_provider.clj:43 | `resolve-provider` | OUTER-EVIDENCE | `provider` neither a map nor an `ifn?` (nil when no `:outer-policy-evidence-provider` is passed to `select-live`, meta_live_outer_selector.clj:519 `select-live`, resolve call at :603-605) | record-and-continue; also relabelled, see Chains 1 | meta_live_outer_selector, outer_policy_field_boundary, outer_policy_runtime_selection, selection_refusal_record, report/cascade_decision |
| `:outer-policy-evidence-provider-threw` | outer_policy_evidence_provider.clj:53 | `resolve-provider` | OUTER-EVIDENCE | provider fn threw; `:error-class` kept | record-and-continue | NONE |
| `:provider-source-unavailable` | outer_policy_evidence_provider.clj:64 | `load-static` | OUTER-EVIDENCE | EDN file unreadable | record-and-continue | NONE |
| `:provider-schema` / `:provider-declaration` / `:provider-records` / `:provider-source-pin` / `:provider-source-pin-mismatch` / `:provider-malformed` | outer_policy_evidence_provider.clj:19-26 (returned by `verify`, used as `:reason` at :45) | `verify`, `resolve-provider` | OUTER-EVIDENCE | provider response fields (`:schema`, `:declaration` map, `:records` vector, `:source-pin` path+sha256, pin equals recomputed pin) | record-and-continue; `:provider-refused` is the fallback when status is not `:provided` and no `:reason` | not grepped per kind |
| `:learning-authority-unavailable` | outer_policy_provider_runtime.clj:29 | `provide` | OUTER-EVIDENCE | `(get-in state [:belief :status])` not `:produced` | record-and-continue | NONE |
| `:policy-evidence-incomplete` (default) and other `:reason` from `sealed-error` / `malformed-declaration` | outer_policy_evidence.clj:176 (outside the scope list; read only via grep) | `join` | OUTER-EVIDENCE | rows with non-empty `:absent-terms` (:159, :173) | record-and-continue | not grepped (Uncertain) |
| `:policy-evidence` terms: `:policy-identity`, `:sealed-policy-binding`, `:vocabulary`, `:finite-objective-input`, `:posterior-not-normalized`, `:posterior-alpha-mismatch`, `:ln-e-mismatch`, `:eig-mismatch` (as `:absent-terms`, first one becomes `:reason`) | outer_policy_objective.clj:51-79, reason chosen at :93-96 | `evaluate` | OUTER-OBJECTIVE | per-row numeric inputs of the joined evidence | record-and-continue (status `:refused`) | outer_policy_objective_test not grepped per term |
| `:menu-pin-mismatch`, `:policy-population-mismatch`, `:vocabulary-mismatch`, `:gamma` | outer_policy_objective.clj:42-47 | `evaluate` (`malformed-top`) | OUTER-OBJECTIVE | evidence menu-pin vs sealed menu-pin; policy population; vocabulary; gamma finite and positive | record-and-continue | not grepped |
| `:objective-input-invalid` | outer_policy_objective.clj:96 | `evaluate` | OUTER-OBJECTIVE | `rows` empty with no other reason | default (label) | NONE found |
| `:invalid-tie-draw` | outer_policy_objective.clj:107 | `evaluate` | OUTER-OBJECTIVE | tied minima and `draw`/`rand` not in [0,1) | stop (status `:refused`) | NONE |
| `:cascade-identity-mismatch` | outer_policy_objective.clj:117 | `evaluate` | OUTER-OBJECTIVE | `identity/verify-selected-identity` not `:verified` | record-and-continue | outer_policy_objective, wm/selection_cascade_identity |
| `:cascade-identity-mismatch` (second site) | wm/family_selection.clj:493-495 | `select-over-families` (inner) | INNER | same check on the inner decision | record-and-continue (`:status :refused`) | wm/selection_cascade_identity |
| `:strict-policy-selection-required` | selected_outer_cascade_policy.clj:32 | `seal` | OUTER-SEAL | objective receipt status not `:selected` (e.g. abstained on a tie) | record-and-continue | NONE |
| `:selected-policy-*` (schema, status, seal, menu-invalid, menu-substitution, evidence-invalid, evidence-substitution, objective-seal, objective-substitution, not-strict, objective-recomputation, entry-missing, canonical-record, target-mismatch, occurrence-mismatch, digest-mismatch, malformed) | selected_outer_cascade_policy.clj:65-92 (returned by `verify`); used as `:reason` at :97 and at meta_live_outer_selector.clj:643 | `verify`, `decision` | OUTER-SEAL / HANDOFF | carrier fields vs recomputation | record-and-continue (:97 `{:status :refused :reason defect}`) | not grepped per kind |
| `:selected-policy-target-not-admitted` | meta_live_outer_selector.clj:630 (`carrier-defect` when `selected-task` absent) | `select-live` | OUTER-SEAL | selected target id not among `on-map` task views | record-and-continue | NONE |
| `:outer-learning-not-configured` | meta_live_outer_selector.clj:213 | outer-learning certificate builder | OUTER-LEARNING | `outer-learning` nil | record-and-continue (`:status :absent`) | NONE |
| `:outer-learning-mode-not-admitted` | meta_live_outer_selector.clj:218 | same | OUTER-LEARNING | mode not `#{:shadow :live}` | record-and-continue | NONE |
| `:outer-learning-state-unavailable` | meta_live_outer_selector.clj:224, 684-685 | same; `effective-selection` | OUTER-LEARNING | belief `:status` not `:produced`; or live mode with no shadow selection | stop for the outer selection in live injury mode (:684 makes the effective selection refused); certificate-only at :224 | meta_live_outer_selector, run_debugger |
| `:outer-learning-shadow-not-consumed` | meta_live_outer_selector.clj:230 | same | OUTER-LEARNING | some score not `:scored`, or learned selection not `:selected` | record-and-continue, but runner stops on it, see `:wm/outer-learning-not-consumed` | NONE |

### C. Handoff and inner selection (wm/cascade_decision.clj, wm/family_selection.clj, policy.clj)

| kind | file:line | raising function | phase | missing/invalid input named | disposition | tests |
|---|---|---|---|---|---|---|
| `:outer-task-selection-refused` | scripts/futon2/report/war_machine.clj:1586 (field-level, target `{:absent :outer-task-not-selected}`, `:missing :outer-task-selection`); wm/cascade_decision.clj:1340 (carrier handoff, `:failure-kind (:reason projected)`); :1346 (`:claimed-policy-selection-without-carrier`) | `outer-task-refusal`; `cascade-decision` | HANDOFF | `(get-in receipt [:chosen :id])` nil; or `:selected-outer-cascade-policy` fails `verify`; or `:outer-policy-selection-claimed?` without a carrier | record-and-continue into `abstain` (-> decision_gate allowed kind) | decision_gate, selection_refusal_record, report/cascade_decision, report/mission_read_once |
| `:claimed-policy-selection-without-carrier` | wm/cascade_decision.clj:1346 | `cascade-decision` | HANDOFF | `:outer-policy-selection-claimed?` true, `:selected-outer-cascade-policy` absent | record-and-continue (abstain) | NONE |
| `:no-policy-family` | wm/cascade_decision.clj:1048 (`family-refusals`), :1214, :1222 | `family-refusals`; `reading-family-decision` | INNER | target whose family has empty `policies`; `:failure-kind` = first failure's `:kind` else `:empty-policy-family`; or field `:status` not `:formed` (`(:reason field)`); or `:no-enumerated-target` | record-and-continue (abstain) | wm/reading_family_decision, report/cascade_decision, report/coverage_account, report/new_wanted_token_admission |
| `:empty-policy-family` / `:no-enumerated-target` | wm/cascade_decision.clj:1049, 1223 | same | INNER | label values for `:failure-kind` | default (labels) | report/cascade_decision (no-enumerated-target) |
| `:incomplete-policy-menu-coverage` | wm/family_selection.clj:449 (decision `:kind`, `:missing-targets`) | `select-over-families` | INNER | some target has a failed family (`missing-targets`) | record-and-continue (`:status :abstained`); NOTE also tested for in selected_target_reading.clj:204 and full_loop_runtime.clj:217 as an outer `:policy :reason`, see Uncertain 1 | meta_live_outer_selector, outer_policy_runtime_selection, target_reading_preparation, wm/family_selection |
| `:no-computed-policy-family` | wm/family_selection.clj:518 | `select-over-families` | INNER | `ranked` empty, no computed family | record-and-continue (`:status :abstained`) | NONE |
| `:repeat-refused` (status) / `:repeat-choice-after-refusal` (kind) | wm/family_selection.clj:462; wm/cascade_decision.clj:1266 | `select-over-families`; `reading-family-decision` | INNER | `(:refusal excluded)`: all admissible policies equal the previous refused choice | record-and-continue (abstain) | wm/q6_repeat_after_refusal (kind); status NONE |
| `:model-contract-refused` | wm/cascade_decision.clj:1276 (`:certification-kind` = inner kind) | `reading-family-decision` | INNER | certification refusal from `family-selection`, :466 | record-and-continue | wm/reading_model_contracts |
| `:preference-not-certified` | wm/cascade_decision.clj:1135 | certification fn | INNER | `uncertified` preference receipts | record-and-continue (-> :model-contract-refused) | wm/reading_model_contracts |
| `:reading-precision-refused` | wm/cascade_decision.clj:1140 | same | INNER | `precision/carry-beta` returned refused (kind from `ex-data` :1132) | record-and-continue; carry kept for next tick | wm/reading_model_contracts |
| `:beta-not-declared` | wm/cascade_decision.clj:1155 | `declared-beta` | INNER | `(get-in assembled [:declared-betas target])` nil for a computed family | record-and-continue (:1229 prepends refusal to family refusals) | many (cascade_problems, decision_gate, full_loop_runner, wm/reading_family_decision, ...) |
| `:incommensurable-family` | wm/cascade_decision.clj:1159 | `declared-beta` | INNER | more than one distinct beta across computed families | record-and-continue | judge_refusal_abstention, report/cascade_decision, others |
| `:no-constructed-candidate` | wm/cascade_decision.clj:818 | target admission | INNER (older construction route) | `target-decline` with `:no-admissible-candidate` | record-and-continue | many |
| `:missing-common-horizon` | wm/cascade_decision.clj:212 (`stop!`), :657 | cascade lane | INNER | `:horizon-steps` not a positive int; "no default" | stop | report/new_wanted_token_admission |
| `:controlled-authority-absent`, `:three-halves-policy-absent`, `-invalid`, `-projection-mismatch`, `:admitted-policy-population-mismatch`, `:cascade-admission-refused`, `:application-order-invalid` | wm/cascade_decision.clj:828-941 | `controlled-three-halves-admission` and neighbours | pre-G composition (comment says production does not call it) | mode, policy, certificate | stop (returns refused) | three_halves_policy |
| `:free-energy-not-supplied` | raised in cascade_selection (outside scope); mapped at wm/cascade_decision.clj:1177 | `selector-refusal->judge-refusal` | INNER | `:missing :historical-prefix-f` | stop (rethrown as "cascade decision refused") | many (cascade_selection, wm/reading_family_decision, ...) |
| `:no-acting-cascade-candidate` | policy.clj:490 | `select-action-cascades` | INNER | no candidate with a positive-mass first acting pattern (`acting` empty) | stop (throws "Cascade selection refused"); also used as fallback label, Chains 3 | 13 files incl. gate_refusal_abstention, judge_refusal_abstention, phase_kind, selection_refusal_record, report/cascade_decision |
| `:identity-forged`, `:cascade-validation-error` | wm/cascade_decision.clj:1575, 1583, 1591 | post-selector retained-identity check | HANDOFF/CLOSE | digest of retained cascade vs carried | record-and-continue (downstream refusal, runner full_loop_runner.clj:2289) | wm/cascade_runner_seam, wm/reading_refusal_terminal, wm/selection_cascade_identity, wm/executable_cascade |
| `:cascade-enactment-open` | wm/cascade_decision.clj:1516, 1555 | enactment | CLOSE | explicit execution-semantics boundary | record-and-continue (`:repairable true`, runner :2278) | wm/cascade_runner_seam, wm/selected_reading_runner |
| `:prefix-f-commissioning-refused` | wm/cascade_decision.clj:90 | commissioned Prefix-F preparation | INNER | thrown ex-info | stop | not grepped |
| `:policy-prefix` family: `:invalid-policy-prefix` | policy.clj:275 | prefix check | INNER | `prefix` invalid | stop | not grepped |
| `:precision-consumption-mismatch` | policy.clj:466 | precision carry | INNER | carry consumption | stop | not grepped |
| `{:absent :first-enabled-refused}` | policy.clj:207, 234 | first-enabled helpers | INNER | `manifest/first-enabled` threw | default (typed absence) | not grepped |
| `:empty-cascade-is-not-an-action` | policy.clj:625 | `select-action-cascades` | INNER | excluded empty cascades | record-and-continue (`:excluded-non-actions`) | not grepped |

### D. Gate (decision_gate.clj) -- all throw `{:error :inadmissible-decision :reason r}`

| kind | file:line | raising function | phase | input named | disposition | tests |
|---|---|---|---|---|---|---|
| `:missing-observation-locators` | decision_gate.clj:111 | `refuse!` via check fn | GATE | observation locators | stop (becomes abstention, Chains 4) | gate_refusal_abstention |
| `:beta-not-recorded` | :153 | | GATE | beta in selection law | stop | decision_gate |
| `:reading-policy-identity-invalid`, `:reading-policy-debt-invalid` | :167, :173 | | GATE | reading-policy identity/debt | stop | not grepped |
| `:missing-construction-receipt`, `:missing-interpretation-receipts`, `:empty-interpretation-receipts` | :179, :182, :186 | | GATE | receipts on a cascade decision | stop | not grepped |
| `:posterior-over-non-cascade` | :195 | | GATE | candidate shape | stop | not grepped |
| `:ticket-queue-certificate-mismatch`, `:ticket-queue-candidates-mismatch`, `:ticket-queue-choice-invalid` | :219, :223, :251 | | GATE | ticket-queue plan vs decision | stop | not grepped |
| `:missing-recorded-posterior`, `:posterior-not-normalised`, `:chosen-action-not-a-candidate` | :259, :264, :266 | | GATE | recorded posterior | stop | not grepped |
| `:chosen-action-is-not-an-action`, `:no-acting-candidate` | :284, :288 | | GATE | empty-cascade chosen / all-empty roster | stop | NONE for `:no-acting-candidate` |
| `:chosen-not-bayes-action`, `:chosen-mass-not-recorded`, `:chosen-mass-not-marginal` | :296, :309, :311 | | GATE | chosen action mass vs marginal | stop | not grepped |
| `:empty-refusals`, `:unknown-refusal-kind` | :324, :329 | `check-abstention!` | GATE | abstention with empty `:refusals`, or a refusal whose `:kind` is not in `allowed-refusal-kinds` (:49-70) or lacks `:target` | stop | decision_gate (empty-refusals); unknown-refusal-kind NONE |
| `:flat-action`, `:not-a-decision` | :341, :342 | `emit!` | GATE | decision shape | stop | not grepped |

### E. Runner (full_loop_runner.clj, full_loop_runtime.clj, selected_target_reading.clj, loop_node_exercise.clj)

| kind | file:line | raising function | phase | input named | disposition | tests |
|---|---|---|---|---|---|---|
| `:wm/selection-terminal-abstention` (ex-info; `:failure-kind` = `:abstained` or `:no-selection`) | full_loop_runner.clj:2333-2336 | `selection-terminal-condition` | RUNNER-SELECTION | `(selected-entry judgement)` nil; `:refusals` and `:targets` copied from the decision | stop only when the restart debugger is attached (comment :6395-6402); the detached path continues to the throw at :6827 | meta_field_observation, meta_injury_store, run_debugger |
| `:wm/outer-learning-not-consumed` | full_loop_runner.clj:2311-2314 | `selection-terminal-condition` | RUNNER-SELECTION | selected entry with `(:status learning)` `:refused` | stop (debugger path) | run_debugger |
| "War Machine abstained or selected no addressable action" (`:outcome :abstained` / `:no-selection` / `:incomplete`) | full_loop_runner.clj:6827 via `no-entry-failure-data` :2275-2300 | run-opportunity selection phase | RUNNER-SELECTION -> CLOSE | no `selected-entry` | stop; `:failure-kind` from `(:kind downstream-refusal)` only; abstained decisions carry no `:failure-kind` here, only `:outcome` | many |
| judge refusal ("cascade decision refused", kind keyword) | full_loop_runner.clj:699 (`judge-refusal`), consumed :6315-6322 | `judge-refusal` | RUNNER-SELECTION | `:kind`, `:target`, `:missing` of ex-data; missing falls back to `{:absent :kind-names-no-missing-input}` | record-and-continue (becomes abstention: `judge-refusal-abstention` :4415) | judge_refusal_abstention |
| gate refusal | full_loop_runner.clj:724 (`gate-refusal`) | `gate-refusal` | RUNNER-SELECTION | `(:reason d)`; target from detail else flight target; missing from `:missing-tokens` else absent | record-and-continue (abstention) | gate_refusal_abstention, phase_kind |
| `:untyped-failure` | full_loop_runner.clj:4978 (`failure-kind-from`) | `failure-kind-from` | CLOSE | no `:failure-kind` or `:outcome` anywhere in the cause chain and no transport match | default (label) | many; phase_kind |
| `:required-loop-node-unexercised` | wm/loop_node_exercise.clj:166 | `require-complete!` | RUNNER-SELECTION | any node `:status :refused`, i.e. `:meta-algorithm-wrong-route` (:64-ish); gated by `:require-loop-node-exercise?` (full_loop_runtime.clj:301, runner :6385) | stop | not grepped (Uncertain) |
| `:meta-algorithm-wrong-route` | full_loop_runner.clj:2707; wm/loop_node_exercise.clj:60 (`refused node`); d_predecessor_task_authority.clj:376 | `construct-selected-action :run-algorithm` | RUNNER-SELECTION / D-AUTH | action type `:run-algorithm` on the ordinary route | stop (refused) | not grepped |
| `:policy-nondiscrimination` | full_loop_runner.clj:6858-6865 | run-opportunity | RUNNER-SELECTION | `discrimination` `:passes?` false | stop | full_loop_runner, selection_refusal_record |
| `:agent-unavailable` (no cast; reviewer unavailable or equals author) | full_loop_runner.clj:6832-6850 | run-opportunity | after selection | `author`/`reviewer` nil; reviewer not available | stop | many |
| `:decision-input-freshness-unestablished` | full_loop_runner.clj:6286-6290 | run-opportunity | before selection | `:refresh-required?` true and refresh `:outcome` not `:ok` | stop | NONE |
| `:stale-runner-source`, `:stale-outer-selection-composition` | full_loop_runner.clj:676-695 | serving-source check | start of click | runner namespaces not current vs canonical checkout | stop | not grepped |
| `:selected-target-reading-unpinned` | selected_target_reading.clj:320 | `repair-need!` | READING-PREP | need has no pin | record-and-continue (`published false`) | selected_target_reading |
| `:designated-author-unavailable` | selected_target_reading.clj:323; full_loop_runtime.clj:78 (`:state :not-contacted`) | `repair-need!`; `designated-agent-answer-fn` | READING-PREP | `:author` not invoke-ready/idle | record-and-continue | full_loop_runtime, handoff_contract, selected_target_reading |
| `:selected-target-reading-request-refused`, `-contract-unavailable`, `-pin-mismatch`, `-validation-refused`, `-publication-refused` | selected_target_reading.clj:333, 358, 362, 483, 502 | `repair-need!` | READING-PREP | request/contract/pin/validation/publication | record-and-continue | validation-refused: selected_target_reading; publication-refused: wm/recorded_flight; others NONE |
| `:selected-target-reading-repair-threw` | full_loop_runner.clj:2145 | `selected-target-reading-step` | READING-PREP | repair-fn threw | record-and-continue (record kept, no redecision) | NONE |
| `:reading-preparation-threw` | target_reading_preparation.clj:47 (outside list; read) | `prepare!` | READING-PREP | repair-fn threw for a need | record-and-continue (`:status :refused :reason :publication-refused`) | target_reading_preparation |
| `:occurrence-square-authoring-threw` | full_loop_runner.clj:2169 | `occurrence-square-authoring-step` | READING-PREP | repair-fn threw | stop: :6492 throws "Occurrence-square authoring failed before G" with `:failure-kind (:kind record)` | NONE |
| `:selected-target-reading-redecision-failed`, `:occurrence-square-redecision-failed` | full_loop_runner.clj:6446, 6480 | redecision wrappers | READING-PREP | any Throwable in the redecision | stop (wraps cause) | not grepped |

### F. Meta injury and terminal record (neighbours; fire after a click closes)

| kind | file:line | raising function | phase | input named | disposition | tests |
|---|---|---|---|---|---|---|
| `:ordinary-selection-abstention` (non-injury reason for `:abstained`) | meta_terminal_injury.clj:11 | `explicit-non-injury-kinds` | CLOSE/RECORD | closed click with failure kind `:abstained` | default: an abstained click is classified non-injury, not an injury | meta_terminal_injury |
| `:classification/invalid`, `:classification/malformed`, `:classification/unknown`, `:terminal-record/malformed`, `:ledger/tampered`, `:injury/retry-click-mismatch`, `:injury/terminal-history-requires-new-transaction`, `:injury-integration/malformed` | meta_terminal_injury.clj:80, 82, 108, 123, 150, 153, 214, 226, 229, 231 | `refusal` | RECORD | record shape (`:record/source-pin`, run-id, click-id) and ledger | stop (refused) | not grepped |
| `:injury-source-bytes-missing`, `:external-injury-source-authority-invalid`, `:injury-run-record-bytes-missing`, `-authority-invalid`, `:injury-source-drift`, `:injury-run-record-drift`, `:injury-projection-mismatch`, `:required-injury-evidence-not-present`, `:injury-source-unreadable`, `:injury-observation-does-not-match-source` | meta_injury_observation.clj:57-146 | `refusal` | RECORD | injury source bytes/authority/pins | stop (refused) | not grepped |
| `:algorithm-catalog-invalid`, `:duplicate-identities`, `:algorithm-catalog-file-mismatch`, `:source-drift`, `:algorithm-catalog-unreadable`, `:external-snapshot-authority-required`, `-invalid`, `:external-snapshot-pin-mismatch`, `:field-schema-invalid`, `:field-counts-mismatch`, `:registry-row-dropped`, `:registry-identity-mismatch`, `:algorithm-catalog-accounting-mismatch` | meta_field_observation.clj:129-353 | `refusal` | OUTER-FIELD (meta field verification) | snapshot pin, catalog, counts | stop (refused); surfaces as `:meta-field-observation-unverified` | meta_field_observation (not grepped per kind) |
| `:candidate-identity-mismatch` (failure-kind `:terminal-receipt-invalid`) | wm/terminal_receipt.clj:76-78 | `selected-g-decomposition` (terminal_receipt.clj:58) | CLOSE | selected candidate matches not exactly one retained G | stop | not grepped |
| `:selected-candidate-g-not-recorded` | wm/terminal_receipt.clj:73, 92 | same | CLOSE | `scoring` empty or `g-terms` not a non-empty map | record-and-continue (`:status :missing`) | not grepped |
| D predecessor: `:after-token-not-observed`, `:after-token-evidence-unavailable`, `:precision-selected-declaration-unestablished`, `-ambiguous`, `:precision-observation-schedule-mismatch`, `:carry-no-predecessor`, `:deferred-artifact-not-fresh-execution`, `:historical-verification-not-execution`, `:binding-not-retained`, `:dispatch-not-retained`, `:task-execution-incomplete` | d_predecessor_task_authority.clj:57-59, 176-181, 271-279, 338-357 | `refuse!` / maps | D-AUTH (after enactment) | task execution evidence | stop / refused | not grepped |

## Chains

1. Evidence-provider absence travels as a reason string through four hops.
   - `outer_policy_evidence_provider.clj:43` builds `{:status :refused :reason :outer-policy-evidence-provider-required}` when `provider` is neither a map nor a fn.
   - `outer_policy_evidence_provider.clj:19-22` (`verify`) turns any non-`:provided` status into `(or (:reason provided) :provider-refused)`; `:45` wraps it as a new refusal with `:reason defect`, `:missing-policy-digests` and a fixed `:absent-terms` list (:46-49). The fixed list names `:outcome-vocabulary ... :durable-terminal-records` whether or not those were the cause.
   - `meta_live_outer_selector.clj:639` (`(not= :provided (:status provided)) provided`) makes it the `ordinary-result`; `:706-707` copies `(:reason ordinary-result)` into `[:policy :reason]` and `:738` into `:chosen {:absent <reason>}`; `:absent-terms` of the provider are copied at :720.
   - `scripts/futon2/report/war_machine.clj:1586` (`outer-task-refusal`) wraps the META selection under `:selection` with `:kind :outer-task-selection-refused`, `:missing :outer-task-selection`; the reason is no longer a top-level key there.
   - `wm/cascade_decision.clj:1220-1222` (empty `families` branch) filters `(:refusals assembled)` for that kind and abstains with it.
   - `wm/cascade_decision.clj:1401-1405` sets `[:decision :reason]` to `(some :reason refusals)`, else `:failure-kind` of the `:outer-task-selection-refused` refusal, else `:no-acting-cascade-candidate`.
   - `meta_terminal_injury.clj:47-51` (`unconfigured-route?`) searches the record tree for the value `:outer-policy-evidence-provider-required` under `:reason :kind :failure-kind :absent` and adds it to the cause id.

2. `:no-current-target-reading` is turned into other kinds before anything stops.
   - Raised as a typed absence at `target_reading_registry.clj:191`. `family_selection.clj:121` maps it to coverage `:absent`; `:305-307` (`measured-frontier`) counts it as `:reading-absent`. No stop at this point.
   - `family_selection.clj:360` (`:no-canonically-bound-policy`) fires only if every family is non-computed. If at least one family is computed, the others are silently omitted from the menu (they appear in `:frontier :reading-absent-ids` and in `:failures`) and selection proceeds.
   - `meta_live_outer_selector.clj:638` copies that reason into the outer receipt (`[:policy :reason]`, :711), where `selected_target_reading.clj:204` (`prechoice-field`) and `full_loop_runtime.clj:217` read it to decide whether preparation runs.
   - `selected_target_reading.clj:215` (`prechoice-needs`) reads the original `:no-current-target-reading` out of `:failures`, so the same label is re-read from the field, not from the reason.
   - On the inner side, `wm/cascade_decision.clj:1048-1050` turns an all-failed family into `:no-policy-family` with `:failure-kind (:kind (first failures))`; only the first failure's kind survives.

3. `:no-acting-cascade-candidate` is both a raised kind and a fallback label.
   - Raised at `policy.clj:490` (throw "Cascade selection refused", kind under `:refusal`).
   - `wm/cascade_decision.clj:1405` uses it as the last alternative in `(or ...)` for `[:decision :reason]` of any abstained decision whose `:reason` is nil, i.e. also when the abstention came from `:no-policy-family`, `:incommensurable-family`, `:model-contract-refused` with no `:reason` key in its refusals. Refusals that carry `:reason` (`:model-contract-refused` copies `[:reason ...]` at :1273) or `:failure-kind` on an `:outer-task-selection-refused` entry keep their own label. So not every abstention is relabelled; those without those two keys are.
   - `full_loop_runner.clj:4985-4993` (`thrower-kind`) reads `:refusal :kind` from ex-data, so the policy.clj throw is typed through `phase-kind-failure` (:4995-5008) into `:failure-kind :no-acting-cascade-candidate`.

4. Judge/gate refusals are converted to abstentions, losing the exception shape.
   - `wm/cascade_decision.clj:1177-1181` rethrows a selection refusal as "cascade decision refused" with `:kind`, `:missing` (only `:free-energy-not-supplied` -> `:historical-prefix-f`, all others `{:absent :kind-names-no-missing-input}`), `:refusal`, and `:target` only when exactly one computed target.
   - `full_loop_runner.clj:699-722` (`judge-refusal`) re-reads it; `:missing` falls back again at :719-723 (`:incommensurable-family` -> `:common-beta`/`:common-horizon`; `:live-c-*` -> `:live-c`).
   - `full_loop_runner.clj:724-744` (`gate-refusal`) does the same for `:inadmissible-decision`; `:kind` is `{:absent :no-reason-given}` if `:reason` is not a keyword; `:missing` is `{:absent :reason-names-no-missing-input}` unless the detail has `:missing-tokens`.
   - `full_loop_runner.clj:4410-4418` (`judge-refusal-abstention`) rethrows as ex-info "War Machine abstained: ..." with `{:outcome :abstained :judge-refusal jr}`.
   - `full_loop_runner.clj:4948` (`explicit-failure-kind`) reads only `:failure-kind` / `:outcome`, so the recorded `:failure-kind` is `:abstained`, not the refusal's kind; the kind survives in `:judge-refusal` and `[:decision :abstention :targets]` (`abstention-carrier` :747-775).

5. `failure-detail` drops non-scalar detail (full_loop_runner.clj:845-870).
   - Scalars (keyword, number, boolean, nil, string up to 300 chars) are kept; vectors kept only when at most 32 all-scalar items; maps keep only their scalar-valued entries; every other key is listed under `:omitted`. Exceptions: keys `:refusals` and `:outer-task-selection` are kept through `bounded-failure-value` to depth 6 (:857-858).
   - Consequence: an abstention's `:decision`, `:judgement`, `:failure-detail` map with nested maps (e.g. the `:selection-terminal-abstention` ex-data key `:decision`) is reduced to its scalar entries; only the two named keys carry nested refusal structure into the run record. `run-record-failure` (:896-930) calls it on `(:error-data d)`.

6. Terminal-abstention outcome wrapper. `full_loop_runner.clj:2289-2296` (`no-entry-failure-data`) gives a typed downstream refusal `{:outcome :abstained :failure-kind (:kind downstream-refusal)}`; the comment there says `:refused` is not a valid close outcome (it would throw `invalid-close-outcome`). For an abstained decision with no downstream refusal the result is `{:outcome :abstained}` with no `:failure-kind` (:2297-2299), and `:6827` throws with that data.

7. Original failure kind wrapped by a close failure. `full_loop_runner.clj:884-890` (`original-typed-failure`), :7700-7706 (`retained-close-failure`) and :891-930 (`run-record-failure` merging `:original-failure`, recording `:wrapped-by`) keep the first typed failure when close throws.

8. Abstention classified as non-injury. `meta_terminal_injury.clj:11` maps a closed `:abstained` click to `:ordinary-selection-abstention`, so the abstentions in sections A-C do not feed the META injury ledger.

## Uncertain

1. `:incomplete-policy-menu-coverage` as an outer `[:policy :reason]`. `selected_target_reading.clj:204`, `full_loop_runtime.clj:217` test for it in the outer receipt. Grep over `src` and `scripts` finds only one producer, `wm/family_selection.clj:449`, where it is the `:kind` of an inner decision (`select-over-families`), not of the outer receipt. The outer `:policy :reason` is `(:reason ordinary-result)` (meta_live_outer_selector.clj:706-707), whose possible values I read are: sealed-menu `:reason` (`:canonical-binding-refused`, `:no-canonically-bound-policy`, or `:sealed-policy-menu-invalid`), provider/evidence/objective reasons, `:policy-field-config-required`, snapshot-verification reasons. I did not find that value assigned there. `meta_live_outer_selector_test` and `outer_policy_runtime_selection_test` mention the keyword; I did not read them, so a test may inject it.
2. `:no-canonically-bound-policy` is the only value left that triggers preparation in `prechoice-field`, and has no test mention in `test/`.
3. Kinds mentioned in the table as "not grepped": the many `decision_gate` reasons, `target-source-*` kinds (source rows come from `full-eligible-target-field`, read only in part), `:policy-evidence-incomplete` (outer_policy_evidence.clj, outside the scope list, read only through grep at :159, :173, :176), `:required-loop-node-unexercised`, `:stale-runner-source`.
4. Runner sites I found by grep but did not read to a phase: full_loop_runner.clj:287 and :310 (R16 stop-line has no durable repair identity / park registration failed), :399 (`:wm/phase-breakpoint`), :1353/1424/1475/1560/1578 (Agency and substrate preflight failures), :1597/1608/1664 (strategic selection endpoint failures), :3044-3057 (author refusal handling), :3642-3841 (revision), :4093-4316 (grounding), :4462 onwards (attempt evidence and D-task evidence), :5256/5293 (cohort options), :7004-7160 (historical verification and dispatch binding), `:ambiguous-tie` at :4364, learning-ledger refusal :6031, cascade-feedback refusal :5809.
5. Not read at all: meta_live_outer_selector.clj sources of `:repair-finding/evidence-unreadable` (:300), `:agency-evidence-unavailable` (:347), `:source-uncommitted` etc. (:412-433): these are `{:state :unknown :reason ...}` provenance values, likely attribution (not selection stops), but I did not trace whether `ownership` state `:unknown` filters a task out (:577-580 only excludes `:active`/`:ambiguous`).
6. `outer_policy_provider_runtime.clj:15` throws "Policy outcome source drift" (`{:path path}`, no kind); I did not read its caller to see whether it is caught.
7. policy.clj has other `throw` sites (:94-110 `ex-info (name kind)` for score input validation) that I did not map to a kind.
