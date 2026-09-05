#!/usr/bin/env python3
"""L6/L7/L8/L9/L10 RATIONALE BACKFILL (generalized L3 pilot form).

Usage: l6_backfill.py SECTIONS_CSV [LIBRARY_DIR] [ROW_TAG] [GROUND_SECTIONS_CSV]
GROUND_SECTIONS_CSV (default "problems"): sections whose patterns form the
grounding corpus (L8 uses "problems,futon-theory" per its row statement).
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
RECEIPT = "L%s-no-source-check.edn" % TAG
# Source corpus: every node in the grounding sections (default: problems),
# with its own @holds-at tokens and full text. L8 passes
# "problems,futon-theory" per its row statement. Same match rule as
# runs/l6_no_source_check.py: holds-token equality, or the pattern's
# qualified id named in a node's text.
GROUND_SECTIONS = [x.strip() for x in (sys.argv[4] if len(sys.argv) > 4
                                       else "problems").split(",")]
corpus = []
for _sec in GROUND_SECTIONS:
    _pdir = os.path.join(LIB, _sec)
    for _fn in sorted(os.listdir(_pdir)):
        if _fn.endswith(".flexiarg"):
            _pid = _sec + "/" + _fn[:-len(".flexiarg")]
            _txt = open(os.path.join(_pdir, _fn), encoding="utf-8",
                        errors="replace").read()
            _holds = re.findall(r"^@holds-at\s+(\S+)", _txt, re.M)
            corpus.append((_pid, _holds, _txt))

def corpus_match(pid, holds):
    for (nid, nholds, ntxt) in corpus:
        if (holds and any(h in nholds for h in holds)) or (pid in ntxt):
            return nid, ("holds" if (holds and any(h in nholds for h in holds)) else "named")
    return None

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
            m = corpus_match(pid, [holds] if holds else [])
            if m:
                add.append("@why %s (source: %s match against the problem corpus per the committed check futon2 holes labs library-loop runs l6_no_source_check.py, receipt runs/L6-no-source-check.edn; %s rationale-backfill, zai-1, 2026-09-05)"
                           % (m[0], m[1], TAG))
            else:
                add.append("@why no problem-corpus source matched (@holds-at %s; holds-token and id-text searches over every node in [%s] found no match). Negative claim per the committed check futon2 holes labs library-loop runs l6_no_source_check.py, receipt runs/%s. Rationale not invented (%s rationale-backfill, zai-1, 2026-09-05)"
                           % (holds if holds else "absent", " ".join(GROUND_SECTIONS), RECEIPT, TAG))
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
        changed.append((pid, "corpus-match" if (not has_why and corpus_match(pid, [holds] if holds else [])) else
                        ("no-source" if not has_why else "kept-why"),
                        "own-how" if not has_how else "kept-how"))
for c in changed:
    print(c[0], "|", c[1], "|", c[2])
print("%s-backfill: %d files annotated" % (TAG, len(changed)))
