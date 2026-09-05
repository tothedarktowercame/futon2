# L14 — edge-resolution re-census delta

Before: `L12-census-receipt.edn` (futon3 `5704359`). After: `L14-census-receipt.edn` / `L14-census-graph.edn` (futon3 `c0b001c`), generated from two differently named clean worktrees, byte-identical (receipt md5 `b8822c3ba4895c2d58bec73cd65a6f0e`), EDN-parseable.

## Library-wide

| measure | before | after |
|---|---|---|
| carrying @why | 533 | 533 |
| reachable up, problems | 40 | 41 |
| up, problems+WR | 63 | 64 |
| down, problems | 34 | 67 |
| down, problems+WR | 89 | 104 |

@why-unreachable share (down, problems+WR): 1202/1291 = 93.1% -> 1187/1291 = 91.9%.

## Per-section reachability delta (sections that changed)

| section | up-p | up-p+wr | dn-p | dn-p+wr |
|---|---|---|---|---|
| aif | 0->0 | 0->0 | 0->19 | 5->20 |
| cycle-machine | 0->0 | 0->0 | 0->1 | 5->5 |
| features | 0->0 | 0->0 | 0->2 | 2->2 |
| process | 0->0 | 0->0 | 0->5 | 8->8 |
| war-room | 8->8 | 28->28 | 0->6 | 28->28 |

Unchanged sections omitted (full tables in both receipts).

## Match accounting (receipt `L14-edge-resolution.edn`, md5 `adf94d7aa8c8f036bd5aacf92667a445`)

469 patterns re-matched; 35 resolvable edges added (basis holds or named — no other basis admitted, no invention); 2 matches already edged before L14; 443 unmatched, left alone, reason distribution: 443 `no-holds-and-not-named`, 0 `holds-token-without-node-and-not-named` (every holds-token pattern matched its L5 node).
