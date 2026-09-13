# Store-v2 independent review — repair required

Reviewed final source `5e78192a` and receipts `c9b50fe1`. All five current and committed source/test/dependency pins match. Retained raw gates report 25 tests / 95 assertions, zero failures/errors, clean kondo/parens, and a deliberate failure. Passing gates were not rerun.

Executed `head-control.clj` in a fresh temporary isolated store. After a valid commit, changing only HEAD's revision and state digest to invented values still permits both recovery and capture at generation 1. The current transaction retains revision `slow-5` and its different digest. Raw output and exit 0 are retained beside this review. This reproduces a strict recovery invariant already repaired in v1; v2 is not accepted.

Further source findings requiring repair and controls:

- HEAD lacks exact schema and current transaction/state/revision joins. Genesis lacks complete schema/state/digest/generation validation.
- Recovery constructs its expected transaction using the transaction's own prior, without joining the provenance expected HEAD to the actual parent/store/generation.
- Publication checks application-ID retries and current HEAD but omits event conflicts and linear revision uniqueness; recovery only checks application/event duplicates. Restore pre-publication and recovery conflict rules without losing stable retries.
- The constructor claims existing non-v2 stores refuse, but delegates initialization and ownership to v1 and creates a provenance directory without a format discriminator. Define and enforce fresh-v2 versus legacy handling explicitly; no implicit migration.

Only the HEAD finding is an executed counterexample in this review; the others are source findings. Repair remains isolated temporary storage only. No production authority, completeness/freshness approval, later-prior acquisition, runtime adapter, or restart authorization is established. All earlier WM obligations remain open.
