# Provenance review: input/output closure mismatch

18a35e0e/5c29c9e4 seven current/historical pins verified. Raw retained gates show 21 tests/111 assertions, clean kondo/parens and deliberate failure. No passing suite rerun.

Additional isolated control (lead-control.clj, exit 0) changes E3 pending run to borrowed and updates its descriptor, E3 config pin and canonical config coherently. Original E3/E2b output descriptors remain unchanged. construct returns structural-artifact. Input closure hashes agree internally but are not bound to canonical output source manifests/subjects. The artifact retains incompatible pending and output facts.

Next bounded pure repair: exact canonical input/output identity, pending subject, context and source-manifest joins across E3 and E2b, including their E1/E2a and R9 correspondence. Refuse stale output when input bytes change even if config hashes change coherently. This is structural consistency, not executing canonical verifiers or granting authority. Retain no-authority labels and separate expected HEAD ownership. No storage publication or runtime work.
