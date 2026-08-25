#!/usr/bin/env python3
"""feature_constellation.py — E-feature-constellation v0: a computed feature
network seeded from real data. Zero hand-typed rows.

Seed  = the live cascade's mission clusters (GET :7070/api/alpha/cascade-real/graph
        — the "Mission clusters" cards in pipeline-pattern-cascade-live.html).
Grow  = member missions + the attestation-weighted pattern roads between them
        (futon6/data/mission-carpet-roads.json, mission_carpet.py method:
        weight = max turn-attestation over shared distinctive pattern scopes).
Magnitude (per mission) = confirmed pattern warrants (:applied crosslinks)
        + log1p(summed incident road attestation)   [enacted, not declared]
Retract = the Interest Network's k-collapse, reimplemented faithfully from
        webarxana/src/webarxana/client/graph.cljs restrict-to-super-core:
        keep nodes with magnitude STRICTLY above the median, keep edges only
        between survivors, keep the largest connected component. Retraction is
        pure filtering — nothing is folded or re-weighted.
Layout = Futon City coordinates (futon6/data/mission-carpet-pos-embed.json,
        BGE-embedding-derived), reused as-is.

Outputs (same dir), all from ONE run because the seed is a live query:
feature-constellation.json (nodes/edges/provenance), .png (print-light,
A4-figure-sized), .svg (the web build embeds this) and .pdf (p4ng's print
build includes this -- pdflatex cannot read SVG).

Run it with the matplotlib that exists on this machine -- there is none in
the system python:
    ~/code/chatgpt-tui/.venv/bin/python feature_constellation.py margin
"""

import json
import math
import os
import sys
import time
import urllib.request
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
CODE = os.path.dirname(os.path.dirname(HERE))          # ~/code
ROADS = os.path.join(CODE, "futon6/data/mission-carpet-roads.json")
COORDS = os.path.join(CODE, "futon6/data/mission-carpet-pos-embed.json")
GRAPH_URL = "http://localhost:7070/api/alpha/cascade-real/graph"
OUT_JSON = os.path.join(HERE, "feature-constellation.json")
# "margin" renders the same computed graph at sidebar proportions: the labels
# that a full-width figure can afford (per-mission satellites, per-cluster
# counts) collide below ~400px, so the variant drops them rather than shrinking
# them into illegibility. Same data, same retraction — fewer annotations.
MARGIN = len(sys.argv) > 1 and sys.argv[1] == "margin"
_suffix = "-margin" if MARGIN else ""
OUT_PNG = os.path.join(HERE, f"feature-constellation{_suffix}.png")
OUT_SVG = os.path.join(HERE, f"feature-constellation{_suffix}.svg")
OUT_PDF = os.path.join(HERE, f"feature-constellation{_suffix}.pdf")

# ---------------------------------------------------------------- ingest ----

#   120s, not 20s: the cascade graph is ~85 KB assembled per request and
#   measured 13s warm on 2026-08-25; a cold one exceeded 20s and the script
#   died with TimeoutError mid-figure.
with urllib.request.urlopen(GRAPH_URL, timeout=120) as r:
    graph = json.load(r)

# Fail closed on :section-status before drawing anything. The endpoint has a 5s
# per-page deadline and marks a section :failed when it blows -- HTTP 200,
# well-formed, honest in the payload. During the 2026-08-25 mission-scope
# backfill it did so on 5 of 6 requests, and this script, which reads only
# graph["patterns"]["edges"], saw an empty edge list: every mission's warrant
# count came out 0, magnitude collapsed to road attestation alone, the median
# cut moved 7.595 -> 3.584, and one run died in the retraction with
# "max() iterable argument is empty". A figure drawn from that is not this
# figure with fresher numbers, it is a different measurement wearing the same
# caption. See futon1b/README-backlog-catchup.md §4.
_bad = sorted(k for k, v in (graph.get("section-status") or {}).items()
              if v.get("status") != "ok")
if _bad:
    raise SystemExit(
        "refusing to draw: cascade graph sections not ok: " + ", ".join(_bad) +
        "\n  the substrate is busy or degraded; rerun when they read ok")
roads = json.load(open(ROADS))
coords = json.load(open(COORDS))

# cascade mission id "<repo>-d/mission/<name>"  <->  carpet stem "M-<name>"
def stem(mission_id):
    return "M-" + mission_id.rsplit("/", 1)[1]

cluster_of = {}          # mission-id -> cluster short name
for row in graph["clusters"]:
    cluster_of[row["mission"]] = row["cluster"].rsplit("/", 1)[1]

applied = defaultdict(int)   # mission-id -> confirmed (:applied) warrant count
for e in graph["patterns"]["edges"]:
    if e["relation"] == "applied" and e["mission"] in cluster_of:
        applied[e["mission"]] += 1

# roads between clustered missions (join via stem)
by_stem = {stem(m): m for m in cluster_of}
edges = []               # (mission-a, mission-b, attestation)
road_strength = defaultdict(int)
for a, b, w in roads:
    ma, mb = by_stem.get(a), by_stem.get(b)
    if ma and mb and ma != mb:
        edges.append((ma, mb, w))
        road_strength[ma] += w
        road_strength[mb] += w

magnitude = {m: applied[m] + math.log1p(road_strength[m]) for m in cluster_of}

# ------------------------------------------------- retraction to core ----
# Faithful to graph.cljs restrict-to-super-core + keep-largest-component.

def median(xs):
    xs = sorted(xs)
    n = len(xs)
    if n == 0:
        return 0
    mid = n // 2
    return xs[mid] if n % 2 else (xs[mid - 1] + xs[mid]) / 2

cut = median(list(magnitude.values()))
core = {m for m, g in magnitude.items() if g > cut}          # STRICTLY above
core_edges = [(a, b, w) for a, b, w in edges if a in core and b in core]

adj = defaultdict(set)
for a, b, _ in core_edges:
    adj[a].add(b)
    adj[b].add(a)

def components(nodes):
    seen, comps = set(), []
    for n in nodes:
        if n in seen:
            continue
        comp, stack = set(), [n]
        while stack:
            x = stack.pop()
            if x in comp:
                continue
            comp.add(x)
            stack.extend(adj[x] - comp)
        seen |= comp
        comps.append(comp)
    return comps

comps = components(core)
keep = max(comps, key=len) if comps else set()
kept_edges = [(a, b, w) for a, b, w in core_edges if a in keep and b in keep]

dropped_clusters = sorted(set(cluster_of.values())
                          - {cluster_of[m] for m in keep})

# ---------------------------------------------------------------- emit ----

nodes = []
for m in sorted(keep):
    s = stem(m)
    xy = coords.get(s)
    nodes.append({"id": m, "stem": s, "cluster": cluster_of[m],
                  "applied": applied[m], "road-strength": road_strength[m],
                  "magnitude": round(magnitude[m], 3),
                  "xy": xy})

provenance = {
    "generated-at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
    "seed": "cascade-real/graph mission clusters (live)",
    "missions-clustered": len(cluster_of),
    "road-edges-joined": len(edges),
    "magnitude": "applied-warrants + log1p(sum incident road attestation)",
    "median-cut": round(cut, 3),
    "core-after-retraction": len(keep),
    "edges-after-retraction": len(kept_edges),
    "clusters-fully-retracted": dropped_clusters,
    "algorithm": "graph.cljs restrict-to-super-core (strict>median) + keep-largest-component",
    "layout": "futon6 mission-carpet-pos-embed.json (BGE-derived Futon City coords)",
    "outward-names": "cluster ids relabeled for presentation; see OUTWARD map in this script",
}
json.dump({"provenance": provenance, "nodes": nodes,
           "edges": [{"a": a, "b": b, "attestation": w} for a, b, w in kept_edges]},
          open(OUT_JSON, "w"), indent=1)
print(json.dumps(provenance, indent=1))

# --------------------------------------------------------------- render ----
# Two-level feature network: cluster hubs = feature nodes, positioned at the
# semantic centroid of their members' Futon City coords; retained core
# missions ring their hub; inter-feature edges = summed road attestation
# between members of different clusters (aggregation stated on the figure).

import matplotlib
matplotlib.use("Agg")
# Emit real <text> elements rather than glyph outlines. matplotlib's default
# (svg.fonttype="path") converts every label to vector paths: crisp, but not
# text -- it cannot be selected, searched, or read by a screen reader, and the
# consuming document ends up with a picture of words (Joe, 2026-08-25).
matplotlib.rcParams["svg.fonttype"] = "none"
import matplotlib.pyplot as plt

SURFACE = "#fcfcfb"
RAMP = ["#cde2fb", "#b7d3f6", "#9ec5f4", "#86b6ef", "#6da7ec", "#5598e7",
        "#3987e5", "#2a78d6", "#256abf", "#1c5cab", "#184f95", "#104281"]
INK, INK2, INK3 = "#1a1a19", "#4a4a47", "#8a8a85"

node_by_id = {n["id"]: n for n in nodes}
by_cluster = defaultdict(list)
for n in nodes:
    by_cluster[n["cluster"]].append(n)

# Outward-facing feature names (presentation layer only — the computed
# structure is untouched; the mapping is recorded in the output JSON).
# Grounded in each cluster's top cited patterns / member missions.
OUTWARD = {
    "00-war-machine":         "Active Inference",
    "01-invariant-queue":     "Live Invariants",
    "02-coupling-rational":   "Mathematical Reasoning",
    "03-vsatarcs-invariants": "Self-Documenting Systems",
    "04-learning-system":     "Learning Systems",
    "05-pattern-stack":       "Pattern Languages",
    "06-stack-futonzero":     "Self-Improving Loops",
    "07-essays-arxana":       "Knowledge Commons",
    "08-peripheral-plan":     "Planning & Verification",
    "09-peripheral-refactor": "Systems Integration",
    "10-substrate-metric":    "Ground Metrics",
    "11-codex-agency":        "Multi-Agent Coordination",
}

def outward(cl):
    name = OUTWARD.get(cl, cl.split("-", 1)[1])
    if len(name) > 18 and " " in name:
        parts = name.split(" ")
        mid = len(parts) // 2
        name = " ".join(parts[:mid]) + "\n" + " ".join(parts[mid:])
    return name

# hub positions: semantic centroids of member coords, then a repulsion pass
hub = {}
for cl, ns in by_cluster.items():
    pts = [n["xy"] for n in ns if n["xy"]]
    hub[cl] = [sum(p[0] for p in pts) / len(pts), sum(p[1] for p in pts) / len(pts)]
span = max(max(p[0] for p in hub.values()) - min(p[0] for p in hub.values()),
           max(p[1] for p in hub.values()) - min(p[1] for p in hub.values()))
min_sep = span * 0.34
for _ in range(200):
    moved = False
    cls = sorted(hub)
    for i, a in enumerate(cls):
        for b in cls[i + 1:]:
            dx = hub[b][0] - hub[a][0]
            dy = hub[b][1] - hub[a][1]
            d = math.hypot(dx, dy) or 1.0
            if d < min_sep:
                push = (min_sep - d) / 2
                ux, uy = dx / d, dy / d
                hub[a][0] -= ux * push; hub[a][1] -= uy * push
                hub[b][0] += ux * push; hub[b][1] += uy * push
                moved = True
    if not moved:
        break

# feature-feature edges: summed attestation between members of two clusters
ff = defaultdict(int)
for a, b, w in kept_edges:
    ca, cb = cluster_of[a], cluster_of[b]
    if ca != cb:
        ff[tuple(sorted((ca, cb)))] += w

# mission positions: ring around the hub, ordered by magnitude
pos = {}
for cl, ns in by_cluster.items():
    cx, cy = hub[cl]
    R = min_sep * (0.30 + 0.012 * len(ns))
    for i, n in enumerate(sorted(ns, key=lambda n: -n["magnitude"])):
        ang = 2 * math.pi * i / len(ns) - math.pi / 2
        pos[n["id"]] = (cx + R * math.cos(ang), cy + R * math.sin(ang))

gmin = min(n["magnitude"] for n in nodes)
gmax = max(n["magnitude"] for n in nodes)

def ramp_color(g):
    t = 0 if gmax == gmin else (g - gmin) / (gmax - gmin)
    return RAMP[min(len(RAMP) - 1, int(t * len(RAMP)))]

fig, ax = plt.subplots(figsize=(5.2, 3.6) if MARGIN else (11, 5.5), dpi=200)
# Transparent ground: the consuming document supplies the page colour, and a
# baked-in near-white panel reads as a pasted-in tile against it (Joe,
# 2026-08-25). SURFACE survives where it is doing work rather than filling:
# the label boxes, and the thin rings that keep overlapping nodes apart.
fig.patch.set_alpha(0.0)
ax.set_facecolor("none")
ax.axis("off")

# inter-feature edges (aggregated) between hubs
fmax = max(ff.values()) if ff else 1
for (ca, cb), w in sorted(ff.items(), key=lambda kv: kv[1]):
    (x1, y1), (x2, y2) = hub[ca], hub[cb]
    ax.plot([x1, x2], [y1, y2], color="#b9b9b3", zorder=1,
            linewidth=0.6 + 5.0 * (w / fmax), alpha=0.35 + 0.45 * (w / fmax),
            solid_capstyle="round")

# spokes hub -> member (very recessive)
for n in nodes:
    x, y = pos[n["id"]]
    cx, cy = hub[n["cluster"]]
    ax.plot([cx, x], [cy, y], color="#e2e2dd", zorder=2, linewidth=0.5)

# member missions
for n in nodes:
    x, y = pos[n["id"]]
    r = 18 + 80 * ((n["magnitude"] - gmin) / (gmax - gmin or 1))
    ax.scatter([x], [y], s=r, color=ramp_color(n["magnitude"]), zorder=4,
               edgecolors=SURFACE, linewidths=0.8)

# selective labels: top-2 missions per feature, pushed outward from the hub
for cl, ns in by_cluster.items():
    for n in sorted(ns, key=lambda n: -n["magnitude"])[:(0 if MARGIN else 2)]:
        x, y = pos[n["id"]]
        cx, cy = hub[cl]
        dx, dy = x - cx, y - cy
        d = math.hypot(dx, dy) or 1.0
        ax.annotate(n["stem"][2:], (x, y),
                    xytext=(10 * dx / d, 10 * dy / d + 3),
                    textcoords="offset points", fontsize=8.5, color=INK2,
                    ha="left" if dx >= 0 else "right", zorder=5)

# hub nodes + labels (identity by label; hub size = member warrant total)
# per-cluster label nudges (points), for hand-tuned collision relief
LABEL_NUDGE = {
    "03-vsatarcs-invariants": (45, 0),    # Self-Documenting Systems -> right
    "02-coupling-rational":  (-45, 0),    # Mathematical Reasoning -> left
}
amax = max(sum(n["applied"] for n in ns) for ns in by_cluster.values()) or 1
for cl, ns in sorted(by_cluster.items()):
    cx, cy = hub[cl]
    warrants = sum(n["applied"] for n in ns)
    s = 260 + 900 * (warrants / amax)
    ax.scatter([cx], [cy], s=s, color="#256abf", zorder=6,
               edgecolors=SURFACE, linewidths=1.6)
    (ndx, ndy) = LABEL_NUDGE.get(cl, (0, 0))
    ax.annotate(outward(cl), (cx, cy),
                xytext=(ndx, ndy + 16 + 9 * (s / 1160) ** 0.5),
                textcoords="offset points", color=INK,
                fontsize=7 if MARGIN else 15,
                fontweight="bold", ha="center", zorder=7,
                bbox=dict(boxstyle="round,pad=0.25", facecolor=SURFACE,
                          edgecolor="#d8d8d3", linewidth=0.7, alpha=0.95))
    if not MARGIN:
        ax.annotate(f"{len(ns)} missions · {warrants} warrants", (cx, cy),
                    xytext=(0, -20 - 9 * (s / 1160) ** 0.5), textcoords="offset points",
                    color=INK3, fontsize=9.5, ha="center", zorder=7)

# no in-figure title/subtitle (the consuming document captions it);
# a small corner stamp keeps the artifact self-dating.
ax.text(1, 0.005, f"computed {time.strftime('%Y-%m-%d')} · zero hand-typed rows",
        transform=ax.transAxes, color=INK3, fontsize=5 if MARGIN else 8,
        ha="right", va="bottom")

fig.tight_layout()
fig.savefig(OUT_PNG, bbox_inches="tight", transparent=True)
# SVG as well: the consuming document sets text in the page, and a raster's
# labels pixelate at print scale. Same figure, same data, vector text.
fig.savefig(OUT_SVG, bbox_inches="tight", format="svg", transparent=True)
#   PDF for the print build: pdflatex reads PDF natively and cannot read SVG.
#   Written in the same run as the SVG because the seed is a live query --
#   a second run is a different figure, not the same one in another format.
fig.savefig(OUT_PDF, bbox_inches="tight", format="pdf", transparent=True)
print("wrote", OUT_PNG, OUT_SVG, "and", OUT_PDF, "| ff-edges:", len(ff))
