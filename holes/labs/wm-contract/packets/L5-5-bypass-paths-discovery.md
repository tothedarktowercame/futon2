# L5-5 — Discovery: the two paths that bypass outer selection, as selections (claude-2 → claude-3, 2026-11-01)

**Discovery only: no code changes, no `data/` writes.** Deliverable:
`reconciliation/L5-5-bypass-paths-claude3-2026-11-01.md` committed on futon2 `main` (at `9b3914709` or
later), that file only.

## Why

One loop, two runs per click, each a RunFacts (plan). L5-4f found two paths that enter the inner run
without an outer selection and now write typed `:not-exercised` selection records: direct `:task`
entry (`full_loop_runtime.clj:1324`: the operator names the task) and the self-heal routes
(`full_loop_runtime.clj:1649`, arbitration). Joe's ruling 4 says selection is over tasks; an
operator-named task is a selection with a declared source, and a self-heal is the machine selecting
repair work, which the task-selection README's row 3 ("the repair click": repair-goes-to-the-front →
only-what-can-start-now → end-in-something-shown) describes as a cascade. The question is what each
would need to be an ordinary selection run that Lean can read.

## Questions (cite file:line)

1. **Direct task entry.** Where the operator's task arrives (CLI, API, click script), what is
   recorded about the choice today, and what a selection record for it would carry: the field (the
   same open set), a single-cascade menu `operator-declared-pick` with the declared source (who,
   when, the words), its G under the kernel (the pick's relation and reach as for any pick), the
   abstention floor, and Q2/Q11 consequences: a one-cascade menu fails Q2's floor of two for a
   selection run; say whether the honest reading is "a declared pick is a different kind of run"
   (a third `RunKind`) or "the operator's cascade competes with the library's and must win". Give
   the theory: an operator instruction is an observation with a declared source, not a prior over
   policies.
2. **Self-heal.** What triggers it, what it knows (the injury record, the repair obligation, the
   repaired target), and whether the README's repair-click cascade could run over the field with
   those facts: which steps are executable now (repair-goes-to-the-front needs an obligation fact per
   task; `open-repair-obligation` already classifies tickets in `focus_receipt.clj`), which are not.
3. **Records.** For each path: what the `:not-exercised` record carries today, and the diff to a
   full selection record.
4. **Readers.** Who reads the two paths' records (projector, alert, report card, futon3c).

## Gates

Read-only. Bell claude-2 with the report path and your recommendation for each path.
