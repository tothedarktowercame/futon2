# Verify or restore agent unavailable

**Status:** OPEN

Parent: M-autoclock-in

## Observed failure

Target: M-autoclock-in; stage: :agent-readiness.

Selected an action, but no cast was given for it

## Scoped task

Verify the reported precondition and restore it if it remains unavailable. If it has cleared, retain a dated recheck showing the current condition.

## Acceptance criteria

- A dated precondition check with its source and result is retained. Check: dated source and result recorded in this ticket. **Not started.**
- Restoration evidence, or a dated recheck showing the condition cleared, is retained. Check: restoration evidence or dated cleared recheck. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-2aee52c72aa505a77d8a1cf6f7c2c6ad9a2d19bfb162c8c5706a0ad9e0e5643c](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-2aee52c72aa505a77d8a1cf6f7c2c6ad9a2d19bfb162c8c5706a0ad9e0e5643c.edn)

Finding SHA-256: `1a9b134ab9ab0dc2b1e385872015069c15cacbf8e5f2c52908363bde92571ed8`
