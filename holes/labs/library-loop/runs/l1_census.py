#!/usr/bin/env python3
"""L1 RATIONALE-REACHABILITY CENSUS (library-loop row L1, class :M).

Read-only over /home/joe/code/futon3/library. Emits:
  L1-census-receipt.edn  -- counts + annotation histogram + unresolved refs
  L1-census-graph.edn    -- nodes + edges (nodes+edges EDN, not prose)
  L1-census-report.md    -- short report incl. the grep floor command

Deterministic: every collection is sorted before emission, so a rerun on
an unchanged library is byte-identical.
"""
import os, re, sys, hashlib, subprocess
from collections import defaultdict

LIB = "/home/joe/code/futon3/library"
OUT = os.path.dirname(os.path.abspath(__file__))

ann_line = re.compile(r"^@([A-Za-z0-9_-]+)\s+(.*)$")
# annotation value tokens shaped like section/name refs
ref_tok = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9._-]+)+")

def q(s):  # EDN string
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

def kw(s):
    return ":" + re.sub(r"[^A-Za-z0-9-]", "-", s)

nodes = {}          # id -> {annotations}
edges = []          # [id from, kind, id to, resolved?]
ann_hist = defaultdict(int)   # annotation key -> occurrence count across files
files = 0
files_missing_flexiarg = []

for root, dirs, fnames in os.walk(LIB):
    dirs.sort()
    for fn in sorted(fnames):
        if not fn.endswith(".flexiarg"):
            continue
        path = os.path.join(root, fn)
        rel = os.path.relpath(path, LIB)
        pid = rel[:-len(".flexiarg")]
        files += 1
        anns = defaultdict(list)
        with open(path, encoding="utf-8", errors="replace") as f:
            for line in f:
                m = ann_line.match(line)
                if not m:
                    continue
                key, val = m.group(1), m.group(2).strip()
                anns[key].append(val)
                ann_hist[key] += 1
        if "flexiarg" not in anns:
            files_missing_flexiarg.append(pid)
        nodes[pid] = dict(anns)

# build edges: every annotation value, every ref-shaped token
unresolved = set()
for pid in sorted(nodes):
    for key, vals in nodes[pid].items():
        for val in vals:
            for tok in ref_tok.findall(val):
                if tok in nodes:
                    if tok != pid or key in ("merged-from",):
                        edges.append((pid, key, tok, True))
                else:
                    unresolved.add((pid, key, tok))
                    edges.append((pid, key, tok, False))

# problem-stating node sets
problems = sorted(n for n in nodes if n.startswith("problems/"))
wr = sorted(n for n in nodes if n.startswith("war-room/wr-"))

# @why-reachability, reading A ("chains down to a problem"):
# P is reachable iff following @why edges upward (P --why--> target) can
# reach a problem-stating node.
def reachable_up(roots):
    seen = set()
    frontier = list(roots)
    while frontier:
        n = frontier.pop()
        if n in seen:
            continue
        seen.add(n)
        for val in nodes.get(n, {}).get("why", []):
            t = ref_tok.findall(val)
            for tok in t:
                frontier.append(tok)
    return seen

# reading B ("justified-by edges traversed downward from problems"):
# edge R -> P when P's @why is R; roots are the problem-stating nodes.
down = defaultdict(set)
for pid in nodes:
    for val in nodes[pid].get("why", []):
        for tok in ref_tok.findall(val):
            down[tok].add(pid)

def reachable_down(roots):
    seen = set()
    frontier = list(roots)
    while frontier:
        n = frontier.pop()
        if n in seen:
            continue
        seen.add(n)
        frontier.extend(down.get(n, ()))
    return seen

rootsA1 = set(problems)
rootsA2 = set(problems) | set(wr)
reachA1 = reachable_up(rootsA1)
reachA2 = reachable_up(rootsA2)
reachB1 = reachable_down(rootsA1)
reachB2 = reachable_down(rootsA2)

carrying_why = sorted(n for n in nodes if "why" in nodes[n])
carrying_how = sorted(n for n in nodes if "how" in nodes[n])
carrying_why_posthoc = sorted(n for n in nodes if "why-posthoc" in nodes[n])

# grep floor
grep_cmd = "grep -rlE '@(why|how)' --include='*.flexiarg' /home/joe/code/futon3/library"
floor = subprocess.run(grep_cmd, shell=True, capture_output=True, text=True)
floor_n = len([l for l in floor.stdout.splitlines() if l.strip()])

# git sha of futon3 at census time
sha = subprocess.run("git -C /home/joe/code/futon3 rev-parse HEAD",
                     shell=True, capture_output=True, text=True).stdout.strip()

with open(os.path.join(OUT, "L1-census-graph.edn"), "w") as f:
    f.write(";; L1 census graph -- nodes+edges EDN. Sorted; deterministic.\n")
    f.write("{:nodes [\n")
    for n in sorted(nodes):
        f.write("  %s\n" % q(n))
    f.write("]\n :edges [\n")
    for (a, k, b, res) in sorted(edges):
        f.write("  {:from %s :kind %s :to %s :resolved %s}\n"
                % (q(a), kw(k), q(b), ("true" if res else "false")))
    f.write("]}\n")

with open(os.path.join(OUT, "L1-census-receipt.edn"), "w") as f:
    f.write(";; L1 RATIONALE-REACHABILITY CENSUS receipt. Deterministic rerun.\n")
    f.write("{:row :L1\n :library-git-sha %s\n" % q(sha))
    f.write(" :files-parsed %d\n" % files)
    f.write(" :files-missing-flexiarg-id %s\n" % (str(sorted(files_missing_flexiarg)).replace("'", '"')))
    f.write(" :patterns-total %d\n" % len(nodes))
    f.write(" :carrying-why %d\n" % len(carrying_why))
    f.write(" :carrying-why-posthoc-only %d\n"
            % len([n for n in carrying_why_posthoc if n not in carrying_why]))
    f.write(" :carrying-how %d\n" % len(carrying_how))
    f.write(" :carrying-why-or-how-unique %d\n"
            % len(set(carrying_why) | set(carrying_how)))
    f.write(" :grep-floor-command %s\n" % q("cd /home/joe/code/futon3/library && " + grep_cmd))
    f.write(" :grep-floor-files %d\n" % floor_n)
    f.write(" :problem-nodes %d %s\n" % (len(problems), str(problems).replace("'", '"')))
    f.write(" :wr-nodes %d\n" % len(wr))
    f.write(" :why-reachable-up-from-problems %d\n" % len(reachA1))
    f.write(" :why-reachable-up-from-problems+wr %d\n" % len(reachA2))
    f.write(" :why-reachable-down-from-problems %d\n" % len(reachB1))
    f.write(" :why-reachable-down-from-problems+wr %d\n" % len(reachB2))
    f.write(" :edges-total %d\n" % len(edges))
    f.write(" :edges-resolved %d\n" % len([e for e in edges if e[3]]))
    f.write(" :edges-unresolved %d\n" % len([e for e in edges if not e[3]]))
    f.write(" :annotation-keys [\n")
    for k in sorted(ann_hist):
        f.write("  {:key %s :count %d}\n" % (kw(k), ann_hist[k]))
    f.write(" ]\n :unresolved-refs [\n")
    for (a, k, b) in sorted(unresolved):
        f.write("  {:from %s :kind %s :to %s}\n" % (q(a), kw(k), q(b)))
    f.write(" ]}\n")

print("nodes=%d edges=%d why=%d how=%d floor=%d reachUpProb=%d reachUpProbWR=%d reachDnProb=%d reachDnProbWR=%d"
      % (len(nodes), len(edges), len(carrying_why), len(carrying_how), floor_n,
         len(reachA1), len(reachA2), len(reachB1), len(reachB2)))
