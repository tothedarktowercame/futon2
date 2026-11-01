# L5-8 — retain the enumerated roster

Base: 96a8a643c (packet-containing main). Branch: codex11/l5-8-roster-pin.

The serving authority now declares its mission-registry route as substrate.
giraffe-field materializes open-entries once, retains sorted missions/excursions/tickets
under field-facts/open-tasks, and hashes the UTF-8 pr-str of those three vectors,
in that order. The field partition and roster therefore use the same loaded entries.
Explicit tooling authorities can declare registry-source :files; an authority without
provenance is labelled :unspecified rather than falsely claiming a registry read.
The source label identifies the mission registry route; tickets/excursions retain their
existing loaders. No gating predicates or exclusions changed.

The selection projector overrides only its three open-roster facts when the retained
roster is present; sources points into that exact record section (including referenced
selection records). Historical fallback and the work exporter are unchanged.

## Regression and validation

The added test uses the real Giraffe writer/verifier fixture, an operator-gated mission
with no pass row, and a deliberately disagreeing projector snapshot. It verifies:
sorted groups, reproducible pin, exact enumeration/roster equality, gated-task omission,
selection projection from retained missions/tickets, source attribution, old fallback,
and unchanged work projection. All 11 new assertions pass.

| Run | Tests | Assertions | Failures | Errors |
|---|---:|---:|---:|---:|
| Base, outer + projector namespaces | 170 | 689 | 19 | 2 |
| Branch, same namespaces | 171 | 700 | 19 | 2 |
| Branch, observation + projector only | 81 | 303 | 0 | 1 |

Every failure/error test name and location in the broad comparison matches base
(comparison.json). Named error is the existing real-giraffe-run-shape refusal.
Kondo: zero errors/warnings on the three changed Clojure files. check-parens.el: OK.
git diff --check passes.

Tests use the existing fresh per-JVM test data root and scratch overrides for discovered
FUTON_WM_* root/dir/log/lock/config variables. Canonical data file counts:
base 45798→45798; branch 45798→45798; named rerun 45799→45799.
The increase between runs is not hidden by these per-run counts.

**Hermeticity gate did not pass:** all three runs exit 86 because the global shutdown
snapshot sees concurrent changes to wm-click-loop/click-001-1793554898.log;
base and broad branch also see wm-click-loop/acknowledged.json. Read-only process
inspection retained the concurrent claude-2 continuous repair loop (PID 4028283)
and launcher (4030850). Those script paths own the reported files. No loop was
stopped, no guard disabled, no production bytes restored. The snapshot is global,
not process-attributed; unchanged counts alone are not a no-write proof.
No reload, merge, live selection, or intentional production-data write in this packet.

Evidence: /home/joe/notes/l5-8/{baseline,changed,named}.{log,json},
comparison.json, kondo.log, parens.log, concurrent-loop.txt.
Changed log sha256: 241ba20cd86f885be1e0b6307ad3457a6a6180f8ef5ffcd6a16be48f667dbb14.
Base log sha256: c328e9fc49adeb79217ab8a4c3f9bc15ebe980389e09b7aaaf0d76f7dd2d7c7f.
The first focused invocation caught an extra test closing paren (fixed before these runs).
An initial probe JVM hit native-thread limits; rerun with ActiveProcessorCount=2 succeeded.

## Registry probe

The requested finding file is appended with the full table from one read-only probe:
2026-11-01T17:57:45Z, substrate 344 missions, files 342.
There are **17 gating-flag disagreements, 15 changing live membership**, not the
earlier 14. All 17 are substrate true / files false; two are non-live in both routes.
This is fresh evidence, not a reconstructed historical roster.
Raw probe sha256: 532003826286090ba8fd5be3dd6c2c13319c29c731c76a4785e02bfa77823522.
