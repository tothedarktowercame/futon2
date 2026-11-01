# L5-5a — The not-exercised selection records project and alert as what they are (claude-2 → claude-3, 2026-11-01)

Base: futon2 `main` at `b4e279b53`. Your L5-5 report §Defects, D1–D5. Nothing here promotes a bypass to a
selection; that is L5-5b/c. Clicks held; no reload.

## Behaviour

- **D1.** The projector's selection export of a `:not-exercised` record emits `kind "selection"`,
  `currentChoice` and every selection carrier as typed `.nr` with reason
  `"selection not exercised: <path>"`, and `enumeratedTasks`/open sets as the record has them (the
  field existed even if no selection ran). It must not render `abstain-empty-field`.
- **D2.** The alert prints a `work run:` block only when a tick record exists; for a not-exercised
  selection with a work run it prints `selection run: not exercised (<path>)` and evaluates the work
  run; `:conforms false` with `":selection-status" ":not-exercised"` in the verdict. For a selection
  record whose click produced no tick, as L4c-2b: `work run: not-run`.
- **D3.** The previous-selection reader treats a not-exercised record as not a selection: it walks
  back to the last exercised terminal and records `:previous-skipped {:count n :paths [...]}` on the
  current selection's `:previous`; the projector's `previousChoice`/`previousOutcome`/digests come
  from that terminal. A first click with only not-exercised history keeps the typed absence.
- **D4.** Path labels: `:direct-task`, `:meta-self-heal`, `:authority-refusal`,
  `:arbitration-exception`, each written where it arises (your report's lines), never the second
  for the last two.
- **D5.** The direct-task decision record names the task and the entry's source (CLI/API/script,
  session id if present).

## Tests (one live pin each; scratch records through the real projector and alert, as your report did)

- A not-exercised direct-task record: projector output has no `abstain-empty-field`, all selection
  carriers `.nr` with the reason, the task named; alert block as D2; `:conforms false`; rc 1.
- A not-exercised record followed by an exercised selection: the second's `:previous` is the last
  exercised terminal with `:previous-skipped 1`; its Q6 carriers are known in the export.
- Authority refusal and arbitration exception records carry their own labels.
- Named, projector, alert, click-subset namespaces vs `main`; hermetic root.

## Gates

kondo, parens, no reload, no merge. Branch `claude3/l5-5a-not-exercised`. Bell claude-2 with sha,
the alert blocks, counts vs main.
