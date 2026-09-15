import DarkTower.WarMachine.MachineBeliefDistribution
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.MachineBeliefState
open DarkTower.WarMachine.MachineBeliefDistribution
namespace Row7Binding
noncomputable def exactPosterior : Posterior
  | .spawned => (1 : ℝ) / 28
  | .refined => (1 : ℝ) / 14
  | .strengthened => (3 : ℝ) / 28
  | .addressed => (1 : ℝ) / 7
  | .falsified => (5 : ℝ) / 28
  | .foreclosed => (3 : ℝ) / 14
  | .reopened => (1 : ℝ) / 4
theorem exact_nonnegative : ∀ s, 0 ≤ exactPosterior s := by
  intro s; cases s <;> norm_num [exactPosterior]
theorem exact_Normalised : Normalised exactPosterior := by
  norm_num [Normalised, Status.all, exactPosterior]
noncomputable def kernel := selectedKernel exactPosterior exact_nonnegative exact_Normalised
def context : EntityContext := ⟨0, "wm-production", "2026-09-12-row-7"⟩
noncomputable def stored : machineBeliefState := fun e => if e = 0 then some exactPosterior else none
theorem selected : stored context.entity = some exactPosterior := by simp [stored, context]
theorem conserved : stored context.entity = some exactPosterior ∧
    ∀ s, kernel.mass () s = exactPosterior s :=
  selectedPosterior_conserved stored context exactPosterior selected exact_nonnegative exact_Normalised
theorem kernel_normalised : (kernel.support () |>.map (kernel.mass ())).sum = 1 :=
  selectedKernel_normalised exactPosterior exact_nonnegative exact_Normalised
theorem coordinate_spawned : kernel.mass () .spawned = (1 : ℝ) / 28 := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .spawned
theorem coordinate_refined : kernel.mass () .refined = (1 : ℝ) / 14 := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .refined
theorem coordinate_strengthened : kernel.mass () .strengthened = (3 : ℝ) / 28 := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .strengthened
theorem coordinate_addressed : kernel.mass () .addressed = (1 : ℝ) / 7 := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .addressed
theorem coordinate_falsified : kernel.mass () .falsified = (5 : ℝ) / 28 := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .falsified
theorem coordinate_foreclosed : kernel.mass () .foreclosed = (3 : ℝ) / 14 := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .foreclosed
theorem coordinate_reopened : kernel.mass () .reopened = (1 : ℝ) / 4 := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .reopened
noncomputable def prodPosterior : Posterior
  | .spawned => (7589757911525539 : ℝ) / 72057594037927936
  | .refined => (5075107643853621 : ℝ) / 36028797018963968
  | .strengthened => (5627707221685375 : ℝ) / 18014398509481984
  | .addressed => (3442556320081687 : ℝ) / 36028797018963968
  | .falsified => (2683201850079625 : ℝ) / 36028797018963968
  | .foreclosed => (5982758850052747 : ℝ) / 36028797018963968
  | .reopened => (7589757911525539 : ℝ) / 72057594037927936
theorem production_not_Normalised : ¬ Normalised prodPosterior := by
  norm_num [Normalised, Status.all, prodPosterior]

#print axioms exact_nonnegative
#print axioms exact_Normalised
#print axioms selected
#print axioms conserved
#print axioms kernel_normalised
#print axioms coordinate_spawned
#print axioms coordinate_refined
#print axioms coordinate_strengthened
#print axioms coordinate_addressed
#print axioms coordinate_falsified
#print axioms coordinate_foreclosed
#print axioms coordinate_reopened
#print axioms production_not_Normalised
#print axioms DarkTower.WarMachine.MachineBeliefDistribution.selectedPosterior_conserved
#print axioms DarkTower.WarMachine.MachineBeliefDistribution.selectedKernel_coordinate
#print axioms DarkTower.WarMachine.MachineBeliefDistribution.selectedKernel_normalised
end Row7Binding
