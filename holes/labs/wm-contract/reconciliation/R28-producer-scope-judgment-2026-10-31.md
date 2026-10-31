# R28 producer-scope judgment — 2026-10-31

Decision: leave B2-D4-R28 OPEN. Do not commission a new retention field on the evidence reviewed here.

The original discovery identifies selected-target-reading -> selected-target-reading-redecision -> retained-selected-target-reading as the producer/observer chain that lost a reading after failed redecision. Its original occurrence is run 46f8cac7, whose observer says not-required despite the reading phase having run. That historical failure is a regression fixture, not a live success of the later repair.

The independently reviewed 39a845a3 retry is occurrence-square-redecision, downstream of selection. It did retry and complete that phase, then failed at cascade-resume. A direct read of its retained record confirms interpretation-ask is selection-complete-no-ask-required, the selection frontier has no reading-absent ids, and its preparation backlog is selected-target-reading-not-required. These observations agree with the distinction between producers; they do not demonstrate the repaired selected-target-reading failure-retention path.

Accordingly, retract both earlier arguments: “no retry” is false, and the unrelated not-applicable field does not prove that a reading was silently lost. Also reject the proposed new requirement for an occurrence-square-specific field: the written closure condition does not prescribe such a field. The remaining evidence question is whether a real failed redecision on the original affected producer retains the actual reading in its supported run record, without the false not-required fallback. Search for that witness before proposing any new code or charged run.

This judgment changes no tracker status or closure condition, and establishes no new runtime defect. No tests, serving evaluation, click, or credit consumption were required. The record was read using the existing read-only r28_probe2.clj script under Babashka, not a test runner.

## Evidence pins

- Original discovery: `holes/labs/wm-contract/DISCOVERY-B2-D4-R28-2026-10-29.md`, SHA256 `a058650cfc57f0f1e82e112d662a1a806cbd31e16f42f1c2bd697785253bea03`.
- Reviewed run: `data/wm-runs/tick-run-record-2026-10-29-39a845a3-496a-4bce-888c-e8b0ca2835ab.edn`, SHA256 `591c5505e2a155663e9f5e676e8dccc28c7bacef3bb53cc2a195dd0f846601dd`.
- Independent review: [B1-E3-R28-independent-review-zai6-2026-10-31.md](B1-E3-R28-independent-review-zai6-2026-10-31.md). Its chronology and producer distinction are accepted; its added field-specific closure requirement is not.
