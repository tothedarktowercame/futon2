#!/usr/bin/env python3
"""L18 GROUND THE SERVED-REFUSED SETS (targeted, no invented rationale).

Usage: l18_ground.py [LIBRARY_DIR]
Mints section-grain problem nodes ONLY where the section's own committed
documentation states the problem (README/INDEX goal sentences, quoted with
pointers -- the L15 precedent), then adds the shared @why edge to every
pattern in those sections (mechanical, idempotent). Sections without such a
source are left alone and counted as honest no-source remainder.
"""
import os, re, sys

LIB = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library")

NODES = {
    "devmap-coherence": ("devmap-self-consistency-standards",
        "Devmap self-consistency standards",
        "keeping the FUTON devmaps self-consistent and structurally sound ... machine-checkable standards for devmap quality",
        "futon3 library/devmap-coherence/README.md, opening paragraph"),
    "eight-gates": ("tensions-are-navigated-not-eliminated",
        "Tensions are navigated, not eliminated",
        "Patterns hold tensions, not obstacles ... the preference model refinement (Mission 2) revealed that IF/HOWEVER fields are not obstacles to eliminate but tensions to navigate",
        "futon3 library/eight-gates/README.md, Core Insight section"),
    "or3": ("transferable-open-research-practice",
        "Transferable open-research practice needs distilling from cases",
        "Seventeen design patterns distilled from seven interview-based case studies of open research practice at Oxford Brookes ... or3 covers transferable practice -- what these particular people did that a colleague in another School could adopt",
        "futon3 library/or3/README.md, opening paragraph"),
    "baldwin": ("baldwin-causal-claims-vs-engineering-metaphor",
        "Baldwin causal claims must be separated from the engineering metaphor",
        "This library separates the high-level engineering metaphor in futon-theory/baldwin-cycle from the causal claims an evolutionary experiment must support",
        "futon3 library/baldwin/INDEX.md, opening paragraph"),
}

tmpl = """@flexiarg problems/{slug}
@title Problem node: {title}
@keywords problem-node, section-grain, {kw}
@audience futon stack operators, library authors
@tone plain
@style pattern
@why section-doc authority: {docsrc} (quoted below; no library pattern grounds this node)
@how no mechanism claimed at problem grain; the section's own patterns are the mechanisms

! conclusion: This is a problem-stating node at section grain for the {sec} section, from the section's own committed documentation: {gist}.

  + context: Minted by library-loop row L18, targeting the L17 advisory report's served-refused sets ({sec} members appear refused in the zaif/construct cascades). Source: {docsrc}.

  + IF:
    Work happens in or consumes the {sec} section's patterns.

  + HOWEVER:
    The section's own documentation states the problem: "{quote}" (source: {docsrc})

  + THEN:
    This node is the landing point for the shared @why edges from the {sec} patterns (mechanical pass, row L18); per-pattern questions route to the section documentation, not to invention here.

  + BECAUSE:
    A section whose documentation states one shared problem should carry one problem node, not per-pattern copies (the L11/L15 cost finding).

    + evidence: {docsrc}; L17 report runs/L17-advisory-gate-report.md.
"""

EDGE = ("@why problems/%s (L18 section-grain grounding; basis: this pattern is a member of the %s section whose shared problem that node states, quoting the section's own %s; source: futon2 holes labs library-loop runs l18_ground.py; zai-1, 2026-09-05)")

minted = edged = 0
for sec, (slug, title, quote, docsrc) in sorted(NODES.items()):
    node_path = os.path.join(LIB, "problems", slug + ".flexiarg")
    if not os.path.exists(node_path):
        open(node_path, "w", encoding="utf-8").write(tmpl.format(
            slug=slug, title=title, sec=sec, quote=quote, docsrc=docsrc,
            kw=sec, gist=quote.split(" ... ")[0]))
        minted += 1
    edge = EDGE % (slug, sec, docsrc.split(",")[0])
    d = os.path.join(LIB, sec)
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".flexiarg"):
            continue
        path = os.path.join(d, fn)
        lines = open(path, encoding="utf-8").read().splitlines()
        if any("L18 section-grain grounding" in l for l in lines):
            continue
        body = next((i for i, ln in enumerate(lines)
                     if re.match(r"^\s*[!+?]\s*[A-Za-z]", ln)), len(lines))
        last_meta = max([i for i, ln in enumerate(lines[:body])
                         if ln.startswith("@")], default=0)
        lines = lines[:last_meta + 1] + [edge] + lines[last_meta + 1:]
        open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")
        edged += 1
print("l18: %d nodes minted, %d patterns edged" % (minted, edged))
