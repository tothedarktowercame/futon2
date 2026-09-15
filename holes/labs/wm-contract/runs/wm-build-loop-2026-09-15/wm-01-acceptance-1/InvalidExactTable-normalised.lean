import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def rejectedExactTable : ExactTable Unit := by
  refine {
    support := [()]
    mass := fun _ => 1/2
    nonnegative := by intros; norm_num
    normalised := ?normalised
    support_nodup := by simp
    mass_eq_zero_of_not_mem := by intro o h; cases o <;> simp_all
  }
  all_goals norm_num at *
