# Completed-run consumption witness

These are copies of the durable outer state (click-count 0, pending f74df9cc),
its Makeup migration evidence, and the actual f74df9cc record. The two run
stream events and the history-index row are retained verbatim from the live
publications. The Giraffe directory is the pending selection's original
verified snapshot. Tests copy these files into a temporary directory before
reading or mutating any of them. They use the existing frozen click-51 outer
observation from ../q6-reselection/preview.edn as the subsequent observation.

No source record, index, ledger or live state is mutated. The classified-arm
control modifies the copied record and republishes its SHA; the tamper arm
modifies it without republishing the SHA.

Lean correspondence is unchanged: R20Step.Observation.endingClass admits
CTauClassPreference.EndingClass.notYetEvaluated; R20Step.endingDelta contributes
zero evidence, advanceC decays C, advanceStateCounts and advanceGainCounts
retain their ordinary update rules, and step increments runId. This packet
implements the previously missing completed-run adapter, not a new Lean rule.
