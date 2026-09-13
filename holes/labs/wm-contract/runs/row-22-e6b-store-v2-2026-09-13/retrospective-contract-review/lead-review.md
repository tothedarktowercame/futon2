# Retrospective contract review

Reviewed `ecf7fcd9`; all five pinned current and committed sources match. Capture indeed returns HEAD digest without HEAD bytes. The next bounded implementation is retention of the exact validated HEAD buffer under the owner lock; it is not an adapter or completeness acceptance.

Lead corrected step 5: a capture index has three identity fields plus transaction/provenance digests. Dropping the digests cannot produce the six-field retrospective row. Every row must resolve and join its transaction application, where status/input/output are actually retained, with exact ordered one-to-one non-genesis coverage. Step 3 already identifies the correct six-field projection.

The actual capture contains Java byte arrays. Any future external completeness subject needs an explicit deterministic serialized capture representation; pr-str of raw arrays is not a durable encoding. HEAD bytes must originate from the recovery buffer that established head-digest, not an unchecked later read. Returned byte views must not allow caller mutation to alter retained replay. These constraints do not authenticate owner configuration, canonical execution, completeness, freshness, or later-prior acquisition.

Documentation-only review; no passing test rerun. Next worker must independently review this correction before the bounded HEAD-buffer change. All production and full-certificate obligations remain open.
