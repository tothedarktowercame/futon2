# C237 — Q pin / glossary-binding coupling

As of 2026-08-31, the Q-interface map is pinned to the current content of
`DarkTower/WarMachine/Holes.lean`:

```
sha256 64f264cad5905d2a3beb0bb9237f5a91aeecb707e803ace20872300e099942fc
last content commit 38a5c2188fddc9bbdddf47dc3efc4b259b11ced5
```

The changes since the prior pin (`3aa5a59b0c6a508d72df3f42bc9a10dda7e5e8dd`)
witnessed `softmax` and `bayesFactorThreshold`. They did not change
`PredictiveOutcomeKernel`, the missing machine-Q construction, the
`predictiveOutcomeRisk` / `expectedFreeEnergy` consumers, or
`expectedInformationGain`. The Q map's semantics therefore remain unchanged.

## Required closure step

A glossary binding that changes `Holes.lean` entails re-verifying the Q-facing
declarations and, only when their semantics survive, refreshing the
`:lean-spine` pin. This is part of the Lean contract regeneration procedure in
`RUNBOOK.md`; it is not an automatic acceptance. The checker reports
`PIN_BEHIND` and names the remedy when the step is omitted.

## Falsifier

```
bb -cp . checks/q_interface_completeness_check.clj --negative-pin-behind
```

The mutation substitutes a real historical spine digest without refreshing
the pin. It must observe a non-passing Q verdict in state `PIN_BEHIND`; the
outer control succeeds only when that red verdict is preserved.
