# Repair 057 artifact contract review — 2026-09-11

Independent full-loop runner regression: 127 tests, 601 assertions, zero
failures/errors. Reviewed e620ff89 and actual downstream consumers.

The remaining issue is not another demonstrated selector defect. The existing
retry commits are candidates for admission with new review evidence. However,
`fresh-artifact-binding` requires a changed descendant HEAD in the author
window, `author-prompt` demands substantive newly authored work and rejects
report-only artifacts, and `reviewer-prompt` says the worker authored the
commit. `ground-commit!` also refuses an already grounded implementation.
`record-implementation!` alone does not establish these caller conditions.
Thus permission to reuse a commit at the store API is not proof that the
ordinary author path supports historical revalidation.

Do not introduce a token change, describe a historical commit as newly
authored, or turn off the fresh-author check. Proposed structural direction:
a separately typed historical-repair revalidation action, binding the actual
implementation/source ancestry and exact failure, fresh executed independent
review and a new evidence/discharge identity. Existing implementation identity
must remain immutable; new verification evidence must not pretend the code
entity was created again. Awaiting-validation still requires a separate
production-shaped successor; source drift, wrong repair association, absent
review, duplicate verification and unsupported evidence must refuse.

This is a proposal, not an implemented action or a relaxation of current
contracts. Prepare the new cohort/pins without activation while qualifying
this path. Repair reviewer must be distinct from the invoking coordinator.
No live repair record, capacity, attempt, service or credential was changed.
