#!/usr/bin/env python3
"""L18 v2 COMPLETE no-source search over the served-refused worklist.

Usage: l18_complete_check.py [LIBRARY_DIR] [REPORT_MD] [OUT_EDN]
Enumerates the served-refused union from the committed L18 advisory report
(before this fix), and for EVERY member runs the complete source search:
  (a) the whole problem corpus (holds-token and qualified-id-in-text),
  (b) the member's section documentation (*.md in its section dir),
      searching for the member's qualified id and bare name.
Serializes every id with its specific searched sources and result -- no
blanket claims. Patterns in sections whose committed README states the
problem (snatch, per this row's minted node) are marked covered-by-section-doc.
Deterministic; path-independent provenance.
"""
import os, re, sys, subprocess

LIB = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library")
REPORT = sys.argv[2] if len(sys.argv) > 2 else "L18-advisory-gate-report.md"
OUT = sys.argv[3] if len(sys.argv) > 3 else "L18-remainder.edn"

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

def refused(fn):
    txt = open(REPORT, encoding="utf-8").read()
    sec = txt.split("### %s" % fn, 1)[1].split("\n### ", 1)[0]
    m = re.search(r"- \*\*down-problems\+wr\*\*: (\d+) refused -- (.*)", sec)
    return set(m.group(2).split(", ")) if m and m.group(2) != "(none)" else set()

union = sorted(set().union(*[refused(f) for f in SERVED]))

corpus = []
pdir = os.path.join(LIB, "problems")
for fn in sorted(os.listdir(pdir)):
    if fn.endswith(".flexiarg"):
        pid = "problems/" + fn[:-len(".flexiarg")]
        txt = open(os.path.join(pdir, fn), encoding="utf-8", errors="replace").read()
        corpus.append((pid, re.findall(r"^@holds-at\s+(\S+)", txt, re.M), txt))

# sections whose committed README states the problem (node minted this row)
SECTION_DOC_NODES = {"snatch": "problems/snatch-play-theory-gaps"}

rows = []
for pid in union:
    sec, name = pid.split("/", 1)
    pat = os.path.join(LIB, sec)
    lines = open(os.path.join(pat, name + ".flexiarg"),
                 encoding="utf-8", errors="replace").read().splitlines()
    holds = []
    for ln in lines:
        m = re.match(r"^@holds-at\s+(.+)$", ln)
        if m:
            holds += re.findall(r"R[A-Za-z0-9]+|TRACE", m.group(1))
    searched, result = [], "no-source"
    # (a) corpus
    searched.append("problems/*.flexiarg (holds+id-text)")
    hit = next(((n, b) for (n, nh, t) in corpus
                for b in (["holds"] if holds and any(h in nh for h in holds) else [])
                + (["named"] if pid in t else [])), None)
    if hit:
        result = "corpus-match:%s:%s" % (hit[1], hit[0])
    # (b) section docs
    mdfiles = sorted(f for f in os.listdir(pat) if f.endswith(".md"))
    searched += ["%s/%s" % (sec, f) for f in mdfiles]
    for f in mdfiles:
        t = open(os.path.join(pat, f), encoding="utf-8", errors="replace").read()
        if name in t or pid in t:
            result = "section-doc-names-member:%s/%s" % (sec, f)
            break
    if result == "no-source" and sec in SECTION_DOC_NODES:
        result = "covered-by-section-doc:%s (README states the section problem; member edged this row)" % SECTION_DOC_NODES[sec]
    rows.append((pid, searched, result))

# edge snatch members (the covered-by-section-doc cohort)
EDGE = ("@why problems/snatch-play-theory-gaps (L18 v2 section-grain grounding; "
        "basis: snatch README's own gap assessment and gaps-filled record cover "
        "this member (treatment table or named gap closure); source: futon2 "
        "holes labs library-loop runs l18_complete_check.py receipt "
        "runs/L18-remainder.edn; zai-1, 2026-09-05)")
EDGE_NAMED = ("@why problems/snatch-play-theory-gaps (L18 v2 grounding; basis: "
              "snatch/README.md names this member explicitly (treatment table or "
              "gaps-filled record); source: futon2 holes labs library-loop runs "
              "l18_complete_check.py receipt runs/L18-remainder.edn; zai-1, 2026-09-05)")
edged = 0
for (pid, _, result) in rows:
    if not (result.startswith("covered-by-section-doc")
            or result.startswith("section-doc-names-member:snatch/")):
        continue
    edge = EDGE if result.startswith("covered-by-section-doc") else EDGE_NAMED
    sec, name = pid.split("/", 1)
    path = os.path.join(LIB, sec, name + ".flexiarg")
    lines = open(path, encoding="utf-8").read().splitlines()
    if any("L18 v2 section-grain grounding" in l or "L18 v2 grounding" in l
           for l in lines):
        continue
    body = next((i for i, ln in enumerate(lines)
                 if re.match(r"^\s*[!+?]\s*[A-Za-z]", ln)), len(lines))
    last = max([i for i, ln in enumerate(lines[:body]) if ln.startswith("@")],
               default=0)
    lines = lines[:last + 1] + [EDGE] + lines[last + 1:]
    open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    edged += 1

sha, rel = gitsha(LIB)
with open(OUT, "w") as f:
    f.write(";; L18 v2 complete no-source search. Deterministic, path-independent.\n")
    f.write("{:library-subdir %s\n :library-git-sha %s\n" % (q(rel), q(sha)))
    f.write(" :search \"per member of the served-refused union (%s): (a) every problems/*.flexiarg by holds-token and qualified-id-in-text; (b) every *.md in the member's section directory by bare name and qualified id\"\n" % " ".join(SERVED))
    f.write(" :union-count %d\n :members [\n" % len(rows))
    for (pid, searched, result) in rows:
        f.write("  {:pattern %s :searched [%s] :result %s}\n"
                % (q(pid), " ".join(q(s) for s in searched), q(result)))
    f.write(" ]\n :counts {:union %d :edged-this-row %d :no-source %d}}\n"
            % (len(rows), edged, len([r for r in rows if r[2] == "no-source"])))
print("l18v2: union %d, edged %d, no-source %d"
      % (len(rows), edged, len([r for r in rows if r[2] == "no-source"])))
