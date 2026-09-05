#!/usr/bin/env python3
"""L3 negative-claim check: no-match @why annotations are reproducible.

Usage: l3_no_dossier_check.py [AIF_DIR] [WM_CONTRACT_DIR] [OUT_EDN]
Enumerates every aif pattern's @holds-at node and the dossier node set
extracted from the PROBLEMS-*.md "## Rn —" headings, and records the full
match/no-match table plus the searches performed. Deterministic (sorted).
Receipt is the source pointer cited by every no-match @why annotation.
"""
import os, re, sys, subprocess

AIF = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library/aif")
WM = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                     else "/home/joe/code/futon2/holes/labs/wm-contract")
OUT = sys.argv[3] if len(sys.argv) > 3 else os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "L3-no-dossier-check.edn")

def gitsha(d):
    repo = subprocess.run("git -C %s rev-parse --show-toplevel" % d,
                          shell=True, capture_output=True, text=True).stdout.strip()
    return (subprocess.run("git -C %s rev-parse HEAD" % repo, shell=True,
                           capture_output=True, text=True).stdout.strip(),
            os.path.relpath(d, repo))

dossier_nodes = sorted(set(re.findall(
    r"^## (R[A-Za-z0-9]+|TRACE) —", "\n".join(
        open(os.path.join(WM, f), encoding="utf-8").read()
        for f in sorted(os.listdir(WM)) if f.startswith("PROBLEMS-")), re.M)))

def q(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

rows = []
for fn in sorted(os.listdir(AIF)):
    if not fn.endswith(".flexiarg"):
        continue
    lines = open(os.path.join(AIF, fn), encoding="utf-8").read().splitlines()
    holds = []
    for ln in lines:
        m = re.match(r"^@holds-at\s+(R[A-Za-z0-9]+|TRACE)", ln)
        if m:
            holds.append(m.group(1))
    matched = [h for h in holds if h in dossier_nodes]
    rows.append((fn, holds, matched))

aif_sha, aif_rel = gitsha(AIF)
wm_sha, wm_rel = gitsha(WM)

with open(OUT, "w") as f:
    f.write(";; L3 negative-claim check receipt. Deterministic, path-independent rerun.\n")
    f.write("{:aif-subdir %s\n :aif-git-sha %s\n :wm-contract-subdir %s\n :wm-contract-git-sha %s\n"
            % (q(aif_rel), q(aif_sha), q(wm_rel), q(wm_sha)))
    f.write(" :search \"every *.flexiarg in aif-root scanned for line-leading @holds-at Rn tokens; dossier node set extracted from ^## Rn - headings across PROBLEMS-*.md in wm-contract-root\"\n")
    f.write(" :dossier-nodes [%s]\n" % " ".join(q(n) for n in dossier_nodes))
    f.write(" :patterns [\n")
    for (fn, holds, matched) in rows:
        f.write("  {:pattern %s :holds-at [%s] :matched [%s] :no-dossier-match %s}\n"
                % (q("aif/" + fn[:-len(".flexiarg")]),
                   " ".join(q(h) for h in holds),
                   " ".join(q(h) for h in matched),
                   ("true" if not matched else "false")))
    f.write(" ]\n :counts {:patterns %d :matched %d :no-dossier-match %d}}\n"
            % (len(rows), len([r for r in rows if r[2]]),
               len([r for r in rows if not r[2]])))
print("l3-no-dossier-check: %d patterns, %d matched, %d no-match; dossier nodes: %s"
      % (len(rows), len([r for r in rows if r[2]]),
         len([r for r in rows if not r[2]]), ",".join(dossier_nodes)))
