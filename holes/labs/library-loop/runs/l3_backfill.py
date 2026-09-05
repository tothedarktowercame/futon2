#!/usr/bin/env python3
"""L3 RATIONALE BACKFILL PILOT (library/aif only).

Usage: l3_backfill.py [AIF_DIR] [WM_CONTRACT_DIR]
Defaults are the canonical checkouts; both roots are explicit arguments so
the pilot reruns against isolated sibling worktrees, as L1 does.

For every aif pattern: add a dossier-derived @why (from the matching
twenty-node problem dossier statement in futon2 PROBLEMS-*.md, first sentence
quoted in full) and an own-mechanism @how (from the pattern's ! conclusion,
untruncated). Where no dossier node matches, the @why annotation SAYS SO and
carries a source pointer to the committed negative-claim check
(runs/l3_no_dossier_check.py -> runs/L3-no-dossier-check.edn).
Every annotation carries a source pointer. Touches only the given AIF_DIR.
"""
import os, re, sys

AIF = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library/aif")
WM = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                     else "/home/joe/code/futon2/holes/labs/wm-contract")
BATCH = {"R1": "PROBLEMS-r1-r3-r3a-batch3.md", "R3": "PROBLEMS-r1-r3-r3a-batch3.md",
         "R3a": "PROBLEMS-r1-r3-r3a-batch3.md",
         "R2": "PROBLEMS-r2-r8-r7-batch2.md", "R8": "PROBLEMS-r2-r8-r7-batch2.md",
         "R7": "PROBLEMS-r2-r8-r7-batch2.md",
         "R4": "PROBLEMS-r4-r5-r6-pilot.md", "R5": "PROBLEMS-r4-r5-r6-pilot.md",
         "R6": "PROBLEMS-r4-r5-r6-pilot.md",
         "R15": "PROBLEMS-r15-r11-r17-batch5.md", "R11": "PROBLEMS-r15-r11-r17-batch5.md",
         "R17": "PROBLEMS-r15-r11-r17-batch5.md",
         "R16": "PROBLEMS-r16-r13-r14-batch4.md", "R13": "PROBLEMS-r16-r13-r14-batch4.md",
         "R14": "PROBLEMS-r16-r13-r14-batch4.md",
         "R9": "PROBLEMS-assurance-band-batch6.md", "R10": "PROBLEMS-assurance-band-batch6.md",
         "R12": "PROBLEMS-assurance-band-batch6.md", "R20": "PROBLEMS-assurance-band-batch6.md"}

def dossier_problem(node):
    path = os.path.join(WM, BATCH[node])
    txt = open(path, encoding="utf-8").read()
    m = re.search(r"^## %s —.*?(?=^## )" % re.escape(node), txt, re.M | re.S)
    if not m:
        return None, path
    sec = m.group(0)
    p = re.search(r"### The problem it solves\s*\n+(.*)", sec, re.S)
    if not p:
        return None, path
    body = " ".join(p.group(1).split())
    first = re.split(r"(?<=[.!?]) ", body)[0]
    return first, path  # full first sentence, untruncated

def first_conclusion(lines):
    for ln in lines:
        m = re.match(r"^!\s+conclusion:\s*(.*)$", ln)
        if m:
            return m.group(1).strip()  # untruncated
    return ""

changed = []
for fn in sorted(os.listdir(AIF)):
    if not fn.endswith(".flexiarg"):
        continue
    path = os.path.join(AIF, fn)
    lines = open(path, encoding="utf-8").read().splitlines()
    if any("L3 rationale-backfill pilot" in l for l in lines):
        continue  # already backfilled (idempotent rerun)
    holds = None
    for ln in lines:
        m = re.match(r"^@holds-at\s+(R[A-Za-z0-9]+)", ln)
        if m:
            holds = m.group(1)
            break
    gist = None
    if holds and holds in BATCH:
        gist, dpath = dossier_problem(holds)
        if gist:
            why = ("@why %s dossier problem: %s (source: futon2 holes labs wm-contract %s, %s, The problem it solves; L3 rationale-backfill pilot, zai-1, 2026-09-05)"
                   % (holds, gist, os.path.basename(dpath), holds))
        else:
            why = ("@why no dossier statement extracted for %s in %s; rationale not invented (L3 rationale-backfill pilot, zai-1, 2026-09-05)"
                   % (holds, os.path.basename(dpath)))
    else:
        why = ("@why no matching problem dossier node (@holds-at %s; not among the twenty dossier nodes R1-R17, R20, TRACE). Negative claim per the committed check futon2 holes labs library-loop runs l3_no_dossier_check.py, receipt runs L3-no-dossier-check.edn (enumerates every aif pattern's @holds-at against the dossier node set extracted from PROBLEMS-*.md headings). Rationale not invented (L3 rationale-backfill pilot, zai-1, 2026-09-05)"
               % (holds if holds else "absent"))
    c = first_conclusion(lines)
    mech = None
    if c:
        mech = ("@how own mechanism: %s (source: this pattern's ! conclusion line; L3 rationale-backfill pilot, zai-1, 2026-09-05)" % c)
    body = next((i for i, ln in enumerate(lines)
                 if re.match(r"^\s*[!+?]\s*[A-Za-z]", ln)), len(lines))
    last_meta = max([i for i, ln in enumerate(lines[:body])
                     if ln.startswith("@")], default=0)
    new = lines[:last_meta + 1] + [why] + ([mech] if mech else []) + lines[last_meta + 1:]
    open(path, "w", encoding="utf-8").write("\n".join(new) + "\n")
    changed.append((fn, holds, "dossier" if gist else "no-dossier"))
for c in changed:
    print(c[0], "|", c[1], "|", c[2])
print("l3-backfill: %d files annotated" % len(changed))
