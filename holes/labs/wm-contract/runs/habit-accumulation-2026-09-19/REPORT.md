# WIRE-habit-accumulate — author handoff, 2026-09-19

Implementation: `4c47b645430a9aa6b17c192d52445418af4b8c36`. Author: codex-33.
Acceptance remains for an independent reviewer.

`judge` now calls `select-and-record-cascade!`, which calls the existing
`cascade-decision`, records its selected representative, and returns the same
result. No prior is attached to the selector. Production persistence is
`futon2/data/wm-habit/cascade-prior.edn`, with a stable `.lock` sidecar.
The writer serializes read/fold/replace, syncs file contents and publishes by
atomic replacement. Tests write only temporary stores; no production habit
file was seeded and no JVM was reloaded. Clicks consumed: 0.

Identity follows r108: target in the existing mission slot, ordered pattern
IDs with string/keyword types and namespaces intact, and empty topology for
flat precedence. Empty vectors key; missing/non-vector/invalid IDs do not.
Mixed string/keyword conventions raise `:mixed-pattern-id-types`. Existing
explicit topology is retained. Each counted key has its selection basis in
`:selection-bases`: `:first-ranked-sharing-chosen-action`. Counts are selections
of a representative, not evidence of successful actuation.

## Registered execution evidence

Acceptance warrant (subject-bound to `WIRE-habit-accumulate`):
`test-registry-78d77a1a608a93370f56c3fafec3b24b47bcfd2d48e7c4a46cc1fea2153d95d8`.
Binding ID: `b74f3084-d4f1-47c6-a224-c283171f514e`, through
`futon3c.test-registry.validation/bind-subject!` in the canonical subject index.
5 tests, 32 assertions, 0 failures, 0 errors; process duration 3123 ms.
Execution receipt: `fe049e9b-b068-4415-abb5-352763c9fd1c.log` beside this report.

Existing prior regression warrant:
`test-registry-2292c2bb18659df82135ade4ce24ccbbbc90f1ef2e9fb960e5e3d0daf4977ddb`.
6 tests, 22 assertions, 0 failures, 0 errors; process duration 790 ms.

All four changed Clojure files passed clj-kondo (0 errors, 0 warnings) and
`futon4/dev/check-parens.el`. The registry initially refused uncommitted inputs;
direct tests were run before committing, then both namespaces were registered.

## Measurements and scope

Six offline selections use a nonempty policy read from the declared production
source `M-wm-08-external-f2.edn`, plus its empty alternative. Both are eligible;
G alternates which choice is preferred, beta is 2, and F is 0.2 / 0.7.
The before-fixture was captured from the unchanged selector; both arms cross
the same EDN read boundary for stable printing of unordered collections.
Six complete decision-byte comparisons pass through the selection/recording
wrapper, with the upstream constructor replaced by the real selector over
these fixed scored inputs. A separate test runs the actual cascade constructor,
scorer and selector through the wrapper and compares the whole live decision.
This is offline replay, not a full `judge` scan or a production click.

After six selections:

- `[:pattern-cascade "M-wm-08-external-f2" [] {}]`: 2 observations.
- `[:pattern-cascade "M-wm-08-external-f2" [:cascade-construction/run-it-on-a-real-case] {}]`: 4 observations.
- ln E, empty then nonempty: `[-0.9808292530117262 -0.4700036292457356]`.
- E: `[0.375 0.625]`.
- File size: 474 bytes. After 100 further nonempty observations: 478 bytes,
  106 samples, still 2 keys (4 bytes / 100 repeated selections).

Storage is O(distinct policy identities), plus counter digits, not O(ticks).
A fixed menu needs no per-tick history bound. A stream of new identities can
still grow the state without limit; this replay does not measure that churn.
No arbitrary eviction bound was introduced, because it would discard learned
counts and change the learning contract. The read side still needs to convert
ln E to E before supplying `:habit`; this change does not address that mismatch.

## Findings that limit the packet's claims

The prior contradictions are resolved by the corrected rulings. Whole wrapper
results from two independent constructor calls are not byte-identical: lane
route telemetry carries fresh wall-clock timestamps. Complete live decisions
are byte-identical; the test compares those without deleting certificate or
selection fields. The first direct run caught this test-scope mistake.

The habit records the first-ranked representative sharing the chosen Bayes
action. It does not establish that a unique complete policy was sampled or
that downstream execution succeeded. The existing selection semantics remain
unchanged, and the persisted basis makes this distinction explicit.
