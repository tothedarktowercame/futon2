#!/usr/bin/env python
"""Adjudicate the AIF-12 merge: principled concept or embedding overreach?

Tests whether the 5 C8+C1 patterns added to the C2 core by embedding-gated BMR
(floor=0.7, cutoff=−20) genuinely belong to the AIF concept, using signals
INDEPENDENT of the embedding cosine that proposed the merge.

Three tests:
  1. Raw co-occurrence (mission-overlap Jaccard, embedding-INDEPENDENT)
  2. Live usage (co_app edges from real cascades)
  3. Qualitative (flexiarg titles for the 5 added members)

Exploratory findings only. No commit.

Usage:
    /home/joe/code/gflownet/.venv/bin/python aif12_merge_adjudication.py
"""
from __future__ import annotations

import itertools
import json
import re
import statistics
from pathlib import Path
from typing import Dict, List, Set, Tuple

# ---------------------------------------------------------------------------
# The 12 patterns and their constellation assignments
# ---------------------------------------------------------------------------
C2_CORE = [
    "aif-as-environment-not-instruction",
    "candidate-pattern-action-space",
    "evidence-precision-registry",
    "expected-free-energy-scorecard",
    "policy-precision-commitment-temperature",
    "structured-observation-vector",
    "term-to-channel-traceability",
]
C8_ADDED = [
    "learn-as-you-go",
    "loop-failure-signals",
    "structured-events-only",
]
C1_ADDED = [
    "structural-tension-as-observation",
    "par-as-obligation",
]
ALL_12 = C2_CORE + C8_ADDED + C1_ADDED
ADDED_5 = C8_ADDED + C1_ADDED


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------
def load_mission_sets():
    """Load each pattern's mission-set from mission-pattern-scopes.edn."""
    text = Path("/home/joe/code/futon6/data/mission-pattern-scopes.edn").read_text()
    pattern_missions: Dict[str, Set[str]] = {}
    for m in re.finditer(
        r':mission\s+"(M-[^"]+)".*?:applied\s+\[([^\]]*)\]', text, re.DOTALL
    ):
        mission_id = m.group(1)
        patterns = re.findall(r'"([^"]+)"', m.group(2))
        for p in patterns:
            pattern_missions.setdefault(p, set()).add(mission_id)
    return pattern_missions


def load_live_usage():
    """Load co_app live usage edges."""
    data = json.loads(
        Path("/home/joe/code/futon6/data/coapp-live-usage.json").read_text()
    )
    edges = set()
    for entry in data["co_app_usage"]:
        stems = tuple(sorted(entry["stems"]))
        edges.add(stems)
    return edges


# ---------------------------------------------------------------------------
# Test 1: Raw co-occurrence (Jaccard + shared-count)
# ---------------------------------------------------------------------------
def jaccard(set_a: Set[str], set_b: Set[str]) -> float:
    union = set_a | set_b
    if not union:
        return 0.0
    return len(set_a & set_b) / len(union)


def test_1_cooccurrence(pattern_missions):
    print("=" * 72)
    print("TEST 1: Raw co-occurrence (embedding-INDEPENDENT mission overlap)")
    print("=" * 72)

    # WITHIN-C2 pairs (both core AIF): C(7,2) = 21
    within_c2 = []
    for pi, pj in itertools.combinations(C2_CORE, 2):
        ms_i = pattern_missions.get(pi, set())
        ms_j = pattern_missions.get(pj, set())
        jac = jaccard(ms_i, ms_j)
        shared = len(ms_i & ms_j)
        within_c2.append((jac, shared, pi, pj))

    # CROSS-BOUNDARY pairs (C2 member × C8/C1 member): 7×5 = 35
    cross_boundary = []
    for pi in C2_CORE:
        for pj in ADDED_5:
            ms_i = pattern_missions.get(pi, set())
            ms_j = pattern_missions.get(pj, set())
            jac = jaccard(ms_i, ms_j)
            shared = len(ms_i & ms_j)
            cross_boundary.append((jac, shared, pi, pj))

    # Also WITHIN-ADDED (pairs among the 5 added): C(5,2) = 10
    within_added = []
    for pi, pj in itertools.combinations(ADDED_5, 2):
        ms_i = pattern_missions.get(pi, set())
        ms_j = pattern_missions.get(pj, set())
        jac = jaccard(ms_i, ms_j)
        shared = len(ms_i & ms_j)
        within_added.append((jac, shared, pi, pj))

    def stats(pairs, label):
        jacs = [p[0] for p in pairs]
        shared = [p[1] for p in pairs]
        print(f"\n  {label} ({len(pairs)} pairs):")
        print(f"    Jaccard:  mean={statistics.mean(jacs):.4f}  "
              f"median={statistics.median(jacs):.4f}  "
              f"range=[{min(jacs):.4f}, {max(jacs):.4f}]")
        print(f"    Shared:   mean={statistics.mean(shared):.2f}  "
              f"median={statistics.median(shared):.1f}  "
              f"range=[{min(shared)}, {max(shared)}]")

    stats(within_c2, "WITHIN-C2 (core AIF, baseline)")
    stats(cross_boundary, "CROSS-BOUNDARY (C2×C8/C1, the merge ADDS these)")
    stats(within_added, "WITHIN-ADDED (among the 5 added members)")

    # Verdict
    within_mean = statistics.mean(p[0] for p in within_c2)
    cross_mean = statistics.mean(p[0] for p in cross_boundary)
    ratio = cross_mean / within_mean if within_mean > 0 else 0
    print(f"\n  Cross-boundary / within-C2 Jaccard ratio: {ratio:.2f}")
    if ratio >= 0.5:
        print("  VERDICT: cross-boundary ≈ within-C2 → co-occurrence SUPPORTS the merge")
        print("  → AIF-12 merge is PRINCIPLED (learned structure)")
    elif ratio >= 0.25:
        print("  VERDICT: cross-boundary moderately below within-C2 → PARTIAL support")
        print("  → Some added members co-occur genuinely; others may be overreach")
    else:
        print("  VERDICT: cross-boundary << within-C2 → pulled by cosine, not usage")
        print("  → OVERREACH (recovery ceiling holds)")

    # Show the cross-boundary pairs sorted by Jaccard
    cross_boundary.sort(key=lambda x: -x[0])
    print(f"\n  Cross-boundary pairs (sorted by Jaccard desc):")
    for jac, shared, pi, pj in cross_boundary[:10]:
        print(f"    J={jac:.3f} shared={shared}  {pi} × {pj}")
    print(f"  ...")
    for jac, shared, pi, pj in cross_boundary[-5:]:
        print(f"    J={jac:.3f} shared={shared}  {pi} × {pj}")

    print()
    return ratio


# ---------------------------------------------------------------------------
# Test 2: Live usage (co_app edges)
# ---------------------------------------------------------------------------
def test_2_live_usage(live_edges):
    print("=" * 72)
    print("TEST 2: Live usage (co_app edges from real cascades)")
    print("=" * 72)
    print(f"  Total live co_app edges: {len(live_edges)}")

    # Count WITHIN-C2 pairs in live edges
    within_c2_live = 0
    within_c2_total = 0
    for pi, pj in itertools.combinations(C2_CORE, 2):
        within_c2_total += 1
        if tuple(sorted((pi, pj))) in live_edges:
            within_c2_live += 1

    # Count CROSS-BOUNDARY pairs in live edges
    cross_live = 0
    cross_total = 0
    for pi in C2_CORE:
        for pj in ADDED_5:
            cross_total += 1
            if tuple(sorted((pi, pj))) in live_edges:
                cross_live += 1

    # Count WITHIN-ADDED pairs
    within_added_live = 0
    within_added_total = 0
    for pi, pj in itertools.combinations(ADDED_5, 2):
        within_added_total += 1
        if tuple(sorted((pi, pj))) in live_edges:
            within_added_live += 1

    # Any of the 12 patterns appear in live edges at all?
    all_12_set = set(ALL_12)
    patterns_in_live = set()
    for edge in live_edges:
        for s in edge:
            if s in all_12_set:
                patterns_in_live.add(s)

    within_rate = within_c2_live / within_c2_total if within_c2_total else 0
    cross_rate = cross_live / cross_total if cross_total else 0

    print(f"\n  WITHIN-C2:     {within_c2_live}/{within_c2_total} pairs appear "
          f"({within_rate:.1%})")
    print(f"  CROSS-BOUNDARY: {cross_live}/{cross_total} pairs appear "
          f"({cross_rate:.1%})")
    print(f"  WITHIN-ADDED:  {within_added_live}/{within_added_total} pairs appear")
    print(f"  Patterns of the 12 appearing in ANY live edge: "
          f"{len(patterns_in_live)}/12")
    if patterns_in_live:
        print(f"    {sorted(patterns_in_live)}")

    if cross_rate > 0 and within_rate > 0:
        ratio = cross_rate / within_rate
        print(f"\n  Cross/within rate ratio: {ratio:.2f}")
        if ratio >= 0.5:
            print("  VERDICT: cross-boundary pairs co-apply in real cascades")
            print("  → SUPPORTS the merge")
        else:
            print("  VERDICT: cross-boundary pairs rarely co-apply")
            print("  → WEAKENS the merge claim")
    elif within_rate > 0 and cross_rate == 0:
        print(f"\n  VERDICT: zero cross-boundary co-application despite within-C2 usage")
        print("  → OVERREACH (added members don't co-apply with core in practice)")
    else:
        print(f"\n  VERDICT: insufficient live data for either group")

    print()
    return cross_rate, within_rate


# ---------------------------------------------------------------------------
# Test 3: Qualitative (flexiarg titles)
# ---------------------------------------------------------------------------
FLEXIARG_TITLES = {
    "aif-as-environment-not-instruction": "AIF as Environment, Not Instruction",
    "candidate-pattern-action-space": "Candidate Pattern Action Space",
    "evidence-precision-registry": "Evidence Precision Registry",
    "expected-free-energy-scorecard": "Expected Free Energy as Multi-Term Scorecard",
    "policy-precision-commitment-temperature": "Policy Precision as Commitment Temperature",
    "structured-observation-vector": "Structured Observation Vector",
    "term-to-channel-traceability": "Term-to-Channel Traceability",
    "learn-as-you-go": "Capture Realtime Learnings as Patterns",
    "loop-failure-signals": "Realtime Loop Failure Signals",
    "structured-events-only": "Prefer Structured PSR/PUR Events",
    "structural-tension-as-observation": "Structural Tension Is the Observation Vector for Library Evolution",
    "par-as-obligation": "PAR as Obligation (G0)",
}


def test_3_qualitative():
    print("=" * 72)
    print("TEST 3: Qualitative — flexiarg titles for the 5 ADDED members")
    print("=" * 72)

    print("\n  C2 CORE (7) — for reference:")
    for p in C2_CORE:
        print(f"    {p}: {FLEXIARG_TITLES.get(p, '?')}")

    print("\n  ADDED (5) — do these read as AIF/active-inference?")
    aif_reads = 0
    for p in ADDED_5:
        title = FLEXIARG_TITLES.get(p, "?")
        # Heuristic: contains AIF/EFE/observation/policy/precision/realtime/loop/tension
        aif_keywords = ["aif", "observation", "policy", "precision", "realtime",
                        "loop", "tension", "pattern", "structured", "event"]
        reads_aif = any(k in title.lower() for k in aif_keywords)
        if reads_aif:
            aif_reads += 1
        marker = "← AIF-adjacent" if reads_aif else "← NOT obviously AIF"
        print(f"    {p}: {title}  {marker}")

    print(f"\n  {aif_reads}/5 added members read as AIF-adjacent by title")
    print()
    return aif_reads


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    pattern_missions = load_mission_sets()
    live_edges = load_live_usage()

    # Verify all 12 patterns have mission data
    missing = [p for p in ALL_12 if p not in pattern_missions]
    if missing:
        print(f"WARNING: patterns missing from corpus: {missing}")

    print(f"12 patterns: 7 C2-core + 3 C8-added + 2 C1-added")
    print(f"Mission sets loaded for {len([p for p in ALL_12 if p in pattern_missions])}/12")
    print()

    ratio_1 = test_1_cooccurrence(pattern_missions)
    cross_rate, within_rate = test_2_live_usage(live_edges)
    aif_reads = test_3_qualitative()

    # Overall verdict
    print("=" * 72)
    print("OVERALL VERDICT")
    print("=" * 72)
    signals_principled = 0
    signals_overreach = 0

    if ratio_1 >= 0.5:
        signals_principled += 1
    else:
        signals_overreach += 1

    if cross_rate > 0 and within_rate > 0 and cross_rate / within_rate >= 0.5:
        signals_principled += 1
    else:
        signals_overreach += 1

    if aif_reads >= 3:
        signals_principled += 1
    else:
        signals_overreach += 1

    print(f"  Test 1 (co-occurrence ratio {ratio_1:.2f}): "
          f"{'SUPPORTS' if ratio_1 >= 0.5 else 'WEAKENS'}")
    print(f"  Test 2 (live usage {cross_rate:.1%} vs {within_rate:.1%}): "
          f"{'SUPPORTS' if cross_rate > 0 and cross_rate / max(within_rate, 0.001) >= 0.5 else 'WEAKENS'}")
    print(f"  Test 3 ({aif_reads}/5 AIF-adjacent): "
          f"{'SUPPORTS' if aif_reads >= 3 else 'WEAKENS'}")

    if signals_principled >= 2:
        print(f"\n  → AIF-12 merge is PRINCIPLED ({signals_principled}/3 signals support)")
        print("  → Learned structure; the clustering split a genuine concept.")
    else:
        print(f"\n  → AIF-12 merge is OVERREACH ({signals_overreach}/3 signals weaken)")
        print("  → Recovery ceiling confirmed; constellation granularity vindicated.")

    print("\n[Exploratory findings, not committed]")


if __name__ == "__main__":
    main()
