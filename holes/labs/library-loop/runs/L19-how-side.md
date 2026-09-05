# L19 — the @how side, measured (discovery only)

Library at futon3 `e58576cec0f14c3da4667ed452d522c561487ee8`. No annotations written, no gate change, no library
file modified. Deterministic; byte-identical rerun.

## 1. @how edge-target distribution

| measure | count |
|---|---|
| patterns carrying @how | 542 |
| @how annotation values total | 555 |
| resolvable @how edges (value tokens that are pattern ids) | 32 |
| patterns whose @how is entirely prose (no resolvable token) | 521 |
| distinct @how targets (mechanism patterns pointed at) | 25 |

Note: the L3-L10 backfill wrote own-mechanism @how lines that quote the
pattern's own conclusion -- by construction self-referential or prose, so
they do not create edges; the resolvable @how edges predate or come from
curated editorial @how lines (README-flexiarg 5a: @how is written by an
editor, later).

## 2. Candidate @how root sets, measured (not decided)

| root set | size |
|---|---|
| problems | 40 |
| problems+wr | 68 |
| mechanism-bearing nodes (outgoing resolvable @how) | 21 |
| problems+mech-bearing | 61 |
| problems+wr+mech-bearing | 89 |
| problems+how-targets | 65 |

## 3. Combined how-OR-why readings for the seven cascades

Traversal: downward justified-by over @why edges UNION @how edges (a
pattern is reachable if a root justifies it via either relation).

| cascade | consulted | why-only (p+wr) | +how (p) | +how (p+wr) | +how (p+mech) | +how (p+wr+mech) |
|---|---|---|---|---|---|---|
| alfworld-cascade.edn | 1 | 0 | 0 | 0 | 0 | 0 |
| ants-cascade.edn | 6 | 5 | 5 | 5 | 5 | 5 |
| construct-cascade.edn | 33 | 18 | 17 | 16 | 17 | 16 |
| open-cascade.edn | 1239 | 826 | 853 | 819 | 847 | 814 |
| retrodiction-cascade.edn | 1239 | 826 | 853 | 819 | 847 | 814 |
| snatch-cascade.edn | 12 | 0 | 0 | 0 | 0 | 0 |
| zaif-cascade.edn | 53 | 26 | 27 | 25 | 27 | 25 |

## Recommendation sketch (with costs, deciding nothing)

- The @how side adds few new roots today: most resolvable @how edges run
  BETWEEN ordinary patterns, so 'mechanism-bearing' as a root set admits
  21 nodes and moves the served cascades by the margins above.
- Cost of adopting a combined reading: the census/gate would need a second
  edge kind traversed and an agreed root set; the choice is Joe's/F7-F8's.
- Reversal: if @how edges are later curated en masse (editorial), the
  mechanism-bearing root set grows and the combined reading changes shape.
