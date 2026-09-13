import DarkTower.WarMachine.FullCertificateEventBinding
open DarkTower.WarMachine.FullCertificatePredicate
open DarkTower.WarMachine.FullCertificateRunBinding
open DarkTower.WarMachine.FullCertificateEventBinding

-- The ordinary-match branch accepts different selected/enacted occurrences.
-- This witnesses a local predicate gap, not a full production certificate.
example (fixed : ExternallyFixedRun) (req : FullScopeRequirements)
    (pin : ExactBytePin) :
    let s : EventSubject := ⟨fixed.identity, "selected-occurrence", "same-action", "selection", pin⟩
    let e : EventSubject := ⟨fixed.identity, "substituted-occurrence", "same-action", "enactment", pin⟩
    let expected : ExternallyFixedEventPair := ⟨s, e, "pair-authority", pin⟩
    let actual : EventBindingEvidence := ⟨⟨s, e⟩, none⟩
    s.occurrenceId ≠ e.occurrenceId ∧
      eventSelectionExact fixed req expected actual (.match "same-action" "same-action") := by
  exact ⟨by change ("selected-occurrence" : String) ≠ "substituted-occurrence"; decide, rfl, rfl, rfl, rfl, rfl, rfl⟩
