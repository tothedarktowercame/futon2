# C468 — the J4 arms and the π₀ arms, on the machine's own field (RUN7 / stage S2)

Date: 2026-09-01. Seat: claude-20 (CLI work seat, wm-build-loop).
Field: `holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn` — 20 records,
code sha `039b0b8`, β₀ = 1.0. Raw output: `runs/2026-09-01-s2/ARMS.txt`.
Script: `holes/labs/wm-contract/run7_beta_arms.clj`.

## What is new against C464, which asked these two questions in July

C464 could only order candidates by `:G-total`, because the July records did not carry the
`:controller-score` that policy selection consumes, and it had to **reconstruct** each
candidate's horizon-one prediction with the forward model because those records predate I3.
Both limits are gone. S2's records carry `:controller-score` per candidate, and their F_π was
scored by the tick itself against **persisted** predictions. Every rank below is the machine's
own rank, not a proxy for it — the C464/C22 rank-proxy caveat does not apply to this file.

## 1. The J4 arms: carry against reset

Carried arm — β_prior is the previous tick's converged posterior. Fixed arm — β_prior is β₀
every tick. Both solved 20/20, converged and bracketed, at every β₀.

| β₀ | carried β, tick 1 → 20 | ranks moved (max) | ticks where the argmax differs |
|----|------------------------|-------------------|-------------------------------|
| 0.5 | 0.502445 → 0.544606 | 0 → 42 of 145 (max move 41) | **0 of 20** |
| 1.0 | 1.002646 → 1.034342 | 0 throughout | **0 of 20** |
| 2.0 | 2.002454 → 2.011969 | 0 throughout | **0 of 20** |
| 5.0 | 5.002114 → 4.985734 | 0 throughout | **0 of 20** |

**The arms differ, and the difference does not reach the decision.** This reproduces C464's
result on a field where the rank is the machine's own rather than a G-total proxy, which is
what RUN7 asked for. The rank movement is concentrated entirely at β₀ = 0.5: at γ ≈ 2 the G
term is weighted twice as heavily, so a 8% β difference reorders up to 42 of 145 candidates;
at γ ≈ 1 and below, a 3% β difference reorders none.

**Direction depends on β₀, and that is the interesting part.** At 0.5, 1.0 and 2.0 the carry
drifts **up**; at 5.0 it drifts **down**. So on this field the carry is contracting toward
some value inside (0.5, 5.0) rather than running away — consistent with C464's July-04
trajectories converging near 0.453, and with no floor or ceiling approached at any β₀.
**Twenty ticks is nowhere near that contraction** (C22): the 0.5 and 5.0 arms are still 4.44
apart at t=20, having started 4.5 apart. The carry closed 1.3% of the gap.

## 2. The π₀ arms: where ln E enters

I1 slice b1 recorded the choice — SPM computes `pu = spm_softmax(qE + w*Q)`
(`spm_MDP_VB_X.m:964`), carrying the habit prior in π₀ as well as π; `friston2017.txt:684`
gives π₀ = σ(−γG) with no E; the shipped `converge-beta` puts it in neither — and decided the
SPM form on the grounds that the choice "does not bite on the live path today". `ln E` is
available per candidate on these records as `:habit-prior-bias`
(`src/futon2/aif/efe.clj:896`; `:habit-prior-source :learned-frequency` on 20/20 records), so
the three arms are measurable here. All three solved 20/20, converged and bracketed.

| arm | β tick 1 → 20 | γ tick 1 → 20 |
|-----|---------------|---------------|
| ln E in neither (what the run computed) | 1.002646 → 1.034342 | 0.99736 → 0.96680 |
| ln E in π only (friston2017 text) | 1.006849 → 1.106121 | 0.99320 → 0.90406 |
| ln E in both (SPM code) | 1.003761 → 1.029614 | 0.99773 → 0.97124 |

**The choice bites, by a factor of three, and the grounds recorded for it are confirmed.**
After 20 ticks the three arms' γ span 0.904 to 0.971 — the text form drifts 3.3× further from
1.0 than the code form does. And the reason is exactly the argument claude-1 gave for the code
form: with E in **both**, (π − π₀) cancels it and isolates what F_π alone does to the
posterior, so that arm sits closest to the ln-E-free one (Δγ 0.0044 at t=20); with E in **π
only**, the habit prior enters the policy error uncancelled and drives β away (Δγ 0.063). The
decision was made on a reading and is now measured. This is not a ruling and is not written to
any registry — it is evidence for the (b2) row that will make one.

## 3. Where the dark posterior's argmax sits, and why the obvious comparison is wrong

Per tick, on all 20: G spread 125.34, F_π spread 0.7540, and the dark posterior's argmax is
**the machine's rank 134 of 145**, with dG +0.3168 and dF −0.5634 against the machine's rank 1.

The first version of this measurement compared the two **spreads** — 125 against 0.75 — and
would have supported "F_π cannot move the answer". That is the wrong comparison, and it is
wrong in a way worth naming: G's spread is a property of the tail (the field runs 10 → 135),
while what decides an argmax is how the terms differ **among the leaders**. There the gap is
+0.32 of G against −0.56 of F_π, so F_π wins, and the dark π ranks a candidate the machine
puts 134th of 145 at the top.

**What this does and does not bound.** It bounds **S4** (RUN9, live F_π in the policy
posterior), where this π is what would select. It says nothing about **S3** (RUN8, live γ),
which changes γ in σ(−γG) and adds no F_π term — and §1 above is the S3-relevant number: at
γ ≈ 1 the carry moves no ranks at all over 20 ticks.

**The field is not frozen, though these summaries are nearly constant.** All 20 G vectors and
all 20 F_π vectors are pairwise distinct, and 18 of 20 observations are distinct. The spread
and the top-gap agree to four decimals anyway. So the smooth monotone β trajectory is not an
artifact of re-solving one frozen field — and equally, whatever the field's motion is, it is
too small to appear in these statistics.

## Bounds on all of the above

- **One run, twenty ticks, one route** (RUN3's verdict on S2: 180 hops, 9 distinct, all twenty
  ticks the same path). Every number here is from one path through the apparatus.
- **β₀-dominated.** Twenty ticks is ~1% of the distance to whatever the carry contracts to.
- **F_π lags G by one tick** by construction: G is expected under this tick's candidates,
  while F_π can only be scored from the previous tick's prediction of this tick's observation.
  Documented at `src/futon2/aif/policy_precision.clj/align-f-pi-and-g`, not silent.
- The 145-candidate field includes cascade rows appended after ranking; no candidate was
  excluded from the join on 19 of 20 ticks (143 on tick 1, cold-start).
