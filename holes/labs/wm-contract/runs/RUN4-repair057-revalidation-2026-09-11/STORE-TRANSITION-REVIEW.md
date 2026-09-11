# Historical admission review — changes required

Reviewed Futon2 178ad070 and Futon3c 32fb1f27. Actual producer-to-store test
passes 1/12. Independent disposable reproduction `store-read-repro.clj`
returns `{:status-with-invalid-verification :awaiting-validation}` for a
verification file containing only `{:repair/id "repair-057"}`. The read path
indexes any EDN map and changes lifecycle solely on its presence. This removes
an open obligation from stop-line selection. Strict validation must occur at
every consuming read; malformed, foreign or stale references must refuse.

Writer review: it validates a verification artifact's shape but does not read
the actual root/findings record or join its captured bytes to the verification's
finding digest. A stale caller obligation or different finding bytes with the
same repair ID can therefore be admitted. Use the canonical finding as source
of truth and bind the exact digest. Check output-path confinement, safe IDs,
no symlinks and crash-safe immutable publication using established controls.
Do not reuse the old unvalidated EDN reader for the new admission family.

Sequencing: record-historical-verification! immediately marks open-obligations
awaiting-validation, while historical-revalidation-entry requires an open
obligation. The positive construction test passes a stale pre-write map. An
actual selector consuming the store cannot select that action in this state.
Select a validated verification candidate while the obligation is still open;
only execution should commit awaiting-validation. Do not perform admission as
preparation just to make an action selectable.

Successor seam: resolve! still requires :repair/implementation for a machine
failure, but historical admission attaches only :repair/verification. Implement
a distinct supported historical branch retaining fresh, independent,
production-shaped successor evidence and distinct attempt identity. Never
reinterpret this as ordinary fresh code grounding or skip the successor.

All thirteen live source pins were independently rechecked unchanged. No live
record, service, capacity or attempt was changed. These are implementation
corrections, not an external prerequisite or a request for new permission.
