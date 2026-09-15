import DarkTower.WarMachine.FloatCarriedRowCorrespondence
open DarkTower.WarMachine.MachineModelSpec
open DarkTower.WarMachine.FloatCarriedRowCorrespondence
namespace FloatCarrierControls
noncomputable def rejected := retainedRow.toProbabilityKernel (by
  refine ?exact_total_one_premise
  all_goals norm_num [retainedRow, retainedMass, DarkTower.WarMachine.MachineBeliefState.Status.all])
end FloatCarrierControls
