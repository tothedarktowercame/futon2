# Serving retention deployment note review — codex-26

Reviewed69ff088c and compared the complete HTTP diff5fcf9912^..0d40a359. Current HTTP SHA matches defb1ed3b0deeb16ba15857f8efe5ac4e53ed6c442723dfae8808b38406f65fb. The eighteen named selected definitions cover this diff, including the two dynamic hook roots. Source availability still does not prove live identity.

Not executable yet. The note refers to a selective-form loader that does not exist. Also queue hold is explicitly documented in handle-agency-queue-hold as continuing to accept bells behind the hold; it does not establish the required admission freeze. An invocation may enter an old function body before waiting on the writer lock, then resume after Var replacement. Merely holding the lock while changing Vars does not exclude that case.

Next bounded preparation must implement and isolate-test the exact pinned-source loader with dependency checks, complete source-form identity receipt, rollback of existing roots and newly interned Vars/dynamic metadata, and induced partial-load failure. Establish an actual admission/quiescence boundary for all creation surfaces or report the exact missing control and retain refusal; do not equate queue holds with ingress rejection. Do not install a speculative live gate. Produce concrete commands and failure behavior for later independent review, not prose placeholders.

Post-load probe design must account for all archive join fields remaining immutable: delivery/execution metadata must be final before manually retaining a projection, otherwise later hot/archive agreement may legitimately refuse. Keep test/probe artifacts out of R9 admission. No live eval, reload, restart, holds, backup/ledger/archive mutations, or probe was executed. Notes only need no test rerun.
