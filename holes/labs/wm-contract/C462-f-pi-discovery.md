# C462 — discovery: past-observation free energy by policy

Date: 2026-09-01  
Reader: codex-22  
Scope: worklist I2, slice (a); discovery only

## 1. What B.4 and B.9 say

Parr, Pezzulo, and Friston print the policy update as

> \(\nabla_\pi F = 0 \Leftrightarrow \pi = \sigma(\ln E - F - G)\).

This is equation B.9 on p. 247 (`parr2022.txt:12654-12664`). The immediately
preceding text says that the vector \(F\) has elements \(F_\pi\) defined by
B.4 (`parr2022.txt:12659-12664`).

Equation B.4 on p. 245 is printed as

> \(F_\pi = s_{\pi1}\!\cdot(\ln s_{\pi1}-\ln A\!\cdot o_1-\ln D)
> + \sum_{\tau=2} s_{\pi\tau}\!\cdot(\ln s_{\pi\tau}-\ln A\!\cdot o_\tau
> -\ln B_{\pi\tau}s_{\pi,\tau-1})\).

The extracted typography is at `parr2022.txt:12560-12563`. B.3 defines
\(Q(\tilde s\mid\pi)=\prod_\tau Q(s_\tau\mid\pi)\), the observation likelihood
\(P(o_\tau\mid s_\tau)=\mathrm{Cat}(A)\), and policy-conditioned transitions
\(P(s_{\tau+1}\mid s_\tau,\pi)=\mathrm{Cat}(B_{\pi\tau})\), with initial-state
prior \(D\) (`parr2022.txt:12528-12535`). Thus \(F_\pi\) is a function of the
observed trajectory \(o_{1:T}\), posterior state beliefs \(s_{\pi\tau}\),
likelihood \(A\), policy-conditioned transitions \(B_{\pi\tau}\), and initial
prior \(D\). Its displayed sum is over time \(\tau=2,\ldots,T\); each dot
product is also an expectation/sum over hidden-state alternatives, as the book
explains directly after B.4 (`parr2022.txt:12566-12573`). It is past-observation
model evidence for each policy, not expected free energy of future outcomes
(`parr2022.txt:12630-12646`).

## 2. What the WM predicts and retains

The WM already makes a prediction separately for every candidate action.
`forward-model/predict` maps one `(state, action)` to a next-observation mean
and variance, next belief, action, and predicted events
(`src/futon2/aif/forward_model.clj:311-356`). `predict-multi-horizon` repeats
the *same action* for K steps and returns a trajectory plus final state
(`src/futon2/aif/forward_model.clj:279-309`).

`compute-efe` invokes the single-step predictor and optionally the repeated-
action multi-horizon predictor (`src/futon2/aif/efe.clj:601-614`). It returns
the single-step prediction in each candidate evaluation under `:prediction`
(`src/futon2/aif/efe.clj:829-844`), and `rank-actions` maps `compute-efe` over
all included candidate actions (`src/futon2/aif/efe.clj:903-921`). The arena
therefore has per-candidate predictions in the in-memory `wm-ranked` value at
the current tick (`scripts/futon2/report/war_machine.clj:4536-4567`).

**No durable WM structure retains those predictions across ticks.** The judge
result includes current `:ranked-actions` (`scripts/futon2/report/war_machine.clj:4768-4784`),
but the durable trace explicitly drops each ranked action's deeply nested
`:prediction` (`src/futon2/aif/trace.clj:76-81`) before writing the trace's
`:ranked-actions` (`src/futon2/aif/trace.clj:425-455`). Consequently an
observation at tick \(t+1\) cannot be matched from the trace to every
candidate's prediction at tick \(t\). Re-running `predict` later is not a
record of what the running machine actually predicted with the then-loaded
model and state.

## 3. The missing quantity and state

The quantity required by B.9 is one observed-data variational free energy per
candidate-policy model, \(F_\pi(o_{1:T})\), equivalently an approximation to
\(-\ln P(o_{1:T}\mid\pi)\) after optimizing the policy-conditioned posterior
state beliefs (`parr2022.txt:12509-12522`). It is not a posterior *over*
candidates; B.9 uses the vector of per-policy model evidences to form that
posterior (`parr2022.txt:12637-12664`).

At minimum, a tick record would need a durable prediction ledger written with
the ranked-action trace: for each stable candidate identity, record the source
tick, action, horizon/grain, predicted observation distribution at each
horizon, the corresponding predicted/posterior state belief, and model/version
provenance. At tick \(t+1\), the actual observation and its source tick would
be joined to the ledger entry and yield `:F-policy-observed` for each candidate.
The natural write seam is the trace construction where `:ranked-actions` is
currently stripped (`src/futon2/aif/trace.clj:425-455`); the source material is
already attached by `compute-efe` (`src/futon2/aif/efe.clj:829-844`). A full
B.4 trajectory additionally needs the sequence of \(s_{\pi\tau}\), \(A\),
\(B_{\pi\tau}\), and \(D\), not merely predicted means
(`parr2022.txt:12528-12560`).

## 4. Why the existing scalar F is not F_pi

The existing function computes
\(F=\tfrac12\operatorname{mean}_k(\Pi_k\epsilon_k^2)\) over the entries of one
`prediction-errors` map (`src/futon2/aif/free_energy.clj:184-206`). Each entry
is a channel's observed-minus-predicted error and precision
(`src/futon2/aif/free_energy.clj:155-182`). The arena calls it once on the
shared current `prediction-errors` (`scripts/futon2/report/war_machine.clj:4450-4452`)
and stores one scalar as `:variational-free-energy`
(`scripts/futon2/report/war_machine.clj:4740-4754`).

Therefore B.4's \(F_\pi\) cannot be derived per candidate from those same
channel errors. The implemented scalar sums/means over observation **channels**
for one current belief prediction; B.4 sums over **trajectory time and hidden
states**, using a distinct posterior and transition model conditioned on each
policy (`parr2022.txt:12528-12573`). Copying the same scalar into every
candidate would produce a constant vector and cancel from the softmax.

## 5. Grain

**B.4's trajectory-level \(F_\pi\) is not presently well defined at the WM's
candidate grain.** `compute-efe` scores a single `(state, action)`
(`src/futon2/aif/efe.clj:903-921`), while its multi-horizon path repeats that
same action rather than representing an action sequence or reactive policy
(`src/futon2/aif/forward_model.clj:279-309`). The code therefore supplies
neither a policy trajectory nor the policy-conditioned state beliefs and
transitions required by B.4 (`parr2022.txt:12528-12560`).

The smallest honest version is a declared horizon-one policy `[a]` for each
candidate action. Persist every candidate's tick-\(t\) predictive distribution,
then score the common tick-\(t+1\) observation under each candidate model. In
B.4 this retains only the initial/one-step term, with no \(\tau\ge2\) trajectory
sum (`parr2022.txt:12560-12563`). It need not be degenerate because `predict`
can return action-specific means, variances, beliefs, and events
(`src/futon2/aif/forward_model.clj:311-356`). By contrast, the tempting version
that assigns today's single channel-error scalar to every action is necessarily
degenerate and changes no posterior, because that scalar is computed only once
before candidate ranking (`scripts/futon2/report/war_machine.clj:4450-4452,4536-4537`).

The current selector implements \(Q(a)\propto\exp(\ln E(a)-G(a)/\tau)\)
(`src/futon2/aif/policy.clj:82-104`) and exposes those weights in the decision
(`src/futon2/aif/policy.clj:285-298`). Adding a non-degenerate horizon-one
observed-data term would require an explicit decision about its temperature
placement; B.9 itself prints `ln E - F - G`, without the WM's engineering
temperature transform (`parr2022.txt:12659-12664`).
