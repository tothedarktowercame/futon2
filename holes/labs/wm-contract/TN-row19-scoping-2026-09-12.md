# TN: Row 19 scoping — R9 no-self-certification checker

Date: 2026-09-12  
Scope: design only; no production, census, registry, or accounting change.

## 1. Authority and verified pins

R9 is the assurance rule that checks which prevent the system from certifying
its own work must exist and refuse. At futon2
`1bb20921dac43447c739c2258eda095a90d451ab`, the recorded R9 account remains
`:verdict :hole`: two narrower implementations exist, but there is no node-wide
admission layer (`holes/labs/wm-contract/VERIFY-r-nodes.edn:1888-1908`). The
Row 17 prerequisite is
`runs/row-17-r9-trace-noncredit-2026-09-12/non-credit-record.edn`; it specifies
the Agency provenance inputs and the closed refusals Row 19 must implement.
Agency source was read at futon3c
`3682c693a7e379dc8d86cf90716d34da5c478544`.

The old R9 prose cites `full_loop_runner.clj:2598-2611` and `:1227-1271`.
Those line numbers have drifted. At current HEAD, the same narrow mechanisms
are the author/reviewer inequality refusal at
`src/futon2/aif/full_loop_runner.clj:3123-3135` and review-execution evidence
reconstructed and gated from the review job at `:1631-1675`. Row 19 should pin
declarations/content hashes, not carry the stale numbers forward.

## 2. Certification boundaries that need R9

### A. Witness admission

`scripts/witnesses/node_witness.clj:132-188` is the executable admission
boundary. A proposed witness is only pending (`:156-164`). An admitted witness
must have a successful verification receipt, matching subject, pinned artifact
and source, kind-specific checks, and a review receipt (`:165-185`). The review
receipt must say `:verdict :approved` and contain a nonblank `:reviewer`
(`:180-182`). It must point back to the same verification receipt (`:183-185`).
`scripts/merge_witnesses.bb:39-54` applies `validate!` to every fragment and
emits the derived registry; `:55-62` prevents an admitted identity from being
silently revised.

This is strong artifact and receipt validation, but it is not R9. Neither
`receipt!` nor `validate!` compares the reviewer's identity with the witness
producer. A reviewer string is sufficient. The current working practice is
that Codex seats author the source/fragment and `claude-15` independently reads
and approves it. For example,
`runs/row-15-depth-proof-2026-09-12/review-receipts.edn`, selector
`[:records "R13-depth-machinery-capture-20260912-v1"]`, names reviewer
`claude-15`; `checks/witness-fragments/machineDepth.edn:1-64` binds that review
to the admitted claim. The receipt itself does not name the Codex author job,
so independence cannot be derived from this file alone.

Agency does retain the needed raw identity evidence. Job creation records
`:job-id`, target `:agent-id`, `:caller`, `:request-digest`, timestamps,
execution state and pending delivery
(`futon3c/src/futon3c/transport/http.clj:1334-1359`). Finalization adds result,
summary, artifact reference, execution evidence and trace identity
(`:1571-1618`); delivery records outcome, surface, destination and time
(`:1682-1737`). Row 19 must join those real producer and reviewer jobs to the
review receipt instead of trusting its free string.

### B. Existing narrow full-loop enforcement

The full-loop runner already refuses when `author = reviewer` or the reviewer
is unavailable (`full_loop_runner.clj:3123-3135`). Separately,
`review-execution-evidence` corroborates declared execution with actual
`tool_use` job events, and `review-execution-gate` fails code review without
executed tool evidence (`:1631-1675`). These checks protect this one runner.
They do not guard witness admission, working-evidence credit, census edits, or
registry integration. Row 19 should reuse their semantics or extract a shared
pure predicate; it must not create a second, weaker definition of
independence.

### C. R-node verification account

`VERIFY-r-nodes.edn:1888-1908` is an adjudicated facts/accounting artifact. It
states that R9 runs no equation, records the two narrow implementations, calls
the node a hole, and reports zero R9 route records. Today changes to that
verdict are reviewed by the owner/reviewer workflow and repository history;
there is no checker at the file's write/admission boundary that proves the
author and reviewer differ. A simulation may validate its factual claims, but
that is not an identity-separation check on the agent who writes the verdict.

### D. Other verdict and credit surfaces

1. `ALIGN-rnode-process-census.md` records `exists`/`absent` cells and dated
   adjudications. Its own TRACE discussion documents a prior self-credit
   incident (`:175-185`). Editing and committing the Markdown is presently a
   human/agent review practice; no admission function checks producer against
   reviewer.
2. Row 17 `working-evidence.edn` artifacts are proposals. They deliberately do
   not change census cells. Their maximum-claim wording is reviewable, but no
   common schema currently requires a distinct reviewer before a later owner
   copies a credit into accounting.
3. Witness fragments become canonical `checks/witness-registry.edn` through
   `merge_witnesses.bb`; this is the only surveyed registry integration with a
   central executable validator. It verifies pins, proof execution and review
   content, not reviewer independence.
4. Direct edits to `aif-equations.edn`, `VERIFY-r-nodes.edn`,
   `ALIGN-rnode-process-census.md`, `WORK-REMAINING.md`, and generated reports
   are controlled by review/commit convention plus their domain-specific
   linters. No common R9 gate is invoked at their certification transition.

Thus “someone checked it” and “the admission boundary refuses self-review” are
different claims. Only the full-loop runner presently establishes the latter,
and only inside its own run.

## 3. Checker contract

### 3.1 Input and identity joins

The checker consumes the Row 17 input spec without default identities:

- declared role binding: `{:author A :reviewer R}`;
- producer job: `:job-id`, `:agent-id`, `:caller`, `:request-digest`,
  `:created-at`, `:started-at`, `:finished-at`, `:state`, `:terminal-code`,
  `:execution`, `:artifact-ref`, `:result-summary`, `:delivery`, `:trace-id`;
- reviewer job: the same fields;
- certification subject: artifact path/digest or commit, verification receipt,
  review receipt, and the boundary at which status/credit would change.

The minimum positive join is:

1. declared author equals the producer job's `:agent-id`;
2. declared reviewer equals the reviewer job's `:agent-id`;
3. author and reviewer are unequal canonical Agency seat identities;
4. the producer's `:artifact-ref` identifies the exact reviewed artifact or
   commit;
5. the reviewer job's request digest is recomputed from the retained review
   commission and equals the ledger digest;
6. the review receipt names the same verification receipt and reviewer;
7. both `:trace-id` values resolve through `:trace->job` to those job ids;
8. the reviewer job contains qualifying review execution and precedes the
   admission event.

Caller is provenance, not automatically author or reviewer: orchestration may
make `:caller` the owner while `:agent-id` is the worker. The checker must use a
declared role binding and verify it against job targets. It may not infer a role
from a familiar prefix such as `codex-` or `claude-`.

### 3.2 Refusals and result record

Every guarded boundary returns either an admission token or throws/returns a
typed `:r9/no-self-certification` refusal. Missing data fails closed. Required
causes are:

- `:r9/author-equals-reviewer`;
- `:r9/producer-identity-missing` or `:r9/reviewer-identity-missing`;
- `:r9/producer-job-missing` or `:r9/reviewer-job-missing`;
- `:r9/unjoinable-producer-reviewer-pair` for digest, artifact, receipt,
  trace-index, chronology, or role-binding disagreement;
- `:r9/review-execution-missing`;
- `:r9/artifact-binding-mismatch`;
- `:r9/anchor-missing` until the bootstrap authority is selected.

The successful result is not a boolean. It is a retained
`:wm/r9-independence-admission-v1` record containing the boundary, certification
subject digest, declared author/reviewer, both job ids and trace ids, exact
ledger source hash/revision, all join results, checker source hash, bootstrap
anchor, decision `:admitted`, and timestamp. A refusal retains the same public
identities and failed join names but no prompt/result bodies. This lets a later
audit reproduce why admission happened without exposing unrelated text.

### 3.3 Boundary adapters

| Boundary | Mechanical placement | Positive evidence | Negative controls |
|---|---|---|---|
| Witness admission | Before `node_witness/validate!` can accept `:status :admitted`, or inside a new admitted-only guard called by it. | A real Codex-produced witness and real claude-15 review job/receipt, joined to the exact artifact and verification receipt. | Same real pair with author=reviewer; producer identity deleted; reviewer identity deleted; wrong artifact ref; wrong request digest; trace id mapped to another job; review execution removed. |
| Full-loop discharge | Reuse the common checker at the existing `3123-3135` readiness gate and at final discharge, retaining current refusal compatibility. | A real completed author/reviewer cycle already captured by the runner. | Existing equality/unavailable controls plus swapped jobs, missing ledger record, and artifact mismatch. |
| Working-evidence credit | A proposed row remains a proposal; any adapter that changes it to accepted/credited must require an R9 admission record. | A real Row 17-style evidence packet and its actual owner review job. | Self-review, reviewer string without job, unjoinable source hash, and proposed row copied directly to credited status. |
| R-node census/verdict | A separate census integration command consumes a proposed edit plus its R9 admission record before writing or emitting a status-changing patch. | A real reviewed census change with producer/reviewer jobs and committed subject digest. | Direct status edit without token; token for another file/digest; author=reviewer; stale ledger pin. |
| Witness registry merge | The merge receives the witness admission token and verifies its subject digest is included in the merge input digest. | Disposable merge of a real admitted fragment, then retained production merge receipt. | Replayed token after fragment mutation; token for a different claim; missing token. |

Synthetic mutations are useful controls, but they are insufficient positives
where real review jobs and artifacts exist. Each adapter's acceptance packet
must contain at least one real production-boundary admission and at least one
real refusal attempt at that boundary. Tests may derive negative variants from
the retained real pair so the identity and join structure remain realistic.

## 4. Bootstrap: who certifies the checker?

The R9 checker is itself authored code. Making it exempt would be the precise
failure it is meant to prevent. Recursively asking the same checker to certify
its own first version also has no base case. One bootstrap authority must be
ruled and recorded before implementation admission:

1. **Human/operator anchor.** Joe records an operator-signed approval of an
   exact checker source hash, test receipt hashes, and author/reviewer job pair.
   Later checker versions are admitted by the previous anchored version plus a
   distinct-agent review. This has a clear recursion base, but depends on a
   human signature/identity format that must be specified.
2. **Cross-agent genesis constraint.** The first version requires unequal
   Agency `:agent-id`s for author and reviewer, immutable job-ledger pins, and a
   repository commit signed/accepted by an operator-owned branch rule. This
   reduces discretionary content checking but still ultimately trusts the
   operator's repository and Agency identity authorities.
3. **Threshold genesis.** Require an operator plus two distinct agent reviews
   over the same source hash. This gives stronger error resistance at greater
   operational cost; it still needs a human/operator root identity.

This TN does not select among them. Until the owner records that decision, the
checker must refuse its own admission with `:r9/anchor-missing`. “The author was
Codex and the reviewer was Claude” is not a bootstrap rule: model-family names
are not authenticated identities and can change independently of seats.

## 5. What Row 19 does not own

- Row 17's Agency lifecycle credit is complete and is not reopened.
- Row 18 owns discovery/build work for the R20 link.
- Census, `VERIFY-r-nodes.edn`, equation registry, witness registry, and
  generated-accounting edits remain separate reviewed integration packets.
- Row 19 does not make Agency job traces into WM TRACE records.
- Row 19 does not certify semantic correctness of a proof or review; it proves
  identity separation and evidence linkage at the admission boundary. Existing
  proof/typecheck/domain validators retain their jobs.

## 6. One-behaviour build packets

1. **Bootstrap ruling and anchor schema.** Owner selects one §4 option. Define
   the immutable anchor record and commission missing/tampered/wrong-subject
   refusals. Acceptance: one real operator-anchored genesis record over exact
   source/test hashes. No checker admission work proceeds without it.
2. **Pure identity/join checker.** Add one source module implementing the input
   schema, canonical identity comparison, ledger joins, chronology, and typed
   closed refusals. Acceptance: a retained real author/reviewer pair passes;
   equality, missing identities/jobs, swapped trace mappings, stale pins,
   request-digest mismatch and artifact mismatch all refuse. Emit the complete
   R9 decision record.
3. **Witness-admission guard.** Call the checker for admitted witnesses before
   `node_witness/validate!` returns `:verified-binding`; proposals remain
   pending without review. Acceptance: one existing real Codex/claude-15
   witness admission passes from real ledger records, and a self-review attempt
   against the same boundary refuses. Registry merge remains disposable in
   this packet.
4. **Full-loop commonization.** Replace only the narrow duplicated identity
   predicate with the shared checker and bind final discharge to its token,
   preserving current typed outcomes. Acceptance: one retained real full-loop
   author/reviewer cycle plus real boundary refusal; existing full-loop suite
   remains byte-compatible outside the new evidence record.
5. **Working-evidence credit adapter.** Define the one transition from
   `:proposed` to reviewed/creditable and require an R9 token for its exact
   digest. Acceptance: a real evidence packet and owner review pass; direct
   promotion, self-review, missing job and mutation after review refuse.
6. **Census/verdict adapter.** Produce, but do not directly apply, a census or
   `VERIFY-r-nodes` patch only after an R9 token binds the old hash, new hash,
   author and reviewer. Acceptance: one real reviewed change and real refused
   unreviewed edit. Canonical edit/application belongs to its integration
   owner.
7. **Registry integration.** Make `merge_witnesses.bb` bind the R9 token into
   its input digest and retain the integration receipt. Acceptance: a real
   admitted fragment merges; token replay after mutation and cross-claim token
   reuse refuse. Only after independent review may the canonical registry be
   regenerated.

Packets 2-7 depend on packet 1; packets 3-6 depend on packet 2. Packet 7 depends
on packet 3. Each packet cites `WORK-REMAINING.md` row 19 and retains real
positive and refusal executions at the boundary it changes.
