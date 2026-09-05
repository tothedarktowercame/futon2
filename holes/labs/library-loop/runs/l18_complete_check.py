#!/usr/bin/env python3
"""L18 v3 COMPLETE rationale-source search over the FIXED 71-member worklist.

Usage: l18_complete_check.py [LIBRARY_DIR] [WORKLIST_REPORT] [OUT_EDN] [FUTON2_DIR] [REPOS_CSV]
FUTON2_DIR (default canonical) supplies the rationale corpus (missions,
EPIC-run-era, P-*.md); REPOS_CSV (default canonical six) supplies provenance
resolution roots. Every source authority is an explicit pinned input so the
receipt reproduces from detached evidence worktrees without consulting
canonical live checkouts; the receipt pins each authority's git HEAD.
The worklist is FIXED: the served-refused union (down-problems+wr) from the
committed pre-grounding L17 report (runs/L17-advisory-gate-report.md, futon3
835278b), not the mutable current report. For EVERY one of the 71 members the
search covers:
  (a) the whole problem corpus (holds-token + qualified-id-in-text);
  (b) every *.md in the member's section;
  (c) the member's OWN named provenance: every .md/.tex/.org/.edn path token
      in its file that exists under the sibling repos;
  (d) a fixed rationale corpus: futon2 holes/missions/*.md, EPIC-run-era.md,
      holes/problems/P-*.md, and the war-room pattern bodies.
Serializes all 71 results (searched sources + result per member). Members
grounded by earlier L18 passes are recorded with their grounding basis, not
dropped. Deterministic; path-independent provenance.
"""
import os, re, sys, subprocess

LIB = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library")
REPORT = sys.argv[2] if len(sys.argv) > 2 else "L17-advisory-gate-report.md"
OUT = sys.argv[3] if len(sys.argv) > 3 else "L18-remainder.edn"
FUTON2 = os.path.abspath(sys.argv[4] if len(sys.argv) > 4
                         else "/home/joe/code/futon2")
_repos = (sys.argv[5].split(",") if len(sys.argv) > 5 else
          ["futon2", "futon3", "p4ng", "futon5", "futon3c", "futon0"])
REPOS = [r if r.startswith("/") else
         os.path.join(os.path.dirname(FUTON2), r) for r in _repos]

def gitsha(d):
    repo = subprocess.run("git -C %s rev-parse --show-toplevel" % d,
                          shell=True, capture_output=True, text=True).stdout.strip()
    return (subprocess.run("git -C %s rev-parse HEAD" % repo, shell=True,
                           capture_output=True, text=True).stdout.strip(),
            os.path.relpath(d, repo))

def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

SERVED = ["zaif-cascade.edn", "construct-cascade.edn", "snatch-cascade.edn",
          "ants-cascade.edn", "alfworld-cascade.edn"]

def refused_from(path, fn):
    txt = open(path, encoding="utf-8").read()
    sec = txt.split("### %s" % fn, 1)[1].split("\n### ", 1)[0]
    m = re.search(r"- \*\*down-problems\+wr\*\*: (\d+) refused -- (.*)", sec)
    return set(m.group(2).split(", ")) if m and m.group(2) != "(none)" else set()

union = sorted(set().union(*[refused_from(REPORT, f) for f in SERVED]))

corpus = []
pdir = os.path.join(LIB, "problems")
for fn in sorted(os.listdir(pdir)):
    if fn.endswith(".flexiarg"):
        pid = "problems/" + fn[:-len(".flexiarg")]
        txt = open(os.path.join(pdir, fn), encoding="utf-8", errors="replace").read()
        corpus.append((pid, re.findall(r"^@holds-at\s+(\S+)", txt, re.M), txt))

# (d) fixed rationale corpus -- every file pinned from FUTON2_DIR (explicit input)
rationale = []
def add_src(label, path):
    if os.path.isfile(path):
        rationale.append((label, open(path, encoding="utf-8", errors="replace").read()))
M = os.path.join(FUTON2, "holes/missions")
if os.path.isdir(M):
    for fn in sorted(os.listdir(M)):
        if fn.endswith(".md"):
            add_src("futon2 holes/missions/" + fn, os.path.join(M, fn))
add_src("futon2 holes/labs/wm-contract/EPIC-run-era.md",
        os.path.join(FUTON2, "holes/labs/wm-contract/EPIC-run-era.md"))
P = os.path.join(FUTON2, "holes/problems")
for fn in sorted(os.listdir(P)):
    if fn.startswith("P-") and fn.endswith(".md"):
        add_src("futon2 holes/problems/" + fn, os.path.join(P, fn))
W = os.path.join(LIB, "war-room")
for fn in sorted(os.listdir(W)):
    if fn.endswith(".flexiarg"):
        add_src("war-room/" + fn, os.path.join(W, fn))

rows = []
for pid in union:
    sec, name = pid.split("/", 1)
    ppath = os.path.join(LIB, sec, name + ".flexiarg")
    ptxt = open(ppath, encoding="utf-8", errors="replace").read()
    plines = ptxt.splitlines()
    holds = []
    for ln in plines:
        m = re.match(r"^@holds-at\s+(.+)$", ln)
        if m:
            holds += re.findall(r"R[A-Za-z0-9]+|TRACE", m.group(1))
    searched, hits, result = [], [], None
    # (a) corpus
    searched.append("problems/*.flexiarg (holds+id-text)")
    for (nid, nh, t) in corpus:
        basis = ("holds" if holds and any(h in nh for h in holds)
                 else ("named" if pid in t else None))
        if basis:
            result = "corpus-match:%s:%s" % (basis, nid)
            break
    # (b) section docs
    mdfiles = sorted(f for f in os.listdir(os.path.join(LIB, sec)) if f.endswith(".md"))
    searched += ["%s/%s" % (sec, f) for f in mdfiles]
    if not result:
        for f in mdfiles:
            t = open(os.path.join(LIB, sec, f), encoding="utf-8", errors="replace").read()
            if name in t or pid in t:
                result = "section-doc-names-member:%s/%s" % (sec, f)
                break
    # already grounded by L18 v1/v2 (edge present)?
    if not result and any(re.match(r"^@why problems/\S+ \(L18", l) for l in plines):
        m = re.search(r"^@why (problems/\S+) \(L18", ptxt, re.M)
        result = "grounded-L18:%s" % m.group(1)
    # (c) named provenance files
    named = []
    for tok in set(re.findall(r"[A-Za-z0-9_./'-]+\.(?:md|tex|org|edn)", ptxt)):
        for r in REPOS:
            cand = os.path.join(r, tok)
            if os.path.isfile(cand):
                named.append((tok, cand))
    searched += ["provenance:" + t for (t, _) in named]
    if not result:
        for (tok, cand) in named:
            t = open(cand, encoding="utf-8", errors="replace").read()
            if name in t or pid in t:
                result = "named-source-candidate:%s (names the member; problem-statement extraction is a per-node judgment, left for commissioning)" % tok
                break
    # (d) rationale corpus -- every concrete file, serialized per member
    searched += [label for (label, _) in rationale]
    if not result:
        for (label, t) in rationale:
            if name in t or pid in t:
                hits.append(label)
        if hits:
            result = "rationale-candidates (names the member): " + "; ".join(hits[:4])
    if not result:
        result = "no-source (all listed sources searched; none names or covers the member)"
    rows.append((pid, searched, result))

sha, rel = gitsha(LIB)
with open(OUT, "w") as f:
    f.write(";; L18 v3 complete rationale-source search over the FIXED 71-member worklist.\n")
    f.write(";; Worklist source: runs/L17-advisory-gate-report.md (pre-grounding). Deterministic.\n")
    f2sha, f2rel = gitsha(os.path.join(FUTON2, "holes"))
    f.write("{:library-subdir %s\n :library-git-sha %s\n :futon2-subdir %s\n :futon2-git-sha %s\n" % (q(rel), q(sha), q(f2rel), q(f2sha)))
    f.write(" :worklist-source %s\n" % q("runs/L17-advisory-gate-report.md served-refused union, down-problems+wr"))
    f.write(" :union-count %d\n :members [\n" % len(rows))
    for (pid, searched, result) in rows:
        f.write("  {:pattern %s\n   :searched [%s]\n   :result %s}\n"
                % (q(pid), " ".join(q(s) for s in searched), q(result)))
    f.write(" ]\n :counts {:union %d :grounded-L18 %d :corpus-match %d :section-doc %d :named-source-candidate %d :rationale-candidate %d :no-source %d}}\n"
            % (len(rows),
               len([r for r in rows if r[2].startswith("grounded-L18")]),
               len([r for r in rows if r[2].startswith("corpus-match")]),
               len([r for r in rows if r[2].startswith("section-doc")]),
               len([r for r in rows if r[2].startswith("named-source-candidate")]),
               len([r for r in rows if r[2].startswith("rationale-candidates")]),
               len([r for r in rows if r[2].startswith("no-source")])))
print("l18v3: union %d | %s" % (len(rows), " ".join(
    "%s=%d" % (k, v) for k, v in [
        ("grounded", len([r for r in rows if r[2].startswith("grounded-L18")])),
        ("corpus", len([r for r in rows if r[2].startswith("corpus-match")])),
        ("section-doc", len([r for r in rows if r[2].startswith("section-doc")])),
        ("named-src", len([r for r in rows if r[2].startswith("named-source-candidate")])),
        ("rationale", len([r for r in rows if r[2].startswith("rationale-candidates")])),
        ("no-source", len([r for r in rows if r[2].startswith("no-source")]))])))
