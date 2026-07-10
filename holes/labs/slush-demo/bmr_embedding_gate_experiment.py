#!/usr/bin/env python
"""BMR over-merge diagnosis + embedding-gated merge experiment.

Exploratory experiment: diagnose the BMR over-merge on the REAL pattern corpus,
then test an embedding-gate fix using BGE cosine similarity.

NOT a committed artifact. Findings only. No commit.

Usage:
    /home/joe/code/gflownet/.venv/bin/python bmr_embedding_gate_experiment.py
"""
from __future__ import annotations

import itertools
import json
import math
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Dict, List, Set, Tuple

# ---------------------------------------------------------------------------
# Lanczos log-gamma (exact port of futon2.aif.bmr/log-gamma)
# ---------------------------------------------------------------------------
_LANCZOS_G = 7.0
_LANCZOS_C = [
    0.99999999999980993, 676.5203681218851, -1259.1392167224028,
    771.32342877765113, -176.61502916214059, 12.507343279669339,
    -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
]


def log_gamma(z: float) -> float:
    """Log-gamma. Uses math.lgamma (C builtin, ~1e-11 match to Lanczos port)."""
    return math.lgamma(z)


def log_multivariate_beta(v: List[float]) -> float:
    return sum(log_gamma(x) for x in v) - log_gamma(sum(v))


def bayesian_model_reduction(
    full_prior: List[float],
    full_posterior: List[float],
    reduced_prior: List[float],
) -> Tuple[List[float], float, bool]:
    """Exact port of futon2.aif.bmr/bayesian-model-reduction.

    Returns (reduced_posterior, delta_F, accept_at_-3).
    """
    a = list(full_prior)
    A = list(full_posterior)
    a_prime = list(reduced_prior)
    A_prime = [A[i] + a_prime[i] - a[i] for i in range(len(A))]
    delta_F = (
        log_multivariate_beta(A) + log_multivariate_beta(a_prime)
        - log_multivariate_beta(a) - log_multivariate_beta(A_prime)
    )
    return A_prime, delta_F, delta_F <= -3.0


def pair_reduced_prior(row_i: List[float], row_j: List[float]) -> List[float]:
    """Pooled average (matches a4a/pair-reduced-prior: concat pooled+pooled)."""
    pooled = [(row_i[k] + row_j[k]) / 2.0 for k in range(len(row_i))]
    return pooled + pooled


def score_pair(row_i: List[float], row_j: List[float], prior: float = 0.1):
    """Score a merge hypothesis (matches a4a/score-pair). Returns (delta_F, accept)."""
    n = len(row_i)
    full_prior = [prior] * (2 * n)
    full_posterior = list(row_i) + list(row_j)
    reduced_prior = pair_reduced_prior(row_i, row_j)
    _, delta_F, _ = bayesian_model_reduction(full_prior, full_posterior, reduced_prior)
    return delta_F, delta_F <= -3.0


# ---------------------------------------------------------------------------
# Union-find for concept clustering
# ---------------------------------------------------------------------------
class UnionFind:
    def __init__(self, items):
        self.parent = {x: x for x in items}

    def find(self, x):
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]
            x = self.parent[x]
        return x

    def union(self, a, b):
        ra, rb = self.find(a), self.find(b)
        if ra != rb:
            # Keep lexicographically smaller as representative
            survivor = min(ra, rb)
            retired = max(ra, rb)
            self.parent[retired] = survivor

    def groups(self) -> Dict[str, List[str]]:
        g = defaultdict(list)
        for x in self.parent:
            g[self.find(x)].append(x)
        return dict(g)


# ---------------------------------------------------------------------------
# Load corpus + build concentration matrix
# ---------------------------------------------------------------------------
PRIOR = 0.1


def load_corpus():
    """Parse mission-pattern-scopes.edn → edges [(pattern, mission), ...]."""
    text = Path("/home/joe/code/futon6/data/mission-pattern-scopes.edn").read_text()
    edges = []
    for m in re.finditer(
        r':mission\s+"(M-[^"]+)".*?:applied\s+\[([^\]]*)\]', text, re.DOTALL
    ):
        mission_id = m.group(1)
        applied_str = m.group(2)
        patterns = re.findall(r'"([^"]+)"', applied_str)
        for p in patterns:
            edges.append((p, mission_id))
    return edges


def build_concentration(edges):
    """Build pattern×mission concentration matrix (prior 0.1, like a4a)."""
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


def load_embeddings():
    """Load BGE pattern embeddings, return {stem: {id, vector}}."""
    data = json.loads(
        Path("/home/joe/code/futon3a/resources/notions/bge_pattern_embeddings.json").read_text()
    )
    emb = {}
    for rec in data:
        # ID is like "futon-stack/argument" — stem is last segment
        full_id = rec["id"]
        stem = full_id.split("/")[-1]
        emb[stem] = {"id": full_id, "vector": rec["vector"]}
    return emb


def cosine(vec_a, vec_b):
    """Cosine = dot product (vectors are L2-normalized)."""
    return sum(a * b for a, b in zip(vec_a, vec_b))


# ---------------------------------------------------------------------------
# Experiments
# ---------------------------------------------------------------------------
def experiment_1_reproduce_overmerge(conc, multi_patterns):
    """Reproduce the over-merge: BMR on all pairs, count concepts."""
    print("=" * 72)
    print("EXPERIMENT 1: Reproduce the BMR over-merge")
    print("=" * 72)

    n_pairs = len(multi_patterns) * (len(multi_patterns) - 1) // 2
    print(f"Patterns (in >=2 missions): {len(multi_patterns)}")
    print(f"Outcome dim (missions): {len(conc[multi_patterns[0]])}")
    print(f"Pairs to score: {n_pairs}")

    accepted = 0
    rejected = 0
    delta_fs = []
    for pi, pj in itertools.combinations(multi_patterns, 2):
        dF, accept = score_pair(conc[pi], conc[pj])
        delta_fs.append(dF)
        if accept:
            accepted += 1
        else:
            rejected += 1

    print(f"Accepted (dF <= -3): {accepted}")
    print(f"Rejected: {rejected}")
    print(f"delta-F range: [{min(delta_fs):.2f}, {max(delta_fs):.2f}]")
    print(f"delta-F mean: {sum(delta_fs)/len(delta_fs):.2f}")

    # Union-find over accepted pairs
    uf = UnionFind(multi_patterns)
    for pi, pj in itertools.combinations(multi_patterns, 2):
        dF, accept = score_pair(conc[pi], conc[pj])
        if accept:
            uf.union(pi, pj)
    groups = uf.groups()
    print(f"Concepts after merge: {len(groups)}")
    largest = max(len(v) for v in groups.values())
    print(f"Largest concept: {largest} patterns")
    print("→ OVER-MERGE CONFIRMED" if len(groups) <= 2 else "→ No over-merge")
    print()
    return delta_fs


def experiment_2_embedding_gate(conc, multi_patterns, embeddings):
    """Sweep cosine floor {0.4,...,0.8}, report concepts + clusters."""
    print("=" * 72)
    print("EXPERIMENT 2: Embedding-gated merge (cosine floor sweep)")
    print("=" * 72)

    # Precompute cosine for all pairs
    print("Computing pairwise cosines...", flush=True)
    cosines = {}
    missing = set()
    for pi, pj in itertools.combinations(multi_patterns, 2):
        if pi not in embeddings or pj not in embeddings:
            if pi not in embeddings:
                missing.add(pi)
            if pj not in embeddings:
                missing.add(pj)
            continue
        cos = cosine(embeddings[pi]["vector"], embeddings[pj]["vector"])
        cosines[(pi, pj)] = cos

    if missing:
        print(f"WARNING: {len(missing)} patterns missing from embeddings")
        for m in sorted(missing)[:5]:
            print(f"  missing: {m}")

    for floor in [0.4, 0.5, 0.6, 0.7, 0.8]:
        uf = UnionFind(multi_patterns)
        n_scored = 0
        n_accepted = 0
        accepted_pairs = []
        for (pi, pj), cos in cosines.items():
            if cos >= floor:
                n_scored += 1
                dF, accept = score_pair(conc[pi], conc[pj])
                if accept:
                    uf.union(pi, pj)
                    n_accepted += 1
                    accepted_pairs.append((pi, pj, cos, dF))

        groups = uf.groups()
        # Filter to groups with >1 member (actual merges)
        multi_groups = {k: v for k, v in groups.items() if len(v) > 1}
        singletons = {k: v for k, v in groups.items() if len(v) == 1}

        print(f"\n  Floor={floor:.1f}: pairs_above_floor={n_scored} "
              f"accepted={n_accepted} concepts={len(groups)} "
              f"({len(multi_groups)} multi-member, {len(singletons)} singletons)")

        if multi_groups:
            # Show 3-5 example clusters sorted by size
            sorted_clusters = sorted(multi_groups.values(), key=len, reverse=True)
            for cluster in sorted_clusters[:5]:
                # Compute avg pairwise cosine within cluster
                intra_cos = []
                for a, b in itertools.combinations(cluster, 2):
                    if (a, b) in cosines:
                        intra_cos.append(cosines[(a, b)])
                avg_cos = sum(intra_cos) / len(intra_cos) if intra_cos else 0
                members_str = ", ".join(cluster[:8])
                if len(cluster) > 8:
                    members_str += f", ... ({len(cluster)} total)"
                print(f"    [{len(cluster)}] {members_str}")
                print(f"         avg intra-cos={avg_cos:.3f}")
        elif len(groups) > 0:
            print(f"    (no multi-member clusters; all {len(singletons)} singletons)")

    print()
    return cosines


def experiment_3_threshold_sensitivity(conc, multi_patterns, cosines, floor=0.6):
    """At fixed cosine floor, sweep stricter delta-F cutoffs."""
    print("=" * 72)
    print(f"EXPERIMENT 3: Threshold sensitivity (cosine floor={floor})")
    print("=" * 72)

    for cutoff in [-3, -10, -20, -30]:
        uf = UnionFind(multi_patterns)
        n_scored = 0
        n_accepted = 0
        for (pi, pj), cos in cosines.items():
            if cos >= floor:
                n_scored += 1
                dF, _ = score_pair(conc[pi], conc[pj])
                if dF <= cutoff:
                    uf.union(pi, pj)
                    n_accepted += 1

        groups = uf.groups()
        multi_groups = {k: v for k, v in groups.items() if len(v) > 1}
        largest = max((len(v) for v in groups.values()), default=0)
        print(f"  cutoff={cutoff:>4}: pairs_scored={n_scored} accepted={n_accepted} "
              f"concepts={len(groups)} (multi={len(multi_groups)}, largest={largest})")

    # Also sweep at floor=0.7 and 0.8
    for floor2 in [0.7, 0.8]:
        print(f"\n  --- floor={floor2} ---")
        for cutoff in [-3, -10, -20, -30]:
            uf = UnionFind(multi_patterns)
            n_scored = 0
            n_accepted = 0
            for (pi, pj), cos in cosines.items():
                if cos >= floor2:
                    n_scored += 1
                    dF, _ = score_pair(conc[pi], conc[pj])
                    if dF <= cutoff:
                        uf.union(pi, pj)
                        n_accepted += 1

            groups = uf.groups()
            multi_groups = {k: v for k, v in groups.items() if len(v) > 1}
            largest = max((len(v) for v in groups.values()), default=0)
            print(f"  cutoff={cutoff:>4}: pairs_scored={n_scored} accepted={n_accepted} "
                  f"concepts={len(groups)} (multi={len(multi_groups)}, largest={largest})")

    print()
    return


def experiment_4_recommendation(conc, multi_patterns, cosines):
    """Find best (floor, cutoff) and show example clusters."""
    print("=" * 72)
    print("EXPERIMENT 4: Recommended (floor, cutoff)")
    print("=" * 72)

    results = []
    for floor in [0.4, 0.5, 0.6, 0.7, 0.8]:
        for cutoff in [-3, -10, -20, -30]:
            uf = UnionFind(multi_patterns)
            n_accepted = 0
            for (pi, pj), cos in cosines.items():
                if cos >= floor:
                    dF, _ = score_pair(conc[pi], conc[pj])
                    if dF <= cutoff:
                        uf.union(pi, pj)
                        n_accepted += 1
            groups = uf.groups()
            n_concepts = len(groups)
            multi_groups = [v for v in groups.values() if len(v) > 1]
            largest = max((len(v) for v in groups.values()), default=0)
            results.append((floor, cutoff, n_concepts, len(multi_groups),
                            largest, n_accepted, groups, multi_groups))

    # Print full grid
    print("\n  Full grid (floor × cutoff → #concepts / #multi / largest):")
    print(f"  {'floor':>5} {'cutoff':>6} {'concepts':>8} {'multi':>5} {'largest':>7} {'accepted':>8}")
    for floor, cutoff, nc, nm, lg, na, _, _ in results:
        marker = " ←" if 5 <= nc <= 30 and lg < 20 else ""
        print(f"  {floor:>5} {cutoff:>6} {nc:>8} {nm:>5} {lg:>7} {na:>8}{marker}")

    # Find best: 5-30 concepts, no degenerate mega-cluster (largest < 20)
    sensible = [r for r in results if 5 <= r[2] <= 30 and r[4] < 20]
    if sensible:
        # Pick the one with most multi-member clusters
        best = max(sensible, key=lambda r: r[3])
        floor, cutoff, n_concepts, n_multi, largest, n_acc, groups, multi_groups = best
        print(f"\n  RECOMMENDED: floor={floor}, cutoff={cutoff}")
        print(f"  → {n_concepts} concepts ({n_multi} multi-member clusters, largest={largest})")
        multi_groups_sorted = sorted(multi_groups, key=len, reverse=True)
        print(f"\n  Example clusters:")
        for cluster in multi_groups_sorted[:8]:
            intra_cos = []
            for a, b in itertools.combinations(cluster, 2):
                if (a, b) in cosines:
                    intra_cos.append(cosines[(a, b)])
            avg_cos = sum(intra_cos) / len(intra_cos) if intra_cos else 0
            print(f"    [{len(cluster)}] (avg cos={avg_cos:.3f})")
            for m in cluster[:6]:
                print(f"        {m}")
            if len(cluster) > 6:
                print(f"        ... ({len(cluster)} total)")
    else:
        print("\n  NO (floor, cutoff) gives 5-30 concepts without a degenerate mega-cluster.")
        print("  → The count representation IS the wrong substrate for BMR at this dimension.")
        print("  → Lean harder on embeddings for concept formation.")

        # Show the least-degenerate option
        # Find floor=0.8 results (highest cosine gate)
        f08 = [r for r in results if r[0] == 0.8 and r[2] > 1]
        if f08:
            best = max(f08, key=lambda r: r[3])
            floor, cutoff, n_concepts, n_multi, largest, n_acc, groups, multi_groups = best
            print(f"\n  Best available (floor={floor}, cutoff={cutoff}):")
            print(f"  → {n_concepts} concepts ({n_multi} multi-member clusters)")
            multi_groups_sorted = sorted(multi_groups, key=len, reverse=True)
            for cluster in multi_groups_sorted[:8]:
                intra_cos = []
                for a, b in itertools.combinations(cluster, 2):
                    if (a, b) in cosines:
                        intra_cos.append(cosines[(a, b)])
                avg_cos = sum(intra_cos) / len(intra_cos) if intra_cos else 0
                print(f"    [{len(cluster)}] (avg cos={avg_cos:.3f})")
                for m in cluster[:6]:
                    print(f"        {m}")

    print()
    return


def main():
    edges = load_corpus()
    conc, all_missions, all_patterns = build_concentration(edges)
    embeddings = load_embeddings()

    pat_counts = Counter(p for p, _ in edges)
    multi_patterns = sorted([p for p, c in pat_counts.items() if c >= 2])

    print(f"Corpus: {len(edges)} edges, {len(all_patterns)} patterns, "
          f"{len(all_missions)} missions")
    print(f"Multi-mission patterns: {len(multi_patterns)}")
    print(f"Embeddings loaded: {len(embeddings)} patterns")
    print()

    # Exp 1: reproduce over-merge
    experiment_1_reproduce_overmerge(conc, multi_patterns)

    # Exp 2: embedding-gate sweep
    cosines = experiment_2_embedding_gate(conc, multi_patterns, embeddings)

    # Exp 3: threshold sensitivity at floor=0.6
    experiment_3_threshold_sensitivity(conc, multi_patterns, cosines, floor=0.6)

    # Exp 4: recommendation
    experiment_4_recommendation(conc, multi_patterns, cosines)

    print("=" * 72)
    print("DONE — exploratory findings, not a committed artifact")
    print("=" * 72)


if __name__ == "__main__":
    main()
