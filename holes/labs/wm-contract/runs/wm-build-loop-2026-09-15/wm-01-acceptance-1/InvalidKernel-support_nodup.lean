import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def rejectedKernel : ProbabilityKernel Unit Unit := by
  refine {
    support := fun _ => [(), ()]
    mass := fun _ _ => 1/2
    nonnegative := by intros; norm_num
    normalised := by intro s; norm_num
    support_nodup := ?support_nodup
    mass_eq_zero_of_not_mem := by intro s o h; cases o <;> simp_all
  }
  all_goals intro s; simp_all
