# P2 revision 3: bounded adjudication

Reviewed proposal: p2/criteria-matrix-zai6 at c112d794c. This note changes no tracker status, source, live state or run evidence.

## B3: no tie-predicate contradiction established

Coordinator direct EDN reads reproduce the reported numbers: 3415 has three scores4.957390779577444; f805 has three scores4.3134283183311295. Both carry three tied cascade IDs and tie? false.

However, outer_select.clj:84–95 explicitly defines these fields differently: tied-cascades lists all minimum-score cascades, whereas tie? counts DISTINCT TARGETS picked by those minimum-score cascades. The comment states that cascades sharing a minimum but picking the same task are not a tie in the choice. The identical logic is present in pre-run commit e298c6628 (verified with git show), so this is not a post-run reinterpretation.

Therefore equal scores plus tie? false alone do not prove a defect. Do not open a new defect row or interrupt P4 on that basis. The remaining historical question is whether those cascades actually picked different targets; a pure recomputation from pinned historical inputs or retained per-cascade picks would answer it. This note does not claim that recomputation was performed. Keep B3's wider justification unresolved where its required evidence is missing; remove the unsupported contradiction label.

## Other P2 decisions

- A2/A3: UNKNOWN is appropriate without a time-valid serving-envelope/authority join. Existing preflight receipts are admissible evidence if they match this run's time, exact composition and actual admission options; mere absence from the run record is not proof that no evidence exists elsewhere. No new machinery authorized to replace a bounded evidence search.
- C2: checkpoint timing is not proof of pre-send durability. Keep UNKNOWN unless a retained checkpoint/dispatch join establishes the order. No new instrumentation or rerun is authorized by this missing historical evidence.
- D1: use the coordinator's D1-3415-receipt-verification-2026-10-31.md/json. Three revision-bound checks were independently recomputed; one reached, two untouched. They are C4 anchored declaration-presence checks on mission Markdown, not execution of the described behaviors. Earlier blanket no-measurements claim is retracted.
- E4: retain contrary intervention evidence for the actual historical pair, while distinguishing it from an unattempted predeclared P6 sequence. No loop-demonstrated claim.
- R28: retain the limited24-record search and live-witness-owed result. No new producer field required.

Finish the P2 evidence account with these distinctions. Remaining unknowns are explicit evidence limits, not an automatic new repair queue.
