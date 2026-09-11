# Materialized historical admission positive gate

Implemented local runner correction c6ed9f59 and combined fixture futon3c
03884e80. close! had deliberately avoided making another failure finding on
historical admission, but then overwrote :repair-obligation with that nil
finding. Preserve the existing verified transition for this outcome only.
Ordinary failure and task paths keep their prior behavior.

Before correction the composed actual async path closed the cohort but failed
projection with :historical-run-record-binding-mismatch; returned transition
was nil. After correction: 1 test / 18 assertions, zero failures/errors.
Full runner regression: 130 tests / 617 assertions, zero failures/errors.
Lint and parentheses pass on changed code and fixture; scoped diff check clean.

Fixture composes canonical U88 template copying/materialization, authenticated
series preparation, real admission, real qualification producer and verification
store, historical action ports, actual run-opportunity! core and async wrapper,
activated disposable cohort, routed record, immutable projection/click binding,
strict historical reader and observational recording. A repeated series step
retains one attempt and one close with no controller terminal; visibility pending,
task verdict unknown, canonical admission awaiting-validation. Execution and
controller identities differ and requested pin remains authenticated-not-enacted.

External fixtures: availability, selector judgment, environment reads, code-state,
substrate preflight, review job/execution evidence, and notification/report ports.
No external author/reviewer or real infrastructure-attestation result is claimed.
The current isolated selection has no route, so the real historical route
producer supplies the actual admitted stop-line route. Actual store, authority
readers, cohort, run writer and evidence publication are not stubbed.

Run from futon3c (requires sibling test helpers, and is deliberately slow):
clojure -Sdeps '{:aliases {:review {:extra-paths ["test" "dev" "../futon2/test"]}}}' -M:review -e '(require (quote futon3c.wm.run4-historical-roundtrip-test)) (let [r (clojure.test/run-tests (quote futon3c.wm.run4-historical-roundtrip-test))] (shutdown-agents) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))'

This is a positive integration gate for non-advancing historical observation.
It does not establish a production successor scheduling policy, repair resolution,
U88 success, operator acceptance, or live deployment. Independent review of the
local fix and fixture remains required before preparing any authorized next run.
