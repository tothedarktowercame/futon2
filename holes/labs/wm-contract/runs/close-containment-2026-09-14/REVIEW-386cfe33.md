# Independent review — zai-5's 386cfe33 (dispatch-seat registration)

Reviewer: claude-15, 2026-09-14. Context: this commit landed on main during
the cohort-53 attempt-001 loop run (round 1); the loop's round-2 rejection
targeted the sibling amendment 69e1062e (freshness hole, since fixed at
94ce60fe), not this commit — so it sat on main with no completed review.

VERDICT: **ACCEPTED with two notes.**

The mechanism: `ensure-dispatch-seat!` registers `wm-full-loop` (type
claude, delivery-mode inbox) at every `run-opportunity!` start, idempotent
(409 ignored), fail-open with a stderr note, 2s timeout. It addresses the
recorded 2026-09-13 incident: the author job's terminal delivery answered
caller-not-a-registered-seat and the reviewer's in-thread reply 404'd —
both from the same missing registration. Matches the wm-build-loop
`ensure_seats` precedent. The ordering test (registration strictly before
the core) is sound.

Notes:

1. **Test-suite mesh noise.** `default-agency-base` is the live :7070 and
   the call is unconditional, so runner tests that do not override
   :agency-base now POST an idempotent registration to the real agency per
   run-opportunity! invocation. Fail-open + 409 + fast connection-refused
   makes this noise, not harm, but a future one-liner should make the
   registration injectable (`:ensure-dispatch-seat-fn` opt) so the suite
   stops touching the production mesh.
2. **A standing operator rule goes stale at the next serving-JVM reload:**
   "wm-full-loop is not a registered recipient — never bell it" stops
   being true once this code runs in production; in-thread bellbacks to
   wm-full-loop become routable. Author-turn reply text remains a valid
   channel (the runner still reads job results).
