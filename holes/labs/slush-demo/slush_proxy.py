"""Real pattern-constellation reward proxy for the GFlowNet slush.

The old slush demo used an 8-concept synthetic catalogue. This version uses the
existing embedding constellations P0-P17 from
``pipeline-semilattice-clusters.edn`` as the concept space. Count-BMR is used
only as a negative result in the sibling experiment files; it is not used to
form concepts here.

Reward semantics:

    EFE = risk - lambda * EIG
    R   = exp(-EFE)

where risk is the negative target-mission coverage score and EIG is the summed
constellation posterior stddev. EIG is epistemic value and is subtracted, never
added as an ambiguity cost.
"""
from __future__ import annotations

import itertools
import math
import re
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Set, Tuple

import torch

from gflownet.proxy.base import Proxy

PRIOR = 0.1
N_CONCEPTS = 18
MAX_SELECTION = 5
TARGET_MISSION = "M-self-documenting-stack"
PASS_MIN_COVERAGE = 5
COVERAGE_WEIGHT = 2.0
LAMBDA = 1.0

SEMILATTICE_PATH = Path(
    "/home/joe/code/futon3c/holes/excursions/pipeline-semilattice-clusters.edn"
)
MISSION_PATTERN_SCOPES_PATH = Path("/home/joe/code/futon6/data/mission-pattern-scopes.edn")

CONSTELLATION_LABELS = {
    0: "writing / pattern discipline / categorical objects",
    1: "futon-theory / storage / coordination / invariants",
    2: "AIF / operator / surface inhabitation",
    3: "small cluster",
    4: "small cluster",
    5: "small cluster",
    6: "runtime invariants / boot reachability / snapshots",
    7: "small cluster",
    8: "agency / coordination law / routing",
    9: "small cluster",
    10: "small cluster",
    11: "small cluster",
    12: "small cluster",
    13: "math proof strategy / rational reconstruction",
    14: "small cluster",
    15: "small cluster",
    16: "small cluster",
    17: "small cluster",
}

_ACTIVE_LAMBDA = LAMBDA
_ACTIVE_TARGET = TARGET_MISSION


def pattern_stem(pattern_id: str) -> str:
    return str(pattern_id).split("/")[-1]


def normalize_mission_id(mission_id: str) -> str:
    return str(mission_id).split("@", 1)[0]


def parse_pattern_constellations(path: Path = SEMILATTICE_PATH) -> Dict[str, int]:
    text = path.read_text()
    membership = {}
    for m in re.finditer(r':pattern\s+"([^"]+)"\s+:cluster\s+(\d+)', text):
        membership[pattern_stem(m.group(1))] = int(m.group(2))
    return dict(sorted(membership.items()))


def parse_mission_patterns(path: Path = MISSION_PATTERN_SCOPES_PATH) -> Dict[str, List[str]]:
    text = path.read_text()
    missions = {}
    for m in re.finditer(
        r':mission\s+"(M-[^"]+)".*?:applied\s+\[([^\]]*)\]', text, re.DOTALL
    ):
        mission = normalize_mission_id(m.group(1))
        patterns = [pattern_stem(p) for p in re.findall(r'"([^"]+)"', m.group(2))]
        missions[mission] = patterns
    return dict(sorted(missions.items()))


def corpus_edges(mission_to_patterns: Dict[str, List[str]]) -> List[Tuple[str, str]]:
    return [
        (pattern_stem(pattern), normalize_mission_id(mission))
        for mission, patterns in mission_to_patterns.items()
        for pattern in patterns
    ]


def dirichlet_moments(alpha: Sequence[float]) -> List[Dict[str, float]]:
    """Per-component Dirichlet moments. Ships stddev, never variance."""
    v = [float(x) for x in alpha]
    alpha0 = sum(v)
    denom = alpha0 * alpha0 * (alpha0 + 1.0)
    return [
        {"mean": ai / alpha0, "stddev": math.sqrt(ai * (alpha0 - ai) / denom)}
        for ai in v
    ]


def concept_stddev(alpha: Sequence[float]) -> float:
    """RMS of per-outcome stddevs, matching futon2.aif.a4a."""
    stds = [m["stddev"] for m in dirichlet_moments(alpha)]
    if not stds:
        return 0.0
    return math.sqrt(sum(s * s for s in stds) / len(stds))


def aggregate_rows(rows: Sequence[Sequence[float]]) -> List[float]:
    if not rows:
        return []
    return [
        PRIOR + sum(value - PRIOR for value in values)
        for values in zip(*rows)
    ]


def constellation_eig(
    pattern_to_constellation: Dict[str, int],
    edges: Sequence[Tuple[str, str]],
) -> Dict[int, float]:
    """Return {constellation -> aggregate posterior stddev}.

    This is the Python mirror of futon2.aif.a4a/constellation->eig.
    """
    norm_p2c = {pattern_stem(p): c for p, c in pattern_to_constellation.items()}
    norm_edges = [(pattern_stem(p), normalize_mission_id(m)) for p, m in edges]
    patterns = sorted(norm_p2c)
    missions = sorted({mission for _, mission in norm_edges})
    mission_idx = {mission: i for i, mission in enumerate(missions)}
    empty_row = [PRIOR] * len(missions)
    pattern_rows = {pattern: list(empty_row) for pattern in patterns}
    for pattern, mission in norm_edges:
        if pattern in pattern_rows and mission in mission_idx:
            pattern_rows[pattern][mission_idx[mission]] += 1.0

    rows_by_constellation: Dict[int, List[List[float]]] = defaultdict(list)
    for pattern, constellation in norm_p2c.items():
        rows_by_constellation[constellation].append(pattern_rows.get(pattern, empty_row))

    return {
        constellation: concept_stddev(aggregate_rows(rows))
        for constellation, rows in sorted(rows_by_constellation.items())
    }


PATTERN_TO_CONSTELLATION = parse_pattern_constellations()
MISSION_TO_PATTERNS = parse_mission_patterns()
CORPUS_EDGES = corpus_edges(MISSION_TO_PATTERNS)
CONSTELLATION_EIG = constellation_eig(PATTERN_TO_CONSTELLATION, CORPUS_EDGES)


def target_constellations(target_mission: str = TARGET_MISSION) -> Set[int]:
    return {
        PATTERN_TO_CONSTELLATION[p]
        for p in MISSION_TO_PATTERNS.get(normalize_mission_id(target_mission), [])
        if p in PATTERN_TO_CONSTELLATION
    }


def set_active_target(target_mission: str) -> None:
    global _ACTIVE_TARGET
    _ACTIVE_TARGET = normalize_mission_id(target_mission)


def set_active_lambda(lam: float) -> None:
    global _ACTIVE_LAMBDA
    _ACTIVE_LAMBDA = float(lam)


def all_candidates(max_selection: int = MAX_SELECTION) -> List[Tuple[int, ...]]:
    return list(itertools.combinations(range(N_CONCEPTS), max_selection))


def coverage_count(selection: Tuple[int, ...], target_mission: str | None = None) -> int:
    target = target_constellations(target_mission or _ACTIVE_TARGET)
    return len(set(selection) & target)


def eig(selection: Tuple[int, ...]) -> float:
    return sum(CONSTELLATION_EIG.get(i, 0.0) for i in selection)


def a3_passes(selection: Tuple[int, ...], target_mission: str | None = None) -> bool:
    return coverage_count(selection, target_mission) >= PASS_MIN_COVERAGE


def risk(selection: Tuple[int, ...], target_mission: str | None = None) -> float:
    return -COVERAGE_WEIGHT * coverage_count(selection, target_mission)


def efe(selection: Tuple[int, ...], lam: float = LAMBDA, target_mission: str | None = None) -> float:
    return risk(selection, target_mission) - lam * eig(selection)


def reward(selection: Tuple[int, ...], lam: float = LAMBDA, target_mission: str | None = None) -> float:
    return math.exp(-efe(selection, lam, target_mission))


def singleton_score(constellation: int, target_mission: str | None = None) -> float:
    singleton = (constellation,)
    return -risk(singleton, target_mission) + _ACTIVE_LAMBDA * eig(singleton)


def greedy_candidate(target_mission: str | None = None) -> Tuple[int, ...]:
    ranked = sorted(
        range(N_CONCEPTS),
        key=lambda c: (-singleton_score(c, target_mission), c),
    )
    return tuple(sorted(ranked[:MAX_SELECTION]))


def greedy_tie_count(target_mission: str | None = None) -> int:
    scores = [singleton_score(c, target_mission) for c in range(N_CONCEPTS)]
    cutoff = sorted(scores, reverse=True)[MAX_SELECTION - 1]
    return sum(1 for s in scores if abs(s - cutoff) < 1e-12)


def selection_from_proxy_state(state: dict) -> Tuple[int, ...]:
    out: List[int] = []
    dones = state.get("_dones", [])
    for k, v in state.items():
        if isinstance(k, int) and k < len(dones) and dones[k]:
            idx = int(v[0]) - 1
            if idx >= 0:
                out.append(idx)
    return tuple(sorted(out))


class SlushProxy(Proxy):
    """GFlowNet proxy returning R = exp(-EFE) for real constellation choices."""

    def __init__(self, **kwargs):
        kwargs.setdefault("reward_min", 1e-8)
        kwargs.setdefault("do_clip_rewards", True)
        super().__init__(**kwargs)

    def setup(self, env=None):
        pass

    def __call__(self, states: Iterable[dict]) -> torch.Tensor:
        vals = []
        for st in states:
            sel = selection_from_proxy_state(st)
            vals.append(reward(sel, lam=_ACTIVE_LAMBDA, target_mission=_ACTIVE_TARGET))
        return torch.tensor(vals, dtype=self.float, device=self.device)
