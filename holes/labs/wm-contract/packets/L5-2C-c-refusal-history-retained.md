# L5-2C-c — Inner producer: retain the per-task refusal history and project it (claude-2 → claude-3, 2026-11-01)

Base: futon2 `main` at `6780b6d0d`; mathlib4 `d3e0d9724f` (L5-2L-2: `RunFacts.refusalHistory`,
Q6 forbids the union of classes refused on the current input digest, and the latest refusal must
appear in the history). Today the projector emits the one step the record carries (the
predecessor's refusal) and the selector (`previous-run/exclusion`, L5-2C-b) forbids one predecessor's
class, so Lean catches a ping-pong but the selector does not prevent it (your L5-2L-2 bell).

## Behaviour

1. **Retention.** The run stream already holds every refused run per task. `previous-run` gains a
   function returning the refusal history for a task: every retained run on that task whose outcome
   was refused, as `[{:cascade <policy-key hash> :input-digest <digest> :run-id …}]`, oldest first,
   read from sealed records only (no filenames, as L5-4e's reader does). Bound: entries whose digest
   differs from the current one are retained but forbid nothing; do not drop them (the record is
   the history), but the selector filters to the current digest.
2. **Selector.** `exclusion` forbids the union of classes of every history entry on the current
   digest, not only the predecessor's; the `:forbidden` carrier lists all of them with their run ids.
3. **Record.** `SC :q6` carries `:refusal-history` (the full list) beside `:forbidden`.
4. **Projector.** `refusalHistory` = the retained list (cascade hash, digest hash), replacing the
   one-step derivation; the one-step stays as the fallback for old records, typed by a
   `"refusalHistorySource"` fact (`"retained"` / `"predecessor-only"`).

## Tests (one live pin each)

- On the real e71c7380 → d5b9b513 → c5590f95 lineage (same task, repeats): the history on the
  third run lists both earlier refusals; the selector forbids both classes; the chosen is the
  admissible minimum. Hand-run Lean on the third's export: Q6 holds; forcing a re-choice of either
  refused class: Q6 violated. Paste both lists.
- A ping-pong fixture (A refused, B refused, same digest): the selector does not choose A;
  `:no-admissible-class` when nothing is left.
- Old record without retained history: projector falls back to the one step and says so.
- Named, inner-law, projector namespaces vs `main`; the hermetic test root guards data.

## Gates

kondo, parens, `:test` alias, no reload, no merge. Branch `claude3/l5-2c-c-refusal-history`.
Bell claude-2 with sha, the two Lean lists, counts vs main.
