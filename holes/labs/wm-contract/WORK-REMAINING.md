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

## Phase 0 — the machinery that makes "provably so" possible (5)

1. Build the witness carrier: the mechanism that attaches a
   measured-against-production proof to the node that owns it, with
   refusals when the proof or link is missing. The witness-evidence
   definition (TN-node-witness-carrier-proposal, 2df0662d) is
   ADOPTED as the working standard; its honesty constraints
   (maximum claims, named refusals) are unchanged.
2. Credit the three measurement proofs that already exist —
   observation, precision, prediction-error, each proven equal to
   production output — to their nodes through row 1.
3. DONE (14dc45cc, reviewed; live 2026-09-12). The click path
   records both output-validator verdicts (pass/fail + findings +
   input digests) inside the construction record it saves, on
   every status including refusal; digests recompute; reloaded
   into the serving JVM.
4. DONE (37f111c4, reloaded + live-probed 2026-09-12). The
   repaired end-of-run projection is in the running service.
5. Make every terminal path produce a run record — the stop-line
   repair path currently produces none
   (`runner-did-not-observe-topology-route`, v3 cycle). Discovery
   then fix.

## Phase 1 — the six probability objects the mathematics assumes
## and the machine does not have (6)

Each row means: implement it in the running system, state it in
Lean, and prove the implementation matches at reference points.
(FUNDAMENTALS.edn names each; a scoping pass pins current state and
acceptance per object before implementation packets go out.)

6. The predictive-outcome-kernel constructor.
7. The belief-to-state distribution.
8. The controlled transition kernel.
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
