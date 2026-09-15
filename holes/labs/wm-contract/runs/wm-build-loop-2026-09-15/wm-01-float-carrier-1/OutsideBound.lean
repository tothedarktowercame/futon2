import DarkTower.WarMachine.FloatCarriedRowCorrespondence
open DarkTower.WarMachine.MachineModelSpec
open DarkTower.WarMachine.FloatCarriedRowCorrespondence
namespace FloatCarrierControls
def bad : FloatCarriedRow Bool where
  support := [false]
  mass := fun b => if b then 0 else 1 + 2/10^12
  nonnegative := by intro b; cases b <;> norm_num
  support_nodup := by decide
  mass_eq_zero_of_not_mem := by intro b h; cases b <;> norm_num at *
  nearNormalised := by
    refine ?nearNormalised
    all_goals norm_num [floatRowBound]
end FloatCarrierControls
