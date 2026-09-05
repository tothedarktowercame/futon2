# Per-node problem dossiers — batch 5: R15, R11, R17

**Scope and numbering.** This is discovery, not a change to the War Machine.
`R15`, `R11`, and `R17` below use
`p4ng/empirics-futon/control-stages.edn` numbering.  All three are aligned
between contract and catalogue (`p4ng/R-concordance.md:32-38`).  Catalogue
`R17′`, `R17″`, and `R17‴` are native extensions with no contract requirement
(`p4ng/R-concordance.md:42`); claims about them are labelled separately and
are not silently attributed to control-stage R17.

## R15 — Hierarchy and timescale

### The problem it solves

R15 solves how a fast tactical loop and a slower strategic loop become a
hierarchy rather than two unrelated scorers: the roster places **“Hierarchy
and timescale”** in SELECT (`p4ng/empirics-futon/control-stages.edn:26`;
control-stage numbering).  The operational criterion requires a two-level
generative model in which the upper level supplies a prior to the lower and
belief propagates across levels (`docs/futon-aif-completeness.md:434-439`;
contract R15).  The catalogue requires strategic selection to fix the tactical
target and witnessed tactical outcomes to update the next strategic state,
while keeping the temporal discount distinct from commitment temperature
(`p4ng/sec-catalog.tex:245`; catalogue R15).  The equation registry records no
R15 equation: it observes one flat level, names deep temporal models as the
alternative, and classifies R15 as plumbing
(`holes/labs/wm-contract/aif-equations.edn:325-328,472-473`; control-stage
R15).  R15 therefore solves explicit cross-timescale conditioning: separate
the quantities at each level, carry witnessed results upward, and let the
updated slow state parameterise the next fast policy.

### Residual problems it does not solve

- `:implementation-flaw` — The operational record says R15 remains partial:
  the K=3 horizon and cascade grain hierarchy do not propagate beliefs through
  nested generative models (`docs/futon-aif-completeness.md:434-439`).  The
  equation census independently observes **“single-level”** and says R15 runs
  no equation (`holes/labs/wm-contract/aif-equations.edn:325-328`), so the
  declared cross-level inference is absent from the implementation.
- `:undetermined` — Campaign S records a positive two-tick coupling witness,
  including a Beta(1,1) to Beta(2,1) slow-state update, but explicitly limits
  that evidence to coupling rather than a general nested generative model
  (`p4ng/sec-catalog.tex:245`).  The source establishes one concrete instance,
  not whether the broader operational criterion is satisfied.
- `:both` — The policy-grain mission says compliance requires an explicit
  hierarchy and still lists the dark end-to-end hierarchy shadow as unfinished
  (`holes/missions/M-wm-aif-policy-grain-compliance.md:35,127-166,236`).  The
  model must state the relationship among strategic target, tactical rollout,
  and nested belief, and the runtime must exercise that relationship end to
  end; both declaration and implementation remain incomplete.

**Would we know? — missing at nested-belief agreement.** The Campaign S
two-tick witness cited at `p4ng/sec-catalog.tex:245` checks one slow-to-fast
coupling, while `clojure -M:test -n futon2.aif.rollout-test` checks the flat
temporal rollout.  Missing is a check that an upper-level posterior becomes a
lower-level prior and that lower-level evidence updates that same upper model;
the nearest explicit criterion is `docs/futon-aif-completeness.md:434-439`.
Absence search: `rg -n "nested|upper.level|lower.level|deep temporal|strategic.*prior|tactical.*posterior"
/home/joe/code/futon2/src/futon2/aif /home/joe/code/futon2/test/futon2/aif
/home/joe/code/futon2/scripts/futon2/report`.

## R11 — Hierarchical shared budget

### The problem it solves

R11 solves how locally factored proposals share a finite resource without
silently oversubscribing it: the roster places **“Hierarchical shared budget”**
in SELECT (`p4ng/empirics-futon/control-stages.edn:27`; control-stage
numbering).  The operational criterion says that when multiple AIF agents act
on shared state, a coordination layer must make their actions compose
coherently (`docs/futon-aif-completeness.md:280-284`; contract R11).  The
catalogue makes the resource invariant concrete: local agents propose inside
sub-budgets and a coordinator arbitrates the shared consumable budget at every
level (`p4ng/sec-catalog.tex:243`; catalogue R11).  The equation registry
contains no R11 equation and classifies it as plumbing
(`holes/labs/wm-contract/aif-equations.edn:472-473`; control-stage R11).  R11
therefore solves globally feasible selection without erasing local factoring:
compare cheap local and expensive strategic moves on one budget, refuse
over-subscription, and record the arbitration boundary.

### Residual problems it does not solve

- `:undetermined` — The operational criterion still says R11 is N/A because
  the WM is a single observer and the cyberants and VSATARCS surfaces do not
  coordinate writes (`docs/futon-aif-completeness.md:280-284`), whereas the
  catalogue records an exact-replaying Campaign S arbiter that spent all seven
  units without exceeding the budget (`p4ng/sec-catalog.tex:243`).  Without a
  source-authority join, the current scope and satisfaction status disagree.
- `:undetermined` — The factoring census lists R11 among the three unmapped
  mechanisms because no war-room failure forced a ruling about budgeted
  selection (`p4ng/empirics-futon/factoring-table.tex:32-36`).  This is a thin
  evidence base by design: it does not show the budget mechanism wrong, but it
  leaves its governing operational constraint unadjudicated.
- `:implementation-flaw` — The operational document's re-evaluation trigger is
  a second writer over the WM substrate, yet it records no shared belief state
  or coordination between the named agent populations
  (`docs/futon-aif-completeness.md:282-284`).  Thus the general multi-agent
  composition problem remains unimplemented even if the narrower Campaign S
  ranked-field budget arbiter is accepted.

**Would we know? — existing budget checks, missing scope agreement.**
`clojure -M:test -n futon2.aif.hierarchical-budget-test` and `clojure -M:test
-n futon2.aif.hierarchical-budget-adapter-test` exercise over-subscription,
every-level charging, malformed-input refusal, the live selection boundary,
and exact replay.  Missing is a check joining those ranked fields to the
contract's multiple-agent/shared-state criterion and its VSATARCS writer
trigger; the disagreement is visible at
`docs/futon-aif-completeness.md:280-284` and `p4ng/sec-catalog.tex:243`.
Absence search: `rg -n "hierarchical-budget|shared budget|VSATARCS|cyberants|shared state|writer capability"
/home/joe/code/futon2/src /home/joe/code/futon2/test
/home/joe/code/futon2/scripts`.

## R17 — Structure learning

### The problem it solves

R17 solves changing the model's structure rather than merely sharpening fixed
parameters: the roster places **“Structure learning”** in SELECT's assurance
band (`p4ng/empirics-futon/control-stages.edn:42`; control-stage numbering).
The operational criterion requires a slow loop that proposes structural
additions and accepts them by expected model-evidence gain, recording the
result as a morphogenesis event
(`docs/futon-aif-completeness.md:462-467`; contract R17).  The catalogue's base
R17 instead states Bayesian Model Reduction: accumulated counts are rescored
under a simpler model, and a merge or prune is accepted only at a declared
evidence threshold (`p4ng/sec-catalog.tex:340`; catalogue R17).  The equation
registry declares both Dirichlet accumulation `a-conc` and model-evidence
change `Delta-F`, with offline BMR implementing the latter
(`holes/labs/wm-contract/aif-equations.edn:162-173`; control-stage R17).  R17
therefore solves evidence-governed structural change: proposals may generate
candidate additions or reductions, but recorded model evidence—not likeness or
the proposer itself—must decide acceptance.

### Residual problems it does not solve

- `:implementation-flaw` — Both R17 quantities resolve in the Box-5 Lean join:
  `a-conc` names `DirichletConcentrations` and `Delta-F` names
  `bayesFactorThreshold` (`p4ng/sec-lean-state-generated.tex:29-30`).  But the
  equation registry marks the concentration carrier only and the declared
  tick-model accumulation unrealised: runtime A4a counts capability × mission
  substrate records instead of observed `o` and belief `mu`
  (`holes/labs/wm-contract/aif-equations.edn:162-168,465-467`).  The defect is
  runtime/formal agreement, not absence of Lean names.
- `:implementation-flaw` — The registry observes offline-BMR-only learning and
  says live `F` is not its input
  (`holes/labs/wm-contract/aif-equations.edn:329-332`).  The operational
  criterion instead asks for model-structure additions accepted by `Delta-F ∧
  Delta-G` (`docs/futon-aif-completeness.md:462-467`), so the current WM path
  does not implement the criterion it is meant to discharge.
- `:undetermined` — Campaign S accepted reductions that collapsed all seven
  capability concepts into one; the catalogue says the mechanism is
  run-evidenced while the usefulness of that reduction remains an
  operator-facing question (`p4ng/sec-catalog.tex:340`).  Exact replay proves
  determinism, not that the learned structure improves the model.
- `:aif-extension-needed` — Catalogue R17′/R17″/R17‴ separately supply an
  uncertainty control, a proposal generator, and evidence-bearing vocabulary
  expansion (`p4ng/sec-catalog.tex:342-346`), while the concordance says these
  have no contract requirements (`p4ng/R-concordance.md:42`).  If those
  mechanisms are to count toward the operational criterion's structural
  additions, the contract needs explicit criteria for them rather than folding
  native extensions into base R17 implicitly.

**Would we know? — component checks exist; agreement and usefulness checks are
missing.** `clojure -M:test -n futon2.aif.bmr-test`, `clojure -M:test -n
futon2.aif.a4a-test`, and `clojure -M:test -n futon2.aif.r17-offline-test`
check the threshold calculation, concentration pipeline, accepted/rejected
envelopes, parent identity, and exact replay.  Missing is a test that feeds the
tick model's `o` and `mu` into the declared `a-conc`, relates the runtime
`Delta-F` to its Lean threshold, and measures whether an accepted structural
change improves a held-out criterion; the nearest recorded mismatch is
`holes/labs/wm-contract/aif-equations.edn:162-173,465-467`.  Absence search:
`rg -n "prediction-errors|belief-state|DirichletConcentrations|bayesFactorThreshold|held.out|morphogenesis"
/home/joe/code/futon2/src/futon2/aif /home/joe/code/futon2/test/futon2/aif
/home/joe/code/mathlib4/DarkTower/WarMachine`.

## Scope limits

I read the four accepted dossier forms, the four commissioned source families
for all three nodes, the concordance, Box-5's generated Lean join, the factoring
census, the policy-grain mission, and only the implementation/test indexes
needed to identify concrete checks.  I did not run Campaign S, inspect its raw
records, adjudicate which document controls the R11 status disagreement, prove
that Campaign S's R15 coupling is a nested generative model, assess whether the
seven-to-one R17 reduction was useful, or inspect Lean beyond the generated
join and registry descriptions.  I did not run the War Machine or its tests:
the `would-we-know` commands identify existing component checks and missing
conformance checks; they are not claims that those checks passed today.
