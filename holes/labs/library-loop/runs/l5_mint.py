#!/usr/bin/env python3
"""L5 PROBLEM-NODE MINING (dossier nodes).

Usage: l5_mint.py [WM_CONTRACT_DIR] [PROBLEMS_DIR]
Mints one library/problems/*.flexiarg node per twenty-node problem dossier
(PROBLEMS-*.md "## Rn — ..." -> "### The problem it solves"), quoting the
dossier's problem statement verbatim (first paragraph, capped at 6 sentences),
with a source pointer and no invention. L2's committed five-component form.
"""
import os, re, sys

WM = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                     else "/home/joe/code/futon2/holes/labs/wm-contract")
OUTD = os.path.abspath(sys.argv[2] if len(sys.argv) > 2
                       else "/home/joe/code/futon3/library/problems")

def slugify(s):
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", s.lower())).strip("-")

dossiers = []
for fn in sorted(f for f in os.listdir(WM) if f.startswith("PROBLEMS-")):
    txt = open(os.path.join(WM, fn), encoding="utf-8").read()
    for m in re.finditer(r"^## (R[A-Za-z0-9]+|TRACE) — (.+)$", txt, re.M):
        node, title = m.group(1), m.group(2).strip()
        sec = txt[m.end():]
        nxt = re.search(r"^## ", sec, re.M)
        sec = sec[:nxt.start()] if nxt else sec
        p = re.search(r"### The problem it solves\s*\n+(.*?)(?:\n### |\n\n)", sec, re.S)
        body = " ".join(p.group(1).split()) if p else ""
        sents = re.split(r"(?<=[.!?]) ", body)
        sents = sents[:6] if len(sents) >= 3 else sents  # cap at 6 sentences
        quote = " ".join(sents)
        if len(sents) == 6 and not quote.endswith(tuple(".!?")):
            quote += " … [first six sentences; see source for the remainder]"
        dossiers.append((node, title, quote, fn))

tmpl = """@flexiarg problems/{slug}
@title Problem node {node}: {title}
@keywords problem-node, {node}, dossier, {kw}
@audience futon stack operators, library authors
@tone plain
@style pattern
@holds-at {node}
@why dossier authority: {fn}, {node}, The problem it solves (no library pattern grounds this node; its authority is the recorded dossier)
@how no mechanism claimed at problem grain; what solving requires is the dossier's residual-problems list (source: futon2 holes labs wm-contract {fn}, {node})

! conclusion: This is a problem-stating node for control-map node {node} ({title}): {oneline}

  + context: Minted by library-loop row L5 from the twenty-node problem dossiers; it exists so @why edges from mechanism patterns have a problem node to land on.

  + IF:
    A pattern's rationale is that it answers the problem this node states.

  + HOWEVER:
    The problem, verbatim from the dossier: {quote} (source: futon2 holes labs wm-contract {fn}, {node}, The problem it solves)

  + THEN:
    Treat this node as the landing point for @why edges from patterns that answer it; solving the problem itself is scoped by the dossier's residual-problems list, not by this node.

  + BECAUSE:
    The census (library-loop L1) found only six problem-stating nodes for 1256 patterns, so most @why edges had nothing to land on; minting the dossier problems as nodes is the recorded-corpus repair.
    + evidence: futon2 holes labs wm-contract {fn}, section "## {node} — {title}", subsection "The problem it solves".
"""

minted = []
for (node, title, quote, fn) in sorted(dossiers):
    slug = ("%s-%s" % (node.lower(), slugify(title)))
    oneline = re.split(r"(?<=[.!?]) ", quote)[0]
    kw = slugify(title)
    path = os.path.join(OUTD, slug + ".flexiarg")
    if os.path.exists(path):
        continue  # idempotent
    open(path, "w", encoding="utf-8").write(
        tmpl.format(slug=slug, node=node, title=title, quote=quote, fn=fn,
                    oneline=oneline, kw=kw))
    minted.append(slug)
for s in minted:
    print(s)
print("l5-mint: %d dossier nodes minted (of %d dossiers)" % (len(minted), len(dossiers)))
