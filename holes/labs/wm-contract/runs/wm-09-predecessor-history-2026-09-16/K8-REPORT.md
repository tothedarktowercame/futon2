# WM-09 K8: explicit archived closed-history discovery

Author: codex-7. Consolidated successor to K7 implementation 615a9987, pending exact-subject independent review. Coordinator K8 packet/pins are retained in K8-SOURCE-PINS.json. The coordinator's 24-close census is a supplied bounded census, not rerun here.

Discovery now supports root/cohort/attempt and root/archives/archive-group/cohort/attempt. All canonical close paths are deduplicated before reading/order comparison; archive-group names supply no dates or authority. Archived closes enter unchanged K1–K7 relevance, ordering, identity, manifest and current admission checks. Search-layout provenance accompanies success/absence and predecessor refusal.

Attempt recognition uses the existing full_loop_cohort/attempt-dirs naming contract (attempt plus three digits). A recognized attempt without a close is not a predecessor; it is not an open-attempt status census. Unknown nested directories, misplaced close files, unreadable directories and unclassifiable entries refuse with a path. Symbolic links below supplied roots refuse rather than following a cycle or escaping the root. Supplied roots are canonicalized as before.

The existing full_loop_runner/checkpoint-evidence-manifest reads a flat attempt/evidence directory. Initial caller tests exposed this layout; their 10 failures are retained in k8-initial-caller-tests.log. Discovery now explicitly recognizes that producer-defined evidence directory, checks its entries, and does not treat it as an attempt or silently ignore a close/unknown subdirectory there. No external schema or runner change was needed.

Hermetic controls cover archived latest damaged matching history above older live valid history; valid archived history winning temporal order; duplicate supplied root/archive-group paths without false ambiguity; unrelated archived exclusion with path provenance; recognized open attempts with retained evidence; unsupported nesting; and cycle/escape links. Prior K7 targetless and annotated-close controls remain active.

Final gates and raw output are K8-GATES.json and k8-*.log. The predecessor namespace passes 23 tests / 279 assertions; focused caller tests pass 2 / 16. The retained classification control distinguishes typed history refusal from an untyped code fault. Lint and parentheses are checked. The negative run uses isolated source 615a9987 with the new controls, demonstrating pre-K8 false absence/order behavior.

## Operational limits

B2 remains: wm-outer-loop-43-v1/attempt-053's annotated close blocks all-root receipt-mode discovery pending separate justified disposition. Archives may expose additional unsupported predecessors or invalid bindings; this is not a recovery mechanism. Moving a record into an archive does not repair its recorded data-root, manifests or identity. Modern archived fixtures pass only when their bindings actually agree. No production records, roots, classifier, serving runtime, checklist or DAG changed. All nine original diagnostic files match 7e738d53. Source correctness is not operational readiness or accepted delivery.

Pre-K8 comparison: 23 tests / 279 assertions, 23 failures, 0 errors, exit 1. Final lint and parentheses rerun after the comment-only source attribution correction also pass.
