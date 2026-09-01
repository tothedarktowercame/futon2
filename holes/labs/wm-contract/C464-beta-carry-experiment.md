# C464 — beta carry experiment on recorded WM fields

Date: 2026-09-01  
Seat: codex-22  
Scope: the 38 forms in `data/wm-trace/wm-trace-2026-07-04.edn` and the 18
forms in `data/wm-trace/wm-trace-2026-07-05.edn`.

## Result

The carried- and fixed-prior arms differ.  Depending on field and initial
beta, their computed candidate order differs on 14–36 transitions.  Neither
arm changes the computed argmin/argmax action relative to the tau=1 control on
any of the 54 transitions.  This is an ordering experiment over `:G-total`,
not a reconstruction of the machine's historical ranking: the records do not
carry the `:controller-score` consumed by policy selection
(`src/futon2/aif/policy.clj:416-421`).

Carry does not diverge on these fields.  Across all starts its posterior stays
in `[0.281370, 5.576647]`, far from the solver bracket `[1e-6, 1e6]`.  The
July-04 trajectories spike and then converge near 0.453 regardless of their
start.  July-05 is mixed: the 0.5 start mostly falls to 0.281, the 5.0 start
mostly rises to 5.577, and the middle starts are non-monotone.  This finite
record therefore shows path dependence, but neither monotone runaway nor an
approach to the floor or ceiling.

## Method and bounds

The committed companion program reads every top-level form with repeated
`clojure.edn/read`; counts were 38 and 18, not one.  Each row below describes
37 or 17 adjacent-record transitions.  At transition *t*, it derives every
candidate's horizon-one prediction using `forward-model/predict`
(`src/futon2/aif/forward_model.clj:311`), from record *t*'s own observation and
belief, and scores that prediction against record *t+1*'s observation using
`f-pi-vector` (`src/futon2/aif/policy_free_energy.clj:146`) with
`{:absent-variance :floor}`.  These are reconstructed predictions; they were
not persisted by these pre-I3 traces.

For both experimental arms, `converge-beta`
(`src/futon2/aif/policy_precision.clj:82`) receives the reconstructed F-pi
vector and recorded `:G-total` vector.  The carried arm feeds the converged
posterior into the next transition.  The fixed arm supplies the declared
beta-zero every time.  Both start from the same beta-zero at transition zero,
so that transition cannot distinguish carry from reset.  The control orders
the same `:G-total` vector at tau=1 and applies no F-pi term; it is neither beta
arm.

Every reported "rank" is therefore a **computed G/F-pi proxy rank**.  The
persisted ranks cannot be reconstructed: their controller-score ordering is
known to agree with G-total order at only 2 of 110 positions in the measured
field.  Candidate counts also vary by tick (the changed-rank counts below can
exceed 110).  No enactment conclusion is drawn from a narrow act gate.

Reproduction (redirection only; no pipeline):

```sh
cd /home/joe/code/futon2
clojure -M holes/labs/wm-contract/c464_beta_carry_experiment.clj > /tmp/C464-results.edn
```

Exit code was 0.  The output contains, for every cell, the exact beta-prior,
beta-posterior, gamma (`1 / beta-posterior`), iteration, bracket/convergence,
rank-change, and argmax-change trajectories.  Tick indices in it are
zero-based transition indices.

## Summary by field, arm, and declared beta-zero

`beta range -> final` summarizes the exact posterior trajectory printed by the
program.  A fixed arm's prior trajectory is the named beta-zero repeated at
every transition; a carried arm's prior trajectory is beta-zero followed by
the preceding posterior.  `rank count range` is the range, across transitions,
of candidates whose proxy rank differs from control.  `arms differ` compares
the full carried and fixed proxy orderings.

| field | beta0 | arm | beta range -> final | iterations | rank count range | largest move | arms differ | argmax moves |
|---|---:|---|---|---|---|---:|---:|---:|
| 07-04 | 0.5 | carried | 0.452876–2.755913 -> 0.452876 | 48–51 | 49–139 | 82 | 18/37 | 0 |
| 07-04 | 0.5 | fixed | 0.430785–2.379041 -> 0.431055 | 41–51 | 49–99 | 62 | — | 0 |
| 07-04 | 1.0 | carried | 0.452986–2.767622 -> 0.452986 | 42–51 | 53–139 | 82 | 35/37 | 0 |
| 07-04 | 1.0 | fixed | 0.764677–2.735161 -> 0.767096 | 45–51 | 53–109 | 62 | — | 0 |
| 07-04 | 2.0 | carried | 0.453209–3.046035 -> 0.453209 | 45–51 | 67–139 | 82 | 36/37 | 0 |
| 07-04 | 2.0 | fixed | 1.547507–3.310085 -> 1.999782 | 40–51 | 67–142 | 82 | — | 0 |
| 07-04 | 5.0 | carried | 0.453798–4.970568 -> 0.453798 | 39–51 | 69–139 | 100 | 35/37 | 0 |
| 07-04 | 5.0 | fixed | 3.994993–5.489417 -> 5.042390 | 46–51 | 80–140 | 137 | — | 0 |
| 07-05 | 0.5 | carried | 0.281370–0.499992 -> 0.281370 | 47–50 | 35–120 | 84 | 15/17 | 0 |
| 07-05 | 0.5 | fixed | 0.387607–2.092059 -> 0.433230 | 47–51 | 65–120 | 91 | — | 0 |
| 07-05 | 1.0 | carried | 0.757250–2.295934 -> 1.143923 | 42–50 | 97–143 | 95 | 14/17 | 0 |
| 07-05 | 1.0 | fixed | 0.749143–2.444623 -> 0.753164 | 46–50 | 74–120 | 91 | — | 0 |
| 07-05 | 2.0 | carried | 1.582912–2.871493 -> 1.692511 | 47–51 | 99–143 | 95 | 15/17 | 0 |
| 07-05 | 2.0 | fixed | 1.582912–3.013467 -> 2.025518 | 43–50 | 99–141 | 91 | — | 0 |
| 07-05 | 5.0 | carried | 4.830195–5.576647 -> 5.576647 | 44–50 | 98–143 | 135 | 14/17 | 0 |
| 07-05 | 5.0 | fixed | 4.761027–5.302700 -> 5.058436 | 47–50 | 98–143 | 135 | — | 0 |

All 432 solves (54 transitions × 4 starts × 2 arms) were bracketed and
converged.  The gamma-at-each-tick vectors are printed in `/tmp/C464-results.edn`
by the reproduction command rather than rounded independently here.

## What these fields can and cannot decide

- They separate carry from reset after the common initial condition: full
  proxy orderings differ in every field/start combination.  Agreement at
  transition zero is structural, not evidence for agreement between arms.
- They show no action change at the computed proxy argmax.  Thus the carry
  ruling changes many lower ranks here but would not change the selected
  candidate under an un-gated argmax enactment of this proxy.
- They cannot establish what the historical WM would have selected, because
  `:controller-score` is absent.  Nor do they test unscaled versus by-tau F-pi:
  the control's tau is exactly 1, where those formulas coincide.
- The carry trajectories neither approach the numerical floor nor ceiling.
  July-04 strongly forgets beta-zero; July-05 retains enough path dependence
  for different starts to finish at materially different posteriors.  More
  fields would be needed to distinguish stable field dependence from long-run
  drift.
