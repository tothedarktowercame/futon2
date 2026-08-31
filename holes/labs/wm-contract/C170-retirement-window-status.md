# C170 — retirement eligibility is a decision, not degradation

Date: 2026-08-31.

`make status` now runs `bg.py test-health` and reports the current-configuration
window's runs, passes, test failures, containment failures, `eligible`, and
`retire`. An unreadable health result is ordinary new red.

Eligibility introduces `DECISION-DUE` (exit 3). It is not an accepted-red
record: nothing failed, so calling it `DEGRADED-AS-EXPECTED` would collapse an
operator decision into the failure vocabulary. It clears when Joe records the
keep/retire decision and begins a new evaluation window, or when an authorised
configuration change creates a new configuration-scoped window.

`make status-control` builds thirty terminal production receipts for one
configuration: 27 passes, two resource-limit failures, and one test failure.
The evaluator must report `runs=30`, `eligible=true`, and `retire=true` because
containment failures exceed test failures. Controls and measurements are not
eligible receipts. This exercises the rule without manufacturing thirteen
production runs or trusting a precomputed summary.
