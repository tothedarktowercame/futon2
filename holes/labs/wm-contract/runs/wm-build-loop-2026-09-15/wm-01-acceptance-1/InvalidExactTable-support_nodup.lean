import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def rejectedExactTable : ExactTable Unit := by
  refine {
    support := [(), ()]
    mass := fun _ => 1/2
    nonnegative := by intros; norm_num
    normalised := by norm_num
    support_nodup := ?support_nodup
    mass_eq_zero_of_not_mem := by intro o h; cases o <;> simp_all
  }
  all_goals simp_all
