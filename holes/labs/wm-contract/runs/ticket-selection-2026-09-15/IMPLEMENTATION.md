# Ticket selection implementation — packet 21033

Source: futon2 a24cb621; futon3c 35022e42. Exact diffs: futon2.diff and futon3c.diff. Source-only implementation, not loaded or clicked.

The earlier prediction blocker is resolved by the lead’s explicit declared prior (invoke-1789479853453-21033-f928800f): a ticket is one open item, f=advance-mission-ordinal-factor(1), mission-health delta 0.04*f and variance 0.015, addressed-event weight weight*f. No sorry-count prediction. This is shared prior geometry, not calibrated ticket success or a derivation from observed completion frequency. The lead-owned blocker TN was left untouched.

Triage now measures missions plus tickets without changing the shared mission inventory. Ticket statuses map to complete/inactive/not-actionable/open. Ticket completion raises health; tickets never enter blocked or abandoned-in-progress penalties. Registry scan fences, classification, proposal, executability, authority, independent completeness, document/psi resolution, runner lookup and shared transport guardrails are wired.

## Verification

| Suite | Tests | Assertions |
|---|---:|---:|
| Ticket integration | 4 | 44 |
| Existing mission registry, unchanged tests | 19 | 54 |
| Forward model | 24 | 72 |
| Controller authority | 4 | 25 |
| Enumeration completeness | 10 | 35 |
| War Machine report | 95 | 544 |
| HTTP including ticket guardrail resolution | 9 | 42 |
| Guardrails | 6 | 34 |
| Experimental selector | 9 | 39 |

All final suites: zero failures/errors. clj-kondo: zero errors/warnings; the existing full_loop_runner informational str message is retained. Explicit-path check-parens passed. Raw logs retain two initial ticket-test harness failures (missing closing parenthesis, then private predict-effects access); the corrected focused suite passes. No full live tick or production activation was performed.

registry-readback.json retains the full 34-ticket snapshot and exact 20 live identities. completeness-readback.json independently reports complete agreement with the actual enumerator. These are read-only source/registry checks, not admission or execution evidence.

## Exact live-ticket list

- `T-car3-phase2-impl`
- `T-fail-invoke-error`
- `T-watu-replay-layer`
- `T-cx-new-blocks-emacs`
- `T-fail-agent-not-found`
- `T-fail-invoke-interrupted`
- `T-fail-operator-cancelled`
- `T-mq7-unaddressable-caller`
- `T-wm-wrong-corpus-26082026`
- `T-fail-no-execution-evidence`
- `T-zai-chat-transient-timeout`
- `T-fail-worker-lost-on-restart`
- `T-car3-queue-routing-design-note`
- `T-jvm-provenance-before-multi-jvm`
- `T-kangaroo-caching-and-compaction`
- `T-wm-turns-are-not-operator-turns`
- `T-fixture-becomes-registry-26082026`
- `T-fail-activation-authority-unavailable`
- `T-evidence-pinned-to-mutable-prose-26082026`
- `T-strategic-cascade-emits-disconnected-patterns`
