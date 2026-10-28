**This changes the specification** (`DarkTower/WarMachine/R20*.lean`); Joe
directed the change on 2026-10-28 ("re-architect the lean and then get back
to work re-architecting the code").

# SPEC: R20 as one step function (2026-10-28)

Owner: claude-11. Agreed with Joe 2026-10-28 (M-giraffe session, turns
12:11–12:20). Lean first, then the Clojure follows the Lean.

## What changes

Today `DarkTower/WarMachine/R20Run.lean` describes a run as a sequence of
click records `c : ℕ → Click Policy` plus a predicate `Carries L a b` that
says what one click must hand the next, and `RunCertified` collects those
conditions. The records can hold any values; the theorems say what follows
*if* the conditions hold.

The rebuild makes the run something we compute:

    step : Makeup → State → Observation → Click × State
    run  : Makeup → List Observation → List Click     -- fold of step from init m

- **Makeup**: fixed for the whole run, declared once (the affective level
  `L`, the target relation, the likelihood `A`, the prior strengths, κ).
- **State**: everything one click passes to the next: `d` counts, valence
  belief, habit slow state, θ slow state, preference counts `c`, previous
  focus, run id, and later the selection record and the previous verdict.
- **Observation**: what arrives at a click from outside: the hidden-state
  observation, the click's outcomes, its ending class.
- **Click** has no field for an outside candidate list, menu or selection.
  Anything a click uses is an observation at that click, a value in `State`,
  or part of `Makeup`.

Each theorem is about what `step` produces, for every makeup and every
observation sequence. It is proved by unfolding `step`, not by assuming a
predicate. There are no proofs against invented bad runs
(memory: constructive-validation-not-negative-controls).

## Preferences C are learned (Joe 2026-10-28)

C moves from `CTauClassPreference.terminalPreference` (a constant) into
`State` as Dirichlet counts `c` over the four terminal classes (focused,
related, unrelated, stopTheLine; `notYetEvaluated` is not a terminal outcome,
so it gets no count and the waiting-step preference is unchanged).

- Prior: `c₀ = κ · (55, 35, 5, 5)/100`, κ > 0 in `Makeup`.
- Update: each click adds one count for its observed ending class. This is
  the Dirichlet count update `c ← ω·c + η·o` with η = ω = 1 (no learning
  rate below 1, no forgetting). Sources, checked 2026-10-28 [1–3]:
  [1] gives the rule for d (Eq. 34) and a (Eq. 37) and says only that
  "analogous update rules apply to the other model parameters (B, C, E)";
  [2] learns outcome preferences by accumulating Dirichlet parameters (flat
  prior, 1 per option); [3] §4.1 writes the C update out as `d + α·s` with
  α a learning rate. Our kit prior κ·(55,35,5,5)/100 replaces their flat one.
- C at the click is `c / Σ c`, and it takes the place of `terminalPreference`
  in `classPreference`.
- Theorem: at click n, `c = c₀ + (counts of ending classes in obs₀ … obs_{n-1})`.

Later upgrade, not in this rebuild: hierarchical C, where a higher level
sets the lower level's preferences (the "hierarchical contextualisation" of
[4]; it is a conceptual account, not a learning scheme for C), or Bayesian
model reduction of `c` [5] (pattern `aif/structure-learning-by-model-reduction`). `Makeup` keeps room for this:
the update rule is one named function.

### References for C

1. Smith, R., Friston, K. J. & Whyte, C. J. (2022). A step-by-step tutorial
   on active inference and its application to empirical data. *Journal of
   Mathematical Psychology* 107, 102632. doi:10.1016/j.jmp.2021.102632
   (§4, "Modeling learning", Eqs. 32–37).
2. Sajid, N., Ball, P. J., Parr, T. & Friston, K. J. (2021). Active
   inference: demystified and compared. *Neural Computation* 33(3), 674–712.
   doi:10.1162/neco_a_01357. arXiv:1909.10863.
3. Sajid, N., Tigas, P., Zakharov, A., Fountas, Z. & Friston, K. (2021).
   Exploration and preference satisfaction trade-off in reward-free
   learning. arXiv:2106.04316 (§4.1).
4. Pezzulo, G., Rigoli, F. & Friston, K. J. (2015). Active Inference,
   homeostatic regulation and adaptive behavioural control. *Progress in
   Neurobiology* 134, 17–35. doi:10.1016/j.pneurobio.2015.09.001.
5. Friston, K., Parr, T. & Zeidman, P. (2018). Bayesian model reduction.
   arXiv:1805.07092.

Local copies: `~/notes/papers/` (not committed).

## Slices (one handoff each, reviewed between)

1. **`R20Step.lean`, carried counts.** New module beside `R20Run.lean`, which
   is left untouched. Defines `Makeup`, `State`, `Observation`, `Click`,
   `init`, `step`, `run`. Covers the quantities whose update needs no
   within-click inference beyond Bayes: `d` (evidence posterior is
   `EvidenceFromTheProcess.posterior` of the `d`-mean prior, `A` and the
   observation, computed inside `step`), habit (`R15TemporalHierarchy.advance`),
   θ (`PatternTheta.advanceWitnessed`), learned `c`, focus, run id.
   Theorems: the closed forms at click n for `d`, habit, θ and `c`; the
   evidence row of every click conforms; run ids increase; each click's
   `previousFocus` is the previous click's focus.
   Precision and valence are slice 2; the module says so.
2. **Precision and valence inside `step`.** β prior from `descend L valence`;
   β̄ computed inside `step` from E, G, F (`PrecisionFromObservation`);
   valence update by `valenceUpdate`/`predict`. Port `run_betaPrior_step`,
   `zero_charge_keeps_valence`, `run_betaPrior_constant_of_flat`.
3. **Menu and selection from State.** `OuterSelection` and the policy menu
   become values `step` computes from the selection record in `State`,
   the way θ counts are learned.
4. **Verdict as observation.** The certificate verdict at click n is computed
   in `step` and is part of what click n+1 sees.
5. **Rows as theorems about `step`.** One theorem per R20 row (the active-
   inference rows and, separately, liveness, independence, joint action,
   cost). Then show the old `RunCertified` holds of `run m obs`, so the
   existing results become corollaries, and retire the free `Click` records.
6. **Clojure follows.** futon2's click code is rebuilt to the same shape
   (one step function over a state map), aligned row by row with the Lean.

## Gates per slice

`lake build DarkTower.WarMachine.R20Step` (or the slice's module) with no
`sorry`, no new `axiom`; `#print axioms` on the headline theorems shows only
`propext`, `Classical.choice`, `Quot.sound`. Warrant with the commit sha.
