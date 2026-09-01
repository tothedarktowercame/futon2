# C347 — nontrivial positive-proof receipt migration

Date: 2026-09-01

The ten nontrivial witnesses identified by C343 were migrated in the recorded order, in two reviewable batches:

1. `actGate`, `aliveness`, `ambiguity`, `expectedFreeEnergy`, `expectedInformationGain`, `logMultivariateBeta`;
2. `PrecisionMap`, `predictionError`, `predictiveOutcomeRisk`, `softmax`.

Every receipt pins declaration slices for the complete positive statement/body and its named semantic dependencies, the fixture identity and explicit fixture-to-Lean adapter, and the Lean toolchain/manifest plus elaboration and axiom result. All ten fit the C332 schema without weakening, whole-file hashing, or a resistant witness shape. Receipt validation remained approximately 2–4 seconds per binding when run individually; the six-receipt sequential focused pass took 22.56 seconds total.

Together with the six receipts already present, all 16 nontrivial value/coherence witnesses are now source-pinned. Positive-source receipt coverage is therefore 16/31. Glossary coverage remains 31/33; this packet added no binding.

The eleven construction/projection witnesses remain deliberately unpinned in this packet. Their authoritative witness fragments now record `:positive-evidence-limit :construction-or-projection-only` and state the limit explicitly: a future source pin can prevent silent weakening, but the witness establishes carrier construction or stored normalization, not downstream behaviour.
