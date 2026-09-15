import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

def negativePrecision : PrecisionMap := by
  intro channel
  refine { value := -2, nonnegative := ?precision_nonnegative }
  all_goals norm_num
