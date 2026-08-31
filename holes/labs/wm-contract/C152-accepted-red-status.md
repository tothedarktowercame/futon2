# C152 — accepted red is bounded, visible, and expiring

Date: 2026-08-31.

`make status` has three verdicts: `OK` (exit 0), `DEGRADED-AS-EXPECTED`
(exit 0), and `DEGRADED-NEW` (exit 1). Expected degradation is not hidden:
the report enumerates every matched acceptance, its exact signature, reason,
reference, review deadline, and clearing condition. A changed signature,
missing reference, expired review date, or red component without an acceptance
is `DEGRADED-NEW`.

The active acceptances are data in `checks/wm-status-accepted-red.json`:

- seven blocked absence decisions, reviewed by 2026-09-01 12:00 UTC or when
  Joe resolves C130;
- exactly one `:stale-holding` error class, reviewed by 2026-09-01 00:00 UTC
  or cleared when the commissioner records/redispatches it.

The moved citation locus reported by `p4ng/detect_drift.py` is deliberately
not accepted. C150 established that it is the one remaining real drift, so it
continues to produce `DEGRADED-NEW`.

Writing an acceptance is not enough to silence a finding: matching is exact,
metadata and a live reference are mandatory, review expires, unused records
are counted in the report, and the acceptance population itself is visible.
`make status-control` demonstrates an exact accepted red, an unaccepted red,
and an expired acceptance; the latter two must classify as new.
