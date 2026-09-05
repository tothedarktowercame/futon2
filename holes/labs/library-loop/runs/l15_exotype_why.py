#!/usr/bin/env python3
"""L15 section-grain rationale pass for iiching exotype records.

Usage: l15_exotype_why.py [LIBRARY_DIR]
Adds the shared @why edge (problems/exotype-encoding-programme) to every
iiching/exotype-NNN record. TEMPLATE is excluded (it is a minting-workflow
artifact, not a pattern -- rename it to a non-.flexiarg name so the census
stops counting it). Idempotent via the L15 marker.
"""
import os, re, sys

LIB = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library")
TARGET = "problems/exotype-encoding-programme"
EDGE = ("@why %s (L15 section-grain rationale; basis: this record is one of the "
        "256 exotype encodings whose shared problem that node states, quoting "
        "the section README's goal sentence; source: receipt of the mechanical "
        "pass, futon2 holes labs library-loop runs l15_exotype_why.py; "
        "zai-1, 2026-09-05)" % TARGET)

changed = excluded = 0
d = os.path.join(LIB, "iiching")
for fn in sorted(os.listdir(d)):
    if fn == "TEMPLATE.flexiarg":
        # non-pattern minting template: stop the census counting it
        os.rename(os.path.join(d, fn), os.path.join(d, "TEMPLATE.flexiarg.txt"))
        excluded += 1
        continue
    if not (fn.startswith("exotype-") and fn.endswith(".flexiarg")):
        continue
    path = os.path.join(d, fn)
    lines = open(path, encoding="utf-8").read().splitlines()
    if any("L15 section-grain rationale" in l for l in lines):
        continue
    body = next((i for i, ln in enumerate(lines)
                 if re.match(r"^\s*[!+?]\s*[A-Za-z]", ln)), len(lines))
    last_meta = max([i for i, ln in enumerate(lines[:body])
                     if ln.startswith("@")], default=0)
    lines = lines[:last_meta + 1] + [EDGE] + lines[last_meta + 1:]
    open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    changed += 1
print("l15: %d exotype records edged, %d template excluded+renamed"
      % (changed, excluded))
