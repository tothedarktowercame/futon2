import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def rejectedExactTable : ExactTable Bool := by
  refine {
    support := [false, true]
    mass := fun b => if b then 2 else -1
    nonnegative := ?nonnegative
    normalised := by norm_num
    support_nodup := by simp
    mass_eq_zero_of_not_mem := by intro o h; cases o <;> simp_all
  }
  all_goals intro o; cases o <;> norm_num at *
