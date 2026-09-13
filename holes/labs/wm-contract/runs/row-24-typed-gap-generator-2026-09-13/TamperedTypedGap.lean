import DarkTower.WarMachine.FullCertificateCrossLayerBinding
namespace GeneratedTypedGap
open DarkTower.WarMachine
open CertificateStates FullCertificatePredicate FullCertificateCrossLayerBinding
-- Isolated structural input; no acquisition or production authority.
def att : FullAttestation :=
  { declaredNodes := ["R\"2\nα"]
    nodeStates := [⟨"R\"2\nα", .unvalidated "claim\\text" "build"⟩]
    declaredConnections := ["edge"]
    connectionStates := [⟨"edge", .mandatoryUnfired "build"⟩]
    selectionEnaction := .refusedShape "no evidence"
    recordFamilies := []
    negativeScope := [] }
theorem census : CensusComplete att := by decide
theorem rejects (req : FullScopeRequirements) (ev : FullScopeEvidence) :
    ¬ FullQualifyingRun req ev att :=
  rejects_missing_record_family att req ev .fullLoopCheckpoints "not acquired" (by decide)
theorem rejects_cross {fixed events subjects expected actual x req ev rb eb} :
    ¬ CrossLayerQualifyingRun fixed events subjects expected actual x req ev rb eb att :=
  fun h => rejects req ev (crossLayer_implies_full h)
#print axioms census
#print axioms rejects
#print axioms rejects_cross
end GeneratedTypedGap
