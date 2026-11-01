From claude-11. One test, no source change. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

The path-level acceptance witness codex-11 requires before retiring injury
a59c85a6 (`/home/joe/notes/wm-next/criteria-completeness/t3-retirement/acceptance-boundary.md`;
read it first, its five expectations are the spec). The repair is futon2
`cc050c0a7` (held-open-cascade-resume catch at `full_loop_runner.clj`, around
the old `:9013-9033`, now calling `record-held-resume-stop-line!`). The
existing test `t3_held_resume_stop_line_test.clj` drives only the helper.

## The witness (meet all five expectations in the boundary note)

1. Drive the real held-open-cascade-resume catch through its supported runner
   entry, with the cascade-resume handler returning the typed error that
   becomes `::held-open-cascade-resume`. The helper is not the entry point.
2. Capture the actual `:opportunity/:end/:incomplete` observation the catch
   emits (attempt identity, repair root), via the runner's own phase sink
   hook, not a replaced emitter. Do not stub T3 or the producer.
3. Negative: with only the producer call removed (a `with-redefs` of
   `:repair-system-record-fn` to a no-op is acceptable as the negative, say so),
   real T3 on that observation yields `[:missing-durable-stop-line]`.
4. Positive: at the moment the end event is published, the store holds the
   ordinary producer's finding with the exact qualified attempt id, `:open`,
   `:incomplete-recoverable`, expected repair root, and T3 yields `[]`.
5. The result stays held/incomplete with its checkpoint; and a producer that
   throws does not yield a clear T3 or a successful result.

Hermetic stores (count files before/after). Retain the test's output log under
`/home/joe/code/storage/test-registry/` with the commit sha it ran at, for
codex-11's replay receipts.

## Gates

kondo, check-parens; run only this test namespace and
`futon2.aif.t3-held-resume-stop-line-test`; commit only the test file by
explicit path. No reloads, no clicks, no ledger operations.
