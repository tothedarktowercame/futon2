# R14 → R5, round 2

## What R5 asked for that I can supply

- I already emit applied selection gain, effective temperature, temperature
  mode, and the score spans used in the temperature-dependent calculation
  (`src/futon2/aif/policy.clj:209-224`).
- I already name the live boundary, chosen action, controller score, and the
  fact that actuation remains with downstream gates
  (`src/futon2/aif/policy.clj:259-271`).
- I can preserve R5's original vector as an audit input while separately
  reporting the temperature-dependent counterfactual order. The policy already
  builds the latter without mutating `ranked-actions`
  (`src/futon2/aif/policy.clj:240-253`).
- I accept R5's corrected authority split. The receipt must say
  `:controller-order-authority :live` and
  `:temperature-order-authority :counterfactual-only`; `chosen` comes from the
  first non-no-op in R5's order (`src/futon2/aif/policy.clj:247-253`).
- Given an immutable score-field identity from R5, I can echo it in a receipt
  and run the same field at two gains. Adding that identity and sensitivity
  witness is proposed work; neither field exists today.

## What R5 asked for that I cannot supply

- I cannot supply the source score-vector identity today because R5 does not
  produce one and the arena passes only the vector
  (`scripts/futon2/report/war_machine.clj:4513-4518`). Inventing a digest after
  receipt would not identify R5's act of scoring.
- I cannot guarantee preservation of support exclusions. R5 stores them in
  vector metadata, then the arena maps and filters into new vectors before my
  call (`src/futon2/aif/efe.clj:855-862`,
  `scripts/futon2/report/war_machine.clj:4488-4513`).
- I cannot certify a malformed score field or finite scores at my present
  boundary. `effective-temperature` coerces values with `double`; it is not a
  score-schema validator (`src/futon2/aif/policy.clj:71-80`). R5 must validate
  at production, and I must validate the tagged delivery at admission.
- I cannot honestly assign transport retry or timeout semantics to today's
  direct pure call. A retry would simply recompute; no receipt or idempotency
  authority exists. I agree these Delivery fields should record observed
  `:not-applicable/in-process`, not invented milliseconds.

## Where R5's picture of me is wrong

R5 says empty input should become explicit refusal rather than a plausible
temperature and cites `adaptive-temperature` returning `tau-min`. At my actual
top-level boundary, empty `ranked-actions` is intercepted before temperature is
computed and returns the disjoint `:abstain :no-candidates` branch
(`src/futon2/aif/policy.clj:360-365`). The defect is narrower but important:
that branch cannot distinguish no proposals, all candidates excluded, or a
lost/failed score delivery.

R5 also asks me to preserve its admitted set and ordering. I can preserve them
as evidence, but I must not promise that R14 never derives another order: that
is exactly what temperature and habit priors calculate. The contract must keep
`:r5/controller-order` and `:r14/temperature-order` separate, then state which
one had live authority. Conflating them would make today's counterfactual look
like the live choice.
