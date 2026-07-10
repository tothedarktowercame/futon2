"""Discharge-trained reward for the GFlowNet slush (slice-2a).

The production slush reward (slush_proxy.py) is TARGET-MISSION COVERAGE:
    risk = -w * |selection ∩ target_mission_constellations|,  EFE = risk - λ·EIG.
It reconstructs one named mission's constellations. Slice-2a wants the
DISCHARGE-trained move-prior: sample constellation-compositions that are ALIVE
(went well), learned from the real outcome labels — not coverage of a fixed
target.

REWARD = a STEEP aliveness score over the selection. The lesson from the four
prior GFN runs (E-cascade-sampler-sampler, fold_embed gfn_seed_v0) is that FLAT
rewards (coverage / exact-set overlap) give a zero-gradient null; the one
positive run (fold-gfn) used a steep exp(β·F1). So here:

    aliveness(S) = w · occupancy(S) + b         (logit of an alive-vs-mess model)
    EFE(S)       = -aliveness(S) - λ·EIG(S)
    R(S)         = exp(-EFE) = exp(aliveness(S) + λ·EIG(S))

`w` is fit by L2-regularized logistic regression on each labelled mission's
CONSTELLATION-OCCUPANCY vector (which of P0-P17 its :applied patterns hit) vs
its mission-wholeness class (alive=1 / mess=0). The occupancy label is over
scope-tree structure, NOT the patterns, so scoring pattern-compositions against
it is non-circular (M-G-over-cascades §5a).

Running this file IS the reward-informativeness GATE (reward-before-generator):
it reports leave-one-out AUC of the aliveness model + a label-shuffle null. If
the structural reward does not separate alive/mess above the null, there is no
point training a GFN on it (that is exactly what killed the prior runs).
"""
from __future__ import annotations

import math
import re
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

import numpy as np

import slush_proxy as sp  # reuse the constellation + mission-pattern parsing

WHOLENESS_PATH = Path("/home/joe/code/futon6/data/mission-wholeness.edn")
N_CONCEPTS = sp.N_CONCEPTS
L2 = 1.0            # ridge strength (18 features, ~130 examples)
BETA = 1.0          # reward steepness on the aliveness logit
SEED = 20260710


def parse_wholeness(path: Path = WHOLENESS_PATH) -> Dict[str, Dict]:
    """{mission -> {:class, :L}} for the labelled (alive/mess) missions."""
    text = path.read_text()
    out = {}
    for m in re.finditer(
        r':mission\s+"(M-[^"]+)"\s+:class\s+:(\w+)\s+:L\s+([0-9.]+)', text
    ):
        out[sp.normalize_mission_id(m.group(1))] = {"class": m.group(2), "L": float(m.group(3))}
    return out


def mission_occupancy(mission: str, p2c: Dict[str, int]) -> np.ndarray:
    """Binary occupancy over the 18 constellations for a mission's :applied set."""
    o = np.zeros(N_CONCEPTS, dtype=np.float64)
    for p in sp.MISSION_TO_PATTERNS.get(sp.normalize_mission_id(mission), []):
        c = p2c.get(sp.pattern_stem(p))
        if c is not None and 0 <= c < N_CONCEPTS:
            o[c] = 1.0
    return o


def build_dataset() -> Tuple[np.ndarray, np.ndarray, List[str]]:
    """(X occupancy, y alive=1/mess=0, mission-ids) over labelled alive/mess missions."""
    p2c = sp.PATTERN_TO_CONSTELLATION
    whole = parse_wholeness()
    X, y, ids = [], [], []
    for mission, rec in whole.items():
        if rec["class"] not in ("alive", "mess"):
            continue
        occ = mission_occupancy(mission, p2c)
        if occ.sum() == 0:            # pattern-less (pipeline-like); no signal
            continue
        X.append(occ); y.append(1.0 if rec["class"] == "alive" else 0.0); ids.append(mission)
    return np.array(X), np.array(y), ids


def fit_logistic(X: np.ndarray, y: np.ndarray, l2: float = L2, iters: int = 2000, lr: float = 0.5):
    """L2-regularized logistic regression via gradient descent (deterministic)."""
    n, d = X.shape
    w = np.zeros(d); b = 0.0
    for _ in range(iters):
        z = X @ w + b
        p = 1.0 / (1.0 + np.exp(-z))
        gw = X.T @ (p - y) / n + l2 * w / n
        gb = float(np.mean(p - y))
        w -= lr * gw; b -= lr * gb
    return w, b


def auc(scores: np.ndarray, y: np.ndarray) -> float:
    """Rank AUC (fraction of alive>mess pairs ordered correctly)."""
    pos = scores[y == 1]; neg = scores[y == 0]
    if len(pos) == 0 or len(neg) == 0:
        return float("nan")
    wins = sum((pos[:, None] > neg[None, :]).sum() for _ in [0]) + 0.5 * (pos[:, None] == neg[None, :]).sum()
    return float(wins) / (len(pos) * len(neg))


def loo_auc(X: np.ndarray, y: np.ndarray) -> float:
    """Leave-one-out AUC — honest generalization of the aliveness model."""
    scores = np.zeros(len(y))
    for i in range(len(y)):
        mask = np.ones(len(y), dtype=bool); mask[i] = False
        w, b = fit_logistic(X[mask], y[mask])
        scores[i] = X[i] @ w + b
    return auc(scores, y)


# --- module-level fitted model, for the GFN reward (fit on all labelled data) ---
_X, _Y, _IDS = build_dataset()
_W, _B = fit_logistic(_X, _Y)
CONSTELLATION_ALIVE_WEIGHT = {i: float(_W[i]) for i in range(N_CONCEPTS)}


def aliveness(selection: Sequence[int]) -> float:
    """Alive-vs-mess logit of a constellation selection (the discharge score)."""
    o = np.zeros(N_CONCEPTS);
    for c in selection:
        if 0 <= c < N_CONCEPTS:
            o[c] = 1.0
    return float(o @ _W + _B)


def discharge_reward(selection: Sequence[int], lam: float = 1.0, beta: float = BETA) -> float:
    """R = exp(beta·aliveness + lam·EIG) — steep, retains the epistemic term."""
    eig = sum(sp.CONSTELLATION_EIG.get(i, 0.0) for i in selection)
    return math.exp(beta * aliveness(selection) + lam * eig)


try:
    import torch
    from gflownet.proxy.base import Proxy

    class SlushDischargeProxy(Proxy):
        """GFlowNet proxy: R = exp(beta·aliveness + lam·EIG) over constellation choices."""
        def __init__(self, lam: float = 1.0, beta: float = BETA, **kwargs):
            kwargs.setdefault("reward_min", 1e-8); kwargs.setdefault("do_clip_rewards", True)
            super().__init__(**kwargs)
            self.lam = lam; self.beta = beta
        def setup(self, env=None):
            pass
        def __call__(self, states: Iterable[dict]) -> "torch.Tensor":
            vals = [discharge_reward(sp.selection_from_proxy_state(st), self.lam, self.beta) for st in states]
            return torch.tensor(vals, dtype=self.float, device=self.device)
except Exception:
    pass  # torch/gflownet not importable in a plain venv — the reward + gate still run


def _gate():
    """Reward-informativeness gate: LOO-AUC vs a label-shuffle null."""
    X, y, ids = _X, _Y, _IDS
    print(f"labelled missions: {len(y)}  (alive {int(y.sum())} / mess {int((1-y).sum())})  features={X.shape[1]}")
    real = loo_auc(X, y)
    rng = np.random.default_rng(SEED)
    nulls = []
    for _ in range(20):
        yp = rng.permutation(y)
        nulls.append(loo_auc(X, yp))
    nulls = np.array(nulls)
    print(f"LOO-AUC (real)          = {real:.3f}")
    print(f"LOO-AUC (label-shuffle) = {nulls.mean():.3f} ± {nulls.std():.3f}  (max {nulls.max():.3f})")
    print(f"GATE: {'PASS' if real > nulls.max() else 'FAIL'} "
          f"(real {real:.3f} vs null-max {nulls.max():.3f})")
    order = sorted(range(N_CONCEPTS), key=lambda i: -_W[i])
    print("\nlearned per-constellation ALIVE weight (want mgmt P1/P2/P6 +, math P13 -):")
    for i in order:
        lbl = sp.CONSTELLATION_LABELS.get(i, "")
        print(f"  P{i:<2} {_W[i]:+.3f}  {lbl}")


if __name__ == "__main__":
    _gate()
