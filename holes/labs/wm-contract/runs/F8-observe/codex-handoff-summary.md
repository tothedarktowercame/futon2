# F8 leg 1 slice 5 handoff

Commits: mathlib4 `169662b19653d20c76274bdce4845ab414da10cf`;
futon2 **this commit** (its SHA is reported in the bell reply because a commit
cannot contain its own hash without changing that hash).

The registry carrier is `machineObservation`. It reuses `Holes.Channel` and
`Holes.ObservationVector`, pairs the total numeric vector with an absence
envelope, represents the advertised `[0,1]` range as `BoundedObservation`
rather than falsely refining the carrier, and requires an `EnvelopeMatches`
proof at the vector boundary.

All three dispatched findings reproduced. Pass-through inputs produced
`:stack-pct 70.0`, `:loop-health -3.0`, and `:active-repo-ratio 5.0`, while
the two explicitly clamped channels returned 1.0 at and above their caps. An
incomplete present summary threw `java.lang.NullPointerException` with message
`Cannot invoke "Object.getClass()" because "x" is null`; this is stated and
recorded, not repaired. Empty input produced fourteen numeric zeroes with
fourteen absent variants. A mismatched envelope was refused with
`clojure.lang.ExceptionInfo` and the production matching-envelope message.
Every numeric readback delta is 0.0.

Gates after the last source edit: direct Lean main exit 0, 0 errors, 0 warnings;
direct Lean witness exit 0, 0 errors, 0 warnings; Lake build exit 0, 2706/2706;
axiom audit exit 0 over 29 declarations, 0 `sorryAx`; clj-kondo exit 0, 0
errors, 0 warnings; check-parens exit 0, OK; scoped observation tests exit 0,
6 tests / 30 assertions / 0 failures / 0 errors; deterministic readback exit
0 with SHA-256 `13306f7001e36cbe740ff5d58f28bb53e218bb5b1309be7fe8db3c230a71208b`.

The packet's literal `clojure -M:test -n futon2.aif.observation-test` exited 1
before running tests because this repository defines `:test` with `:exec-fn`,
not `:main-opts`; Clojure treated `-n` as a filename. The supported,
namespace-scoped `clojure -X:test :nses '[futon2.aif.observation-test]'` command
produced the green counts above.

No production source, registry, worklist, control map, report, p4ng file,
publish, or generated DAG was changed. No live tick or run lock was taken and
nothing was written under `data/`.
