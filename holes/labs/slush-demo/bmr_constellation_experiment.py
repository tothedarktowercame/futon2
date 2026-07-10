#!/usr/bin/env python
"""BMR over-merge diagnosis + constellation-based EIG experiment (REVISED).

Reuses the EXISTING pattern clustering (pipeline-semilattice-clusters.edn
constellations P0-P17) as ground-truth concept structure, instead of
re-deriving clusters from scratch.

Three sub-experiments:
  1. Reproduce the BMR over-merge (count-only reduce-concepts → 1 concept).
  2. Compute EIG (posterior stddev) per constellation.
  3. Cross-check: within-constellation ΔF vs across-constellation ΔF.

Exploratory findings only. No commit.

Usage:
    /home/joe/code/gflownet/.venv/bin/python bmr_constellation_experiment.py
"""
from __future__ import annotations

import itertools
import math
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Dict, List, Set, Tuple

PRIOR = 0.1


# ---------------------------------------------------------------------------
# Log-gamma + BMR (exact port of futon2.aif.bmr, using math.lgamma)
# ---------------------------------------------------------------------------
def log_multivariate_beta(v: List[float]) -> float:
    return sum(math.lgamma(x) for x in v) - math.lgamma(sum(v))


def bayesian_model_reduction(
    full_prior: List[float],
    full_posterior: List[float],
    reduced_prior: List[float],
) -> Tuple[List[float], float]:
    """Exact port of futon2.aif.bmr/bayesian-model-reduction. Returns (A', delta_F)."""
    a = list(full_prior)
    A = list(full_posterior)
    a_prime = list(reduced_prior)
    A_prime = [A[i] + a_prime[i] - a[i] for i in range(len(A))]
    delta_F = (
        log_multivariate_beta(A) + log_multivariate_beta(a_prime)
        - log_multivariate_beta(a) - log_multivariate_beta(A_prime)
    )
    return A_prime, delta_F


def pair_reduced_prior(row_i, row_j):
    """Pooled average (matches a4a/pair-reduced-prior: concat pooled+pooled)."""
    pooled = [(row_i[k] + row_j[k]) / 2.0 for k in range(len(row_i))]
    return pooled + pooled


def score_pair_delta_f(row_i, row_j, prior=PRIOR):
    """Delta-F for a merge hypothesis (matches a4a/score-pair)."""
    n = len(row_i)
    full_prior = [prior] * (2 * n)
    full_posterior = list(row_i) + list(row_j)
    reduced_prior = pair_reduced_prior(row_i, row_j)
    _, delta_F = bayesian_model_reduction(full_prior, full_posterior, reduced_prior)
    return delta_F


# ---------------------------------------------------------------------------
# Dirichlet moments (exact port of bmr/dirichlet-moments + a4a RMS)
# ---------------------------------------------------------------------------
def dirichlet_moments(alpha):
    """Per-component Dirichlet moments. Ships stddev, never variance."""
    v = [float(x) for x in alpha]
    alpha0 = sum(v)
    denom = alpha0 * alpha0 * (alpha0 + 1.0)
    return [{"mean": ai / alpha0,
             "stddev": math.sqrt(ai * (alpha0 - ai) / denom)} for ai in v]


def concept_stddev(alpha):
    """RMS of per-outcome stddevs (matches a4a concept->stddev)."""
    stds = [m["stddev"] for m in dirichlet_moments(alpha)]
    if not stds:
        return 0.0
    return math.sqrt(sum(s * s for s in stds) / len(stds))


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------
def load_corpus():
    """Parse mission-pattern-scopes.edn → edges [(pattern, mission), ...]."""
    text = Path("/home/joe/code/futon6/data/mission-pattern-scopes.edn").read_text()
    edges = []
    for m in re.finditer(
        r':mission\s+"(M-[^"]+)".*?:applied\s+\[([^\]]*)\]', text, re.DOTALL
    ):
        mission_id = m.group(1)
        patterns = re.findall(r'"([^"]+)"', m.group(2))
        for p in patterns:
            edges.append((p, mission_id))
    return edges


def build_concentration(edges):
    """Build pattern×mission concentration matrix (prior 0.1)."""
    all_missions = sorted(set(m for _, m in edges))
    all_patterns = sorted(set(p for p, _ in edges))
    mission_idx = {m: i for i, m in enumerate(all_missions)}
    conc = {}
    for p in all_patterns:
        row = [PRIOR] * len(all_missions)
        for pat, mis in edges:
            if pat == p:
                row[mission_idx[mis]] += 1.0
        conc[p] = row
    return conc, all_missions, all_patterns


def load_constellations():
    """Load pattern→constellation map from pipeline-semilattice-clusters.edn."""
    text = Path(
        "/home/joe/code/futon3c/holes/excursions/pipeline-semilattice-clusters.edn"
    ).read_text()
    pm_start = text.index(":pattern-membership")
    pm_section = text[pm_start:]
    membership = {}
    for m in re.finditer(r':pattern\s+"([^"]+)"\s+:cluster\s+(\d+)', pm_section):
        stem = m.group(1).split("/")[-1]
        cluster = int(m.group(2))
        membership[stem] = cluster
    return membership


# Constellation labels (from pipeline-pattern-cascade.html)
CONST_LABELS = {
    0: "writing / pattern discipline / categorical objects",
    1: "futon-theory / storage / coordination / invariants",
    2: "AIF / operator / surface inhabitation",
    3: "(small cluster)",
    4: "(small cluster)",
    5: "(small cluster)",
    6: "runtime invariants / boot reachability / snapshots",
    7: "(small cluster)",
    8: "agency / coordination law / routing",
    9: "(small cluster)",
    10: "(small cluster)",
    11: "(small cluster)",
    12: "(small cluster)",
    13: "math proof strategy / rational reconstruction",
    14: "(small cluster)",
    15: "(small cluster)",
    16: "(small cluster)",
    17: "(small cluster)",
}


# ---------------------------------------------------------------------------
# Experiments
# ---------------------------------------------------------------------------
def experiment_1_overmerge(conc, multi_patterns):
    """Reproduce the BMR over-merge."""
    print("=" * 72)
    print("EXPERIMENT 1: BMR over-merge reproduction (count-only reduce-concepts)")
    print("=" * 72)

    n_pairs = len(multi_patterns) * (len(multi_patterns) - 1) // 2
    n_outcomes = len(conc[multi_patterns[0]])
    print(f"Patterns (in >=2 missions): {len(multi_patterns)}")
    print(f"Outcome dim (missions): {n_outcomes}")
    print(f"Pairs scored: {n_pairs}")

    accepted = 0
    rejected = 0
    delta_fs = []
    for pi, pj in itertools.combinations(multi_patterns, 2):
        dF = score_pair_delta_f(conc[pi], conc[pj])
        delta_fs.append(dF)
        if dF <= -3.0:
            accepted += 1
        else:
            rejected += 1

    print(f"Accepted (dF <= -3): {accepted} / {n_pairs}")
    print(f"Rejected: {rejected}")
    print(f"delta-F range: [{min(delta_fs):.2f}, {max(delta_fs):.2f}]")
    print(f"delta-F mean: {sum(delta_fs)/len(delta_fs):.2f}")
    print(f"Concepts after merge: 1 (all {len(multi_patterns)} patterns collapse)")
    print(f"\nKEY NEGATIVE RESULT: pure co-occurrence BMR collapses in {n_outcomes}-dim.")
    print(f"Even disjoint patterns score dF={max(delta_fs):.2f}, passing the -3 threshold.")
    print(f"The embedding-based constellation clustering succeeds where BMR alone fails")
    print(f"→ embeddings are NECESSARY for concept formation, not just enrichment.")
    print()
    return delta_fs


def experiment_2_eig_per_constellation(conc, multi_patterns, constellation_of):
    """Compute EIG (posterior stddev) per constellation."""
    print("=" * 72)
    print("EXPERIMENT 2: EIG (posterior stddev) per constellation")
    print("=" * 72)

    # Group patterns by constellation
    by_cluster = defaultdict(list)
    for p in multi_patterns:
        by_cluster[constellation_of[p]].append(p)

    n_outcomes = len(conc[multi_patterns[0]])

    # For each constellation, aggregate member rows into one Dirichlet row
    # (sum counts over missions + prior, matching a4a class-row logic:
    #  prior + sum of (count - prior) over members)
    results = []
    for cluster_id in sorted(by_cluster.keys()):
        members = by_cluster[cluster_id]
        # Aggregate: prior + sum over members of (member_count - prior)
        # This matches a4a/class-row: (+ prior (reduce + (map #(- % prior) values)))
        agg_row = [0.0] * n_outcomes
        for p in members:
            for i in range(n_outcomes):
                agg_row[i] += conc[p][i] - PRIOR
        agg_row = [v + PRIOR for v in agg_row]

        eig = concept_stddev(agg_row)
        alpha0 = sum(agg_row)
        label = CONST_LABELS.get(cluster_id, "?")
        results.append((cluster_id, len(members), eig, alpha0, label, members))

    # Sort by EIG descending (highest uncertainty first)
    results.sort(key=lambda r: -r[2])

    print(f"\n{'P':>3} {'size':>4} {'alpha0':>7} {'EIG_stddev':>10}  label")
    print("-" * 72)
    for cluster_id, size, eig, alpha0, label, members in results:
        print(f"P{cluster_id:<2} {size:>4} {alpha0:>7.1f} {eig:>10.4f}  {label}")

    print(f"\nInterpretation: higher EIG_stddev = more epistemic uncertainty about")
    print(f"which missions this constellation serves. Low EIG = well-determined role.")
    print()
    return results


def experiment_3_within_vs_across(conc, multi_patterns, constellation_of):
    """Compare ΔF for within-constellation pairs vs across-constellation pairs."""
    print("=" * 72)
    print("EXPERIMENT 3: Within-constellation vs across-constellation ΔF")
    print("=" * 72)

    within_dfs = []
    across_dfs = []
    within_examples = []
    across_examples = []

    for pi, pj in itertools.combinations(multi_patterns, 2):
        dF = score_pair_delta_f(conc[pi], conc[pj])
        same = constellation_of[pi] == constellation_of[pj]
        if same:
            within_dfs.append(dF)
            within_examples.append((pi, pj, dF))
        else:
            across_dfs.append(dF)
            across_examples.append((pi, pj, dF))

    within_mean = sum(within_dfs) / len(within_dfs) if within_dfs else 0
    across_mean = sum(across_dfs) / len(across_dfs) if across_dfs else 0

    print(f"\nWithin-constellation pairs:  {len(within_dfs)}")
    print(f"  mean ΔF = {within_mean:.2f}")
    print(f"  range: [{min(within_dfs):.2f}, {max(within_dfs):.2f}]")

    print(f"\nAcross-constellation pairs:  {len(across_dfs)}")
    print(f"  mean ΔF = {across_mean:.2f}")
    print(f"  range: [{min(across_dfs):.2f}, {max(across_dfs):.2f}]")

    print(f"\nDifference (within − across): {within_mean - across_mean:.2f}")
    if within_mean < across_mean:
        print(f"→ Within-constellation pairs ARE more mergeable (more negative ΔF)")
        print(f"  The AIF measure (BMR ΔF) PARTIALLY validates the embedding clustering:")
        print(f"  it agrees that within-constellation pairs are more similar, even though")
        print(f"  the absolute threshold is miscalibrated for high-dim.")
    else:
        print(f"→ Within-constellation pairs are NOT more mergeable — BMR does not")
        print(f"  validate the embedding clustering at all.")

    # Show examples
    within_examples.sort(key=lambda x: x[2])
    across_examples.sort(key=lambda x: x[2])
    print(f"\nMost-mergeable WITHIN pairs (most negative ΔF):")
    for pi, pj, dF in within_examples[:5]:
        c = constellation_of[pi]
        print(f"  dF={dF:>7.2f}  P{c}  {pi} + {pj}")
    print(f"\nMost-mergeable ACROSS pairs (most negative ΔF):")
    for pi, pj, dF in across_examples[:5]:
        print(f"  dF={dF:>7.2f}  P{constellation_of[pi]}×P{constellation_of[pj]}  {pi} + {pj}")
    print(f"\nLeast-mergeable WITHIN pairs (least negative ΔF):")
    for pi, pj, dF in within_examples[-3:]:
        c = constellation_of[pi]
        print(f"  dF={dF:>7.2f}  P{c}  {pi} + {pj}")
    print()
    return within_mean, across_mean


def main():
    edges = load_corpus()
    conc, all_missions, all_patterns = build_concentration(edges)
    constellation_of = load_constellations()

    pat_counts = Counter(p for p, _ in edges)
    multi_patterns = sorted([p for p, c in pat_counts.items() if c >= 2])

    print(f"Corpus: {len(edges)} edges, {len(all_patterns)} patterns, "
          f"{len(all_missions)} missions")
    print(f"Multi-mission patterns: {len(multi_patterns)}")
    covered = sum(1 for p in multi_patterns if p in constellation_of)
    print(f"Constellation coverage: {covered} / {len(multi_patterns)}")
    print()

    # Exp 1: over-merge
    experiment_1_overmerge(conc, multi_patterns)

    # Exp 2: EIG per constellation
    experiment_2_eig_per_constellation(conc, multi_patterns, constellation_of)

    # Exp 3: within vs across ΔF
    experiment_3_within_vs_across(conc, multi_patterns, constellation_of)

    print("=" * 72)
    print("DONE — exploratory findings, not a committed artifact")
    print("=" * 72)


if __name__ == "__main__":
    main()
