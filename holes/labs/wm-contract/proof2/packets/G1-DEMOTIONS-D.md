# G1-DEMOTIONS-D — what closes each edge the declaration rule demoted (DISCOVERY)

PROOF-2a ⟨2⟩1. Discovery only: no Lean, mathlib4, registry, p4ng, or plan
file was written. Inputs: p4ng `62af31d` generator (G1 + any-definer fix),
futon2 registry at HEAD via `git show HEAD:holes/labs/wm-contract/aif-equations.edn`
(as-of 2026-09-26, sha-prefix 22997a93; the worktree copy carries another
lane's uncommitted edit and was not used), mathlib4 at HEAD `0e976af61e`
("the co-application kernel at the containment order (W9-1)"). Reproduced
into scratch: `AIF_EQ=/tmp/g1d/eq.edn AIF_OUT=/tmp/g1d/ bb
empirics-futon/gen_lean_dag.bb`, census **17 imported / 11
term-present-not-imported / 11 absent / 1 consumer-unplaced of 40** — the
counts claude-9 reported at b79b4ab666, unchanged at the newer HEAD.

Every demoted term carries `:reached-without-term`. 24 demoted term-entries
over 16 registry edges (some edges have two importers of the same term).
Causes: **(h)** exogenous, no hint possible as-is; **(m)** registry/Lean
mismatch; **(p)** declaring module not imported — real Lean gap; **(w)**
closed by pending work. Line numbers are at mathlib4 `0e976af61e`.

## 1. Per-edge entries

### (h) A at R4 — seven edges, ONE hint closes all seven

Edges: [R4→R1] `:belief-state`, [R4→R2] `:observe`, [R4→R3]
`:state-belief-update`, [R4→R3a] `:state-prediction-error`, [R4→R5]
`:ambiguity`, [R4→R7] `:likelihood-precision`, [R4→R8] `:policy-free-energy`.
Reached on every one: `TokenObservation` (plus `RolloutAtMachine` for R3a,
R7). The registry already rules the reading: exogenous `:A` is
`:superseded-by :token-likelihood` — at token grain A **is**
`TokenObservation.tokenLikelihood (r : AdjudicationRates V)`
(TokenObservation.lean:46).

The machine's A in each consumer, checked in the Lean:

| consumer | binder / declaration that IS A |
|---|---|
| R1 `machineTrajectory` | `M.A` — `ForwardModel.A` field (PolicyRollout.lean:28), via `machineStep` |
| R2 `machineObservationLaw` | `M.A` inside `observationAfter M world u = ∑_s M.B u sPrev s * M.A s o` (ObservationProcess.lean:35) |
| R3 `machineStep` | literally `exactUpdate M.A (M.B u) (obs (t+1)) μ` (BeliefStepAtMachine.lean:58) |
| R3a `machineStatePredictionError` | `M.A` via `machineStates`/`errorAtStates` (StatePredictionErrorAtMachine.lean:110-120) |
| R5 `ambiguity` | the `A : observationKernel` binder (Holes.lean:7223), instantiated from `GenerativeModel.observation` (Holes.lean:7115) |
| R7 `adjudicationRatesOf` | the rates themselves: A at token grain is `tokenLikelihood r`, `r : AdjudicationRates V` computed from the counted records (AdjudicationCounts.lean:333) |
| R8 `prefixF` | `Step.A : S → O → ℝ` field (PrefixFreeEnergyAtMachine.lean:87), used in `stepF` (:105) |

**Closure (smallest packet, registry only):** add `:lean-term {:A
"tokenLikelihood"}` to the seven importing rows. `tokenLikelihood` is a
declaration in `TokenObservation`, reached on all seven edges, so G1's
exogenous-hint rule admits it. Caveat for the row owner: at CHANNEL grain A
stays exogenous (the exogenous entry's own note says so); the hint claims
the token-grain reading and each row may want a `:lean-term-note` saying
that, in the RC1/RC2 style already used for `:eps`/`:T`.

### (h→p) B at R4 — four edges, NO hint can close them today

Edges: [R4→R1], [R4→R2], [R4→R3], [R4→R3a] (importers `:belief-state`,
`:observe`, `:state-belief-update`, `:state-prediction-error`). B's binder
in every consumer is `M.B u` — the `ForwardModel.B` field
(PolicyRollout.lean:25), a **free structure field**: nothing reached
declares B. B's cascade-grain definer per the registry (`:superseded-by
:co-application-kernel`) is `CoApplicationKernel.coApplyKernel` — an R4
module **no B consumer imports** (reached lists: TokenObservation,
RolloutAtMachine only). So this is (h) with a (p) tail: the hint would be
`coApplyKernel`, but its module is not reached.

Closures: **(a)** Lean instantiation at cascade grain — the
belief/observation modules take `B := coApplyKernel pat r` (needs the `:r`
instantiation too, §1.(p) below); **(b)** a registry ruling that B's
carrier is the `ForwardModel.B` field and a re-host. (a) is a real Lean
packet, bigger than an import; (b) is a ruling. These four are the
genuinely-open subset of the exogenous demotions.

### (h, by design) [R16→R2] `:world`

Importer `:observe`; reached `ActionAtMachine` (R16's only module).
`machineAction` takes no world. The binder that IS the world is `world : S`
in `machineObservationLaw` (ObservationAtMachine.lean:44 — R2's OWN module)
/ `sPrev : S` in `observationAfter` (ObservationProcess.lean:34), and the
`:observe` row already records exactly this in `:enters-through {:world
:sPrev}` with the note ":world is exogenous, so there is no module to
import and the note is the whole of what can be said". **Closure: none —
correctly absent.** Only a re-host of `:world` (a registry ruling) changes
it. No packet.

### (m, generator-side) [R2→R4] `:interp`

Importer `:co-application-kernel`, which ALREADY carries `:lean-term
{:interp "pat"}` — and `pat : ι → InterpretedPattern V` is indeed
`coApplyKernel`'s binder (CoApplicationKernel.lean:99). It still demotes
because G1 requires the hint's declaration/binder to live in a reached
**source** module, and `pat` lives in the consumer's own module. The
carrier is `CascadeTransition.InterpretedPattern` (CascadeTransition.lean:22),
which `CoApplicationKernel` **imports** — the Lean import behind the edge
exists. What fails is the registry/generator side: `:interp` is hosted at
R2 (module `ObservationAtMachine`, declares nothing interp), and its
exogenous entry's `:lean "CascadeTransition.InterpretedPattern"` is not
read as a source by the generator — the exogenous entry's own `:lean-note`
says so ("gen_lean_dag.bb does not read an exogenous :lean as a term (G0's
limit)").

**Closure (smallest packet):** a generator G2 clause — read an exogenous
entry's `:lean` as its source. Then `CascadeTransition` is the source
module, reached from `CoApplicationKernel`, and the existing `"pat"` hint
makes the edge imported. One generator clause + one fixture; no Lean, no
import. Alternative: registry re-host of `:interp` (the exogenous entry's
note records the R2-vs-R6 placement as movable, undecided).

### (m, placement) [R2→R7] `:ref-label` — two importers

- `:adjudication-rates` → `adjudicationRatesOf (records : List (Record κ
  V))`; the admitted label is `Record.admitted : Option Admitted`
  (AdjudicationCounts.lean:79,87-91).
- `:channel-identity` → `currentPopulation (loaded) (labels : List (Label κ
  V))`; `Label.record.admitted` again (ChannelIdentity.lean:12-18).

Both carriers live in **R7's own modules**. R2's `ObservationAtMachine`
declares nothing label-like. **Closure:** a registry ruling — ref-label's
Lean carrier is `AdjudicationCounts.Record.admitted`; declaring that makes
the term R7-local (no cross edge to import), matching the exogenous
entry's note that the admitted label is R2-hosted only because "a judgement
recorded ABOUT a token is an observation". Not a hint, not an import.

### (m, confirmed) [R3a→R7] `:eps`

Importer `:precision`. The edge's source row `:prediction-error` names
`sensoryPredictionError` (module `SensoryPredictionError`, channel grain,
**not reached**). But `machineChannelPrecision` (the `:precision` row's own
`:lean`) consumes **`machineStatePredictionError`**
(ChannelPrecisionAtMachine.lean:84) — the STATE prediction error, declared
in `StatePredictionErrorAtMachine`, which IS reached. The registry's other
R3a row `:state-prediction-error` names exactly that declaration.

**Closure (smallest packet, registry only):** re-point the [R3a→R7] `:eps`
edge's source row from `:prediction-error` to `:state-prediction-error` →
owner module reached → imported. Secondary mismatch to flag for the row
owner: the `:precision` formal `Π_k := 1/max(Var(ε_k), ε0)` is
channel-indexed while `machineChannelPrecision` is over state errors at
`x : S` — the formal and the Lean differ in grain. (The row already carries
`:lean-term {:eps "errors"}` for `ChannelPrecision.channelPrecision`'s
window binder; that hint names a module that is also not reached, so it
alone cannot close the edge.)

### (p) [R4→R5] `:Q-o-pi` — two importers

- `:risk` → `outcomeRisk (Q : PredictiveOutcomeKernel PolicyIndex Obs)`
  (OutcomeRiskKL.lean:33-37) — Q is a **free** kernel argument.
- `:ambiguity` → `ambiguity (predictedState : ProbabilityKernel …)`
  (Holes.lean:7221-25) — likewise supplied.

The owner is `machineRollout` (RolloutAtMachine.lean:77), returning
`Fin (T+1) → O → ℝ` — the predicted outcome distribution Q(o|π). No R5
module (`Holes`, `OutcomeRiskKL`, `MachineQ`, `TargetGrainG`) imports
`RolloutAtMachine`. Meanwhile `MachineQ` builds its own
`machinePredictiveOutcomeKernel` (MachineQ.lean:205) from
`GenerativeModel + QReading + belief` — NOT from `machineRollout`; the two
constructions of Q(o|π) are never connected in the Lean.

Closures: **(a)** an instantiation module (`OutcomeRiskAtMachine`, the
shape of the existing AtMachine modules) importing `RolloutAtMachine` +
`OutcomeRiskKL`, supplying `Q` from `machineRollout`'s `predictedOutcome`;
**(b)** a registry re-point of the edge's source to
`machinePredictiveOutcomeKernel` — R5-local, so the edge becomes a
self-edge and the import question dissolves (a ruling about which
construction the registry's Q(o|π) means). (a) closes the import; (b) is
cheaper but changes what the edge claims.

### (p) [R5→R6] `:G`

Importer `:policy-posterior`. Owner `machineExpectedFreeEnergy`
(MachineQ.lean:280). Consumers bind G as a **free** argument:
`machinePosteriorE (G : PolicyIndex → EReal)` (PolicyPosteriorAtMachine.lean:870)
and `machineAction`'s `grade : PolicyIndex → Holes.ExpectedFreeEnergyValue`
(ActionAtMachine.lean:88-89). No R6 module (`MachinePolicySet`,
`PolicyPosteriorAtMachine`, `ContainmentOrder`) imports `MachineQ`;
reached: `Holes`, `OutcomeRiskKL` only. **Real gap.**

**Closure (smallest packet):** one instantiation module importing `MachineQ`
+ `PolicyPosteriorAtMachine`, supplying `grade := machineExpectedFreeEnergy
model reading belief Cdist positivePreference`. Same shape as the other
AtMachine instantiation modules; closes this edge.

### (p — premise correction) [R6→R4] `:r`

The requisition's lead listed this as **(w)** "W9-1-LEAN in flight". W9-1
has LANDED (mathlib4 HEAD `0e976af61e` is its commit) and **did not close
the edge**: `coApplyKernel` takes `r : ι → ι → Prop` as a **free argument**
(CoApplicationKernel.lean:99); `CoApplicationKernel` imports only
`CascadeTransition` and `CascadeOrder`, never `Proof2.ContainmentOrder`;
`containmentOrder` (ContainmentOrder.lean:68) remains uninstantiated. The
commit title's "at the containment order" describes intent, not an import.

**Closure (smallest packet):** import `Proof2.ContainmentOrder` in
`CoApplicationKernel` (or a sibling instantiation module) and add the
instantiated kernel `coApplyKernel pat (containmentOrder pat)` with its
theorems. One import closes [R6→R4] `:r` and is also the prerequisite for
closing the B edges at cascade grain (§1.B option a).

### (p) [R7→R3] `:Pi`

Importer `:belief-update` → `LaplaceBeliefUpdate.beliefUpdate`, which binds
`prec : Channel → ℝ` **free** (LaplaceBeliefUpdate.lean:33) and imports
`SensoryPredictionError` (channel grain). Owner `machineChannelPrecision`
lives in `ChannelPrecisionAtMachine`, not reached (reached:
`AdjudicationCounts` only). `BeliefStepAtMachine`'s own header (:196-199)
records the situation: `machineStep` takes no precision, and what the edge
actually supplies at R3 is the counted A through `KernelAtCounts` /
`AdjudicationCounts`.

Closures: **(a)** an instantiation module wiring `prec :=
ChannelPrecision.channelPrecision` of the sensory-error window (channel
grain — matching the `:precision` formal `Π_k`; note this is
`ChannelPrecision`, not the state-grained `ChannelPrecisionAtMachine`) into
`beliefUpdate`; **(b)** a registry ruling re-pointing the [R7→R3] term from
`:Pi` to the counted-A reading the BeliefStepAtMachine comment describes.
Same grain mismatch as `:eps` lurks in (a).

### (w) π at R6 — three edges, held on W11-1

Edges: [R6→R16] (`:action`), [R6→R4] (`:forward-model`), [R6→R8]
(`:policy-free-energy`). Owner `machinePolicySet` (MachinePolicySet.lean:40,
over `Candidate` = `GOverCascades.CascadePolicy`). Reached on all three:
`PolicyPosteriorAtMachine` only. The consumers bind π as: `policies : List
PolicyIndex` in `PolicyInputs` (ObservationAtMachine.lean:31) feeding
`machineAction`; `π : PolicyIndex` with the check `π ∈ (inputsAt t
μ).policies` in `machineRollout` (RolloutAtMachine.lean:79-86); `π :
PolicyKey M P` in `prefixF` (PrefixFreeEnergyAtMachine.lean:159). **Held on
W11-1's `:policy-set` rebind — a decision now with Joe (P0 step 4).** The
W11-2 trial (codex-1's `:policy-posterior-at-prefix-f` row) showed [R8→R6]
closes via `PrefixFreeEnergyAtMachine` while [R2→R6] is correctly refused.
No packet until the ruling.

## 2. Smallest packets, and which edges share one

| packet | kind | closes |
|---|---|---|
| **P-hint-A**: `:lean-term {:A "tokenLikelihood"}` on 7 importing rows | registry only | 7 A edges |
| **P-repoint-eps**: [R3a→R7] source row `:prediction-error` → `:state-prediction-error` | registry only | 1 edge |
| **P-gen-interp** (G2): generator reads an exogenous entry's `:lean` as source | generator clause + fixture | [R2→R4] `:interp` |
| **P-inst-r**: `CoApplicationKernel` imports `Proof2.ContainmentOrder`, instantiate `r := containmentOrder pat` | one Lean import + instantiated kernel | [R6→R4] `:r`; prerequisite for B option (a) |
| **P-inst-G**: instantiation module, `grade := machineExpectedFreeEnergy …` | one new Lean module | [R5→R6] `:G` |
| **P-inst-Q**: instantiation module, `Q` from `machineRollout` (or registry re-point to `machinePredictiveOutcomeKernel`) | one new Lean module, or ruling | [R4→R5] `:Q-o-pi` (both importers) |
| **P-inst-Pi**: instantiation module, `prec := ChannelPrecision.channelPrecision …` (or registry re-point to counted-A) | one new Lean module, or ruling | [R7→R3] `:Pi` |
| rulings needed | registry | B ×4 (carrier ruling or cascade-grain instantiation), `:ref-label` (R7-local carrier), `:world` (stays absent by design) |
| held | W11-1 decision with Joe | π ×3 |

If every packet lands: 17 + 7 + 1 + 1 + 1 + 1 + 2(terms on [R4 R5]) + 1 ≈
imported count rises by 12 edges worth of terms; the residue is B ×4,
`:world`, `:ref-label` ×2 importers, and the held π ×3.

## 3. Premise corrections for the plan owner

1. **[R6→R4] `:r` is not (w).** W9-1 landed (mathlib4 `0e976af61e`) and left
   `r` a free argument of `coApplyKernel`; `Proof2.ContainmentOrder` is
   imported nowhere in the consumer's closure. It is (p) with a one-import
   fix (P-inst-r).
2. **`:eps` is a clean (m).** `machineChannelPrecision` consumes the STATE
   prediction error; the edge's source row names the sensory one. The
   re-point target row (`:state-prediction-error`, same node) already
   exists — no Lean change.
3. **The A/B asymmetry.** The requisition grouped A and B under (h). A
   closes with one hint everywhere (`tokenLikelihood`); B cannot — its
   binder is a free `ForwardModel` field and its cascade-grain definer
   (`coApplyKernel`) is reached by no B consumer. B is the genuinely-open
   subset.
4. **`:interp` is not an import gap.** The Lean import
   (`CoApplicationKernel` → `CascadeTransition`) already exists; the
   blocker is that the generator does not read an exogenous entry's `:lean`
   (a documented G0 limit). One generator clause closes it.
5. Census at mathlib4 `0e976af61e` (not the requisition's `b79b4ab666`),
   same counts — the tree drift changed no edge class.
