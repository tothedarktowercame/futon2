#!/usr/bin/env python3
"""L17 ADVISORY GATE REPORT (discovery only; no wiring, no refusal imposed).

Usage: l17_advisory_report.py [FUTON3_CHECKS_DIR] [GRAPH_EDN] [OUT_MD]
Extracts each cascade artifact's consulted pattern ids (same extraction as
L4's strawman, for baseline comparability), runs runs/l13_graph_gate.clj
under all four readings, and writes the standing report with per-cascade
tables, bare exit codes, and the delta against L4's baseline.
"""
import os, re, sys, subprocess, tempfile

CHK = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/checks")
GRAPH = os.path.abspath(sys.argv[2] if len(sys.argv) > 2 else "L17-graph.edn")
OUT = sys.argv[3] if len(sys.argv) > 3 else "L17-advisory-gate-report.md"
GATE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "l13_graph_gate.clj")

# node id set from the graph, for unique-name resolution (L4's extraction)
nodes = re.findall(r'^  "([^"]+)"$', open(GRAPH, encoding="utf-8").read(), re.M)
ids = set(nodes)
name_index = {}
for n in nodes:
    name_index.setdefault(n.split("/", 1)[1], []).append(n)

def cascade_patterns(path):
    txt = open(path, encoding="utf-8", errors="replace").read()
    out = set()
    for tok in set(re.findall(r"[A-Za-z0-9][A-Za-z0-9._'-]*(?:/[A-Za-z0-9._'-]+)+", txt)):
        if tok in ids:
            out.add(tok)
    for kw in set(re.findall(r":([a-z0-9][a-z0-9-]{3,})", txt)):
        if kw in name_index and len(name_index[kw]) == 1:
            out.add(name_index[kw][0])
    return sorted(out)

READINGS = ["up-problems", "up-problems+wr", "down-problems", "down-problems+wr"]
cascades = {}
for fn in sorted(os.listdir(CHK)):
    if fn.endswith("-cascade.edn") or fn == "construct-cascade.edn":
        cascades[fn] = cascade_patterns(os.path.join(CHK, fn))

# L4 baseline refused counts (down-problems+wr) from the strawman receipt
L4_BASE = {"zaif-cascade.edn": 50, "construct-cascade.edn": 31,
           "snatch-cascade.edn": 12, "ants-cascade.edn": 6,
           "alfworld-cascade.edn": 1, "open-cascade.edn": 1187,
           "retrodiction-cascade.edn": 1187}

rows = []
tmp = tempfile.mkdtemp()
for fn in sorted(cascades):
    plist = os.path.join(tmp, fn + ".edn")
    open(plist, "w").write("[" + " ".join('"%s"' % p for p in cascades[fn]) + "]")
    res = {}
    for r in READINGS:
        p = subprocess.run(["bb", GATE, GRAPH, plist, r],
                           capture_output=True, text=True)
        refused = [l.strip() for l in p.stdout.splitlines()
                   if l.startswith("   ")]
        m = re.search(r"gate: (\d+)/(\d+) refused", p.stdout)
        res[r] = (p.returncode,
                  int(m.group(1)) if m else None,
                  int(m.group(2)) if m else None, refused)
    rows.append((fn, cascades[fn], res))

out = ["# L17 — advisory gate report (discovery only)\n",
"Gate: `runs/l13_graph_gate.clj` (offered, not wired). Graph: current",
"`L17-graph.edn` (futon3 `%s`); cascades: the seven checks-directory"
% os.environ.get("L17_SHA", ""),
"artifacts L4 enumerated, consulted-ids extracted identically to L4 for",
"comparability. Readings: the four from L1/L4. No library or checks file",
"was modified by this row; exit codes are the gate's bare exits.\n",
"## Per-cascade x reading (refused/consulted, exit)\n",
"| cascade | consulted | up-p | up-p+wr | down-p | down-p+wr | L4 baseline (down-p+wr) |",
"|---|---|---|---|---|---|---|"]
for (fn, plist, res) in rows:
    cells = []
    for r in READINGS:
        code, nref, ntot, _ = res[r]
        cells.append("%d/%d, exit %d" % (nref, ntot, code))
    out.append("| %s | %d | %s | %s | %s | %s | %d |" %
               (fn, len(plist), cells[0], cells[1], cells[2], cells[3],
                L4_BASE.get(fn, -1)))
out.append("\n## Delta against L4's baseline (down-problems+wr)\n")
for (fn, plist, res) in rows:
    code, nref, ntot, _ = res["down-problems+wr"]
    base = L4_BASE.get(fn, -1)
    out.append("- `%s`: %d refused at L4 -> %d now (%+d); consulted %d."
               % (fn, base, nref, nref - base, ntot))
out.append("\n## Refused patterns, named (harness cascade, all readings)\n")
for (fn, plist, res) in rows:
    if fn != "zaif-cascade.edn":
        continue
    for r in READINGS:
        code, nref, ntot, refused = res[r]
        out.append("- **%s**: %d refused -- %s" %
                   (r, nref, ", ".join(refused) if refused else "(none)"))
out.append("\nStanding conclusion: after L5-L16 the served surface is still "
"majority-refused under every reading except where the exotype cohort's "
"shared grounding dominates (open/retrodiction whole-library cascades). "
"The direction choice and any wiring remain reserved to F7/F8; this report "
"imposes nothing.")
open(OUT, "w").write("\n".join(out) + "\n")
print("l17: %d cascades x %d readings reported -> %s" % (len(rows), len(READINGS), OUT))
