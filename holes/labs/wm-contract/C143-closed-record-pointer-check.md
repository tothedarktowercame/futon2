# C143 — closed-record pointer enforcement

Date: 2026-08-31

`checks/closed_record_pointer_check.clj` resolves all contract declarations of
kind `closed` through a closed set of repository conventions or an explicit
`record: repo:path` owner marker, then requires the target to be a regular file.
It does not claim that the record semantically proves the declaration; C141's
sample remains the judgement layer.

## Before and after

Before repair, **2 of 89** closed declarations failed:

- `TickRunRecord`: owner text ended in `runs-once receipt`, while the supporting
  record is `holes/problems/BUILD-packets/WM-RUN1.md`.
- `RouteHop`: owner text ended in `route tracer`, while the supporting record is
  `holes/problems/BUILD-packets/WM-RUN2.md`.

Both owners now retain Joe and their stable holder-registry suffix while adding
the explicit packet path. After repair: **89/89 resolve, 0 fail**.

`--negative` changes `Channel` to an explicit absent record and is rejected.
The positive check and negative control are both classified in the workspace
gate.

## Holder-registry counts

The twelve `:decls` values summed to 80 against the current 100-declaration
contract. `holder_check` never consumed them. They have been deleted rather
than refreshed: declaration population belongs to the generated contract, and
a second hand-maintained count would recreate the C35 stale-literal defect.
Holder ownership remains the registry's single job.
