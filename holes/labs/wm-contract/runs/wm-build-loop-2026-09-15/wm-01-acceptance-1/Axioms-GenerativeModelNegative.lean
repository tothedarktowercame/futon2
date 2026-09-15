import DarkTower.WarMachine.GenerativeModelNegative
open Lean Elab Command
elab "#review_axioms" : command => do
  let wanted : List String := ["DarkTower.WarMachine.GenerativeModelNegative.policyPrior", "DarkTower.WarMachine.GenerativeModelNegative.transition", "DarkTower.WarMachine.GenerativeModelNegative.wrongObservation"]
  let env ← getEnv
  for user in wanted do
    let found := env.constants.toList.filter fun (n, _) => (privateToUserName n).toString == user
    if found.isEmpty then throwError "Missing reviewed declaration: {user}"
    for (n, _) in found do
      let id := mkIdent n
      elabCommand (← `(#print axioms $id))
      let deps ← collectAxioms n
      for a in deps do
        unless [``propext, ``Classical.choice, ``Quot.sound].contains a do
          logError m!"Unexpected axiom in {n}: {a}"
#review_axioms
