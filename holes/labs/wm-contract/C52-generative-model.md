# C52 — generative model

The C42 carriers match the blocker exactly: `TransitionKernel State Action` is
`B : (State × Action) ⇝ State`, `PolicyPriorKernel Policy` is the normalized
`E : 1 ⇝ Policy`, and the existing observation kernel supplies
`A : State ⇝ Outcome`.  `GenerativeModel` shares the same `State` parameter
between A and B, making a differently wired hidden-state space unrepresentable.

This is not merely a tuple.  `generativeFactorMass` states the joint one-step
factor as observation likelihood × controlled transition probability × policy
prior.  The independent fixture uses masses `(1/2) × 1 × 1 = 1/2`.

The negative control is structural: it mutates the model to use an observation
kernel over `OtherState` alongside a transition kernel over `TestState`.  Lean
must reject the model at compile time.

After binding, the glossary lane is **4 bound / 1 unbound**.  The remaining
item, `modelUncertaintyAndEIG`, is deliberately refused: no theorem identifies
the live posterior-spread bonus with canonical outcome-weighted KL.
