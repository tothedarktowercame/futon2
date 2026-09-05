#!/usr/bin/env python3
"""L2/L5 flexiarg parse gate.

Usage: l2_parse_gate.py [SECTION] [LIB]
Default section "process" (L2). Section "problems" (L5) checks the same
structure but requires an @holds-at-or-source pointer instead of the
conventions-note citation. Exit 1 on any failure.
"""
import os, re, sys

SEC = sys.argv[1] if len(sys.argv) > 1 else "process"
LIB = sys.argv[2] if len(sys.argv) > 2 else "/home/joe/code/futon3/library"
REQ = ["context", "if", "however", "then", "because"]
# Pre-existing problems/* nodes minted by another lane before this gate
# existed (L5 acceptance gates the L5-minted files; legacy files are left
# untouched per the row's path-scope rule).
LEGACY = {
    "problems/commitment-temperature-is-instrumented-as-gain",
    "problems/operator-turns-become-inference-observations",
    "problems/per-tick-mismatch-instruments-outer-loop-gain",
    "problems/refusal-prediction-error-v1--source-field-missing",
    "problems/satisfied-rungs-are-counted-and-surfaced",
    "problems/tension-proposes-candidates",
}
fails = []
n = 0
d = os.path.join(LIB, SEC)
for fn in sorted(os.listdir(d)):
    if not fn.endswith(".flexiarg"):
        continue
    n += 1
    pid = SEC + "/" + fn[:-len(".flexiarg")]
    if pid in LEGACY:
        continue
    # Backfill rows (L6+) check the backfill bar: id match, a conclusion
    # node, authored @why and @how, and a source pointer. The full
    # five-component structure is the L2/L5 mint bar; pre-existing
    # structural gaps in backfill sections are a census finding, not
    # something this gate fails on (repairing them would restructure
    # patterns, outside a backfill row's scope).
    BACKFILL_SECTIONS = {"war-room", "cascades", "futon-theory",
                         "ukrns", "snatch", "vsatlas", "storage",
                         "math-formalization", "math-formalization-CA",
                         "math-informal", "math-strategy"}
    lines = open(os.path.join(d, fn), encoding="utf-8").read().splitlines()
    meta = {}
    nodes = []
    for ln in lines:
        m = re.match(r"^@([A-Za-z0-9_-]+)\s+(.*)$", ln)
        if m:
            meta.setdefault(m.group(1), []).append(m.group(2).strip())
            continue
        m = re.match(r"^(\s*)([!+?])\s*([^:]+):\s*(.*)$", ln)
        if m:
            nodes.append((len(m.group(1).expandtabs(2)), m.group(2),
                          m.group(3).strip().lower()))
    # top level = smallest indent per marker (README §1: conclusion at 0,
    # the five components at the first `+` indent)
    plus_indents = [i for (i, mk, _) in nodes if mk == "+"]
    top_plus = min(plus_indents) if plus_indents else 0
    body_top = [(mk, lbl) for (i, mk, lbl) in nodes
                if i == top_plus or (mk == "!" and i == 0)]
    if meta.get("flexiarg", [None])[0] != pid:
        fails.append((pid, "@flexiarg id mismatch"))
    concl = [b for b in body_top if b[0] == "!" and "conclusion" in b[1]]
    if SEC in BACKFILL_SECTIONS:
        if len(concl) != 1:
            fails.append((pid, "expected exactly one ! conclusion:, got %d" % len(concl)))
    else:
        if len(concl) != 1:
            fails.append((pid, "expected exactly one ! conclusion:, got %d" % len(concl)))
        comps = {b[1].split("(")[0].strip() for b in body_top if b[0] == "+"}
        for r in REQ:
            if r not in comps:
                fails.append((pid, "missing required component +%s:" % r.upper()))
    for k in ("why", "how"):
        if k not in meta:
            fails.append((pid, "missing @%s" % k))
    text = "\n".join(lines)
    if SEC == "process":
        if "N-process-trap-recording-conventions" not in text:
            fails.append((pid, "no conventions-note receipt citation"))
        if "P-assured-process" not in text:
            fails.append((pid, "no P-assured-process problem naming"))
    elif SEC in BACKFILL_SECTIONS or SEC == "problems":
        if not (re.search(r"^@(source|why|how) .*source:", text, re.M)
                or "@holds-at" in text
                or "source:" in text):
            fails.append((pid, "no source pointer"))
    if re.search(r"^@draft", text, re.M):
        fails.append((pid, "@draft pattern cannot carry @why (AC8)"))
for f in fails:
    print("FAIL %s: %s" % f)
print("l2-parse-gate: %d files, %d failures" % (n, len(fails)))
sys.exit(1 if fails else 0)
