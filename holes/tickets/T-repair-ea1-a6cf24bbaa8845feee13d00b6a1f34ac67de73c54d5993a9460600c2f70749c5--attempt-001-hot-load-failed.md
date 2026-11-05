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

Finding: [repair-ea1-a6cf24bbaa8845feee13d00b6a1f34ac67de73c54d5993a9460600c2f70749c5--attempt-001-hot-load-failed](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-ea1-a6cf24bbaa8845feee13d00b6a1f34ac67de73c54d5993a9460600c2f70749c5--attempt-001-hot-load-failed.edn)

Finding SHA-256: `ee1601b34fe7d2f9a72f05fc8f4c1c18e645965162b09558b1f57155913da24c`
