import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def rejectedExactTable : ExactTable Bool := by
  refine {
    support := [false]
    mass := fun b => if b then 100 else 1
    nonnegative := by intro b; cases b <;> norm_num
    normalised := by norm_num
    support_nodup := by simp
    mass_eq_zero_of_not_mem := ?mass_eq_zero_of_not_mem
  }
  all_goals intro o h; cases o <;> norm_num at *
