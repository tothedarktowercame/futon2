#!/usr/bin/env python3
"""Slice-0: a *trusted* GFlowNet trainer harness (pure numpy, no torch).

Purpose (per `holes/TN-gflownets-fable-review.md`): before touching the mission
reward again, prove the trainer itself is correct on synthetic problems small
enough to enumerate exactly. This isolates the two defects the technote pins as
structural in slice-2:

  F1 (undertraining)      -- the trainer here trains to convergence and we
                             *measure* convergence against the exact target.
  F2 (shared scalar logZ) -- a single global logZ is mis-specified when the
                             true partition function Z(m) differs per mission.
                             We reproduce the bug as an auditable gate (G0b):
                             the shared-logZ arm must FAIL while the
                             conditional-logZ(m) arm PASSES.

Design choices that make this a trainer-correctness test rather than a
function-approximation test:

  * Tabular forward policy P_F, one logit table per (mission, state). Fully
    expressive per mission, so a failed G0a can only mean the *objective /
    logZ* is wrong -- not that the model lacked capacity.
  * Set-generation with the standard uniform backward policy P_B(parent)=1/|S|,
    which correctly accounts for the |S|! orderings that reach a set. (The
    slice-2 TB dropped the P_B term; that is a latent third defect this harness
    makes visible.)
  * The trained sampler's terminal marginal is computed *exactly* by DP over the
    enumerable DAG, so the G0a gate carries no Monte-Carlo noise.

Trajectory-balance constraint (with a STOP action terminating each trajectory):

    logZ(m) + sum_t log P_F(a_t | s_t)  ==  log R(x | m) + sum_t log P_B(s_t | s_{t+1})

With uniform P_B, sum_t log P_B = log(1 / |x|!) = -logfact(|x|). Define the
per-trajectory residual

    delta = logZ(m) + sum_t log P_F(a_t|s_t) + logfact(|x|) - log R(x|m)

and minimise E[delta^2] (trajectory balance). At the optimum the terminal
marginal is proportional to R(x|m) and the learned logZ(m) equals the true
log-partition. Both are checked by the gates.
"""
from __future__ import annotations

import argparse
import json
import math
from dataclasses import dataclass, field
from itertools import combinations
from pathlib import Path
from typing import Callable, Dict, FrozenSet, List, Sequence, Tuple

import numpy as np

HERE = Path(__file__).resolve().parent
SLUSH_DIR = HERE.parent
FINDINGS_PATH = SLUSH_DIR / "findings" / "slice0_trainer_findings.md"
RESULTS_PATH = SLUSH_DIR / "findings" / "slice0_trainer_results.json"

STOP = -1  # sentinel action id: terminate the trajectory at the current set

State = FrozenSet[int]
RewardFn = Callable[[State], float]


# --------------------------------------------------------------------------- #
# small numeric helpers
# --------------------------------------------------------------------------- #
def logsumexp(xs: Sequence[float]) -> float:
    a = np.asarray(xs, dtype=np.float64)
    if a.size == 0:
        return -math.inf
    m = float(a.max())
    if m == -math.inf:
        return -math.inf
    return m + float(np.log(np.exp(a - m).sum()))


def log_softmax(logits: np.ndarray) -> np.ndarray:
    m = float(logits.max())
    z = m + float(np.log(np.exp(logits - m).sum()))
    return logits - z


def softmax(logits: np.ndarray) -> np.ndarray:
    m = float(logits.max())
    e = np.exp(logits - m)
    return e / e.sum()


_LOGFACT = [0.0]
for _i in range(1, 64):
    _LOGFACT.append(_LOGFACT[-1] + math.log(_i))


def logfact(k: int) -> float:
    return _LOGFACT[k]


# --------------------------------------------------------------------------- #
# the enumerable set-DAG
# --------------------------------------------------------------------------- #
def enumerate_sets(n: int, max_len: int) -> List[State]:
    """All subsets of {0..n-1} of size 0..max_len -- the terminal states."""
    out: List[State] = []
    for k in range(0, max_len + 1):
        for combo in combinations(range(n), k):
            out.append(frozenset(combo))
    return out


def actions(state: State, n: int, max_len: int) -> List[int]:
    """Admissible actions from `state`: add any unused item (if room), then STOP.

    At max_len the only action is STOP, so trajectories are forced to terminate.
    """
    if len(state) < max_len:
        adds = [i for i in range(n) if i not in state]
    else:
        adds = []
    return adds + [STOP]


# a trajectory template: the ordered (state, action) steps ending in STOP, plus
# the terminal set. Templates are mission-agnostic (only the reward differs), so
# they are enumerated once and reused across missions and training iterations.
@dataclass
class TrajTemplate:
    steps: Tuple[Tuple[State, int], ...]  # (state, action); last action is STOP
    terminal: State


def enumerate_trajectories(n: int, max_len: int) -> List[TrajTemplate]:
    """Every ordered add-sequence of length 0..max_len, each closed by STOP.

    A set of size k is reached by k! such templates -- exactly the orderings the
    uniform backward policy P_B accounts for. The full DAG is tiny, so training
    can be done deterministically full-batch over all templates (no MC noise)."""
    out: List[TrajTemplate] = []

    def rec(state: State, steps: List[Tuple[State, int]]):
        # option: STOP here
        out.append(TrajTemplate(tuple(steps) + ((state, STOP),), state))
        if len(state) < max_len:
            for i in range(n):
                if i not in state:
                    rec(frozenset(state | {i}), steps + [(state, i)])

    rec(frozenset(), [])
    return out


# --------------------------------------------------------------------------- #
# synthetic submodular coverage missions (rehearses the v3 reward shape)
# --------------------------------------------------------------------------- #
@dataclass
class Mission:
    mid: str
    n: int
    max_len: int
    # item -> set of want-atoms it covers (bitmask over range(n_atoms))
    cover: Dict[int, frozenset]
    atom_weight: np.ndarray  # weight per want-atom
    beta: float              # reward temperature (scales accuracy - lambda*complexity)
    lam: float               # complexity penalty
    item_cost: np.ndarray    # per-item complexity cost

    def log_reward(self, state: State) -> float:
        covered: set = set()
        for i in state:
            covered |= self.cover[i]
        accuracy = float(self.atom_weight[list(covered)].sum()) if covered else 0.0
        complexity = float(sum(self.item_cost[i] for i in state))
        return self.beta * (accuracy - self.lam * complexity)


def make_missions(
    *,
    seed: int,
    n: int,
    max_len: int,
    specs: Sequence[Tuple[str, int, float]],
) -> List[Mission]:
    """Build one mission per spec `(mid, n_atoms, beta)`.

    Varying n_atoms and beta makes the true log-partition Z(m) differ across
    missions by several nats -- the precondition for the F2 (shared-logZ) bug.
    Submodularity comes from union-of-coverage: a pattern's marginal accuracy
    depends on what is already selected.
    """
    rng = np.random.default_rng(seed)
    missions: List[Mission] = []
    for mid, n_atoms, beta in specs:
        atom_weight = rng.uniform(0.5, 1.5, size=n_atoms)
        item_cost = rng.uniform(0.5, 1.5, size=n)
        cover: Dict[int, frozenset] = {}
        for i in range(n):
            # each item covers 1..3 atoms; overlap across items -> submodularity
            k = int(rng.integers(1, min(4, n_atoms) + 1))
            atoms = rng.choice(n_atoms, size=k, replace=False)
            cover[i] = frozenset(int(a) for a in atoms)
        missions.append(
            Mission(
                mid=mid, n=n, max_len=max_len, cover=cover,
                atom_weight=atom_weight, beta=beta, lam=0.5, item_cost=item_cost,
            )
        )
    return missions


# --------------------------------------------------------------------------- #
# exact targets
# --------------------------------------------------------------------------- #
def exact_gibbs(reward: RewardFn, sets: Sequence[State]) -> Tuple[Dict[State, float], float]:
    logs = [reward(x) for x in sets]
    lz = logsumexp(logs)
    probs = {x: math.exp(lr - lz) for x, lr in zip(sets, logs)}
    return probs, lz


# --------------------------------------------------------------------------- #
# tabular GFN policy + trainer
# --------------------------------------------------------------------------- #
@dataclass
class GFN:
    n: int
    max_len: int
    conditional_logz: bool
    logz_lr_mult: float = 5.0
    # per (mission, state) logit vector aligned to actions(state)
    logits: Dict[Tuple[str, State], np.ndarray] = field(default_factory=dict)
    logz: Dict[str, float] = field(default_factory=dict)
    # Adam state
    _m: Dict = field(default_factory=dict)
    _v: Dict = field(default_factory=dict)
    _t: int = 0

    def _zkey(self, mid: str) -> str:
        return mid if self.conditional_logz else "SHARED"

    def get_logits(self, mid: str, state: State) -> np.ndarray:
        key = (mid, state)
        vec = self.logits.get(key)
        if vec is None:
            vec = np.zeros(len(actions(state, self.n, self.max_len)))
            self.logits[key] = vec
        return vec

    def get_logz(self, mid: str) -> float:
        return self.logz.setdefault(self._zkey(mid), 0.0)

    # ---- sampling ---------------------------------------------------------- #
    def sample(self, mid: str, reward: RewardFn, rng: np.random.Generator, eps: float):
        """Sample one trajectory with eps-uniform exploration (TB is off-policy,
        so exploration keeps every state in the gradient's support)."""
        state: State = frozenset()
        steps = []  # (mid, state, chosen_local_idx)
        while True:
            acts = actions(state, self.n, self.max_len)
            logits = self.get_logits(mid, state)
            probs = (1.0 - eps) * softmax(logits) + eps / len(acts)
            idx = int(rng.choice(len(acts), p=probs / probs.sum()))
            steps.append((state, idx))
            a = acts[idx]
            if a == STOP:
                break
            state = frozenset(state | {a})
        terminal = state
        return steps, terminal, reward(terminal)

    # ---- one gradient step over a batch ------------------------------------ #
    def train_step(self, batch, lr: float):
        grads_logits: Dict[Tuple[str, State], np.ndarray] = {}
        grads_logz: Dict[str, float] = {}
        for mid, steps, terminal, log_r in batch:
            log_pf = 0.0
            for state, idx in steps:
                ls = log_softmax(self.get_logits(mid, state))
                log_pf += float(ls[idx])
            delta = self.get_logz(mid) + log_pf + logfact(len(terminal)) - log_r
            # d delta / d logits(state) = onehot(idx) - softmax
            for state, idx in steps:
                key = (mid, state)
                sm = softmax(self.get_logits(mid, state))
                g = -sm
                g[idx] += 1.0
                acc = grads_logits.get(key)
                if acc is None:
                    grads_logits[key] = 2.0 * delta * g
                else:
                    acc += 2.0 * delta * g
            zk = self._zkey(mid)
            grads_logz[zk] = grads_logz.get(zk, 0.0) + 2.0 * delta
        b = max(1, len(batch))
        self._t += 1
        for key, g in grads_logits.items():
            self._adam_apply(("L", key), self.logits[key], g / b, lr)
        # logZ is a single scalar that must travel from 0 to the true
        # log-partition; give it a larger step so it does not bottleneck.
        for zk, g in grads_logz.items():
            cur = np.array([self.logz.get(zk, 0.0)])
            self._adam_apply(("Z", zk), cur, np.array([g / b]), lr * self.logz_lr_mult)
            self.logz[zk] = float(cur[0])

    def _adam_apply(self, key, param: np.ndarray, grad: np.ndarray, lr: float,
                    b1=0.9, b2=0.999, eps=1e-8):
        m = self._m.get(key)
        v = self._v.get(key)
        if m is None:
            m = np.zeros_like(param)
            v = np.zeros_like(param)
        m = b1 * m + (1 - b1) * grad
        v = b2 * v + (1 - b2) * (grad * grad)
        mhat = m / (1 - b1 ** self._t)
        vhat = v / (1 - b2 ** self._t)
        param -= lr * mhat / (np.sqrt(vhat) + eps)
        self._m[key] = m
        self._v[key] = v

    # ---- exact terminal marginal (no Monte-Carlo) -------------------------- #
    def terminal_marginal(self, mid: str, sets: Sequence[State]) -> Dict[State, float]:
        """P_model(x) = [sum over orderings of prod P_F(add)] * P_F(STOP | x),
        computed by DP over the DAG in increasing set size."""
        reach: Dict[State, float] = {frozenset(): 1.0}
        by_size: Dict[int, List[State]] = {}
        for x in sets:
            by_size.setdefault(len(x), []).append(x)
        for size in range(1, self.max_len + 1):
            for x in by_size.get(size, []):
                total = 0.0
                for i in x:
                    parent = frozenset(x - {i})
                    r_par = reach.get(parent)
                    if not r_par:
                        continue
                    acts = actions(parent, self.n, self.max_len)
                    p_add = softmax(self.get_logits(mid, parent))[acts.index(i)]
                    total += r_par * float(p_add)
                reach[x] = total
        out: Dict[State, float] = {}
        for x in sets:
            acts = actions(x, self.n, self.max_len)
            p_stop = softmax(self.get_logits(mid, x))[acts.index(STOP)]
            out[x] = reach.get(x, 0.0) * float(p_stop)
        return out

    # ---- deterministic full-batch TB (the correctness gate's trainer) ------ #
    def fit_exact(
        self,
        missions: Sequence[Mission],
        trajs: Sequence[TrajTemplate],
        *,
        iters: int,
        lr: float,
    ) -> "GFN":
        """Full-batch trajectory balance over every enumerated trajectory. The
        TB objective's unique global minimum (delta==0 for all trajectories) is
        the exact Gibbs sampler with logZ(m)==true log-partition; deterministic
        gradient descent reaches it with no Monte-Carlo noise. When logZ is a
        single shared scalar, delta==0 is unreachable for missions with
        different Z, so the minimiser is a warped (tempered) distribution."""
        # precompute action index maps and per-mission log-rewards once
        act_idx: Dict[State, Dict[int, int]] = {}
        for tpl in trajs:
            for state, action in tpl.steps:
                if state not in act_idx:
                    acts = actions(state, self.n, self.max_len)
                    act_idx[state] = {a: j for j, a in enumerate(acts)}
        log_r = {m.mid: {tpl.terminal: m.log_reward(tpl.terminal) for tpl in trajs}
                 for m in missions}
        lf = {tpl.terminal: logfact(len(tpl.terminal)) for tpl in trajs}
        for _ in range(iters):
            self._t += 1
            # cache softmax / log-softmax per (mid, state) for this iteration
            cache: Dict[Tuple[str, State], Tuple[np.ndarray, np.ndarray]] = {}

            def cached(mid: str, state: State):
                key = (mid, state)
                hit = cache.get(key)
                if hit is None:
                    lg = self.get_logits(mid, state)
                    hit = (log_softmax(lg), softmax(lg))
                    cache[key] = hit
                return hit

            grads_logits: Dict[Tuple[str, State], np.ndarray] = {}
            grads_logz: Dict[str, float] = {}
            for m in missions:
                for tpl in trajs:
                    log_pf = 0.0
                    for state, action in tpl.steps:
                        ls, _ = cached(m.mid, state)
                        log_pf += float(ls[act_idx[state][action]])
                    delta = (self.get_logz(m.mid) + log_pf + lf[tpl.terminal]
                             - log_r[m.mid][tpl.terminal])
                    for state, action in tpl.steps:
                        _, sm = cached(m.mid, state)
                        g = -sm.copy()
                        g[act_idx[state][action]] += 1.0
                        key = (m.mid, state)
                        acc = grads_logits.get(key)
                        if acc is None:
                            grads_logits[key] = 2.0 * delta * g
                        else:
                            acc += 2.0 * delta * g
                    zk = self._zkey(m.mid)
                    grads_logz[zk] = grads_logz.get(zk, 0.0) + 2.0 * delta
            scale = 1.0 / len(trajs)
            for key, g in grads_logits.items():
                self._adam_apply(("L", key), self.logits[key], g * scale, lr)
            for zk, g in grads_logz.items():
                cur = np.array([self.logz.get(zk, 0.0)])
                self._adam_apply(("Z", zk), cur, np.array([g * scale]),
                                 lr * self.logz_lr_mult)
                self.logz[zk] = float(cur[0])
        return self


def total_variation(p: Dict[State, float], q: Dict[State, float]) -> float:
    keys = set(p) | set(q)
    return 0.5 * sum(abs(p.get(k, 0.0) - q.get(k, 0.0)) for k in keys)


# --------------------------------------------------------------------------- #
# training driver
# --------------------------------------------------------------------------- #
def train(
    missions: Sequence[Mission],
    *,
    conditional_logz: bool,
    steps: int,
    batch: int,
    lr: float,
    eps: float,
    seed: int,
    eps_end: float = 0.02,
) -> GFN:
    """Train by trajectory balance. `eps` is annealed linearly to `eps_end`:
    broad exploration early (every state gets gradient), then near-on-policy
    sampling late so the terminal marginal is refined where the mass lives."""
    n = missions[0].n
    max_len = missions[0].max_len
    gfn = GFN(n=n, max_len=max_len, conditional_logz=conditional_logz)
    rng = np.random.default_rng(seed)
    for t in range(steps):
        eps_t = eps + (eps_end - eps) * (t / max(1, steps - 1))
        b = []
        for _ in range(batch):
            m = missions[int(rng.integers(len(missions)))]
            steps_tr, terminal, log_r = gfn.sample(m.mid, m.log_reward, rng, eps_t)
            b.append((m.mid, steps_tr, terminal, log_r))
        gfn.train_step(b, lr)
    return gfn


def train_exact(
    missions: Sequence[Mission],
    trajs: Sequence[TrajTemplate],
    *,
    conditional_logz: bool,
    iters: int,
    lr: float,
) -> GFN:
    gfn = GFN(n=missions[0].n, max_len=missions[0].max_len,
              conditional_logz=conditional_logz)
    return gfn.fit_exact(missions, trajs, iters=iters, lr=lr)


# --------------------------------------------------------------------------- #
# gates
# --------------------------------------------------------------------------- #
@dataclass
class ArmResult:
    conditional_logz: bool
    per_mission: List[Dict]
    max_tv: float
    mean_tv: float
    max_logz_err: float


def evaluate_arm(gfn: GFN, missions: Sequence[Mission], sets: Sequence[State]) -> ArmResult:
    rows = []
    for m in missions:
        gibbs, true_lz = exact_gibbs(m.log_reward, sets)
        model = gfn.terminal_marginal(m.mid, sets)
        tv = total_variation(model, gibbs)
        learned_lz = gfn.get_logz(m.mid)
        rows.append({
            "mission": m.mid,
            "true_logz": true_lz,
            "learned_logz": learned_lz,
            "logz_err": abs(true_lz - learned_lz),
            "tv": tv,
            "model_mass": sum(model.values()),
        })
    return ArmResult(
        conditional_logz=gfn.conditional_logz,
        per_mission=rows,
        max_tv=max(r["tv"] for r in rows),
        mean_tv=float(np.mean([r["tv"] for r in rows])),
        max_logz_err=max(r["logz_err"] for r in rows),
    )


def run_gates(
    *,
    seed: int = 20260710,
    n: int = 10,
    max_len: int = 3,
    iters: int = 800,
    lr: float = 0.05,
    tv_gate: float = 0.05,
    logz_gate: float = 0.1,
) -> Dict:
    # >=3 missions whose reward magnitude (beta) and want-size differ -> Z spread.
    # betas/atom-counts chosen so the true logZ spread comfortably clears the
    # >=3 nat (e^3) precondition without being so large it slows convergence.
    specs = [
        ("M-small", 4, 0.35),
        ("M-mid", 6, 0.6),
        ("M-large", 9, 0.9),
        ("M-xl", 12, 1.2),
    ]
    missions = make_missions(seed=seed, n=n, max_len=max_len, specs=specs)
    sets = enumerate_sets(n, max_len)
    trajs = enumerate_trajectories(n, max_len)

    # true log-partition spread (the F2 precondition)
    true_lz = {m.mid: exact_gibbs(m.log_reward, sets)[1] for m in missions}
    lz_spread = max(true_lz.values()) - min(true_lz.values())

    cond = train_exact(missions, trajs, conditional_logz=True, iters=iters, lr=lr)
    shar = train_exact(missions, trajs, conditional_logz=False, iters=iters, lr=lr)

    cond_res = evaluate_arm(cond, missions, sets)
    shar_res = evaluate_arm(shar, missions, sets)

    g0a_pass = cond_res.max_tv < tv_gate and cond_res.max_logz_err < logz_gate
    # G0b: shared arm must fail G0a (some mission's TV over the gate) while
    # the conditional arm passes -- pinning F2 as a real, fixed defect.
    g0b_pass = g0a_pass and (shar_res.max_tv >= tv_gate)

    return {
        "config": {
            "seed": seed, "n": n, "max_len": max_len, "iters": iters, "lr": lr,
            "tv_gate": tv_gate, "logz_gate": logz_gate,
            "n_sets": len(sets), "n_trajs": len(trajs), "specs": specs,
        },
        "true_logz": true_lz,
        "logz_spread_nats": lz_spread,
        "conditional_arm": cond_res.__dict__,
        "shared_arm": shar_res.__dict__,
        "G0a_pass": bool(g0a_pass),
        "G0b_pass": bool(g0b_pass),
    }


# --------------------------------------------------------------------------- #
# findings
# --------------------------------------------------------------------------- #
def write_findings(result: Dict, path: Path = FINDINGS_PATH) -> None:
    c = result["config"]
    cond = result["conditional_arm"]
    shar = result["shared_arm"]
    lines = [
        "# Slice-0 — Trusted GFN trainer harness (gates G0a / G0b)",
        "",
        "Pure-numpy tabular GFlowNet trained by trajectory balance on synthetic",
        "submodular-coverage missions small enough to enumerate exactly. This",
        "validates the *trainer* (F1 undertraining, F2 shared-logZ) before the",
        "mission reward is trusted. See `holes/TN-gflownets-fable-review.md`.",
        "",
        "## Config",
        "",
        f"- n items / max_len: {c['n']} / {c['max_len']}  "
        f"({c['n_sets']} terminal sets, {c['n_trajs']} trajectories enumerated)",
        f"- full-batch iters / lr: {c['iters']} / {c['lr']}",
        f"- gates: TV < {c['tv_gate']}, |logZ−true| < {c['logz_gate']} nat",
        f"- missions (mid, n_atoms, beta): {c['specs']}",
        "",
        "## True log-partition spread (F2 precondition)",
        "",
        f"- Z(m) spread across missions: **{result['logz_spread_nats']:.2f} nats** "
        f"(need ≥ 3 to stress a shared scalar logZ)",
        "",
        "| mission | true logZ |",
        "|---|---:|",
    ]
    for mid, lz in result["true_logz"].items():
        lines.append(f"| {mid} | {lz:.3f} |")
    lines += [
        "",
        "## Gate G0a — conditional logZ(m) recovers the exact Gibbs sampler",
        "",
        f"- max TV to exact Gibbs: **{cond['max_tv']:.4f}** (gate {c['tv_gate']})",
        f"- mean TV: {cond['mean_tv']:.4f}",
        f"- max |learned logZ − true logZ|: **{cond['max_logz_err']:.4f}** nat (gate {c['logz_gate']})",
        "",
        "| mission | TV | learned logZ | true logZ | logZ err |",
        "|---|---:|---:|---:|---:|",
    ]
    for r in cond["per_mission"]:
        lines.append(
            f"| {r['mission']} | {r['tv']:.4f} | {r['learned_logz']:.3f} | "
            f"{r['true_logz']:.3f} | {r['logz_err']:.4f} |"
        )
    lines += [
        "",
        "## Gate G0b — a shared scalar logZ is mis-specified (reproduces F2)",
        "",
        "Same policy capacity, only logZ collapsed to one global scalar.",
        "",
        f"- shared-arm max TV: **{shar['max_tv']:.4f}** (must be ≥ {c['tv_gate']} to demonstrate the bug)",
        f"- shared-arm mean TV: {shar['mean_tv']:.4f}",
        "",
        "| mission | TV (shared logZ) | TV (conditional logZ) |",
        "|---|---:|---:|",
    ]
    tv_cond = {r["mission"]: r["tv"] for r in cond["per_mission"]}
    for r in shar["per_mission"]:
        lines.append(f"| {r['mission']} | {r['tv']:.4f} | {tv_cond[r['mission']]:.4f} |")
    lines += [
        "",
        "## Verdict",
        "",
        f"- **G0a (trainer correctness): {'PASS' if result['G0a_pass'] else 'FAIL'}**",
        f"- **G0b (F2 shared-logZ bug demonstrated + fixed): {'PASS' if result['G0b_pass'] else 'FAIL'}**",
        "",
        "G0a passing means the objective + conditional logZ + uniform-P_B set",
        "handling are correct: the trained sampler reproduces the enumerated",
        "Gibbs distribution and recovers the true log-partition. G0b passing",
        "means the shared-scalar-logZ variant provably fails on missions with",
        "different Z — F2 is real, and conditional logZ(m) fixes it. This gate",
        "is now auditable forever.",
        "",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines))


def main(argv: Sequence[str] | None = None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--iters", type=int, default=800)
    p.add_argument("--lr", type=float, default=0.05)
    p.add_argument("--seed", type=int, default=20260710)
    p.add_argument("--write-findings", action="store_true")
    args = p.parse_args(argv)
    result = run_gates(seed=args.seed, iters=args.iters, lr=args.lr)
    print(json.dumps({
        "logz_spread_nats": result["logz_spread_nats"],
        "conditional": {"max_tv": result["conditional_arm"]["max_tv"],
                        "max_logz_err": result["conditional_arm"]["max_logz_err"]},
        "shared": {"max_tv": result["shared_arm"]["max_tv"]},
        "G0a_pass": result["G0a_pass"],
        "G0b_pass": result["G0b_pass"],
    }, indent=2))
    if args.write_findings:
        RESULTS_PATH.parent.mkdir(parents=True, exist_ok=True)
        RESULTS_PATH.write_text(json.dumps(result, indent=2, sort_keys=True, default=float))
        write_findings(result)
        print(f"wrote {RESULTS_PATH}")
        print(f"wrote {FINDINGS_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
