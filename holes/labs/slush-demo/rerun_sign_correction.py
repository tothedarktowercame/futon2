"""Corrected-sign epistemic-pull re-run — SYNTHETIC DEMO ONLY.

Re-runs the slush GFN with the CORRECTED EFE sign (EFE = risk - lambda*EIG,
so high stddev RAISES reward) at lambda=1, 3, 5, and compares against the
category-error baseline (EFE = risk + lambda*ambiguity, old sign).

Reports epistemic pull (avg ambiguity of GFN samples vs uniform baseline) at
each lambda. Prediction: corrected sign -> positive pull that grows with lambda.

Usage:
    /home/joe/code/gflownet/.venv/bin/python rerun_sign_correction.py [N_STEPS]
"""
from __future__ import annotations

import itertools
import math
import sys
import time
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, "/home/joe/code/gflownet")

import slush_proxy as sp  # noqa: E402
from slush_proxy import (  # noqa: E402
    CONCEPT_STDDEVS,
    N_CONCEPTS,
    VALUES,
    ambiguity,
    selection_from_proxy_state,
)

K_SAMPLES = 128
MAX_SELECTION = 3
N_STEPS_DEFAULT = 1000
SEED_BASE = 20260613


def all_candidates():
    return list(itertools.combinations(range(N_CONCEPTS), MAX_SELECTION))


def make_config(n_steps, seed):
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
            "policy.forward.n_hid=64",
            "policy.forward.n_layers=2",
            "evaluator.first_it=False",
            "evaluator.period=-1",
            "logger.do.online=False",
            f"seed={seed}",
            "device=cpu",
        ],
    )


def train_and_sample(n_steps, seed):
    from gflownet.utils.common import gflownet_from_config

    config = make_config(n_steps, seed)
    gfn = gflownet_from_config(config)
    t0 = time.time()
    gfn.train()
    train_s = time.time() - t0
    batch, _ = gfn.sample_batch(n_forward=K_SAMPLES, train=False)
    states = batch.get_terminating_states()
    samples = []
    for st in states:
        sel = selection_from_proxy_state(gfn.env.states2proxy([st])[0])
        samples.append(sel)
    return samples, train_s


def avg_ambiguity(samples):
    return float(np.mean([ambiguity(s) for s in samples]))


def main():
    n_steps = int(sys.argv[1]) if len(sys.argv) > 1 else N_STEPS_DEFAULT
    candidates = all_candidates()
    uniform_avg = float(np.mean([ambiguity(c) for c in candidates]))

    print("=" * 72)
    print("SIGN-CORRECTION RE-RUN — epistemic pull (SYNTHETIC DEMO)")
    print("=" * 72)
    print(f"Uniform-random baseline avg ambiguity: {uniform_avg:.4f}")
    print(f"Steps: {n_steps}, K={K_SAMPLES} samples per run")

    # --- Theoretical pull from true reward-normalized distribution ---
    print("\n--- THEORETICAL pull (from true reward-normalized dist) ---")
    print(f"  {'sign':>20} {'lambda':>6} {'theor_pull':>10} {'R_ratio':>8}")
    for sign_label, sign in [("category-error (+)", +1), ("corrected (-)", -1)]:
        for lam in [1, 3, 5, 10]:
            rewards = np.array([
                math.exp(-(sp.risk(c) + sign * lam * sp.ambiguity(c)))
                for c in candidates
            ])
            probs = rewards / rewards.sum()
            theor_amb = float(np.sum([
                probs[i] * ambiguity(candidates[i])
                for i in range(len(candidates))
            ]))
            theor_pull = theor_amb - uniform_avg
            ratio = float(max(rewards) / min(rewards))
            print(f"  {sign_label:>20} {lam:>6} {theor_pull:>+10.4f} {ratio:>8.1f}")

    # --- Category-error baseline (old sign: EFE = risk + lambda*ambiguity) ---
    # Temporarily patch efe/reward to the old sign
    print("\n--- CATEGORY-ERROR BASELINE (old sign: risk + lambda*ambiguity) ---")
    orig_efe = sp.efe
    orig_reward = sp.reward

    def efe_old(sel, lam=1.0):
        return sp.risk(sel) + lam * sp.ambiguity(sel)

    def reward_old(sel, lam=1.0):
        return math.exp(-efe_old(sel, lam))

    sp.efe = efe_old
    sp.reward = reward_old
    sp._ACTIVE_LAMBDA = 1.0

    print(f"  lambda=1 (old sign)...", flush=True)
    samples_old, t_old = train_and_sample(n_steps, SEED_BASE)
    pull_old = avg_ambiguity(samples_old) - uniform_avg
    distinct_old = len(set(tuple(sorted(s)) for s in samples_old))
    print(f"  Trained {t_old:.0f}s | distinct={distinct_old}/56 | "
          f"avg_amb={avg_ambiguity(samples_old):.4f} | pull={pull_old:+.4f}")

    # Restore corrected sign
    sp.efe = orig_efe
    sp.reward = orig_reward

    # --- Corrected sign at lambda=1, 3, 5, 10 ---
    results = []
    for lam in [1, 3, 5, 10]:
        print(f"\n--- CORRECTED SIGN (EFE = risk - {lam}*EIG) ---", flush=True)
        sp._ACTIVE_LAMBDA = lam
        samples, t_s = train_and_sample(n_steps, SEED_BASE + lam * 100)
        pull = avg_ambiguity(samples) - uniform_avg
        distinct = len(set(tuple(sorted(s)) for s in samples))
        avg_amb = avg_ambiguity(samples)
        print(f"  lambda={lam} | Trained {t_s:.0f}s | distinct={distinct}/56 | "
              f"avg_amb={avg_amb:.4f} | pull={pull:+.4f}", flush=True)
        results.append((lam, pull, avg_amb, distinct))

    # --- Summary ---
    print("\n" + "=" * 72)
    print("EPISTEMIC PULL SUMMARY (avg_ambiguity - uniform_baseline)")
    print("=" * 72)
    print(f"  {'sign':>20} {'lambda':>6} {'avg_amb':>8} {'pull':>8} {'distinct':>8}")
    print(f"  {'category-error (+)':>20} {1:>6} {avg_ambiguity(samples_old):>8.4f} "
          f"{pull_old:>+8.4f} {distinct_old:>8}")
    for lam, pull, avg_amb, distinct in results:
        print(f"  {'corrected (−)':>20} {lam:>6} {avg_amb:>8.4f} "
              f"{pull:>+8.4f} {distinct:>8}")

    grows = all(
        results[i][1] < results[i + 1][1] for i in range(len(results) - 1)
    )
    all_positive = all(r[1] > 0 for r in results)
    print(f"\n  Prediction: pull POSITIVE and GROWS with lambda?")
    print(f"    All positive: {all_positive}")
    print(f"    Monotone increasing: {grows}")
    if all_positive and grows:
        print("    -> CONFIRMED: corrected sign steers toward high-stddev (epistemic value)")
    else:
        print("    -> PARTIAL: see individual values for details")

    print("\n[DEMO COMPLETE — synthetic data, not production]")


if __name__ == "__main__":
    main()
