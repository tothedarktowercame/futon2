# D staged token carry, phase 2a — codex-6

Owner: claude-12. Ruling: `1bc98c04`, staged 2a. Conditioning is **not wired**.
This delivery does not claim a posterior was consumed, enact any action,
reload the serving JVM, or change the entity-health belief.

## Implemented

The joint decision still reads `scoring-input-receipts/initial-belief` and
passes exactly its value to scoring. The selection certificate additionally
contains `:token-belief-stage` with schema `:wm/token-belief-stage-v1`:

- `:conditioning-status :not-wired`, `:observation-updates []`.
- `:occurrence-id` identifies the selection occurrence when provided; `:tau`
  is explicitly nil, since no observation step was consumed. The observation
  has `:status :missing`, reason `:conditioning-not-wired`, and matching
  occurrence/tau fields. No observed present/absent vector is manufactured.
- Initialization, continuation belief, domain declarations, token-set carrier
  cardinality and positive support size are recorded together.
- `:prospective-carry` holds the fresh token belief and its universe for the
  next tick. The tick reads that field from the previous persisted decision.
  `:prospective-prior` is retained but explicitly not admitted for consumption.
  The existing trace and run-record writers preserve the certificate intact.
- `:z-semantics :per-step-redraw` is a declaration, not a claim that inference ran.

The existing initial-origin and incoming-belief equalities remain mandatory.
The staged extension additionally checks initialization -> no update ->
consumed value, prospective state and observation identity; invented updates
and tampered continuation beliefs are rejected. This is not the active 2b
receipt or Q scorer-entry interface.

The finite adapter and manifest `exact-update` share
`exact-belief-core/condition-predicted`. Impossible received observations are
`:refused`, malformed rational inputs are `:invalid`. The manifest's
`token-belief-at-runtime-authority` records `:retired-from-runtime`, date and
successor. `token-belief-at` remains the mathematical Option-style trajectory;
its Lean correspondence tests remain. Runner continuation policy is not
inserted into this mathematical reference.

## Conservativity and adversarial checks

`baseline-outcomes.edn` was generated in a separate tooling JVM with the
pre-D `war_machine.clj` from HEAD `1bc98c04` ahead of the working source on
the classpath. Other dependencies were held constant, including concurrent
Q work. The baseline metadata/log are retained here; the fixture is committed under
`test/fixtures/d-token-carry/` (resources are ignored by this repository). The test compares exact
serialized bytes of action, chosen mass, beta, selection law and consumed D
verdicts against this baseline, both without prior carry and with a prior
that disagrees with the fresh facts. Additive certificate fields are not
counted as unchanged selection outcomes.

The admitted tick fixture has six target-qualified tokens: 64 possible token
sets, one positive state in current D. This is a measured fixture cardinality,
not a count of the current serving process's live domain. Every staged
certificate now records its own declaration-domain count without enumerating
the powerset. The orientation test uses all 64 states, the real cascade
transition with declared theta=1/2, and three positive incoming states. It
checks the hand-computed asymmetric prediction and rejects a transposed B.

Tests also use the real trace writer/reader and real run-record writer to
check persistence, retain entity-health belief unchanged, reject invented
conditioning and tampered prospective/consumed values, and exercise the
original `:initial-belief-value-mismatch` check.

## Execution roles

Claude-12 authorized pre-commit validation followed by a committed registry
run (bell `invoke-1789929845615-22712-480cede0`). AGENTS.md forbids committing
unrun tests; the registry correctly refuses uncommitted scope. Both roles
must be recorded; disagreement is a stop, not a choice of the green result.

`precommit-receipt.json` records commands, HEAD, source/test hashes, log hashes
and exits for the four targeted namespaces. All passed. A subsequent scoped
test addition exercises the full 64-state carrier; its separate validation
receipt records why only that namespace ran again. No whole-suite run.

Post-commit warrant IDs and comparison are recorded in a follow-up receipt
commit after registration. Registry configurations are included in this
directory, one per namespace. Lint and the workspace parens check also run
before landing. Independent owner review and the 2b checkpoint remain.

## Registered results

Implementation commit: `c300c67b`. All four committed registry runs passed:
**59 tests, 623 assertions, zero failures/errors**. `validation-comparison.json`
checks each namespace against its final pre-commit result; counts and success
agree. `postcommit-receipt.json` records commands, commit and log hashes.

- `futon2.aif.exact-belief-adapter-test`: 7/101; `test-registry-5d237a5d5c530e1d0f91dc0f3a7d5f115661a5c34d69904a03d5dc61e4018be5`.
- `futon2.aif.token-belief-carry-test`: 4/30; `test-registry-30193d624f56768e283d98a15bbf721750ea2fe82a3a58e95151f23229f6deb4`.
- `futon2.aif.scoring-input-receipts-test`: 3/45; `test-registry-f0b91d0467d113f1f65e3fec444187e8ba4a513b2c895ca5f2a68d95a88dded1`.
- `futon2.aif.cascade-model-manifest-test`: 45/447; `test-registry-095cb58eecec126df1f63c7d974fa59ceb2a23feb43b648b295da5116d1952e5`.

Pre-commit validation saw concurrent Q edits in the working manifest; those
edits were absent from the canonical checkout before registration. The warrants
therefore cover the committed D-only implementation and its actual load closure,
not uncommitted Q work. The outcomes still match the captured baseline.

## Recorded live carrier discovery

The newest persisted live run on disk at inspection was
`2026-09-20-019f6e43-5ea5-40b9-bfe6-1a0e1358d5f3` (started 16:17:55 UTC). Its five
candidates consume A over **7 target-qualified tokens**, hence **128 token-set
states**; each recorded D has **one positive state** and reads `:point-mass`.
`live-carrier.edn` retains the source path/hash, exact token universe and
per-candidate support counts. This is a read of existing evidence, not a new
live tick or serving-JVM measurement. The 64-state orientation fixture remains
a separate constructed test with the same token-set representation.
