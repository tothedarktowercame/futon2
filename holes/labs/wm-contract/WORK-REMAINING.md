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
2. IN PROGRESS (1ca908d3). The three proofs' 24 claims are carried
   as :proposed with pins audited (precision matches; observation's
   one historical divergence and prediction-error's two census
   divergences recorded, not re-pinned). The carrier correctly
   refuses to admit them: retained F8 transcripts carry no checker
   exit codes and no review receipts. REMAINING: re-run the three
   Lean witness checkers to produce executed verification receipts,
   independent review receipts, then admission + canonical registry
   integration as a reviewed step.
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

6. The predictive-outcome-kernel constructor.
7. The belief-to-state distribution.
8. DONE as source module + proofs + witness (futon2 5f6b40da +
   41d61a40; mathlib4 e1b75181..c1c71011 + 34180899 rejection
   cases; reviewed via receipts 2026-09-12). Declared-prior
   authority only, honestly recorded (:empirical-claim false; no
   measured action law exists). Live-filter wiring belongs to the
   integration owner after rows 7/9/10.
9. The policy-conditioned state predictive.
10. The machine preference distribution.
11. The parameter kernels.

## Phase 2 — learning arrows and the remaining equations (5)

12. U91: replace R17's per-tick recount with genuine accumulation —
    counts that carry forward tick to tick by the declared update
    rule, proven against RealisesDeclaredAccumulation.
13. U92: feed that accumulation from the live tick's observation
    and belief, making the two learning arrows into R17 real.
14. Build the machine-Q link: the forward model actually receiving
    a policy-conditioned outcome distribution. RULED: build, not
    descope — narrowing the claim instead is the facade option.
15. Write measurement proofs for the five machine declarations that
    lack one: belief state, belief update, depth, temperature,
    action.
16. Write measurement proofs for the remaining equation-bearing
    nodes not covered by rows 2 and 15 (inventory first; roughly
    five).

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
