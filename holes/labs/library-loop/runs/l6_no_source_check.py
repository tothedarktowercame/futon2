#!/usr/bin/env python3
"""L6+ negative-claim check: no-source @why annotations are reproducible.

Usage: l6_no_source_check.py SECTIONS_CSV [LIBRARY_DIR] [OUT_EDN]
Enumerates, for every pattern in the given sections, its @holds-at token and
whether an L5 problems/ dossier node exists for it. The receipt is the source
pointer cited by every no-source @why annotation. Path-independent
provenance (repo-relative subdir + git HEAD); deterministic.
"""
import os, re, sys, subprocess

SECTIONS = [s.strip() for s in (sys.argv[1] if len(sys.argv) > 1
                                else "war-room,cascades,futon-theory").split(",")]
LIB = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                      else "/home/joe/code/futon3/library")
OUT = sys.argv[3] if len(sys.argv) > 3 else os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "L6-no-source-check.edn")

def gitsha(d):
    repo = subprocess.run("git -C %s rev-parse --show-toplevel" % d,
                          shell=True, capture_output=True, text=True).stdout.strip()
    return (subprocess.run("git -C %s rev-parse HEAD" % repo, shell=True,
                           capture_output=True, text=True).stdout.strip(),
            os.path.relpath(d, repo))

holds_to_node = {}
for fn in sorted(os.listdir(os.path.join(LIB, "problems"))):
    if fn.endswith(".flexiarg"):
        m = re.match(r"^(r[a-z0-9]+)-", fn)
        if m:
            tok = ("R" + m.group(1)[1:])
            tok = "R3a" if tok == "Ra" else tok
            holds_to_node.setdefault(tok, "problems/" + fn[:-len(".flexiarg")])

def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

rows = []
for sec in SECTIONS:
    d = os.path.join(LIB, sec)
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".flexiarg"):
            continue
        lines = open(os.path.join(d, fn), encoding="utf-8").read().splitlines()
        holds = None
        for ln in lines:
            m = re.match(r"^@holds-at\s+(R[A-Za-z0-9]+|TRACE)", ln)
            if m:
                holds = m.group(1)
                break
        target = holds_to_node.get(holds) if holds else None
        rows.append((sec + "/" + fn[:-len(".flexiarg")], holds, target))

sha, rel = gitsha(LIB)
with open(OUT, "w") as f:
    f.write(";; L6+ no-source check receipt. Deterministic, path-independent rerun.\n")
    f.write("{:library-subdir %s\n :library-git-sha %s\n" % (q(rel), q(sha)))
    f.write(" :search \"every *.flexiarg in the given sections scanned for line-leading @holds-at Rn-or-TRACE tokens; matched against L5 dossier nodes extracted from problems/rN-* filenames\"\n")
    f.write(" :dossier-node-keys [%s]\n" % " ".join(q(k) for k in sorted(holds_to_node)))
    f.write(" :patterns [\n")
    for (pid, holds, target) in rows:
        f.write("  {:pattern %s :holds-at %s :dossier-node %s}\n"
                % (q(pid), q(holds) if holds else "nil", q(target) if target else "nil"))
    f.write(" ]\n :counts {:patterns %d :matched %d :no-match %d}}\n"
            % (len(rows), len([r for r in rows if r[2]]),
               len([r for r in rows if not r[2]])))
print("l6-no-source-check: %d patterns, %d matched, %d no-match"
      % (len(rows), len([r for r in rows if r[2]]), len([r for r in rows if not r[2]])))
