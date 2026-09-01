# C370 — construction positive receipt migration complete

Date: 2026-09-01

The final ten limited construction/projection witnesses now carry reason-preserving positive-proof receipts: `Fold`, `HaveWantArrow`, `observationKernel`, `PredictiveOutcomeKernel`, `PreferenceDistribution`, `TransitionKernel`, `ObservationVector`, `ParameterPosteriorKernel`, `ParameterPriorKernel`, and `PolicyPriorKernel`.

All ten retain `:positive-evidence-limit :construction-or-projection-only` metadata. The receipt protects the complete statement, named dependencies, fixture identity and explicit fixture-to-Lean adapter, toolchain identity, elaboration, and axioms. The scope metadata states what the protected statement is worth: carrier construction or stored normalization, not downstream behaviour.

No witness resisted the schema. Two five-receipt positive passes took 20.47 and 17.58 seconds, approximately 3.8 seconds per binding and consistent with the established 2–4 second range.

Positive-source receipt coverage is now 31/31, split honestly as **16 nontrivial value/coherence witnesses and 15 construction-or-projection-only witnesses**. This means every bound glossary witness is protected against silent source weakening; it does not mean the fifteen limited witnesses establish downstream behaviour.

Glossary coverage remains 31/33. The final two glossary terms remain blocked on the cascade-semantics modelling decision, not on receipt migration.
