# Superseded-attempt dismissal: implementation receipt

Author: codex-1. Requested by claude-12, 2026-09-19.
Mechanism commit: `4f8c2f7f5d526c0029e9cc9994625ae154ec1cc7`.
Application is reserved for independent review. No live dismissal, resolution,
implementation record, or click was invoked.

## Target tracing (read-only)

Paths below are relative to the futon2 repository. IDs in the table are
short labels only; the citations identify the full immutable records.

| Finding | Own retained target proof | Target implementation | Distinct and later? |
|---|---|---|---|
| [259a attempt-001 revision-unchanged](../../../data/wm-repair-obligations/findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged.edn) | `:target` names 3f4cac attempt-003 artifact-binding-mismatch. No deeper selected-action target is retained in this finding. Opened `2026-09-14T23:53:20.021474557Z`. | [Implementation](../../../data/wm-repair-obligations/implementations/repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-003-artifact-binding-mismatch.edn): `:implementation-attempt "attempt-001"`, `:implemented-at "2026-09-15T00:10:09.232045124Z"`. | Later, but distinctness is **not established**: the short attempt ID aliases the suffix of this finding's full failed-attempt ID. Conservatively refuses `:self-implementation`. |
| [259a attempt-002 build-failed](../../../data/wm-repair-obligations/findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-002-build-failed.edn) | `:target`, `[:selected-entry :action :target]`, and `[:selected-entry :action :repair-obligation :repair/id]` agree on b0ee attempt-001 machine-repair-lacks-grounded-review-evidence. Opened `2026-09-15T00:24:58.735923806Z`. | [Implementation](../../../data/wm-repair-obligations/implementations/repair-ea1-b0eeafa0e59b4dc0dd9e0abe1cbbed0e687c5827b3ade8320c98c7f82a79032b--attempt-001-machine-repair-lacks-grounded-review-evidence.edn): `:implementation-attempt "ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-002"`, `:implemented-at "2026-09-15T01:53:42.308786029Z"`. | **Yes**, different full cohort/attempt ID and strictly later timestamp. No application performed. |
| [9b6 attempt-003 evidence-not-single-edn](../../../data/wm-repair-obligations/findings/repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003-evidence-not-single-edn.edn) | `:target` names initialization-a3e2319f initialization-failed; no deeper selected-action target is retained in this finding. Opened `2026-09-15T02:18:32.445288442Z`. | [Implementation](../../../data/wm-repair-obligations/implementations/repair-initialization-a3e2319f-9562-4182-b651-54f409b90e39-initialization-failed.edn): `:implementation-attempt "ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003"`, `:implemented-at "2026-09-15T02:27:53.785596225Z"`. | Later, but **exactly the same attempt**. Refuses `:self-implementation`. |

All three target implementation records report `:awaiting-validation`.
No resolution record for these exact three target IDs was found in the
read-only scan. This contradicts a blanket interpretation that all remaining
attempt findings have distinct later implementations.

Deep tracing matters: the 429 finding's embedded obligation also contains
ancestor `:target "repair-attempt-001"` and still deeper
`:target "M-f11-find-production-successor"`. Neither is the target selected
by this failed attempt. The resolver accepts only the current attempt's own
target carriers, requires agreement when multiple are retained, and requires
the target finding to exist. It does not walk arbitrarily to an implemented
ancestor. These results were obtained by parsing the retained EDN and walking
map/vector paths, not by matching text occurrences.

## Implemented contract

`dismiss-superseded-attempt!` accepts only `:authority`, `:reason`, and `:actor`
in the disposition. Finding ID, current open status, target resolution,
implementation identity, and timestamp evidence are checked before any write.
Evidence cannot be supplied through the disposition. The append-only
`dismissals/` record copies target ID, record ID/path, implementation attempt,
and timestamp; the original finding remains byte-identical.

Missing/conflicting targets refuse `:target-not-resolvable`. Absent or
non-implementation records refuse `:target-not-implemented`. Exact same IDs
and full/short suffix aliases refuse `:self-implementation`. Missing, invalid,
equal, or earlier timestamps refuse `:implementation-predates-attempt`.
Resolution-only proof must be an actual `:resolved` record with a distinct
`:validation-attempt`; a `:superseded` resolution is not implementation proof.
An implementation record takes precedence, so later validation cannot hide
a same-attempt implementation.

## Executed gates and warrant

```sh
clj-kondo --lint src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_obligation_test.clj
clojure -M:test -m cognitect.test-runner -n futon2.aif.repair-obligation-test
```

Static gates: zero lint errors/warnings; parens `OK`. Precommit namespace run:
26 tests, 182 assertions, zero failures/errors. This preliminary run satisfies
the repository prohibition on committing unrun tests; it is not the warrant.

Two registry preflight attempts refused before test execution: the copied old
config supplied the now-forbidden `:test-environment` key, then supplied an
explicit `-m` command rather than the registry's logical namespace command.
The corrected config uses the registry's canonical environment and runner.
No guard was altered. The registry requires committed source, hence the
precommit check followed by one registered execution of the committed scope.

From `/home/joe/code/futon3c`:

```sh
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/dismiss-superseded-attempt-2026-09-19/registry.edn
```

Warrant: `test-registry-96681db6c952216f7be4a32cd3204264d414ae4e7e6e4eec0ba87bdb7a403f44`.
Bound subject: `repair-store/dismiss-superseded-attempt`.
Registered result: 26 tests, 182 assertions, zero failures/errors, exit 0,
2398 ms. [Config](runs/dismiss-superseded-attempt-2026-09-19/registry.edn),
[execution log](runs/dismiss-superseded-attempt-2026-09-19/c8529f2e-9f48-46cc-a8b8-7ab98bd7951e.log),
[closure](runs/dismiss-superseded-attempt-2026-09-19/c8529f2e-9f48-46cc-a8b8-7ab98bd7951e.closure.edn).

Adversarial controls cover live targets, successful later implementations,
same-attempt and short aliases, earlier/equal/malformed dates, missing IDs,
conflicting/deep-only/missing target carriers, forged disposition fields,
resolution-only proof, immutable original bytes, and duplicate dismissal.
The T8 integration control observes one witness with three live findings,
then zero after the real verb dismisses all three in a temporary store.
Open-obligations and audit-history readback are tested as well. All mutation
tests use temporary roots; the production repair store was read only.
