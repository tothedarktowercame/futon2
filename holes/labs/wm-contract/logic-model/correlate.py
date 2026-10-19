#!/usr/bin/env python3
"""Join the PLOP-2026 code annotations (p4ng/plop-2026-code-annotations.json)
with the static logic-model tables in this directory, on the annotation anchor
(file path + function symbol). Prints one markdown table: for each annotation,
which tables mention its anchor file and symbol, and on which lines."""
import json, re, sys, pathlib, collections
HERE = pathlib.Path(__file__).resolve().parent
ANN = pathlib.Path("/home/joe/code/p4ng/plop-2026-code-annotations.json")
TABLES = ["phases", "refusals", "producers", "consumers", "stores"]
docs = {t: (HERE / f"{t}.md").read_text().splitlines() if (HERE / f"{t}.md").exists() else None for t in TABLES}
d = json.load(open(ANN))
def hits(needle):
    out = {}
    for t, lines in docs.items():
        if lines is None: continue
        rows = [i + 1 for i, l in enumerate(lines) if needle in l]
        if rows: out[t] = rows
    return out
print("| pattern | status | anchor (file::symbol) | phases | refusals | producers | consumers | stores |")
print("|---|---|---|---|---|---|---|---|")
summary = collections.Counter()
for a in d["annotations"]:
    for x in a.get("anchors", []):
        if x.get("repo") != "futon2": continue
        fname = pathlib.Path(x["path"]).name
        sym = (x.get("symbol") or "").split(" ")[0].strip(":")
        fh = hits(fname); sh = hits(sym) if sym else {}
        cells = []
        for t in TABLES:
            if docs[t] is None: cells.append("(pending)"); continue
            f, s = fh.get(t, []), sh.get(t, [])
            both = sorted(set(f) & set(s))
            cells.append(("sym " + ",".join(map(str, both[:4]))) if both else (("file " + ",".join(map(str, f[:3]))) if f else "-"))
        on_model = any(c.startswith("sym") for c in cells)
        summary[(a["status_current"], on_model)] += 1
        print(f"| {a['pattern']} | {a['status_current']} | {fname}::{sym} | " + " | ".join(cells) + " |")
print()
print("Anchors by (annotation status, found-by-symbol-in-any-table):")
for k, v in sorted(summary.items()): print(f"  {k}: {v}")
