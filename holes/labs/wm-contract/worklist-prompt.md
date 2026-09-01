# wm-edge worklist: one item per invocation

You are working through `futon2/holes/labs/wm-contract/worklist.edn`, the item
ledger of `futon2/holes/TN-edge-review-aif-wiring.md` (read §1 for the standard,
§6-§9 for the items' background). Do exactly ONE item, then stop.

1. Run `bb worklist_check.bb` in that directory. If it fails, fix the ledger
   only if the failure is a formatting error you introduced; otherwise stop and
   report.
2. Pick the FIRST item with `:status :open` whose `:class` is not `:J` and whose
   `:owner` is your lane (or any lane, if told so). Never take a `:J` item.
3. Do what its `:acceptance` says -- nothing more. Every claim you record must
   carry a `file:line` pointer, a run-record id, or "not found". Do not write a
   ruling: rulings go only into `aif-equations.edn :choices` or
   `control-map-edges.edn :decisions` by Joe. A `:decisions` entry you write
   records a CODE-BACKED correction (basis = pointers), not a preference.
4. Before committing: run `bash p4ng/empirics-futon/negative_controls.sh` and
   `bb p4ng/empirics-futon/pointer_check.bb`. Do NOT run `gen_aif_dag.bb` into a
   publish; regeneration from the registries happens after review (gate rule,
   TN §9a).
5. Commit with a message naming the item id. Set the row to
   `:status :done-unreviewed` and fill `:evidence` with the sha and the
   pointer(s). If blocked, set `:status :blocked` with `:blocker`. Commit the
   ledger. Run `bb worklist_check.bb` again.
6. Report in three lines: item id, what changed (sha), what the reviewer should
   check. Stop.

7. If the row carries `:loop-mode :one-slice-per-invocation` (a multi-slice item such as I1/I2), do
   the NEXT unfinished slice named in its `:statement`/`:progress`, append what you did to
   `:progress "<slice>: <sha>"`, and leave `:status :open` unless the last slice is done. A slice
   that touches src/ needs clj-kondo, check-parens and tests; a slice that runs the machine needs
   the pre-flight first and must hold `data/wm-trace/.run-lock` (RUN12) once it exists.
