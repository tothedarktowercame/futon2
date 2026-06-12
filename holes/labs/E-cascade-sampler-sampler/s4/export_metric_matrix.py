"""Contest v1: freeze the ground-metric signal for the 121 WM targets.

THE PAIRED CONSUMER: M-substrate-metric v1 (DELIVERED, campaign open
"until a paired consumer consumes live") is consumed here by
E-cascade-sampler-sampler v1 as the GENERATION proxy. Imports the
delivered engine (futon3c/scripts/substrate_metric_e1_curvature.py)
unmodified; emits per-target min-incident-kappa / action-intensity and
pairwise hop distances as a frozen artifact (metric-matrix-v0.json) so
the contest stays deterministic.
"""
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, "/home/joe/code/futon3c/scripts")
import substrate_metric_e1_curvature as eng

HERE = Path(__file__).parent

# 1. the frozen targets
circs = json.load(open(HERE / "circumstances-v0.json"))
targets = sorted({m.get("source_action", {}).get("target")
                  for c in circs for m in c["moves"]} - {None})
print(f"{len(targets)} distinct frozen targets", flush=True)

# 2. the live graph, via the delivered engine (same families as the full pass)
t0 = time.time()
edges = []
for fam in eng.FEEDS_MU_TYPES:
    edges.extend(eng.fetch_hyperedges_by_type(fam, 2000))
multi, simple, edge_list = eng.build_graph(edges)
print(f"graph: {len(simple)} nodes, {len(edge_list)} edges "
      f"({time.time()-t0:.1f}s fetch+build)", flush=True)

# 3. map mission stems -> metric node ids (suffix match on /mission/<stem>)
def node_for(stem: str):
    suffix = "/mission/" + stem.removeprefix("M-")
    hits = [n for n in simple if n.endswith(suffix)]
    return sorted(hits)[0] if hits else None

node_of = {t: node_for(t) for t in targets}
matched = {t: n for t, n in node_of.items() if n}
print(f"matched {len(matched)}/{len(targets)} targets to metric nodes", flush=True)

# 4. per-target curvature: min incident kappa over the node's edges
intensity = {}
for t, node in matched.items():
    kappas = []
    for e in edge_list:
        a, b = e["a"], e["b"]
        if node in (a, b):
            try:
                res = eng.curvature_for_edge(
                    multi, simple,
                    {"edge": [a, b], "relation": e["relation"]})
                kappas.append(res.kappa)
            except RuntimeError:
                continue
    if kappas:
        mk = min(kappas)
        intensity[t] = {"min_incident_kappa": mk,
                        "action_intensity": max(0.0, -mk) if mk < 0 else mk * 0.1,
                        "incident_edges": len(kappas)}
print(f"curvature for {len(intensity)} targets", flush=True)

# 5. pairwise hop distances among matched nodes
nodes = {t: n for t, n in matched.items()}
dist = {}
node_set = set(nodes.values())
for t, n in nodes.items():
    d = eng.bfs_distances(simple, n, node_set)
    dist[t] = {t2: d.get(n2) for t2, n2 in nodes.items() if t2 != t}

out = {"frozen-at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
       "engine": "substrate_metric_e1_curvature (M-substrate-metric v1, unmodified import)",
       "graph": {"nodes": len(simple), "edges": len(edge_list)},
       "targets": len(targets), "matched": len(matched),
       "intensity": intensity,
       "distances": dist}
(HERE / "metric-matrix-v0.json").write_text(json.dumps(out))
print("wrote metric-matrix-v0.json", flush=True)
