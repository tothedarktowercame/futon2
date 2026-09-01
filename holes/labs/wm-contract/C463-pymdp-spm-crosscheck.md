# C463 — pymdp/SPM policy-precision cross-check

Date: 2026-09-01  
Reader: codex-22  
Scope: worklist V5, discovery only

Revisions and URLs are recorded in `refs/README.md` under “External
implementation cross-checks.” All pointers below are to those immutable local
revisions.

## 1. Is policy precision updated from evidence?

### pymdp

No. The current `Agent` accepts precision as constructor data — quote:
`gamma: float | Array = 1.0` — and stores it once — quote:
`self.gamma = jnp.broadcast_to(gamma, (self.batch_size,))`
(`/home/joe/code/refs-external/pymdp/pymdp/agent.py:146-147,368-369`). Policy
inference passes that stored value unchanged — quote: `gamma=self.gamma`
(`/home/joe/code/refs-external/pymdp/pymdp/agent.py:896-906`). The only
assigning occurrence of `self.gamma` in current `pymdp/` is the constructor
line above; there is no evidence update, convergence loop, step constant, or
iteration bound for gamma in this implementation.

The lower-level control function still defaults its standalone argument to
16.0 — quote: `gamma: float = 16.0` and “Policy precision parameter”
(`/home/joe/code/refs-external/pymdp/pymdp/control.py:270-283,315-316`) — but
the public current `Agent` default is 1.0. Therefore claude-1's expectation
“fixed constructor hyperparameter” is **confirmed**, while “default 16.0” is
**refuted** for current `Agent` (it describes the control helper and legacy
agent, not the current constructor).

### SPM

Yes, unless precision updating is suppressed. SPM sets the prior rate — quote:
`beta = MDP(1).beta; ... beta = 1` — and fixes the inner iteration count —
quote: `Ni = 16` (`/home/joe/code/refs-external/spm/toolbox/DEM/spm_MDP_VB_X.m:224-245`).
At every time point it runs `for i = 1:Ni`, recomputes policy posteriors, and
updates the rate from evidence:

> `eg = (qu - pu)'*Q(p{m});`  
> `dFdg = qb{m} - beta + eg;`  
> `qb{m} = qb{m} - dFdg/2;`

(`/home/joe/code/refs-external/spm/toolbox/DEM/spm_MDP_VB_X.m:959-974`). Thus
the step constant is **1/2 in rate-parameter (`qb`) space**, and the bound is
exactly **16 iterations per time point**; this loop has no precision-specific
convergence break (`spm_MDP_VB_X.m:959-984`). With `OPTIONS.gamma` true, the
update is suppressed and `w=1/beta` (`spm_MDP_VB_X.m:968-975`). SPM therefore
does pay a fixed-step cost, but this file bounds it at 16 rather than iterating
the beta relaxation to convergence.

## 2. Is precision carried or reset?

### pymdp

Because pymdp does not update gamma, it simply remains the constructor value
stored on the `Agent` (`agent.py:146-147,368-369`) and is reused whenever
`infer_policies` passes `gamma=self.gamma` (`agent.py:858-866,896-906`). The
code does not distinguish “carry an updated posterior across time” from
“reset to a prior”; no updated posterior exists. It also does not define a WM
tick, so whether a WM tick should correspond to a pymdp time step or episode is
**not answered by this code**.

### SPM

Within one trial, precision is carried across time. Before the time loop SPM
initializes — quote: `qb{m} = beta` and `w{m} = 1/qb{m}`
(`/home/joe/code/refs-external/spm/toolbox/DEM/spm_MDP_VB_X.m:431-445`). At a
later time point it explicitly copies the previous expected precision — quote:
`if t > 1; w{m}(t) = w{m}(t - 1)` — then updates the same `qb{m}` in the
16-step loop (`spm_MDP_VB_X.m:951-974`).

Across trials it resets from that trial's configured `MDP.beta` (or default
1). The multiple-trial driver recursively solves each trial
(`spm_MDP_VB_X.m:152-187`), while its inter-trial updater says “moves
Dirichlet parameters” and copies only `a`, `b`, `c`, `d`, and `e`, not `beta`
(`spm_MDP_VB_X.m:1435-1456`). Each recursive trial consequently runs the
initialization at lines 224-225 and 431-434 anew. Claude-1's expectation —
initialize `qb` from prior beta once per trial, then gradient-step it at each
time point — is **confirmed**.

The SPM code likewise cannot decide what a WM tick means. If one WM tick is a
time point in a continuing trial, SPM supports carrying the posterior rate. If
one WM tick is a new trial, SPM resets it. That mapping is a WM modelling
decision, not present in either external repository.

## 3. Do past-data F_pi and expected G enter together, and what does precision scale?

### pymdp

pymdp has no past-observation per-policy free-energy term in this posterior.
Its documentation says the posterior is the softmax of `neg_efe * gamma + lnE`
(`/home/joe/code/refs-external/pymdp/pymdp/agent.py:858-866`), and the executed
return is exactly — quote —
`nn.softmax(gamma * neg_efe_all_policies + log_stable(E))`
(`/home/joe/code/refs-external/pymdp/pymdp/control.py:356-359`; the inductive
variant repeats it at `control.py:909-912`). Therefore gamma multiplies the
expected-free-energy score alone; pymdp is silent on whether gamma should
scale `F_pi`, because it does not include `F_pi` at all.

### SPM

SPM includes both. It computes the posterior — quote:

> `qu = spm_softmax(qE{m}(p{m}) + w{m}(t)*Q(p{m}) + F(p{m}));`

and the corresponding prior without past-data free energy — quote:

> `pu = spm_softmax(qE{m}(p{m}) + w{m}(t)*Q(p{m}));`

(`/home/joe/code/refs-external/spm/toolbox/DEM/spm_MDP_VB_X.m:951-965`). The
file names `F` “(negative) free energy” and accumulates it separately per
policy over time/state beliefs (`spm_MDP_VB_X.m:804-865`); it names `Q`
“expected free energy” and accumulates future terms separately
(`spm_MDP_VB_X.m:896-947`). Hence expected precision `w` multiplies expected
free energy `Q` alone; past-data `F` is unscaled. Signs are negative-score
conventions here, but the separation is unambiguous.

## Comparison with the three WM positions

1. **Precision is evidence-updated:** pymdp contradicts this by keeping gamma
   fixed (`agent.py:368-369,896-906`); SPM supports it with the `eg`, `dFdg`,
   and half-step update (`spm_MDP_VB_X.m:959-974`).
2. **The posterior precision is carried:** pymdp is silent about an updated
   posterior, though its fixed constructor value persists. SPM supports carry
   within a trial (`spm_MDP_VB_X.m:951-974`) but resets at trial boundaries
   because the multi-trial transfer copies only `a` through `e`
   (`spm_MDP_VB_X.m:1435-1456`). The WM tick/trial identification remains open.
3. **`F_pi` is unscaled while precision multiplies `G` alone:** SPM directly
   supports this with `qE + w*Q + F` (`spm_MDP_VB_X.m:963-964`). pymdp supports
   the “precision multiplies G alone” part but is silent on scaling `F_pi`
   because its posterior omits `F_pi` (`control.py:356-359`).

---

## Owner review (claude-20, 2026-09-01)

Every quote above was opened in the clones and checked verbatim. `Ni = 16` is at
`spm_MDP_VB_X.m:244`, `qb{m} = beta` and `w{m} = 1/qb{m}` at `:433-434`,
`dFdg = qb{m} - beta + eg;` / `qb{m} = qb{m} - dFdg/2;` at `:972-973`, and
`spm_MDP_update` copying only `a`–`e` at `:1444-1448`. pymdp's default is
`gamma: float | Array = 1.0` at `agent.py:146`, and its posterior is
`nn.softmax(gamma * neg_efe_all_policies + log_stable(E))` at
`control.py:359`. The report's line ranges are a few lines wide in places but
every quote says what it is claimed to say. claude-1's remembered 16.0 default
was tested and refuted rather than restated, which is what was asked.

**Three things the read establishes that the report did not draw.**

**1. SPM's β step has no γ², and ours did.** Appendix B gives the flow as
`β̇ = −∂_βF = γ²·ε_γ` (`friston2017.txt:1705-1720`), and our first
implementation followed it. The code does not: `qb{m} = qb{m} - dFdg/2` steps
in β with a flat constant of 1/2 and no precision factor. That γ² is exactly
what made our stepping solver cost `step/β²` per iteration — 5 iterations at
β₀ = 0.5 against 171,018 at β₀ = 50. So the paper's appendix and the paper's
own code disagree about the β-space step, and the cost we found belongs to the
appendix form, not to the fixed point. Our bisection sidesteps both.

**2. SPM does not iterate to convergence.** `Ni = 16` is a fixed count with no
precision-specific break in the loop (`:959-984`). The text says "usually one
would iterate the equalities in equation 2.7 until convergence"
(`friston2017.txt:683-685`); the implementation does sixteen half-steps and
takes what it has. On our field a fixed 16 steps would land far from the root
for any β₀ above about 1 — the stepping solver needed 24 iterations at β₀ = 1
and 258 at β₀ = 2, and those were with the γ² form. So "converged" in SPM means
"sixteen steps happened", and any comparison of its γ against ours is a
comparison against an unconverged value.

**3. SPM's π₀ includes ln E; ours and the paper's text do not.**
`pu = spm_softmax(qE + w*Q)` carries the habit prior, while
`friston2017.txt:684` gives π₀ = σ(−γ·G) with no E. Both `qu` and `pu` carry
`qE`, so it partly cancels in `(qu − pu)` — but softmax is not linear, so it
does not cancel exactly, and the WM has a live `ln E` seam
(`policy.clj:82-100`). Slice I1 (b2) must decide whether ln E enters both, and
say which; today `converge-beta` puts it in neither.

**4. The β/trial finding is the load-bearing one for J4's grounds, and the
report has it right.** `spm_MDP_update` moves `a`, `b`, `c`, `d`, `e` between
trials and does *not* move `beta`. Joe ruled β carried on the grounds that the
paper puts β in η = (a, b, d, β) with the accumulating parameters — and the
same authors' code carries every other member of η across trials while
resetting β. That is a tension with the *grounds*, not with the ruling, and it
is recorded as worklist J5 for Joe rather than acted on here.

## Novelty table

Built by claude-20 at review, not by the reporter: the "shared" columns are
lookups anyone can check, but every "absent" is an absence claim, and absence
is what this campaign keeps catching. Each is a search shown below, not one
pattern that missed. "Ours alone" is claimed only where both other columns are
absent with a pointer to where the thing would have been.

| decision | SPM | pymdp | ours alone? |
|---|---|---|---|
| J1 γ = 1/β updated from evidence | **shared** — `spm_MDP_VB_X.m:972-974` | absent — `gamma` is set once at `agent.py:368` and only read at `:905`; no assignment anywhere else in `pymdp/*.py` | no |
| J2 F_π in the policy posterior | **shared** — `qu = spm_softmax(qE + w*Q + F)`, `:963` | absent — posterior is `softmax(gamma*neg_efe + log_stable(E))`, `control.py:359`; no F term | no |
| J2 Laplace channel F retired | absent — SPM has no continuous Laplace channel to retire | absent — same | **yes** (a WM-specific stack composition, C448) |
| J3 build order from a registry DAG | absent — no equation registry; order is the order of the file | absent — same | **yes** |
| J4 β carried across ticks | **partial** — carried across time steps within a trial (`:951-974`), reset at each trial (`:1444-1448` copies `a`–`e` only) | absent — nothing to carry | open: depends on tick↔trial (J5) |
| I1 bisection on the scalar root | absent — fixed 16 half-steps, `Ni = 16` at `:244`, no convergence break at `:959-984` | absent — no solver at all | **yes** |
| I2 γ on G alone, F_π unscaled | **shared** — `w*Q + F` at `:963`, `w` on `Q` only | **shared for the γ·G half** — `gamma * neg_efe` at `control.py:359`; silent on F, having none | no |
| I2 horizon-one policy per candidate | absent — policies are multi-step sequences (`p{m} = 1:Np(m)`) | absent — `policy_matrix` is `(n_policies, n_timepoints, n_control_factors)`, `control.py:356` | **yes** (forced by the WM's one-action-per-tick grain, C462 §5) |
| I3 per-candidate prediction + Q(π) persisted per tick | absent — `wn`/`un` record precision and policy posteriors for plotting (`:977-981`), not per-candidate predictions for later scoring | absent — no trace store | **yes** |
| D route-conformance figure checked against recorded runs | absent — no such artifact | absent — no such artifact | **yes** |

Read plainly: the parts of this work that match the formalism are **shared with
SPM and were worth checking** — evidence-updated precision, F_π in the
posterior, γ on G alone. What is ours alone is the apparatus around it (the
registry, the build order, the trace, the conformance figure), the horizon-one
grain forced by the WM's tick, and — unexpectedly — **the solver**. Nobody else
here solves eq. 2.7's fixed point; SPM takes sixteen steps toward it and pymdp
never poses the question.
