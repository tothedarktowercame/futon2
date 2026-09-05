# Per-node problem dossiers — batch 4: R16, R13, R14

**Scope and numbering.** This is discovery, not a change to the War Machine.
`R16`, `R13`, and `R14` below use
`p4ng/empirics-futon/control-stages.edn` numbering.  Each is aligned between
contract and catalogue (`p4ng/R-concordance.md:34-37`), so no neighbouring
concept is silently substituted for its node.

## R16 — Grounded actuation

### The problem it solves

R16 solves what makes a selected action an intervention rather than another
internal calculation: the roster places **“Grounded actuation”** in ACT
(`p4ng/empirics-futon/control-stages.edn:28-31`; control-stage numbering).  The
operational criterion requires the loop to **“act → observe”** such that the
consequence feeds the next belief, with consent and act-gate recorded
(`docs/futon-aif-completeness.md:441-460`; contract R16).  The catalogue insists
that closure requires a **“typed, ungameable witness to a substrate outside the
model”**, because replaying the model's own construction is re-observation, not
action (`p4ng/sec-catalog.tex:311`; catalogue R16).  The equation registry gives
the selected action as `u_t := argmax_u sum_pi delta(u, pi_t) Q(pi)` and notes
that enactment is the first passing act gate, not merely the recorded selection
(`holes/labs/wm-contract/aif-equations.edn:158-161`; control-stage R16).  R16
therefore solves causal closure: a warranted choice must cross a recorded gate,
change an external substrate, return independently checkable evidence, and
alter what the next tick believes.

### Residual problems it does not solve

- `:implementation-flaw` — Action `u` is runtime-inhabited but has no resolving
  Lean site: the generated join reports **“action / R16 / u / not in Lean”**
  (`p4ng/sec-lean-state-generated.tex:25-29`), while the runtime takes the first
  passing act gate and executes it (`src/futon2/aif/enact.clj:250-321`).  The act
  runs, but its agreement with the declared action equation cannot be stated on
  the proof side.
- `:implementation-flaw` — The current enactor describes itself as
  artifact-only re-observation with **“No substrate write. No outward action”**
  (`src/futon2/aif/enact.clj:12-16`), matching the catalogue's finding that the
  pinned implementation returned an in-model construction rather than an
  external effect (`p4ng/sec-catalog.tex:311`).  That is the precise facade the
  R16 problem exists to reject, so it is an implementation flaw rather than a
  missing AIF concept.
- `:undetermined` — The process-assurance census finds R16 has no `parked`
  lifecycle cell (`holes/labs/wm-contract/ALIGN-rnode-process-census.md:82-90,150-153`),
  but the governing process record explicitly leaves open whether parking is an
  obligation or only the transport's waiting discipline
  (`holes/problems/P-assured-process.md:90-118`).  Its absence is real; whether
  R16 must implement it is not yet ruled.
- `:implementation-flaw` — The same census finds no `surfaced` discharge after
  R16's otherwise recorded commission, dispatch, return, check, and record
  (`holes/labs/wm-contract/ALIGN-rnode-process-census.md:89,120-130,150-153`).
  `P-assured-process` names `surfaced` as a distinct required lifecycle stage
  and B1 bulletin as its discharge channel
  (`holes/problems/P-assured-process.md:55-65,69-82`), so the missing recorded
  discharge is an implementation gap in process assurance.

**Would we know? — missing at external-effect/formal/process agreement.**
`clojure -M:test -n futon2.aif.enact-scheduled-path-test` and `clojure -M:test
-n futon2.aif.enact-failure-test` check the runtime gate/executor path and its
failure records; the operational closure criterion is recorded at
`docs/futon-aif-completeness.md:441-460`.  Missing is one check joining a Lean
`u`, an external typed witness that affects the next belief, and a `surfaced`
handoff discharge; the nearest process census is
`holes/labs/wm-contract/ALIGN-rnode-process-census.md:89,150-153`.  Absence
search: `rg -n "defines :u|GroundedActuation|:parked|notify/discharged-at|bulletin"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src/futon2/aif`.

## R13 — Temporal policy depth

### The problem it solves

R13 solves how delayed consequences can outrank a greedy first move: the roster
places **“Temporal policy depth”** in SELECT
(`p4ng/empirics-futon/control-stages.edn:24`; control-stage numbering).  The
operational criterion requires genuine multi-step `G(pi)` and a witness where a
multi-step policy beats the one-step choice
(`docs/futon-aif-completeness.md:302-312`; contract R13).  The catalogue states
the invariant plainly: **“the thing being scored is the cascade, not its first
move”**, with later costs discounted over a horizon greater than one
(`p4ng/sec-catalog.tex:256`; catalogue R13).  The equation registry defines `T`
as the temporal policy depth bounding the sums in `Q(o|pi)` and `G`, with runtime
horizons supplied by the WM and rollout
(`holes/labs/wm-contract/aif-equations.edn:146-148`; control-stage R13).  R13
therefore solves policy adequacy across time: represent a trajectory, propagate
predictions through it, accumulate discounted value, and demonstrate that depth
can change the preferred choice.

### Residual problems it does not solve

- `:implementation-flaw` — Depth `T` is runtime-inhabited but has no resolving
  Lean site: the join reports **“depth / R13 / T / not in Lean”**
  (`p4ng/sec-lean-state-generated.tex:23-26`), while the runtime uses a default
  multi-step horizon and activates WM horizon scoring when anticipation data is
  present (`src/futon2/aif/forward_model.clj:278-303`;
  `scripts/futon2/report/war_machine.clj:6280-6284`).  The computation exists,
  but formal agreement over its horizon does not.
- `:implementation-flaw` — The operational record says the apparatus and
  two-step-beats-greedy witness pass, but live-ranking integration remains
  partial and acting on the selected policy is held for operator arming
  (`docs/futon-aif-completeness.md:308-312`).  This is incomplete use of an
  already specified policy-depth mechanism, not a need for a new AIF term.
- `:both` — Two policy grains coexist: rollout sequence policies and cascade
  semilattice policies, with their conjunction deferred to the act gate
  (`docs/futon-aif-completeness.md:309-312`).  The model must declare their
  relationship and the runtime must score/enact the declared unit consistently,
  so this residual spans specification and implementation.

**Would we know? — missing at live and formal policy-grain agreement.**
`clojure -M:test -n futon2.aif.rollout-test` checks multi-step accumulation,
selection, abstention, and the non-greedy witness described at
`docs/futon-aif-completeness.md:302-312`.  Missing is a Lean `T` agreement test
and a live trace assertion that the same declared multi-step policy grain changes
ranking and reaches actuation; the nearest scoped status is
`docs/futon-aif-completeness.md:308-312`.  Absence search: `rg -n
"defines :T|TemporalDepth|horizon-steps|policy-rollout-score|selected-policy"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src
/home/joe/code/futon2/test /home/joe/code/futon2/scripts`.

## R14 — Commitment temperature

### The problem it solves

R14 solves how operational confidence controls commitment without changing the
underlying value model: the roster places **“Commitment temperature”** in SELECT
(`p4ng/empirics-futon/control-stages.edn:25`; control-stage numbering).  The
operational criterion identifies canonical policy precision `gamma`, inferred
from realized outcomes, and records the runtime relationship
`tau_eff = tau_spread / gamma`
(`docs/futon-aif-completeness.md:407-432`; contract R14).  The catalogue requires
an explicit dial coupled to pressure signals so the system neither thrashes nor
tunnels, preserving the invariant that **“exploration is a dial, not a random
number generator buried in the selector”**
(`p4ng/sec-catalog.tex:241`; catalogue R14).  The equation registry defines
`tau` as commitment temperature, the inverse of policy-selection precision,
and points to the effective-temperature runtime
(`holes/labs/wm-contract/aif-equations.edn:149-151`; control-stage R14).  R14
therefore solves calibrated decisiveness: outcome-derived confidence and
current pressure should sharpen or relax selection through an explicit,
inspectable quantity.

### Residual problems it does not solve

- `:implementation-flaw` — Temperature `tau` is runtime-inhabited but has no
  resolving Lean site: the join reports **“temperature / R14 / tau / not in
  Lean”** (`p4ng/sec-lean-state-generated.tex:24-27`), while
  `effective-temperature` computes it in runtime
  (`src/futon2/aif/policy.clj:77-95`).  The dial exists computationally but lacks
  a proof-side agreement target.
- `:implementation-flaw` — The catalogue's implementation note says the
  temperature and habit-adjusted ordering are computed, but the live path takes
  the head of the controller ordering, leaving the dial counterfactual and the
  WR-27 badge false (`p4ng/sec-catalog.tex:241`).  A temperature that changes no
  enacted selection does not solve the commitment-control problem.
- `:undetermined` — The operational record separately reports outcome-updated
  `gamma` as live and demonstrably different from its prior
  (`docs/futon-aif-completeness.md:411-432`), while the catalogue reports the
  `tau`/habit ordering as counterfactual (`p4ng/sec-catalog.tex:241`).  These
  claims may describe distinct selection seams; without a joined authority
  trace, whether R14 as a whole controls enactment is undetermined rather than
  resolved by choosing one source.

**Would we know? — missing at enacted-choice/formal agreement.** `clojure
-M:test -n futon2.aif.policy-precision-test` and `clojure -M:test -n
futon2.aif.policy-test` check outcome-folded `gamma`, effective temperature,
and selection calculations; the operational witness is summarized at
`docs/futon-aif-completeness.md:407-432`.  Missing is a Lean `tau` agreement
test plus a live ablation showing the temperature changes the enacted choice at
the authoritative selector, rather than only a recorded counterfactual order;
the nearest contradiction is `p4ng/sec-catalog.tex:241`.  Absence search: `rg
-n "defines :tau|CommitmentTemperature|effective-temperature|habit-authority|counterfactual-only"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src
/home/joe/code/futon2/test /home/joe/code/futon2/scripts`.

## Scope limits

I read the accepted dossier form, the four commissioned source families for all
three nodes, the concordance, the Box-5 generated join, the R16 process census
and governing process record, and only the runtime sites needed to establish
whether `u`, `T`, and `tau` are inhabited.  I did not inspect live trace data,
execute an actuation, adjudicate the R16 parking question, trace every selector
seam, or read Lean files beyond the generated join's index result.  I did not
run the War Machine or its tests: the `would-we-know` commands identify existing
component checks and missing conformance checks; they are not claims that those
checks passed today.
