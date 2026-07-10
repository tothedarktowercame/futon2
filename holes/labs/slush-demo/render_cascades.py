"""render_cascades.py — post-hoc gallery of cascade WIRING DIAGRAMS, arm-labeled.

Purely supplementary data (Joe, 2026-07-10/11): the deposits already carry the
fold's wiring (boxes, seq/copar hyperedges, terminals, satiety); this renders
each as a picture and computes structural metrics, grouped by proposer arm
(unsealed post-adjudication), to see whether one arm's cascades are visibly
more semilattice-like than the other's.

Encoding (identity never by color alone):
  seq edge   = solid arrow           copar edge = dashed, open diamond
  :full box  = solid border          :partial   = dashed border
  terminal   = double border (peripheries=2)
  arm        = header chip color + TEXT label (GFN #2a78d6 / INCUMBENT #d67e2a
               — pair validated via dataviz validate_palette.js, CVD dE 107)

Metrics per deposit: boxes, seq, copar, copar_fraction, depth (longest seq
chain), meet_nodes (boxes in >1 hyperedge), chain? (pure list), holes.
Semilattice-ness proxy = copar_fraction and meet_nodes: a pure list has
copar=0, meets=0, depth=n.

Run: python3 render_cascades.py   (writes findings/cascade-gallery/)
"""
from __future__ import annotations
import html, json, re, subprocess
from pathlib import Path

DEPOSITS = Path("/home/joe/code/futon6/data/fold-turns")
OUT = Path("/home/joe/code/futon2/holes/labs/slush-demo/findings/cascade-gallery")
PROPOSALS = Path("/home/joe/code/futon2/holes/labs/slush-demo/findings/proposals")

ARM_COLOR = {"gfn": "#2a78d6", "incumbent": "#d67e2a"}

BATCHES = {  # deposit stem -> (mission, batch)
    "ft-learning-loop-012": 1, "ft-futonzero-generative-012": 1, "ft-chipwitz-corps-012": 1,
    "ft-chipwitz-corps-013": 2, "ft-chipwitz-corps-014": 2,
    "ft-learning-loop-013": 2, "ft-learning-loop-014": 2,
    "ft-futonzero-generative-013": 2, "ft-futonzero-generative-014": 2,
    "ft-canon-fingerprint-store-002": 3, "ft-canon-fingerprint-store-003": 3,
    "ft-autoclock-in-002": 3, "ft-autoclock-in-003": 3,
    "ft-state-snapshot-witness-002": 3, "ft-state-snapshot-witness-003": 3,
}


def load_sealed():
    sealed = {}
    for f in PROPOSALS.glob("*.SEALED.json"):
        sealed.update(json.load(open(f)))
    return sealed


def parse_deposit(path: Path):
    t = path.read_text()
    hash_m = re.search(r':proposal/hash\s+"([0-9a-f]+)"', t)
    nodes = []
    for m in re.finditer(r'\{:id :(b\d+)\s+:role[^:]*:satiety :(\w+)', t):
        nodes.append({"id": m.group(1), "satiety": m.group(2)})
    if not nodes:  # alternate key order
        for m in re.finditer(r'\{:id :(b\d+)(?:(?!\{:id).)*?:satiety :(\w+)', t, re.S):
            nodes.append({"id": m.group(1), "satiety": m.group(2)})
    nodes = list({n["id"]: n for n in nodes}.values())
    # wiring hyperedges only (first :hyperedges block)
    wiring = t.split(":hyperedges")[1] if ":hyperedges" in t else ""
    wiring = wiring.split(":terminals")[0]
    edges = []
    for m in re.finditer(r'\{[^{}]*?:from \[([^\]]*)\][^{}]*?:to \[([^\]]*)\][^{}]*?:connective :(\w+)', wiring):
        frm = re.findall(r':(b\d+)', m.group(1))
        to = re.findall(r':(b\d+)', m.group(2))
        edges.append({"from": frm, "to": to, "connective": m.group(3)})
    terminals = re.findall(r':(b\d+)', t.split(":terminals")[1].split("]")[0]) if ":terminals" in t else []
    fits = {}
    for chunk in re.split(r'\{:id :', t)[1:]:
        bid = re.match(r'(b\d+)', chunk)
        fp = re.search(r':fits-pattern\s+"([^"]+)"', chunk[:2000])
        if bid and fp and bid.group(1) not in fits:
            fits[bid.group(1)] = fp.group(1)
    holes = len(set(re.findall(r':id :h\d+|\{:id "h\d+"|:(h\d+)\b', t.split(":policy-holes")[1]))) if ":policy-holes" in t else 0
    holes = len(re.findall(r'\{:id :h\d+|\{:id "h\d+"', t))
    return {"hash": hash_m.group(1) if hash_m else None, "nodes": nodes,
            "edges": edges, "terminals": set(terminals), "fits": fits,
            "holes": holes}


def metrics(d):
    seq = [e for e in d["edges"] if e["connective"] == "seq"]
    copar = [e for e in d["edges"] if e["connective"] == "copar"]
    tensor = [e for e in d["edges"] if e["connective"] == "tensor"]
    n = len(d["nodes"])
    # longest seq chain via DAG longest path over expanded seq pairs
    succ = {}
    for e in seq:
        for a in e["from"]:
            for b in e["to"]:
                succ.setdefault(a, set()).add(b)
    depth_cache = {}
    def depth(v, seen=()):
        if v in depth_cache: return depth_cache[v]
        if v in seen: return 0
        d_ = 1 + max((depth(w, seen + (v,)) for w in succ.get(v, ())), default=0)
        depth_cache[v] = d_
        return d_
    max_depth = max((depth(nd["id"]) for nd in d["nodes"]), default=0)
    part = {}
    for e in d["edges"]:
        for v in e["from"] + e["to"]:
            part[v] = part.get(v, 0) + 1
    meets = sum(1 for v, c in part.items() if c > 1)
    total = len(d["edges"])
    return {"boxes": n, "seq": len(seq), "copar": len(copar) + len(tensor),
            "copar_frac": round((len(copar) + len(tensor)) / total, 2) if total else 0.0,
            "depth": max_depth, "meets": meets,
            "chain": total > 0 and len(copar) + len(tensor) == 0 and max_depth == n,
            "holes": d["holes"]}


def to_dot(stem, d, arm):
    color = ARM_COLOR[arm]
    lines = [f'digraph "{stem}" {{',
             '  rankdir=LR; bgcolor="#fcfcfb"; pad=0.3;',
             '  node [fontname="Helvetica" fontsize=11 shape=box style="rounded,filled" '
             'fillcolor="#ffffff" color="#52514e" fontcolor="#0b0b0b" margin="0.12,0.08"];',
             '  edge [fontname="Helvetica" fontsize=9 color="#52514e"];',
             f'  label="{stem}  ·  {arm.upper()}"; labelloc=t; fontname="Helvetica-Bold"; '
             f'fontsize=13; fontcolor="{color}";']
    for nd in d["nodes"]:
        pat = d["fits"].get(nd["id"], "").split("/")[-1][:26]
        style = "rounded,filled" if nd["satiety"] == "full" else "rounded,filled,dashed"
        peri = 2 if nd["id"] in d["terminals"] else 1
        lines.append(f'  {nd["id"]} [label="{nd["id"]}\\n{pat}" style="{style}" '
                     f'peripheries={peri}];')
    for e in d["edges"]:
        for a in e["from"]:
            for b in e["to"]:
                if e["connective"] == "seq":
                    lines.append(f'  {a} -> {b} [label="seq"];')
                else:
                    lines.append(f'  {a} -> {b} [label="{e["connective"]}" style=dashed '
                                 f'arrowhead=odiamond dir=forward];')
    lines.append("}")
    return "\n".join(lines)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    sealed = load_sealed()
    rows = []
    cells = {}
    for stem, batch in sorted(BATCHES.items(), key=lambda kv: (kv[1], kv[0])):
        path = DEPOSITS / f"{stem}.edn"
        if not path.exists():
            continue
        d = parse_deposit(path)
        arm = sealed.get(d["hash"], {}).get("proposer", "unknown")
        m = metrics(d)
        rows.append({"deposit": stem, "batch": batch, "arm": arm, **m})
        dot = to_dot(stem, d, arm)
        svg = subprocess.run(["dot", "-Tsvg"], input=dot.encode(),
                             capture_output=True, check=True).stdout.decode()
        svg = svg[svg.index("<svg"):]
        (OUT / f"{stem}.svg").write_text(svg)
        cells[stem] = (arm, svg)

    # per-arm aggregates
    agg = {}
    for arm in ("gfn", "incumbent"):
        rs = [r for r in rows if r["arm"] == arm]
        if rs:
            agg[arm] = {k: round(sum(r[k] for r in rs) / len(rs), 2)
                        for k in ("boxes", "seq", "copar", "copar_frac", "depth", "meets", "holes")}
            agg[arm]["n"] = len(rs)
            agg[arm]["chains"] = sum(1 for r in rs if r["chain"])

    tbl = ["| deposit | batch | arm | boxes | seq | copar | copar% | depth | meets | chain? | holes |",
           "|---|---|---|---|---|---|---|---|---|---|---|"]
    for r in rows:
        tbl.append(f"| {r['deposit']} | {r['batch']} | {r['arm']} | {r['boxes']} | {r['seq']} "
                   f"| {r['copar']} | {r['copar_frac']} | {r['depth']} | {r['meets']} "
                   f"| {'CHAIN' if r['chain'] else 'lattice' if r['copar'] else ('single' if r['boxes']==1 else 'tree')} | {r['holes']} |")
    (OUT / "metrics.md").write_text(
        "# Cascade structural metrics (post-hoc, arm-labeled)\n\n" + "\n".join(tbl) +
        "\n\n## Per-arm means\n\n```\n" + json.dumps(agg, indent=1) + "\n```\n")

    # gallery html — text arm labels everywhere (contrast WARN relief); table = the table view
    parts = ["<!doctype html><meta charset='utf-8'><title>Cascade gallery</title>",
             "<body style='background:#fcfcfb;color:#0b0b0b;font-family:Helvetica,Arial,sans-serif;"
             "max-width:1100px;margin:2em auto;padding:0 1em'>",
             "<h1 style='font-size:20px'>Cascade wiring gallery — GFN vs incumbent (post-hoc)</h1>",
             "<p style='color:#52514e'>seq = solid arrow · copar = dashed diamond · dashed box = "
             ":partial satiety · double border = terminal. Arm is labeled in text on every diagram.</p>"]
    for stem, batch in sorted(BATCHES.items(), key=lambda kv: (kv[1], kv[0])):
        if stem in cells:
            arm, svg = cells[stem]
            parts.append(f"<div style='margin:1.5em 0;border-left:4px solid {ARM_COLOR.get(arm,'#999')};"
                         f"padding-left:1em'><div style='font-weight:bold'>{html.escape(stem)} "
                         f"· batch {batch} · <span style='color:{ARM_COLOR.get(arm,'#999')}'>"
                         f"{arm.upper()}</span></div>{svg}</div>")
    parts.append("<h2 style='font-size:16px'>Metrics</h2><pre style='color:#52514e'>" +
                 html.escape((OUT / 'metrics.md').read_text()) + "</pre></body>")
    (OUT / "gallery.html").write_text("\n".join(parts))
    print(f"rendered {len(cells)} diagrams -> {OUT}/gallery.html")
    print(json.dumps(agg, indent=1))


if __name__ == "__main__":
    main()
