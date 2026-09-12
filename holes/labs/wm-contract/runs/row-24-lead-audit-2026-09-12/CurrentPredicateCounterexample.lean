import DarkTower.WarMachine.CertificateStates

/- A regression witness for the precursor's scope, not a qualifying run.
   The current predicate accepts every deliberately bad state below together.
   The completed row-24 predicate must reject the equivalent generated value. -/
namespace Row24LeadAudit
open DarkTower.WarMachine.CertificateStates

def ruling : QualifyingRuling :=
  { chosenReading := .three, ruledOutItems := [], bytesPin := "not-a-verified-hash" }

def bad : FullAttestation :=
  { declaredNodes := ["R2"]
    nodeStates := [⟨"R2", .supportedAtRun "unjoined-claim" "unverified-scope"⟩]
    declaredConnections := ["R16->R2"]
    connectionStates := [⟨"R16->R2", .mandatoryUnfired "this-run"⟩]
    selectionEnaction := .refusedShape "no selected/enacted pair"
    recordFamilies := allRecordFamilies.map (fun f => .typedGap f "missing")
    negativeScope := [⟨"unruled-gap", "no authorizing ruling"⟩] }

theorem precursor_accepts_incomplete_run : qualifyingRun ruling bad := by
  simp [qualifyingRun, ruling, readingThree, CensusComplete, bad,
    allRecordFamilies, RecordFamilyPresence.family, positiveAtThisRun]

#print axioms precursor_accepts_incomplete_run
end Row24LeadAudit
