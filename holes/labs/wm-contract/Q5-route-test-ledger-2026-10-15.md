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

