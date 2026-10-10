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

- 2026-10-10, real-run Q4 producer diagnosis: replaying
  `data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn`
  reproduces one scoring row with risk `1.0498221244986776`, ambiguity `0.0`,
  expected information gain `0.0`, horizon 4, preference steps `[3]`, and no
  graded progress steps.  The first responsible producer is
  `futon2.aif.wm.cascade-decision/class-observation-model`: it deliberately
  defines deterministic class emission and a unit-mass
  `:ending/not-yet-evaluated` preference at taus 1--3, followed by the fixed
  terminal class preference at tau 4.  Accordingly,
  `cascade-observation-scoring` computes genuinely zero observation entropy;
  this is not a missing ambiguity carrier.  The joint scorer also does not
  enable its separate Beta-pattern parameter-information mode.  Although
  `cascade-shape-g` has a separate `:progress-count` model and progressive C,
  replacing the joint class model with it would change the declared
  observation and preference semantics rather than recover dropped data.
  Q4 therefore has a design blocker: define the joint policy's epistemic
  observation and per-tau progressive preference before a falsifying producer
  regression or implementation repair can be written.  No term was made
  positive, no field was renamed, and no preference row was synthesized.

- 2026-10-10, item 3/Q7 correspondence: repaired `pathAbsenceCount` to walk
  the enacted candidate and its selection-to-terminal receipts, or the typed
  abstention carrier when no action was chosen.  It no longer walks rejected
  candidates or population-wide certificate diagnostics.  On the real
  2026-10-05 record this changes the count from 65 to 8.  All eight remaining
  absences belong to the selected candidate/receipt path and therefore remain
  honest Q7 failures rather than being waived.  Q8 correspondence remains
  open, as does repair of the eight selected-path absences.

- 2026-10-10, item 3/Q7 producer repair: a completed selection that correctly
  requires no interpretation request is now retained as typed
  `:not-applicable`, not falsely as `:absent`.  Applied to the 2026-10-05
  record shape, this removes one reporting-induced absence while leaving the
  seven substantive failures visible: two reviewer-falsifier refusals and
  five missing policy-prefix F carriers.  No Lean change is required.

- 2026-10-10, selected-policy F correspondence: the runtime no longer runs a
  reduced `sigma(log E - gamma G)` law when a menu policy has no admitted
  prefix F.  It now returns typed `:free-energy-not-supplied`, naming every
  affected policy, as `Proof2/PrefixFreeEnergyPosterior.lean` requires.
  Contradictory prefixes retain zero weight.  This repairs the false posterior
  rather than relabelling the five missing carriers; producing sufficient
  policy-grain history remains an operational prerequisite for selection.

- 2026-10-10, item 3/Q8 identity correspondence: `constructedCascades` and
  `comparedPolicies` now use the canonical full-action SHA-256 identity already
  used by the runtime.  The compared population comes from the actual
  selection posterior, not a separate certificate list, and repeated local
  labels such as `:C1` no longer collapse policies belonging to different
  targets.  Missing per-target slice/pool receipts still make Q8 honestly
  non-recomputable; this change does not manufacture them.

- 2026-10-10, item 3/Q2/Q8 construction correspondence: query-time slices
  now remain attached as provenance after interpretation, without replacing
  or widening the admitted interpretation pool.  Selection records, for
  every assembled target, the retrieved slice, actual constructor pool,
  whole-library-pin verdict, and number of policies that reached the scoring
  certificate; missing slice provenance is typed absent.  The exporter emits
  this receipt and derives `constructorPatternCount` from the union of the
  recorded pools.  It deliberately does not force `pool = slice` or a
  positive policy count: those are Q2 requirements, and a real mismatch must
  fail rather than be normalized away.  Historical records without the new
  carrier remain non-recomputable.

- 2026-10-10, selected-policy F identity repair: policy-prefix admission no
  longer indexes histories by constructor-local labels such as `:C1`, which
  collide across targets.  Prefixes are now stored and consumed by Lean's
  complete cascade `PolicyKey` (target, ordered patterns, semilattice).  A
  two-target regression with the same local label proves that each policy
  receives only its own observed F.  The Lean rule remains strict: every menu
  policy needs a nonempty admitted history; no initial `F = 0` is invented.
  Several older joint-decision fixtures still assert selection without such
  histories and must be repaired to carry per-policy steps or expect the
  specified typed refusal.

- 2026-10-10, prefix-F fixture correspondence: repaired the joint-decision
  suite so tests whose subject is downstream of prefix admission supply one
  explicit admitted step for every fixture policy.  The diagnostic
  single-lane helper, which has no flight carrier, now asserts its specified
  R14 `:free-energy-not-supplied` boundary.  The full joint-decision namespace
  is clean again (16 tests, 107 assertions).  This is test repair, not a
  production waiver: the runtime still refuses any real menu containing a
  never-executed policy, exactly as the current Lean specification says.

- 2026-10-10, preference-schedule fixture correspondence: repaired the
  preference-schedule decision suite with one explicit admitted step keyed by
  the complete `PolicyKey` for every menu policy, including the no-op policy.
  The lane entry point now consumes those supplied conditioning steps at R14,
  and the joint diagnostic lane forwards the same carrier.  A focused
  no-history case still stops at R14 with typed
  `:free-energy-not-supplied`; no initial F value is inferred.  The targeted
  preference-schedule and run-facts namespaces are clean together (18 tests,
  62 assertions).

- 2026-10-10, item 2/Q9 diagnosis: the reported 2-of-3 result is a real C
  defect, not an exporter classification error.  The class model defines
  `:focused`, `:related`, and `:unrelated` as outcomes where the candidate's
  own acceptance criterion was reached; `:stop-the-line` means it was not.
  Joe's fixed 2026-09-22 terminal distribution is 55/35/5/5, so the
  `:unrelated` completion outcome ties the non-closing outcome.  A focused
  characterization test pins the exact three comparisons and the single
  falsifier.  `Requirements.Q9` requires every represented completion pair
  to be strict, while `CTauClassPreference.terminal_order` explicitly proves
  this tie.  Repair therefore requires an explicit specification decision;
  neither the census nor the meaning of `:unrelated` may be changed to hide
  it.

- 2026-10-10, item 2/Q9 design disposition: do not repair the tie by changing
  5% to an adjacent arbitrary value.  `Q9-FACTOR-C-DESIGN-2026-10-10.md`
  separates closure from relevance-given-closure, recommends aggregate
  closure dominance as the Q9 law, and records the stronger admissibility
  constraint required if every individual completion class must dominate.
  It also distinguishes learning predictive outcome frequencies from changing
  normative C: the latter requires registered evaluative evidence and
  authority.  Runtime and Lean semantics remain unchanged pending acceptance
  of that specification decision.
