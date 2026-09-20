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
