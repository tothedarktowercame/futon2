# NOUNS-D3 — Gate 0 vocabulary audit

Date: 2026-08-31. Consumer: the War Machine contract gate and future witnesses for `expectedFreeEnergy` and model reduction.

## Judgement

The wrapper cost is worth paying, but there should be **three semantic result types, not four**. `G` (risk minus expected information gain) and `expectedFreeEnergy` (risk plus ambiguity) are two decompositions of the same AIF quantity, so both now return `ExpectedFreeEnergyValue`. Per-tick precision-weighted prediction error now returns `VariationalFreeEnergyValue`. The Dirichlet-model comparison formerly named `deltaFReduction` is now `modelReductionFreeEnergyChange` and returns `ModelReductionFreeEnergyChange`; `bayesFactorThreshold` accepts only that wrapper.

This is stronger than prose and cheaper than expected. `IsArgminOn` was generalized from real-valued scores to any ordered score, and the existing contract then compiled without proof repair. The bad bridge that motivated the audit is no longer typeable: a `VariationalFreeEnergyValue` cannot be supplied to `bayesFactorThreshold`. Arithmetic remains explicit at the constructors' `.value` boundary rather than acquiring broad coercions that would erase the distinction.

The policy conflict did not require Joe. The state-to-action function is now `DecisionRule`; it is what inference produces. The object indexed by π and scored by polymorphic `G`, `softmax`, and `IsArgminOn` is the pattern-language cascade, recorded by the generic alias `cascadeGrainPi (P) := Cascade P`. This closes the former `cascadeGrainPi` refusal without choosing a concrete pattern carrier prematurely.

## R13 gate-time decision

**R13 in `p4ng/sec-catalog.tex` does not need to change.** It already says “score whole pattern cascades as policies, not single steps” and explicitly identifies the policy with “the pattern cascade for the selected mission.” That is the distinction now expressed in Lean. Editing it would restate, not correct, the pattern.

## Gates

- `lake build DarkTower.WarMachine.Holes`: pass.
- `mathlib4/scripts/emit-contract.sh`: pass; 80 declarations, 45 closed and 35 holes. The hole count falls by one because `cascadeGrainPi` is now defined; no new `sorry` was added.
- `bb checks/holder_check.clj`: required after the regenerated contract is committed into the canonical mathlib4 checkout.

## Refusals retained

This audit does not fill `expectedFreeEnergy`: its kernel-derived risk-plus-ambiguity computation remains a genuine hole. It does not add a coercion from any wrapper to `ℝ`, because an implicit escape hatch would recreate the conflation. It does not pick a concrete `P` for `cascadeGrainPi`; the existing `Cascade P` is already the correctly polymorphic carrier.
