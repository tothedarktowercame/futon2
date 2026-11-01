From claude-11. Clojure task, small repair. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Close deficit B2-D4-R28 before run001 (codex-11's disposition,
`holes/labs/wm-contract/run001-v2-disposition.md`). codex-13's discovery
(`DISCOVERY-run001-e3-r28-2026-10-28.md`, `f619b62a6`): on a failed
selected-target reading or redecision, the full preparation record
(`:records`, `:pending`, ...) lives only in the exception until close, and
the close projection (`full_loop_runner.clj:918-957`) drops exactly those
fields; the success checkpoint (`:7840-7849`) is never reached.

## Change (smallest)

At the two failure points of the with-task reading path in
`src/futon2/aif/full_loop_runner.clj` — the `:selected-target-reading` phase
failure and the redecision catch at `:7427-7435` — before rethrowing:

1. write the complete preparation record (the `record` / reading result the
   exception carries, unprojected) as EDN to a durable file beside the run's
   other records (e.g. `data/wm-runs/<run-id>-selected-target-reading-failure.edn`,
   following how `persist-run-record!` chooses its directory; cite it),
   atomically (temp + rename);
2. add `{:path … :sha256 …}` of that file to the rethrown ex-data under
   `:selected-target-reading/failure-record`, so the close projection, which
   keeps scalar children, retains the reference.

If writing the file fails, keep the original exception and add the write
error to ex-data; do not mask the reading failure.

## Tests

Hermetic (temp run-record dir; count files before/after): a redecision that
throws produces the failure file containing `:records` and `:pending`
byte-for-byte, and the persisted run record's failure detail carries the
path and a matching sha256. Same for a reading-phase failure. A successful
reading writes no failure file.

## Gates

kondo, check-parens; run only the new test namespace (and any existing
runner test namespace you touch); commit only changed/new files by explicit
path (futon2 is shared). No reloads, no clicks.
