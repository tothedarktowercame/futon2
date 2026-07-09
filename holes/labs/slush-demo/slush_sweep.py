#!/usr/bin/env python
"""Lambda-sweep + multi-mission robustness for the GFN slush.

Reuses slush_proxy + run_slush_demo infra. Parameterizes over
(mission, coverage_weight, EIG-weight lambda, n_steps).

TASK 2: Lambda sweep at 3000 steps over M-self-documenting-stack.
TASK 3: Best config across ~5 missions.

Usage:
    /home/joe/code/gflownet/.venv/bin/python slush_sweep.py
"""
from __future__ import annotations

import sys
import time
import json
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, "/home/joe/code/gflownet")

import slush_proxy as sp
from slush_proxy import (
    CONSTELLATION_EIG,
    N_CONCEPTS,
    MAX_SELECTION,
    a3_passes,
    all_candidates,
    coverage_count,
    eig,
    greedy_candidate,
    greedy_tie_count,
    reward,
    selection_from_proxy_state,
    set_active_lambda,
    set_active_target,
    target_constellations,
)

K_SAMPLES = 1024
SEED = 20260709
N_STEPS = 3000


def make_config(n_steps, seed=SEED):
    from hydra import compose, initialize_config_dir
    from hydra.core.global_hydra import GlobalHydra
    GlobalHydra.instance().clear()
    initialize_config_dir(
        config_dir="/home/joe/code/gflownet/config", version_base="1.1"
    )
    return compose(
        config_name="tests",
        overrides=[
            "env=choices",
            f"env.n_options={N_CONCEPTS}",
            f"env.max_selection={MAX_SELECTION}",
            "env.with_replacement=False",
            "env.can_select_fewer_than_max=False",
            "proxy=uniform",
            "proxy._target_=slush_proxy.SlushProxy",
            "gflownet=trajectorybalance",
            f"gflownet.optimizer.n_train_steps={n_steps}",
            "policy.forward.n_hid=96",
            "policy.forward.n_layers=2",
            "evaluator.first_it=False",
            "evaluator.period=-1",
            "logger.do.online=False",
            f"seed={seed}",
            "device=cpu",
        ],
    )


def train_and_sample(n_steps, lam, target, seed=SEED):
    from gflownet.utils.common import gflownet_from_config
    set_active_target(target)
    set_active_lambda(lam)
    config = make_config(n_steps, seed)
    gfn = gflownet_from_config(config)
    t0 = time.time()
    gfn.train()
    train_s = time.time() - t0
    batch, _ = gfn.sample_batch(n_forward=K_SAMPLES, train=False)
    states = batch.get_terminating_states()
    samples = []
    for st in states:
        samples.append(selection_from_proxy_state(gfn.env.states2proxy([st])[0]))
    return samples, train_s


def evaluate(target, lam, samples):
    """Compute all metrics for a given target + lambda + samples."""
    candidates = all_candidates()
    cand_index = {c: i for i, c in enumerate(candidates)}

    # True distribution at this lambda
    true_rewards = np.array([reward(c, lam=lam, target_mission=target) for c in candidates], dtype=np.float64)
    true_dist = true_rewards / true_rewards.sum()

    # Sampled distribution
    counts = np.zeros(len(candidates), dtype=np.float64)
    for s in samples:
        key = tuple(sorted(s))
        if key in cand_index:
            counts[cand_index[key]] += 1.0
    total = counts.sum()
    post_dist = counts if total == 0 else counts / total

    # Pearson correlation
    mask = (post_dist > 0) | (true_dist > 0)
    corr = 0.0
    if mask.any() and post_dist[mask].std() > 0 and true_dist[mask].std() > 0:
        corr = float(np.corrcoef(post_dist[mask], true_dist[mask])[0, 1])

    # Distinct + A3-passing
    distinct = {tuple(sorted(s)) for s in samples}
    passing = {s for s in distinct if a3_passes(s, target)}

    # Greedy baseline
    greedy = greedy_candidate(target)
    greedy_passes = a3_passes(greedy, target)
    greedy_ties = greedy_tie_count(target)

    # Avg coverage + EIG
    avg_cov = float(np.mean([coverage_count(s, target) for s in samples]))
    avg_eig = float(np.mean([eig(s) for s in samples]))
    uniform_cov = float(np.mean([coverage_count(c, target) for c in candidates]))
    uniform_eig = float(np.mean([eig(c) for c in candidates]))

    # Top-mode coverage
    reward_threshold_90 = float(np.percentile(true_rewards, 90))
    high_reward_cands = {c for c, r in zip(candidates, true_rewards) if r >= reward_threshold_90}
    gfn_high_modes = distinct & high_reward_cands
    greedy_high_modes = {greedy} & high_reward_cands

    return {
        "pearson": corr,
        "distinct": len(distinct),
        "a3_passing": len(passing),
        "greedy_passes": greedy_passes,
        "greedy_ties": greedy_ties,
        "avg_cov": avg_cov,
        "avg_eig": avg_eig,
        "uniform_cov": uniform_cov,
        "uniform_eig": uniform_eig,
        "cov_above_uniform": avg_cov > uniform_cov,
        "eig_above_uniform": avg_eig > uniform_eig,
        "gfn_top_modes": len(gfn_high_modes),
        "greedy_top_modes": len(greedy_high_modes),
        "n_high_reward": len(high_reward_cands),
    }


def run_single(target, lam, n_steps=N_STEPS):
    """Train + evaluate one (target, lambda) config."""
    print(f"  Training: target={target}, lambda={lam}, steps={n_steps}...", flush=True)
    samples, train_s = train_and_sample(n_steps, lam, target)
    metrics = evaluate(target, lam, samples)
    metrics["train_s"] = round(train_s, 1)
    return metrics


def task2_lambda_sweep():
    """TASK 2: Lambda sweep over {1,2,4,8,16} at 3000 steps on M-self-documenting-stack."""
    target = "M-self-documenting-stack"
    lambdas = [1, 2, 4, 8, 16]
    print("=" * 80)
    print("TASK 2: LAMBDA SWEEP — target={}, steps={}".format(target, N_STEPS))
    print("=" * 80)
    results = {}
    for lam in lambdas:
        print(f"\n--- lambda={lam} ---", flush=True)
        metrics = run_single(target, lam)
        results[lam] = metrics
        print(f"  Pearson={metrics['pearson']:.4f}  "
              f"avg_cov={metrics['avg_cov']:.4f} (uni {metrics['uniform_cov']:.4f}) "
              f"cov_above={metrics['cov_above_uniform']}")
        print(f"  avg_eig={metrics['avg_eig']:.6f} (uni {metrics['uniform_eig']:.6f}) "
              f"eig_above={metrics['eig_above_uniform']}")
        print(f"  A3_pass={metrics['a3_passing']}  greedy_pass={metrics['greedy_passes']}  "
              f"greedy_ties={metrics['greedy_ties']}")
        print(f"  GFN_top={metrics['gfn_top_modes']}/{metrics['n_high_reward']}  "
              f"greedy_top={metrics['greedy_top_modes']}/{metrics['n_high_reward']}")
    return results


def task3_multi_mission(best_lam, n_steps=N_STEPS):
    """TASK 3: Best config across ~5 missions."""
    missions = [
        "M-self-documenting-stack",
        "M-three-column-stack",
        "M-capability-star-map",
        "M-structural-law",
        "M-categorical-code",
    ]
    print("\n" + "=" * 80)
    print(f"TASK 3: MULTI-MISSION ROBUSTNESS — lambda={best_lam}, steps={n_steps}")
    print("=" * 80)
    results = {}
    for m in missions:
        print(f"\n--- mission={m} ---", flush=True)
        metrics = run_single(m, best_lam)
        results[m] = metrics
        print(f"  Pearson={metrics['pearson']:.4f}  "
              f"avg_cov={metrics['avg_cov']:.4f} (uni {metrics['uniform_cov']:.4f})")
        print(f"  avg_eig={metrics['avg_eig']:.6f} (uni {metrics['uniform_eig']:.6f}) "
              f"eig_above={metrics['eig_above_uniform']}")
        print(f"  A3_pass={metrics['a3_passing']}  greedy_pass={metrics['greedy_passes']}  "
              f"greedy_ties={metrics['greedy_ties']}")
        print(f"  GFN_top={metrics['gfn_top_modes']}/{metrics['n_high_reward']}  "
              f"greedy_top={metrics['greedy_top_modes']}/{metrics['n_high_reward']}")
    return results


def main():
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", type=int, default=0, help="0=both, 2=sweep, 3=multi-mission")
    parser.add_argument("--best-lambda", type=int, default=4, help="lambda for task 3")
    args = parser.parse_args()

    t2_results = {}
    t3_results = {}
    if args.task in (0, 2):
        t2_results = task2_lambda_sweep()
    if args.task in (0, 3):
        t3_results = task3_multi_mission(args.best_lambda)

    # Write JSON
    output = {"task2": {str(k): v for k, v in t2_results.items()},
              "task3": t3_results,
              "best_lambda": args.best_lambda}
    out_path = Path(__file__).parent / "findings" / "slush_sweep_results.json"
    out_path.write_text(json.dumps(output, indent=2))
    print(f"\nResults written to {out_path}")


if __name__ == "__main__":
    main()
