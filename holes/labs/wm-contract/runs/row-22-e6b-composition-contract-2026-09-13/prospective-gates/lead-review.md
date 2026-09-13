# Prospective API accepted as cooperative isolated proposal evidence

Reviewed 19b07730/50ec2792. Seven current and historical source pins match. Raw retained gates show 10 tests/63 assertions with no failures, clean kondo/parens, and deliberate failure exit 1. No passing suite rerun.

Candidate interface accepts only a configured evidence-set id; exact seven source roles exclude ledger/universe/claimed-next. Shared core performs the retained checks and recomputes next state. Complete prior/next evidence records, raw source hashes, pr-str value hashes, canonical output hashes, fixed destination time and proposal-evidence-only label are retained. Existing public retrospective verifier code and its ledger requirement remain unchanged.

Trust limits: trusted-config is explicitly trusted caller configuration, not authenticated by this helper. validator-source-digest measures a classpath source resource, not proof of the executing Var's installed bytes. Dependency hashes are declarations matching the independently reviewed current source snapshot, not runtime dependency measurements. Acceptance is scoped to the reviewed isolated implementation, not tamper-resistant runtime code/configuration ownership. Do not promote these fields to deployment authority.

Next: source-pinned common store-state carrier design. Prior is an evidence record with top-level slow fields; computed next is a distinct evidence envelope containing :state. Define common state projection, metadata/revision joins, exact value digest convention, consecutive transition continuity and stable retries before any adapter implementation. Keep original evidence records and retrospective hashes intact. No store adapter, publication or production authority yet.
