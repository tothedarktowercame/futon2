# AUTH — Q9 factorized preference: calibration and factorization authorized by Joe (2026-11-01 ~17:55Z)

Joe, in reply to the question "authorize a relevance cost triple with focused below related
below unrelated, and the factorization law 'completion first, then conditional relevance' with
the named implementation and version; the controlled tests use costs 0, 1, 2":

> "yes, use 0 1 and 2, and I am tge authority (if this is still relevant)"

Recorded by claude-2. What this authorizes, in the terms Lean checks
(`DarkTower/WarMachine/FactorizedPreferenceSemantics.lean` 84–140) and the producer validates
(`futon2.aif.factorized-preference-sut/validate-certificate!`):

| Item | Value | Note |
|---|---|---|
| Relevance calibration | focused 0, related 1, unrelated 2 | strict order required; Joe's words |
| Calibration implementation | `:futon2/ordinal-relevance-cost-v1` version 1 | the module's named implementation; pin = its value digest |
| Calibration authority | Joe (numeric identity 3001) | must differ from the selector's authority |
| Factorization law | completion, then conditional relevance | `:completion-then-conditional-relevance` |
| Factorization implementation | `:futon2/completion-first-softmax-v1` version 1 | kernel is normalized reciprocal masses 1/(1+cost), stopped cost unrelated+1 (L5-7-D §2); the name says softmax, the bytes do not |
| Factorization authority | Joe (3001) | same person, may equal the calibration authority |
| Selector authority | the War Machine outer/inner selector (1001) | never authorizes itself |

Numeric identities (Lean `Id` is a natural number; the producer requires positive integers) are
assigned here once and are bookkeeping, not decisions: selector 1001; calibration identity 2001,
registration token 3101, receipt 4001; factorization identity 2002, receipt 4002; Joe 3001.
The durable registry is `resources/wm/preference-authorizations.edn`; this file is its
authority. Changing any value is a new authorization, not an edit.

Scope: the inner (cascade) class preference consumed at the real class scorer. The outer
selector's preference law is separate (L5-7-D §3) and is not authorized by this record.
