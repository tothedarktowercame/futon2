#!/usr/bin/env python3
"""L14 EDGE-RESOLUTION PASS over the backfilled sections.

Usage: l14_resolve.py SECTIONS_CSV [LIBRARY_DIR]
Re-matches every pattern in the given sections (L3's aif plus L6-L10's) against
the current 32-node problem corpus. Where a match exists and the pattern does
not already carry an @why edge to that node, adds the resolvable edge in the
syntax l1_census.py reads. NO invented matches: the only two admissible bases
are (holds) the pattern's @holds-at token equals the problem node's own
@holds-at token, and (named) the problem node's own text names the pattern's
qualified id. Unmatched patterns are left alone and counted with a reason.
Idempotent via the L14 marker. Receipt: runs/L14-edge-resolution.edn.
"""
import os, re, sys, subprocess

SECTIONS = [s.strip() for s in (sys.argv[1] if len(sys.argv) > 1
                                else "aif").split(",")]
LIB = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                      else "/home/joe/code/futon3/library")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   (os.environ.get("L14_RECEIPT") or "L14-edge-resolution.edn"))

def gitsha(d):
    repo = subprocess.run("git -C %s rev-parse --show-toplevel" % d,
                          shell=True, capture_output=True, text=True).stdout.strip()
    return (subprocess.run("git -C %s rev-parse HEAD" % repo, shell=True,
                           capture_output=True, text=True).stdout.strip(),
            os.path.relpath(d, repo))

corpus = []
pdir = os.path.join(LIB, "problems")
for fn in sorted(os.listdir(pdir)):
    if fn.endswith(".flexiarg"):
        pid = "problems/" + fn[:-len(".flexiarg")]
        txt = open(os.path.join(pdir, fn), encoding="utf-8", errors="replace").read()
        holds = re.findall(r"^@holds-at\s+(\S+)", txt, re.M)
        corpus.append((pid, holds, txt))

def matches(pid, holds):
    out = []
    for (nid, nholds, ntxt) in corpus:
        if holds and any(h in nholds for h in holds):
            out.append((nid, "holds"))
        elif pid in ntxt:
            out.append((nid, "named"))
    return out

ref_tok = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9._-]+)+")

def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

rows, added, already, unmatched = [], [], [], []
for sec in SECTIONS:
    d = os.path.join(LIB, sec)
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".flexiarg"):
            continue
        pid = sec + "/" + fn[:-len(".flexiarg")]
        path = os.path.join(d, fn)
        lines = open(path, encoding="utf-8").read().splitlines()
        holds = re.findall(r"^@holds-at\s+(R[A-Za-z0-9]+|TRACE)", "\n".join(lines), re.M)
        holds = [h for h in holds]  # findall already takes every token on the line
        # multi-token @holds-at lines: capture every R-token/TRACE on the line
        for ln in lines:
            m = re.match(r"^@holds-at\s+(.+)$", ln)
            if m:
                for tok in re.findall(r"R[A-Za-z0-9]+|TRACE", m.group(1)):
                    if tok not in holds:
                        holds.append(tok)
        existing = set()
        for ln in lines:
            m = re.match(r"^@why\s+(.*)$", ln)
            if m:
                existing |= {t for t in ref_tok.findall(m.group(1)) if t.startswith("problems/")}
        ms = matches(pid, holds)
        new_edges = [(n, b) for (n, b) in ms if n not in existing]
        if new_edges:  # idempotence via the existing-edge check above
            add = ["@why %s (L14 edge-resolution; basis: %s -- the problem node's own %s covers this pattern; source: receipt runs/L14-edge-resolution.edn; zai-1, 2026-09-05)"
                   % (n, b, "holds-at token" if b == "holds" else "text names the pattern id")
                   for (n, b) in new_edges]
            body = next((i for i, ln in enumerate(lines)
                         if re.match(r"^\s*[!+?]\s*[A-Za-z]", ln)), len(lines))
            last_meta = max([i for i, ln in enumerate(lines[:body])
                             if ln.startswith("@")], default=0)
            lines = lines[:last_meta + 1] + add + lines[last_meta + 1:]
            open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")
            added.append((pid, new_edges))
        if ms:
            already.extend((pid, n, b) for (n, b) in ms if n in existing)
        if not ms:
            unmatched.append((pid, "no-holds-and-not-named" if not holds
                              else "holds-token-without-node-and-not-named"))
        rows.append((pid, holds, existing, new_edges))

sha, rel = gitsha(LIB)
with open(OUT, "w") as f:
    f.write(";; L14 edge-resolution receipt. Deterministic, path-independent.\n")
    f.write("{:library-subdir %s\n :library-git-sha %s\n" % (q(rel), q(sha)))
    f.write(" :bases \"holds: pattern @holds-at token equals problem node @holds-at token; named: problem node text contains the pattern's qualified id. No other basis admitted.\"\n")
    f.write(" :sections [%s]\n" % " ".join(q(s) for s in SECTIONS))
    f.write(" :added-edges [\n")
    for (pid, es) in added:
        f.write("  {:pattern %s :to [%s]}\n"
                % (q(pid), " ".join("[%s %s]" % (q(n), q(b)) for (n, b) in es)))
    f.write(" ]\n :already-edged %d\n :unmatched [\n" % len(already))
    for (pid, reason) in unmatched:
        f.write("  {:pattern %s :reason %s}\n" % (q(pid), q(reason)))
    f.write(" ]\n :counts {:patterns %d :edges-added %d :already-edged %d :unmatched %d}}\n"
            % (len(rows), sum(len(e) for (_, e) in added), len(already), len(unmatched)))
print("l14: %d patterns, %d edges added, %d already, %d unmatched"
      % (len(rows), sum(len(e) for (_, e) in added), len(already), len(unmatched)))
