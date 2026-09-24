# Stop-line discharge — the repaired-and-never-discharged twelve, 2026-09-24

Executed by kimi-3, belled by claude-5. No clicks (`POST /wm/click` untouched);
no finding edited or deleted; `grounded?` / `:resolved?` unweakened. Store
writes: nine `resolutions/` records, via the two authorized writers.

Script: `discharge.clj` (clj-kondo 0/0; futon4 check-parens OK;
`futon2.aif.repair-obligation-test`: 34 tests, 233 assertions, 0F+0E beforehand).
Full stdout: `output.txt`.

## Pre-discharge honesty checks (read-only)

- All twelve `implementations/` records read; their `replacement-commit`s taken
  as the grounding claim.
- Live substrate read-back (futon1b, restarted 02:47:39Z with the rescue-ladder
  repair): `GET /api/alpha/entity/full-loop/implementation/<commit>` for all
  twelve commits. Ten read back honestly — `:props :implementation/commit` ==
  entity-id commit and `:implementation/target` == the finding id. Two have NO
  entity: `f19e38557a` (attempt-047) and `190568dd` (9b6ce3a4--001) — those
  repairs were manual and were never grounded.
- All grounded closes surveyed across `data/wm-full-loop*` (52 grounded closes).

## Classified (a) — close exists and grounds honestly today; discharge ran (9)

| finding | R (repair close) | S (production-shaped successor) | route |
|---|---|---|---|
| repair-ea1-7093b8fb…--attempt-001-untyped-failure | machinery-50 att-002, 09-14T17:16, 642ab3eb | machinery-53 att-002, 09-14T22:18 | successor-resolution! |
| repair-ea1-3f4cac24…--attempt-001-untyped-failure | machinery-51 att-001, 09-14T18:21, 5e7708da | machinery-53 att-002 | successor-resolution! |
| repair-initialization-a9177cab-… | machinery-51 att-002, 09-14T19:10, f06da0fa | machinery-53 att-002 | successor-resolution! |
| repair-ea1-3f4cac24…--attempt-003-artifact-binding-mismatch | machinery-54 att-001 adjudication 09-15T00:10, 1344347a | machinery-55 att-002, 09-15T01:53 | successor-resolution! |
| repair-ea1-b0eeafa0…--attempt-001 | machinery-55 att-002, 09-15T01:53, 0476071e | machinery-59 att-002, 09-19T15:06 | successor-resolution! |
| repair-ea1-504ad863…--attempt-001 | machinery-59 att-002, 09-19T15:06, 9dfdbcac | machinery-60 att-001, 09-19T16:42 | successor-resolution! |
| repair-initialization-6d5da36a-… | outer-loop-45 attempt-056, 07-25, 087dc4d7 | outer-loop-46 attempt-059, 07-27 | resolve! (no run/id recorded for outer-loop runs; mirrors resolutions/repair-attempt-029\|030) |
| repair-attempt-051-feature-card-missing-or-invalid | outer-loop-46 attempt-059, 4110519b | outer-loop-46 attempt-060 | resolve! |
| repair-attempt-052-strategic-selection-unavailable | outer-loop-46 attempt-060, 3bdc381e | outer-loop-46 attempt-061 | resolve! |

Judgment call, flagged: for 3f4cac24--003 the implementation attempt was
orphaned AFTER its `:authoritative-substrate-discharge` adjudication
(2026-09-15T00:10:13Z, grounded witness, entity honest today) and never wrote
007-closed — the exact case the runner's close-fallback comment describes. The
adjudication's recorded-at stands as R's closed-at; codex-18 previously used
the same adjudication as S when resolving repair-attempt-029.

## Classified (b) — close exists and legitimately does not ground (1)

**repair-initialization-a3e2319f-…**: machinery-55 attempt-003's terminal close
(2026-09-15T02:31:23Z) is `:outcome :build-failed`, `:grounded? false`, refusal
`:evidence-not-single-edn` (close-retention: "Attempt evidence must contain one
EDN form", derived.stderr). Its adjudication 4s earlier IS grounded and the
entity 76c7088b reads back honestly — but discharging a finding whose repairing
attempt terminally failed is a judgment I would be making over the machine's
own terminal record. Owed: a fresh validation dispatch (the grounding is
already in place; a validation run should close it cleanly). The close failure
itself opened a separate finding
(repair-ea1-9b6ce3a4…--attempt-003-evidence-not-single-edn), not mine.

## Classified (c) — no close was ever written (2)

**repair-attempt-047-review-execution-evidence-missing**: manual repair by
claude-1 (2026-07-22, commit f19e38557a). No run close, and no grounding
entity — `full-loop/implementation/f19e38557a…` does not exist in the
substrate. The implementations/ record asserts `{:resolved? true :dial-moved?
true}` but the substrate contradicts it. Owed: a repair-validation dispatch
that grounds the commit (cannot run while the line is stopped and no clicks
are permitted).

**repair-ea1-9b6ce3a4…--attempt-001-artifact-binding-mismatch**: manual repair
by codex-15 (`commit-identity-binding-2026-09-19`, commit 190568dd). Same
state: no run close, no grounding entity. Owed: same as attempt-047.

No dismissal route is proposed for either; if one is wanted it would have to
be a new typed route (e.g. :ungrounded-manual-repair) with its own proof
carrier — described, not built.

## Board count

`repair/open-obligations` at run time: **before 43 open / 26 :machine-failure;
after 34 open / 17 :machine-failure.** Nine resolutions written
(resolved-at 2026-09-24T04:04:03Z). The bell's figure of 29 open
machine-failure findings did not match the live read at run time (26); the
delta predates this run and is noted for claude-5.
