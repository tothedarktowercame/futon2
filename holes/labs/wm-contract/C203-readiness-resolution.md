# C203 — Readiness resolution kinds

Date: 2026-08-31

`UNAVAILABLE` and `UNVERIFIED` describe evidence, not remediation. They do not
map directly to the operator instruction: bounded admission can be unavailable
and self-clearing, while reviewer absence is unavailable and requires operator
action. Each readiness item therefore also reports one of:

- `self-clearing`: wait for the owning delivery/job to settle, then rerun;
- `operator-action`: Joe must select or restore the required live resource.

The summary remains `READY` / `NOT-READY` and qualifies the latter as
`(waiting)` or `(needs-you)`. Any operator-action blocker wins over any number
of waiting blockers. `make run-readiness-resolution-control` constructs that
mixed case and must report `needs-you`; `0=pass, 2=control-slipped`.
