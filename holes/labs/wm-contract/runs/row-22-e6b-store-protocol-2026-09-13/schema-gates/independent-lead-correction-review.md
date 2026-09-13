# Independent review of revision-identity correction `79c957a2`

The recorded source/test SHA-256 values match the reviewed working bytes:

- store: `d9ee5f848419567e41f328a6b9fe14951f2568efcff4ee2dccbcac87a6bcd1fd`
- test: `10dd42bd1162ee2e1ddd75a131defd4d89a9ea799e52dc2101bec800cfe2175b`

The retained raw gate output reports 14 tests / 39 assertions with zero
failures or errors, clean clj-kondo, and clean explicit-path parens. Those
unchanged passing checks were not rerun.

The correction is complete for the isolated store's linear revision-identity
invariant. At commit time, immediate prior=destination remains rejected by the
proposal schema; the new check rejects a destination equal to any older
consumed prior revision. At recovery time the uniqueness set starts with the
HEAD destination and accumulates every child prior while walking to genesis.
Consequently every state revision in a valid chain is unique: HEAD destination
plus each earlier state as the next child's prior. The added forged-chain test
also exercises recovery rather than only the constructor. The check does not
claim rollback freshness, which still requires external expected-head
authority.

No regression is evident in idempotent retry: an existing application is
resolved and compared before the new-destination check. New monotone revisions
continue through the ordinary path. The unchanged lifetime lease, strict byte
validation, no-overwrite object publication, HEAD atomicity, and poison/recover
rules are unaffected.

## Next smallest composition boundary

The next packet should be a pure isolated adapter between
`machine-slow-feedback-evidence/verify-feedback` and
`machine-slow-feedback-store/compare-and-commit!`. It must consume the complete
verified E6b result plus its resolved source pins, compare that result's prior
revision/state digest and next record to the currently recovered HEAD, and
derive the store proposal's application id, event id, transition subject,
input/output digests, authority pins, and fixed commit time without accepting
caller booleans or alternate payloads. A store commit is permitted only when
the proposal is a deterministic projection of that exact verifier result.

Required controls are a borrowed verifier result, changed source pin, stale
HEAD, altered next state, application/event substitution, replay under the
same stable id, and a verifier refusal causing no filesystem publication. This
would establish isolated semantic-result-to-storage correspondence only; it
would not supply production outcome authority, external completeness,
rollback freshness, runtime wiring, or restart authorization.

