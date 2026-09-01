# C467 — RUN11: trace records carry the run id their receipt carries

Item RUN11 of `worklist.edn`. What was wrong: RUN10 gave the diagnostic
*receipt* a run id (`tick-run-record-<date>-<run-id>.edn`) and left the *trace*
on one file per date with nothing in a record naming the run that wrote it. On
2026-09-01 three runs' records went into `data/wm-trace/wm-trace-2026-09-01.edn`
and the only discriminator was `:timestamp`
(`runs/2026-09-01-s1b/COLLISION-NOTE.md`).

## 1. The field

`:run/id` on the trace record — deliberately the SAME key and the same value the
receipt carries (`scripts/futon2/run_tick_once.clj:179`), so a receipt and its
trace record join by equality rather than by clock comparison.

The id reaches the record along the path `:wm-version` already used:

| step | pointer |
| --- | --- |
| one id minted per tick | `scripts/futon2/run_tick_once.clj:263-266` |
| into the judge opts | `scripts/futon2/run_tick_once.clj:203-217`, `:231` |
| onto the judgement | `scripts/futon2/report/war_machine.clj:5000-5006` |
| onto the persisted record | `src/futon2/aif/trace.clj:521-536` |

One id per TICK, not per run script: `run-tick-once*` mints a UUID per
invocation, so a 20-tick run has 20 ids and 20 receipts. That is what makes the
receipt→record join one-to-one.

## 2. Cost — measured, not assumed

Measured over the twenty S1b policy-detail records
(`runs/2026-09-01-s1b/wm-trace-s1b.edn`), `pr-str` bytes with and without the
key:

| record | before | after | delta |
| --- | --- | --- | --- |
| 0 | 1,045,286 | 1,045,334 | +48 (0.00459%) |
| 1 | 1,045,321 | 1,045,369 | +48 (0.00459%) |
| 2 | 1,045,290 | 1,045,338 | +48 (0.00459%) |

Record sizes across the twenty run 1,043,963–1,046,822 bytes. The delta is
identical on every record because a UUID string has fixed width. So the field is
UNCONDITIONAL — not behind a flag, and there is no flag-off byte shape to
preserve for it.

## 3. Absence is explicit, and which producers are absent

`:run/id` is present-only: a producer that mints no run id writes no key, and an
absent key means "this producer could not name a run", not "this record belongs
to an unnamed one". Two producers are in that position today and neither was
changed:

- the scheduled runner, `scripts/wm_scheduled_run.clj:113` — calls
  `trace/write-trace!` on a judgement it built without a run id;
- the full-loop runner, `src/futon2/aif/full_loop_runner.clj:2639` — same.

Tested as absence rather than nil:
`test/futon2/aif/trace_test.clj` (`run-id-is-absent-not-nil-when-the-producer-has-none-test`).

## 4. Round trip

`test/futon2/aif/trace_test.clj` (`run-id-roundtrips-through-the-shared-trace-file-test`)
writes three records into ONE per-date file — two runs plus one producer with no
id — through the real `write-trace!`, reads them back with `read-trace`, and
asserts the ids survive in write order and that exactly one record belongs to
the first run. That is the collision of 2026-09-01 in miniature, resolved by
identity.

`test/run_tick_once_test.clj` closes the wiring on the real entrypoint path:
inside the hermetic `full-diagnostic-tick-issues-no-http-post-test`, the opts
handed to `generate-war-machine` carry `:run-id`, and it equals the
`:run/id` of the receipt the same tick wrote.

## 5. RUN3 now selects by identity; timestamps are the fallback

`run3_conformance.bb` takes a RUN DIRECTORY as well as a trace file. Given a
directory it reads the run's `tick-run-record-<date>-<id>.edn` receipts, takes
the ids from their `:run/id` field (not from the filename, so a renamed file
cannot widen the selection), and selects from the shared per-date trace file
the records whose `:run/id` is one of them.

Records written before this landed carry no id. For those the run directory's
pre-extracted `wm-trace-*.edn` — which S1b built from a timestamp range — is the
FALLBACK, entered only when by-id selection finds nothing. Which path was taken
is printed and written into `conformance.edn` as `:selection`, with
`:selection-fallback-file` when it fell back.

S1b re-checked through the new entry point: `bb run3_conformance.bb
runs/2026-09-01-s1b` reports `selection by-timestamp-range (20 receipts, 20 run
ids, dates 2026-09-01, fallback wm-trace-s1b.edn)` and the SAME verdict as
before — 20 records, 180 hops, CONFORMANT, 0 refutations, 0 unmapped, 1
ruling-unrealised (R5→R6), 1 excluded (R2→R7), 19/22 drawn edges unfired. The
run's 20 receipts DO carry ids; its 20 trace records do not, which is exactly
the state RUN11 fixes going forward and cannot fix retrospectively.

## 6. Control on the selection itself

By-id selection would otherwise be unexercised code claiming to be the method,
since no record in existence carries an id. `run3_conformance_controls.bb`
control 4 plants a per-date trace file holding three records — two with this
run's id carrying the real S1b route (conformant), one with ANOTHER run's id
carrying a planted unknown pair R4→R20 (not conformant) — and one receipt naming
only this run. It asserts the check reports `selection by-run-id`, reads
`2 records, 2 routes`, and exits 0.

The control is sensitive: adding a second receipt naming the other run widens
the selection to `3 records, 3 routes` and the check reports `NOT CONFORMANT —
0 refutation(s), 2 unmapped hop(s)`. So passing means the filter selected, not
that the check is blind. `FUTON_WM_TRACE_DIR` exists for this control alone
(`run3_conformance.bb`, `trace-dir`).

## 7. Not claimed

- No war-machine tick was run. RUN11's acceptance asks for a round-trip test,
  not a live run; the field's behaviour under a real tick is covered by the
  hermetic entrypoint test and by the unit round trip, not by a record on disk.
  The first real record carrying `:run/id` will come from the next run.
- The two producers named in §3 still write no run id. That is the stated
  present-only case, not an oversight to be read as coverage.
- S1b's own records are not retro-fitted. Its conformance remains selected by
  the timestamp range its COLLISION-NOTE fixes.

## Gates

clj-kondo 0 errors / 0 warnings and `check-parens` OK on the five changed
Clojure files. `clojure -X:test :nses [futon2.report.war-machine-test
futon2.aif.trace-test run-tick-once-test wm-run-lock-test]`: 86 tests, 322
assertions, 0 failures, 0 errors. `bb run3_conformance_controls.bb`: PASS.
`bash p4ng/empirics-futon/negative_controls.sh`: PASS (16 negative, 10
positive). `bb p4ng/empirics-futon/pointer_check.bb`: 207 pointers in 3 files,
0 unresolved.
