# Independent review: ordinary selection precedence

Verdict: **CHANGES-REQUESTED**

Reviewer: `codex-15`. Author: `codex-2`.
Review job: `invoke-1789847623191-22503-b9ce22fc`.
Reviewed implementation: `8b6827da1fc093b2831d51f348de71a3fec6eccf`.
Authority: `RULING-selection-precedence-2026-09-19.md`.
Execution receipt: `RECEIPT-selection-always-2026-09-19.md`.

## Warrant and limits

Consumed warrant
`test-registry-27e7b793e5c88d371700844297e6c3efb71787b26cf9d04b6231df86ad690f5e`
through POST `http://localhost:7070/api/alpha/test-registry/check` against
`/home/joe/code/futon2`. The initial request without review paths refused
`:review-diff-required`; supplying all three paths in the implementation
diff produced `:warrant? true` at `2026-09-19T19:54:13.657747227Z`.
Only the registration EDN is outside closure; production and test changes
are covered. Postcheck matched; run HEAD matches the reviewed full SHA.

```text
run/id: 81e4eac5-31ca-448d-a1fc-87048a15b8b9
tests: 2; assertions: 19; failures: 0; errors: 0; exit: 0
duration-ms: 3961
```

No tests were rerun. The receipt reports clean static gates. The warranted
tests establish selection transformation, ordinary entry construction,
posterior discrimination, controller-decision retention, and exact observed
obligation count/IDs in selection and durable record, with two obligations
and with none. They intentionally stop at construction; they cannot establish
that later execution treats obligations solely as evidence. The supplied
obligations also do not include an incomplete-recoverable job.

## Adequacy against the three operative consequences

1. **Selection and evidence without veto: incomplete.** The selection-site
   changes satisfy the immediate precedence and certificate-retention parts.
   However, `full_loop_runner.clj:4134` still calls `recovery-snapshot` whenever
   the first open `stop-line` exists, regardless of selected action. The helper
   at line 3214 reads a job for an `:incomplete-recoverable` obligation.
   A nonterminal job then throws `:recovery-job-not-complete`; a terminal
   failed job can supersede the obligation and throw `:recovery-job-terminal`.
   A completed author-wait job can instead populate `recovered-author-job`,
   causing the ordinary action's author-dispatch to reuse that old job
   (line 4229 onward). Thus repair memory can still veto or replace ordinary
   execution after an apparently successful selection. This is a concrete
   remaining route, established by source inspection, not a claimed executed
   reproduction. The construction-stop fixtures terminate before it.
2. **External repair as the standing mechanism: only partly satisfied.**
   Store verbs and external lanes are unchanged, but the ungated recovery
   route above still consumes repair work inside an ordinary click. Keep the
   observed queue separate from an explicitly selected repair obligation and
   scope recovery reads, reuse, supersession and repair-contract fallback to
   the latter. Preserve separately authorized successor validation; do not
   disable those checks to solve this issue.
3. **Two later production clicks from the existing budget: unchanged.**
   This diff does not consume or alter that authorization. The reviewer
   performed no clicks or serving-JVM reload. Passing this scoped warrant
   cannot certify the planned production validation; it remains future work
   after an adequate runner change lands.

## Explicit historical action preservation

The helper and execution branch still exist, and candidate probing is now
conditional on an explicitly selected historical action. However, the branch
is not bound to that action's obligation: `stop-line` remains the first open
non-environmental queue member (line 3770), and both candidate lookup (3835)
and execution (4088) use it. `historical-revalidation-entry` embeds the
selected obligation and admission in the action, but this path does not
derive its execution obligation from those fields or compare against the
selected target. If ordinary selection explicitly chooses historical B while
queue-first is A, the execution port receives A and its candidate, while the
selection/construction describe B. Existing transition checks compare against
A, so they do not establish selected-target agreement. Retaining the branch
alone is insufficient preservation under its new entry mechanism.

## Concrete reconciliation

- Separate the observed obligations from the obligation selected for repair
  execution. Ordinary selection must not read/reuse a recovery job or invoke
  recovery supersession merely because an unrelated obligation is open.
  Audit remaining singular `stop-line` uses, including the measured-acquisition
  discharge-contract fallback, for the same coupling.
- Bind explicit historical execution to the selected obligation/target and
  validate admission against that identity. Preserve existing transition and
  actor checks rather than weakening them.
- Extend hermetic coverage beyond construction: an ordinary action with an
  open recoverable job must reach its own fresh dispatch without consulting
  that job; an explicit historical B selected with A first in the queue must
  execute B or refuse a mismatched admission. Use controlled dispatch/ports
  and temporary stores, then supply the refreshed warrant for re-review.

Obsolete diversion tests are not claimed green here. Their acknowledged
status is not the reason for this verdict; the two concrete downstream
couplings are.

## Bounded witness attestation

`:resolved?`: **not attested** for the ruling's evidence-without-veto behavior.
`:dial-moved?`: **true only at the warranted selection/record boundary**:
open obligations no longer replace the selected entry or suppress the
transformation, discrimination or retained controller decision. That local
movement is insufficient for an implementation-discharge witness claiming
the whole behavior resolved. This CHANGES-REQUESTED job must not be bound
as an approving review in `record-implementation!`.

No code changes, suites, production clicks or repair-store mutations were
performed. Only this review note was written and committed.
