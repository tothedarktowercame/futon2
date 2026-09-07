# D1 evidence: document-reference hypergraph

## Computation

At the repository revisions pinned in `graph.edn`, the deterministic sweep found **479 nodes**, **410 document hyperedges**, and **2078 incidences**. Node counts are `{'excursion': 160, 'mission': 299, 'problem': 20}`; hyperedges by source type are `{'excursion': 130, 'mission': 262, 'problem': 18}`; incidences by referenced-member type are `{'excursion': 250, 'mission': 1778, 'problem': 50}`. There are **69** source documents with no extracted corpus reference.

The largest component has **350** nodes; the graph has **112** components. On the largest nontrivial component, normalized lambda_2 is **0.041001106**, versus configuration-null **0.223548827 +/- 0.051779321** (z = **-3.53**). Unnormalized lambda_2 is **0.103564694**, versus **0.302994265 +/- 0.069250476** (z = **-2.88**). The null used **200** rewirings, seed **54112026**, preserving node degree and hyperedge size by binary-incidence double-edge swaps.

## Reading

The real normalized lambda_2 is -3.53 SD below its null, and the unnormalized value is -2.88 SD below its null. Both operators therefore agree in direction: the references have more bottlenecked/module-like wiring than the degree sequence alone predicts. This is not the one-hyperedge degeneracy described for deployed memories. The graph is nevertheless fragmented, with 112 components and 69 documents making no outgoing corpus reference. It can supply a structured outcome domain for preference computations on its 350-document largest component, but a computation over the whole corpus must state how it treats the smaller components and isolates.

No ticket node was found. Mechanically enumerated category absences:

- `futon2`: absent excursion, ticket; census `{'excursion': 0, 'mission': 10, 'problem': 20, 'ticket': 0}`
- `futon3`: absent problem, ticket; census `{'excursion': 11, 'mission': 38, 'problem': 0, 'ticket': 0}`
- `futon3a`: absent excursion, problem, ticket; census `{'excursion': 0, 'mission': 3, 'problem': 0, 'ticket': 0}`
- `futon3b`: absent excursion, problem, ticket; census `{'excursion': 0, 'mission': 1, 'problem': 0, 'ticket': 0}`
- `futon3c`: absent problem, ticket; census `{'excursion': 100, 'mission': 169, 'problem': 0, 'ticket': 0}`
- `futon4`: absent problem, ticket; census `{'excursion': 2, 'mission': 26, 'problem': 0, 'ticket': 0}`
- `futon5`: absent excursion, problem, ticket; census `{'excursion': 0, 'mission': 16, 'problem': 0, 'ticket': 0}`
- `futon5a`: absent problem, ticket; census `{'excursion': 27, 'mission': 13, 'problem': 0, 'ticket': 0}`
- `futon6`: absent problem, ticket; census `{'excursion': 20, 'mission': 23, 'problem': 0, 'ticket': 0}`

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
find /home/joe/code/REPO -path '*/.git' -prune -o -path '*/data' -prune -o -path '*/.history' -prune -o -type f   '(' -path '*/holes/missions/M-*.md' -o -path '*/holes/excursions/E-*.md'   -o -path '*/holes/problems/P-*.md' -o -path '*/library/problems/*'   -o -path '*/holes/tickets/*' -o -name 'TICKET-*' ')' -print
```
