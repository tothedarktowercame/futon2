import DarkTower.WarMachine.FullCertificateRecordConnectionBinding
open DarkTower.WarMachine.FullCertificateRunBinding
open DarkTower.WarMachine.FullCertificateRecordConnectionBinding
-- Arbitrarily replacing all causal references has no effect on record validation.
example (fixed : ExternallyFixedRun) (s : RecordFamilySubject) (refs : CausalReferences) :
    recordSubjectValid fixed {s with references := refs} ↔ recordSubjectValid fixed s := by
  rfl
-- Any unrelated nonempty causal record ID preserves connection validity.
example (fixed : ExternallyFixedRun) (s : ConnectionSubject)
    (h : connectionSubjectValid fixed s) :
    connectionSubjectValid fixed {s with causalRecordId := "unrelated-old-record"} := by
  exact ⟨h.1, h.2.1, h.2.2.1, h.2.2.2.1, h.2.2.2.2.1,
    h.2.2.2.2.2.1, h.2.2.2.2.2.2.1, by change ("unrelated-old-record" : String) ≠ ""; decide⟩
