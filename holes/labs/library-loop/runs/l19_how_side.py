#!/usr/bin/env python3
"""L19 THE @how SIDE, measured (discovery only; no annotations, no gate change).

Usage: l19_how_side.py [FUTON3_LIBRARY_DIR] [CHECKS_DIR] [OUT_MD]
Measures, on the current library graph:
  1. @how edge-target distribution: how many @how values resolve to pattern
     ids vs remain prose;
  2. candidate @how root sets with counts (measured, not decided):
     mechanism-bearing nodes (outgoing resolvable @how), patterns whose @how
     resolves, problems nodes, and combinations;
  3. combined how-or-why readings for the seven L4-enumerated cascades:
     refusal deltas under how-OR-why reachability from each candidate root
     set, against the why-only down-problems+wr baseline.
Path-independent provenance; deterministic.
"""
import os, re, sys, subprocess

LIB = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library")
CHK = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                      else "/home/joe/code/futon3/checks")
OUT = sys.argv[3] if len(sys.argv) > 3 else "L19-how-side.md"

ann_line = re.compile(r"^@([A-Za-z0-9_-]+)(?:\s+(.*))?$")
ref_tok = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9._-]+)+")

def parse_annotations(lines):
    anns = {}
    i = 0
    while i < len(lines):
        m = ann_line.match(lines[i])
        if not m:
            i += 1
            continue
        key, parts = m.group(1), []
        if m.group(2):
            parts.append(m.group(2).strip())
        i += 1
        while i < len(lines):
            ln = lines[i]
            if ln.startswith((" ", "\t")):
                parts.append(ln.strip()); i += 1
            elif ln.strip() == "" and i + 1 < len(lines) and lines[i+1].startswith((" ", "\t")):
                i += 1
            else:
                break
        anns.setdefault(key, []).append("\n".join(parts).strip())
    return anns

nodes = {}
for root, dirs, fnames in os.walk(LIB):
    dirs.sort()
    for fn in sorted(fnames):
        if fn.endswith(".flexiarg"):
            rel = os.path.relpath(os.path.join(root, fn), LIB)
            with open(os.path.join(root, fn), encoding="utf-8", errors="replace") as f:
                nodes[rel[:-len(".flexiarg")]] = parse_annotations(f.read().splitlines())

def edges_of(kind):
    res = {}
    for pid, anns in nodes.items():
        for val in anns.get(kind, []):
            for t in ref_tok.findall(val):
                if t in nodes and t != pid:
                    res.setdefault(pid, set()).add(t)
    return res

why, how = edges_of("why"), edges_of("how")
how_vals = [(pid, v) for pid, anns in nodes.items() for v in anns.get("how", [])]
how_total = len(how_vals)
how_resolving = sum(1 for (pid, v) in how_vals
                    for t in ref_tok.findall(v) if t in nodes and t != pid)
# prose-only: @how values whose ref-shaped tokens all fail to resolve
how_prose_files = {pid for pid in nodes
                   if nodes[pid].get("how")
                   and not any(t in nodes and t != pid
                               for v in nodes[pid]["how"] for t in ref_tok.findall(v))}

problems = {n for n in nodes if n.startswith("problems/")}
wr = {n for n in nodes if n.startswith("war-room/wr-")}
mech_bearing = set(how)                       # outgoing resolvable @how
how_targets = set().union(*how.values()) if how else set()

def reach(adj, roots):
    seen, frontier = set(), list(roots)
    radj = {}
    for a, bs in adj.items():
        for b in bs:
            radj.setdefault(b, set()).add(a)   # downward: target justifies source
    while frontier:
        n = frontier.pop()
        if n in seen:
            continue
        seen.add(n)
        frontier.extend(radj.get(n, ()))
    return seen

def reach_up(adj, roots):
    seen, frontier = set(), list(roots)
    while frontier:
        n = frontier.pop()
        if n in seen:
            continue
        seen.add(n)
        frontier.extend(adj.get(n, ()))
    return seen

ROOTSETS = {
    "problems": problems,
    "problems+wr": problems | wr,
    "problems+mech-bearing": problems | mech_bearing,
    "problems+wr+mech-bearing": problems | wr | mech_bearing,
    "problems+how-targets": problems | how_targets,
}

# cascades (L4 extraction)
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
cascades = {}
for fn in sorted(os.listdir(CHK)):
    if fn.endswith("-cascade.edn") or fn == "construct-cascade.edn":
        cascades[fn] = cascade_patterns(os.path.join(CHK, fn))

def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

sha = subprocess.run("git -C %s rev-parse HEAD" % LIB, shell=True,
                     capture_output=True, text=True).stdout.strip()

lines = []
A = lines.append
A("# L19 — the @how side, measured (discovery only)\n")
A("Library at futon3 `%s`. No annotations written, no gate change, no library" % sha)
A("file modified. Deterministic; byte-identical rerun.\n")
A("## 1. @how edge-target distribution\n")
A("| measure | count |")
A("|---|---|")
A("| patterns carrying @how | %d |" % len([n for n in nodes if nodes[n].get("how")]))
A("| @how annotation values total | %d |" % how_total)
A("| resolvable @how edges (value tokens that are pattern ids) | %d |" % len([1 for a, bs in how.items() for _ in bs]))
A("| patterns whose @how is entirely prose (no resolvable token) | %d |" % len(how_prose_files))
A("| distinct @how targets (mechanism patterns pointed at) | %d |" % len(how_targets))
A("\nNote: the L3-L10 backfill wrote own-mechanism @how lines that quote the")
A("pattern's own conclusion -- by construction self-referential or prose, so")
A("they do not create edges; the resolvable @how edges predate or come from")
A("curated editorial @how lines (README-flexiarg 5a: @how is written by an")
A("editor, later).\n")
A("## 2. Candidate @how root sets, measured (not decided)\n")
A("| root set | size |")
A("|---|---|")
A("| problems | %d |" % len(problems))
A("| problems+wr | %d |" % len(problems | wr))
A("| mechanism-bearing nodes (outgoing resolvable @how) | %d |" % len(mech_bearing))
A("| problems+mech-bearing | %d |" % len(problems | mech_bearing))
A("| problems+wr+mech-bearing | %d |" % len(problems | wr | mech_bearing))
A("| problems+how-targets | %d |" % len(problems | how_targets))
A("\n## 3. Combined how-OR-why readings for the seven cascades\n")
A("Traversal: downward justified-by over @why edges UNION @how edges (a")
A("pattern is reachable if a root justifies it via either relation).\n")
A("| cascade | consulted | why-only (p+wr) | +how (p) | +how (p+wr) | +how (p+mech) | +how (p+wr+mech) |")
A("|---|---|---|---|---|---|---|")
whyhow = {a: set(bs) for a, bs in why.items()}  # deep copy: protect the why-only baseline
for a, bs in how.items():
    whyhow.setdefault(a, set()).update(bs)
for fn in sorted(cascades):
    cl = cascades[fn]
    base = reach(why, problems | wr)
    cells = [len([p for p in cl if p not in base])]
    for name in ["problems", "problems+wr", "problems+mech-bearing", "problems+wr+mech-bearing"]:
        rs = reach(whyhow, ROOTSETS[name])
        cells.append(len([p for p in cl if p not in rs]))
    A("| %s | %d | %d | %d | %d | %d | %d |" % (fn, len(cl), *cells))
A("\n## Recommendation sketch (with costs, deciding nothing)\n")
A("- The @how side adds few new roots today: most resolvable @how edges run")
A("  BETWEEN ordinary patterns, so 'mechanism-bearing' as a root set admits")
A("  %d nodes and moves the served cascades by the margins above." % len(mech_bearing))
A("- Cost of adopting a combined reading: the census/gate would need a second")
A("  edge kind traversed and an agreed root set; the choice is Joe's/F7-F8's.")
A("- Reversal: if @how edges are later curated en masse (editorial), the")
A("  mechanism-bearing root set grows and the combined reading changes shape.")
open(OUT, "w").write("\n".join(lines) + "\n")
print("l19: %d nodes, %d resolvable @how edges, report -> %s"
      % (len(nodes), sum(len(v) for v in how.values()), OUT))
