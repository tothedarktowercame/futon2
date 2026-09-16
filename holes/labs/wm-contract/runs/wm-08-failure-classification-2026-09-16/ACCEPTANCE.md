# Accepted bounded WM-08 failure-classification repair

Implementation: codex-9, futon2 `eeaaf199` followed by transport correction `fb849b22`.
Independent reviewer: claude-3, acceptance commit `b2e98483c5e1c1247e1ade64b8921d706ff50afd`.
Codex-9 read the committed review and verified its commit scope on 2026-09-16. This records independent acceptance; it is not an additional independent test run.

The blocking transport regression in review `1b070d4b` is resolved. Machine faults retain machine repair and their original causes; recognized transport faults retain the runner's environmental classification; content and budget handling retain their appropriate contracts. The required classifier is the runner's existing function. Failure records and source companions remain admitted in the tested cases.

Reviewer reran 14 tests / 467 assertions (interpretation-job), 7 / 31 (receipt-construction), and 5 / 27 (interpretation-evidence), all green, plus adversarial controls. Because another author was editing receipt-construction, review used the `fb849b22` copies of that module and test in isolated JVMs. Acceptance therefore applies to the reviewed snapshot, not unreviewed concurrent changes or the serving JVM.

Open findings: F1 interpreter self-reported availability/budget; F2 unreadable diagnostic data containing non-EDN objects; N1 a throwing replacement classifier can mask the original fault (reviewer reports not reachable with the actual supplied classifier). F1/F2 predate this repair; reviewer classified all three as non-blocking for this bounded correction. F3 final full-namespace log is resolved.

No serving activation, production click, ordinary-find evidence or checklist closure is claimed. WM-08 and WM-09 remain open. Their owners are being notified of this accepted prerequisite repair and its exact limits.

Review file SHA256: 1d3789dcbf2f74b0688a68bb5b888bec5189b4430e7a869cc7d538d7f42a4deb
