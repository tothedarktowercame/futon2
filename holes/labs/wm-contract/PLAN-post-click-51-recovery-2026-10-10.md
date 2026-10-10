# War Machine recovery review after click 51

**Status:** review and repair; no click authorized.
**Authority:** Joe, 2026-10-10, in discussion with codex-68.

This note records the worklist resulting from review of the restored
pre-claude-12 runtime, the Lean requirements, and run
`2026-10-10-c82068b4-6ba2-4480-b162-cf77a8d66fda`.  The run's reports are
diagnostic inputs, not waivers.  A known defect must be fixed and checked
before another click; inconsistent reporting is itself a defect to repair,
not a reason to disregard the requirement.

## Worklist

1. Reconcile Q2 and Q8 with the outer task policy, the target-local cascade
   policy set, and `Proof2/TargetGrainG.lean`.  G scores policies.  Do not let
   a posterior over work cascades silently become a task selector, and do not
   call cascade-policy selection an “inner task selector”.

2. Locate the one non-strict completion-preference comparison reported by Q9
   (2 of 3 were strict), determine whether it is a C defect or an exporter
   correspondence defect, and repair the responsible layer.

3. Repair the run-facts/report-card correspondence.  In particular, Q4 must
   not simultaneously report absent information gain and thousands of
   per-policy information terms; Q7 must count the selected execution path,
   not absences in every rejected candidate; and Q8 must use globally stable
   policy identities and the declared seat/learned-behaviour population.

4. Specify what the task policy observes and optimises.  The task decision is
   parametric in the task, while cascade construction and G remain policy
   grain.  Reconcile the restored task-first runtime with the outer
   meta-policy construction model rather than restoring the all-target
   work-cascade posterior.

5. Keep the selected cascade open during enactment.  Selection commits to a
   policy, not to an immutable one-shot ruling.  As construction, authoring,
   review, measurements, blockers, and new observations arrive, the policy may
   be refined at operational speed.  Each refinement must:

   - remain scoped to the already selected task unless the outer task policy
     makes a separately receipted new decision;
   - retain the prior cascade, triggering observation/blocker, candidate
     revisions, and the reason for the chosen revision;
   - compare revised cascade policies at policy grain using the applicable G
     terms and current evidence;
   - update the author/reviewer contract so the currently enacted step and
     acceptance criteria are explicit;
   - preserve selected-to-enacted identity and close-time evidence without
     pretending that mere selection proves pattern use; and
   - stop or emit a typed repair obligation when no warranted refinement is
     available.

This extends the already recorded property in `M-G-wm-wiring.md`: G applies
to partial cascades, which are constructed progressively per problem.  The
new requirement is that progressive construction remains live during the
build rather than ending at initial dispatch.

## Click gate

No new click is requested until items 1–5 have explicit dispositions and all
known click-path defects are fixed and verified in the serving process.  A
reporting contradiction, unbounded memory path, or unexercised production
writer keeps the gate closed.

## Progress

- 2026-10-10, item 3/Q4 correspondence: repaired the Clojure exporter and
  Lean-term generator to supply all six Q4 carriers required by
  `Requirements.lean`.  The exporter now refuses to combine per-occurrence G
  terms with a differently sized policy identity set.  On the real
  2026-10-05 run it reports one compared policy, horizon 4, preference step
  `[3]`, no completed-progress-graded steps, and contributing terms
  `risk=true, ambiguity=false, informationGain=false`; all three term
  carriers are recorded for that policy.  This is an honest Q4 failure and
  removes the later 5,413-term/16-policy contradiction.  Q7 and Q8
  correspondence work remains open.
