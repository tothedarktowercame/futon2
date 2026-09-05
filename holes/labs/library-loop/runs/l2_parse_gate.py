#!/usr/bin/env python3
"""L2 flexiarg parse gate for library/process/*.flexiarg.

Validates README-flexiarg section 1/3 structure: @flexiarg id matches the
file's path, one `! conclusion:` node, and the five required components
(context, IF, HOWEVER, THEN, BECAUSE) as top-level `+` nodes; each file also
carries @why, @how, @receipts-or-evidence citations. Exit 1 on any failure.
"""
import os, re, sys

LIB = sys.argv[1] if len(sys.argv) > 1 else "/home/joe/code/futon3/library"
SEC = "process"
REQ = ["context", "if", "however", "then", "because"]
fails = []
n = 0
d = os.path.join(LIB, SEC)
for fn in sorted(os.listdir(d)):
    if not fn.endswith(".flexiarg"):
        continue
    n += 1
    pid = SEC + "/" + fn[:-len(".flexiarg")]
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
    if "N-process-trap-recording-conventions" not in text:
        fails.append((pid, "no conventions-note receipt citation"))
    if "P-assured-process" not in text:
        fails.append((pid, "no P-assured-process problem naming"))
    if re.search(r"^@draft", text, re.M):
        fails.append((pid, "@draft pattern cannot carry @why (AC8)"))
for f in fails:
    print("FAIL %s: %s" % f)
print("l2-parse-gate: %d files, %d failures" % (n, len(fails)))
sys.exit(1 if fails else 0)
