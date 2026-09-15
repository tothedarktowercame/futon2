import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def rejectedKernel : ProbabilityKernel Unit Bool := by
  refine {
    support := fun _ => [false, true]
    mass := fun _ b => if b then 2 else -1
    nonnegative := ?nonnegative
    normalised := by intro s; norm_num
    support_nodup := by intro s; simp
    mass_eq_zero_of_not_mem := by intro s o h; cases o <;> simp_all
  }
  all_goals intro s o; cases o <;> norm_num at *
