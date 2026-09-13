# Common carrier contract review: composition not yet accepted

Reviewed 5ba1e504. Both cited implementation pins match current and historical bytes. No executable change or test rerun.

Carrier shape distinguishes prior top-level slow fields from successor :state. Later-prior acquisition is correctly kept external, not manufactured by relabeling. However the deterministic store proposal and stage 5 require further design:

1. Store proposal :application/:input/digests is the prospective seven-role map; unchanged verify-feedback expects exactly {:context, :prior, :e2b, :outcome} value digests. Role names differ (:prior-state versus :prior, :e2b-subject versus :e2b), and this equality is exact.
2. Store proposal output/digest is the carrier digest; unchanged verifier expects the digest of the complete next evidence record. These are different subjects. A postcommit adapter must explicitly derive, verify and bind both views rather than copy or relabel one digest.
3. Contract says the full evidence and expected HEAD are retained, but the strict store proposal/transaction schemas have no fields for them. In-memory retention does not establish restart-safe provenance. Specify a versioned durable binding and atomic publication/recovery relation before claiming capture includes original evidence. No unreviewed schema expansion is authorized by this review.

Additional limits: prior complete schema enforcement is a future requirement, not established by current selective core checks. Projection must specify fixed field order and nested value serialization or honestly retain exact carrier bytes; pr-str is not canonical. Proof of installed code ownership remains absent.

Next bounded design repair must name the exact mapping between committed storage and unchanged retrospective input, durable immutable proposal/evidence retention, and source acquisition of later priors. Preserve separate raw-source, record-value and carrier digests; no synthetic committed universe or completeness approval. No adapter implementation yet.
