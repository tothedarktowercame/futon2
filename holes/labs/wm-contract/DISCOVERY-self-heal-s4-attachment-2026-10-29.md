# Discovery: S4 attachment for repair-cascade choice

Date: 2026-10-29. Scope: canonical checkouts only; discovery, not a design or
implementation packet.
## 1. Least-change attachment point and available inputs

**Attach the choice in `route!`, after the final injury selection is known and
before a new transaction is prepared.** The first selection may be superseded
after a deferred transaction is restored, so the stable injury choice is the
`selection` at `src/futon2/aif/meta_self_heal_production.clj:1588-1600`.
Transaction movement/resume is then resolved at `:1601-1619`, and a new work
order is prepared at `:1620-1624`. For a new repair, score between those two
operations and seal the chosen cascade into the work order. For a resumed
repair, do not score again: read the sealed choice from the existing work order.
That preserves the coordinator's existing resume rule, under which recorded
receipts are not redispatched (`src/futon2/aif/meta_repair_coordinator.clj:
449-451`).
This is more precise than merely “between injury-selection and
coordinate-repair.” `coordinate-repair` is first called much later at
`meta_self_heal_production.clj:1694-1702`; choosing there would be after work
order preparation and binding, and the loop would reconsider the choice at
each checkpoint. The choice belongs before `prepare-repair`, with the selected
cascade becoming part of transaction identity.
Inputs present at that seam, compared with `RepairCascadeG`:

- **Injury state belief: absent.** The route has the selected active ledger
  entry and diagnosis binding (`meta_self_heal_production.clj:1560-1568,
  1631-1640`). `RepairCascadeG` instead needs a distribution over
  `realDefect | neverDefect`; before reproduction its example is half mass on
  each (`../mathlib4/DarkTower/WarMachine/RepairCascadeG.lean:71-89,258-270`).
  No conversion from ledger/diagnosis evidence to that belief exists.
- **Whether reproduction has run: absent as a repair-pattern fact.** The live
  runtime has already prepared diagnosis and independently run the acceptance
  proposal before entering `route!`
  (`src/futon2/aif/full_loop_runtime.clj:761-799`). The acceptance receipt says
  `:ran-before-repair` (`meta_self_heal_production.clj:799-818`), but there is no
  `reproduce-on-retained-inputs` phase or `RepairState.reproduced` carrier. The
  Lean guard needs that boolean (`RepairCascadeG.lean:91-107`).
- **Pattern counts: absent.** `route!`, the work order, and the coordinator have
  no counts for the fourteen repair patterns. Repair G expects one positive
  pair per pattern (`RepairCascadeG.lean:153-165`). Searches for
  `pattern-count` in both self-heal implementation files found nothing.
- **Learned C: absent at this seam.** Ordinary outer state, including its
  learned `:c`, is loaded only inside the ordinary route after META arbitration
  (`full_loop_runtime.clj:621-650`; `src/futon2/aif/outer_state.clj:112-130`).
  That C is also over selection endings, not RepairCascadeG's `MetaOutcome`.
  The META preference reader supplies an operator-authorized provisional prior,
  explicitly not empirical calibration
  (`src/futon2/aif/meta_preference_authority.clj:63-91`), and it is not read by
  `route!`. Searches for `ending-counts`, `gain-counts`, `class-preference`, and
  `preference` in the two self-heal files and `full_loop_runtime.clj` found no
  self-heal scoring input.
Thus the attachment point is structurally available, but none of RepairCascadeG's
four model inputs is already present there in its required meaning. Diagnosis
and acceptance are evidence from which the first two might be derived; they are
not yet those inputs.
## 2. One scorer for both kinds of policy?

`outer-score/outer-score` is not that function. It fixes the latent states to
`:doable/:blocked`, outcomes to ending × gain, constructs a target-selection
model, and computes its own Fisher novelty
(`src/futon2/aif/outer_score.clj:7-11,94-147,173-187`). It cannot score a repair
state/model by substitution.
The generic **stepG-equivalent** does exist:
`futon2.aif.cascade-g/step-g`. It takes caller-supplied categorical state belief
`q`, likelihood `a`, preference `c`, and comparison context; it checks common
model/outcome support and returns risk plus ambiguity without knowing the state
or outcome vocabulary (`src/futon2/aif/cascade_g.clj:50-102`). `total-g` sums a
declared schedule (`:104-125`). Therefore selection and repair models can share
this mathematical scorer without creating a second scorer, provided each has
an honest adapter producing those generic inputs.
The existing novelty wrapper is not yet generic enough for repair cascades.
`cascade-coapply-g/compare-at-horizon` adds frontier-weighted Beta novelty, but
requires co-application candidate shape, a `:class-emission` observation model,
and the class-only validation path
(`src/futon2/aif/cascade_coapply_g.clj:30-54,98-143`). Its lower scoring path
does accept a caller-supplied observation model per candidate (`:56-70`), but
the wrapper refuses another model kind (`:120-136`). The arithmetic helper
`beta-novelty` is reusable (`:14-21`); RepairCascadeG's admitted-pattern sum
needs a repair composition adapter (`RepairCascadeG.lean:153-165`), not a
second definition of risk plus ambiguity.
Search result: `outer_score.clj` contains no call to `cascade-g/step-g`; its
comment says it evaluates the finite sums directly because it lacks model
authority (`outer_score.clj:173-180`). So today there is a generic core, but no
single high-level runtime function into which both current policy record shapes
can be passed unchanged.
## 3. Cost mass as cascade length grows

**No existing futon2 repair model predicts cost outcome mass in proportion to
repair-cascade length.** RepairCascadeG's current point kernel predicts one
outcome after applying the whole cascade (`RepairCascadeG.lean:128-138`). It
can predict `tokenUse` for one particular retired-never-defect state, but that
condition depends on the final state, not step count (`:128-132`). Its novelty,
by contrast, explicitly sums each admitted pattern (`:153-165`), which is why
the file warns that novelty grows with cascade length (`:5-12`).
Futon2 records observed elapsed time and token use in the META dataset
(`src/futon2/aif/meta_outcome_dataset.clj:420-445`) and normalizes observed
values against a candidate resource envelope
(`src/futon2/aif/meta_preference_authority.clj:95-128`). Those are observation
and preference inputs, not a forward model assigning predicted cost mass per
repair step. Searches for `:elapsed-ms`, `:token-use`, `elapsedMs`, and
`tokenUse` across `futon2/src/futon2/aif` found no repair-cascade prediction;
the hits were observation/timing records and the two files just cited.
Consequently the existing model does not counter the length-growing novelty
with length-growing predicted `elapsed-ms`/`token-use` risk.
## 4. Fixed coordinator phases versus `RepairPattern`

The coordinator declares only `[:author :review :enact :retry]`
(`src/futon2/aif/meta_repair_coordinator.clj:12`); claim is an additional
coordinator-created boundary without a port receipt (`:473-500`). Mapping those
boundaries to the fourteen constructors in `RepairCascadeG.lean:21-43`:

- **claim:** no constructor. It records repair ownership/session identity; the
  injury was already recorded before scoring, as Lean's initial state also
  assumes (`RepairCascadeG.lean:88-89`). It is not `recordFinding`.
- **author:** only a partial `repairProducer`. It returns a patch/worktree and
  advances to `:authored` (`meta_repair_coordinator.clj:502-534`), but does not
  enforce that the producer rather than the checker changed.
- **review:** partial `differentSeatReviews`. Sessions are required distinct
  (`:40-68`) and the same patch must be approved (`:535-563`), but the phase
  does not require the path witness that the pattern's Lean guard represents
  (`RepairCascadeG.lean:99-101`).
- **enact:** no constructor. It activates the approved patch and establishes
  retry identity (`meta_repair_coordinator.clj:564-598`).
- **retry:** partial `retryRepairedPhase`. It enforces repair/patch/click
  attribution (`:599-615`), but queues an ordinary retry rather than proving
  that only the repaired phase was rerun.
`redBeforeRepair` exists outside the coordinator: acceptance is run red before
repair (`meta_self_heal_production.clj:799-849`). Diagnosis is a partial
`locateProducer` outside it (`:1395-1426`). Neither is selectable as a
coordinator phase.

The constructors with **no executable phase** are `recordFinding`,
`locateProducer` (only the fixed diagnosis analogue), `reproduceRetained`,
`correctRefusalIsNotDefect`, `redBeforeRepair` (only the fixed acceptance
analogue), `witnessPath`, `noOpIsObservation`, `takeInUnpredicted`,
`matchFinding`, `introducePattern`, and `retireLedger`. `repairProducer`,
`differentSeatReviews`, and `retryRepairedPhase` have only the partial fixed
phase mappings above. Therefore an arbitrary selected repair cascade cannot be
executed by today's coordinator; selection alone would not make S4 true.
## Search boundary

I read canonical `RepairCascadeG.lean`, the route/runtime/coordinator files,
`outer_score.clj`, `cascade_g.clj`, `cascade_coapply_g.clj`, the META preference
and outcome readers, and all fourteen self-heal pattern headers. I searched
canonical `futon2/src/futon2/aif` for repair cost predictions and both self-heal
implementation files for model/count/preference carriers. I did not inspect
tests, historical run records, or noncanonical worktrees.
