# WM-LEAN-ABSENT-TRIAGE-D: the 21 registry edges with no Lean import behind them

claude-11, 2026-09-26. Read-only. This triages the 10 `present` and 11 `absent` rows of
WM-LEAN-VS-REGISTRY-I (p4ng 33d9a72, outputs e22c72b), which compared futon2
`aif-equations.edn` at 9c8b3f33 against mathlib4 759b8ca884. Each row gets one
class, with evidence from the consumer's Lean declaration and the registry row.
Line numbers refer to `mathlib4/DarkTower/WarMachine/` at 759b8ca884.

Counts, one per edge:

| class | edges |
|---|---|
| instantiation | 10 |
| grain | 5 |
| name-mismatch | 3 |
| not-written | 3 |

Where an edge carries two terms of different classes, the row names both and
the edge is counted under the class that needs more work.

## Does the model's own convention agree with "the instantiation is the missing equation"?

Yes. The model states each equation parametrically. `S O U` are `variable`
carriers, and the terms are binders: `softmaxWithFPi … (tau : ℝ)`,
`exactBeliefAt … (u : ℕ → U) (o : ℕ → O)`. The machine's own value of a term is a
separate `machine*` declaration: `MachineTemperature.machineTemperature`,
`MachinePolicySet.machinePolicySet`. Nothing yet applies the one to the other.
Writing `softmaxWithFPi … machineTemperature …` in a module is therefore new
content, not a restatement. Three instantiations cannot be a bare substitution:

- **`machineTemperature` returns `Except TemperatureError ℝ`**
  (MachineTemperature.lean:42). The instantiation has to carry the error arm as
  a typed absence, as the standing rulings require. It cannot supply a default
  τ.
- **`IsBayesAction` is a `Prop`** (ActionMarginal.lean:26). Feeding `u` into
  `exactBeliefAt` needs a chosen maximiser. The existence theorem at :56
  (`∃ u, IsBayesAction step Q u`) gives one, but only non-constructively.
- **`sensoryPredictionError` takes `o : ObservationVector`**, the channel vector
  from `Holes` (SensoryPredictionError.lean:27-28). `observationAfter`'s `o : O`
  is the token observation (ObservationProcess.lean:34). The two are different
  carriers, so R2→R3a for the sensory error needs a stated map between them
  before any instantiation. See W6.

## The 21 rows

| # | edge | term(s) | consumer declaration (Lean) | class | evidence |
|---|---|---|---|---|---|
| 1 | R1→R3a | mu | `SensoryPredictionError.sensoryPredictionError (g) (o : ObservationVector) (μ : M)` :27 | instantiation | `μ` is a free binder. The belief `exactBeliefAt` is never passed in. |
| 2 | R14→R6 | tau | `PolicyPosterior.softmaxWithFPi … (tau : ℝ) … (htau : 0 < tau)` :15-19 | instantiation | `PolicyPosterior` imports only `Holes`, and nothing places `machineTemperature` at this binder. Same module as row 10. |
| 3 | R16→R1 | u | `ExactBeliefTrajectory.exactBeliefAt … (u : ℕ → U) (o : ℕ → O)` :177-178 | instantiation | `u` is a free action sequence. The registry says it is the executed `argmax` of R16. |
| 4 | R16→R2 | u, world | `ObservationProcess.observationAfter (M) (sPrev : S) (u : U) (o : O)` :34 | instantiation | `u` is a free binder. `world` enters as `sPrev`, "the world's own state s_{t-1}" per the row's `:formal`; that part is a grain note (`:enters-through :sPrev`). |
| 5 | R2→R1 | o | `exactBeliefAt … (o : ℕ → O)` :178 | instantiation | `o` is a free observation sequence. The registry's wire is that `o` is drawn from `observationAfter`, so the instantiation is a coupling (the trajectory under the process), not a substitution. Same module as 3, 6 and 4's `u`. |
| 6 | R2→R3 | o | `ExactBeliefTrajectory.exactUpdate (A) (B) (o : O) (sPrev)` :56 | instantiation | As row 5, one tick. |
| 7 | R2→R3a | o | `sensoryPredictionError (o : ObservationVector)`; `StatePredictionError.statePredictionError (A) (B B') (o : O) …` :60-61 | instantiation | The state error's `o : O` instantiates directly. The sensory error's `o` is a different carrier (see above) and needs W6 first. |
| 8 | R4→R3a | A, B | `statePredictionError (A : S → O → ℝ) (B B' : S → S → ℝ)` :60 | instantiation | Free binders. The forward model's `M.A`/`M.B` (PolicyRollout.lean:23-33) is never passed in. |
| 9 | R6→R16 | Q-pi, pi | `ActionMarginal.IsBayesAction (u : U)`, with section variables `(step : Policy → U) (Q : Policy → ℝ)` :16-17, :26 | instantiation | `Q` is the posterior, left free. `pi` enters as `step`, the policy's action at t (a name difference noted on the row). |
| 10 | R8→R6 | F-pi | `softmaxWithFPi … (fPi : PolicyIndex → ℝ)` :17 | instantiation | `fPi` is free. `PolicyVariationalFreeEnergy.variationalFreeEnergy` is never passed in. Same module as row 2. |
| 11 | R1→R17 | mu | `DirichletLearning.accumulate (a : DirichletParams O S) (t : Trial O S)` | grain | `Trial O S := List ((O → ℝ) × (S → ℝ))` (:34). The row's `:formal` sums `o_tau ⊗ s_tau`, and `mu` enters as the second component of each trial entry. The registry's `:imports [:o :mu :a-conc]` is finer than the one `t` binder. |
| 12 | R1→R4 | mu | `PolicyRollout.predictedOutcome (M : ForwardModel S O U) (π) (n) (o)` | grain | The rollout starts from `M.q₀` (PolicyRollout.lean:31-33), a field of the model. The row's `:formal` says it is "rolled forward by B from mu", but no declaration puts the current belief into `q₀`. |
| 13 | R13→R4 | T | `predictedOutcome … (n : ℕ)` | name-mismatch | `n` is the rollout depth, which the row calls T ("over depth T"). |
| 14 | R16→R17 | u | `Holes.PolicyPriorKernel PolicyIndex := ProbabilityKernel Unit PolicyIndex` (Holes.lean:7102) | not-written | The registry's `:lean` names a type, not a construction, and no declaration builds `E` from enactment records. Separately, the import `:u` is wrong (see "Does E need u?" below). |
| 15 | R2→R17 | o | `accumulate (t : Trial O S)` | grain | As row 11: `o` is the first component of each trial entry. |
| 16 | R2→R4 | interp | `Proof2.CoApplicationKernel.coApplyKernel (pat : ι → InterpretedPattern V) (r) (s s')` :99-100 | name-mismatch | `interp` is `pat`, typed `CascadeTransition.InterpretedPattern` (CascadeTransition.lean:22). The exogenous `:interp` row carries no `:lean`, so the generator cannot see the carrier. |
| 17 | R2→R6 | interp | `CascadeOrder.acyclicDescent (r : α → α → Prop) : Prop` :21 | not-written | `acyclicDescent` is a predicate on an arbitrary `r`. The row's `:formal` constructs `r` from the interpreted patterns ("Unit A is above unit B iff A produces a token B's guard …"), and no declaration does that. `Proof2/ChainOrder.lean` builds a chain only from a given precedence list (`listChain` :46, `listPat`), not from `consumes`/`produces`. |
| 18 | R2→R7 | o, ref-label | `TokenObservation.AdjudicationRates V` (structure: `falseNeg falsePos` with bounds, :38-42); `LikelihoodPrecision.precisionLikelihood (ζ) (A)` | not-written | The rates are a record of given numbers; nothing counts them from recorded verdicts against reference labels, which is what the row's `:formal` does. In `:likelihood-precision`, `o` enters only through the `beta_zeta` update (`(o_zeta_tau - o_tau).ln A s_tau`), and only `A_ζ` is written. |
| 19 | R2→R8 | o | `PolicyVariationalFreeEnergy.variationalFreeEnergy (lik prior q : S → ℝ) : EReal` | grain | `lik` is P(o\|s) at the observed `o`, i.e. `fun s => A s o`. The row imports `o` and `A` separately. |
| 20 | R3a→R7 | eps | `ChannelPrecision.channelPrecision (eps0) (h0) (errors : Fin (n + 1) → ℝ)` | name-mismatch | `errors` is the window of one channel's `eps` history, and `Var(eps_k)` in the row's `:formal` is taken over it. |
| 21 | R4→R8 | A | `variationalFreeEnergy (lik prior q)` | grain | As row 19: `A` enters through `lik`. |

### Does E need u? (row 14)

No. The row's `:formal` says an enactment record adds one count "to its
candidate's policy key [mission, ordered pattern ids, semilattice], and only
when the W_c verdict" is empty. The flight's code does the same:
`enactment_habit/increment` reads the selection's candidate and the attempts.
The machine's map (futon3c `holes/labs/M-wm-wiring/wm-flight-wiring.edn`,
`:r7-increment`) declares that it reads `[:attempts :candidate :wc-verdict]`.
Nowhere does it read the action marginal's `u`. What E counts is the ENACTED
POLICY, not the argmax action. The registry import `:u` should be the enacted
candidate, together with `:wc`. That symbol is not defined in the registry
today; the closest is `:pi` (the policy set, R6).

### Grain: note or finer declaration?

- **Rows 11, 15 (trial) and 19, 21 (`lik`): a registry note.** The Lean
  signature is the right grain for these equations. The Dirichlet update is
  over the whole trial, and F(π) is written against the joint likelihood. A
  note `:enters-through {:o :t, :mu :t}` / `{:o :lik, :A :lik}` states where
  the registry's finer terms go. A finer declaration would restate the same
  equation with more arguments.
- **Row 12 (`mu` into the rollout via `M.q₀`): a finer Lean declaration.** Here
  the missing link is the machine's own wire (the rollout starts from the
  current belief), and a note would hide that it is unwritten. This is packet W4.
- **Row 4's `world` (via `sPrev`): a registry note.** `world` is exogenous; no
  Lean module defines it to import.

## Packets, in Joe's order

Acceptance throughout is the generator's verdict for the edge (`bb
p4ng/empirics-futon/gen_lean_dag.bb`, `:registry-vs-lean :edges`). Two limits
of the generator bear on that acceptance:

- **It turns blue only through an import.** A registry correction by itself
  moves an edge from red to orange at best. Blue needs a module placed at the
  consumer that imports one placed at the source.
- **It does not yet read `:lean-term` or `:enters-through`.** Packet G0 is that
  change (mine), and it has to land first, or the registry corrections will
  show no movement.

A new instantiation module counts for its edge only if the registry row's
`:lean` is re-pointed to it. That re-pointing is part of each W packet: a
one-line registry edit in the same commit.

### G0: generator reads the registry's join hints (p4ng, claude-11)
- **Change:** `gen_lean_dag.bb` counts a binder named by the row's
  `:lean-term {sym "binder"}` as a parameter, and a term listed in
  `:enters-through {sym binder}` as `:term-present-not-imported` with `:how
  :enters-through`. It also resolves an exogenous symbol's `:lean` as that
  symbol's carrier.
- **Test:** a fixture edge of each kind in `test_gen_lean_dag.sh`.

### 1. Registry corrections (cheap; they shrink the red)
Each is a futon2 `aif-equations.edn` row edit. The acceptance is the edge
turning orange.

- **RC1, R3a→R7:** on `:precision`, add `:lean-term {:eps "errors"}`. Keep the
  registry's name; `errors` is the eps history, and the note should say
  "window of eps".
- **RC2, R13→R4:** on `:forward-model`, add `:lean-term {:T "n"}`. Registry
  name canonical.
- **RC3, R2→R4:** on `:co-application-kernel`, add `:lean-term {:interp
  "pat"}`; on the exogenous `:interp`, add `:lean
  "CascadeTransition.InterpretedPattern"`.
- **RC4, R2→R8 and R4→R8:** on `:policy-free-energy`, add `:enters-through {:o
  :lik :A :lik}`.
- **RC5, R1→R17 and R2→R17:** on `:dirichlet-accumulation`, add
  `:enters-through {:o :t :mu :t}`.
- **RC6, R16→R17:** on `:enactment-habit`, replace `:u` in `:imports` with the
  enacted candidate. This needs a symbol, either a new `:candidate` defined by
  the selection at R6 or `:pi`; the registry owner decides. The R16→R17 edge
  then goes away, and an R6→R17 edge takes its place.
- **Note, row 4:** on `:observe`, add `:enters-through {:world :sPrev}`.

### 2. Not-written (components)
- **C1, containment order from the patterns (R2→R6).**
  - Module: a `containmentOrder (pat : ι → InterpretedPattern V) : ι → ι →
    Prop` in `Proof2/` (new `ContainmentOrder.lean`), per the row's `:formal`
    (unit A above unit B iff A produces a token B's guard consumes).
  - Theorem: that it satisfies `acyclicDescent` under the row's stated
    conditions.
  - Registry: `:containment-order :lean` re-pointed to it.
  - Acceptance: R2→R6 becomes present, and blue once `:interp` has a carrier
    module (RC3).
- **C2, adjudication rates from records (R2→R7, `ref-label`).**
  - Module: `adjudicationRatesOf (records : List (verdict × refLabel)) :
    AdjudicationRates V` in `TokenObservation.lean` or a new
    `AdjudicationCounts.lean`, with exact rational counts per the row.
  - Acceptance: R2→R7 `ref-label` present.
- **C3, the likelihood-precision update (R2→R7, `o`).**
  - Module: `betaZetaPost` per the row's `:formal`, in
    `LikelihoodPrecision.lean`.
  - Acceptance: R2→R7 `o` present.
- **C4, the habit prior from enactment records (after RC6).**
  - Module: `habitPrior (records) : PolicyPriorKernel PolicyIndex`, counting one
    per record with an empty W_c verdict; new `EnactmentHabit.lean`.
  - Registry: `:enactment-habit :lean` re-pointed from the type to it.
  - Acceptance: the corrected edge (from the selection) is present.

### 3. Instantiation (wires)
- **W1, the machine's policy posterior (R14→R6, R8→R6; one module).**
  - Module: new `MachinePolicyPosterior.lean`, importing `PolicyPosterior`,
    `MachineTemperature` and `PolicyVariationalFreeEnergy`.
  - Content: `softmaxWithFPi habit grade (fun π => F π) τ …` where τ comes from
    `machineTemperature opts`. The `.error` arm is carried as a typed absence,
    not a default.
  - Registry: `:policy-posterior :lean` re-pointed.
  - Acceptance: R14→R6 and R8→R6 blue.
- **W2, the action from that posterior (R6→R16).**
  - Module: new `MachineActionChoice.lean`, importing `ActionMarginal` and W1.
  - Content: `IsBayesAction step (W1 posterior as Policy → ℝ)`, with `step` the
    policy's action at t.
  - Registry: `:action :lean` re-pointed.
  - Acceptance: R6→R16 blue.
- **W3, the closed-loop belief trajectory (R16→R1, R2→R1, R2→R3, R16→R2's `u`;
  one module).**
  - Module: new `ClosedLoopTrajectory.lean`, importing `ExactBeliefTrajectory`,
    `ObservationProcess` and W2.
  - Content: `exactBeliefAt` with `u t` a chosen `IsBayesAction` maximiser and
    `o` coupled to `observationAfter M sPrev (u t)`.
  - Registry: `:belief-state`, `:state-belief-update` and `:observe` `:lean`
    re-pointed, or one new row naming the coupling.
  - Acceptance: those four edges blue.
- **W4, the rollout from the current belief (R1→R4, grain row 12).**
  - Module: `forwardFromBelief (M) (μ) := {M with q₀ := μ}` (with μ's
    nonnegativity and sum as hypotheses), plus `predictedOutcome` at it. Goes
    in `PolicyRollout.lean` or a new module importing `ExactBeliefTrajectory`.
  - Acceptance: R1→R4 blue.
- **W5, the state prediction error at the model and the observed o (R4→R3a,
  R2→R3a state).**
  - Module: `statePredictionError M.A (M.B u) (M.B u') o …`, importing
    `PolicyRollout` and `ObservationProcess`.
  - Acceptance: R4→R3a blue, and the state half of R2→R3a.
- **W6, the sensory prediction error at the belief (R1→R3a, R2→R3a sensory).**
  First a stated map from the token observation `O` to `Holes.ObservationVector`
  (or the registry's decision that the sensory error's `o` is the channel
  vector, so the source is not R2's token process). Then
  `sensoryPredictionError g o (exactBeliefAt …)`. Discovery precedes build;
  this could be two packets.

## What this rests on

- **Read:** the 21 rows of `:registry-vs-lean :edges` in p4ng
  `empirics-futon/aif-lean-dag.edn` at e22c72b; each consumer declaration at the
  lines cited; the registry rows `:formal` and `:imports` at futon2 9c8b3f33;
  `ForwardModel`, `Trial`, `InterpretedPattern`, `PolicyPriorKernel`,
  `AdjudicationRates`; the ActionMarginal section variables; `Proof2/ChainOrder`
  and `CascadeCoapplication` for any construction of r from the patterns (none
  found).
- **Not done:** no Lean was compiled; no edit to the Lean model, the registry
  or the generator.
