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

6. LEGS (a)+(b) DONE; LEG (c) REOPENED 2026-09-12 (codex-24
   batch-A audit, TN 7e19ef83): the witness readback set
   :lean-reference to the production row itself — circular, so
   its 0.0 deltas were tautological. Caught by the batch-A
   exact-reference audit, missed in the original review (a
   mechanical delta check cannot detect reference circularity;
   reviews now check reference independence). The Lean module,
   bridge theorem, refusals, and sensitivity control all stand
   (proven in Lean, independent). REPAIR (packet out, codex-24):
   derive independent exact/rational references from the pinned
   row-9 inputs; record production-vs-reference deltas plus the
   :float-carried admission (production sums are 1.00000000000000005
   /...06 exactly — within v1.1's bound, but the exact Lean
   normalization must use the exact-rational reference, never the
   production bytes as their own reference). Failed Lean attempt
   retained unamended (mathlib4 c86eed2bba, removed f893753dfc).
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
    execution — retained in f35ae290's history.
13. IN PROGRESS: discovery TN reviewed (817957a8: anchor at
    war_machine.clj:6280, carry in the daily WM trace as
    :accumulation-state, migration-required refusal not silent
    reinit, TN-9a boundary on the R1->R17/R2->R17 flip);
    implementation packet out (codex-23). The TN's shared
    :accumulation-update-input envelope also serves row 15's
    belief-update capture — one retention seam, two proofs.
14. Build the machine-Q link: the forward model actually receiving
    a policy-conditioned outcome distribution. RULED: build, not
    descope — narrowing the claim instead is the facade option.
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
    from trace record 0, deltas 0.0. DEPTH and ACTION join the
    needs-capture family (codex-22 split finding, no synthetic
    substitutes): trace omits :policy-depth/:horizon-steps and the
    ranked-action input maps + selector option packet — capture
    candidates for the row-13 trace envelope seam, alongside
    belief update and the row-16 five.
16. IN PROGRESS. Inventory done and reviewed (TN 6e6b2f5c): 18
    equation-bearing declarations — 4 admitted (26 claims), 4
    under row 15, 5 provable now from retained pins, 5 needing
    additive input capture (R8 free-energy, R8 policy-F, R5
    ambiguity, R6 policy-set, R17 model-reduction — capture
    candidates for the row-13 trace envelope seam). Scope: 10
    proofs. BATCH A SPLIT (TN 7e19ef83, audit supersedes the
    inventory's provable-now verdicts for these three): R4 needs
    the row-6 leg-(c) repair (independent exact reference); R5
    risk needs a complete retained Q/C pair from the row-14
    consumer plus a reviewed production categorical-risk function
    (unsupported-risk classifies one boundary only; the
    strict-positivity premise is unprovable on seven-zero C); R5
    expected-free-energy needs full scoring-input capture or a
    reviewed production EFE composer (the sum is inline in the
    scorer; no production function accepts the retained scalars).
    Batch B in flight (codex-22: R6 softmax, R17 binding — same
    audit discipline applies).

## Phase 3 — the assurance nodes (4)

17. Credit the Agency's existing work-lifecycle machinery (dispatch
    receipts, parking, coordination ledger) to the loop-assurance
    nodes it already serves — today no node gets credit for it.
18. Implement R20's chartered interoceptive commitment link — the
    genuinely missing piece, distinct from row 17's crediting.
19. Implement R9, "No self-certification": the checks that prevent
    the system from certifying its own work must exist and refuse.
20. Extend the evidence census to R11 ("Hierarchical shared
    budget") and R15 ("Hierarchy and timescale"), currently outside
    census scope entirely; each needs its own working-evidence row
    or an explicit recorded basis for why its ladder is complete.

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
