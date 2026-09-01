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
