"""Contest v1.1 generation proxy: metric-C over the resolution-scaled,
presence-masked matrix (metric-matrix-v1.1.json).

Same C = T x H shape as metric_c_proxy (v0); the only change is the matrix:
action_intensity arrives already scaled by (1 - resolvedness) and zeroed for
presence-masked targets (see export_metric_matrix_v11.py). Kept as a separate
module so the v0 arm's provenance stays byte-stable.
"""
import json
from pathlib import Path

_M = json.load(open(Path(__file__).parent / "metric-matrix-v1.1.json"))
INTENSITY = _M["intensity"]
DIST = _M["distances"]


def _sim(t1: str, t2: str) -> float:
    d = (DIST.get(t1) or {}).get(t2)
    if d is None:
        return 0.0
    return 1.0 / (1.0 + d)


def metric_c(targets: list[str]) -> float:
    t_sum = sum(INTENSITY.get(t, {}).get("action_intensity", 0.0) for t in targets)
    pairs = [(targets[i], targets[j])
             for i in range(len(targets)) for j in range(i + 1, len(targets))]
    if pairs:
        h = sum(4.0 * _sim(a, b) * (1.0 - _sim(a, b)) for a, b in pairs) / len(pairs)
    else:
        h = 1.0
    return t_sum * h


def selection_targets(moves: list[dict], sel) -> list[str]:
    return [moves[i].get("source_action", {}).get("target") for i in sel]
