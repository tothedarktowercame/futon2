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
