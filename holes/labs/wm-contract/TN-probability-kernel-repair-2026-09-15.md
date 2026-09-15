# Probability-kernel repair discovery — 2026-09-15

Author: codex-27. Requested by claude-20 for F13. **Discovery complete; repair not implemented.** Mathlib4 HEAD `a43440ab6136d47a7384a902325cfb72900ef48e`; no tracked WarMachine source diff at inspection. Only this note is a repository write. No source reload, click, configuration change, or full build was performed.

## Reproduction, actually executed

No active Lake/Lean build process was present at the preflight inspection; `.lake/build.lock` was absent. Used mathlib4's own existing packages, with no changes to `.lake` management. Command, run from `/home/joe/code/mathlib4`:

```sh
lake env lean /tmp/codex27-probability-kernel-repro.lean > /tmp/codex27-probability-kernel-repro.stdout 2> /tmp/codex27-probability-kernel-repro.stderr
```

**Exit 0; stderr empty.** The scratch source copies codex-26's control and adds a named negative-precision theorem and signature checks. This is an independent execution, not a quotation of the previous receipt. Complete scratch source:

```lean
import DarkTower.WarMachine.Holes
open DarkTower.WarMachine.Holes

-- Accepted "normalised" kernel: the sole outcome has probability only 1/2.
noncomputable def duplicateKernel : ProbabilityKernel Unit Unit where
  support _ := [(), ()]
  mass _ _ := 1/2
  nonnegative _ _ := by norm_num
  normalised _ := by norm_num
example : duplicateKernel.mass () () = 1/2 := rfl

-- Accepted kernel: an outcome outside the listed support has mass 100.
noncomputable def hiddenMassKernel : ProbabilityKernel Unit Bool where
  support _ := [false]
  mass _ b := if b then 100 else 1
  nonnegative _ b := by cases b <;> norm_num
  normalised _ := by norm_num
example : hiddenMassKernel.mass () true = 100 := rfl

-- The function named variational free energy accepts negative precision.
example : (variationalFreeEnergy (fun _ => -2) (fun _ => 1)).value = -1 := by
  norm_num [variationalFreeEnergy, Channel.all]

#print axioms duplicateKernel
#print axioms hiddenMassKernel

theorem negativePrecisionAccepted :
    (variationalFreeEnergy (fun _ => -2) (fun _ => 1)).value = -1 := by
  norm_num [variationalFreeEnergy, Channel.all]
#print axioms negativePrecisionAccepted
#check ProbabilityKernel.mk
#check variationalFreeEnergy
```

Exact stdout:

```text
'duplicateKernel' depends on axioms: [propext, Classical.choice, Quot.sound]
'hiddenMassKernel' depends on axioms: [propext, Classical.choice, Quot.sound]
'negativePrecisionAccepted' depends on axioms: [propext, Classical.choice, Quot.sound]
DarkTower.WarMachine.Holes.ProbabilityKernel.mk.{u_1, u_2} {S : Type u_1} {O : Type u_2} (support : S → List O)
  (mass : S → O → ℝ) (nonnegative : ∀ (s : S) (o : O), 0 ≤ mass s o)
  (normalised : ∀ (s : S), (List.map (mass s) (support s)).sum = 1) : ProbabilityKernel S O
DarkTower.WarMachine.Holes.variationalFreeEnergy (precision error : Channel → ℝ) : VariationalFreeEnergyValue
```

SHA256 scratch `367606d29f94337700100ddabda2219df9a786b95f596528fdfc8bbbf668990f`; stdout `8b42911b6e13a748d08d4cbb5c430b85e352e7b7608696ce776c005845c3c3f9`; `Holes.lean` `4dc0a76b9999d09b2ab49c932117e5b7dcfec523e5e61735b3a84191229cd02b`. Constructor signature output confirms the imported compiled object exposes the defective four-field constructor; the VFE signature confirms the real-valued precision boundary. None of the three printed declarations uses `sorryAx`.

The two kernel controls are mathematical counterexamples: Unit has one outcome whose mass is 1/2, and Bool's total mass is 101 although only false is listed. Precision -2 produces F=-1 with unit errors.

## Smallest correct repair

**Recommend (a): retain List, add Nodup and zero off support.** This is a mechanical enforcement of the already-declared finite-distribution meaning, with no choice of state space, preferred outcome, gain or probability value. Do not deduplicate and renormalize malformed inputs, silently drop outcomes, or identify declared support with strictly positive support. The existing system intentionally names zero-mass outcomes and sometimes their order.

| Candidate | Soundness and definitions | Proof / migration burden |
|---|---|---|
| (a) List + row Nodup + mass zero outside row | Every nonzero mass lies in a finite list, each element is counted once, masses are nonnegative and total mass is 1. A listed zero is allowed. Risk, entropy, ambiguity, EFE and EIG keep their current list sums and real log convention. | Smallest change: two proofs at constructors, repair actual malformed masses, retain existing list-sum lemmas. No equality typeclass is needed in the structure signature. An adapter to a finite PMF can establish correspondence later. |
| (b) Finset + mass zero outside row | Also sound; Finset rules out repetitions structurally. Rewrite sums to finite-set sums; preserve risk/EIG positivity premises and zero-mass convention. Finset alone without zero-off-support is insufficient. | All `.map ... .sum`, membership simplification, exact-list support equalities, and list-sum interchange proofs need migration. Finset construction/proofs may use DecidableEq or classical choice. Preserve a separate ordered list where order is meaningful: `MachinePreferenceDistribution.lean:67` and `:71` inspect first named / first positive outcomes. Extra work without a stronger distribution guarantee than (a). |
| (c) PMF with an explicit finite-support guarantee | Mathlib PMF is globally normalized and has no hidden mass, but PMF alone need not have finite support. Use `S → PMF O` plus row finite-support proofs, or a wrapper carrying a finite declared carrier and zero off it. Convert ENNReal values to ℝ for the existing finite KL/entropy sums; keep positivity/infinite-risk admission explicit. | Largest migration: real/ENNReal conversions, finite tsum-to-sum lemmas, support equalities, and fixture proofs. PMF's support is the nonzero set, so retain independent declared carrier/order metadata for named zeros. `PMF.ofFinset` supplies the relevant sound constructor, not an automatic conversion from malformed current rows. |

Local library authority (inspected source): `mathlib4/Mathlib/Probability/ProbabilityMassFunction/Basic.lean:46` defines PMF as ENNReal functions with HasSum 1; `:79` makes support the nonzero set. `mathlib4/Mathlib/Probability/ProbabilityMassFunction/Constructions.lean:164` defines `ofFinset` with both finite-sum normalization and zero-off-finset obligations. PMF is the compared existing mathlib structure; bare finite measures would additionally need total mass 1.

For (a), the listed support is a finite **enclosing carrier**, not necessarily the minimal mathematical support. Nodup allows conversion to a Finset without altering the sum; zero off carrier makes the global sum equal to that finite sum. This supplies the finite probability-distribution guarantee. Existing strict positivity on *all listed* Q outcomes in `Holes.lean:7131` is stronger than positive-Q-only KL admission; preserve it here. `PreferenceRiskSeparation.lean:44` already supplies the latter admission separately. No probability repair can turn the arbitrary ambiguity estimator in `Holes.lean:7151` into a kernel-derived estimator automatically.

## Per-declaration impact in the 17 exact-token files

Census: `rg -l ProbabilityKernel mathlib4/DarkTower/WarMachine --glob '*.lean'` returned 17 files; all appear below. **Edit** rows identify actual source changes; no-edit/recheck rows distinguish inherited constraints from text rewrites. Proof reuse is a source-level assessment, not a claim that the unimplemented migration has compiled. Existing numerical theorems downstream of a changed fixture must be rerun even where their text can remain unchanged.

| Source (relative to `mathlib4/DarkTower/WarMachine/`) | Declaration | Required change / recheck |
|---|---|---|
| `Holes.lean:7015` | `ProbabilityKernel` | Add `support_nodup : ∀ s, (support s).Nodup` and `mass_eq_zero_of_not_mem : ∀ s o, o ∉ support s → mass s o = 0`. Keep the four existing fields and list sum. |
| `Holes.lean:7022` | `PredictiveOutcomeKernel` | No alias edit; automatically inherits the stronger kernel obligations. Re-elaborate constructors through this alias. |
| `Holes.lean:7026` | `ParameterPriorKernel` | No alias edit; automatically inherits the stronger kernel obligations. Re-elaborate constructors through this alias. |
| `Holes.lean:7030` | `ParameterPosteriorKernel` | No alias edit; automatically inherits the stronger kernel obligations. Re-elaborate constructors through this alias. |
| `Holes.lean:7035` | `TransitionKernel` | No alias edit; automatically inherits the stronger kernel obligations. Re-elaborate constructors through this alias. |
| `Holes.lean:7039` | `PolicyPriorKernel` | No alias edit; automatically inherits the stronger kernel obligations. Re-elaborate constructors through this alias. |
| `Holes.lean:7043` | `PreferenceDistribution` | No alias edit; automatically inherits the stronger kernel obligations. Re-elaborate constructors through this alias. |
| `Holes.lean:7067` | `observationKernel` | No alias edit; automatically inherits the stronger kernel obligations. Re-elaborate constructors through this alias. |
| `Holes.lean:7051` | `GenerativeModel` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7058` | `generativeFactorMass` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7098` | `observationKernelRowMass` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7131` | `predictiveOutcomeRisk` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7139` | `observationEntropy` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7144` | `ambiguity` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7151` | `expectedFreeEnergy` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7159` | `G_eq_expectedFreeEnergy` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7181` | `parameterInformationGain` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7190` | `expectedInformationGain` | No formula/statement/proof rewrite under the list repair; re-elaborate against repaired inputs. Finite sums now count each outcome once and exhaust all nonzero mass. |
| `Holes.lean:7249` | `eigCounterPredictive` | FIX: constant mass 1 also reaches other vertices of `EIGCounterObs`; restrict mass to `eigCounterOutcome`, add the two proofs, retain its listed point-mass value. |
| `Holes.lean:7256` | `eigCounterPrior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `Holes.lean:7263` | `eigCounterPosterior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `Holes.lean:7270` | `eigCounterPositivePrior` | Recheck existing positivity proof; parameter masses are unchanged. |
| `Holes.lean:7277` | `modelUncertaintyAndEIG` | Recheck `norm_num` after the predictive fixture repair; the valid point-mass model still gives EIG 0 versus bonus 1. Current fixture inhabitant cannot be retained unchanged. |
| `GenerativeModelWitness.lean:20` | `observation` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. The indexed observation family has only its two evidence constructors, not observations at all vertices. |
| `GenerativeModelWitness.lean:28` | `wrongObservation` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. The indexed observation family has only its two evidence constructors, not observations at all vertices. |
| `GenerativeModelWitness.lean:34` | `transition` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. The indexed observation family has only its two evidence constructors, not observations at all vertices. |
| `GenerativeModelWitness.lean:40` | `policyPrior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. The indexed observation family has only its two evidence constructors, not observations at all vertices. |
| `GenerativeModelNegative.lean:16` | `wrongObservation` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `GenerativeModelNegative.lean:21` | `transition` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `GenerativeModelNegative.lean:26` | `policyPrior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `PolicyPriorKernelNegative.lean:9` | `stateConditioned` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. Preserve `badPrior` rejection for state-versus-Unit conditioning. |
| `PreferenceDistributionConditioningNegative.lean:8` | `stateConditioned` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck the original negative control for its intended type mismatch. |
| `TransitionKernelUncontrolledNegative.lean:6` | `uncontrolled` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck the original negative control for its intended type mismatch. Use the delta at input state; both states exist. |
| `MachineBeliefDistribution.lean:13` | `selectedKernel` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. Use exhaustive seven-constructor `Status.all`; caller hypotheses remain unchanged. |
| `LocalPreferenceModule.lean:9` | `ExactTable` | Add the same two support obligations at rational mass type. Otherwise `ExactTable.kernel` promises an impossible conversion for malformed tables. |
| `LocalPreferenceModule.lean:15` | `ExactTable.kernel` | Forward table `Nodup`; cast the table off-support equality to ℝ. Keep the existing cast-sum normalization proof. |
| `LocalPreferenceModule.lean:52` | `binaryTable` | Add row support `Nodup` and off-support-zero proofs; preserve existing masses and normalization proof. Case-split vertex and Bool; both yes/no remain listed even at p=0 or p=1. |
| `MachineQ.lean:84` | `QReading` | No new field required for output-kernel repair. Correct the comment at line 74 that says off-support mass is unconstrained. `states` is nonempty by `beliefNormalised`; transfer row `Nodup` through the existing support equalities. This does not certify raw `beliefMass` off its state list. |
| `MachineQ.lean:139` | `machinePredictedStateKernel` | Add `Nodup` by taking a state in `reading.states` (nonempty by belief normalization), then using `transitionSupport` at `reading.plan π`. Prove zero off states termwise from transition zero-off-support. No new model parameter. |
| `MachineQ.lean:187` | `machinePredictiveOutcomeKernel` | Add `Nodup` via an observation row and `observationSupport`; prove zero off outcomes termwise from observation zero-off-support. Use the same nonempty-state argument. |
| `MachineQ.lean:122` | `predictedStateMass_sum` | Existing list-sum formula/proof or wrapper can stay; recheck after the two constructor obligations are supplied. |
| `MachineQ.lean:168` | `predictiveOutcomeMass_sum` | Existing list-sum formula/proof or wrapper can stay; recheck after the two constructor obligations are supplied. |
| `MachineQ.lean:210` | `rowsEqualOfEqualPlans` | Existing list-sum formula/proof or wrapper can stay; recheck after the two constructor obligations are supplied. |
| `MachineQ.lean:221` | `plansDifferOfRowsDiffer` | Existing list-sum formula/proof or wrapper can stay; recheck after the two constructor obligations are supplied. |
| `MachineQ.lean:238` | `machineAmbiguity` | Existing list-sum formula/proof or wrapper can stay; recheck after the two constructor obligations are supplied. |
| `MachineQ.lean:245` | `machineExpectedFreeEnergy` | Existing list-sum formula/proof or wrapper can stay; recheck after the two constructor obligations are supplied. |
| `MachineQWitness.lean:130` | `demoObservation` | FIX: `mass s o := aRow s o.2` ignores the vertex. Restrict to the evidence vertex and zero elsewhere, preserving every mass in `demoAlphabet`; add distinct-alphabet and zero-off-support proofs, recheck normalization. |
| `MachineQWitness.lean:150` | `demoTransition` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `MachineQWitness.lean:161` | `demoPolicyPrior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `MachineQWitness.lean:225` | `demoRowsNormalised` | Recheck against repaired demoObservation; listed evidence masses/plans are unchanged, so no intended numerical claim changes. |
| `MachineQWitness.lean:234` | `demoOrdinaryRowMasses` | Recheck against repaired demoObservation; listed evidence masses/plans are unchanged, so no intended numerical claim changes. |
| `MachineQWitness.lean:245` | `demoPolicyConditionedDifference` | Recheck against repaired demoObservation; listed evidence masses/plans are unchanged, so no intended numerical claim changes. |
| `MachineQWitness.lean:255` | `demoPlansDiffer` | Recheck against repaired demoObservation; listed evidence masses/plans are unchanged, so no intended numerical claim changes. |
| `MachineQWitness.lean:271` | `flatReadingRowsCoincide` | Recheck against repaired demoObservation; listed evidence masses/plans are unchanged, so no intended numerical claim changes. |
| `MachinePredictiveOutcome.lean:14` | `MachineA` | No structure edit needed: `supportExact` transports new kernel support obligations to `outcomes`. Status is inhabited. |
| `MachinePredictiveOutcome.lean:39` | `fullPlanOutcomeKernel` | Add outcomes `Nodup` from any Status row of `a.kernel`; prove zero off outcomes termwise using `supportExact` and kernel zero-off-support. Keep the double-sum normalization proof. |
| `MachineModelSpec.lean:44` | `Contract` | No field edit needed: kernel fields inherit the new guarantees; existing named carrier/order equalities stay. Its `FloatCarriedRow` is a separate approximate type, not converted into an exact probability kernel by this repair. |
| `PreferenceLadderDraft.lean:37` | `PreferenceFamily` | No edit; its kernel-valued field inherits guarantees. Terminal zero/support constraints remain compatible. |
| `PreferenceLadderDraft.lean:49` | `DispositionKernel` | Inherited constructor gains the two proofs; keep named zeroSupport and its specification. No local concrete constructor was found. |
| `PreferenceLadderDraft.lean:57` | `dispositionPredictiveMass` | No edit to existing expression. Arbitrary raw Qd/support parameters are not upgraded to probability kernels by strengthening Cterminal; this remains a separate seam. |
| `PreferenceLadderDraft.lean:64` | `SatisfiesDispositionBridge` | No edit to existing expression. Arbitrary raw Qd/support parameters are not upgraded to probability kernels by strengthening Cterminal; this remains a separate seam. |
| `PreferenceLadderDraft.lean:73` | `dispositionRisk` | No edit to existing expression. Arbitrary raw Qd/support parameters are not upgraded to probability kernels by strengthening Cterminal; this remains a separate seam. |
| `PreferenceLadderDraft.lean:86` | `SingleSupportRiskIsPolicyConstant` | No edit to existing expression. Arbitrary raw Qd/support parameters are not upgraded to probability kernels by strengthening Cterminal; this remains a separate seam. |
| `PreferenceRiskSeparation.lean:14` | `constantConditional` | Forward both new proofs from `distribution` at Unit, just as normalization is forwarded. |
| `PreferenceRiskSeparation.lean:23` | `predictiveMass` | No formula/proof rewrite expected; zero masses remain explicit, and existing list normalization algebra is unchanged. |
| `PreferenceRiskSeparation.lean:30` | `constant_absorbs_prediction` | No formula/proof rewrite expected; zero masses remain explicit, and existing list normalization algebra is unchanged. |
| `PreferenceRiskSeparation.lean:44` | `riskAdmissible` | No formula/proof rewrite expected; zero masses remain explicit, and existing list normalization algebra is unchanged. |
| `PreferenceRiskSeparation.lean:49` | `scalarKL` | No formula/proof rewrite expected; zero masses remain explicit, and existing list normalization algebra is unchanged. |
| `PreferenceRiskSeparation.lean:56` | `preferred_zero_refuses` | No formula/proof rewrite expected; zero masses remain explicit, and existing list normalization algebra is unchanged. |
| `PreferenceRiskWitness.lean:12` | `groundedPrediction` | Add row support `Nodup` and off-support-zero proofs; preserve existing masses and normalization proof. Prove distinct mapped disposition constructors and zero outside the organization support. |
| `PreferenceRiskWitness.lean:20` | `grounded_admissible` | Recheck unchanged statement/proof; twelve named outcomes (including zeros), KL log 2 and the zero-preference refusal remain intact. |
| `PreferenceRiskWitness.lean:25` | `concrete_scalarKL` | Recheck unchanged statement/proof; twelve named outcomes (including zeros), KL log 2 and the zero-preference refusal remain intact. |
| `PreferenceRiskWitness.lean:30` | `same_twelve_support` | Recheck unchanged statement/proof; twelve named outcomes (including zeros), KL log 2 and the zero-preference refusal remain intact. |
| `PreferenceRiskWitness.lean:35` | `abstained_refuses` | Recheck unchanged statement/proof; twelve named outcomes (including zeros), KL log 2 and the zero-preference refusal remain intact. |
| `AmbiguityBridge.lean:47` | `deterministicPredictedState` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `AmbiguityBridge.lean:53` | `deterministicObservation` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `AmbiguityWitness.lean:11` | `predictedState` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `AmbiguityWitness.lean:17` | `observationModel` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |

## Additional alias-constructor impact (not covered by the 17-file grep)

A search for support/normalised constructors and the kernel alias names exposes these additional obligations. Omitting them would leave the migration incomplete. The rows below cover concrete kernel constructors found in WarMachine; `InteroceptivePolicyPosteriorFinite.finiteWeights` is a different normalized Fin-indexed weight structure, and `MachineForwardModelWitness` constructs approximate `FloatCarriedRow`, so neither gets ProbabilityKernel fields.

| Source (relative to `mathlib4/DarkTower/WarMachine/`) | Declaration | Required change / recheck |
|---|---|---|
| `ExpectedFreeEnergyWitness.lean:15` | `Q` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `ExpectedFreeEnergyWitness.lean:21` | `Cdist` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `ExpectedInformationGainWitness.lean:19` | `Q` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `ExpectedInformationGainWitness.lean:25` | `prior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `ExpectedInformationGainWitness.lean:31` | `posterior` | Add singleton `Nodup`; case-split Parameter to prove off-support mass is zero (b already has mass zero). Existing normalization proof stays. |
| `ObservationKernelWitness.lean:11` | `reference` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `PolicyPriorKernelWitness.lean:9` | `reference` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `ParameterPriorKernelHabitNegative.lean:6` | `habit` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `MachineParameters.lean:13` | `machineParameterPrior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `MachineParameters.lean:29` | `machineParameterPosterior` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `MachineTransition.lean:18` | `controlled` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `MachineTransition.lean:47` | `uncontrolled` | Add `Nodup` by enumerating the distinct carrier; off-support zero is vacuous by exhaustive membership. Existing normalization proof stays. |
| `ParameterPriorKernelWitness.lean:12` | `prior` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck existing row-sum/numerical witnesses; preserve all listed masses. |
| `ParameterPosteriorKernelWitness.lean:18` | `posterior` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck existing row-sum/numerical witnesses; preserve all listed masses. |
| `PredictiveOutcomeKernelWitness.lean:15` | `predictive` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck existing row-sum/numerical witnesses; preserve all listed masses. |
| `PredictiveOutcomeRiskWitness.lean:13` | `predictive` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck existing row-sum/numerical witnesses; preserve all listed masses. |
| `PredictiveOutcomeRiskWitness.lean:19` | `preference` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck existing row-sum/numerical witnesses; preserve all listed masses. |
| `PreferenceDistributionWitness.lean:13` | `fair` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck existing row-sum/numerical witnesses; preserve all listed masses. |
| `TransitionKernelWitness.lean:12` | `controlled` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck existing row-sum/numerical witnesses; preserve all listed masses. |
| `PredictiveOutcomeKernelUnconditionalNegative.lean:6` | `unconditional` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck the original negative control for its intended type mismatch. |
| `ParameterPriorKernelOutcomeNegative.lean:10` | `outcomePrediction` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck the original negative control for its intended type mismatch. |
| `ParameterPosteriorKernelOutcomeNegative.lean:6` | `outcomePrediction` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck the original negative control for its intended type mismatch. |
| `ParameterPosteriorKernelPriorNegative.lean:6` | `parameterPrior` | Current mass is nonzero off support. Replace it with the declared point/uniform mass on the listed outcomes and zero elsewhere; prove `Nodup`, zero-off-support and normalization. Recheck the original negative control for its intended type mismatch. |
| `PredictiveOutcomeRiskWitness.lean:25` | `positivePreference` | Proof currently uses globally constant preference mass. After restricting it to the two evidence outcomes, use membership in predictive singleton support before proving positivity. |
| `F10RuledCarrier.lean:88` | `seed` | Add row support `Nodup` and off-support-zero proofs; preserve existing masses and normalization proof. Preserve the twelve named outcomes and their present order; prove the mapped carrier exhaustive at organization and mass zero elsewhere. |
| `MachinePreferenceDistribution.lean:29` | `machineC` | Add row support `Nodup` and off-support-zero proofs; preserve existing masses and normalization proof. Preserve the twelve named outcomes and their present order; prove the mapped carrier exhaustive at organization and mass zero elsewhere. |
| `BeliefUpdateFalsifier.lean:7` | `kernel` | Add row support `Nodup` and off-support-zero proofs; preserve existing masses and normalization proof. Already a genuine delta kernel; singleton membership discharges both obligations. |

Negative fixtures must remain **well-formed probability distributions before the intended type error**. A missing-field error is not a successful conditioning/domain negative control. For point-mass fixtures, use exactly the already-declared selected outcome; no new model choice is necessary. A typed repair refusal is appropriate if a future constructor cannot exhibit its declared normalization/support obligations.

## Does any established result depend on the defect?

**Yes for the exact existing malformed inhabitants/conversion promise; no numerical theorem found whose intended conclusion requires the defect.** The current `LocalPreferenceModule.lean:15` conversion cannot remain a total conversion from the unrestricted ExactTable while preserving masses/support: the rational analogues of both audit controls inhabit ExactTable. Its domain must be strengthened at `:9`. That is an actual contract impact, not a proof-script inconvenience.

Concrete invalid witnesses include `MachineQWitness.lean:130` (non-evidence vertex mass), `Holes.lean:7249` (the EIG counterexample predictive mass at other vertices), `TransitionKernelWitness.lean:12` (mass 1 at both states despite singleton support), `ParameterPriorKernelWitness.lean:12`, `ParameterPosteriorKernelWitness.lean:18`, `PredictiveOutcomeKernelWitness.lean:15`, and the FIX rows above. Their existing declarations are accepted because off-support mass is unconstrained. The repair must not report those exact inhabitants as still valid.

I found **no deliberate duplicate-support witness** and **no searched numerical theorem that needs duplicate counting or off-support nonzero mass for its conclusion**. This is a scoped source audit of the kernel users, aliases and constructor consumers listed here, not a post-migration build result. The numerical fixtures evaluate listed outcomes; restricting the malformed masses preserves those values. In particular, `Holes.lean:7277` can still refute unconditional bonus=EIG using a genuine point-mass model, and the policy-separated evidence masses in `MachineQWitness.lean:245` survive the evidence-vertex restriction. Neither claim should be re-certified until its repaired fixture and proof have actually run. Generic sum identities such as `PreferenceRiskSeparation.lean:30` use normalization algebra and remain valid under the stronger hypotheses.

Separate seams are unchanged: `MachineQ.lean:84` does not constrain raw beliefMass off its state list; the output kernel proofs can nevertheless use transition/observation zero-off-support. `MachineParameters.lean:20` has a raw RegisteredLikelihood table and `:29` requires positive evidence at every outcome. Strengthening that raw table or designing conditioning on impossible observations would be another modelling/API task, not a necessary step to make every ProbabilityKernel inhabitant a finite distribution. Likewise `MachineModelSpec.lean:112` deliberately admits near-normalized float rows. This note neither normalizes those rows nor claims they are exact kernels.

## Nonnegative precision migration

Change only the precision input of VFE to the existing PrecisionMap. Signed errors remain signed. All repository VFE invocations were searched; no legitimate negative-precision numerical theorem was found. The scratch negative-precision witness should become a rejected constructor/call, and a positive nonnegativity theorem should commission the corrected boundary. Nonnegative input proves this particular squared-error expression nonnegative; it is not a claim about every other functional called free energy.

| Source (relative to `mathlib4/DarkTower/WarMachine/`) | Declaration | Required change / recheck |
|---|---|---|
| `Holes.lean:7092` | `variationalFreeEnergy` | Change precision argument to `PrecisionMap`, keep signed error `Channel → ℝ`; multiply by `(precision k).value` inside the unchanged sum. Add a nonnegativity theorem from nonnegative weights and squares (new declaration). |
| `Holes.lean:7103` | `beliefUpdate` | Pass `precision` directly in both VFE calls at 7123/7125; mean/variance clauses already project its nonnegative coordinates and stay unchanged. |
| `Holes.lean:7084` | `PrecisionMap` | No change; already Channel → NonnegativeReal, admitting zero precision. |
| `Holes.lean:7088` | `VariationalFreeEnergyValue` | No mandatory change: input discipline and a theorem suffice to prove VFE outputs nonnegative; arbitrary direct construction of this wrapper is still possible. Strengthening the result wrapper is a separate API change. |
| `VariationalFreeEnergyWitness.lean:23` | `constantGaussianReference` | Wrap the fixture precision 2 with its `by norm_num [gaussianReference]` nonnegativity proof; keep the raw reference record and F=1 arithmetic. No fixture-byte change needed. |
| `PrecisionWitness.lean:22` | `weightedReference` | Pass `precisionTwo` directly, removing real-valued projections in the argument; existing unfolding/norm_num proof should reduce to identical arithmetic. |
| `PrecisionWitness.lean:27` | `swappedReference` | Pass `precisionOne` directly, removing real-valued projections in the argument; existing unfolding/norm_num proof should reduce to identical arithmetic. |
| `PrecisionWitness.lean:32` | `precisionAndErrorAreNotInterchangeable` | Pass `precisionTwo / precisionOne` directly, removing real-valued projections in the argument; existing unfolding/norm_num proof should reduce to identical arithmetic. |
| `PrecisionNegative.lean:19` | `badPrecision` | Existing signed-map type rejection remains. Add a separate VFE-call negative control so the function boundary itself is commissioned; do not claim this old test exercised that boundary. |
| `VariationalFreeEnergyNegative.lean:17` | `badVariationalValue` | Unchanged: this checks EFE-value versus VFE-value type separation, not precision sign. |
| `MachinePrecision.lean:190` | `machinePrecisionMap` | No change needed; already returns PrecisionMap with nonnegative coordinate proofs. |

| Source | Declaration | Required change / recheck |
|---|---|---|
| `BeliefUpdateFalsifier.lean:28` | positive anonymous `example` | Recheck unfolding of beliefUpdate/VFE; the existing PrecisionMap of ones makes the arithmetic identical. |
| `BeliefUpdateFalsifier.lean:47` | no-update anonymous `example` | Recheck after signature migration; proof uses the mean clause, not negative precision. |
| `BeliefUpdateFalsifier.lean:56` | unchanged-variance anonymous `example` | Recheck after migration; proof uses the variance clause. |

`MachineBeliefUpdate.lean:23` and `:112` describe a separate operational signed-error/precision-cancellation calculation, not calls to Holes.variationalFreeEnergy; no mechanical signature edit follows there. The raw GaussianReference precision field (`VariationalFreeEnergyWitness.lean:9`) can stay real fixture data: explicitly validate its value when constructing the PrecisionMap instead of silently clamping it.

## Validation and next implementation packet

Completed here: actual scratch compilation (exit 0), exact stdout capture, all 17 direct-reference files inspected, alias-constructor and all VFE call-site searches, local PMF API inspection. No repaired code or full dependency rebuild is claimed; note-only work needs no Clojure/Lisp gates.

The mechanical packet is: add the two kernel fields; strengthen ExactTable and forward obligations; repair FIX fixtures to their already-declared point/uniform supports; supply machine composition proofs; change the VFE precision boundary and its callers. Then run each affected positive/negative Lean witness, preserving intended errors, plus three rejection controls for duplicate support / hidden mass / negative precision and a theorem of VFE nonnegativity. Do not claim F13's cascade computation or production correspondence from this carrier repair alone. No new modelling decision is required for this recommended packet; the separate seams above remain explicitly outside it.
