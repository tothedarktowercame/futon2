# The work remaining — the single execution authority

2026-09-12 (v2, reordered under RULINGS Item 5). One task per row,
plain language, build order. **A QUALIFYING RUN is a run with every
node defined, validated, and working, provably so, whose
certificate attests exactly that. Anything else is fake.** Runs
before Phase 5 completes are machinery tests only. No row waits on
an operator decision: former decision points are resolved inline
per Item 5. Update this file in place; git holds history.

**39 rows: 5 proof-machinery + 6 probability objects + 5 learning
and equations + 4 assurance nodes + 3 wiring + 5 certificate-and-run
+ 4 papers + 7 standing tracker rows.**

## Signature discipline (adopted by Joe 2026-09-12)

Per the apex patterns (progress-is-a-witnessed-state-change,
violation-signature-before-work) and the signature audit
(runs/signature-audit.edn, guarded by runs/check_signature_audit.bb):

- A row may start only when its breach detector (signature) exists;
  where the row's own work builds the detector, the packet must
  build and commission it (induced violation) before any positive
  claim.
- BLOCKED until their named detector exists: rows 13, 14, 15, 16,
  17, 18, 20, 26, 32, 33, 36, 37 (gaps per the audit). Rows 6-11
  are UNBLOCKED: their signature-builder (the scoping spec's
  acceptance leg (c) reference points + the MachineModelSpec
  contract refusals) is done and reviewed; each implementation
  packet carries its own witness detector with negative controls.
- Every ticket/dispatch cites its row number.
- Typed statuses only: :not-proven is legal; an unbacked
  :validated is a finding.
- One test run per packet: the author commits an execution receipt
  (command, exit code, counts, tree sha); review validates the
  receipt against the reviewed commit instead of re-running
  (Joe, 2026-09-12). Re-run only on receipt mismatch, diff
  contradiction, or harness changes.
- "Is work happening?" = which rows changed state with witnesses
  this week (inline DONE annotations + git history of this file).

## Phase 0 — the machinery that makes "provably so" possible (5)

1. DONE (1ca908d3, reviewed 2026-09-12). The witness carrier:
   fragments + merge_witnesses.bb admission with the TN's maximum
   claims and named refusals; deterministic merge (byte-identical
   twice), 23 controls green including no-output-on-refusal.
2. DONE (2026-09-12). Executed verification receipts produced by
   real checker runs (codex-23 f9be1c4b + mathlib4 1f09599c3f:
   exit 0, axiom census propext/choice/Quot.sound only, no
   sorryAx); independent review receipts by claude-15 (198ca443);
   all 24 claims :verified-binding through the carrier's full
   gate, deterministic; canonical registry integrated (1dd22548,
   --check green). Historical pin divergences remain recorded,
   never re-pinned.
3. DONE (14dc45cc, reviewed; live 2026-09-12). The click path
   records both output-validator verdicts (pass/fail + findings +
   input digests) inside the construction record it saves, on
   every status including refusal; digests recompute; reloaded
   into the serving JVM.
4. DONE (37f111c4, reloaded + live-probed 2026-09-12). The
   repaired end-of-run projection is in the running service.
5. DONE (discovery 354b500e, fix 70b45a02, reviewed + reloaded
   live 2026-09-12). Every terminal path produces a run record:
   stop-line repair (STOP_LINE route, typed incomplete kind,
   requested-not-enacted pin), initialization failure, cohort
   stopping rule, and a fail-closed throw replacing the old
   silent absence; pinned-production and historical record
   shapes unchanged.

## Phase 1 — the six probability objects the mathematics assumes
## and the machine does not have (6)

Each row means: implement it in the running system, state it in
Lean, and prove the implementation matches at reference points.
Scoping DONE (SPEC-fundamentals-build, 4d6670aa, reviewed): build
order is 7/8/10 parallel, then 9, then 6, then row 14; 11 a
separate branch. Wave-0 shared contract DONE (futon2 e9587553 +
mathlib4 57130bd7, reviewed 2026-09-12): MachineModelSpec v1 with
typed refusals resolves the four semantic bindings (single-entity
state, derived outcome authority with evidence vertex owed,
declared-vs-measured kernel authority, finite registered parameter
hypotheses; continuous Dirichlet a named exclusion). Rows 6-11
implementation packets are ready to dispatch and HELD pending
operator workflow conversation (standdown 2026-09-12).

6. DONE (2026-09-12, second review after circularity repair). The
   original leg (c) was circular (:lean-reference := production
   row; caught by the batch-A audit TN 7e19ef83, missed in the
   first review — reviews now check reference independence).
   Repair (codex-24 b3ca7b0b..fe59d5ab, reviewed): fully
   independent exact-IEEE-rational reference pipeline; 36
   coordinates over three policies, max delta exactly 2^-55
   (~2.78e-17), production rows admitted :float-carried, exact
   sums recorded as-is. DEEP FINDING: the pinned belief row itself
   sums to 1 + 2^-55 exactly, so the derived kernel inherits it —
   an exactly-normalized PredictiveOutcomeKernel is
   unconstructible at production pins without unauthorized
   renormalization. RULED (reviewer, per contract v1.1's purpose):
   FloatCarriedRow.nearNormalised is the Lean target for
   production-derived rows; exact normalization belongs to a
   reviewed mathematical model instance (row 14's A/model
   content). Lean module, bridge theorem, refusals, sensitivity
   control stand. Failed attempt history retained (c86eed2bba).
   Row-14 placeholder-A note unchanged.
7. DONE (futon2 72950ab5..e6d52a2a, mathlib4 4750f9fa; reviewed
   2026-09-12). Leg (c) surfaced a real contract conflict —
   production float rows sum one ulp off 1 — settled as contract
   v1.1 (futon2 60d298df, mathlib4 2f46171d: exact IEEE summation,
   |sum-1| <= 1e-12, typed :float-carried, no renormalization).
   Both real rows admit with retained sums recorded; packet-9
   :belief-input produced; five negative controls refuse.
8. DONE as source module + proofs + witness (futon2 5f6b40da +
   41d61a40; mathlib4 e1b75181..c1c71011 + 34180899 rejection
   cases; reviewed via receipts 2026-09-12). Declared-prior
   authority only, honestly recorded (:empirical-claim false; no
   measured action law exists). Live-filter wiring belongs to the
   integration owner after rows 7/9/10.
9. DONE (futon2 71543de9/7bb1745f, mathlib4 4a65380f..6ce906a4;
   reviewed 2026-09-12). Full-plan iteration composing rows 7+8;
   induction proofs for terminal + every retained step; horizon
   control verified by independent arithmetic (same-first-action
   plans diverge at depth 2). Open obligation assigned to row 6:
   bridge theorem tying predictedStateStep to the generic
   machinePredictedStateKernel (MachineQ.lean).
10. DONE as source module + proofs + witness (futon2
    b071b528/ebbbafde/a18e3f66; mathlib4 b9712a17..03bba0e7;
    reviewed 2026-09-12). Twelve tagged masses exact vs Lean, sum
    1, seven named zeros; support derived from the contract's
    outcome authority; evidence consumption refuses; Q-positive/
    C-zero typed :risk :infinite, no epsilon. Reviewer re-ran the
    clj gate once (receipt had bound to the pre-commit tree); 3/32
    green at HEAD. Row-14 consumer integration is a later row.
11. DONE (futon2 33791015..abb658a3, mathlib4 3b4fbfef..d8185c01;
    reviewed 2026-09-12). Two real registered hypotheses
    (identity-B vs controlled-B through the row-6 A), both passing
    contract validation, placeholder-A caveat recorded in the
    registrations with the row-6 pointer. Bayes posterior moves on
    informative outcomes ({1,0}/{0,1}), preserved on
    noninformative; posterior-predictive equals likelihood
    marginal; zero-evidence, support-mismatch, and mutated-
    registration controls refuse. PHASE 1 COMPLETE with this row.

## Phase 2 — learning arrows and the remaining equations (5)

12. DONE (futon2 01a5e8a4 + witness 57c471fe..271d6095, mathlib4
    cbf0959b; reviewed 2026-09-12). Eq.21 recurrence proven
    (RealisesDeclaredAccumulation discharged; anti-recount theorem
    non-vacuous); three-real-tick replay in recorded order, 294
    coordinates, max IEEE delta 0.0; recount imposter fails,
    dropped tick and support mutation refuse. The author
    self-caught an initially unsupported witness claim before
    execution — retained in f35ae290's history. SCOPE
    CLARIFICATION (2026-09-12, from the R17 witness-generation
    stop): the row-12 correspondence is AT IEEE SEMANTICS (its
    reference replicates the declared rule in double arithmetic);
    the exact-rational recurrence differs by rounding residuals
    (~2^-59 scale), carried honestly under contract v1.2's
    declared residual criterion by the R17 claim packet.
13. DONE (2026-09-12; TN 817957a8; seam 6e096a2a; completion
    480ebd17..404a152d; reviewed + reloaded live). Live tick
    accumulation at the war_machine.clj:6280 anchor; carry in the
    daily trace; one committed configuration record threaded
    through BOTH production callers (on-demand run_tick_once +
    scheduled wm_scheduled_run) with typed refusal on
    malformation; full-judge three-tick machinery evidence
    (row13-full-1..3, 294 coordinates delta 0.0,
    :full-judge-redirected-run true); three refusals before
    append. MIGRATION DISCHARGED (2026-09-12, by the reviewer per
    the runbook; receipt at runs/row-13-live-accumulation-
    2026-09-12/migration-receipt.edn): predecessor record carries
    :accumulation-state (98 coordinates at declared prior 1.0,
    :last-tick bound to its own identity) via byte-surgery with
    every other byte verified unchanged; byte-for-byte backup
    retained beside the daily trace; no writer was running. The
    next live traced tick performs a normal recurrence step — its
    :accumulation-update-input is row 15's first real captured
    belief update; verify that step on the next trace write. The
    shared :accumulation-update-input envelope is now retained
    per tick: row 15's belief-update capture prerequisite is
    SATISFIED at the seam (proof packet can follow after the
    first migrated live tick supplies a real captured update).
    Registry flip R1->R17/R2->R17 remains TN-9a-gated, not
    performed.
14. Build the machine-Q link: the forward model actually receiving
    a policy-conditioned outcome distribution. RULED: build, not
    descope — narrowing the claim instead is the facade option.
    DISCOVERY DONE (2026-09-12, TN-row14-discovery-2026-09-12.md,
    9f671e03/2a529c57; every file:line pin re-verified at HEAD by
    the reviewer). Findings: the live scorer consumes continuous
    means/variances and its disposition lane is a different object
    from the machine Q; machine-predictive's twelve-wide Q reaches
    nothing but its own tests. SEAM RULED: efe/compute-efe's
    option boundary, per-candidate, with the full Q/C pair + pins
    retained per ranked action (NOT the once-per-tick row-13
    accumulation envelope). Build split adopted (TN §4, six
    packets): (1) reviewed A declaration artifact, (2) model
    assembly, (3) scoring adapter, (4) compute-efe injection,
    (5) trace retention, (6) row-11 registration refresh.
    Packet 3 DONE (2026-09-12, futon2 804b37fe/e14caa8d,
    reviewed): machine_q_risk.clj standalone adapter — ordered
    twelve-support equality, model/revision identity, pins,
    row-sum-admission routing (:exact/:float-carried, no local
    tolerance, no renormalization), D_KL[Q||C] with
    :infinite-risk refusal carrying the offending outcomes on the
    seven-zero-C shape; reviewer recomputed the 1/2*ln(4/3)
    reference bit-identical; 3 tests/11 assertions at tree
    804b37fe. Packet 4 DONE (2026-09-12, futon2
    0536763d/42a0d8a5/836f0831, reviewed): opt-in :machine-q on
    compute-efe — per-candidate provider, adapter risk folded
    weighted into g-risk, full Q/C pair + adapter result retained
    under :machine-q, refusals propagate uncaught; option-absent
    path proven byte-identical (reviewer recomputed the baseline
    digest e00e160d at the pre-change tree in a worktree —
    matches the committed pin); enabled test drives the REAL
    predictive-outcome-kernel and the C-flip reverses ranking;
    38 tests/147 assertions. Packet 5 DONE (2026-09-12, futon2
    65851e31/ff887374/e23bd7cb/0b90518e, reviewed): complete
    per-ranked-action :machine-q pair retained through
    strip-ranked-action; :machine-q-missing-q/-c,
    :machine-q-incomplete, :support-mismatch refuse in
    strip-ranked-action BEFORE any append (tests assert the trace
    file does not exist after refusal); write/read exact = on
    both full pairs; trace schema bumped to 28 with ledger note;
    receipts commit-first with honest prior-attempt trail.
    ROW-14 BUILD STATE: packets 3/4/5 done — the machine-Q link
    exists end to end (producer-callable scorer option + adapter
    + retention), opt-in and not yet live-wired. Remaining:
    packet 1 (A-content, awaits Joe's decision above), packets
    2/6 behind it, live wiring in the judge behind
    retention+evidence per the TN. Serving JVM reloaded from
    master (2026-09-12 post-packet-5: machine-q-risk, efe, trace
    schema 28, war-machine; bottom-up after a stale
    ruled-outcome-c dep surfaced) — next live tick performs the
    migrated accumulation step and persists depth fields.
    :blocker DECISION NEEDED (packet 1 only — model content, not
    principle-settleable): no inspected authority declares a
    semantic status-to-disposition correspondence, and sorting two
    enumerations is not one. Options: (a) Joe declares the 7x12
    correspondences (rationale per nonzero cell, named zeros);
    (b) measured A from pinned production observation/state
    records under the contract's :observed-estimate authority
    (machine_model.clj:71-99) — honest but sparse at current
    retention; (c) explicit noninformative declared A (uniform
    rows) with the content caveat retained, discharging the
    placeholder only structurally. The reviewer builds packets
    3/4/5 meanwhile; nothing in them commits to an A choice.
15. Write measurement proofs for the five machine declarations that
    lack one: belief state, belief update, depth, temperature,
    action. SPLIT 2026-09-12 (codex-24 discovery, correct refusal):
    belief STATE is provable now (row-7 pinned trace rows suffice;
    packet out). Belief UPDATE is not reconstructible at identical
    pins — the trace retains mu-pre/mu-post + aggregates but NOT
    per-entity events, resolved A/B bytes, or a model revision pin
    (inputs to machineBeliefUpdate, MachineBeliefUpdate.lean:181).
    PREREQUISITE (rows-3/5 class, record retention): extend the
    update path to retain {pre-row, per-entity attributed events,
    A/B bytes or hashes + revision, mode, post-row} per tick —
    additive record fields, no behavior change — then one
    machinery-test tick produces the real capture and the proof
    follows on the row-2 pattern. The existing
    F8-belief-update readback is synthetic-reference only and must
    not be relabelled as a trace proof.
    BELIEF STATE DONE (2026-09-12): two production-trace-scoped
    claims admitted (c6835840/e4fe14d6). TEMPERATURE DONE
    (2026-09-12): R14 trace claim admitted (codex-22 da91d1f5..
    8867439b + mathlib4 0c738057/1023b2f4; admission 1a93c20f;
    registry 51 entries b15b15f2) — tau-spread and tau reproduced
    from trace record 0, deltas 0.0. DEPTH CAPTURE DONE
    (2026-09-12, futon2 3b581fbd..0430b558, reviewed): judgement +
    trace retain :horizon-steps (the exact EFE input — nil is an
    observed value selecting the single-step path) and
    :policy-depth-used (derived with EFE's own gate condition),
    beside the configured request; present-only propagation,
    historical records unchanged; redirected machinery capture
    retained (requested 3, :anticipation-events-unavailable,
    effective 1), no live trace or JVM touched; self-caught
    golden-failure repair to present-only in e2052a93. DEPTH
    PROOF DONE at machinery-capture scope (2026-09-12):
    R13-depth-machinery-capture-20260912-v1 admitted at
    :verified-binding (futon2 0a9bdc9d/65f60618; mathlib4
    MachineDepthCaptureWitness.lean at c25e7306, both theorems
    axiom-free) — the pinned capture's effective depth 1 equals
    the declared machineDepth law at EFE input nil; scope denies
    production; the production-scoped claim follows the first
    post-reload live record carrying the fields. RELOAD PLAN: reload trace +
    war_machine namespaces on the serving JVM from master ONCE
    after row-14 packet 5 lands (single reload covers depth
    capture + machine-Q retention; next live tick then also
    performs the migrated accumulation step). REVIEW FINDING
    (queued, one line, held to avoid a concurrent edit on
    trace_test.clj while packet 5 is in flight):
    support-typed-scoring-shadow-is-non-authoritative-test
    asserts the default-off decision shape without binding
    *persist-policy-trace-details?* false, so it fails in any
    environment with FUTON_WM_TRACE_POLICY_DETAILS=1 (this
    machine, since V7 slice-10 5febaeee added the flag) —
    verified at pre-packet base e14caa8d, not a depth-capture
    or packet defect; fix on the packet-5 bellback. ACTION GAP
    ANALYSIS DONE (2026-09-12, TN-row15-action-gap-2026-09-12.md
    f3070db6; reviewer re-verified the strip pins at HEAD and the
    live-record claims from the pinned record): schema-28 policy
    details retain nearly all machineAction inputs; the rank/N
    join to retained action maps is bijective in the real record
    (148/148, verified), so the action-keyed posterior
    reconstructs exactly. Gaps: (a) join completeness unenforced
    (stringable-softmax-weights uses keep — silent drop
    possible); (b) abstain epsilon not captured. SEMANTIC
    FINDING (verified from the pinned record's :selection-law):
    requested/applied :controller-head has internal rank 1 but
    the live reason-bearing selector chose rank 139
    (:moved-from-controller-head? true, :consulted-ranking
    :live-selector-id) — the live decision is NOT an instance of
    Lean machineAction's controller-head branch. RULED in-lane
    per the no-facade principle (row 14 "build, not descope"):
    the divergence is retained as a typed measurement claim from
    existing pins (:diverged-from-machineAction, both ranks),
    and the positive live-action path requires a reviewed Lean
    extension of machineAction to the reason-bearing selector
    boundary — narrowing to the internal decision is the facade
    option and is not taken. Packet sequence: (1) R6 rank-join
    boundary DONE (see row 16 batch B); (2) ABSTAIN-EPSILON
    CAPTURE DONE (2026-09-12, futon2 46774fde..297a6559,
    reviewed): select-action assocs the RESOLVED
    :abstain-epsilon onto every decision path (default and
    explicit, abstain and chosen) — the exact value the
    actuation comparison used; trace needed no change
    (strip-decision passes it; redirected readback confirmed at
    0.125); packet tests clean, ambient broad-run failures
    attributed to concurrent lanes (reviewer re-ran
    trace/policy/selection/machine-q suites ambient — all
    green). machineAction INTERNAL-BRANCHES PROOF now unblocked
    (epsilon captured + schema-28 retention) — queued behind
    mathlib4 contention with the R6 packet-C build; (3)
    divergence witness from existing pins — queued, same
    contention; (4) machineAction live-selector Lean extension
    (spec first).
16. IN PROGRESS. Inventory done and reviewed (TN 6e6b2f5c): 18
    equation-bearing declarations — 4 admitted (26 claims), 4
    under row 15, 5 provable now from retained pins, 5 needing
    additive input capture (R8 free-energy, R8 policy-F, R5
    ambiguity, R6 policy-set, R17 model-reduction — capture
    candidates for the row-13 trace envelope seam). Scope: 10
    proofs. BATCH A SPLIT (TN 7e19ef83, audit supersedes the
    inventory's provable-now verdicts for these three): R4 DONE
    (2026-09-12): R4-forward-model-float-carried-production-pins-v1
    admitted at :verified-binding (futon2 09e47497/ebc519b0;
    mathlib4 MachineForwardModelWitness.lean at 9de3305be6) —
    three FloatCarriedRow instances from the repaired readback's
    independent exact references, nearNormalised (excess exactly
    2^-55), composition proved against exact positional-A; scope
    :production-pins-float-carried-v1.1 defers exact kernel
    normalization to row 14; R5
    risk needs a complete retained Q/C pair from the row-14
    consumer plus a reviewed production categorical-risk function
    (unsupported-risk classifies one boundary only; the
    strict-positivity premise is unprovable on seven-zero C); R5
    expected-free-energy needs full scoring-input capture or a
    reviewed production EFE composer (the sum is inline in the
    scorer; no production function accepts the retained scalars).
    BATCH B SPLIT TOO (codex-22 audit, verified by reviewer runs):
    R6 softmax -> PROVABLE FROM RETAINED PINS (2026-09-12):
    superseded by V7 slice-10 details-on retention (rank-keyed
    posterior, ambient flag on in production) + the action-gap
    TN's verified 148/148 bijective rank join + the R6 boundary
    packet (futon2 abc62ef5/9cb5e33d/1f87a6b6/1a20f1aa,
    reviewed): stringable-softmax-weights now refuses
    :softmax-rank-join-incomplete (missing/extra/duplicate, with
    offenders in the data) before append; passing path
    byte-identical; no schema bump (admission tightened, shape
    unchanged — ruling recorded in the docstring); test fixtures
    bind details off suite-wide so the ambient env cannot leak
    into default-shape claims; 54 tests/173 assertions at
    1f87a6b6. R6 posterior proof STOPPED correctly (2026-09-12,
    codex-23): authority mismatch — the registry equation
    (aif-equations.edn:177-180) declares the B.9 form WITH F_pi,
    but the bound Lean carrier softmax (Holes.lean:7224-7229,
    CLOSED-BY-RECORD) omits F_pi, and its docstring declares the
    F-less form; production selection-scores HAS the F_pi seam,
    so the carrier lags both the declared equation and the code.
    RULED in-lane per no-facade: extend, don't narrow — (A) new
    general carrier softmaxWithFPi + zero-F compatibility
    theorem, the closed softmax and SoftmaxWitness untouched
    (dispatched, codex-23); then (B) reviewer authority edit
    rebinding :lean to the general carrier with a dated note;
    then (C) the 148-coordinate witness against the general
    carrier's F-absent branch (the pinned record has F_pi
    :status :absent), general nonzero-F correspondence retained
    as open until a record with F_pi present exists. Softmax NaN DEFECT
    REPAIRED (2026-09-12, futon2 5750251c/39f2fd0f, reviewed):
    guard at the single selection-scores seam (all softmax-weights
    arities and both direct callers route through it) refuses
    :nonpositive-temperature / :nonfinite-temperature via ex-info
    :refusal with the offending value; positive baseline at
    production tau 1.0 pinned from a pre-repair run and byte-
    identical; 56 tests/301 assertions, receipts at tree 5750251c;
    reviewer reproduced all three refusals + baseline locally;
    serving JVM reloaded from master post-review. R17 binding -> DONE (2026-09-12):
    R17-dirichlet-accumulation-ieee-residuals-v1 admitted at
    :verified-binding (futon2 5360a4a4 admission, 65dce508
    registry; mathlib4 MachineAccumulationWitness.lean at
    7f41356e14/sha256 3ddc2a2d) — 294 exact residual identities
    under contract v1.2, maximum residual 5/36028797018963968
    independently recomputed by the reviewer; scope
    :row12-three-tick-ieee-correspondence (measured recurrence
    correspondence, NOT live R17 wiring, :realised stays false).
    Admission had one refusal round: aif-equations :lean carried
    the annotation "(carrier only, Holes.lean:6935-6936)" and
    validate! requires exact equality with the claim's
    :declaration — refusal retained at 3383caf3, authority
    normalized to the bare declaration name at 77f73a55
    (annotation content preserved in :lean-status/:lean-note).

## Phase 3 — the assurance nodes (4)

17. Credit the Agency's existing work-lifecycle machinery (dispatch
    receipts, parking, coordination ledger) to the loop-assurance
    nodes it already serves — today no node gets credit for it.
    DISCOVERY DONE (2026-09-12, TN-row17-discovery-2026-09-12.md
    2a0acf1d; reviewer spot-verified the R10 boundary,
    execution-evidence refusal, and worker-lost recovery in
    futon3c source). Credits ruled: R10 — full credit
    constructible now (run-scheduled-dispatch! mechanically
    enforces the commission/receipt join with typed :node :R10
    refusals); R20 — two NARROW rows only (work-execution
    tripwires; continuation/deadline handling), never the
    catalogue-wide node (VERIFY zeros stand); R9 — WITHHELD
    (Agency records identities but does not refuse
    author=reviewer; input evidence only; row 19 owns the
    refusal); TRACE — WITHHELD (Agency ledgers are not WM route
    records; a cross-ledger join witness is future work).
    Admission constraint: /tmp ledger hashes are discovery-time
    pins — evidence packets must retain bounded extracted
    fixtures with source hash + extraction time before
    compaction/mutation. Four packets proposed (TN §5). PACKET 1
    STOPPED HONESTLY (2026-09-12, codex-22; verified by the
    reviewer): NO real R10-joined record exists to extract —
    run-scheduled-dispatch! (futon3c f3534b93, 2026-09-08) has
    zero production callers (test-only, caller census verified)
    and the authoritative Futon1b query found 0 records tagged
    [:coordination :scheduled-dispatch :R10] in 20,000 scanned;
    the census E-10-D "live pin" is a 2026-09-06 generic invoke
    predating the boundary (census corrected with a dated note).
    A synthetic fixture record was correctly refused as the
    positive leg. R10 full credit therefore BLOCKED on a build
    prerequisite owned by the integration lane: wire a
    production scheduled entrypoint through
    run-scheduled-dispatch!. The mechanism-level census cells
    (E-10-C/E-10-D exists-by-refusing-code) stand per the census
    vocabulary. PACKET 2 DONE (2026-09-12, futon2
    2cc81a67/ba9b9890/00838b5e, reviewed): real ledger records
    retained for worker-lost-on-restart, operator cancellation,
    delivery failure + a successful control (reviewer
    spot-checked the worker-lost fixture against the live
    ledger — field-identical, redactions declared);
    invoke-no-execution-evidence honestly typed
    :no-real-ledger-instance (no induced failures); 32
    chronology/single-terminal/execution/delivery assertions
    over the fixtures; the credit artifact's maximum-claim
    disclaims the catalogue-wide R20 node, VERIFY zeros
    unchanged; source ledger sha + extraction time pinned; 24h
    compaction of the worker-loss record noted honestly.
    PACKET 3 DONE (2026-09-12, futon2 1a0ab2bf..0fc1a592,
    reviewed): a real completed buffer/poller park cycle
    retained (park-ca4df3cc joined to its terminal invoke job —
    reviewer spot-checked the join in the live store's
    dependency index and the job in the live ledger; payload
    redacted; the projection-not-source-record limitation
    stated); deadline-woken class typed absent (no real
    instance); the invented-dependency negative control ran in
    an ISOLATED store (path verified in the control record and
    receipts — accepted, no early wake, exactly one deadline
    wake; the referential gap named as a limitation, not
    smoothed); credit wording narrow with catalogue-wide R20
    disclaimed; 2 tests/11 assertions; receipts commit-first
    with two failure receipts retained. Packet 4 (R9/TRACE
    non-credit record) dispatched (codex-22).
18. Implement R20's chartered interoceptive commitment link — the
    genuinely missing piece, distinct from row 17's crediting.
19. Implement R9, "No self-certification": the checks that prevent
    the system from certifying its own work must exist and refuse.
20. Extend the evidence census to R11 ("Hierarchical shared
    budget") and R15 ("Hierarchy and timescale"), currently outside
    census scope entirely; each needs its own working-evidence row
    or an explicit recorded basis for why its ladder is complete.
    DISCOVERY DONE (2026-09-12, TN-row20-census-2026-09-12.md
    8ee0dcb7; reviewer verified the census vocabulary/matrix, the
    plumbing vector (aif-equations.edn:1049 at HEAD — TN's
    520-521 pin drifted with concurrent edits), and both V7
    receipt hashes byte-exact). Verdicts, both honest-absence
    (a): R11 and R15 each get a census row of seven typed
    'absent' cells pinned to their retained V7 caller/corpus
    censuses (b80a827f / 508054e6) — built standalone mechanisms
    (hierarchical_budget + adapter; temporal_hierarchy) with
    ZERO production callers and zero node-linked runtime
    records; formal plumbing ladders correctly terminate at
    'named' but the obligated lifecycle ladder is 0/7. TN also
    corrects the premise: R14/R17 have formal rungs but NO
    lifecycle rows (R17 explicitly omitted originally). Two
    independent facts-row packets. R11 ROW DONE (2026-09-12,
    futon2 59941798/aba95c09, p4ng a7d27416, reviewed): seven
    [A11] typed absences with the byte-pinned V7 receipt, five
    planted controls, and the :mechanism-built-and-never-entered
    classification; render control verified (....... -> -------,
    scope 6->7 nodes, formal rung stays named,
    at-obligated-state false; 14 matrix/schema controls green;
    receipts bound to both repos' trees; reviewer spot-verified
    the source pins at HEAD). R15 ROW DONE (2026-09-12, futon2
    99ee3880/0d3bc6ad, p4ng fec6e538, reviewed): seven [A15]
    typed absences, byte-pinned V7 receipt with nine planted
    controls, Campaign-S-in-prose-only recorded; render 7->8
    nodes, strip -------, at-obligated-state false, canonical
    output otherwise unchanged; 14 controls green; no
    completeness/live/nested-model claim. ROW 20 CENSUS
    EXTENSION COMPLETE — both nodes now inside census scope
    with honest-absence rows.

## Phase 4 — the wiring diagram agrees with the code (3)

21. Add to the drawn control diagram the twelve connections that
    exist in code but are not drawn, or annotate edge by edge why
    each stays off the drawing.
22. For the drawn connections that have never fired in any recorded
    run: decide which the qualifying run must exercise (default:
    all that the full loop traverses) and mark the remainder
    aspirational on the figure.
23. R6→R16: keep the per-run correspondence capture running; settle
    the verdict from the build-phase test runs.

## Phase 5 — the certificate and THE run (5)

24. Write the Lean certificate checker at FULL scope: all records
    present and consistent; chosen action equals enacted or carries
    a typed divergence; every node's validation state attested;
    every equation claim tied to its checked declaration at pinned
    bytes; connections attested. The mandatory negative scope may
    contain only items Joe has ruled out in writing — at
    completion, nothing load-bearing remains in it.
25. Write the run→certificate→Lean-file generator with tamper tests
    (every mutated certificate fails), on the September-1 proof
    pattern.
26. Build the on-demand whole-loop entry point over the gated click
    path (MANDATED; repaired-Empirics standard: real work, findings
    routed, breakdowns fixed in-lane).
27. Complete the F11 mission: the F2 reconciliation committed
    through author → independent review → gates; discharges
    repair-024.
28. THE qualifying run: the full loop, on demand, every prior row
    closed, certificate computed and Lean-validated, presented to
    Joe with nothing to decide but acceptance of a stated,
    fully-attested fact.

## Phase 6 — the papers say only what is now true (4)

29. Rewrite the seven record-contradicted claims to their evidenced
    scope (acts-on-recommendation, evidence-not-manufactured,
    drawing-agreement-at-build, deleted per-tick scalar,
    cascade-vs-first-move, witness-feeds-next-belief,
    BMR-over-counts) — several become TRUE again as phases 1–5
    land; rewrite to the certificate, not around it.
30. Point the four companion macros (preregistration, empirics,
    faithfulness table, retraction) at the real sections where the
    material now lives; criterion: only certificate-attested
    material discharges a promise.
31. Locate and pin, or honestly retire, the historical records the
    paper cites but cannot produce (attempt-061 original phase log
    and similar).
32. Rewrite the empirics narrative to the ruled discourse: one
    certified run; defect-feedback described as a feature; no
    commit-continuity story.

## Phase 7 — standing tracker rows (7)

33. RUN13: convergence certificates over accepted runs (after
    Phase 5).
34. F10 slice 1: the outcome-domain decision sheet (produced as an
    artifact for Joe's asynchronous read; does not block).
35. F12: the four O-laws stated in Lean against the Cascade
    carrier, witnessed on a real constructed cascade.
36. U80: dependency-first tickets per retiring implementation
    refusal (after F12).
37. U83: the generated blocked-attempts view.
38. U84: the census count re-run and ALIGN qualification
    re-affirmed or amended with dated evidence.
39. U88: the C_tau horizon-structure artifact (async; does not
    block).

## Done this week (not on the list)

Fold seam + typed refusals; live selection-vs-enaction verdicts;
eight machine-declaration contracts + checked union; readiness
accounting extension; honest Box 3 (obligated-state headline,
lifecycle strips); R3a hosting correction + Box 2 regeneration;
certificate spec + mechanical emitter with tamper controls;
projection repair (row 4 loads it); runner hardening against
misattributed author claims; the PLoP completion list.
