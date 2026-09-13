# Strict store-v2 review — genesis repair required

Reviewed `6550f39c` / `19523ff3`: five current and committed pins match. Read retained raw 28-test/100-assertion success, clean kondo/parens, deliberate failure, and disclosed syntax failure. No passing suite was rerun. HEAD/current equality, explicit format, provenance-parent equality, and retry ordering are represented in source.

Executed two fresh-temp-store controls (exit 0, raw output retained):

1. Replaced genesis authority and committed-at with nil, recomputed its object SHA and HEAD pointer. Capture succeeds at generation zero. This bypasses no digest check; semantic schema validation is incomplete.
2. Initialized with a map containing an Object in authority. Initialization writes transaction and HEAD, then refuses invalid EDN during recovery. Invalid input must refuse before publication; poisoning after publication does not establish that rule.

Existing strict-genesis tests mutate object bytes without updating the digest-named reference, so those controls can refuse on digest mismatch before exercising semantic validation. Add self-consistently rehashed controls.

Source also omits genesis stored-generation validation when genesis has a child, complete authority/time validation, and genesis state/revision consistency. Require one shared complete genesis input/record validation at initialization and recovery, full-record strict EDN round-trip before publication, and an explicit isolated carrier/state contract. Retain source-reviewed HEAD/parent/conflict fixes and stable retries. No v1 changes, production construction, authority, completeness/freshness, later-prior acquisition, or runtime claims are accepted by this review.
