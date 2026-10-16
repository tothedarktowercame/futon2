import DarkTower.WarMachine.RequirementsReader
set_option maxRecDepth 100000
set_option maxHeartbeats 5000000
open DarkTower.WarMachine
open DarkTower.WarMachine.Requirements
open DarkTower.WarMachine.RequirementsReader
def q10WithoutOwnG : PartialRunFacts where
  q1 := .known { Requirements.click20 with openMissions := {476960309, 860697421, 1160381552, 1999351093}, openExcursions := {}, openTickets := {}, enumeratedTasks := {476960309, 860697421, 1160381552, 1999351093} }
  q2 := .nr "not recomputable: frontierCensus,frontierSelection,selectedCascade,comparedPolicies"
  q3 := .known { Requirements.click20 with cascadesWithoutG := {2057082714} }
  q4 := .nr "not recomputable: policyPreferences,gTerms,policiesWithRiskTerm,policiesWithAmbiguityTerm,policiesWithInformationTerm,comparedPolicies"
  q5 := .known { Requirements.click20 with interpretationOrder := .selectionBeforeInterpretation }
  q6 := .nr "not recomputable: previousChoice,previousOutcome,previousInputDigest"
  q7 := .known { Requirements.click20 with pathAbsenceCount := 1 }
  q8 := .nr "not recomputable: frontierCensus,frontierRefresh,frontierSelection,selectedCascade"
  q9 := .nr "not recomputable: factorizedPreference,calibrationAuthorizations,factorizationAuthorizations"
  q10 := .known { Requirements.click20 with differentArrangementPairs := 1, arrangementPairsDistinguishedByG := 0 }
#eval alert q10WithoutOwnG
