import DarkTower.WarMachine.RequirementsReader
set_option maxRecDepth 100000
set_option maxHeartbeats 5000000
open DarkTower.WarMachine
open DarkTower.WarMachine.Requirements
open DarkTower.WarMachine.RequirementsReader
def q2First : PartialRunFacts where
  q1 := .known { Requirements.click20 with openMissions := {476960309, 860697421, 1160381552, 1999351093}, openExcursions := {}, openTickets := {}, enumeratedTasks := {476960309, 860697421, 1160381552, 1999351093} }
  q2 := .nr "not recomputable: frontierCensus,frontierSelection,selectedCascade"
  q3 := .known { Requirements.click20 with cascadesWithoutG := {} }
  q4 := .known { Requirements.click20 with policyPreferences := [{ policy := 1604735827, horizon := 2, preferenceSteps := {0, 1}, gradedPreferenceSteps := {0, 1}, semantics := .progressive, normalization := .perStepCapacityAndPattern, terms := ⟨true, true, true⟩ }, { policy := 997767669, horizon := 2, preferenceSteps := {0, 1}, gradedPreferenceSteps := {0, 1}, semantics := .progressive, normalization := .perStepCapacityAndPattern, terms := ⟨true, true, true⟩ }], gTerms := ⟨true, true, true⟩, policiesWithRiskTerm := 2, policiesWithAmbiguityTerm := 2, policiesWithInformationTerm := 2, comparedPolicies := {1604735827, 997767669} }
  q5 := .known { Requirements.click20 with interpretationOrder := .selectionBeforeInterpretation }
  q6 := .nr "not recomputable: previousChoice,previousOutcome,previousInputDigest"
  q7 := .known { Requirements.click20 with pathAbsenceCount := 1 }
  q8 := .nr "not recomputable: frontierCensus,frontierRefresh,frontierSelection,selectedCascade"
  q9 := .nr "not recomputable: factorizedPreference,calibrationAuthorizations,factorizationAuthorizations"
  q10 := .nr "not recomputable: differentArrangementPairs,arrangementPairsDistinguishedByG"
#eval alert q2First
