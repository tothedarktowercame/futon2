# Review 1164ce68/a2d4079b/f09c19d6: advancement race

Independent service suite: 11 tests / 63 assertions pass. f09c19d6 invokes a
private helper with stubbed lifecycle and resolver; it is not an actual service
restart or controller non-advancement gate.

Retained historical-reconcile-race-review.clj reuses real materialized linked
service/store and actual controller. The scheduling seam writes a valid terminal
through the real controller after the service reconciliation precheck, then lets
the pending controller call proceed. This simulates another request completing
in that interval. No authority validators are stubbed. Ordinary task core remains
the existing U88 fixture, as in the accepted linked-service test.

Observed: interleaved call writes :trial-terminal/:succeeded. Pending call
returns :series-terminal, yet repair/open-obligations still contains repair-057.
The two expected test failures reflect changed response shape; the substantive
failure is controller completion without the linked repair resolution.

Precheck outside controller's JVM/OS lock is not sufficient. Add a server-owned
reconciliation hook invoked under the advancement lock before skipping each
persisted linked terminal or starting its successor. Do not obtain a second
non-reentrant OS lock inside the hook. Fresh durable validation and immutable
resolution must remain authoritative; failure prevents advancement. Retest
actual interleaving, post-publication failure/replay, already-resolved replay,
failed task, source drift and no-new-dispatch behavior.

Packet work need not invent completed verification evidence. Preregister the
verification plan/check population/identity and disabled admission packet now.
Keep the linked successor packet explicitly unmaterialized until its actual
verification execution receipt exists; describe exact receipt-to-link derivation.
Neither a proposed execution ID nor a planned check is evidence of execution.
No production capacity, store, attempt, credential or service changed.
