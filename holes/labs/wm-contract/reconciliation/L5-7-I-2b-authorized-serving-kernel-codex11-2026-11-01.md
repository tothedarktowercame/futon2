# L5-7-I-2b: authorized preference into serving scoring

Base: 8482e826c. Branch: codex11/l5-7-i-2b.
Started from codex6's unreviewed de0230a7d, then corrected and tested load lifetime,
authorization binding, consumed-channel checks and fixtures.
No merge, serving reload, live click or agent contact.

## Wiring

- production-defaults owns one delayed registry load per invocation. Judge retries
  share the carrier or failure. Missing registry refuses before inner posterior
  selection as production-registries-unconfigured. The catch covers loading only.
- preference-registry/load! derives the terminal distribution from the validated
  calibration. terminal-distribution! purely checks that derivation; a missing or
  altered terminal cannot fall back to the legacy kernel.
- Runtime whitelist passes the carrier into family scoring. Class C is preterminal
  unit mass on ending/not-yet-evaluated, then the authorized terminal:
  focused 12/25, related 6/25, unrelated 4/25, stopped 3/25.
  Legacy pure callers without a factorized carrier retain their declared model.
  Sealed policy keys and class-F history are unchanged.
- Canonical rescore retains the carrier/schedule. Certification independently
  re-derives authorization, checks the schedule and recomputes G. Substituting
  the historical kernel refuses. compareAtHorizonN is claimed only when every
  candidate model actually has the theorem's ruled schedule; otherwise the
  label is scoreCascadeCoN-map.
- Before decision emission, every ranked candidate's consumed class C must match.
  computed-kernel-not-consumed is added to the gate's closed refusal set.
  Selected and stopped decisions retain the normalized carrier in their SC.
- Existing inner-abstention scoring receives the retained class model built with
  this authorized terminal; no separate registry IO or legacy terminal is used.

## Carrier distinction

On the default composed scorer, [:certificate :consumed-g :C] is progress C.
The consumed class C is at
[:class-emission :certificate :consumed-g :C :steps].
The guard checks this channel and its model horizon, without relabelling progress
C. Canonical scoring also carries class C at the top level.
The negative test corrupts consumed class C and observes the typed refusal at
the real cascade-decision boundary.

## Runner fixture and Lean

serving_preference_test.clj uses the runtime production judge composition, real
reading-family construction/scoring/decision, real runner and tick persistence.
Report/field assembly supplies a small pinned reading fixture. The worktree
source-drift guard alone is bypassed in that fixture. The runner reaches
cascade-enactment-open; no external work runs. Two judge calls load once.
The export obtains the certificate and both registries from the persisted tick.

Artifacts: /home/joe/notes/l5-7-i-2b/
- fixture-click-isolated/tick-run-record-serving-preference-fixture.edn
- fixture.json, fixture.lean, lean.log, export-fixture.clj, export.log
- test-comparison.json and individual test logs/metadata, kondo.log, parens.log.

Lean: (Requirement.q9, Outcome.holds).
All nine: Q1 violated, Q2 violated, Q3 holds, Q4 holds, Q5 holds, Q6 unverifiable,
Q7 holds, Q9 holds, Q11 unverifiable. This is a Q9 fixture witness, not whole-run
conformance or a serving reload witness.

## Tests versus base

| Run | Tests/assertions | Failures/errors |
|---|---:|---:|
| Main: outer/projector/shape/coapply/inner-law/registry/canonical | 225/1022 | 31/2 |
| Branch: same namespaces, final | 226/1027 | 31/3 |
| Main: runtime/registry/canonical | 37/180 | 13/0 |
| Branch: runtime/registry/canonical | 38/188 | 13/0 |
| Focused canonical and serving tests | 12/76 | 0/0 |
| Full runner fixture and negatives | 4/19 | 0/0 |

Extra broad-suite error: runner-persists-only-supplied-outer-selection, where
the source-drift guard compares the branch's changed registered scorer against
canonical source. Retained as a failed gate, not called baseline.
Other failure-name multiplicities match main. A transient reduction-label
assertion was fixed by requiring a model for every candidate; final coapply
rerun has only main's six existing failures.
Kondo zero errors/warnings; parens and diff checks pass.

## Isolation accounting

Hermetic test JVMs used scratch FUTON_WM overrides and the test data root.
Initial canonical data count: 45938, unchanged by initial tests.
Standalone export without the test alias exposed a missed on-prod-history-root:
one fixture checkpoint reached canonical data. An initial correction used the
wrong option, checkpoint-dir, and a rerun wrote a second fixture checkpoint.
Both individually identified files were moved to relocated-fixture-checkpoint*.edn
under notes; their empty fixture directory was removed. No pre-existing file
was removed. The fixture now sets the actual :on-prod-history-root explicitly.
This was an isolation breach, repaired, not zero historical writes.

The final broad test ran while one checkpoint existed: 45939 before/after.
Restored canonical count: 45938. Worktree-local evidence/ source snapshots from
existing fixture paths are not staged and are not canonical data writes.
The branch is not installed; the full-loop source-drift gate remains an
integration boundary.

Final standalone export used the explicit history root and left no fixture
checkpoint in canonical data. The subsequent observed count was 45939: the
new file is the independently running loop's wm-click-loop/click-002-1793564449.log,
not a fixture output; it was left untouched.

## Artifact SHA256 pins

- fixture.json: e914bb0e3dd2f33f49dc6bdc3301b7cbb464e7478ef597732a211909c72807e6
- fixture.lean: 4a2bf61fb63beef969d52cea9bf9ef0fdc37648a3cd9c6b27453db0b7a7b31ae
- lean.log: fa1d3ebb76fd04c496f5ed82699c3d2debf4a6221abb2471573275f42126d1d9
- test-comparison.json: 4bee6a1cc37f4cf882276271ce2d341939bbd5fb1662c7d2590b5e461eb810dc
- fixture-click-isolated/tick-run-record-serving-preference-fixture.edn: c266401165e12f63e46596384741e36d27bed9500ca21cce1ccf7fb9d4bf5d73
