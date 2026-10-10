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

### Open-cascade refinement certificate

Joe, 2026-10-10: keeping the cascade open as the run progresses is the largest
architectural change.  A stuck run must not merely fail and defer the problem.
It performs one of two explicitly receipted moves:

1. search the pattern library for related patterns, select a warranted result,
   revise the live cascade, and use that pattern to get unstuck; or
2. when the search finds no adequate pattern, author a new pattern, admit it
   through the same interpretation/construction checks, revise the live
   cascade, and use it to get unstuck.

The refinement certificate must retain at least:

- the selected cascade and the observation/blocker that opened refinement;
- the library snapshot and search query/evidence;
- every related pattern considered and the reason for selection or rejection;
- either the existing pattern selected, or the full newly authored pattern
  with provenance and admission evidence;
- the revised cascade, its relation to the prior cascade, and its canonical
  construction/admission certificate;
- the concrete next step enabled by the revision and its selected-to-enacted
  identity; and
- the result of applying it, including another typed refinement or stop if it
  still does not progress.

These certificates are first-class Morning Briefing inputs.  The briefing
must show which runs became stuck, which branch was taken (retrieve or author),
what pattern changed the cascade, whether it enabled progress, and any case
where neither branch produced an admissible revision.  A repair obligation may
remain as escalation evidence, but writing one without first executing and
certifying the retrieve-or-author process does not satisfy this requirement.

Falsifiers include: a stuck boundary closes directly to a repair obligation;
search results or rejected candidates are absent; a newly written pattern is
used without normal admission; the revised cascade is not compared with and
linked to its predecessor; the enabled step is not enacted; or the Morning
Briefing cannot reconstruct the refinement chain from durable receipts.

This extends the already recorded property in `M-G-wm-wiring.md`: G applies
to partial cascades, which are constructed progressively per problem.  The
new requirement is that progressive construction remains live during the
build rather than ending at initial dispatch.

## Click gate

No new click is requested until items 1–5 have explicit dispositions and all
known click-path defects are fixed and verified in the serving process.  A
reporting contradiction, unbounded memory path, or unexercised production
writer keeps the gate closed.

## Specification-extension authority

Joe, 2026-10-10: where an existing Lean carrier substantially defines the
required behavior, the natural repair may extend that Lean model and the
runtime certificates together.  Such a change should be additive where
possible: state the missing bridge or evidence field, add the Lean declaration
and falsifier, emit the corresponding runtime receipt, and gate the production
composition root on it.  Do not stop at “the specification is incomplete”
when the existing formal work determines a faithful extension; do not replace
or weaken an existing definition merely to accommodate current Clojure.

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

- 2026-10-10, real-run Q7 reviewer-falsifier diagnosis: the same persisted
  2026-10-05 record still exports `pathAbsenceCount = 8`, including exactly
  `[:failure :detail :reviewer-falsifier]` and
  `[:failure :detail :reviewer-falsifier-verification]`.  These are truthful
  runtime refusals, not absent detail misclassified by the exporter: the
  retained failure is `:reviewer-falsifier-failed`, the receipt is
  `:refused`, and independent reconstruction refuses with
  `:reviewer-falsifier-applicable-check-failed`.  Selection pinned
  `futon7/holes/M-interim-director-proxy-metric-inventory.md` at revision
  `ae45d5472d7764c99ab4cbb182b247f7adebd56e`, content SHA-256
  `f9fcb8c2b6dd2700687d7732de395bbc6a495d5c60f4781cd27e98f2f8601d98`.
  Author commit `7a9113dfd245ab9918da304f32fc62845ef65741` changed that mission
  document to SHA-256
  `7df9b4de959ff80d08bddd208e8fc34b80b37053fc2ec158d8f7aa36b956a544`;
  its diff rewrote acceptance-section status prose and did not perform the
  executor-only witnessed checkbox advancement provided by `actuator-a3`.
  `mission-standing-observation` therefore correctly refused the source
  mismatch before interpreting standing.  Re-pinning the falsifier to the
  author's changed mission statement would discard the selected source
  authority and weaken mission-statement immutability.  No producer or
  exporter repair is justified, and the historical pair remains counted.

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

- 2026-10-10, prefix-F operational-satisfiability diagnosis: the failed
  historical all-target run is
  `data/wm-runs/tick-run-record-2026-10-10-c82068b4-6ba2-4480-b162-cf77a8d66fda.edn`.
  Its selection certificate contains 5,413 candidate policies across 343
  targets and 5,413 distinct,
  non-nil complete `PolicyKey`s.  Read-only folding of the current production
  store `data/wm-interpretations/flights` reads all seven flight records with
  no unreadable records, but finds zero `:wm/conditioning-step-v1` entries;
  coverage for that historical menu is therefore 0 keys with admitted history
  and 5,413 keys without it.  There are no stored policy keys to be dropped or
  mismatched.  These are not a standing population of current policies:
  current main selects one outer task first and constructs only that target's
  policy family, as recorded below.  The historical measurement demonstrates
  that the all-target run had no prefix-F support; it does not measure the size
  or history coverage of a prospective current-main menu.
  `PrefixFreeEnergyPosterior.lean` requires a nonempty admitted prefix for
  every menu policy and makes absence `notSupplied`, while
  `flight/conditioning-step` is recorded only after a policy has been selected
  and enacted.  Consequently a newly constructed, previously unseen complete
  `PolicyKey` cannot acquire the required history before its first selection
  under the current protocol.  This is an operational/design blocker, not a
  bounded implementation defect.  Before another click, the specification
  must explicitly choose and formalize a bootstrap authority (for example an
  admitted prior/history for unseen policies), or constrain menus to keys with
  prior enacted history and separately specify how new policies enter.  Either
  choice changes the model/protocol; the runtime must not infer `F = 0`.

- 2026-10-10, correction to the preceding diagnosis: the c82068b4 population
  is **quarantined as non-canonical** and must not support a conclusion about
  the current cascade-policy protocol.  A policy is not made canonical merely
  by fitting the structural shape of a `PolicyKey`.  The governing definition
  is `DarkTower.WarMachine.GOverCascades.CascadePolicy`, backed by
  `CascadeEFE.Policy`: an interpreted, admissible composition of design
  patterns retaining nodes, edges, precedence, guarded transitions and the
  predictive model on which cascade-grain G is defined.  The historical run's
  5,413 automatically generated keys across 343 targets came from the
  all-target experiment that displaced the separately chosen task.  That
  population did not establish conformance with the canonical cascade source
  and should have been refused before scoring.  Its 0/5,413 prefix-history
  census is consequently descriptive only of the aberrant run; the claimed
  current operational cold-start blocker is retracted pending measurement on
  a conformant, target-local cascade menu.  The abstract Lean rule that an
  admitted prefix cannot be invented remains in force, but this record does
  not show which conformant policies would encounter it.

- 2026-10-10, process diagnosis for that mistake: the recovery audit began
  from Claude's failed run and its report-card fields, then treated a complete
  `PolicyKey` digest as sufficient evidence of policy identity.  It did not
  begin from `GOverCascades.lean`, `CascadeEFEPolicies`, the months of work in
  `M-G-over-cascades.md`, or the existing
  `problems/g-over-cascade-is-undefined` warning.  This repeated the precise
  failure documented by `TN-G-over-cascades-revisited.md`: accepting a typed
  collection supplied by the runtime without auditing where its cascades came
  from.  Subsequent recovery evidence must establish canonical cascade
  construction and admissibility before counting or scoring a policy menu.

- 2026-10-10, items 1/4 outer-task versus cascade-policy diagnosis: Lean does
  not define an outer task policy.  `Proof2/TargetGrainG.lean` proves when the
  G difference between candidate laws localises to each target's token set;
  its own header says that a prior over targets before any candidate exists is
  undefined.  `Proof2/CascadePolicySet.lean` and
  `Proof2/PrefixFreeEnergyPosterior.lean` bind the scored/posterior carrier to
  complete cascade `PolicyKey`s, not task identities.  Current main observes
  open M/E/T registry rows and pinned pipeline/ownership/standing evidence in
  `meta-live-outer-selector`, then `meta-pipeline-selector/select` chooses one
  task by pairwise sums of shared normalized task-state cost channels.  That is
  a deterministic heuristic ranking signal (despite the receipt reason
  `:minimum-pairwise-task-state-G`), not the Lean cascade-policy G and not a
  declared predictive/preference objective.  The fuller
  `meta-outer-policy/evaluate` objective is proposal/script machinery and is
  not called by the production composition root.  The missing carrier is an
  authorised, formal outer observation/prediction/preference policy and its
  selection theorem; no such objective may be inferred from tactical G.
  After the outer receipt, current `war_machine/judge` reduces
  `cascade-targets` to exactly its chosen id, constructs target-local cascade
  policies, and only then lets `wm.cascade-decision/select-and-record-cascade!`
  compute G and the policy posterior.  Existing regression
  `outer-task-is-selected-before-and-independently-of-cascade-material` pins
  that order.

- 2026-10-10, persisted c82068b4 correspondence: the retained canonical outer
  receipt honestly has status `:absent`, reason
  `:receipt-identity-inconsistent`.  Its observed META receipt selected
  `M-interim-director-proxy-metric-inventory` from 300 candidates by
  `:minimum-pairwise-task-state-G`, but the run's 5,413-policy posterior ranged
  across 343 targets and selected the sole recorded policy for
  `T-repair-occ-487ca3f2205bf084f5d9bbb6b753cdc3908f106e8ff3aec60243a1bff7c34581`.
  Thus that historical run let an all-target policy comparison displace the
  outer choice; the posterior was not a legitimate task selector.  Current
  main already contains the bounded correction in `bb67416b0` (select task
  first and construct cascades only for that target), so no new runtime change
  or restoration of the all-target posterior is justified.  Completing item
  4 requires the explicit outer-policy specification above, not reuse of
  cascade G.

- 2026-10-10, item 5 selected-cascade dispatch audit: the selected cascade is
  not discarded before author or reviewer dispatch.  The applicable Lean
  minimum is explicit selected/enacted identity, not an inference from a
  selection certificate: `CertificateStates.SelectionEnaction` distinguishes
  an exact match, a typed grounded divergence, and refusal, while
  `ConstructionReceipt.Receipt` binds the ordered construction and its
  precedence evidence.  In current Clojure, `selected-entry` carries the
  selected action and enacted step into `construct-selected-action`; the
  resulting construction retains the exact action, ordered `:precedence`,
  interpretation and construction receipts, guards, transitions/products,
  observation locators, and enacted-step acceptance criterion.
  `cascade-plan/cascade-plan-text` renders those carriers into both
  `author-prompt` and `reviewer-prompt`.  Before dispatch,
  `d-predecessor-task-authority/capture-result` verifies that the occurrence's
  action equals the selected action and records a `:candidate-to-minted-join`
  whose selected and enacted action digests match.  `dispatch!` sends the full
  prompt prefixed by a digest of that D-task dispatch, and `complete!` retains
  the join at close.  Build reporting separately starts with
  `:patterns-selected` equal to the construction order and `:patterns-used []`;
  selection therefore is not reported as proof of pattern use.

- 2026-10-10, persisted task-local dispatch evidence: the most recent retained
  task-local run reaching author dispatch is
  `data/wm-runs/tick-run-record-2026-10-09-513e6445-4ce6-4bc4-a685-c5a5617a56de.edn`.
  It selected `M-interim-director-proxy-metric-inventory` and complete key
  `[:pattern-cascade "M-interim-director-proxy-metric-inventory"
  [:ukrns/reader-run-path :war-machine/state-capture
  :orchestration/recorded-handoff
  :measurement/warrant-travels-with-the-number] {}]`.  The corresponding
  persisted D-task record is
  `/home/joe/code/futon3c/data/wm-d-task-enactment/action-235aed91-2265-4a14-93c1-13512c40b54a.edn`.
  It retains all four ordered pattern records and their guards and
  transitions/products, and verifies equal selected/enacted action digest
  `c67a47285abce5b67df6e08d948be2b56a6a29883c316c583ed671518cb41d87`.
  The author job `invoke-1791552084371-607-eeb17721` received the first enacted
  step `:ukrns/reader-run-path` and returned futon7 commit
  `ce2159c0d710ff7f42edd9140119dc8744d08941`; the independent reviewer job
  `invoke-1791552309089-611-d0691474` received the same construction and
  approved the bounded artifact.  The run subsequently stopped on its
  truthful reviewer-falsifier refusal, so it supplies no successful close or
  evidence that all four selected patterns were used.

  This historical record is evidence about carrier continuity only.  It was
  produced during the pre-recovery period and has not been shown to satisfy
  the canonical cascade construction/admissibility contract in
  `GOverCascades.lean`.  Its four-pattern key must therefore not be cited as a
  conformant policy or as evidence that canonical cascade construction worked.
  The source trace and focused tests establish that whatever selected action
  reaches construction is rendered into both contracts; conformance of that
  selected action is a separate, earlier gate.

- 2026-10-10, item 5 evidence-retention boundary: Agency's persisted prompt
  event is deliberately truncated, so the record cannot later reproduce or
  independently verify every byte of the actual author/reviewer prompts.
  The D-task retains the complete selected action and binds the exact dispatch
  action digest, and current deterministic prompt construction demonstrably
  renders that action into both prompts; this establishes no carrier-drop
  code defect, but it is not a byte-level prompt-enactment certificate.  The
  minimum remaining specification decision is whether the selected-action
  digest plus deterministic renderer is the intended binding, or whether
  exact author and reviewer prompt bytes (or their digests) must be retained
  and joined to the returned artifact and close receipt.  No prompt-binding
  semantics or whole refinement loop is invented here; item 5 remains open
  beyond this first dispatch audit.

- 2026-10-10, canonical cascade population audit: a complete `PolicyKey` is
  only the stable identity `(target, ordered pattern ids, semilattice)`; its
  hash does not establish that the identified value is a canonical cascade.
  `GOverCascades.lean` instead aliases the policy to `CascadeEFE.Policy`: the
  carrier contains actual pattern nodes, guarded transition interpretations,
  typed edges and precedence, while `CascadeEFEPolicies.CandidateFamily`
  separately requires a nonempty duplicate-free family, admissible order, one
  common predictive model/preference horizon, and a successful canonical
  score for every member.  Runtime correspondence additionally needs pinned
  pattern/interpretation authority, witnessed construction relations,
  observation locators and acceptance/admission evidence.  The Lean module
  explicitly leaves `CascadeGrainSeam.owed`; therefore no theorem currently
  says that a runtime map or a `PolicyKey` inhabits that carrier.

- 2026-10-10, first missing conformance check before G: current main has real
  checks, but no single canonical admission.  `cascade-sources/load-declared`
  canonicalises namespaced pattern ids and verifies document-backed
  interpretation source bytes; `interpretation-construction` requires typed
  guards/effects, observed tokens, receipts and a supported order;
  `admit-cascade-problem` rejects empty orders, absent interpretations and
  receipts, unwitnessed support/meet/precedence relations, and candidates
  making no horizon progress.  After those checks,
  `wm.cascade-decision/cascade-decision-admitted` constructs plain candidate
  maps and calls `efe/rank-actions`.  Only *after scoring and selection* does
  it emit `candidate-derivations/derivations`; the existing
  `cascade-equivalence/admissible-provenance?` check and the stricter
  `construction-receipt-lean-adapter` admission are thus certificate/audit
  consumers, not a pre-G population gate.  This is the first missing check
  that allowed generated all-target identities to be treated as policies.
  The former 5,413 rows are retained only as a falsifier of that seam, never
  as positive cascade-policy evidence.

- 2026-10-10, canonical-gate disposition: no bounded wiring repair is
  justified from the existing definitions.  The available validators prove
  different pieces and accept different carriers: source loading proves byte
  identity, construction admission proves internal relation witnesses,
  cascade equivalence normalises interpreted effects and rejects three
  forbidden provenance kinds, and the Lean adapter checks a selected recorded
  candidate after the fact.  None validates, before G, the full conjunction
  of actual library membership, interpretation authority, construction
  admissibility, predictive-model correspondence, and CandidateFamily score
  correspondence; indeed the latter includes successful scoring and cannot
  itself be used as a pre-score shape check.  Wiring any one fragment as
  “canonical” would create the facade forbidden by
  `problems/g-over-cascade-is-undefined`.  The minimal specification/build
  decision is to define the runtime-to-`CascadeEFE.Policy` projection and its
  evidence type, separate the pre-score admissibility portion from the
  post-score `CandidateFamily.scored` proof, and designate the composition
  root that must refuse with the missing evidence before invoking G.  No
  validator or runtime refusal was invented in this audit.

- 2026-10-10, corrected category-theoretic carrier audit: the preceding
  diagnosis was too broad.  The runtime-to-policy projection is **not wholly
  undefined**.  `ConstructionReceipt.lean` already defines a decidable,
  executable pre-score receipt for the token-order projection, and current
  Clojure emits and gates that carrier in `relation-witnesses` and
  `admit-cascade-problem`.  `ThreeHalvesBlend.lean` additionally defines the
  richer 3/2-blend carrier and its coverage/consistency laws, but current
  Clojure does not serialize that carrier.  The exact correspondence is:

  | Canonical pre-G obligation | Lean declaration | Receipt/runtime field | Current producer or validator | Status |
  |---|---|---|---|---|
  | Pattern occurrence semantics | `ConstructionReceipt.UnitSemantics` (`produces`, `needs`) | interpreted pattern `:produces`, `[:guard :needs]` | `interpretation-construction`, `construction-receipt-lean-adapter` | Implemented and emitted |
  | Exact token-supported edge | `EdgeWitness`, `edgeValid`, `tokenRelated` | `[:relations :support :relations]` | `construction/relation-witnesses`; `machine-construction-relations-valid?` | Implemented and pre-G gated |
  | Common-origin meet with connected paths | `MeetWitness`, `pathValid`, `meetValid`; `CascadeOrder.IsMeet` direction | `[:relations :meet :relations]` | same producer/validator and Lean adapter | Implemented and pre-G gated after the direction repair below |
  | Precedence equals support and order is a clean linear extension | `Receipt`, `valid` | `:precedence`, `:linear-extension`, `:violations` | same producer/validator | Implemented and pre-G gated |
  | Per-occurrence blend objects and four partial maps | `ThreeHalvesBlend.Theory`, `PMap`, `Square` (`G`, `I₁`, `I₂`, `B`) | none | none | Absent |
  | Auxiliary-leg meaning, square commutation, consistency | `Square.commutes`, `Cone.consistent`, `inconsistencyWitness` | none | none | Absent |
  | 3/2-pushout coverage | `Cone.isPushout`, `pushout_iff_coverage` | none | none | Absent |
  | Adjacent-square pasting | `pastedDiamond`, `prop8` (requires commutation/well-formed maps) | none | none | Absent |
  | Shared-object gluing and construction/application orders | `Gluing`, `Policy`, `Policy.wellFormed`, `Policy.components` | only the unrelated token order/`:precedence` exists | none for blend squares/gluings or separate `applicationOrder` | Absent |
  | Predictive interpretation consumed by G | `CascadeEFE.Model.interpretation`, `policyKernel` | admitted interpretation receipts and model identity | source/admission checks plus scoring model | Present as runtime data; no theorem bridges a `ThreeHalvesBlend.Policy` to the transition kernel |
  | Successful score for every family member | `CascadeEFEPolicies.CandidateFamily.scored` | scoring entries | post-construction scoring | Post-score by definition; not a pre-G gate |

  The narrowest already-defined receipt for the structural/token-order part
  of a pre-score `CascadeEFE.Policy` is therefore
  `ConstructionReceipt.valid`, supplied with the exact interpreted
  `UnitSemantics`; it is not by itself a full 3/2-blend/canonical-policy
  admission.  The Clojure receipt is the same carrier through the pinned Lean
  adapter, not merely a namesake.  The executable witness exposed one real
  mismatch: after the Lean common-origin ruling, Clojure still emitted the
  superseded greatest-common-descendant and paths from operands to that
  descendant.  The previously checked-in
  `ConstructionReceiptRuntimeWitness.lean` consequently failed
  `native_decide`.  The producer, pre-G validator, fixture, and adversarial
  regression now use a closest common origin with paths from the meet to both
  operands; a structurally complete old-direction receipt is refused before
  scoring.

- 2026-10-10, narrowed remaining bridge: no honest full canonical gate can
  yet be wired.  The missing runtime fields are the per-square pinned source
  objects `G/I₁/I₂/B`, all four partial maps, auxiliary flags, gluing/shared
  objects, consistency and pushout-coverage verdicts, pasting evidence, and a
  separate application order.  The missing theorem/build step is the
  adequacy projection from a validated `ThreeHalvesBlend.Policy` to the
  guarded transition carrier and predictive interpretation consumed by
  `CascadeEFE.Policy`.  `CandidateFamily.scored` remains deliberately
  post-score.  Thus defect 7's “wholly owed” wording is retracted, while its
  refusal to bless the historical generated population remains correct.

- 2026-10-10, first additive 3/2 runtime-certificate slice: one occurrence
  can now carry an explicit `:wm/three-halves-square-v1` reading in its pinned
  interpretation receipt.  It records named finite theories and source pins
  for `G/I1/I2/B`, the four partial-map graphs, and both auxiliary flags.
  `futon2.aif.three-halves-square/validate` checks theory/map well-formedness,
  required commutation, and `Cone.consistent`'s functional active-route join;
  `admit-cascade-problem` refuses a missing or invalid occurrence before G as
  `:three-halves-square-invalid` and retains the per-pattern evidence gaps.
  The positive fixture is the v3-documented
  `ukrns/publication-cadence` reading (one publication address, living input,
  frozen input, both discoverable as the blend), pinned to the real library
  bytes.  Its Nat codes have an exact `:names` table and are therefore not
  anonymous substitutes for the reading.

  Lean's additive `ThreeHalvesRuntimeCertificate.Receipt` projects directly
  to the existing `ThreeHalvesBlend.Square`; `Receipt.valid` checks the same
  pins, four hom-set memberships, required commutation, and consistency, and
  `valid_projects_consistent` exposes the bridge.  The generated
  `ThreeHalvesRuntimeWitness.publicationCadence_valid` elaborates from the
  same fixture.  Adversarial tests reject a reversed map, a well-formed but
  noncommuting pair of routes, inconsistent active routes, and an absent
  square at the composition root.

  This slice does **not** assert `Cone.isPushout`, coverage, gluings,
  components, pasting, application-order evidence, or blend-to-transition
  adequacy.  Those remain subsequent additive slices; consequently this is
  not yet full `CascadeEFE.Policy` conformance.

- 2026-10-10, item 5 stuck/refinement audit: the formal transition model does
  not derive a new pattern when a cascade is blocked.
  `CascadeTransition.cascadeKernel_of_noEnabled` makes a no-enabled state an
  identity transition, and `R15StrategicTarget.outcomeClass` classifies no
  reached target as stop-the-line.  The library patterns likewise distinguish
  warranted behavior rather than supplying an automatic selector:
  `process-coherence/stuck-means-signal` requires alert-and-stop instead of an
  unchanged retry; `futon-theory/stop-the-line` permits observation and
  verification while blocking production change; and
  `cascade-construction/order-by-what-each-step-needs` says an unmet guard must
  become an explicit check candidate.  The older `M-wm-policies` Track 3
  `:acquire-patterns` implementation belongs to the legacy portfolio/flat-field
  selector and proposes a cascade; it is not the task-local post-selection
  enactment loop and is not evidence that the current selected cascade refines
  itself.

  | Post-selection boundary | Current main behavior | Pattern-guided refinement? |
  |---|---|---|
  | Construction/fold refusal | Throws typed `:construction-failed`; close records and parks an R16 repair obligation. | No library retrieval or cascade revision. |
  | No enabled transition | Predictive kernel holds the state fixed; initial admission rejects known no-progress candidates, but there is no distinct mid-run reconsideration hook after selection. | No. A later failure becomes a repair obligation. |
  | Author failure | A known artifact-free Agency infrastructure failure may retry the same author contract once; other failures close to R16. | No; retry is not a pattern consultation. |
  | Reviewer `REQUEST_CHANGES`/`REJECT` with findings | With the default one revision round, `run-revision-round` invokes `cascade-revision-producer`: it pins the original selected action and whole mission, asks for an additional interpretation, validates it through `want-interpretation`, reconstructs and admits an executable candidate, and sends the revised cascade and acceptance contract to author and reviewer. | **Yes, provisionally and only at this boundary.** The original and revised identities are retained. Selection among proposals uses recorded pattern-evidence prior, not cascade-grain G. |
  | Reviewer-falsifier failure | Stops, closes the failed attempt, and records/parks a repair obligation. | No second refinement pass. |
  | Grounding failure or grounded no-change | Stops and records/parks a repair obligation carrying the selected entry and failure evidence. | No pattern retrieval or revised policy comparison. |
  | Close failure | The close fallback writes a typed failure close and durable machine-failure repair obligation. | No refinement; this is containment and later repair work. |

- 2026-10-10, item 5 refinement disposition: an existing bounded
  blocker-responsive path is already wired, not bypassed, but it does not
  satisfy the requested general refinement law.  It fires only on a negative
  reviewer verdict with textual findings and remaining revision budget; its
  revision is explicitly provisional, and proposal choice is by
  `pattern-evidence-prior`.  Generalising it to the other boundaries would
  invent trigger, authority, and selection semantics, while calling it a
  canonical refinement would conflict with the still-owed
  runtime-to-`CascadeEFE.Policy` projection.  The additive specification
  needed for item 5 must define: the typed observation that opens refinement
  at each boundary; which current-cascade state and whole-mission evidence are
  authoritative; how retrieved/new patterns become admissible candidate
  revisions; cascade-grain G and prefix-F comparison over original plus
  revisions; a stop condition and repair obligation when none is warranted;
  and the selected-revised-to-enacted identity/close receipt.  No production
  path was broadened in this audit.

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
