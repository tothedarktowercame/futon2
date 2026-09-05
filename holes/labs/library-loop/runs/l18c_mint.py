#!/usr/bin/env python3
"""L18 v3 minting: section-doc problem nodes for the remaining groundable
worklist members (data-mining, cycle-machine, coordination, futon-theory,
plos-npt-with-small-n), each quoting its committed section documentation.
Edges are added ONLY to members of the fixed 71-member worklist. Idempotent.
"""
import os, re, sys

LIB = os.path.abspath(sys.argv[1] if len(sys.argv) > 1
                      else "/home/joe/code/futon3/library")
REPORT = sys.argv[2] if len(sys.argv) > 2 else "L17-advisory-gate-report.md"

SERVED = ["zaif-cascade.edn", "construct-cascade.edn", "snatch-cascade.edn",
          "ants-cascade.edn", "alfworld-cascade.edn"]
def refused_from(fn):
    txt = open(REPORT, encoding="utf-8").read()
    sec = txt.split("### %s" % fn, 1)[1].split("\n### ", 1)[0]
    m = re.search(r"- \*\*down-problems\+wr\*\*: (\d+) refused -- (.*)", sec)
    return set(m.group(2).split(", ")) if m and m.group(2) != "(none)" else set()
worklist = set().union(*[refused_from(f) for f in SERVED])

NODES = {
    "data-mining": ("llm-over-corpus-mining-lessons",
        "LLM-over-corpus mining pipelines need distilled lessons",
        "Lessons distilled from the futon6 meme-mining (forward: turns -> memes/methods) and goals-and-holes (backward: turns -> C-entries/belly) runs on a rented GPU box, 2026-06-25/26. They generalize to any pipeline that runs a model over a large corpus to extract structured records",
        "futon3 library/data-mining/README.md, opening paragraph"),
    "cycle-machine": ("cycle-model-boundary-gaps",
        "Repairs cluster at boundaries the verified cycle model does not cover",
        "Derived from the F27 review (futon3c/holes/technotes/TN-fable-F27-review.md): one week of M-apm-demonstration repairs (~150 APM commits, 2026-08-17..23) fell into three clusters, each a boundary between the verified cycle model and the running apparatus",
        "futon3 library/cycle-machine/INDEX.md, opening paragraph"),
    "coordination": ("coordination-patterns-derivation",
        "Agent coordination needs domain-specific patterns derived from futon-theory",
        "Domain-specific patterns for the futon3 agent coordination pipeline, derived from futon-theory via the derivation xenotype (IDENTIFY -> MAP -> DERIVE -> VERIFY -> INSTANTIATE)",
        "futon3 library/coordination/INDEX.md, opening paragraph"),
    "futon-theory": ("theory-general-enough-to-drive-the-stack",
        "Futon theory must be general enough to drive the whole stack",
        "Design patterns that specify futon theory with sufficient generality to drive development of the entire FUTON stack",
        "futon3 library/futon-theory/INDEX.md, opening paragraph"),
    "plos-npt-with-small-n": ("pattern-coding-and-novel-force-mining",
        "A pattern library must be codeable against a corpus and mined for what it misses",
        "Each corpus paper coded PRESENT / PARTIAL / ABSENT against the 16 library patterns, then mined for NOVEL FORCES the library doesn't yet capture",
        "futon3 library/plos-npt-with-small-n/_pattern-coding-corpus.md, opening paragraph"),
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

  + context: Minted by library-loop row L18 v3, grounding members of the fixed served-refused worklist that the section documentation names. Source: {docsrc}.

  + IF:
    Work happens in or consumes the {sec} section's patterns.

  + HOWEVER:
    The section's own documentation states the problem: "{quote}" (source: {docsrc})

  + THEN:
    This node is the landing point for @why edges from the {sec} worklist members (mechanical pass, row L18 v3); per-pattern questions route to the section documentation, not to invention here.

  + BECAUSE:
    A section whose documentation states one shared problem carries one problem node, not per-pattern copies (the L11/L15/L18 cost finding).

    + evidence: {docsrc}; the L18 v3 complete search receipt runs/L18-remainder.edn.
"""

EDGE = ("@why problems/%s (L18 v3 grounding; basis: the %s section documentation names this member and states the section problem; source: futon2 holes labs library-loop runs l18_complete_check.py receipt runs/L18-remainder.edn; zai-1, 2026-09-05)")

minted = edged = []
for sec, (slug, title, quote, docsrc) in sorted(NODES.items()):
    npath = os.path.join(LIB, "problems", slug + ".flexiarg")
    if not os.path.exists(npath):
        open(npath, "w", encoding="utf-8").write(tmpl.format(
            slug=slug, title=title, sec=sec, quote=quote, docsrc=docsrc,
            kw=sec, gist=quote.split(" (")[0]))
        minted.append(slug)
    edge = EDGE % (slug, sec)
    for pid in sorted(worklist):
        if not pid.startswith(sec + "/"):
            continue
        path = os.path.join(LIB, pid + ".flexiarg")
        lines = open(path, encoding="utf-8").read().splitlines()
        if any("L18 v3 grounding" in l for l in lines):
            continue
        body = next((i for i, ln in enumerate(lines)
                     if re.match(r"^\s*[!+?]\s*[A-Za-z]", ln)), len(lines))
        last = max([i for i, ln in enumerate(lines[:body]) if ln.startswith("@")],
                   default=0)
        lines = lines[:last + 1] + [edge] + lines[last + 1:]
        open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")
        edged.append(pid)
print("l18v3: minted %s; edged %d worklist members: %s"
      % (minted, len(edged), edged))
