# C364 — construction positive receipts, batch 1

Date: 2026-09-01

First reviewable batch migrated: `BeliefState`, `Channel`, `Cohort`, `ControlVocabulary`, and `DirichletConcentrations`.

Each receipt pins the complete construction theorem and named carrier dependencies, an explicit fixture-to-Lean mapping, fixture identity, toolchain identity, elaboration, and axioms. The existing `:positive-evidence-limit :construction-or-projection-only` metadata remains attached: these receipts prevent silent weakening but do not turn carrier-construction evidence into downstream-behaviour evidence.

All five receipts fit the established schema. The five-receipt focused positive pass took 19.79 seconds total. Positive-source receipt coverage moves from 16/31 to 21/31: 16 nontrivial and 5 limited construction witnesses pinned, with 10 limited construction witnesses remaining.

Glossary coverage remains 31/33 pending the cascade-semantics decision; this migration adds no binding.
