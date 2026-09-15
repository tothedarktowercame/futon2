import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def rejectedKernel : ProbabilityKernel Unit Bool := by
  refine {
    support := fun _ => [false]
    mass := fun _ b => if b then 100 else 1
    nonnegative := by intro s b; cases b <;> norm_num
    normalised := by intro s; norm_num
    support_nodup := by intro s; simp
    mass_eq_zero_of_not_mem := ?mass_eq_zero_of_not_mem
  }
  all_goals intro s o h; cases o <;> norm_num at *
