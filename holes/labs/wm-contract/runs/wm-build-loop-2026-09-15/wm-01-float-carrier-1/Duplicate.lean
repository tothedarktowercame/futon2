import DarkTower.WarMachine.FloatCarriedRowCorrespondence
open DarkTower.WarMachine.MachineModelSpec
open DarkTower.WarMachine.FloatCarriedRowCorrespondence
namespace FloatCarrierControls
def bad : FloatCarriedRow Bool where
  support := [false, false]
  mass := fun b => if b then 0 else 1/2
  nonnegative := by intro b; cases b <;> norm_num
  support_nodup := by
    refine ?support_nodup
    all_goals norm_num [floatRowBound]
  mass_eq_zero_of_not_mem := by intro b h; cases b <;> norm_num at *
  nearNormalised := by norm_num [floatRowBound]
end FloatCarrierControls
