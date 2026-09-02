# C486 — U2: adjudicating the τ arms by retrodiction, on S2 / S3 / S4

Date: 2026-09-02. Seat: claude-cli (wm-edge worklist loop, any-lane).
Item: worklist `:U2` (class `:RUN`), from `SPEC-dormant-wiring.md` §U2.
Script: `holes/labs/wm-contract/u2_retrodiction.clj` (632 lines, clj-kondo 0/0,
check-parens OK). Artifact: `holes/labs/wm-contract/U2-RETRODICTION.txt`,
byte-identical on two consecutive runs. **Replay only — no live run, no run
lock taken, nothing written to `data/`.**

## 0. The question, and the vehicle U2 named

`:choices :pi-zero-form :values` says of the three ln E placement arms of the
eq. 2.7 β solve: *"the three arms give three different gammas and all three
converge and bracket on 20/20 ticks, and nothing measured here says which is
better."* SPEC U2 names the vehicle for saying which is better: score each
arm's policy posterior at tick t against the tick's own recorded next
observation, across S2/S3/S4, on the ground that the records carry both halves.

They do. This report runs that measurement, and its result is that **the
vehicle does not work on these fields, for a reason that is itself measured**:
the between-arm comparison is decided before the outcome arrives. §5 is that
measurement; §4 is what the arms did anyway.

## 1. The scoring rule, stated before any number was read

Each arm gives, at tick t, a policy posterior π_t over the aligned candidate
set (the same join `run7_beta_arms.clj` performs). The record at tick t+1
carries, per candidate, F_π = the horizon-one Gaussian observed-data free
energy of *that candidate's tick-t prediction* against tick-(t+1)'s actual
observation — `policy_free_energy.clj:41-151`, called live at
`war_machine.clj:300-305` with `{:absent-variance :floor}`. Lower F_π is a
better fit and exp(−F_π) is the Gaussian likelihood of the observation, so the
natural score is the log evidence of the observed next observation under the
arm's posterior used as mixture weights:

    L_mix(a,t) = ln Σ_i π_t^a(i) · exp(−F_π^{t+1}(i))          higher is better

reported beside two readings that do not assume the mixture:

    E_F(a,t)   = Σ_i π_t^a(i) · F_π^{t+1}(i)                   lower is better
    F@top(a,t) = F_π^{t+1}( argmax_i π_t^a(i) )                lower is better

and bracketed by the two degenerate arms that bound every score on the field:
ORACLE (all mass on argmin F^{t+1}) and ANTI-ORACLE (all mass on argmax
F^{t+1}). Their gap is the whole range a score can move in: **0.754047 nats on
both fields.** Every "how big is that" below is a fraction of it.

Arms scored: the three ln E placements ({neither, π only, both} — the same
three as `:pi-zero-form`) × {carried β prior, fixed β prior} at β₀ = 1.0, plus
a one-step (non-iterated) variant of each, plus three controls: the **default
law** (π = softmax(−G/τ) at the record's own τ, which is 1.0 with
`:tau-source :selection-gain-only` on every tick), **uniform**, and
**machine-choice** (all mass on the action the record's `:decision` carries).

## 2. Controls, run before the arms were read

| control | what it forecloses | result |
|---|---|---|
| 1. solver | the ln E arms resting on an unchecked reimplementation | local `:none` arm vs src `converge-beta`: Δ **0.00e+00** on both fields |
| 2. alignment | scoring a posterior against an outcome that is not its own | 19 of 19 (S2) and 3 of 3 (S4) transitions have `:previous-trace-timestamp`(t+1) = `:timestamp`(t) |
| 3. bracket | a scoring bug that lets an arm beat the oracle | **0** arm-tick scores outside [anti-oracle, oracle] |
| 4. reproduction | reading a second implementation of F_π rather than the run's | recomputed F_π(t+1) from tick t's `:ranked-actions` + tick t+1's observation vs the recorded value: max Δ **0.000e+00**, 145 candidates × 19 transitions |

## 3. Fields

- **S2** — `runs/2026-09-01-s2/wm-trace-s2.edn`, 20 records, 20 usable aligned
  fields, **19 scorable transitions**, 143–145 candidates each.
- **S4** — `runs/2026-09-01-s4/wm-trace-s4.edn`, 4 records, **3 scorable
  transitions**, 144–145 candidates each.
- **S3 — not found.** `runs/2026-09-01-s3/` holds `ARMS.txt` and `README.md`
  only; its README's first line says so ("This directory holds a REPLAY, not a
  20-tick stage run"). RUN8's two live ticks are pre-flight ticks in the shared
  per-date trace and are not a consecutive pair, so no S3 transition exists to
  score. **S3 contributes 0 transitions.** This is a recorded absence, not a
  gap in the study: RUN8 said it ran no stage run, in its own
  `:reviewer-should-check (6)`.

## 4. What the arms did

Mean over transitions. Range = 0.754047 on both fields; "% range" is
(score − anti-oracle)/range for L_mix, and (anti-oracle − F@top)/range for F@top.

### S2, 19 transitions

| arm | mean L_mix | % range | mean E_F | mean F@top | % range | max π weight | entropy |
|---|---|---|---|---|---|---|---|
| ORACLE | 19.715855 | 100.00 | −19.715855 | −19.715855 | 100.00 | 1.0 | 0 |
| none/carried | **19.188693** | 30.09 | −19.183180 | **−19.713855** | **99.73** | 0.010360 | 4.9217 |
| none/fixed | 19.188634 | 30.08 | −19.183139 | −19.713855 | 99.73 | 0.010331 | 4.9216 |
| CONTROL uniform | 19.188231 | 30.03 | −19.183245 | −19.167717 | 27.31 | 0.006902 | 4.9760 |
| pi-only/carried | 19.186082 | 29.74 | −19.181211 | −19.168149 | 27.36 | 0.126772 | 4.6685 |
| pi-only/fixed | 19.185934 | 29.72 | −19.181110 | −19.168149 | 27.36 | 0.126574 | 4.6687 |
| both/carried | 19.185966 | 29.73 | −19.181132 | −19.168149 | 27.36 | 0.126616 | 4.6686 |
| both/fixed | 19.185921 | 29.72 | −19.181101 | −19.168149 | 27.36 | 0.126557 | 4.6687 |
| CONTROL default-law | 19.179171 | 28.83 | −19.175804 | −19.150441 | 25.02 | 0.008305 | 4.9238 |
| CONTROL machine-choice | 19.167717 | 27.31 | −19.167717 | −19.167717 | 27.31 | 1.0 | 0 |
| ANTI-ORACLE | 18.961808 | 0.00 | −18.961808 | −18.961808 | 0.00 | 1.0 | 0 |

### S4, 3 transitions

| arm | mean L_mix | mean E_F | mean F@top |
|---|---|---|---|
| ORACLE | 19.716023 | −19.716023 | −19.716023 |
| CONTROL uniform | **19.189400** | −19.184536 | −19.167885 |
| none/carried | 19.188996 | −19.183522 | −19.714023 |
| none/fixed | 19.188989 | −19.183517 | −19.714023 |
| pi-only/carried | 19.186284 | −19.181475 | −19.168317 |
| both/carried | 19.186257 | −19.181455 | −19.168317 |
| CONTROL default-law | 19.179572 | −19.176234 | −19.150609 |
| CONTROL machine-choice | 19.167885 | −19.167885 | −19.167885 |
| ANTI-ORACLE | 18.961976 | −18.961976 | −18.961976 |

**Read it in this order.**

1. **No arm has predictive skill over uniform, and the study's own two fields
   disagree about the sign.** On S2 the best arm (`none/carried`) beats a
   uniform posterior by 4.6e-4 nats — 0.06% of the range. On S4 uniform beats
   *every* arm, `none/carried` included, by 4.0e-4. A ranking that flips
   between a 19-transition field and a 3-transition field of the same machine
   is not a ranking.
2. **The three arms are not three.** `pi-only` and `both` put ln E in π
   identically and differ only through γ, so they share an argmax on 19 of 19
   and 3 of 3 transitions and their L_mix differs by at most **2.083e-04**
   (S2) / **3.932e-05** (S4) — 0.03% and 0.005% of the range. The separation
   that exists is `none` against the other two (max |ΔL_mix| 2.785e-03; a
   different argmax on 19 of 19 and 3 of 3), and that is the question of
   whether ln E enters π at all, which both sources answer yes and which is
   U3's, not U2's.
3. **F@top splits sharply while L_mix barely moves**, and the split is
   informative: `none`'s argmax lands within 0.002 of the field's best
   retrodictor (99.73% of the range), `pi-only`/`both` at 27.36%, the default
   law at 25.02% — *below* uniform's 27.31%. §5 explains why that is not a
   verdict about selection quality.
4. **The default law is the worst non-degenerate arm on all three scores**,
   at 22 of 22 transitions. It is also the only arm whose weighted mean F
   (−19.175804) is worse than the field mean F (−19.1832): the candidates
   softmax(−G/τ) prefers fit the next observation worse than an average
   candidate does.
5. **The machine's own choice scores last of everything but the anti-oracle.**
   Recorded as a fact about the records, not a claim about the selection rule:
   the action each `:decision` carries has `:decision :rank` 1 and
   `:selection-boundary :reason-bearing-strategic-policy`, while the same
   action sits at `:ranked-actions` rank 115 or 123 of 145 — two ranks over
   different sets. The code site of that boundary was not chased; it is
   outside U2.

### The two sub-questions U2 asked to be read against the arms

**Carried vs fixed β prior** (the across-tick half of `:choices
:gamma-fixed-point`, Joe's `:carried-prior` ruling). β₀ swept, since C468 found
the J4 rank movement concentrated at β₀ = 0.5 and a result at β₀ = 1.0 alone
would be a result about one initial condition:

| β₀ | arm | mean ΔL_mix (carried − fixed) | max \|ΔL_mix\| | ticks with a different argmax |
|---|---|---|---|---|
| 0.5 | none / pi-only / both | +2.930e-04 / +6.620e-04 / +2.220e-04 | 5.702e-04 / 1.240e-03 / 4.331e-04 | **0 / 0 / 0 of 19** |
| 1.0 | none / pi-only / both | +5.939e-05 / +1.478e-04 / +4.429e-05 | 1.137e-04 / 2.805e-04 / 8.498e-05 | **0 / 0 / 0 of 19** |
| 2.0 | none / pi-only / both | +4.853e-06 / +2.058e-05 / +3.563e-06 | 8.252e-06 / 3.879e-05 / 6.052e-06 | **0 / 0 / 0 of 19** |
| 5.0 | none / pi-only / both | −3.855e-07 / +9.575e-07 / −2.919e-07 | 1.007e-06 / 1.614e-06 / 7.591e-07 | **0 / 0 / 0 of 19** |

Same on S4, 0 of 3 at every β₀. The largest effect anywhere is 1.240e-03 —
0.16% of the range — and it never reaches an argmax. Retrodiction does not
separate carrying from resetting.

**Iterated fixed point vs one step** (the within-tick half, which
friston2017.txt:683-685 settles by fiat: "usually one would iterate the
equalities in equation 2.7 until convergence"). Ran anyway, so that "the source
settles it" has a number beside it: max |ΔL_mix| **1.530e-07** (S2) /
**1.165e-07** (S4), 0 argmax differences on 22 of 22. On these fields the
iteration the source requires costs nothing and buys nothing measurable. The
source still settles it; nothing here contradicts it.

## 5. Why the vehicle does not work here — the counterfactual control

Re-score every arm against **the observation the machine had already seen at
tick t**, instead of the one that arrived at t+1. Everything an arm could know
before the outcome is in that counterfactual; whatever survives the difference
is the only part of the score that measures retrodiction.

| field | largest movement of any arm's mean score | spread of that movement **across** arms | largest change in **any** arm-against-arm gap |
|---|---|---|---|
| S2 | 1.701e-04 | **1.066e-14** | **1.066e-14** |
| S4 | 3.848e-04 | 3.874e-04 | 3.874e-04 |

**On S2 the realised outcome enters every arm's score as the same constant, to
machine precision.** Swapping the outcome for one already known changes no
arm-against-arm comparison at all — 1.066e-14 nats. On S4, where the field is
shorter, it changes comparisons by up to 3.874e-04, which is still an order of
magnitude *larger* than the entire `pi-only`-vs-`both` separation (3.932e-05)
and 7× smaller than the `none`-vs-the-others separation (2.751e-03).

The mechanism, decomposed. F_π = Σ_ch ½·ln(2πv_ch) + Σ_ch ½·(o_ch − m_ch)²/v_ch;
call the first sum A (the candidate's declared variances alone) and the second
B (the only part that can depend on what was observed).

- **A's spread across candidates is 0.693147 = ln 2, at every one of the 22
  transitions** — 91.9% of F_π's whole 0.754047 spread. The extreme candidates'
  variance products differ by exactly a factor of 4, and nothing else about
  them enters.
- **B's spread is 0.062500, and B's mean is 0.0032–0.0043.**
- Re-computing B against the *previous* tick's observation moves it by at most
  **1.107e-03** — and that movement is nearly common to all candidates, which
  is why the arm gaps move by 1e-14.

And the reason B is inert is that the world does not move where the candidates
predict:

- The three channels any candidate's action model predicts are
  `:mission-health`, `:sorry-count-norm`, `:ticks-firing-ratio`. **All three
  are constant to 0.000000 across all 20 S2 ticks and all 4 S4 ticks.**
- The channels that *do* move are `:loop-health` (14 distinct values, range
  0.0234), `:mathematics-pct`, `:portfolio-pct`, `:stack-pct` (8 distinct
  each, ranges 3e-6 to 1e-5). **No candidate predicts any of them**; every one
  enters F_π at the 0.01 variance floor, identically for every candidate.
- So the observation genuinely varies — 18 of 20 S2 observations are distinct,
  as C468 reported — and it varies **only in coordinates orthogonal to every
  prediction on the field.**

That is the finding U2's vehicle ran into. It is the same shape as the
selection-gain fold that has never fired (`selection_gain.clj:185-193`, the
`:realized-outcome` field "ABSENT today, sim-only"): retrodiction and the fold
both need realised outcomes on the channels the actions claim to touch, and
this apparatus supplies none.

## 6. Recommendation, and what the evidence cannot reach

**A typed no-discrimination result.** Not "the arms give the same numbers" —
they do not — but: *on these fields the arm ordering is fixed before the
outcome arrives, so the ordering is not evidence about the arms.* By SPEC U2's
own rule ("If the arms do not discriminate on retrodiction, choose by source
fidelity"), each open choice falls back to its source:

1. **The π₀ placement (`:pi-only` vs `:both`) — recommend `:habit-prior-in-both`,
   grounds: source fidelity.** No discrimination on any score, at any β₀, on
   either field (0 of 22 argmax differences; ΔL_mix ≤ 2.083e-04 = 0.03% of the
   range) — and this holds *before* §5 is applied, so it does not even depend
   on the counterfactual argument. SPM's own implementation puts ln E in both
   (`spm_MDP_VB_X.m:963-964`, verified in clone by C463), and that is already
   what ships as the `:pi-zero-form` `:interim`. **This changes nothing; it
   removes the reason the entry was open.**
2. **The β prior across ticks — Joe's `:carried-prior` ruling stands,
   unchallenged and unsupported by these numbers.** 0 of 22 argmax differences
   at every β₀ ∈ {0.5, 1, 2, 5}. Consistent with C464/V6's "the argmax never
   moves". The ruling rests where it rested: on the tick-grain argument
   (`:grounds-audit`, C463), not on a measurement.
3. **The within-tick iteration — the source settles it and the numbers do not
   object** (max |ΔL_mix| 1.530e-07).
4. **The τ law itself (`:temperature-update`) — NOT adjudicated, and this
   study cannot adjudicate it.** The variational arms' posteriors do retrodict
   better than the default law's at 22 of 22 transitions (mean L_mix
   19.1859–19.1887 against 19.1792), but so does a uniform posterior, by more
   than two of the three arms manage. A law that loses to uniform is not shown
   better by that comparison. **The one thing that survives §5 as a fact about
   the machine rather than about the score**: the default law's top candidate
   has a worse-than-field-average F_π at 22 of 22 transitions (F@top −19.150441
   against a field mean of −19.1832 on S2), and under §5's decomposition that
   is predominantly a statement about that candidate's declared variances, not
   about its residual against anything observed.

**What would make U2 answerable.** A field in which the channels the action
model predicts actually move — i.e. the same precondition as the selection-gain
fold's `:realized-outcome`, and the same obstruction RUN4 is blocked on. Until
then, "score the arms against the recorded next observation" is a measurement
of the candidates' declared variances wearing an outcome's name. **Do not
re-run this study on a longer replay of the same apparatus**: length is not the
missing thing, and a 674-tick version of these tables would carry the same
1e-14.

**No ruling is written by this row.** §6 is a recommendation with its grounds;
the decision stays Joe's.

## 7. Bounds

- **22 transitions**, one route, one apparatus. S2's 20 ticks are the same path
  (RUN3: 180 hops, 9 distinct, all twenty ticks the same route); S4 is four
  ticks.
- **β₀ swept for the carry question only.** The ln E arms were compared at
  β₀ = 1.0, the value both stage runs used.
- **F_π is the horizon-one Gaussian term only** — it is not the τ≥2 trajectory
  sum (`policy_free_energy.clj:41-151`'s own last paragraph). A different F_π
  could in principle put weight where this one puts none; nothing here bounds
  that.
- **The `:absent-variance :floor` choice is the run's, not this study's**
  (`war_machine.clj:300-305`). It is what makes 11 of 14 channels enter every
  candidate's F_π identically. A `:reject` run would have no F_π at all
  (the docstring measures this: it rejects on the first moved absent channel),
  so `:floor` is not an alternative that could have been avoided — but the
  A-dominance in §5 is downstream of it, and a study on a field where the
  action model predicts more channels would not inherit it.
- **The counterfactual in §5 uses the previous tick's observation as the
  "already known" outcome.** It is not a null model of the world; it is the
  cheapest outcome that the arm could not have been scored on and still gives
  the same answer.
