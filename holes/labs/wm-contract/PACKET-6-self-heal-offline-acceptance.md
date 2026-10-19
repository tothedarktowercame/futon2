# Packet 6 — offline acceptance

Date: 2026-10-19. No live click, Agency dispatch, serving-state mutation, or
scheduler re-arm was performed.

The on-demand client now gives every submission the stable idempotency key
`wm-on-demand/<run-id>`. A lost POST response is retried only with that same
key. Completion polling is scoped to
`GET /api/alpha/wm/click?click-id=<accepted-id>`; global `last-result` is not
an acceptance or completion authority.

Offline call graph:

`run-on-demand! -> POST durable acceptance -> per-click GET -> terminal result`

The self-heal route validated offline is:

`terminal classifier -> durable injury ledger -> active-injury arbitration ->
sealed work order -> coordinator ports -> attributable retry -> discharge ->
ordinary eligibility`.

Sequential resource-bounded gates covered ledger/property, classifier/store
and campaign replay, coordinator negative modes, the real-git fake-seat
harness/restart path, production arbitration/runtime/wrong-route controls, and
the Packet-6 client. The Futon3c receipt records the server-side acceptance
gate.

Remaining production boundaries are deliberately untested: real HTTP serving
restart recovery of an already-running worker, live Agency seats, sanctioned
reload, attributable live retry, and a real click. The offline contract is
satisfied; live success is not claimed.
