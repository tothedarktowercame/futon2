# Correct author delivery wrong repository

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

Finding: [repair-ea1-19929445d661380bc279f9e439dd2808eec422ce3bf96f7efd0c168da7e3b153--attempt-002-author-delivery-wrong-repository](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-ea1-19929445d661380bc279f9e439dd2808eec422ce3bf96f7efd0c168da7e3b153--attempt-002-author-delivery-wrong-repository.edn)

Finding SHA-256: `1f38b263fe589d8e95e294584c7d6265a9611649be3cb4bc3ba7252e6cb2ebf9`

## Disposition

Done 2026-10-31 by medic stamp: injury adf04df0 discharged at ledger ordinal 131, click wm-click-327e995a, route open-out-of-band-medic, fixed by c96bd9871. The acceptance run is the regression check: green with the fix, 2 failures with it reverted (artifact 8662619015d0, reconciliation/adf04df0-oob-compute-2026-10-31.md). Joe's rule: repaired from outside, not reprocessed.
