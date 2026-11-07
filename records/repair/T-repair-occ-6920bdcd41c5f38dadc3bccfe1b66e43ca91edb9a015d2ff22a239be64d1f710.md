# Verify or restore abstained

**Status:** OPEN

## Observed failure

Target: {:absent :outer-task-not-selected}; stage: :selection.

Context: {:phase :selection}.

War Machine selection produced no addressable action

## Scoped task

Verify the reported precondition and restore it if it remains unavailable. If it has cleared, retain a dated recheck showing the current condition.

## Acceptance criteria

- A dated precondition check with its source and result is retained. Check: dated source and result recorded in this ticket. **Not started.**
- Restoration evidence, or a dated recheck showing the condition cleared, is retained. Check: restoration evidence or dated cleared recheck. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-6920bdcd41c5f38dadc3bccfe1b66e43ca91edb9a015d2ff22a239be64d1f710](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-6920bdcd41c5f38dadc3bccfe1b66e43ca91edb9a015d2ff22a239be64d1f710.edn)

Finding SHA-256: `04fd141573c1e039c984cf4bfec684771dcea3b1ca91a8b76af08ec6481061f9`
