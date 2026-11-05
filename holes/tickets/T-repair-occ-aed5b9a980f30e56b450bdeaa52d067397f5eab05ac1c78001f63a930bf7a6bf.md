# Verify or restore aborted

**Status:** OPEN

## Observed failure

Target: not retained; stage: :selected-target-reading.

Context: {:phase :selected-target-reading}.

War Machine abstained or selected no addressable action

## Scoped task

Verify the reported precondition and restore it if it remains unavailable. If it has cleared, retain a dated recheck showing the current condition.

## Acceptance criteria

- A dated precondition check with its source and result is retained. Check: dated source and result recorded in this ticket. **Not started.**
- Restoration evidence, or a dated recheck showing the condition cleared, is retained. Check: restoration evidence or dated cleared recheck. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-aed5b9a980f30e56b450bdeaa52d067397f5eab05ac1c78001f63a930bf7a6bf](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-aed5b9a980f30e56b450bdeaa52d067397f5eab05ac1c78001f63a930bf7a6bf.edn)

Finding SHA-256: `fc1ad91570147c8816e9f5d83bebfa077bfd2f2207515fb7a4201710ca9a6efd`
