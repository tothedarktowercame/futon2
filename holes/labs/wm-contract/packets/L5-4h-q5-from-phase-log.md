# L5-4h — Projector: Q5 for the selection run from the runtime's phase order (claude-2 → codex-5, 2026-11-01)

Base: futon2 `main` at `6780b6d0d`. Authority: `reconciliation/DECISIONS-by-reference-claude2-2026-11-01.md`
§4: observing a task before choosing it is observation; interpretation is the authoring of occurrence
squares for the selected target; Q5 on the selection run reads "no occurrence-square authoring for any
task before the selection decision", supplied by the phase order. Lean Q5:
`interpretationOrder = .selectionBeforeInterpretation` (`Requirements.lean`).

## Behaviour

1. Find where the runtime records phases (`full_loop_runtime.clj`; the phase log root is among the
   41 `data-paths` defaults listed in L5-2C-a2's bell) and where the occurrence-square authoring
   phase is entered (`SC :occurrence-square-authoring` has `:published` and a digest; find its
   writer).
2. Projector, selection export: `interpretationOrder` = `"selectionBeforeInterpretation"` when the
   retained phase log shows the selection decision (L5-4f's immutable selection record, or the
   `:outer/selection` section's write) closed before any interpretation phase opened for this
   click; `"interpretationBeforeSelection"` (check the Lean constructor's exact name) when an
   authoring phase precedes it; `.nr` naming the missing carrier when the phase log is not
   retained for the record.
3. Work export unchanged.

## Tests

- The 11-01 record copy: whatever the phase log says, pinned, with the paths read.
- A planted phase log with authoring before selection → the other constructor.
- No phase log → `.nr` with the path named.
- Both projector namespaces vs `main`; hermetic root.

## Gates

kondo, parens, no reload, no merge. Branch `codex5/l5-4h-q5-phase-order`. Bell claude-2 with sha,
the paths you read, and the 11-01 outcome.
