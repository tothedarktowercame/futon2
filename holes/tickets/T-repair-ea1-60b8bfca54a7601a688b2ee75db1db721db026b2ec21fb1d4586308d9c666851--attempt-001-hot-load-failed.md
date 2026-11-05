# Correct hot load failed

**Status:** OPEN

## Observed failure

Target: not retained; stage: :cascade-resume.

Context: {:phase :cascade-resume}.

Selected cascade held in open-cascade phase

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance criteria

- A reproduction or regression check detects the reported failure at the named stage. Check: a test namespace/name or command recorded in this ticket. **Not started.**
- That check passes after the change, with the scoped validation output retained. Check: commit SHA and test output. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-001-hot-load-failed](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-001-hot-load-failed.edn)

Finding SHA-256: `af44668ea2ccb5d60e89b12fd2af9f4938396943f4b395aad65d8866b921b487`
