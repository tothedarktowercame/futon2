# L14 — edge-resolution re-census delta

Before: `L12-census-receipt.edn` (futon3 `5704359`). After: `L14-census-receipt.edn` / `L14-census-graph.edn` (futon3 `64436d7`), from two differently named clean worktrees, byte-identical (receipt md5 `6f389722f9dc073db98fba5d54adba9c`), EDN-parseable.

## Library-wide

| measure | before | after |
|---|---|---|
| carrying @why | 533 | 533 |
| reachable up, problems | 40 | 41 |
| up, problems+WR | 63 | 64 |
| down, problems | 34 | 67 |
| down, problems+WR | 89 | 104 |

@why-unreachable share (down, problems+WR): 93.1% -> 91.9%.

## Per-section reachability delta (sections that changed)

| section | up-p | up-p+wr | dn-p | dn-p+wr |
|---|---|---|---|---|
| aif | 0->0 | 0->0 | 0->19 | 5->20 |
| cycle-machine | 0->0 | 0->0 | 0->1 | 5->5 |
| features | 0->0 | 0->0 | 0->2 | 2->2 |
| process | 0->0 | 0->0 | 0->5 | 8->8 |
| war-room | 8->8 | 28->28 | 0->6 | 28->28 |

## Match accounting (transformation receipt `L14-edge-resolution.edn`, md5 `e609bbe7f7e7783dda89f31af9657c09`, pre-state futon3 `5704359`)

469 patterns re-matched with EVERY @holds-at token parsed (multi-token lines included: wr-24 `R13 R15`, wr-25 `R9 R12`); 37 resolvable @why edges added, each citing its basis inline (holds or named; no other basis admitted); 2 matches already edged; 443 unmatched left alone (443 no-holds-and-not-named, 0 holds-token-without-node).
