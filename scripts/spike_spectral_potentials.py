#!/usr/bin/env python3
"""spike_spectral_potentials.py — E-evaluate-policies-spikes, spike 2.

Question (mission §9.6, Joe's PageRank suggestion): how much of the cascade
valuation is recoverable by a FIELD-LIFT — a global stationary potential
computed once at node grain (tractable: power iteration over the pattern
phylogeny), lifted to cascades by summation — versus the constructor's real,
non-additive functionals (wholeness T·H, F = accuracy − λ·complexity)?

Method:
  1. PageRank over the pattern phylogeny (co_app weighted symmetric + descent
     directed, damping 0.85, uniform teleport) → potential π(p) per pattern.
  2. Construct real cascades for a fixed mission sample via the PRODUCTION
     constructor + PRODUCTION ψ recipe (same code paths as the ARGUE exhibit).
  3. Per cascade: potential-sum Σπ(p) and potential-mean over shown patterns.
  4. Spearman rank correlation of potential-sum vs wholeness and vs F across
     the sample. High ρ ⇒ the additive shadow carries the ordering (cheap
     PageRank prior is a usable menu-ranker); low ρ ⇒ the non-additive terms
     (coherence products, saturation, complexity) dominate ⇒ field-lift gives
     bounds/priors only — mission §9.6's caveat, measured.

Honesty: n is small (mission sample), ψ is the thin proxy §9.7 criticises —
this ranks proposals under the SAME thin conditioning the production lane
uses, deliberately: it isolates additive-vs-non-additive, not conditioning.

Usage: futon3a/.venv/bin/python futon2/scripts/spike_spectral_potentials.py
Writes: futon2/holes/labs/M-evaluate-policies/spike-spectral/spectral-comparison.json
"""
import json, re, sys
from pathlib import Path

HOME = Path.home()
CODE = HOME / "code"
OUT_DIR = CODE / "futon2/holes/labs/M-evaluate-policies/spike-spectral"
PHYLO = CODE / "futon6/data/pattern-phylogeny-edges.json"

sys.path.insert(0, str(CODE / "futon3a/holes/labs/M-memes-arrows"))
from cascade_construct import construct_cascade  # noqa: E402

MISSION_SAMPLE = [
    "M-evaluate-policies", "M-G-over-cascades", "M-aif2", "M-fold-ansatz",
    "M-typed-bells", "M-first-flights", "M-apm-solutions",
    "M-invariant-queue-unstuck", "M-futonzero-grounding",
    "M-differentiable-substrate", "M-efe-bge-followon-actions",
    "M-canon-fingerprint-store", "M-bayesian-structure-learning",
]
BUDGET = 6
LAMBDA = 0.25
DAMPING = 0.85

MISSION_DOC_ROOTS = [CODE / r / s
                     for r in ["futon0", "futon1", "futon2", "futon3", "futon3a", "futon3b",
                               "futon3c", "futon4", "futon5", "futon5a", "futon6", "futon7"]
                     for s in ["holes/missions", "holes"]]

def mission_psi(target):
    stem = re.sub(r"^M-", "", target).replace("-", " ")
    for root in MISSION_DOC_ROOTS:
        f = root / f"{target}.md"
        if f.exists():
            lines = f.read_text().splitlines()
            title = next((m.group(1) for l in lines
                          if (m := re.match(r"^#\s*(?:Mission:)?\s*(.*)", l))), None)
            status = next((m.group(1) for l in lines
                           if (m := re.match(r"(?i)^\*\*Status:?\*\*:?\s*(.*)", l))), "")
            if title:
                psi = f"{stem} — want: {title.strip()[:160]}"
                if status.strip():
                    psi += f". have: {status.strip()[:160]}"
                return psi
    return stem

def stem_of(pid):
    return pid.split("/")[-1]

def pagerank(phylo, damping=DAMPING, iters=200, tol=1e-12):
    nodes = sorted({stem_of(p) for p in phylo["patterns"]}
                   | {stem_of(x) for e in phylo["co_app"] for x in e[:2]}
                   | {stem_of(x) for e in phylo["descent"] for x in e})
    idx = {n: i for i, n in enumerate(nodes)}
    out = [{} for _ in nodes]
    def add(a, b, w):
        i, j = idx[stem_of(a)], idx[stem_of(b)]
        if i != j:
            out[i][j] = out[i].get(j, 0.0) + w
    for a, b, w in phylo["co_app"]:      # symmetric, weighted
        add(a, b, float(w)); add(b, a, float(w))
    for a, b in phylo["descent"]:        # directed parent -> child
        add(a, b, 1.0)
    n = len(nodes)
    pr = [1.0 / n] * n
    for _ in range(iters):
        nxt = [(1.0 - damping) / n] * n
        dangling = sum(pr[i] for i in range(n) if not out[i])
        for i in range(n):
            if out[i]:
                tot = sum(out[i].values())
                share = damping * pr[i]
                for j, w in out[i].items():
                    nxt[j] += share * w / tot
        db = damping * dangling / n
        nxt = [x + db for x in nxt]
        if sum(abs(a - b) for a, b in zip(nxt, pr)) < tol:
            pr = nxt
            break
        pr = nxt
    return {nodes[i]: pr[i] for i in range(n)}

def spearman(xs, ys):
    def ranks(v):
        order = sorted(range(len(v)), key=lambda i: v[i])
        r = [0.0] * len(v)
        i = 0
        while i < len(order):
            j = i
            while j + 1 < len(order) and v[order[j + 1]] == v[order[i]]:
                j += 1
            avg = (i + j) / 2.0 + 1
            for k in range(i, j + 1):
                r[order[k]] = avg
            i = j + 1
        return r
    rx, ry = ranks(xs), ranks(ys)
    n = len(xs)
    mx, my = sum(rx) / n, sum(ry) / n
    num = sum((a - mx) * (b - my) for a, b in zip(rx, ry))
    dx = sum((a - mx) ** 2 for a in rx) ** 0.5
    dy = sum((b - my) ** 2 for b in ry) ** 0.5
    return num / (dx * dy) if dx and dy else float("nan")

def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    phylo = json.loads(PHYLO.read_text())
    pot = pagerank(phylo)
    rows = []
    for target in MISSION_SAMPLE:
        psi = mission_psi(target)
        r = construct_cascade(psi, epsilon=0.15)
        shown = [p for (p, _rel, _mc) in r["cascade"][:BUDGET]]
        pots = [pot.get(stem_of(p), 0.0) for p in shown]
        rows.append({
            "target": target,
            "size-shown": len(shown),
            "size-saturated": r["size"],
            "wholeness": r["wholeness"],
            "F": r["cascade-score"],
            "potential-sum": sum(pots),
            "potential-mean": (sum(pots) / len(pots)) if pots else 0.0,
            "patterns": shown,
        })
        print(f"{target:38s} n={len(shown)} wholeness={r['wholeness']:.3f} "
              f"F={r['cascade-score']:.3f} Σπ={sum(pots)*1000:.3f}‰")
    ws = [r["wholeness"] for r in rows]
    fs = [r["F"] for r in rows]
    ps = [r["potential-sum"] for r in rows]
    pm = [r["potential-mean"] for r in rows]
    ns = [r["size-shown"] for r in rows]
    summary = {
        "n": len(rows),
        "damping": DAMPING,
        "spearman": {
            "potential-sum vs wholeness": round(spearman(ps, ws), 3),
            "potential-sum vs F": round(spearman(ps, fs), 3),
            "potential-mean vs wholeness": round(spearman(pm, ws), 3),
            "potential-mean vs F": round(spearman(pm, fs), 3),
            "size vs wholeness (confound check)": round(spearman([float(x) for x in ns], ws), 3),
            "size vs potential-sum (confound check)": round(spearman([float(x) for x in ns], ps), 3),
        },
        "reading": "high rho => additive field-lift carries the ordering (cheap menu prior); low rho => non-additive terms dominate => bounds/priors only (mission §9.6 caveat, measured)",
    }
    out = {"summary": summary, "cascades": rows,
           "top-10-potentials": sorted(pot.items(), key=lambda kv: -kv[1])[:10]}
    (OUT_DIR / "spectral-comparison.json").write_text(json.dumps(out, indent=1))
    print("\n=== SPEARMAN ===")
    for k, v in summary["spearman"].items():
        print(f"  {k}: {v}")
    print(f"\nWrote {OUT_DIR / 'spectral-comparison.json'}")

if __name__ == "__main__":
    main()
