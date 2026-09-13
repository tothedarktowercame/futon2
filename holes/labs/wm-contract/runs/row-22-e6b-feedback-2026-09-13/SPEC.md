# E6b isolated feedback verifier

The lead correction in `../row-22-e6-spec-review-2026-09-13/lead-review.md`
was independently checked against the implementation sources before work. It
correctly identifies two defaults that this boundary must reject before calling
the production function: missing `:fast/succeeded?` becomes failure at
`temporal_hierarchy.clj:216`, and a missing prior class becomes `fresh-entry` at
`:214-215`. It also correctly limits exactly-once evidence to verification of
an externally complete ledger, not storage enforcement.

Pinned computation dependencies:

* `src/futon2/aif/temporal_hierarchy.clj`, SHA-256
  `e3e532ae1b0b123730299bd7caa1105b074b7d27912c5508d29c395f21d34eef`:
  `advance-slow-state`, next-mode derivation and result shape.
* `src/futon2/aif/intrinsic_values.clj`, SHA-256
  `ea07fb662fed93e801e613a102f35f7baa3c3053fd636d478d14e504a1be758b`:
  transitive Beta update through `next-update-record`.

Seven source roles are required from one externally configured root with exact
SHA-256 pins: fixed transition context, complete prior state, exact authorized
E2b subject, independently witnessed terminal outcome, claimed next state,
application ledger and independently complete application universe. Each file
is read into one byte buffer, hashed and parsed as strict single-form UTF-8 EDN.
Production mode refuses unconditionally.

The verifier requires `t+1`, explicit prior and next revisions, a prior entry
for the exact outcome class, boolean success consistent with terminal status,
distinct outcome producer/reviewer identities and an E3-authorized exact E2b
occurrence/action. It invokes the actual pure update and compares the entire
next state. The external universe must name exactly the application expected
for this feedback event; the ledger must contain exactly one committed entry
whose input and output digests match. Replay verifies the same committed bytes
without applying another update.

This does not persist anything or prove that a live store prevents duplicates.
No actual independently owned outcome authority, complete application universe,
state revision store, ledger completeness authority, runtime consumer or
qualifying production transition is available.
