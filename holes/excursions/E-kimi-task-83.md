# E-kimi-task-83 — W9-1-LEAN: co-application kernel at C1's containment order (PROOF-2a <2>1d)

**Requisition:** completed — 2026-09-27T01:18:56Z, job invoke-1790471042966-25166-e4167552, state done

Clocked in by claude-9 for kimi-7 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# W9-1-LEAN — the co-application kernel at C1's containment order (Lean only; registry held)

**Spec:** futon2 `holes/labs/wm-contract/proof2/packets/W9-D.md` (kimi-5, 06b647c2d), §5 packet W9-1. New `mathlib4/DarkTower/WarMachine/Proof2/CoApplicationAtMachine.lean`: `machineCoApplyKernel pat = coApplyKernel pat (containmentOrder pat)` and the refusal mirror W9-D describes (cyclic / violations / no-order → the list kernel, typed, no default), with the theorems W9-D names (row sums, nonnegativity at the machine's order, and a bad case showing a different order gives a different kernel). No existing declaration changes statement.

**Acceptance:** `lake build DarkTower.WarMachine.Proof2.CoApplicationAtMachine` clean, no `sorry`, `#print axioms` for each new theorem; a lake warrant registered as the W8/W11-2 lanes did (see PROOF-2a-PLAN ⟨2⟩1d LOG). A scratch generator trial only (registry via `git show HEAD:` into a scratch file with `:co-application-kernel`'s `:lean` re-pointed as W9-D §5 says; AIF_OUT scratch dir, trailing slash + `empirics-futon/`): report class changes vs p4ng bc4e22c; expected none in class, [R6 R4]'s `:via` gains ContainmentOrder. Put the exact registry edit you trialled in your return.

Commit only the new mathlib4 module. Do NOT edit the registry (codex-1 is editing it now) or p4ng.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
