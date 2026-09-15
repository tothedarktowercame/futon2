import DarkTower.WarMachine.FloatCarriedRowCorrespondence
open DarkTower.WarMachine.MachineModelSpec
open DarkTower.WarMachine.FloatCarriedRowCorrespondence
namespace FloatCarrierControls
def bad : FloatCarriedRow Bool where
  support := [false]
  mass := fun _ => 1
  nonnegative := by intro b; cases b <;> norm_num
  support_nodup := by decide
  mass_eq_zero_of_not_mem := by
    refine ?mass_eq_zero_of_not_mem
    all_goals intro b h; cases b <;> norm_num at *
  nearNormalised := by norm_num [floatRowBound]
end FloatCarrierControls
