# L4 — Graph-certificate strawman (lab note, discovery only)

Row `:L4`, class `:M`. This note **measures**; it decides nothing, implements no
certificate, and installs no runtime refusal — it feeds the F7/F8 merge.
Measured against the actual L1 graph regenerated at futon3 `41448add834a75a65942b48f1206dac3e6553345`
(committed tree, isolated worktree; the canonical working tree holds one
untracked pattern that is excluded). Receipt: `L4-certificate-strawman-receipt.edn`
(md5 `3ae937c79ebb113814edc1ec526a5d56`, byte-identical across two differently
named isolated worktrees, parses as EDN). Script: `l4_certificate_strawman.py`
(library and checks roots as explicit argv; path-independent provenance).

Graph base: 1264 patterns, 118 carrying authored `@why`, 6 `problems/*` nodes,
28 `war-room/wr-*` nodes. @why-reachable: 6 (down, problems-only), 61 (down,
problems+WR), 14 (up, problems), 34 (up, problems+WR) — the four readings from
L1, which reading applies is reserved to the F7/F8 merge.

## (a) Cascade-consulted patterns @why-reachable from a named problem

Which cascades count as "served" is itself reserved; all seven cascade
artifacts in `futon3/checks` are measured, with the qualifier noted:

| cascade | consulted | refused (down, p+WR) | refused (up, p+WR) | no authored @why |
|---|---|---|---|---|
| zaif-cascade.edn (harness cascade) | 54 | 50 | 54 | 41 |
| construct-cascade.edn | 33 | 31 | 33 | 18 |
| snatch-cascade.edn | 12 | 12 | 12 | 2 |
| ants-cascade.edn | 6 | 6 | 6 | 5 |
| alfworld-cascade.edn | 1 | 1 | 1 | 0 |
| open-cascade.edn (whole-library blind) | 1240 | 1187 | 1206 | 1130 |
| retrodiction-cascade.edn (whole-library blind) | 1240 | 1187 | 1206 | 1130 |

Invariant (a) as stated would refuse the **entire served surface today**: even
under the most permissive reading (downward, problems+WR), 50 of 54 patterns
consulted by the zaif harness cascade are not @why-reachable from a named
problem, and 41 of the 54 have no authored `@why` at all. Under the strictest
reading every cascade is refused outright. The gap between "reachable" and
"merely annotated" is also large: 13 zaif-consulted patterns carry an authored
`@why` whose chain still terminates nowhere near a problem node.

## (b) No @why cycle lacks a grounding problem node

There are **zero @why cycles** in the current authored-@why graph (independently
consistent with `zaif-cascade.edn`'s own `:as-of :why-acyclic? true`).
Invariant (b) is therefore **vacuously satisfied today**: no cycle exists, so
none lacks grounding. The invariant's bite is prospective only — it would
matter exactly when backfill work (L2/L3-style) adds enough edges to make
cycles possible.

## (c) Cascade construction refuses patterns with no rationale path

Library-wide refusal counts if construction refused today:

| refusal rule | refused of 1264 |
|---|---|
| no authored `@why` at all | 1147 (90.8%) |
| not @why-reachable, down, problems-only | 1259 |
| not @why-reachable, down, problems+WR | 1204 |
| not @why-reachable, up, problems | 1251 |
| not @why-reachable, up, problems+WR | 1231 |

Per-cascade versions of the same rule are in the receipt. Under every reading,
rule (c) refuses **at least 95%** of the library and, per (a), most of each
cascade — including 41/54 of the harness cascade. The L2 process patterns
(8/8 reachable downward from problems+WR) are the only recent cohort that a
rationale-path refusal would admit whole.

## What this feeds (and does not decide)

The three candidate invariants differ sharply in cost today: (b) is free
(satisfied vacuously), (a) and (c) are catastrophic as hard refusals and would
need grandfathering, quota, or warning-only modes — a choice reserved to the
F7/F8 merge. Direction of "reachable" (up vs down, problems vs problems+WR)
changes counts by up to 65 patterns library-wide and by up to 4 on the harness
cascade; that choice is likewise reserved. Nothing here installs a check.
