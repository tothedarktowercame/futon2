#!/usr/bin/env python3
"""Probe: where do the paper's catalog patterns fall in the pca3-v1 zones?

Read-only overlay probe (M-capability-zones, meta-pattern layer idea,
2026-07-19). No artifact of record is modified; output goes to a probe file.
"""
from __future__ import annotations

import json
import math
import re
from pathlib import Path

TEX = Path("/home/joe/code/p4ng/main-2026.tex")
REDUCTION = Path("data/capability_zones/reduction-pca3-v1.json")
SEEDS = Path("resources/capability_zones/action_class_seeds.json")
OUT = Path("data/capability_zones/pattern-probe-pca3-v1.json")
MODEL = "BAAI/bge-large-en-v1.5"


def detex(s: str) -> str:
    s = re.sub(r"\\(?:textbf|textsc|emph|texttt|path)\{([^{}]*)\}", r"\1", s)
    s = re.sub(r"\\(?:footnote|eqanchor)\{[^{}]*\}", "", s)
    s = re.sub(r"\$[^$]*\$", " ", s)
    s = re.sub(r"\\[a-zA-Z]+\*?(\[[^]]*\])?", " ", s)
    s = re.sub(r"[{}~]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def catalog_patterns() -> list[dict]:
    text = TEX.read_text()
    out = []
    for m in re.finditer(r"\\paragraph\{([^}]+)\}(.*?)(?=\\paragraph\{|\\section|\\subsection|\Z)",
                         text, re.S):
        name, body = m.group(1), m.group(2)
        if "\\textbf{If}" in body and "\\textbf{However}" in body:
            out.append({"pattern": detex(name), "text": detex(body)[:4000]})
    return out


def main() -> None:
    patterns = catalog_patterns()
    print(f"[probe] extracted {len(patterns)} catalog patterns")
    from sentence_transformers import SentenceTransformer
    model = SentenceTransformer(MODEL)
    vecs = model.encode([p["text"] for p in patterns], normalize_embeddings=True)

    reduction = json.loads(REDUCTION.read_text())
    seeds = json.loads(SEEDS.read_text())
    mean, comps = reduction["mean"], reduction["components"]

    def to3d(v):
        c = [x - m for x, m in zip(v, mean)]
        return [sum(a * b for a, b in zip(comp, c)) for comp in comps]

    seeds3 = []
    for s in seeds:
        centroid = (s.get("centroid_evidence_count") or 0) > 0
        seeds3.append({"class": s["class"],
                       "generation": "centroid" if centroid else "text",
                       "point": to3d(s["centroid_seed"] if centroid else s["text_seed"])})

    results = []
    for p, v in zip(patterns, vecs):
        pt = to3d([float(x) for x in v])
        ranked = sorted(({"class": s["class"],
                          "d": math.dist(s["point"], pt)} for s in seeds3),
                        key=lambda r: (r["d"], r["class"]))
        results.append({**p, "text": p["text"][:200] + "…",
                        "zone": ranked[0]["class"], "distance": ranked[0]["d"],
                        "margin": ranked[1]["d"] - ranked[0]["d"],
                        "runner_up": ranked[1]["class"], "point_3d": pt})

    OUT.write_text(json.dumps({"schema": "capability-zone-pattern-probe.v1",
                               "reduction_version": reduction["version"],
                               "results": results}, indent=1))
    zone_counts: dict = {}
    for r in sorted(results, key=lambda r: (r["zone"], -r["margin"])):
        zone_counts[r["zone"]] = zone_counts.get(r["zone"], 0) + 1
        print(f"{r['zone']:<18} m={r['margin']:.4f} (ru {r['runner_up']:<15}) {r['pattern'][:60]}")
    print("\nzones claimed:", json.dumps(zone_counts))
    print("zones unclaimed:", sorted({s['class'] for s in seeds3} - set(zone_counts)))


if __name__ == "__main__":
    main()
