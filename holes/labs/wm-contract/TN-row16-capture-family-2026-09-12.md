# TN — Row 16 capture family: five remaining measurement proofs

Date: 2026-09-12  
Scope: discovery only; no production, registry, census, or canonical-accounting
change  
Futon2 source inspected: `d9fddee240b389bed62b841ea9c200820b072242`  
Mathlib4 source inspected: `df87676b12dd04109ea76370223c6489f1debc43`

## 1. Method and the trace baseline

I resolved every requested target through
`holes/labs/wm-contract/aif-equations.edn` before reading the declaration.  A
declaration name is not evidence for the law attributed to it; the statement
and its input carrier below are the measurement obligation.

The current trace writer declares schema 28 at `src/futon2/aif/trace.clj:243-246`
and describes its sole schema-28 addition at `:369-372`: a complete,
present-only machine-Q Q/C pair on each evaluated ranked action.  Admission of
that pair checks completeness and ordered support at `:84-113`.  Schema 28 does
not itself add any of the inputs for F, F_pi, ambiguity, policy-set, or BMR.

Three other recent additions matter when reconstructing a tick:

* details-on retains each ranked action's predictive mean, variance, and
  variance-status at `trace.clj:115-188`;
* details-on rekeys the complete softmax distribution to stable `rank/N` keys
  at `trace.clj:225-236`;
* depth inputs/results are top-level `:horizon-steps` and
  `:policy-depth-used` at `trace.clj:647-652`; the resolved abstention option is
  emitted by `policy/select-action` at `policy.clj:870-874` and survives the
  decision compactor because only `:softmax-weights` and `:ranked-actions` are
  removed (`trace.clj:233-236`).

The newest real, details-on, version-stamped record set available during this
census is `data/wm-trace/wm-trace-2026-09-12.edn`, SHA-256
`3b25d2d43e3ebbcf19fc6f82e103ae49907b81d0cf009fa8d7884e74ef53e175`.
It contains four real records (timestamps 13:16, 14:02, 16:48, and 17:28 UTC),
all schema **27**, each with 147 or 148 complete ranked actions and the
details-on prediction triple.  Thus there is not yet a real schema-28 tick in
the retained daily trace.  The schema-28 source and packet fixtures establish
the writer shape, not that the opt-in machine-Q scorer ran on a real tick.
None of the verdicts below silently upgrades that source fact into a run fact.

## 2. R8 `:free-energy` — `variationalFreeEnergy`

### Declared law

The registry row is `aif-equations.edn:92-101`; it binds
`DarkTower.WarMachine.Holes.variationalFreeEnergy`.  The actual declaration at
`mathlib4/DarkTower/WarMachine/Holes.lean:7086-7095` returns a wrapped real

```
F = (1/2) * ((sum over Channel.all of precision(k) * error(k)^2)
             / Channel.all.length).
```

A measurement proof therefore needs, at one tick, the complete fourteen-entry
`Channel` order, every precision value, every prediction error, and the
production scalar to compare with the Lean reference.  The division is by all
fourteen declared channels, not merely the observed/error-map support.

### What is retained now

The trace retains `:prediction-errors` and `:precision-state` at
`trace.clj:615-622`, so the operands are at least partly inspectable.  That does
not create the production result.  The production function was deliberately
deleted: `free_energy.clj:7-12` records the ruling, and
`free_energy_test.clj:293-301` asserts that
`compute-variational-free-energy` does not resolve.  The registry itself says
`NO PRODUCER AT HEAD`, `:status :retired` (`aif-equations.edn:92-101`).  Trace
schema 21 removed the former scalar; schema 28 does not restore it.

Some records also have typed absent prediction-error channels.  Even if one
recomputed the formula from retained operands, that would be an analyst's new
calculation, not a readback through an actual production function and not a
measurement of a production F.

### Verdict

**`:blocked-on-other-work` — `:retired-no-production-object`.**  Additive trace
capture cannot retain a value which the machine no longer computes.  This row
must not be reopened as part of row 16.  Only a new operator decision changing
the retirement, followed by a separately reviewed producer, could make a
production measurement proof meaningful.

### Smallest later packet

No capture packet.  If the retirement is ever reversed: (A) decision and new
producer, (B) additive once-per-tick capture of the complete fourteen
precision/error operands and result, (C) measurement witness.  Row 16 owns
none of A or B today.

## 3. R8 `:policy-free-energy` — `machinePolicyFreeEnergy`

### Declared law

The registry row is `aif-equations.edn:102-111`.  Its binding resolves to
`DarkTower.WarMachine.MachinePolicyFreeEnergy.machinePolicyFreeEnergy`, not the
retired scalar above.  The actual carrier at
`MachinePolicyFreeEnergy.lean:45-63` folds a list of channel data through a
typed `Except`:

* positive effective variance contributes
  `(log (2*pi*v) + residual^2/v)/2`;
* negative variance refuses `invalidVariance`;
* declared deterministic zero contributes zero only inside tolerance,
  otherwise refuses `deterministicMismatch`;
* an **absent** zero is replaced by the declared floor only in `.floor` mode.

The production-score relation is separately stated at `:79-95`: F_pi enters
unscaled by default.  A per-tick measurement needs, for every candidate, the
previous prediction mean, prediction variance, variance-status, current
observation, action-identity join, deterministic tolerance, variance floor,
mode, per-candidate typed result, and (when consumed) the scaling arm.

### What is retained now

The retained real S4 run
`holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn`, SHA-256
`aaeccaf477dfd16bcc73064aa979f1fefa9753e2eec4ccd053e5e17acf8efdbf`,
contains four consecutive details-on records.  Each has the current complete
observation and, per ranked action, `:prediction-mean`,
`:prediction-variance`, and `:prediction-variance-status`.  Its
`:f-pi-provenance` retains the previous timestamp, effects mode, candidate
counts, match counts, and the action-type-and-target join.  Its
`:f-pi-by-candidate-id` retains every typed result.  Three ticks record complete
145-candidate coverage and applied unscaled F_pi; the second records the real
`:incomplete-coverage` refusal for one unmatched candidate.  The run README
records the exact invocation and run ids.

The production reader uses the `.floor` arm and supplies the floor/tolerance
at `scripts/futon2/report/war_machine.clj:458-564` (the call is at :545-554);
the calculation itself is `policy_free_energy.clj:41-151`.  Consequently the
S4 bytes plus the pinned source/config are sufficient.  Schema 28's machine-Q,
depth, and abstain fields are irrelevant to this horizon-one retrospective
quantity.

### Verdict

**`:provable-now-from-retained-pins`.**  Use one complete S4 tick and its
immediately preceding retained record, retain the existing incomplete-coverage
tick as a refusal control, and measure every channel term and candidate total.
Do not replace the logarithmic Lean expression with a second Clojure
calculation; follow the existing symbolic-log witness convention recorded in
the registry's `:lean-note`.

### Smallest later packet

One evidence packet: mechanically generate Lean reference statements from the
pinned S4 pair, run the actual production reader, retain per-channel and
per-candidate deltas, commission negative/zero/absent-variance controls, and
submit the canonical witness fragment.  No trace change precedes it.

## 4. R5 `:ambiguity` — `ambiguity`

### Declared law

The registry row begins at `aif-equations.edn:134`.  The actual declaration is
`DarkTower.WarMachine.Holes.ambiguity` at `Holes.lean:7137-7148`.  It is
categorical expected observation entropy:

```
sum_s predictedState.mass(pi,s) *
      [- sum_o A.mass(s,o) * log(A.mass(s,o))].
```

A measurement proof therefore needs, per policy, the complete predicted-state
support and masses and, for every reachable state, the complete observation
kernel A support and row masses.  A scalar predictive variance is not that
carrier.

### What is retained now

The real details-on records retain `:G-ambiguity`, `:ambiguity-mode`, and the
per-candidate predictive variance map (`trace.clj:157-188`).  Production builds
that value in `efe.clj:40-78,725-730`: normally it sums Gaussian differential
entropy `1/2 log(2*pi*e*max(variance,1e-9))`; the learn-action branch instead
uses one capability-zone predictive variance.  Neither branch receives the
categorical predicted-state kernel or the state-conditioned machine A required
by `Holes.ambiguity`.  The per-channel decomposition at `efe.clj:66-78` makes
the Gaussian calculation reviewable, but does not turn it into expected
categorical row entropy.

Rows 6 and 9 now provide machine A and policy-terminal state distributions for
a future canonical computation.  Schema 28 can retain their machine-Q Q/C pair,
but `compute-efe` still does not compute `Holes.ambiguity` from those objects.

### Verdict

**`:blocked-on-other-work` — `:production-carrier-disagrees-with-declared-law`.**
The retained Gaussian inputs are complete for the function the machine runs,
but they cannot witness the categorical Lean declaration.  This is not an
additive-capture gap and must not be bridged by relabelling Gaussian entropy as
kernel entropy.

### Smallest later packets

1. **A — law/implementation bridge (new Lean module; frozen `Holes.lean`).**
   State both quantities on their actual carriers and prove only the special
   cases in which they agree, if any; otherwise retain an explicit divergence.
2. Decide/build the production ambiguity estimator over row-9 state Q and the
   row-6 machine A, with the existing Gaussian lane still honestly named.
3. Add per-candidate retention of the selected estimator's complete inputs and
   typed result on ranked actions (not the once-per-tick row-13 envelope).
4. Produce the measurement witness.

## 5. R6 `:policy-set` — `machinePolicySet`

### Declared law

The registry row begins at `aif-equations.edn:158`.  The binding resolves to
`DarkTower.WarMachine.MachinePolicySet.machinePolicySet`, whose actual statement
at `MachinePolicySet.lean:19-22` is simply the extensional candidate set
represented by a ranked `List Candidate`: membership is list membership.

The same module's `selectionPolicySet` (`:24-37`) is the important refinement:
controller-head reads ranked strategic candidates, full-score posterior reads
the scored strategic list only when F_pi entered, and actuation reads scored
when any habit prior is nonzero.  A proof of **machinePolicySet alone** needs
the complete ordered candidate list and each `Candidate` projection (stable id,
score, no-op marker).  A selectionPolicySet proof additionally needs boundary,
applied law, F_pi-entered, habit-prior presence, and both ranked/scored
representations.  Neither theorem says that selected equals enacted.

### What is retained now

Every trace retains the complete compact `:ranked-actions` vector
(`trace.clj:629`) with action identity, controller score, and rank
(`trace.clj:157-171`).  The newest real details-on records named in section 1
contain 147/148 such rows.  The decision retains its selection boundary,
requested/applied law, F_pi status, habit authority and selection gain on the
strategic path; details-on retains the rank-keyed posterior.  The new depth,
machine-Q, and abstain-epsilon fields do not alter candidate-set membership.

The Lean `Candidate` is a compact projection rather than the rich Clojure action
map.  The proof must declare and test the projection (rank-derived stable id,
controller score, and whether action type is `:no-op`) instead of pretending
the carriers are byte-identical.  All projection inputs are retained.

### Verdict

**`:provable-now-from-retained-pins`.**  Generate the finite Lean list from one
pinned real details-on record and compare extensional membership through the
production trace reader.  If the evidence claim is widened to
`selectionPolicySet`, use a record whose decision carries the required branch
inputs and retain branch-specific controls; do not silently widen the simpler
registry binding.

### Smallest later packet

One evidence packet for `machinePolicySet`: pinned real record, explicit
Clojure-to-`Candidate` projection, generated Lean finite-set theorem, order and
drop controls, readback, and witness fragment.  A separate optional packet may
cover `selectionPolicySet` branch dispatch.

## 6. R17 `:model-reduction` — three laws, one registry binding

### Declared law and binding defect

The registry row is `aif-equations.edn:201-205`.  Its `:formal` text asserts all
of the following:

1. reduced posterior `A' = A + a' - a`;
2. Dirichlet-normalizer evidence change
   `Delta F = ln B(A) + ln B(a') - ln B(a) - ln B(A')`;
3. acceptance iff `Delta F <= -3`.

But its sole `:lean` binding is `bayesFactorThreshold`.  The actual Lean laws
are separate declarations in frozen `Holes.lean`:

* `modelReductionFreeEnergyChange`, `:7210-7218` — the logarithmic equation;
* `bayesFactorThreshold`, `:7220-7221` — only the threshold proposition;
* `bayesianModelReduction`, `:7231-7233` — only the componentwise A' map.

Thus the registry's bound declaration does **not** state its whole formal line.
This is a carrier-lags-registry finding: a threshold-only measurement must not
be labelled a model-reduction proof.

A complete measurement needs, per proposal, ordered full prior `a`, full
posterior `A`, reduced prior `a'`, computed `A'`, computed Delta-F, the -3
threshold, and the accept/reject result, plus the identity of the offline parent
model and proposal.

### What is retained now

Production `bmr/bayesian-model-reduction` receives the three ordered vectors,
constructs A', Delta-F and `accept?` at `bmr.clj:108-138`.  The offline envelope
records proposal A', Delta-F, threshold and decision, and crucially records its
complete replay input at `r17_offline.clj:64-101`; that shape is sufficient for
a measurement proof **when an actual envelope is retained**.

No real retained `:r17/envelope-version` record was found under `holes/` or
`data/` at this HEAD.  `runs/V7-R17-node-sim/00-r17.edn` contains three
production-function reference cases, but its own schema is
`:wm/v7-r17-node-sim-v1` and its concentrations are fixture inputs.  It is a
useful control, not a real offline run record.  Trace schema 28 is unrelated:
R17 runs offline and `r17_offline.clj` is not a tick-trace producer.

### Verdict

**`:needs-additive-capture` — after a binding-completeness packet.**  The
production envelope already has the right fields, so the one producer behavior
is to retain one real `r17-offline/run` envelope, including its replay input and
source/model pins, at the boundary which commissioned it.  Before admission,
the formal subject must cover all three laws; the current threshold-only
registry binding cannot support the advertised proof.

### Smallest later packets

1. **A — composite formal subject (new Lean module; frozen `Holes.lean`).**
   Define a theorem/record that explicitly composes
   `bayesianModelReduction`, `modelReductionFreeEnergyChange`, and
   `bayesFactorThreshold`; propose the registry-binding repair separately under
   registry ownership.
2. Retain one real offline envelope, byte-pinned with parent-model and corpus
   provenance.  Commission replay mutation, vector-order/cardinality, and
   threshold controls.
3. Generate exact A' equations and symbolic-log Delta-F references from those
   pins, run the production replay reader, record numerical deltas, and submit
   the witness fragment.

## 7. Dependency-ordered row-16 split

1. R6 policy-set measurement proof — provable from an existing real details-on
   trace.
2. R8 policy-F measurement proof — provable from the existing S4 consecutive
   records.
3. R17-A composite formal subject — required before a whole-row model-reduction
   claim.
4. R17 real-envelope retention — one offline producer behavior, not a
   `trace.clj` change.
5. R17 measurement proof.
6. R5-A explicit categorical/Gaussian bridge or divergence statement.
7. R5 production estimator decision/build, then per-ranked-action capture, then
   measurement proof.

The retired variational-F scalar has no row-16 packet.  Reinstating it would be
a new operator decision and production build, not capture-family completion.

## 8. Verdicts at a glance

* R8 free-energy / `variationalFreeEnergy` —
  `:blocked-on-other-work :retired-no-production-object`.
* R8 policy-F / `machinePolicyFreeEnergy` —
  `:provable-now-from-retained-pins` (real S4 consecutive details-on records).
* R5 ambiguity / `ambiguity` —
  `:blocked-on-other-work :production-carrier-disagrees-with-declared-law`.
* R6 policy-set / `machinePolicySet` —
  `:provable-now-from-retained-pins` (complete real ranked list plus explicit
  carrier projection).
* R17 model-reduction — `:needs-additive-capture` after packet A repairs the
  threshold-only formal binding; retain one real offline replay envelope.
