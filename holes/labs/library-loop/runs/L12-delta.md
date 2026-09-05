# L12 — re-census and reachability delta (L5-L10)

Baseline: `L1-census-receipt.edn` (futon3 `9ef7bc1815`, md5 `c99a2430d73361991cbe6c96035b0918`).
After: `L12-census-receipt.edn` / `L12-census-graph.edn` (futon3 `5704359`), generated from two differently named clean worktrees, byte-identical (receipt md5 `97f4e72c5930337fd8f58a1e67589016`, graph md5 `b536b6572a90ac401fc0945235073372`), EDN-parseable.

## Library-wide

| measure | L1 baseline | after L5-L10 |
|---|---|---|
| patterns total | 1256 | 1291 |
| problem-stating nodes | 6 | 32 |
| carrying @why | 83 | 533 |
| carrying @how | 21 | 507 |
| @why-reachable up, problems | 14 | 40 |
| up, problems+WR | 34 | 63 |
| down, problems | 6 | 34 |
| down, problems+WR | 53 | 89 |

Joe's number, measured: @why-unreachable share of the library moved from 1203/1256 = 95.8% (L1) to 1202/1291 = 93.1% (after L5-L10) under the most permissive reading (down, problems+WR).

## Per-section delta (sections that changed; before -> after)

| section | patterns (b->a) | why (b->a) | how (b->a) | up-p (b->a) | up-p+wr (b->a) | dn-p (b->a) | dn-p+wr (b->a) |
|---|---|---|---|---|---|---|---|
| agency | 8->8 | 0->8 | 0->8 | 0->0 | 0->0 | 0->0 | 0->0 |
| agent | 17->17 | 0->17 | 0->17 | 0->0 | 0->0 | 0->0 | 0->0 |
| aif | 33->33 | 6->33 | 13->33 | 0->0 | 0->0 | 0->0 | 5->5 |
| campaign-coherence | 3->3 | 0->3 | 0->3 | 0->0 | 0->0 | 0->0 | 0->0 |
| career-coherence | 1->1 | 0->1 | 0->1 | 0->0 | 0->0 | 0->0 | 0->0 |
| cascades | 5->5 | 0->5 | 0->5 | 0->0 | 0->0 | 0->0 | 0->0 |
| code-coherence | 2->2 | 0->2 | 0->2 | 0->0 | 0->0 | 0->0 | 0->0 |
| collaboration-coherence | 8->8 | 0->8 | 0->8 | 0->0 | 0->0 | 0->0 | 0->0 |
| devmap-coherence | 20->20 | 0->20 | 0->20 | 0->0 | 0->0 | 0->0 | 0->0 |
| futon-theory | 37->37 | 0->37 | 0->37 | 0->0 | 0->0 | 0->2 | 0->2 |
| math-formalization | 29->30 | 4->30 | 0->30 | 0->0 | 0->0 | 0->0 | 0->0 |
| math-formalization-CA | 23->23 | 6->23 | 0->23 | 0->0 | 0->0 | 0->0 | 0->0 |
| math-informal | 24->24 | 0->24 | 0->24 | 0->0 | 0->0 | 0->0 | 0->0 |
| math-strategy | 20->20 | 4->20 | 0->20 | 0->0 | 0->0 | 0->0 | 0->0 |
| musn | 17->17 | 0->17 | 0->17 | 0->0 | 0->0 | 0->0 | 0->0 |
| or3 | 17->17 | 0->17 | 0->17 | 0->0 | 0->0 | 0->0 | 0->0 |
| peeragogy | 18->18 | 0->18 | 0->18 | 0->0 | 0->0 | 0->0 | 0->0 |
| plos-npt-with-small-n | 22->22 | 0->22 | 0->22 | 0->0 | 0->0 | 0->0 | 0->0 |
| problems | 6->32 | 5->31 | 0->26 | 6->32 | 6->32 | 6->32 | 6->32 |
| process | 0->8 | 0->8 | 0->8 | 0->0 | 0->0 | 0->0 | 0->8 |
| relationship-coherence | 19->19 | 0->19 | 0->19 | 0->0 | 0->0 | 0->0 | 0->0 |
| snatch | 24->24 | 21->24 | 3->24 | 0->0 | 0->0 | 0->0 | 0->0 |
| storage | 21->21 | 0->21 | 0->21 | 0->0 | 0->0 | 0->0 | 0->0 |
| ukrns | 26->26 | 0->26 | 1->26 | 0->0 | 0->0 | 0->0 | 0->0 |
| vsatlas | 23->23 | 0->23 | 0->23 | 0->0 | 0->0 | 0->0 | 0->0 |
| war-room | 28->28 | 10->28 | 0->28 | 8->8 | 28->28 | 0->0 | 28->28 |
| writing-coherence | 23->23 | 2->23 | 0->23 | 0->0 | 0->0 | 0->0 | 0->0 |

All other sections are unchanged (full tables in both receipts). Sections added by L5: the 26 new `problems/*` nodes.
