# Fix-12: discharge identity

New cohort discharges use
`full-loop/discharge/cohort/<cohort>/run/<run>/attempt/<attempt>`.
Non-cohort opportunities use `full-loop/discharge/run/<run>/attempt/<attempt>`.
Each component is independently UTF-8 URL-encoded. The stored document also
records `:discharge/cohort-id` (when present), `:discharge/run-id`, and
`:discharge/attempt-id`. The witness returns the exact document id.

Cohort identity alone is insufficient. `full-loop-cohort/start-attempt!`
allocates ordinals from the cohort directory. `successor-id` increments a
charter's cohort number, but names are declared, not globally minted:
`execution-context` explicitly documents reuse across data roots and binds
preregistration bytes and data root into its execution authority. The runner's
`run-opportunity!` accepts a supplied run id or generates a dated UUID. We use
that existing run identity, never mint another id in the grounding writer.
Thus the same cohort/run/attempt has a deterministic id; a different run of
the same named cohort is distinct. Reusing a supplied run id denotes the same
run, as elsewhere in the runner. The grounding call takes the cohort from the
recorded start event, overriding any caller-supplied cohort hint.

Missing run or attempt identity refuses before substrate reads/writes. No
fallback to the colliding ordinal-only shape is provided.

## Reader census

Searched `discharge-id`, `full-loop/discharge/`, and `:discharge/` across
`src/`, `scripts/`, `test/`, `/home/joe/code/futon3c/src`, and p4ng Python files.

- `full_loop_runner.clj`: sole full-loop id constructor and writer; returns
  the reference in the grounding witness.
- `full_loop_cli.clj`: two projections copy the witness's `:discharge-id`.
- `delivery_qa.clj`: copies the witness id into evidence ids.
- futon3c `wm/run4_terminal_projection.clj`: copies the witness id.
- `actuator_a3.clj`: separate discharge producer/id function; reads discharges
  by mission and endpoint, not by parsing the full-loop id.
- `a4a_substrate.clj`: queries/projects mission, endpoint, and discharge type.
- `actuator_a6.clj`: reads capability discharge type and endpoint.
- Test references in `full_loop_runner_test`, `delivery_qa_test`,
  `close_terminal_retention_test`, `actuator_a3_test`, and `actuator_a6_test`
  use opaque ids or document fields. Direct grounding fixtures now supply a
  run id. `full_loop_discharge_test` tests the new identity explicitly.
- No matching readers in scripts or p4ng Python files.

No reader reconstructs `full-loop/discharge/<ordinal>`, so no reader needs a
lookup of both shapes. Existing documents and retained witness references
keep their old ids; this change does not migrate or repair overwritten data.

## Evidence and owner follow-up

Before the fix, the direct grounding regression failed four assertions. At
`full_loop_discharge_test.clj:22`, both returned ids were
`full-loop/discharge/attempt-001`; at line 23 there was one discharge instead
of two. The first document's endpoint was replaced by the second commit.

The new pure/direct namespace passes 3 tests / 15 assertions. The two existing
fire-pattern grounding tests pass 2 tests / 11 assertions. These invoke the
real `ground-commit!` with an in-memory substrate and need no source-guard
exception. clj-kondo and check-parens pass on all changed Clojure files.

Per the handoff, `cohort-grounding-uses-the-started-cohort-and-run` is written
but intentionally not executed in this branch worktree: it calls the guarded
`run-opportunity!`. The owner should run it on merged main, then the runner
namespace. It checks the real call-site forwarding, the stored cohort/run/id,
and the Morning Brief witness, including rejection of a wrong caller hint:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-runner-test -v futon2.aif.full-loop-runner-test/cohort-grounding-uses-the-started-cohort-and-run
```
