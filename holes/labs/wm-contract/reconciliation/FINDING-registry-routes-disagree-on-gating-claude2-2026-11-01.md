# Finding: the two mission-registry routes disagree on operator gating (claude-2, 2026-11-01)

`mission-registry/load-missions` zero-arg reads substrate-2 (futon1b) and projects
`:operator-gated?` from `:mission/operator-gated?`; `load-missions "/home/joe/code"` reads the
mission files and derives gating from gate lines. On 2026-11-01 ~18:00Z the substrate route
reports 167 live missions of 344 (30 gated), the file route 181 live (gated missions not
recognised), for the same checkout. Fourteen missions differ, among them M-fold-ansatz,
M-wm-tripwires, M-a4a-structure-node, M-象-cascade, M-aif-over-the-operator.

Consequence today: the field builder (substrate route) and the requirements projector (file
route) read different rosters, and Lean Q1 reports "631 enumerated / 645 open". Repair for the
War Machine: the record carries the roster it used (packet L5-8). The registry discrepancy
itself is for the registry owner: either the file grammar for gate lines or the substrate
ingestion is stale for these 14.

## L5-8 single read-only probe, 2026-11-01T17:57:45Z

Fresh JVM calls `load-missions` and `load-missions "/home/joe/code"`, once each. Raw evidence: `/home/joe/notes/l5-8/probe.edn`; script `probe.clj` beside it. Substrate contains 344 missions; files contain 342. The fresh probe finds **17** differing gating flags, **15** of which alter live membership; it does not reproduce the earlier count of 14. Both routes are read sequentially, not an atomic snapshot. No ingestion or registry mutation performed.

| Mission | Substrate gated? | Files gated? | Substrate live? | Files live? |
|---|---|---|---|---|
| M-a4a-structure-node | true | false | false | true |
| M-aif-over-the-operator | true | false | false | true |
| M-case-studies | true | false | false | true |
| M-descriptive-essay-of-the-stack | true | false | false | true |
| M-fold-ansatz | true | false | false | true |
| M-invariant-queue-unstuck | true | false | false | false |
| M-lab-standard | true | false | false | true |
| M-peeragogy-rewrite | true | false | false | true |
| M-populate-substrate-2 | true | false | false | true |
| M-stack-essay-code-alignment | true | false | false | true |
| M-stack-morphogenetic-rewrite | true | false | false | true |
| M-substrate-metric | true | false | false | false |
| M-text-sidecar | true | false | false | true |
| M-war-machine-aif-completion | true | false | false | true |
| M-wm-tripwires | true | false | false | true |
| M-zaif-harness | true | false | false | true |
| M-象-cascade | true | false | false | true |
