# Independent review: ordinary selection precedence

Final verdict: **APPROVED** — see the final re-review below.

Initial verdict: **CHANGES-REQUESTED** (retained historical review).

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

## Final re-review after the authorized reconciliation

Verdict: **APPROVED**.

Reviewer: `codex-15`. Author: `codex-2`.
THIS approving review job: `invoke-1789847873274-22507-1d2e9d4d`.
Exact final implementation: `420faeb03f1ec12e7387cd6faf9a482cf2871f02`,
on prerequisite `8b6827da1fc093b2831d51f348de71a3fec6eccf`.
The earlier job remains CHANGES-REQUESTED; this new judgment does not
retroactively turn that job into an approving review.

### Consumed warrant

POST `/api/alpha/test-registry/check` against `/home/joe/code/futon2`
accepted
`test-registry-f352e8317f8dd879d2ab5364e09527a3384f344bc2750bcf307c1f12a36f58f7`
at `2026-09-19T19:58:11.741381977Z` with `:warrant? true`, matched postcheck,
and run HEAD equal to the exact final implementation above. Submitted review
paths were `src/futon2/aif/full_loop_runner.clj`,
`test/futon2/aif/selection_always_test.clj`, and
`holes/labs/wm-contract/runs/selection-always-2026-09-19/reconciled/register.edn`.
Only that registration declaration was outside closure.

```text
run/id: 77ec3714-0e3c-4037-947e-f7d0e1e73cf9
tests: 4; assertions: 31; failures: 0; errors: 0; exit: 0
duration-ms: 3875
```

The updated execution receipt records clean static gates and one corrected
namespace execution. I inspected the reconciliation diff and consumed its
warrant; I did not rerun tests.

### Adequacy and closure of the review findings

The reconciliation separates the observed queue from the selected repair
obligation: singular `stop-line` is now nil unless the selected action is
explicitly a repair action, and then is matched by selected target. Ordinary
execution therefore cannot obtain a recovery snapshot from an unrelated
open obligation. Its recovery reuse/supersession route and the observed
repair-contract fallback no longer activate from that queue observation.
The beyond-construction regression supplies an old running author job,
reaches fresh ordinary dispatch, asserts zero reads of that job, and retains
its obligation ID/count in the durable record. This directly covers the
previously untested veto boundary. The measured-acquisition fallback's
isolation is established by source inspection of its now-nil binding, not
by a separate measured-acquisition execution.

For explicit historical selection, candidate lookup and execution now use
the selected obligation. Before execution, the existing
`historical-revalidation-entry` predicate checks admission identity, open
machine-failure status and actor agreement/distinctness. Existing transition
checks remain. The warranted control selects B with A first in memory and
observes B at both candidate lookup and execution; a wrong-obligation
admission refuses before the execution port. This closes the second finding
without deleting historical machinery or relaxing its admission checks.

Against the ruling's three consequences:

1. Ordinary selection retains its transformation, posterior discrimination,
   controller decision, and observed obligation count/IDs. Repair memory no
   longer pre-empts the entry or invokes the identified downstream recovery
   veto/reuse route. Both empty and populated observations remain covered.
2. External repair/store verbs remain available and unchanged. Explicitly
   selected repair machinery is scoped to its own obligation. Separately
   authorized successor validation is preserved; the review does not confuse
   it with automatic repair-entry diversion.
3. The two authorized later production clicks and their existing budget are
   unchanged and unconsumed by this work. The implementation is adequate to
   proceed to that validation stage; the scoped warrant does not replace it.

### Final bounded witness attestation

`:resolved? true` — bounded to the reviewed precedence defect and the
currently warranted runner regressions: ordinary selection/execution is
isolated from unrelated recoverable memory, while explicit historical
execution binds its selected obligation and refuses mismatched admission.

`:dial-moved? true` — the warranted fixtures now retain ordinary selection
and its decision/evidence, reach fresh dispatch without reading the old
recovery job, and execute selected historical B rather than queue-first A.
These are concrete runner-boundary observations, not a claim of production
success or durable repair-obligation resolution.

Limits: controlled policy judgment, hermetic stores and execution ports;
ordinary execution stops at dispatch and historical execution uses a fixture
port. No live policy-generation, production successor, full legacy runner
suite, or production click is certified. Obsolete diversion tests have not
been claimed green. No suite rerun, repair-store mutation, production click,
code edit or serving-JVM reload was performed by this reviewer. Only this
review note was amended and committed. If this review is later used for a
schema-3 implementation record, obtain independently observed evidence for
THIS completed approving job and the artifact binding; the note alone does
not substitute for those records.
