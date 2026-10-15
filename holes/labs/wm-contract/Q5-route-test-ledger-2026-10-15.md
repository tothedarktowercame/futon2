# Q5 reading-family route: §7.4 test namespace ledger (2026-10-15)

Branch `claude-10-q5-family` at e11edb05b (before migration). Each namespace was run
alone through `run_redirected.clj` (drift guards pointed at the worktree).
NOTE-wiring-reading-to-decision §7.4 lists 18 namespaces asserting the retired
interpreted route (rewrite or delete) and 8 whose behavioural claim should survive
once their fixtures carry readings.

| Namespace | §7.4 group | Before (tests/pass/fail/error) | After |
|---|---|---|---|
| `futon2.aif.h4-production-stage-test` | rewrite/delete | 1/3/5/0 | not migrated |
| `futon2.aif.policy-precision-carry-test` | rewrite/delete | 9/50/10/0 | not migrated |
| `futon2.aif.policy-prefix-admission-test` | rewrite/delete | 8/21/0/0 | not migrated |
| `futon2.aif.policy-prefix-f-test` | rewrite/delete | 7/17/0/1 | not migrated |
| `futon2.aif.scoring-input-receipts-test` | rewrite/delete | 3/28/16/1 | not migrated |
| `futon2.aif.temporal-consume-test` | rewrite/delete | 8/4/3/6 | not migrated |
| `futon2.aif.token-belief-carry-test` | rewrite/delete | 4/11/8/1 | not migrated |
| `futon2.aif.token-belief-predecessor-test` | rewrite/delete | 3/18/7/0 | not migrated |
| `futon2.aif.token-observation-initialization-test` | rewrite/delete | 7/36/16/0 | not migrated |
| `futon2.aif.uniform-run-record-test` | rewrite/delete | 5/35/8/0 | not migrated |
| `futon2.report.cascade-decision-test` | rewrite/delete | 16/49/50/2 | not migrated |
| `futon2.report.coverage-account-test` | rewrite/delete | 5/7/0/4 | not migrated |
| `futon2.report.eig-source-remaining-test` | rewrite/delete | 3/10/7/0 | not migrated |
| `futon2.report.measured-a-decision-test` | rewrite/delete | 12/14/22/1 | not migrated |
| `futon2.report.new-wanted-token-admission-test` | rewrite/delete | 6/18/4/0 | not migrated |
| `futon2.report.observation-labels-consume-test` | rewrite/delete | 5/22/10/0 | not migrated |
| `futon2.report.preference-schedule-decision-test` | rewrite/delete | 7/29/6/0 | not migrated |
| `futon2.report.wm01-bindings-test` | rewrite/delete | 2/14/5/0 | not migrated |
| `futon2.aif.cascade-proposals-test` | retain, re-fixture | 6/35/1/0 | not migrated |
| `futon2.aif.gate-refusal-abstention-test` | retain, re-fixture | 6/38/4/0 | not migrated |
| `futon2.aif.judge-refusal-abstention-test` | retain, re-fixture | 6/36/4/0 | not migrated |
| `futon2.aif.repair-proposals-test` | retain, re-fixture | 5/30/1/0 | not migrated |
| `futon2.aif.ticket-queue-test` | retain, re-fixture | 8/42/1/0 | not migrated |
| `futon2.report.cascade-habit-accumulation-test` | retain, re-fixture | 5/32/0/0 | not migrated |
| `futon2.report.cascade-habit-selection-test` | retain, re-fixture | 1/2/1/0 | not migrated |
| `futon2.report.habit-fold-call-test` | retain, re-fixture | 5/2/15/0 | not migrated |

Sampled causes (cascade-proposals, habit-fold-call, policy-prefix-f): the tests
feed interpreted assemblies without `:target-sources` or readings, so the route
abstains `:no-policy-family` before any selector, and the assertions read old-route
products (`:proposal-awaiting-agent-admission` drops, the E fold's per-candidate
source, token-belief prefixes). Not every one is a retired behaviour: proposal
accounting, gate/judge refusal, ticket planning and the E fold are live seams that
must be re-proved over reading families (and the E fold has no reading-policy
identity yet), so they cannot be deleted as retired.


## Packet: prefix/token controls (claude-10, 2026-10-15)

Each namespace run alone through `run_redirected.clj`, before and after.
Shared fixture: `test/futon2/test_support/reading_route.clj` (pinned graph,
current reading, declared beta, complete flight ledger giving the certified
zero-history carrier, admitted-prefix steps recorded in the census, sealed
beta declarations).

| Namespace | Before | After | Disposition |
|---|---|---|---|
| `h4-production-stage-test` | 1/3/5/0 | 1/9/0/0 | Migrated. Unknown history refuses `:free-energy-not-supplied` over the reading family; the exact admitted Prefix-F population selects. Retired: the interpreted-admission three-halves decline. |
| `policy-precision-carry-test` | 9/50/10/0 | 9/59/0/0 | Migrated. Tick consumes the sealed reading-precision carry (old q0/class family not consumed). `abstention-does-not-drop-a-learned-rate` needed a production fix: an abstention keeps the predecessor's reading carry verbatim. Module tests unchanged. |
| `policy-prefix-admission-test` | 8/21/0/0 | 9/29/0/0 | `the-tick-records-the-prefix` restored on the reading route (was deleted in acb73f61b): each candidate's own prefix keyed by its reading-policy key; a policy with no step gets the certified zero-history carrier. |
| `policy-prefix-f-test` | 7/17/0/1 | 7/24/0/0 | Migrated `the-tick-selects-with-its-f-term`: F's effect isolated as the posterior-ratio change, exactly exp(dF), across two reading policies with different G. |
| `uniform-run-record-test` | 5/35/8/0 | 5/56/7/0 | Fixture migrated to the reading route. 7 assertions fail on absent reading-route seams, left failing (not loosened): `[:selection-certificate :scoring N :c-source]` is nil for reading policies, so `wm_run_validity` grades c-source `bad` (4/5 ok); the G-term decomposition census has no reading-route values for terms A/C/D/E/Q (`:consumed-value-not-recorded`). |
| `scoring-input-receipts-test` | 3/28/16/1 | unchanged | Not migrated: seam absent. `scoring-input-receipts/validate-record` requires `:initial-belief-receipt :present` whenever candidates exist (scoring_input_receipts.clj:57-58) and checks the token-belief stage; the reading route has no initial-belief carrier. The habit-read join it also checks is live and re-provable once the validator's reading-route rule is ruled. |
| `temporal-consume-test` | 8/4/3/6 | unchanged | Not migrated: zeta posteriors are published by the retired token-rate lanes. Reconnection obligation (ruling 2026-10-15). |
| `token-belief-carry-test` | 4/11/8/1 | unchanged | Not migrated: token-belief stage / prospective carry. Reconnection obligation (ruling 2026-10-15). |
| `token-belief-predecessor-test` | 3/18/7/0 | unchanged | Not migrated: token-belief input / predecessor validation. Reconnection obligation. |
| `token-observation-initialization-test` | 7/36/16/0 | unchanged | Not migrated: signed observation initialization of the token belief. Reconnection obligation. |

New-route controls after the packet (reading-family-decision, reading-model-contracts,
selected-reading-runner, reading-policy-precision): 48/336/0/0.
