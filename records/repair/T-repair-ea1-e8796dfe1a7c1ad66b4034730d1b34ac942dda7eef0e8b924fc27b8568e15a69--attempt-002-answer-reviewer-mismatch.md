# Correct answer reviewer mismatch

**Status:** DONE

## Observed failure

Target: not retained; stage: :cascade-resume.

Context: {:phase :cascade-resume}.

Selected cascade held in open-cascade phase

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance evidence

Retain a reproduction or regression check that detects the reported failure and passes after the change, together with the scoped validation output. Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-ea1-e8796dfe1a7c1ad66b4034730d1b34ac942dda7eef0e8b924fc27b8568e15a69--attempt-002-answer-reviewer-mismatch](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-ea1-e8796dfe1a7c1ad66b4034730d1b34ac942dda7eef0e8b924fc27b8568e15a69--attempt-002-answer-reviewer-mismatch.edn)

Finding SHA-256: `da341c2bdf809a35b37c6853edfd9f56674590f0cd0437615f4cc843e263a231`

## Disposition

Done 2026-10-31 by medic stamp: injury c309d796 discharged at ledger ordinal 139, click wm-click-2c528c44, route open-out-of-band-medic, fixed by 75b074801. The label :answer-reviewer-mismatch hid a real reviewer refusal, which now closes typed and goes through one objection round (a5b9bbd91). The acceptance run is the regression check: green with the fix, 6 failures with it reverted (reconciliation/c309d796-oob-compute-2026-10-31.md).
