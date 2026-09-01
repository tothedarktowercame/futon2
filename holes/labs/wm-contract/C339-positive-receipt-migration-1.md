# C339 — positive receipt migration, consequence batch 1 (2026-09-01)

Four nontrivial positive witnesses were migrated:

- `bayesFactorThreshold` — exact passing and failing threshold sides;
- `modelReductionFreeEnergyChange` — the Gamma-identity `log 2` result;
- `bayesianModelReduction` — componentwise count conservation; and
- `GenerativeModel` — observation × transition × policy-prior factor mass.

Together with C332's `variationalFreeEnergy` and the pre-existing
`modelUncertaintyAndEIG` proof receipt, all six nontrivial positives identified
in the C324 sample now pin their positive source.

Each migration adds a field-for-field Lean reference object corresponding to
the independent EDN fixture.  Its receipt pins that adapter, the complete
positive theorem, the named semantic declarations in `Holes.lean`, fixture
identity, toolchain/manifest identity, elaboration, and axioms.  The existing
term-specific negative controls continue to pass.

The reusable schema fit all four.  No witness required weakening, a whole-file
hash, or an inferred fixture mapping.  Validation remains about 2–4 seconds
per receipt; the ten positive/control invocations for this batch took about
40 seconds, including their existing negative Lean elaborations.  This is not
a material increase over C332's per-binding estimate.  The current checkers
still perform their original positive Lean invocation in addition to receipt
elaboration; deduplicating that is an optional later optimisation, not part of
the integrity migration.

Positive-source-pinned glossary coverage is now **6/31**.  The four
definitional/projection witnesses remain unmigrated and retain C324's stated
limit: they witness carrier construction or stored normalization, not
downstream behaviour.
