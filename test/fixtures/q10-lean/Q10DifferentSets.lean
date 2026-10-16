import DarkTower.WarMachine.RequirementsReader
set_option maxRecDepth 100000
set_option maxHeartbeats 5000000
open DarkTower.WarMachine
open DarkTower.WarMachine.Requirements
open DarkTower.WarMachine.RequirementsReader
def q10DifferentSets : PartialRunFacts where
  q1 := .known { Requirements.click20 with openMissions := {476960309, 860697421, 1160381552, 1999351093}, openExcursions := {}, openTickets := {}, enumeratedTasks := {476960309, 860697421, 1160381552, 1999351093} }
  q2 := .nr "not recomputable: frontierCensus,frontierSelection,selectedCascade"
  q3 := .known { Requirements.click20 with cascadesWithoutG := {} }
  q4 := .known { Requirements.click20 with policyPreferences := [{ policy := 377883731, horizon := 2, preferenceSteps := {0, 1}, gradedPreferenceSteps := {0, 1}, semantics := .progressive, normalization := .perStepCapacityAndPattern, terms := ⟨true, true, true⟩ }, { policy := 1751666184, horizon := 3, preferenceSteps := {0, 1, 2}, gradedPreferenceSteps := {0, 1, 2}, semantics := .progressive, normalization := .perStepCapacityAndPattern, terms := ⟨true, true, true⟩ }], gTerms := ⟨true, true, true⟩, policiesWithRiskTerm := 2, policiesWithAmbiguityTerm := 2, policiesWithInformationTerm := 2, comparedPolicies := {377883731, 1751666184} }
  q5 := .nr "not recomputable: interpretationOrder"
  q6 := .nr "not recomputable: previousChoice,previousOutcome,previousInputDigest"
  q7 := .known { Requirements.click20 with pathAbsenceCount := 1 }
  q8 := .nr "not recomputable: frontierCensus,frontierRefresh,frontierSelection,selectedCascade"
  q9 := .known { Requirements.click20 with factorizedPreference := none, calibrationAuthorizations := [], factorizationAuthorizations := [] }
  q10 := .known { Requirements.click20 with differentArrangementPairs := 0, arrangementPairsDistinguishedByG := 0 }
#eval alert q10DifferentSets
