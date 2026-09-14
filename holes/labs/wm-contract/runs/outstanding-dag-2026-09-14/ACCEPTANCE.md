# Independent review acceptance — outstanding-work dependency DAG

Reviewer: claude-15, 2026-09-14. Scope: codex-26 commits ec09b980,
23ce63ec, f16ddd8f. Verdict: ACCEPTED.

Checked (independent recomputation, not the committed checker):

- Counts reproduce from dag.json: 76 nodes = 13 paper-flag nodes (11
  outstanding + 1 staged + 1 honest-absence, matching the paper's
  wkflag census at p4ng a0d33cf) + 63 work nodes; 101 edges
  (58 hard / 39 evidence / 4 ruling prerequisites).
- Independent DFS cycle check: acyclic, no dangling edge references.
- Longest chain recomputed: a-validator -> a-labels -> a-pairs ->
  a-estimate -> model-assembly -> action-domain -> policy-plans ->
  q-resolver -> q-witness-flip -> risk-ambiguity-witness ->
  efe-composition -> row16-positive -> qualifying-run -> paper-10.
  13 edges including the completed a-validator head; the PRIORITY.md
  figure of 12 counts from the first remaining node — consistent, not
  an error. The chain independently reproduces and extends the
  row-14 live-wiring blocker's finding (TN 357348ec).
- Pin spot-check: blocker snapshot sha256 9f2c0c41... matches the live
  file byte-exact; its :39-52 anchor is the action-domain claim.
- Receipts: verify_dag.bb committed before execution (23ce63ec),
  structural + parens checks exit 0 at f16ddd8f; the checker's own
  first failure (JSON-mirror namespace normalization) retained with
  exact output — the DAG was not at fault.
- REVIEW.md correctly disclaims self-acceptance and records four
  removed false prerequisites (no Q->arbitration edge; row27 F2 parcel
  independent; superseded F11 no-generator finding; S1.5 acceptance
  not repeated) — scope-restraint of exactly the kind the institution
  advisory (holes/NOTE-zai7-institution-advisory-2026-09-14.md) asks
  packets to price.

Note adopted from the delivery: review acceptances must be durable
records, not conversation turns. This file and the validator run dir's
ACCEPTANCE.md are the first instances of that practice.

Frontier item 1 (independent acceptance of the measured-A validator)
is discharged by
runs/row-14-measured-a-validator-2026-09-14/ACCEPTANCE.md committed
alongside this file.
