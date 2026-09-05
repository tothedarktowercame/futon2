#!/usr/bin/env python3
"""L4 GRAPH-CERTIFICATE STRAWMAN measurement (discovery only; decides nothing).

Usage: l4_certificate_strawman.py [FUTON3_LIBRARY_DIR] [CHECKS_DIR] [OUT_DIR]
Defaults: canonical checkouts; explicit args for isolated worktree reruns.

Against the actual L1 graph (regenerated via l1_census.py semantics) it
measures, for each candidate invariant, how many current patterns and cascade
artifacts would be refused TODAY:
  (a) every pattern consulted by a cascade is @why-reachable from a named
      problem node;
  (b) no @why cycle lacks a grounding problem node;
  (c) cascade construction refuses a pattern with no rationale path
      (no authored @why, or no @why path to a problem node, per reading).
Nothing is implemented, refused, or installed. No library file is written.
"""
import os, re, sys, subprocess, hashlib, json

LIB = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library")
CHK = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                      else "/home/joe/code/futon3/checks")
OUTD = os.path.abspath(sys.argv[3] if len(sys.argv) > 3
                       else os.path.dirname(os.path.abspath(__file__)))
HERE = os.path.dirname(os.path.abspath(__file__))

ann_line = re.compile(r"^@([A-Za-z0-9_-]+)(?:\s+(.*))?$")
ref_tok = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9._-]+)+")

def parse_annotations(lines):
    anns = {}
    headers = 0
    i = 0
    while i < len(lines):
        m = ann_line.match(lines[i])
        if not m:
            i += 1
            continue
        headers += 1
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
    return anns, headers

nodes, why_up, why_down = {}, {}, {}   # why_up: child -> [parents(rationales)]
for root, dirs, fnames in os.walk(LIB):
    dirs.sort()
    for fn in sorted(fnames):
        if not fn.endswith(".flexiarg"):
            continue
        rel = os.path.relpath(os.path.join(root, fn), LIB)
        pid = rel[:-len(".flexiarg")]
        with open(os.path.join(root, fn), encoding="utf-8", errors="replace") as f:
            anns, _ = parse_annotations(f.read().splitlines())
        nodes[pid] = anns

def why_targets(pid):
    out = []
    for val in nodes[pid].get("why", []):
        out += [t for t in ref_tok.findall(val) if t in nodes]
    return out

problems = {n for n in nodes if n.startswith("problems/")}
wr = {n for n in nodes if n.startswith("war-room/wr-")}
probset_strict = problems
probset_wide = problems | wr

def reach_down(roots):
    seen, frontier = set(), list(roots)
    adj = {}
    for n in nodes:
        for t in why_targets(n):
            adj.setdefault(t, []).append(n)
    while frontier:
        n = frontier.pop()
        if n in seen:
            continue
        seen.add(n)
        frontier.extend(adj.get(n, ()))
    return seen

def reach_up(roots):
    seen, frontier = set(), list(roots)
    while frontier:
        n = frontier.pop()
        if n in seen:
            continue
        seen.add(n)
        frontier.extend(why_targets(n))
    return seen

reach = {
    "down-problems": reach_down(probset_strict),
    "down-problems+wr": reach_down(probset_wide),
    "up-problems": reach_up(probset_strict),
    "up-problems+wr": reach_up(probset_wide),
}
carrying_why = {n for n in nodes if "why" in nodes[n]}

# (b) @why cycles (authored @why only), and grounding check
colour, cycles = {}, []
adj = {n: why_targets(n) for n in nodes}
def visit(n, path):
    c = colour.get(n)
    if c == "grey":
        cycles.append(path[path.index(n):] + [n]); return
    if c == "black":
        return
    colour[n] = "grey"
    for t in adj[n]:
        visit(t, path + [n])
    colour[n] = "black"
sys.setrecursionlimit(10000)
for n in sorted(nodes):
    if colour.get(n) is None:
        visit(n, [])
cycles = sorted(tuple(c) for c in cycles)
cycles_grounded_strict = [c for c in cycles if any(x in probset_strict for x in c)]
cycles_grounded_wide = [c for c in cycles if any(x in probset_wide for x in c)]

# cascade artifacts: pattern ids consulted
name_index = {}
for n in nodes:
    name_index.setdefault(n.split("/", 1)[1], []).append(n)

def cascade_patterns(path):
    txt = open(path, encoding="utf-8", errors="replace").read()
    ids = set()
    for tok in set(re.findall(r"[A-Za-z0-9][A-Za-z0-9._'-]*(?:/[A-Za-z0-9._'-]+)+", txt)):
        if tok in nodes:
            ids.add(tok)
    # bare keyword names: unique across sections
    for kw in set(re.findall(r":([a-z0-9][a-z0-9-]{3,})", txt)):
        if kw in name_index and len(name_index[kw]) == 1:
            ids.add(name_index[kw][0])
    return ids

cascades = {}
for fn in sorted(os.listdir(CHK)):
    if fn.endswith("-cascade.edn") or fn == "construct-cascade.edn":
        cascades[fn] = cascade_patterns(os.path.join(CHK, fn))

def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

librepo = subprocess.run("git -C %s rev-parse --show-toplevel" % os.path.dirname(LIB),
                         shell=True, capture_output=True, text=True).stdout.strip()
sha = subprocess.run("git -C %s rev-parse HEAD" % librepo, shell=True,
                     capture_output=True, text=True).stdout.strip()

lines = []
A = lines.append
A(";; L4 graph-certificate strawman measurement. Discovery only; decides")
A(";; nothing; implements no refusal. Deterministic rerun.")
A("{:library-subdir %s\n :library-git-sha %s" % (q(os.path.relpath(LIB, librepo)), q(sha)))
A(" :patterns-total %d\n :carrying-why %d" % (len(nodes), len(carrying_why)))
A(" :problem-nodes %d :wr-nodes %d" % (len(probset_strict), len(wr)))
A(" :reachability {%s}" % ", ".join("%s %d" % (k, len(v)) for k, v in sorted(reach.items())))
A(" :cycles {:why-cycles %d :grounded-at-problem %d :grounded-at-problem+wr %d\n           :ungrounded-strict [%s]\n           :ungrounded-wide [%s]}"
  % (len(cycles), len(cycles_grounded_strict), len(cycles_grounded_wide),
     " ".join(q(" -> ".join(c)) for c in cycles if c not in cycles_grounded_strict),
     " ".join(q(" -> ".join(c)) for c in cycles if c not in cycles_grounded_wide)))
A(" :invariant-a-cascade-patterns-not-why-reachable [")
for fn in sorted(cascades):
    ids = cascades[fn]
    row = {"file": fn, "consulted": len(ids)}
    for k, v in sorted(reach.items()):
        row["refused-" + k] = sorted(ids - v)
    A("  {:file %s :consulted %d" % (q(fn), len(ids)))
    A("   :refused-down-problems %d :refused-down-problems+wr %d :refused-up-problems %d :refused-up-problems+wr %d"
      % (len(row["refused-down-problems"]), len(row["refused-down-problems+wr"]),
         len(row["refused-up-problems"]), len(row["refused-up-problems+wr"])))
    A("   :no-authored-why %d}" % len([i for i in ids if i not in carrying_why]))
A(" ]\n :invariant-c-library-wide-refusal-counts {")
A("  :no-authored-why %d" % (len(nodes) - len(carrying_why)))
for k, v in sorted(reach.items()):
    A("  :not-why-reachable-%s %d" % (k, len(nodes) - len(v)))
A(" }}")

out = os.path.join(OUTD, "L4-certificate-strawman-receipt.edn")
open(out, "w").write("\n".join(lines) + "\n")
print("patterns=%d why=%d cycles=%d cascades=%s" %
      (len(nodes), len(carrying_why), len(cycles),
       {f: len(cascades[f]) for f in sorted(cascades)}))
