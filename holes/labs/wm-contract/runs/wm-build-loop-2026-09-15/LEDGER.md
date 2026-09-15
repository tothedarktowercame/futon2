# War Machine build loop — ledger

Lead: claude-2. Specifications and design decisions: codex-28. Implementers:
codex-2, codex-3 and codex-4. Backstop: Joe. Commission:
`invoke-1789501050276-21222-59c12ada` (2026-09-15). Each packet reports its
source, tests, independent review, serving activation and qualifying use
separately. A tick in this ledger is not a checklist tick.

| Packet | Checklist / nodes | Author | Reviewer | Agency job | State | Evidence |
|---|---|---|---|---|---|---|
| P0 reproduce the target-belief gap | WM-02 Q2/Q4, R1 | claude-2 | n/a (read-only check) | none (in-turn) | **done** | `p1-reproducer/` (readback, pins, futon2 head `93837a1e`) |
| P1 work-target belief reaches row 7 | WM-02 Q1–Q9, R1 | codex-2 (proposed) | claude-2 | not dispatched | **blocked** on D1–D3 | `P1-packet.md` |
| P2 dynamics admission separate from A admission | WM-05 C5/C6, WM-03 B7 | TBD | claude-2 | not dispatched | proposed; needs D5 | none yet |
| P3 same B at belief update and prediction | WM-03 B8 | TBD | claude-2 | not dispatched | proposed; needs D4 | none yet |

## Open design questions sent to codex-28

- **D1** What a work target's state means (mission or ticket, on the
  seven-status carrier).
- **D2** Domain authority and declared D.
- **D3** Update authority for work targets before WM-04.
- **D4** Same B at the belief update and at prediction (WM-03 B8).
- **D5** Dynamics admission separate from observation admission.

Full text: this directory's `P1-packet.md` and the bell reply.

## Findings recorded, not yet assigned

- `war_machine.clj` `previous-selection-non-progress?` reads nil target
  belief as "did not move". This affects non-progress decay in the controller
  score. It is an existing consumer of an absent value.
- `bootstrap-from-stack-annotations` swallows read failures into an empty
  section list.
- The row-7 reader gives the same refusal for an unknown id and for a
  registry target that has no row.
