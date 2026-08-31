# C158 — trace schema compatibility census

Date: 2026-08-31

The live corpus has 801 records across 54 daily files. Declared trace schema
versions are:

```edn
{:unversioned 688, 2 2, 4 75, 6 1, 13 30, 14 5}
```

There are no v15-v20 records yet. Those six producer changes landed today,
but Joe's operator-triggered run remains unspent, so the corpus cannot yet
exercise their emitted shapes. This is loud absence, not evidence that the
new shapes have run successfully.

All current corpus reader entry points agree: `read-all-traces`, the full date
range, and `reduce-traces` each see 801 records; `recent-trace-records` and
`latest-trace-record` select the same final record. The oldest record is the
unversioned `2026-05-18T19:42:49.284838608Z` form with only the original eight
top-level fields.

Semantic reads of the v15-v20 evidence fields now go through
`trace-field-evidence`. Missing fields on unversioned or earlier-version
records return `{:status :absent :reason :predates-field ...}`. A declared
current version missing a required field returns reason `:malformed`; it
cannot enter the permissive legacy arm. Explicit zero is retained as
`:present`, never treated as absence.

The generic EDN readers intentionally preserve historical records byte-shape
rather than injecting synthetic keys. The typed accessor is the compatibility
boundary for consumers; no historical trace was migrated.

Canonical invocations:

```sh
clojure -M -m checks.trace-schema-compatibility
clojure -M -m checks.trace-schema-compatibility --negative
clojure -M:test -m cognitect.test-runner \
  -v futon2.aif.trace-test/older-trace-fields-have-typed-version-skew-absence-test
bb checks/reader_portability_lint.bb
clojure -T:build ci
cd /home/joe/code/futon3 && clojure -X:test
```

This check requires the JVM. Babashka cannot load the production trace
namespace's atomic-move exception class.
