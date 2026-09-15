import DarkTower.WarMachine.FloatCarriedRowCorrespondence
open DarkTower.WarMachine.MachineModelSpec
open DarkTower.WarMachine.FloatCarriedRowCorrespondence
namespace FloatCarrierControls
def sparseMass : Fin 3 → ℚ := fun o => if o = 0 then 1/3 else if o = 1 then 2/3 else 0
def sparse : FloatCarriedRow (Fin 3) where
  support := [0, 1]
  mass := sparseMass
  nonnegative := by intro o; fin_cases o <;> norm_num [sparseMass]
  support_nodup := by decide
  mass_eq_zero_of_not_mem := by intro o h; fin_cases o <;> norm_num [sparseMass] at *
  nearNormalised := by norm_num [sparseMass, floatRowBound]
theorem sparse_exact : (sparse.support.map sparse.mass).sum = 1 := by
  norm_num [sparse, sparseMass]
noncomputable def exactKernel := sparse.toProbabilityKernel sparse_exact
theorem unequal_coordinates : exactKernel.mass () 0 = 1/3 ∧ exactKernel.mass () 1 = 2/3 := by
  norm_num [exactKernel, FloatCarriedRow.toProbabilityKernel, sparse, sparseMass]
theorem outside_is_zero : (2 : Fin 3) ∉ sparse.support ∧ sparse.mass 2 = 0 := by
  decide
theorem exact_normalised : (exactKernel.support () |>.map (exactKernel.mass ())).sum = 1 :=
  sparse.toProbabilityKernel_normalised sparse_exact
example : retainedRow.support ≠ [] := retainedRow.support_ne_nil
#print axioms sparse
#print axioms sparse_exact
#print axioms exactKernel
#print axioms unequal_coordinates
#print axioms outside_is_zero
#print axioms exact_normalised
end FloatCarrierControls
