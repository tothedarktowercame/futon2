# improve-4a: record-only institutional route and attestation account

Branch `fix/narrative-improve-4a`, base `025e5856`. No production store writes,
WM clicks, shared JVM evaluation, or preference/credit/admission changes.

Stable schema/key: `:wm/route-attestation-v1` under close judgment
`:route-attestation`; artifact reference `:route-attestation-ref` has status,
path, SHA-256. File is `<attempt>/retained/route-attestation.edn`, never an eighth
checkpoint or a new file in evidence/. The runner result includes both fields;
the run record retains the reference. The file joins the existing close evidence
manifest when cohort/action occurrence retention applies. Noncohort attempts use
`<run-record-dir>/<run-id>/<attempt-id>/retained/` and return the same fields.
`load-identity` registers and requires the new click-path namespace.

## Input and meaning

Pure `route-attestation/receipt` accepts `{:declarations ... :events ... :target ...
:token-comparison ...}`. Runner declarations come explicitly from option
`:route-attestation`, with keys `:institutions`, `:criteria`, `:bindings`,
`:iad-profile`. No declaration is inferred from selection, a prompt or the
outcome label. Default is `:none-declared`, with eleven typed profile gaps.

Each institution names `:id`, `:version`, `:situation {:condition {:id :version}
:status :holds/:not-holds}`, `:preference {:id :version :engaged? boolean}`,
and `:valence :must/:may/:may-not`. Activation is active, inert (situation not
applicable / preference-not-engaged), or missing (unknown applicability or
invalid declaration). No global rule or independent weight is created. An
explicit institution weight/score is rejected as a typed gap.

Each criterion names id/version, institution id/version, target, scope map,
kind (`:stamp`, `:increment`, `:mission-closure`), evidence-kind, optional timing
`:before-dispatch`, and a target-qualified `:want` for result criteria. Bindings
name criterion id/version, checkpoint and a path **inside that checkpoint's
judgment**. Evidence must match kind/criterion/target/scope (including revision),
contain present status and a digest, and bind the same want for result criteria.
Warrant evidence also requires a test-registry ID. Missing/ambiguous bindings,
wrong scope/kind/want, missing evidence, and unsupported declarations stay typed.
Pre-dispatch registration requires both registration and checkpoint timestamps
before dispatch, AND an earlier checkpoint sequence. Backdating a field alone
cannot turn a post-dispatch checkpoint into a pre-dispatch stamp.

This slice **matches supplied evidence, it does not verify a warrant**. Every
present attestation says `:verification :supplied-not-checked`; the receipt says
`:verification :supplied-checkpoint-evidence-only`. No registry calls, claims of
current freshness/adequacy, or new credit follow. Subsequent slices own those
checks. Source checkpoint, sequence, timestamp and exact path remain recorded.
MAY NOT matches describe recorded prohibited events, not fulfilled obligations;
they are not listed as attested increments. Inert/unknown bindings never produce
increments. Increment is separate from mission-closure even with matching scope.

IAD entries `:1A :1B :2A :2B :3 :4A :4B :5 :6 :7 :8` always appear. Each is an
artifact reference from a checkpoint or a typed gap, never a numeric profile
score. Profile artifacts retain path/digest/source and supplied-not-checked
qualification. Institutions operationalize named preferences; profile completeness
activates nothing and has no scalar value.

No improve-2c schema had landed on base main. Read-only inspection of codex-12's
working `kernel_example.clj:121` found `:attestation {:status :absent :want token
:reason :warrant-join-not-implemented}`. This slice uses that same absent slot
shape with the actual typed reason; a matched supplied warrant uses `:status
:present :want ... :warrant-id ... :verification :supplied-not-checked`. It does
not edit or consume codex-12's namespace. Owner can reconcile integration when
both branches land.

The closed narrative adds one short “Route and attestation” paragraph: MUST met/
missing, MAY, MAY NOT, supplied attested increments, inactive/unknown count; or
“none declared”. It explicitly says supplied evidence is not independently
verified. Invalid declaration input is unavailable, not none-declared.

## Checks and limitations

Before implementation, the real narrative regression on base main failed:

```
expected: (str/includes? (narrative/narrative-text (narrative/load-run root run))
                        "Route and attestation: none declared")
actual: false
```

Full actual output is in `red.log`: 22 tests / 112 assertions, 1 failure / 0 errors.

Fresh local checks (37 tests / 235 assertions total, all green):

- route-attestation-test: 8 / 43. Situational/inert/unknown valences; scope/target/
  want matching and absence; prompt/selected-pattern impostors; late registration;
  original reference updater remains observed false despite grounded-change;
  incremental versus whole-mission meaning; eleven artifacts/gaps; retained-file
  roundtrip, independent SHA-256 check, idempotency; canonical data/ file inventory
  unchanged before/after temp-directory retention.
- run-narrative-test: 23 / 115. Real renderer absent and supplied-account paragraphs.
- load-identity-test: 3 / 12; runner-load-identity-test: 3 / 65, including the new
  participating namespace's registration.
- clj-kondo: zero errors/warnings; check-parens OK on all seven changed files.

`route-attestation-runner-test` is written on the existing passing
`run-feature-card-attempt` fixture: grounded outcome, byte-identical serialized
controller decision with/without route declarations, retained file/hash/receipt,
and no inferred increment. Its attempted run is blocked by the unchanged
`:stale-runner-source` authority guard (1 test / 3 assertions, 0 failures / 1 error).
**Owner must run it on merged canonical main.** Therefore the full runner-level
decision parity is a written acceptance check, not a locally passed claim. Pure
receipt tests preserve the frozen reference values; all wiring runs after the
selection/build, and no decision input is modified. The test's established
hermetic fixtures redirect trace/run-record/repair/trip stores to temporary roots.

## Exact commands

From `/home/joe/code/futon2-narrative-improve-4a`, each namespace separately:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.route-attestation-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.run-narrative-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.load-identity-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.runner-load-identity-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.route-attestation-runner-test
clj-kondo --lint src/futon2/aif/route_attestation.clj src/futon2/aif/full_loop_runner.clj src/futon2/aif/load_identity.clj src/futon2/aif/run_narrative.clj test/futon2/aif/route_attestation_test.clj test/futon2/aif/route_attestation_runner_test.clj test/futon2/aif/run_narrative_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/route_attestation.clj src/futon2/aif/full_loop_runner.clj src/futon2/aif/load_identity.clj src/futon2/aif/run_narrative.clj test/futon2/aif/route_attestation_test.clj test/futon2/aif/route_attestation_runner_test.clj test/futon2/aif/run_narrative_test.clj
```

Environment: Linux, Java 21.0.11; Clojure CLI 1.12.5.1664 / Clojure 1.11.1.
Registry warrant covers the pure/temp-file tests, not the blocked runner test.

Registered warrant: `test-registry-2fc3d02f79eea2a3089b4d3ef784b2b89194d86514f8b236d1876512938298b2` on `6b42529386d8ef153cb2b6a53a33da3705e0be07`: `:warrant? true`, 8 tests / 43 assertions, zero failures/errors, exit 0. Initial registry check returned `:missing-entry`.

```sh
# Separate CLI in /home/joe/code/futon3c
clojure -M -m futon3c.test-registry check /home/joe/code/futon2-narrative-improve-4a/holes/labs/wm-contract/runs/narrative-improve-4a-2026-09-21/registry.edn
clojure -M -m futon3c.test-registry run /home/joe/code/futon2-narrative-improve-4a/holes/labs/wm-contract/runs/narrative-improve-4a-2026-09-21/registry.edn
```
