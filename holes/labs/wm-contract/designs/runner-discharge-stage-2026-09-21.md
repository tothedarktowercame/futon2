# Proposed runner discharge stage — discovery and design only

Owner: claude-12. Author: codex-8.
Request: invoke-1789965672611-22907-8e9f6e0e.
Authority: Joe's ruling 6402ed8a, RULING-queue-and-types-2026-09-21.md:7.
No implementation, live load, dispatch, or store mutation in this packet.

## Findings in current code

Paths below are relative to futon2; line numbers refer to this discovery.

- There already is partial in-loop discharge. full_loop_runner.clj:3677-3687
  recognizes repair action TYPES and compares the native repair id to the action
  target. It does not bind admitted T-<repair/id> cascade targets to findings.
  repair_proposals.clj:35-68 already supplies the untruncated T target, native
  :repair/id, exact finding-byte pin, and verbatim discharge contract. Proposals
  alone are not executable admissions and must remain so.
- full_loop_runner.clj:4318 computes review approval; :4437 starts grounding;
  :4457 resolves recoverable repairs; :4467 registers machine implementations
  with independent-review-evidence and artifact-binding. :4486 resolves an
  earlier validation line. These writes occur before adjudication/close, and
  exceptions enter the generic runner failure path. The earlier validation line
  is picked from memory using take 1 (:3638), not an explicit finding-specific
  successor proof. Reuse the evidence machinery, not that implicit association.
- repair_obligation.clj:1307 record-implementation! leaves a machine repair
  awaiting validation. Schema-3 evidence validation (:112) requires an executed,
  approving independent review and a corroborated fresh artifact binding.
  resolve! (:1406-1430) requires validation attempt != failed attempt AND, for
  machine/review failures, validation attempt != implementation attempt.
- successor-resolution! (:1478) already checks matching finding identities,
  distinct attempt AND run ids, temporal ordering, and grounded successor
  evidence before delegating to resolve!. Use it for machine-repair successors.
- ground-commit! (:2466-2519) witnesses a new reviewed implementation entity in
  the substrate. Its resolved?/dial-moved? values prove insertion/visibility,
  not that a particular finding's failure no longer occurs. It cannot alone
  supply the finding-specific repair witness.
- Store files are CREATE_NEW (:267), not an idempotent upsert. open-obligations
  (:909) overlays implementations and removes resolutions/dismissals. Thus a
  successful resolution disappears from supply BEFORE any later git publication.
- repair_proposals.clj:59 explicitly marks closure observation unavailable:
  an unversioned resolution has no admitted locator. C3 only tests path presence
  at a resolved revision (observation_checks.clj:42-50); it does not validate
  receipt content or prove a repair's success.
- close-core! (:3272), close! (:3528), and persist-run-record! (:502) are distinct
  boundaries. 007-closed is immutable, while the terminal run record is persisted
  after the core result returns. No runner-owned git receipt publisher exists in
  the examined runner. Publishing needs an explicit new bounded capability.

## Proposed integration and state transitions

Add a discharge coordinator behind the runner, with three explicit operations:
prepare discharge context, apply the eligible store transition, publish verified
receipt. It operates on the selected finding only. Route existing repair-action
store writes through the same coordinator; remove the arbitrary-memory validation
shortcut for this path. No duplicate legacy call may also write the same record.

Selection binds the admitted candidate's native repair id, full T target,
finding-source byte hash, and exact discharge contract to a fresh store read.
Require target = (str "T-" repair-id), a matching native id and finding pin, and
an admitted interpretation/candidate. A T-looking string or a proposed supply
record alone grants no authority. Legacy explicit repair actions can bind their
existing native-id carrier through the same verified lookup. Other targets are
:not-applicable, not guessed repairs. Pin disagreements are typed refusals.

Use the final IN-LOOP reviewer job and reviewer-of-record, after revision handling,
not a configured reviewer name or an approval substring. Compute
independent-review-evidence; preserve actual artifact binding, review-job byte
pin, committed repair artifact, and the finding-specific validation evidence.
No requirement is discharged by converting a substrate insertion into a repair
witness. An explicit requirement-to-evidence table validates every declared
requirement; unsupported requirements/unknown artifact shapes refuse. Initial
code-commit support does not fabricate support for data-deposit/spec-document
contracts. Class-specific store API predicates remain authoritative.

For a fresh machine/review-failure repair:

1. Attempt A lands its actual repair, receives an executed independent approving
   review, and establishes a finding-specific grounded repair witness. Register
   implementation via record-implementation!. Status: :awaiting-successor.
2. A separately recorded later production-shaped attempt B demonstrates the
   required behavior for THAT finding. It has its own actual authority-qualified
   attempt id, run id, review, and retained witness. Validate the relationship
   with successor-resolution! and let it call resolve! with all gates intact.
3. Only accepted resolution can produce the final committed discharge receipt.

Do NOT make A look distinct by inventing A/implementation and A/validation ids.
If a previously completed, genuinely distinct successor already exists, it can
be consumed after validating its provenance and ordering. Recoverable and
other classes use their own full contracts; do not force machine implementation
registration onto classes the registrar rejects. Missing evidence means a typed
pending/refused result, never a forged production-shaped flag.

The stage is entered from the runner's finish path after review/adjudication
has produced the necessary context. For successor-resolution!, finish the
immutable execution close first so B's close and witness can actually be pinned;
then run discharge finalization before persisting/returning the terminal run
certificate. 007 describes execution; a separate append-only discharge event
and terminal :repair/discharge section describe the store/publication outcome.
Do not rewrite 007 or the earlier selection certificate to pretend these facts
were known earlier. Keep execution outcome and repair completion separate:
:repair/discharged? is true only at :receipt-committed. A grounded implementation
with refused discharge is NOT reported as a discharged T task.

Proposed terminal statuses:
:not-applicable, :review-not-approved, :evidence-unavailable,
:awaiting-successor, :store-refused, :resolved-receipt-pending,
:publication-refused, :receipt-committed.
Each non-success names the repair id, stage, reason, blocking paths/evidence,
accepted side effects so far, and the original store exception message/data.
Catch store refusals inside this stage; return the typed result and finish the
run record. Do not route them into another attempted generic close. Failure to
persist ANY run record remains a genuine durability fault, not a claimed close.

## Stable committed receipt

Final path (created only after successful resolution):

    holes/labs/wm-contract/discharges/<full-repair-id>.edn

Use the existing safe store-key rule, preserve the full id, reject path escapes.
Pending/refusal records NEVER occupy this final success path. Proposed exact
required top-level keys for schema v1:

```clojure
{:schema :wm/repair-discharge-v1
 :repair/id "<full native id>"
 :target "T-<full native id>"
 :status :resolved
 :discharge-contract <verbatim finding contract>
 :artifact {:shape :code-commit :repo "<canonical repo>" :commit "<repair SHA>"}
 :implementation {:attempt/id "<actual A>" :run/id "<actual run A>"}
 :validation {:attempt/id "<actual B>" :run/id "<actual run B>"
              :production-shaped? true :successor-relation <validated relation>}
 :review {:reviewer "<actual reviewer>" :job-id "<in-loop job>"
          :job-sha256 "<retained job bytes>" :evidence <computed review evidence>}
 :artifact-binding <observed binding for the repair artifact>
 :witness {:ref <retained witness locator> :sha256 "<witness bytes>"}
 :store-records
 {:finding {:id "<id>" :path "findings/<id>.edn"
            :sha256 "<raw bytes>" :edn-text "<exact original UTF-8 bytes>"}
  :implementation {:id "<id>" :path "implementations/<id>.edn"
                   :sha256 "<raw bytes>" :edn-text "<exact original UTF-8 bytes>"}
  :resolution {:id "<id>" :path "resolutions/<id>.edn"
               :sha256 "<raw bytes>" :edn-text "<exact original UTF-8 bytes>"}}
 :producer {:run/id "<finalizing run>" :attempt/id "<finalizing attempt>"
            :runner-source-sha256 "<loaded source>"}}
```

This is the machine code-commit variant. Other supported classes need explicitly
validated schema variants (e.g. no implementation record for a class that never
registers one), not fictional records. Embed exact store bytes so the committed
receipt remains checkable after a runtime store move; hashes are over original
bytes, not a reprinted map. Retain review/witness evidence at content-addressed
committed paths alongside the receipt and use those relative paths as locators.

Do not put the receipt's OWN commit SHA or self-hash inside its bytes. The
publisher returns an external locator {:repo ... :sha <receipt-commit>
:path ... :sha256 <receipt-bytes>}; the terminal certificate stores that locator.
Read the file back with git show at that exact commit and verify bytes and all
embedded record hashes, ids, contract and relationship before declaring success.
Repair commit and receipt commit are different, explicitly named artifacts.

C3 locator: {:check :C3 :repo <receipt repo> :sha <receipt commit> :path <final path>}.
Receipt admission must FIRST validate schema/content/provenance at that revision.
Only then is C3 presence a sound guard for the produced witness's availability;
it is not itself a proof of contract satisfaction. Codex-6's supply follow-up
must use this admitted locator and validate each contract-to-want/observation
mapping. No linked T declaration becomes admitted just because a path was named.

## Publication authority and crash recovery

Recommendation for review: a runner-owned deterministic publisher may write and
commit ONLY the validated receipt and its content-addressed evidence files in
the canonical futon2 checkout. Use an isolated git index/tree, verify the exact
path allowlist and bytes, and compare-and-swap the expected branch tip under a
repository coordination lock. Never sweep another lane's staged/dirty files,
amend an implementation commit, commit code, reset, stash, or force-update HEAD.
A conflicting workspace/ref update is :publication-refused and resumable.
This new git-write capability requires approval with this design; it is not
claimed to exist already. An alternative is an in-loop agent publication job
with the identical byte/path verification; it still must complete automatically,
not leave a human to perform an external closing ritual.

There is no atomic transaction spanning store appends and git. Persist an
append-only discharge intent before the first store write, with immutable pins
and stable operation identity. On recovery, re-read store records and verify
semantic identity, review, artifact and run binding before reusing an already
accepted step. CREATE_NEW conflicts are never blindly ignored; contradictory
existing bytes are typed conflicts. Add a validated library readback/reconciliation
helper instead of hand-writing store EDN or bypassing the library's predicates.
Resume publication of an accepted resolution without re-running resolve! or
regenerating store timestamps. Final receipts are immutable; a different existing
receipt is a conflict, not an overwrite.

Resolution removes a finding from open-obligations. Therefore publication retry
must have its OWN explicit pending-discharge work source (retained intents),
consumed at runner startup/finish even when the finding is no longer in T supply.
Never reopen a resolved finding to force publication, and never call receipt
absence proof that the store resolution did not occur. Certificate reports
:resolved-receipt-pending until pinned readback succeeds.

Runner authority: register implementations; resolve eligible findings through
library APIs; produce reviewed-evidence receipts; bounded receipt publication
if approved. No dismissal API is exposed to this stage. WONTFIX, condition-cleared,
fixture/echo/superseded-attempt dismissals and other operator-authority dispositions
remain outside it. Existing failure finding creation is a separate runner duty.

## Acceptance and decisions requiring review

1. Real discharge: exercise the coordinator with real library/store and git
   operations, an approved in-loop review, actual artifact binding, and distinct
   A/B production-shaped attempts. Assert awaiting-validation after A, resolution
   after B, exact record byte hashes, committed receipt readback and actual C3
   observation at the returned SHA. A same-attempt self-successor must refuse.
   Critical store/relationship/observation functions are not mocked. Isolated
   integration fixtures are not reported as a live machine repair; a live
   acceptance run requires its own authorized attempt(s).
2. Review rejection: zero implementation/resolution/publication writes for the
   selected finding; terminal certificate has :review-not-approved. Verify real
   directory/HEAD snapshots, not just mocked function call counts.
3. Store refusal: induce the real validity failure, preserve its original reason,
   finish the durable terminal certificate, and create no success receipt. If
   implementation was already accepted, report that partial state accurately.
4. Crash cases: after implementation, after resolution before git commit, after
   git commit before terminal record. Resume with no duplicate store records,
   forged ids, lost publication work, or overwritten receipts. Include unrelated
   staged files and head-movement controls for publication, plus a forged receipt
   which passes C3 existence but MUST fail semantic admission.
5. Non-target/unadmitted T proposal, stale finding pin, unrelated successor,
   unsupported requirement, absent witness and attempted WONTFIX all refuse
   without selected-finding discharge writes. Existing non-repair closes remain
   outside the stage; targeted runner/store/observation gates cover integration.

Three decisions need explicit review:

- Preserve the store's two-attempt machine-repair contract. A fresh implementation
  tick may end :awaiting-successor; automatic discharge is a runner lifecycle,
  not an assertion that every repair completes within its implementation tick.
- Approve the narrow receipt git publisher (or choose the verified in-loop agent
  publisher). Without a post-resolution commit, C3 at the original repair SHA
  cannot observe a receipt containing a future resolution hash.
- The request's literal "review rejected => NO store writes" conflicts with the
  existing close-core! failure-finding creation (:3302) and recovery-review
  failure/supersession path (:4397). Recommended acceptance is NO discharge
  writes for the selected finding, while retaining normal failure evidence.
  Suppressing all failure-store writes would need a separate ruling; this design
  does not silently disable that existing accountability behavior.

STOPPED FOR DESIGN REVIEW. No implementation changes in this packet.
