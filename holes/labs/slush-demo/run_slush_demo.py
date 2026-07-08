#!/usr/bin/env python
"""Run the GFN slush on the real P0-P17 constellation reward.

This retires the synthetic 8-concept toy. The choices are the existing
embedding constellations from pipeline-semilattice-clusters.edn, and the
epistemic term is the A4a constellation posterior stddev over the real
mission-pattern co-occurrence corpus.

Usage:
    /home/joe/code/gflownet/.venv/bin/python run_slush_demo.py [N_STEPS] [MISSION]
"""
from __future__ import annotations

import sys
import time
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, "/home/joe/code/gflownet")

import slush_proxy as sp  # noqa: E402
from slush_proxy import (  # noqa: E402
    CONSTELLATION_EIG,
    CONSTELLATION_LABELS,
    COVERAGE_WEIGHT,
    LAMBDA,
    MAX_SELECTION,
    N_CONCEPTS,
    PASS_MIN_COVERAGE,
    TARGET_MISSION,
    a3_passes,
    all_candidates,
    coverage_count,
    efe,
    eig,
    greedy_candidate,
    greedy_tie_count,
    reward,
    selection_from_proxy_state,
    set_active_target,
    target_constellations,
)

K_SAMPLES = 1024
N_STEPS_DEFAULT = 1200
SEED = 20260709


def true_distribution(candidates, target):
    rewards = np.array([reward(c, target_mission=target) for c in candidates], dtype=np.float64)
    return rewards / rewards.sum(), rewards


def make_config(n_steps: int):
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
            f"seed={SEED}",
            "device=cpu",
        ],
    )


def train_and_sample(n_steps: int):
    from gflownet.utils.common import gflownet_from_config

    config = make_config(n_steps)
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


def js_divergence(p, q):
    p = np.asarray(p, dtype=np.float64)
    q = np.asarray(q, dtype=np.float64)
    m = 0.5 * (p + q)

    def kl(a, b):
        mask = a > 0
        return np.sum(a[mask] * np.log2(a[mask] / b[mask]))

    return 0.5 * kl(p, m) + 0.5 * kl(q, m)


def sampled_distribution(samples, candidates, cand_index):
    counts = np.zeros(len(candidates), dtype=np.float64)
    for s in samples:
        key = tuple(sorted(s))
        if key in cand_index:
            counts[cand_index[key]] += 1.0
    total = counts.sum()
    return counts if total == 0 else counts / total


def fmt_candidate(c):
    return "(" + " ".join(f"P{i}" for i in c) + ")"


def main():
    n_steps = int(sys.argv[1]) if len(sys.argv) > 1 else N_STEPS_DEFAULT
    target = sys.argv[2] if len(sys.argv) > 2 else TARGET_MISSION
    set_active_target(target)

    candidates = all_candidates()
    cand_index = {c: i for i, c in enumerate(candidates)}
    true_dist, true_rewards = true_distribution(candidates, target)
    n_cand = len(candidates)
    target_cs = sorted(target_constellations(target))

    print("=" * 72)
    print("GFN SLUSH — real P0-P17 constellation reward")
    print("=" * 72)
    print(f"Target mission: {target}")
    print(f"Selection: {MAX_SELECTION}-of-{N_CONCEPTS} constellations ({n_cand} candidates)")
    print(f"Target constellations ({len(target_cs)}): {[f'P{i}' for i in target_cs]}")
    print(f"Reward: exp({COVERAGE_WEIGHT}*coverage + {LAMBDA}*EIG)")
    print(f"A3-pass proxy: coverage >= {PASS_MIN_COVERAGE} target constellations")

    print("\n--- Constellation EIG (A4a Dirichlet stddev, real corpus) ---")
    for c in range(N_CONCEPTS):
        mark = "*" if c in target_cs else " "
        print(f" {mark} P{c:<2} eig={CONSTELLATION_EIG.get(c, 0.0):.6f}  {CONSTELLATION_LABELS.get(c, '')}")

    print("\n--- Negative result retained: why embeddings, not count-BMR ---")
    print("  count-BMR accepted 6903/6903 pairs on 118 multi-mission patterns")
    print("  count-BMR collapsed to 1 concept; delta-F mean -11.41, range [-33.70, -6.45]")
    print("  embedding-gate sweep had no clean 5-30 concept setting")
    print("  AIF-12 merge adjudication: OVERREACH (co-occ Jaccard ratio 0.41; live co_app 8.6% cross vs 57.1% within)")

    rng = np.random.default_rng(SEED + 77)
    pre_samples = [candidates[i] for i in rng.integers(0, n_cand, size=K_SAMPLES)]
    pre_dist = sampled_distribution(pre_samples, candidates, cand_index)
    pre_jsd = js_divergence(pre_dist, true_dist)

    print(f"\n--- Training TB GFlowNet ({n_steps} steps, CPU) ---")
    samples, train_s = train_and_sample(n_steps)
    print(f"Trained in {train_s:.1f}s, sampled {len(samples)} terminal states")

    post_dist = sampled_distribution(samples, candidates, cand_index)
    post_jsd = js_divergence(post_dist, true_dist)
    l1 = float(np.abs(post_dist - true_dist).sum())
    corr = 0.0
    mask = (post_dist > 0) | (true_dist > 0)
    if mask.any() and post_dist[mask].std() > 0 and true_dist[mask].std() > 0:
        corr = float(np.corrcoef(post_dist[mask], true_dist[mask])[0, 1])

    distinct = {tuple(sorted(s)) for s in samples}
    passing = {s for s in distinct if a3_passes(s, target)}
    greedy = greedy_candidate(target)
    greedy_passes = a3_passes(greedy, target)
    greedy_reward = reward(greedy, target_mission=target)
    greedy_ties = greedy_tie_count(target)

    reward_threshold_90 = float(np.percentile(true_rewards, 90))
    high_reward_cands = {c for c in candidates if reward(c, target_mission=target) >= reward_threshold_90}
    gfn_high_modes = distinct & high_reward_cands
    greedy_high_modes = {greedy} & high_reward_cands

    avg_cov = float(np.mean([coverage_count(s, target) for s in samples]))
    avg_eig = float(np.mean([eig(s) for s in samples]))
    uniform_cov = float(np.mean([coverage_count(c, target) for c in candidates]))
    uniform_eig = float(np.mean([eig(c) for c in candidates]))

    print("\n" + "=" * 72)
    print("(i) DISTRIBUTION MATCH")
    print("=" * 72)
    print(f"  JSD (pre/uniform  vs true):  {pre_jsd:.6f} bits")
    print(f"  JSD (post/trained vs true):  {post_jsd:.6f} bits")
    print(f"  JSD drop:                    {pre_jsd - post_jsd:+.6f} bits")
    print(f"  L1  (post/trained vs true):  {l1:.4f}")
    print(f"  Pearson corr (post vs true): {corr:.4f}")

    print("\n" + "=" * 72)
    print("(ii) MODE SPREAD / A3-PASSING CANDIDATES")
    print("=" * 72)
    print(f"  Distinct candidates sampled:        {len(distinct)} / {n_cand}")
    print(f"  Distinct A3-passing candidates:     {len(passing)}")
    for cand in sorted(passing, key=lambda c: (-reward(c, target_mission=target), c))[:10]:
        print(
            f"    {fmt_candidate(cand)} cov={coverage_count(cand, target)} "
            f"eig={eig(cand):.6f} reward={reward(cand, target_mission=target):.4f}"
        )

    print("\n" + "=" * 72)
    print("(iii) GREEDY BASELINE")
    print("=" * 72)
    print(f"  Greedy candidate: {fmt_candidate(greedy)}")
    print(f"  Greedy cov={coverage_count(greedy, target)} eig={eig(greedy):.6f} reward={greedy_reward:.4f}")
    print(f"  Greedy A3-passing? {greedy_passes}")
    print(f"  Greedy cutoff tie count: {greedy_ties}")
    if greedy_ties > 1:
        print("  KILL-TEST: greedy ties at the cutoff; report as tie-sensitive.")
    print(f"  High-reward modes (top 10%): {len(high_reward_cands)}")
    print(f"  GFN covers {len(gfn_high_modes)} / {len(high_reward_cands)} top modes")
    print(f"  Greedy covers {len(greedy_high_modes)} / {len(high_reward_cands)} top modes")

    print("\n" + "=" * 72)
    print("(iv) REAL EIG PULL")
    print("=" * 72)
    print(f"  Avg coverage, GFN samples:  {avg_cov:.4f}  (uniform {uniform_cov:.4f})")
    print(f"  Avg EIG, GFN samples:       {avg_eig:.6f}  (uniform {uniform_eig:.6f})")
    print("  Sign check: EIG is subtracted in EFE, so higher EIG increases reward.")

    print("\n" + "=" * 72)
    print("SUMMARY")
    print("=" * 72)
    print(f"  JSD post-train: {post_jsd:.6f} bits (pre {pre_jsd:.6f}, drop {pre_jsd - post_jsd:+.6f})")
    print(f"  Distinct sampled: {len(distinct)} / {n_cand}")
    print(f"  Distinct A3-passing sampled: {len(passing)}")
    print(f"  GFN top-mode coverage / greedy: {len(gfn_high_modes)} / {len(greedy_high_modes)}")
    print(f"  Greedy tie count at cutoff: {greedy_ties}")


if __name__ == "__main__":
    main()
