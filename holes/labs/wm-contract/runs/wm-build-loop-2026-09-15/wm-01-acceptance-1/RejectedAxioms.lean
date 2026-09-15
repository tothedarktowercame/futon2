import DarkTower.WarMachine.ProbabilityKernelRepairNegative
open Lean Elab Command
elab "#rejected_axioms" : command => do
  for n in [``DarkTower.WarMachine.ProbabilityKernelRepairNegative.duplicateKernel,
            ``DarkTower.WarMachine.ProbabilityKernelRepairNegative.hiddenMassKernel,
            ``DarkTower.WarMachine.ProbabilityKernelRepairNegative.negativePrecisionCall] do
    let id := mkIdent n
    elabCommand (← `(#print axioms $id))
#rejected_axioms
