# Review 41fea9ec / 96e17d25 and linked service correction

Historical/ordinary validation partition in 41fea9ec preserves the ordinary
resolver and records historical deferral. The new service integration initially
failed its actual materialized caller: read-lifecycle! returns a map with :trials,
but the resolver searched the map directly. Therefore started was always nil,
raising historical-successor-authority-missing after writing the task terminal.
Local correction reads the validated :trials vector.

Retained historical-linked-service-review.clj composes real qualification,
verification/admission store, configured historical-successor link, materialized
U88 service, actual async wrapper/durable task evidence and strict successor
reader into immutable repair resolution. Ordinary task core is the existing
U88 fixture; it is explicitly stubbed. Historical producer/store/readers and
post-publication service caller are real. After correction: 1 test / 25 pass,
zero failures/errors; repair/open-obligations is empty after resolution.
Service namespace: 10 tests / 60 assertions, zero failures/errors. Lint/parens
and scoped diff checks pass. No live capacity, service or store changed.

Remaining mandatory recovery gate: the operation runs only when controller
step returns :trial-terminal. If post-publication resolution fails, replay
skips the existing terminal and may return complete/start another trial, never
retrying the resolution. Derive pending resolution from durable linked lifecycle
before advancing; retry must reread exact evidence and be idempotent, with no
new dispatch while reconciliation is incomplete. A failed task terminal must
not be reported as resolved; its separate observation should remain truthful.
Test injected resolution failure after terminal publication, restart/replay,
identity/source conflicts, already-resolved replay and failed task outcome.

Link schema also needs typed IDs: every? some? currently accepts false/numbers
as dependency values. Bind canonical verification ID/execution and exact frozen
successor identity, not merely whichever admission later occupies repair ID.
Do not freeze a production-ready packet until these gates pass independently.

Run from futon3c with test/dev and ../futon2/test on the review classpath:
clojure -Sdeps '{:aliases {:review {:extra-paths ["test" "dev" "../futon2/test"]}}}' -M:review -e '(load-file "../futon2/holes/labs/wm-contract/runs/RUN4-repair057-revalidation-2026-09-11/historical-linked-service-review.clj") (shutdown-agents)'
