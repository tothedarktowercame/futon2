#!/usr/bin/env python
"""BMR over-merge diagnosis + constellation-based EIG experiment (REVISED).

Reuses the EXISTING pattern clustering (pipeline-semilattice-clusters.edn
constellations P0-P17) as ground-truth concept structure, instead of
re-deriving clusters from scratch.

Three sub-experiments:
  1. Reproduce the BMR over-merge (count-only reduce-concepts → 1 concept).
  2. Compute EIG (posterior stddev) per constellation.
  3. Cross-check: within-constellation ΔF vs across-constellation ΔF.

Then a fourth section that can go red (D61a, defect class 1). Experiments 1-3
print counts and never reject, so before D61a nothing here could fail. The
ACCEPTANCE section states the population, scores two null baselines and one
dimension control, and asserts ten conditions; any red exits non-zero.

Deterministic: stdlib only, no environment input, both randomised checks seeded
from SEED below. Runs in ~10 s.

Usage:
    python3 bmr_constellation_experiment.py            # exit 0 = all checks green
"""
from __future__ import annotations

import itertools
import math
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Dict, List, Set, Tuple

PRIOR = 0.1

# --- acceptance parameters (D61a) --------------------------------------------
THRESHOLD = -3.0        # the merge rule under test: accept iff dF <= THRESHOLD
SEED = 20260902         # both randomised checks draw from this; output is stable
NULL_REPLICATES = 20    # degree-preserving corpus shuffles (check 8, check 9)
PERMUTATIONS = 200      # constellation-label shuffles (check 10)
DIM_SWEEP = (2, 4, 8, 16, 32)   # mission-axis sizes for the dimension control


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
        if dF <= THRESHOLD:
            accepted += 1
        else:
            rejected += 1

    print(f"Accepted (dF <= {THRESHOLD:.0f}): {accepted} / {n_pairs}")
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


# ---------------------------------------------------------------------------
# ACCEPTANCE (D61a): the part of this script that can go red
# ---------------------------------------------------------------------------
def _check(results, name, ok, detail):
    """Record one assertion. `ok` False makes the whole run exit non-zero."""
    results.append((name, bool(ok), detail))
    print(f"  [{'PASS' if ok else 'RED '}] {name}\n         {detail}")


def _accept_rate(rows, pairs):
    """Fraction of `pairs` the merge rule accepts, scoring the given rows."""
    n = sum(1 for a, b in pairs if score_pair_delta_f(rows[a], rows[b]) <= THRESHOLD)
    return n / len(pairs), n


def acceptance(edges, conc, all_missions, multi_patterns, constellation_of, delta_fs):
    """State the population, run the controls and nulls, assert what can fail.

    `delta_fs` is experiment 1's list, in itertools.combinations order, reused
    so the observed scores here are the same numbers experiment 1 printed.
    """
    import random

    print("=" * 72)
    print("ACCEPTANCE (D61a): stated population, null baselines, failable checks")
    print("=" * 72)

    pairs = list(itertools.combinations(multi_patterns, 2))
    n_pat = len(multi_patterns)
    n_out = len(all_missions)
    results = []

    print("\nPOPULATION UNDER TEST")
    print(f"  subjects   : unordered pairs of patterns occupying >= 2 of the {n_out}")
    print(f"               missions in mission-pattern-scopes.edn ({len(edges)} edges,")
    print(f"               {n_pat} such patterns, {len(pairs)} pairs)")
    print(f"  instrument : score_pair_delta_f, prior {PRIOR}, accept iff dF <= {THRESHOLD:.0f}")
    print(f"  claim      : the accept COUNT is dimension-driven and carries no")
    print(f"               similarity signal; the ΔF MEAN and the within/across")
    print(f"               gap do carry one.")
    print()

    # --- the population is real, not empty and not mis-stated ---------------
    _check(results, "population-nonempty-and-enumerated",
           n_pat >= 2 and len(pairs) == n_pat * (n_pat - 1) // 2 and len(pairs) > 0,
           f"{n_pat} patterns -> {len(pairs)} pairs (= n(n-1)/2)")

    distinct = {p: sum(1 for v in conc[p] if v > PRIOR) for p in multi_patterns}
    short = [p for p in multi_patterns if distinct[p] < 2]
    _check(results, "population-predicate-holds",
           not short,
           f"members occupying < 2 DISTINCT missions: {len(short)}"
           + (f" e.g. {short[:3]}" if short else "")
           + " (selection counts edges; this recounts missions)")

    uncovered = [p for p in multi_patterns if p not in constellation_of]
    _check(results, "constellation-coverage-complete",
           not uncovered,
           f"members with no constellation: {len(uncovered)}"
           + (f" e.g. {uncovered[:3]}" if uncovered else ""))

    # --- the instrument can do both things ----------------------------------
    p0 = multi_patterns[0]
    self_df = score_pair_delta_f(conc[p0], conc[p0])
    _check(results, "scorer-accepts-identical-rows",
           self_df <= THRESHOLD,
           f"{p0} merged with a copy of itself: dF = {self_df:.2f}")

    void_df = score_pair_delta_f([PRIOR] * n_out, [PRIOR] * n_out)
    _check(results, "scorer-rejects-evidence-free-rows",
           void_df > THRESHOLD,
           f"two all-prior rows in {n_out}-dim: dF = {void_df:.2f} "
           f"(a scorer that accepted these would accept anything)")

    # --- the sentence experiment 1 prints about disjoint pairs --------------
    worst_i = max(range(len(delta_fs)), key=lambda i: delta_fs[i])
    wi, wj = pairs[worst_i]
    shared = sum(1 for k in range(n_out) if conc[wi][k] > PRIOR and conc[wj][k] > PRIOR)
    _check(results, "least-mergeable-accepted-pair-is-disjoint",
           delta_fs[worst_i] <= THRESHOLD and shared == 0,
           f"dF = {delta_fs[worst_i]:.2f}, shared missions = {shared}: "
           f"{wi} + {wj}")

    # --- dimension control: rescore the same pairs on fewer missions --------
    mission_edges = Counter(m for _, m in edges)
    ranked = [m for m, _ in sorted(mission_edges.items(), key=lambda x: (-x[1], x[0]))]
    idx = {m: i for i, m in enumerate(all_missions)}
    sweep = []
    for k in list(DIM_SWEEP) + [n_out]:
        keep = [idx[m] for m in ranked[:k]]
        rows = {p: [conc[p][i] for i in keep] for p in multi_patterns}
        rate, n_acc = _accept_rate(rows, pairs)
        sweep.append((k, rate, n_acc))
    print("\n  dimension control (same pairs, top-k missions by edge count):")
    for k, rate, n_acc in sweep:
        print(f"    k={k:>3}  accepted {n_acc:>5}/{len(pairs)} = {rate:.4f}")
    monotone = all(sweep[i][1] <= sweep[i + 1][1] + 1e-12 for i in range(len(sweep) - 1))
    _check(results, "dimension-drives-acceptance",
           sweep[0][1] < 1.0 and monotone,
           f"accept rate {sweep[0][1]:.4f} at k={sweep[0][0]} rising to "
           f"{sweep[-1][1]:.4f} at k={sweep[-1][0]}, monotone={monotone}; "
           f"a flat 1.0000 here would refute the 'collapses in {n_out}-dim' reading")

    # --- expected-if-null: degree-preserving corpus shuffle -----------------
    # Permuting the mission column of the edge list keeps every pattern's edge
    # count and every mission's edge count, and destroys which pattern meets
    # which mission. The population is therefore unchanged by construction.
    rng = random.Random(SEED)
    obs_rate, obs_acc = len([d for d in delta_fs if d <= THRESHOLD]) / len(pairs), \
        len([d for d in delta_fs if d <= THRESHOLD])
    obs_mean = sum(delta_fs) / len(delta_fs)
    null_rates, null_means = [], []
    for _ in range(NULL_REPLICATES):
        shuffled = [m for _, m in edges]
        rng.shuffle(shuffled)
        null_edges = [(edges[i][0], shuffled[i]) for i in range(len(edges))]
        null_conc, _, _ = build_concentration(null_edges)
        null_dfs = [score_pair_delta_f(null_conc[a], null_conc[b]) for a, b in pairs]
        null_rates.append(len([d for d in null_dfs if d <= THRESHOLD]) / len(pairs))
        null_means.append(sum(null_dfs) / len(null_dfs))
    print(f"\n  expected-if-null ({NULL_REPLICATES} degree-preserving shuffles, seed {SEED}):")
    print(f"    accept rate  observed {obs_rate:.4f}   null [{min(null_rates):.4f}, {max(null_rates):.4f}]")
    print(f"    mean ΔF      observed {obs_mean:.2f}      null [{min(null_means):.2f}, {max(null_means):.2f}]")

    _check(results, "accept-count-is-not-evidence",
           all(abs(r - obs_rate) < 1e-12 for r in null_rates),
           f"the null reproduces {obs_acc}/{len(pairs)} in {NULL_REPLICATES}/{NULL_REPLICATES} "
           f"replicates, so the accept count separates nothing; red here would mean "
           f"the observed rate IS informative and the interpretation above needs rewriting")

    _check(results, "mean-delta-f-is-evidence",
           obs_mean < min(null_means),
           f"observed mean ΔF {obs_mean:.2f} < every null replicate "
           f"(null min {min(null_means):.2f}); the corpus is more mergeable than "
           f"its degree-matched null by {min(null_means) - obs_mean:.2f} nats")

    # --- expected-if-null: constellation-label permutation ------------------
    by_pair = dict(zip(pairs, delta_fs))
    labels = [constellation_of[p] for p in multi_patterns]

    def gap(label_list):
        lab = dict(zip(multi_patterns, label_list))
        within = [d for (x, y), d in by_pair.items() if lab[x] == lab[y]]
        across = [d for (x, y), d in by_pair.items() if lab[x] != lab[y]]
        if not within or not across:
            return 0.0
        return sum(within) / len(within) - sum(across) / len(across)

    obs_gap = gap(labels)
    rng2 = random.Random(SEED)
    null_gaps = []
    for _ in range(PERMUTATIONS):
        shuffled_labels = labels[:]
        rng2.shuffle(shuffled_labels)
        null_gaps.append(gap(shuffled_labels))
    p_value = sum(1 for g in null_gaps if g <= obs_gap) / len(null_gaps)
    pct5 = sorted(null_gaps)[int(0.05 * len(null_gaps))]
    print(f"\n  expected-if-null ({PERMUTATIONS} constellation-label shuffles, seed {SEED}):")
    print(f"    within-across gap  observed {obs_gap:.4f}   null mean "
          f"{sum(null_gaps)/len(null_gaps):.4f}   null 5th pct {pct5:.4f}")
    _check(results, "within-across-gap-beats-its-null",
           p_value < 0.05,
           f"p = {p_value:.4f} ({sum(1 for g in null_gaps if g <= obs_gap)}/"
           f"{PERMUTATIONS} shuffles at or below the observed gap); red would leave "
           f"experiment 3's 'PARTIALLY validates' unsupported")

    red = [name for name, ok, _ in results if not ok]
    print()
    print(f"  {len(results) - len(red)}/{len(results)} checks green, {len(red)} red"
          + (f": {', '.join(red)}" if red else ""))
    print()
    return red


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

    # A degenerate population is the class-1 shape this acceptance exists to
    # catch, and experiment 1 would die indexing it rather than report it. Say
    # so and exit red before the experiments run.
    if len(multi_patterns) < 2:
        print("=" * 72)
        print("ACCEPTANCE (D61a): stated population, null baselines, failable checks")
        print("=" * 72)
        print("  [RED ] population-nonempty-and-enumerated")
        print(f"         {len(multi_patterns)} patterns occupy >= 2 missions; "
              f"fewer than 2 leaves no pair to score")
        print("\n  0/1 checks green, 1 red: population-nonempty-and-enumerated\n")
        print("=" * 72)
        print("FAILED — 1 acceptance check(s) red: population-nonempty-and-enumerated")
        print("=" * 72)
        return 1

    # Exp 1: over-merge
    delta_fs = experiment_1_overmerge(conc, multi_patterns)

    # Exp 2: EIG per constellation. Descriptive: it states no claim that could
    # be false, so the acceptance section asserts nothing about it.
    experiment_2_eig_per_constellation(conc, multi_patterns, constellation_of)

    # Exp 3: within vs across ΔF
    experiment_3_within_vs_across(conc, multi_patterns, constellation_of)

    # D61a: the section that can go red.
    red = acceptance(edges, conc, all_missions, multi_patterns,
                     constellation_of, delta_fs)

    print("=" * 72)
    if red:
        print(f"FAILED — {len(red)} acceptance check(s) red: {', '.join(red)}")
    else:
        print("DONE — all acceptance checks green (exit 0)")
    print("=" * 72)
    return 1 if red else 0


if __name__ == "__main__":
    raise SystemExit(main())
