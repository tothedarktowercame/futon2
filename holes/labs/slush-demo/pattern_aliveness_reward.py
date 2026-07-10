"""Pattern-grain aliveness reward for the discharge GFN (slice-2a).

Two terms, exactly as the operator framed them (2026-07-10):

  FIRSTLY  — aliveness as the REWARD. Each completed mission awards its graded
             Salingaros L to the patterns it applied; a pattern's credit is the
             popularity-normalized SIGNED-L average over the missions that used
             it (alive award +, mess debit -). A composition's reward is the sum
             of its patterns' credits. This is the R(x) the GFN samples ∝ to.

  SECONDLY — the alive-mission BONUS on the move-prior: a smoothed alive-vs-mess
             document-frequency log-odds per pattern (the train_logodds
             direction). Biases the forward policy, separate from the reward.

Both are popularity-normalized so a pattern is not "alive" merely because many
missions use it (the MAP's volume confound). Fit on the TRAIN split only; the
gate reports LEAVE-ONE-OUT AUC of each term vs a label-shuffle null — the
reward-before-generator check that the pattern grain carries the discharge
signal the constellation-SET grain did not (presence AUC 0.55 vs fraction 0.76).

CAVEAT (operator, 2026-07-10): L is scope-tree structure only; under auto-filled
boxes it saturates. The extension is to fold in execution/discharge (the
rollout_execute want_coverage witness). Historical corpus uses graded L.
"""
from __future__ import annotations

import math
import re
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

import numpy as np

import slush_proxy as sp
from slush_discharge_proxy import parse_wholeness, auc

KAPPA = 2.0     # popularity smoothing for the signed-L credit
ALPHA = 0.5     # Laplace smoothing for the log-odds bonus
BETA = 1.0      # reward steepness
SEED = 20260710


def labelled_missions() -> List[Dict]:
    """[{mission, applied:set, y:1/0, L}] over alive/mess missions with >=1 pattern."""
    whole = parse_wholeness()
    out = []
    for m, rec in whole.items():
        if rec["class"] not in ("alive", "mess"):
            continue
        applied = {sp.pattern_stem(p) for p in sp.MISSION_TO_PATTERNS.get(sp.normalize_mission_id(m), [])}
        if not applied:
            continue
        out.append({"mission": m, "applied": applied,
                    "y": 1.0 if rec["class"] == "alive" else 0.0, "L": rec["L"]})
    return out


def fit_credits(train: Sequence[Dict]) -> Tuple[Dict[str, float], Dict[str, float]]:
    """Return (credit, bonus) per pattern from the TRAIN missions.

    credit(p) = sum_{m uses p} sign(m)*L_m/100 / (uses(p)+KAPPA)   [FIRSTLY, popularity-normed]
    bonus(p)  = log DF-odds alive vs mess (Laplace ALPHA)          [SECONDLY, train_logodds]
    """
    n_alive = sum(1 for m in train if m["y"] == 1.0)
    n_mess = sum(1 for m in train if m["y"] == 0.0)
    signedL, uses, df_alive, df_mess = {}, {}, {}, {}
    for m in train:
        s = 1.0 if m["y"] == 1.0 else -1.0
        for p in m["applied"]:
            signedL[p] = signedL.get(p, 0.0) + s * m["L"] / 100.0
            uses[p] = uses.get(p, 0) + 1
            if m["y"] == 1.0:
                df_alive[p] = df_alive.get(p, 0) + 1
            else:
                df_mess[p] = df_mess.get(p, 0) + 1
    credit = {p: signedL[p] / (uses[p] + KAPPA) for p in signedL}
    bonus = {}
    for p in uses:
        pa = (df_alive.get(p, 0) + ALPHA) / (n_alive + 2 * ALPHA)
        pm = (df_mess.get(p, 0) + ALPHA) / (n_mess + 2 * ALPHA)
        bonus[p] = math.log(pa) - math.log(pm)
    return credit, bonus


def composition_score(applied: set, table: Dict[str, float]) -> float:
    """Aggregate a composition's pattern scores (unseen patterns score 0)."""
    return sum(table.get(p, 0.0) for p in applied)


def loo_auc(missions: List[Dict], which: str) -> float:
    """LOO AUC of the composition score (which='credit' or 'bonus')."""
    scores = np.zeros(len(missions)); y = np.array([m["y"] for m in missions])
    for i in range(len(missions)):
        train = missions[:i] + missions[i + 1:]
        credit, bonus = fit_credits(train)
        table = credit if which == "credit" else bonus
        scores[i] = composition_score(missions[i]["applied"], table)
    return auc(scores, y)


def _gate():
    missions = labelled_missions()
    y = np.array([m["y"] for m in missions])
    print(f"labelled missions: {len(missions)}  (alive {int(y.sum())} / mess {int((1-y).sum())})")
    rng = np.random.default_rng(SEED)
    for which in ("credit", "bonus"):
        real = loo_auc(missions, which)
        nulls = []
        for _ in range(20):
            perm = rng.permutation(len(missions))
            shuffled = [dict(m, y=missions[perm[j]]["y"], L=missions[perm[j]]["L"]) for j, m in enumerate(missions)]
            nulls.append(loo_auc(shuffled, which))
        nulls = np.array(nulls)
        tag = "FIRSTLY reward-credit" if which == "credit" else "SECONDLY alive-bonus"
        print(f"\n[{tag}]  LOO-AUC(real)={real:.3f}  "
              f"null={nulls.mean():.3f}±{nulls.std():.3f} (max {nulls.max():.3f})  "
              f"GATE {'PASS' if real > nulls.max() else 'FAIL'}")
    # top/bottom credited patterns (eyeball the award direction)
    credit, bonus = fit_credits(missions)
    top = sorted(credit.items(), key=lambda kv: -kv[1])[:8]
    bot = sorted(credit.items(), key=lambda kv: kv[1])[:8]
    print("\ntop alive-credited patterns:", [f"{p}({v:+.2f})" for p, v in top])
    print("bottom (mess) patterns:     ", [f"{p}({v:+.2f})" for p, v in bot])


if __name__ == "__main__":
    _gate()
