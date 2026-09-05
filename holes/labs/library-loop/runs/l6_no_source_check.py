#!/usr/bin/env python3
"""L6+ negative-claim check: no-source @why annotations are reproducible.

Usage: l6_no_source_check.py SECTIONS_CSV [LIBRARY_DIR] [OUT_EDN]

Complete source corpus: EVERY library/problems/*.flexiarg node (the six
legacy nodes, the twenty L5 dossier nodes including TRACE, and the six L5
record/ruling nodes), each enumerated with its own @holds-at tokens (taken
from the file, not from filenames).

Match rule, per pattern in the given sections (the match the annotation
wording claims):
  (a) holds-match: the pattern's @holds-at token equals one of the node's
      @holds-at tokens;
  (b) named-match: the pattern's qualified id appears in the node's text.
A pattern with either match has a source; a pattern with neither is the
negative claim. Receipt is the source pointer cited by every no-source
@why annotation. Path-independent provenance; deterministic.
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

# corpus: every problems node with its holds-at tokens and full text
corpus = []
pdir = os.path.join(LIB, "problems")
for fn in sorted(os.listdir(pdir)):
    if not fn.endswith(".flexiarg"):
        continue
    pid = "problems/" + fn[:-len(".flexiarg")]
    txt = open(os.path.join(pdir, fn), encoding="utf-8", errors="replace").read()
    holds = re.findall(r"^@holds-at\s+(\S+)", txt, re.M)
    corpus.append((pid, holds, txt))

def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

rows = []
for sec in SECTIONS:
    d = os.path.join(LIB, sec)
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".flexiarg"):
            continue
        pid = sec + "/" + fn[:-len(".flexiarg")]
        txt = open(os.path.join(d, fn), encoding="utf-8", errors="replace").read()
        holds = re.findall(r"^@holds-at\s+(R[A-Za-z0-9]+|TRACE)", txt, re.M)
        match = None
        for (nid, nholds, ntxt) in corpus:
            if (holds and any(h in nholds for h in holds)) or (pid in ntxt):
                match = (nid,
                         "holds" if (holds and any(h in nholds for h in holds)) else "named")
                break
        rows.append((pid, holds, match))

sha, rel = gitsha(LIB)
with open(OUT, "w") as f:
    f.write(";; L6+ no-source check receipt. Deterministic, path-independent rerun.\n")
    f.write("{:library-subdir %s\n :library-git-sha %s\n" % (q(rel), q(sha)))
    f.write(" :search \"corpus = every library/problems/*.flexiarg (legacy 6 + L5 dossier 20 incl. TRACE + L5 record/ruling 6), each with its own @holds-at tokens read from the file; per pattern in scope: (a) holds-token equality against every corpus node, (b) qualified pattern id searched in every corpus node's full text\"\n")
    f.write(" :corpus-nodes [\n")
    for (pid, holds, _t) in corpus:
        f.write("  {:node %s :holds-at [%s]}\n" % (q(pid), " ".join(q(h) for h in holds)))
    f.write(" ]\n :patterns [\n")
    for (pid, holds, match) in rows:
        f.write("  {:pattern %s :holds-at [%s] :match %s}\n"
                % (q(pid), " ".join(q(h) for h in holds),
                   ("[%s %s]" % (q(match[0]), q(match[1]))) if match else "nil"))
    f.write(" ]\n :counts {:patterns %d :matched %d :no-match %d}}\n"
            % (len(rows), len([r for r in rows if r[2]]),
               len([r for r in rows if not r[2]])))
print("l6-no-source-check: corpus=%d nodes, %d patterns, %d matched, %d no-match"
      % (len(corpus), len(rows), len([r for r in rows if r[2]]),
         len([r for r in rows if not r[2]])))
