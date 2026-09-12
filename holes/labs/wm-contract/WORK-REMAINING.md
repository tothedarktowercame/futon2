# The work remaining — one definitive list

2026-09-12, claude-15, at Joe's direction. This consolidates every
open item from: the paper's generated trackers (Box 2, Box 3), the
private trackers (worklist.edn, FUNDAMENTALS.edn, aif-equations.edn
holes, the process census), the PLoP completion list
(COMPLETION-LIST-plop2026-2026-09-12.md), and the certificate lane.
One task per row, plain language, priority order. Rows marked
**[JOE]** are decisions only you can make; everything else is build
or writing work. Update this file in place as rows close; git holds
the history.

**38 rows total: 7 to certify a run + 17 for per-node evidence + 3
for the wiring diagram + 4 for the papers + 7 standing tracker rows.**

## A. Certify one real run (the ruled definition of done) — 7 rows

1. Make the click path record its two output-validator verdicts
   (pass/fail + findings) inside the construction record it saves.
   It checks them today but does not write them down, so no run can
   be certified yet.
2. Load the repaired end-of-run projection code into the running
   service (waiting only for the current F11 cycle to finish; the
   live service still loses the terminal summary of any run that
   stops early).
3. Finish the standing F11 job: get the F2 reconciliation change
   committed through the author → independent review → gates path.
   This also discharges the last stop-line repair (repair-024). A
   grounded cycle here is a qualifying-run candidate. (Item 3 is
   running now.)
4. **[JOE go needed]** Build the on-demand whole-loop entry point:
   run the full loop once, by hand, over the same gated click path —
   what the old cron used to trigger, at the repaired-Empirics
   standard. Its run is the other qualifying-run candidate.
5. Write the Lean certificate checker: the certificate structure
   plus four computable checks — all seven records present and
   mutually consistent; chosen action equals enacted action or
   carries an explicitly typed divergence; every equation claim tied
   to its checked Lean declaration at the pinned version; the
   statement of what is NOT checked present and exact.
6. Write the generator that turns one run's records into that
   certificate and into a Lean file whose checks are proved by
   computation (the same pattern as the September-1 route proof),
   plus tamper tests: every mutated certificate must fail.
7. **[JOE]** Review the check-strength section of the certificate
   spec, choose which run qualifies, and accept or reject the
   certificate. Acceptance closes the last Lean hole
   (wmRunConformsToWiring) and is the finish line you defined.

## B. Per-node evidence: every box in the diagram backed by proof it
## does what the paper says — 17 rows

8. **[JOE]** Approve the proposed definition of node-level witness
   evidence (codex-16's paragraph), so measured-against-production
   proofs can be credited to the node that owns them.
9. Build the witness carrier: the mechanism that attaches an
   existing measurement proof to its owning node, with refusals when
   the proof or the link is missing.
10. Credit the three measurement proofs that already exist — the
    observation, precision, and prediction-error declarations are
    each proven equal to production output at every reference point
    — to their nodes.
11. Write the same kind of measurement proof for the five machine
    declarations that lack one: belief state, belief update, depth,
    temperature, action.
12. Inventory which remaining equation-bearing nodes still lack
    measurement evidence after rows 10–11, and write those proofs
    (roughly five more).
13. Build the predictive-outcome-kernel constructor — the first of
    six probability objects the paper's mathematics assumes and the
    machine does not have. Each means: implement it in the running
    system and state it in Lean.
14. Build the belief-to-state distribution (second missing
    probability object).
15. Build the controlled transition kernel (third).
16. Build the policy-conditioned state predictive (fourth).
17. Build the machine preference distribution (fifth).
18. Build the parameter kernels (sixth).
19. U91: replace R17's per-tick recount with genuine learning — 
    counts that accumulate across ticks by the declared update rule,
    proven against the repair predicate
    (RealisesDeclaredAccumulation).
20. U92: feed that accumulation from the live tick's observation and
    belief, making the two learning arrows into R17 real.
21. **[JOE]** Decide the machine-Q question: build the missing link
    (the forward model actually receiving a policy-conditioned
    outcome distribution) or formally narrow the paper's claim. Six
    tracker rows collapse into this one decision plus at most one
    build.
22. Credit the Agency's existing work-lifecycle machinery (dispatch
    receipts, parking, coordination ledger) to the loop-assurance
    nodes it already serves — it runs, but no node gets credit, so
    "Scheduled entrypoint", "Two-layer calibration", "Interoceptive
    tripwires" and the trace store all show nearly empty assurance.
23. Implement the chartered interoceptive commitment link for R20
    (the tripwires node) — the piece that is genuinely missing, not
    just uncredited.
24. Implement R9, "No self-certification" — the one node with
    essentially nothing behind it: the checks that prevent the
    system from certifying its own work need to exist and refuse.

## C. The wiring diagram versus the code — 3 rows

25. Add to the drawn control diagram the twelve connections that
    exist in code but are not drawn (or annotate, edge by edge, why
    each stays off the drawing). Until then the diagram under-draws
    the system.
26. Decide, for the drawn connections that have never fired in any
    recorded run (19 of 22 in the September run), which the
    qualifying run should exercise and which are aspirational — and
    mark the aspirational ones as such on the figure.
27. R6→R16 (structure learning feeding the drawing): keep the
    per-run correspondence capture running and settle its verdict
    once enough runs exist. Rides on rows 3–4.

## D. The papers say only what is true — 4 rows

28. Rewrite the seven claims the record contradicts to their
    evidenced scope: "acts on what it recommends" (universal),
    "evidence it did not manufacture" (universal), "drawing
    agreement is a checked property of the build", the deleted
    per-tick fit scalar, "the thing scored is the cascade",
    "the witness feeds the next belief", "BMR over accumulated
    counts".
29. **[JOE]** Decide which paper — PLoP, futon-2026, or the
    mathematical companion — owes each of the four promised
    deliverables (preregistration, empirics, faithfulness table,
    retraction), and point the four macros at real sections. This
    single decision collapses most of the sixty unresolved
    completion-list rows.
30. Locate and pin, or honestly retire, the historical records the
    paper cites but cannot currently produce (the attempt-061
    original phase log and similar).
31. Rewrite the empirics narrative to the ruled discourse: one
    certified run rather than 77 uncertified ones; the
    defect-feedback loop described as a feature, not as validation;
    no commit-continuity story.

## E. Standing tracker rows not absorbed above — 7 rows

32. RUN13: convergence certificates over accepted runs (unblocks
    after block A lands).
33. F10, first slice: the outcome-domain decision sheet for Joe.
34. F12: the four O-laws stated in Lean against the existing Cascade
    carrier, each witnessed on a real constructed cascade.
35. U80: dependency-first tickets for each retiring implementation
    refusal (depends on F12).
36. U83: the generated blocked-attempts view with counts and
    evidence links.
37. U84: the census count re-run and the ALIGN qualification
    re-affirmed or amended with dated evidence.
38. U88: the C_tau horizon-structure interactive pass with Joe.

## Not on this list

Already done (this week): the fold seam and its typed refusals; the
selection-vs-enaction live verdict; the eight machine-declaration
contracts and their checked union; the readiness accounting
extension; the honest Box 3 (obligated-state headline, lifecycle
strips); the R3a hosting correction and Box 2 regeneration; the
certificate spec and its mechanical emitter with tamper controls;
the end-of-run projection repair (committed, not yet loaded — row 2);
the PLoP completion list itself. The old worklist RUN4 master row's
"Joe edits the Lean hole" is row 7 here, not a separate task.
