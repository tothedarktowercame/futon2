# Independent review of the retrospective ledger correction

Reviewed lead commit `ba69fe7ad19eecf570c2f8cb1fa3ead85fdbc974` before the HEAD-buffer implementation.

Verdict: accepted as a documentation correction. The capture index entry
constructed by `machine_slow_feedback_store_v2.clj:264-268` has exactly five
fields: three identities plus transaction and provenance digests. Removing the
two digest fields cannot yield unchanged `verify-feedback`'s six-field ledger
entry (`machine_slow_feedback_evidence.clj:390-393`). The corrected step 5
therefore rightly requires ordered one-to-one transaction resolution and takes
status/input/output only from the joined transaction application. It neither
invents those fields nor grants completeness authority.

No test was rerun for this read-only review.

Pins:

- corrected contract: `5ea0a714b5484c9d85d91a525725d410a8cca43b8b080bdf4e5f9a5bbb4211e9`
- lead review: `1045b0a4d70a6d68f1591b7c03f2c38ef5fed63f197fa63242a9b8f0e28ff8b3`
- lead commit: `ba69fe7ad19eecf570c2f8cb1fa3ead85fdbc974`
