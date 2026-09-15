# P1b-2a: pure work-target tick proposals

Author: codex-2. Commission: `invoke-1789505880964-21235-707897db`,
2026-09-15, from claude-2. Independent review remains with claude-2.

## Scope and API

New namespace: `futon2.aif.work-target-tick`.

- `payload-validator` takes the already-read, pinned declaration envelope and
  returns the store's pure `payload -> :ok | refusal` function. It verifies
  the envelope and derives the exact model context once through P1a's empty
  carry. That validation-only call uses the declaration's decision date;
  it introduces no rows and contributes no payload timestamps. Each payload
  check validates every retained target's support, finite nonnegative unit
  mass, D identity, interpretation revision, decision reference, update label
  and time ordering. It also checks candidate/registry pins. No target is
  skipped because this tick's row-7 inputs do not include it.
- `predecessor-from-store` reads the committed payload at `[:snapshot :payload]`,
  as returned by the actual store API. It projects exactly the three P1a state
  fields. Explicit activation evidence is required. Every adapter stop keeps
  the original store status and reason; malformed heads/state shapes stop.
- `operation-identity` uses structured caller occurrence IDs plus the UUID
  store ID and genesis hash. Caller IDs may be nonblank strings or UUIDs.
  The identity excludes sequence and cutoff; the operation retains cutoff
  for the store's intent hash. Missing caller/scope components refuse.
- `build-proposal` produces one of `:inert`, `:stop`, or `:proposal`. It never
  reads, initializes or writes a store. Stops contain no payload, belief,
  lineage or row-7 results. Every per-target carry refusal stops the proposal
  and names its targets. `:open-mission` non-admissions keep their original
  candidate and reason and gain `:unresolved-full-policy-coverage`.

Times are caller-supplied ISO-8601 instant strings, compared as instants.
The payload cutoff comes from `tick-context :timestamp`; registry `:read-at`
is retained from the supplied acquisition snapshot. P1a's relative default
path is not used: the coordinator must read the declaration at its configured
absolute path before passing its envelope here. Tests use an absolute path.

Candidate identity hashes the ordered vector of action maps (unwrapping
`:action` entries). Encoding matches the store convention: recursively sorted
maps/sets, original sequence order, UTF-8, one trailing newline, fixed printer
bindings and an EDN round-trip check. Candidate count includes duplicates and
non-admissions. Registry pins include every supplied entry, sorted by ID.

The store hashes `[expected-head operation payload]`. Volatile execution
metadata must not be supplied in operation/payload inputs. A lost-response
lookup retains the original expected head, operation and payload. The bridge
has no automatic retry, recomputation or commit path. Row-7 results are outside
both the payload and the commit intent, as commissioned.

## Validation evidence

Final required commands, run from `/home/joe/code/futon2`:

```sh
clj-kondo --lint src/futon2/aif/work_target_tick.clj test/futon2/aif/work_target_tick_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)" -- --no-defaults src/futon2/aif/work_target_tick.clj test/futon2/aif/work_target_tick_test.clj
clojure -X:test :nses '[futon2.aif.work-target-tick-test futon2.aif.work-target-belief-test futon2.aif.work-target-store-test]'
```

Commands ran directly, without pipelines; stdout/stderr went to each `.log`
and the immediate exit status to its `.exit` file. Final receipts:

- `clj-kondo.log` / `clj-kondo.exit`: 0 errors, 0 warnings, exit 0.
- `check-parens.log` / `check-parens.exit`: OK, exit 0.
- `tests.log` / `tests.exit`: 33 tests, 556 assertions, no failures/errors, exit 0.
- `source-pins.sha256`: new source/tests, unchanged P1a/store sources, declaration.

Tests use the production-shaped pinned registry/candidate fixtures from P1a.
They cover the complete activation/store table, independent validator refusals,
a corrupted carried target omitted from current row-7 reads, instant ordering
across timezone offsets, operation collision/stability/scope, proposal
refusals without rows, population hashing and complete open-mission receipts.
Determinism includes comparing bytes from the actual store encoder.

The end-to-end test uses a real temporary filesystem store with this validator:
explicit test-only genesis, first introduction and commit, next-tick carry and
new ticket introduction, second commit with a simulated response loss after
the commit point, idempotent retry with the original expected head, stale-head
refusal for a distinct attempt, and resolution of the first ancestor reference.
An instrumented read confirms that the validator visits both snapshots.
This exercises local store behavior and a simulated lost response; it is not
new power-loss or durability evidence. Existing store coverage limits from the
independent review remain unchanged.

A final source review identified that malformed all-long masses could overflow
before row-sum admission refused them. The validator now rejects masses above
one first (necessarily invalid in a nonnegative unit-mass row), with a
Long/MAX_VALUE control. The final gates were rerun after that change.

## Boundaries

No specification/API conflict required stopping. P1a and store sources are
unchanged. No runner, judge, trace, registry acquisition, production path,
production genesis, reload or click changes are included. This is proposal
construction and isolated integration evidence, not production use or WM-02
completion.
