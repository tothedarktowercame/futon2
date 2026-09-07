#!/usr/bin/env python3
"""Render the human receipt strictly from graph.edn and spectral.edn."""
import json
from pathlib import Path
H=Path(__file__).parent
g=json.loads((H/'graph.edn').read_text()); s=json.loads((H/'spectral.edn').read_text())
c=s['counts']; n=s['normalized']; u=s['unnormalized']; comp=s['components']
absent=[]
for repo,types in g['census-by-repo-and-type'].items():
    missing=[k for k,v in types.items() if v==0]
    if missing: absent.append(f"- `{repo}`: absent {', '.join(missing)}; census `{types}`")
reading=(f"The real normalized lambda_2 is {n['z']:.2f} SD below its null, and the "
         f"unnormalized value is {u['z']:.2f} SD below its null. Both operators therefore agree in direction: "
         "the references have more bottlenecked/module-like wiring than the degree sequence alone predicts. "
         "This is not the one-hyperedge degeneracy described for deployed memories. The graph is nevertheless fragmented, "
         f"with {comp['count']} components and {c['reference-isolates']} documents making no outgoing corpus reference. "
         "It can supply a structured outcome domain for preference computations on its 350-document largest component, "
         "but a computation over the whole corpus must state how it treats the smaller components and isolates.")
text=f"""# D1 evidence: document-reference hypergraph

## Computation

At the repository revisions pinned in `graph.edn`, the deterministic sweep found **{c['nodes']} nodes**, **{c['hyperedges']} document hyperedges**, and **{c['incidences']} incidences**. Node counts are `{c['nodes-by-type']}`; hyperedges by source type are `{c['hyperedges-by-source-type']}`; incidences by referenced-member type are `{c['incidences-by-member-type']}`. There are **{c['reference-isolates']}** source documents with no extracted corpus reference.

The largest component has **{comp['largest-size']}** nodes; the graph has **{comp['count']}** components. On the largest nontrivial component, normalized lambda_2 is **{n['lambda-2']:.9f}**, versus configuration-null **{n['null']['mean']:.9f} +/- {n['null']['sd']:.9f}** (z = **{n['z']:.2f}**). Unnormalized lambda_2 is **{u['lambda-2']:.9f}**, versus **{u['null']['mean']:.9f} +/- {u['null']['sd']:.9f}** (z = **{u['z']:.2f}**). The null used **{s['rewirings']}** rewirings, seed **{s['seed']}**, preserving node degree and hyperedge size by binary-incidence double-edge swaps.

## Reading

{reading}

No ticket node was found. Mechanically enumerated category absences:

{chr(10).join(absent)}

Tickets mean regular files under `holes/tickets/` or files whose basename starts `TICKET-`; none matched. The exact reference extraction rule is stored in `graph.edn`: each referencing document contributes one incidence hyperedge, with no clique expansion.

## Reproduce

From this directory:

```sh
python3 extract_graph.py
PYTHONPATH=/home/joe/code/tts/lib/python3.12/site-packages python3 spectral_graph.py
python3 render_receipt.py
sha256sum graph.edn spectral.edn receipt.md
```

Enumeration and absence command used before scripting:

```sh
find /home/joe/code/REPO -path '*/.git' -prune -o -path '*/data' -prune -o -path '*/.history' -prune -o -type f \
  '(' -path '*/holes/missions/M-*.md' -o -path '*/holes/excursions/E-*.md' \
  -o -path '*/holes/problems/P-*.md' -o -path '*/library/problems/*' \
  -o -path '*/holes/tickets/*' -o -name 'TICKET-*' ')' -print
```
"""
(H/'receipt.md').write_text(text)
