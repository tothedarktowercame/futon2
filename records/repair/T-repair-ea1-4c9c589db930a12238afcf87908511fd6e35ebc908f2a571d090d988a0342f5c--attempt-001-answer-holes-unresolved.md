# Correct answer holes unresolved

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

Finding: [repair-ea1-4c9c589db930a12238afcf87908511fd6e35ebc908f2a571d090d988a0342f5c--attempt-001-answer-holes-unresolved](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-ea1-4c9c589db930a12238afcf87908511fd6e35ebc908f2a571d090d988a0342f5c--attempt-001-answer-holes-unresolved.edn)

Finding SHA-256: `2ece1d8fb96de2ca2acd405e19bbcd3732faae0bc0cd15938cecc66dd0f5b068`

## Completion criteria

- The reported behaviour is reproduced at the named stage — check: a reproduction (or an explicit record of missing reproduction inputs, as the scoped task allows) of a selected cascade held in the open-cascade phase at :cascade-resume, citing the finding record's inputs. **Not started.**
- The cause is corrected — check: a futon2 commit fixing the cause, with the repair id in its message or the ticket. **Not started.**
- A retained regression check passes after the change — check: a test that detects the reported failure (fails on the old code) and passes after; scoped validation output recorded. **Not started.**
- The ticket passes ordinary ticket review — check: a review verdict recorded against the ticket's evidence; only then marked DONE (the ticket's own acceptance rule). **Not started.**
