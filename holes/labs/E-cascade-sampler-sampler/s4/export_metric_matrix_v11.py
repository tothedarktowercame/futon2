"""Contest v1.1 matrix: resolution-state scaling + presence mask over the v0 freeze.

Fixes the two golden-round-1 findings (s4/golden-selections-v0.edn):
- saturation false-positive: v0's export reimplemented action-intensity WITHOUT
  the delivered engine's (1 - resolvedness) scaling (full-pass semantics,
  substrate_metric_e1_curvature.py main); saturated targets kept full intensity.
  v1.1 calls eng.resolution_state per target and scales. Unknown states are
  explicitly non-actionable (the campaign's own rule) -> intensity 0, disclosed.
- operator-coupled targets: presence mask from the agency roster mission-id
  (the slot round 1 found unfilled; structural fix = M-autoclock-in). Presence
  zeroes generation intensity but is exported separately so move vocabulary
  can later differentiate (close-out / advance / avoid).

Deterministic given (metric-matrix-v0.json + mission-doc fetch + roster fetch):
raw intensities and distances are TAKEN FROM the v0 freeze, not recomputed, so
v1.1-vs-v0 deltas are attributable to the two new signals, not substrate drift.
"""
import json
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, "/home/joe/code/futon3c/scripts")
import substrate_metric_e1_curvature as eng

HERE = Path(__file__).parent
_M = json.load(open(HERE / "metric-matrix-v0.json"))


def fetch_json(url: str):
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.load(resp)


# 1. vertex docs (engine main's fetch, unmodified semantics)
mission_docs = {(d.get("hx/endpoints") or [None])[0]: d
                for d in eng.fetch_hyperedges_by_type("code/v05/mission-doc", 1000)}
sorry_docs = {(d.get("hx/endpoints") or [None])[0]: d
              for d in eng.fetch_hyperedges_by_type("code/v05/sorry", 1000)}
docs = {**mission_docs, **sorry_docs}
print(f"{len(mission_docs)} mission docs, {len(sorry_docs)} sorry docs", flush=True)

# 2. presence: agency roster mission-id per agent (live, e.g. claude-3 today)
roster = fetch_json("http://localhost:7070/api/alpha/agents")
agents = roster.get("agents", roster)
present: dict[str, list[str]] = {}
for aid, a in (agents.items() if isinstance(agents, dict) else []):
    mid = isinstance(a, dict) and a.get("mission-id")
    if mid and isinstance(mid, str) and mid != "nil":
        present.setdefault(mid, []).append(aid)
print(f"presence: {present}", flush=True)


# 3. target -> doc endpoint, by the same /mission/<stem> suffix rule as v0's node_for
def doc_for(stem: str):
    suffix = "/mission/" + stem.removeprefix("M-")
    hits = [e for e in docs if e and e.endswith(suffix)]
    return sorted(hits)[0] if hits else None


intensity = {}
n_unknown = n_saturated = n_present = 0
for t, v0 in _M["intensity"].items():
    endpoint = doc_for(t)
    state = (eng.resolution_state(endpoint, docs) if endpoint
             else {"resolvedness": "unknown", "actionable": False,
                   "raw": {"reason": "no-mission-doc"}})
    res = state["resolvedness"]
    raw_intensity = v0["action_intensity"]
    if isinstance(res, (int, float)):
        scaled = raw_intensity * (1.0 - float(res))
        if scaled == 0.0:
            n_saturated += 1
    else:
        scaled = 0.0
        n_unknown += 1
    who = present.get(t, [])
    if who:
        n_present += 1
    intensity[t] = {**v0,
                    "raw_action_intensity": raw_intensity,
                    "action_intensity": 0.0 if who else scaled,
                    "resolvedness": res,
                    "actionable": bool(state["actionable"]),
                    "raw_resolution": state["raw"],
                    "present": who}

out = {"frozen-at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
       "derived-from": {"matrix": "metric-matrix-v0.json",
                        "v0-frozen-at": _M["frozen-at"]},
       "engine": _M["engine"] + " + resolution_state (same module) + roster presence",
       "graph": _M["graph"],
       "targets": _M["targets"], "matched": _M["matched"],
       "disclosures": {"unknown-resolution-zeroed": n_unknown,
                       "resolution-scaled-to-zero": n_saturated,
                       "presence-masked": n_present,
                       "of-matched": len(intensity)},
       "presence": present,
       "intensity": intensity,
       "distances": _M["distances"]}
(HERE / "metric-matrix-v1.1.json").write_text(json.dumps(out))
print(f"wrote metric-matrix-v1.1.json — zeroed: {n_unknown} unknown, "
      f"{n_saturated} saturated, {n_present} presence-masked "
      f"of {len(intensity)} matched", flush=True)
