#!/usr/bin/env python3
"""L6/L7/L8/L9/L10 RATIONALE BACKFILL (generalized L3 pilot form).

Usage: l6_backfill.py SECTIONS_CSV [LIBRARY_DIR] [ROW_TAG]
SECTIONS_CSV e.g. "war-room,cascades,futon-theory".

For every pattern in the given sections:
  - if it has no authored @why: add one from the problem corpus —
    @holds-at Rn -> the L5 dossier node problems/rN-* if it exists
    (a resolvable library edge); otherwise an explicit NO-SOURCE note
    (never invention) with a source pointer to the committed check
    runs/l6_no_source_check.py receipt runs/L6-no-source-check.edn.
  - if it has no @how: add own-mechanism @how from its ! conclusion,
    untruncated, with source pointer.
Annotations are inserted after the last @-meta line before the body.
Idempotent via the ROW_TAG marker. Touches only the given sections.
"""
import os, re, sys

SECTIONS = [s.strip() for s in (sys.argv[1] if len(sys.argv) > 1
                                else "war-room,cascades,futon-theory").split(",")]
LIB = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                      else "/home/joe/code/futon3/library")
TAG = sys.argv[3] if len(sys.argv) > 3 else "L6"

# L5 dossier nodes by holds-at token
prob_dir = os.path.join(LIB, "problems")
holds_to_node = {}
for fn in sorted(os.listdir(prob_dir)):
    if not fn.endswith(".flexiarg"):
        continue
    pid = "problems/" + fn[:-len(".flexiarg")]
    m = re.match(r"^(r[a-z0-9]+)-", fn)
    if m:
        tok = m.group(1)
        tok = "R" + tok[1:]
        if tok == "Ra":
            tok = "R3a"
        holds_to_node.setdefault(tok.upper(), pid)
        holds_to_node.setdefault(tok, pid)

def q(s):
    return s

def first_conclusion(lines):
    for i, ln in enumerate(lines):
        m = re.match(r"^!\s+(conclusion|summary):\s*(.*)$", ln)
        if m:
            if m.group(2).strip():
                return m.group(2).strip()
            # text on continuation lines below the marker
            parts = []
            for l2 in lines[i + 1:]:
                if re.match(r"^\s*[!+?]\s*[A-Za-z]", l2) or (l2.strip() == "" and parts):
                    break
                if l2.strip():
                    parts.append(l2.strip())
                elif not parts:
                    continue
            return " ".join(parts)
    return ""

changed = []
for sec in SECTIONS:
    d = os.path.join(LIB, sec)
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".flexiarg"):
            continue
        path = os.path.join(d, fn)
        pid = sec + "/" + fn[:-len(".flexiarg")]
        lines = open(path, encoding="utf-8").read().splitlines()
        if any(("%s rationale-backfill" % TAG) in l for l in lines):
            continue  # idempotent
        has_why = any(re.match(r"^@why\s+\S", l) for l in lines)
        has_how = any(re.match(r"^@how\s+\S", l) for l in lines)
        if has_why and has_how:
            continue
        holds = None
        for ln in lines:
            m = re.match(r"^@holds-at\s+(R[A-Za-z0-9]+|TRACE)", ln)
            if m:
                holds = m.group(1)
                break
        add = []
        if not has_why:
            target = holds_to_node.get(holds) if holds else None
            if target:
                add.append("@why %s (source: this pattern's @holds-at %s matched to the L5 dossier node; %s rationale-backfill, zai-1, 2026-09-05)"
                           % (target, holds, TAG))
            else:
                add.append("@why no problem-corpus source matched (@holds-at %s; no dossier, WR-ruling problem node, or L5 record node names this pattern's problem). Negative claim per the committed check futon2 holes labs library-loop runs l6_no_source_check.py, receipt runs/L6-no-source-check.edn. Rationale not invented (%s rationale-backfill, zai-1, 2026-09-05)"
                           % (holds if holds else "absent", TAG))
        if not has_how:
            c = first_conclusion(lines)
            if c:
                add.append("@how own mechanism: %s (source: this pattern's ! conclusion line; %s rationale-backfill, zai-1, 2026-09-05)"
                           % (c, TAG))
        if not add:
            continue
        body = next((i for i, ln in enumerate(lines)
                     if re.match(r"^\s*[!+?]\s*[A-Za-z]", ln)), len(lines))
        last_meta = max([i for i, ln in enumerate(lines[:body])
                         if ln.startswith("@")], default=0)
        new = lines[:last_meta + 1] + add + lines[last_meta + 1:]
        open(path, "w", encoding="utf-8").write("\n".join(new) + "\n")
        changed.append((pid, "dossier" if (not has_why and holds_to_node.get(holds)) else
                        ("no-source" if not has_why else "kept-why"),
                        "own-how" if not has_how else "kept-how"))
for c in changed:
    print(c[0], "|", c[1], "|", c[2])
print("%s-backfill: %d files annotated" % (TAG, len(changed)))
