# C283 — quiet-window non-mutating dry run

Date: 2026-09-01. Owner: `wm-organization`.

No quiescence was attempted, no production reload occurred, and no click was
sent. The checklist's read-only and refusal paths were exercised in order.

## 1. Quiescence — correctly refused

The five-repository status loop found untracked files in Futon3c and p4ng.
The lane registry reported three active holdings and one completed-but-stale
holding; it exited 1 on `wm-evidence`'s `:stale-holding`. A bounded Futon3 suite
job was active during the observation. This is not a quiet window, and the
checks did not call it one.

## 2. Runner reload preflight — correctly withheld

`make runner-reload-preflight` passed canonical source, `main` branch,
readable commit, clean Futon2 repository, and namespace resolution. It failed
only `tested-commit` with `no-clean-bounded-receipt-for-current-commit`, printed
`reload-command: null`, retained the exact command under `withheld-command`,
and returned script exit 1 / Make exit 2. It performed no reload.

## 3. Readiness — correctly refused, with one stale seam found

`make run-readiness` returned `NOT-READY (needs-you)` and script exit 1 / Make
exit 2. Named blockers were:

- workspace gate: old failing receipt, missing current workspace basis;
- Futon2 suite: receipt not current for the dirty tree;
- serving runner code: `namespace-not-loaded`, operator action.

Reviewer selection, contract freshness, v20 readback, Futon3 suite, bounded
capacity, and the certifier command check passed. Readiness consumed the prior
gate receipt; it did not start a gate.

The run exposed a stale output seam: readiness still printed a bare `curl`,
which would bypass the external resource observer required by C264/C247. It
now prints the observer invocation with a fresh receipt pathname and selected
reviewer. This changes no click or readiness predicate.

## 4. Missing run ID — correctly refused

`make certify-run` without `RUN_ID` printed `RUN_ID is required`, returned Make
exit 2, and did not change the set of operational-certificate files (the
before/after filename-list hash was identical).

## 5. Observer argument boundary — callable and fail-closed

Invoking `bb -cp . checks/wm_click_resource_observer.clj` without arguments
printed `usage: wm_click_resource_observer.clj RECEIPT REVIEWER`, exited 1,
and left workspace status unchanged. The CLI now validates both arguments
before reaching `observe!`; a receipt path without a reviewer is also refused
rather than posting a click with a null reviewer.

The successful observer path necessarily sends the production POST and was
therefore not exercised. Its injected, non-production path remains covered by
the C230/C264 chain rehearsal.
