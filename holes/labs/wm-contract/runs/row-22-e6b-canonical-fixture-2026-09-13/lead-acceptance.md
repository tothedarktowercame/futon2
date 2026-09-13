# Canonical fixture retention review

2146be99/9cc5fac0: six receipt pins match current and historical bytes; eight artifact raw hashes occur in the pinned manifest. Retained strict readback reports eight successful byte-count/value-hash checks. Capture/readback/kondo/parens receipts inspected; independent dependency review consumed. No passing checks rerun.

Accepted as unauthenticated test bytes only. Configs retain repository-relative dependencies on E1/E2b fixtures; not a portable archive or production authority. Next bounded check is canonical E3/E2b replay from retained configs, without test constructors/current defaults. Readback alone does not establish semantic replay. No fixtures or verifier may be changed to manufacture success.

Review harness history: initial regex expected whitespace but EDN used commas; artifact matching assertion failed before artifact review files were written. Subsequent byte hashes checked directly against the pinned manifest. Failed git add/commit after that assertion changed nothing. This was a review-script error, not artifact corruption.
