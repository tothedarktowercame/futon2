# E6b canonical replay closure v1

The machine-readable inventory is
`e6b-canonical-replay-closure-2026-09-13.edn`. It describes isolated evidence
available at HEAD and missing acquisition authority; it is not production
configuration.

`validate-transition-core` calls E3 and E2b directly
(`machine_slow_feedback_evidence.clj:97-116`). E3 resolves three exact records,
re-resolves E2a/E1, then runs R9 from the review's complete `:r9/input`
(`machine_pre_enact_authorization.clj:85-150`). E2b resolves three witnesses
and independently re-resolves the same E2a/E1 chain
(`machine_enactment_correspondence.clj:90-106`). E2a calls the five-file E1
resolver (`machine_portfolio_restriction.clj:110-116`), which reads and hashes
each configured buffer before mapping (`machine_budget_authority.clj:150-190`).

Therefore immutable restart replay needs all of:

- the externally owned E3 and E2b config identities, roots and exact pins;
- E3 pending, verdict and review bytes;
- the review's complete R9 input: distinct jobs/roles, exact commission
  preimage, review and verification receipts, trace-to-job join, checker pin,
  independently authenticated predecessor/bootstrap anchor, admission time and
  ledger source;
- E2b context, selection and enactment bytes;
- the shared five E1 authority buffers and exact resolver configuration; and
- pinned declarations for E3, E2b, E2a, E1 mapping/resolution and R9, with
  loaded/installed code identity recorded separately when one exists.

The retained E1 and E2b fixtures are byte-pinned in the inventory. E3's three
records and embedded R9 closure are constructed into temporary directories by
the test source (`machine_pre_enact_authorization_test.clj:22-79`) and have no
durable independently owned byte artifacts. The fixture's anchor is a literal
shape, not authenticated genesis. Consequently even isolated durable restart
replay is incomplete; production authority is unavailable.

An eventual resolver must select this closure by an independently configured
identity, read each file once, strictly decode/parse and hash the same buffer,
and reproduce the exact nested configs without consulting current defaults.
It refuses `:e6b/canonical-input-closure-unavailable` for any missing, changed,
extra, borrowed, cross-scope or unauthenticated component. Output digests do
not replace inputs; source declaration hashes do not prove installed code; and
no candidate, store, adapter or current checkout may synthesize an anchor,
review, commission, job/trace join or completeness acceptance.

The source dependency inventory also includes the R11 adapter and hierarchical
budget arbiter called by the E1 mapper. Neither source pins nor generated
isolated fixture bytes establish installed code or external authentication.
The refusal above is specified behavior, not a newly implemented resolver.
