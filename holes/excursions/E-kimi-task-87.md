# E-kimi-task-87 — P-inst-Pi: the Lean consumer of [R7 R3] Pi at the machine's channel precision (PROOF-2a <2>1)

**Requisition:** completed — 2026-09-27T01:48:36Z, job invoke-1790472794004-25196-cc9f0f1e, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition header and git commit both record the kimi job completed with state done; spot-check of task-98 found its deliverable (ready-delta-g) in src. _(WM status classification by zai-1, high confidence; not yet confirmed by the author.)_

Clocked in by claude-9 for kimi-8 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# P-inst-Pi — make the Lean consumer of [R7 R3] Π actually use the machine's source term (⟨2⟩1)

**Spec:** futon2 `holes/labs/wm-contract/proof2/packets/G1-DEMOTIONS-D.md` (kimi-5, a590fe5c0), section "(p) [R7→R3] `:Pi`" and the §2 row P-inst-Pi. The Lean map generator (p4ng `empirics-futon/gen_lean_dag.bb`, now with the G1 rule: an import counts only if a reached source module DECLARES the term; and the least-imported edge class) reports this edge not imported because the consumer row's module never reaches the module declaring the source row's term.

**Change:** the smallest honest closer — preferably ONE new mathlib4 module under `DarkTower/WarMachine/Proof2/` that instantiates the consumer's parametric declaration at the machine's source declaration (as W1/W5/W8/W9-1 did), with the equality theorem, typed absences carried (no defaults), and a bad case showing a different source value changes the consumer's value. If the note's alternative (a registry re-point) is the honest closer instead, say why and do NOT edit the registry: list the exact edit for claude-9 (codex-1 is editing the registry now).

**Acceptance:** `lake build` of the new module clean, no `sorry`, `#print axioms` listed, lake warrant registered (as W8/W9-1 did). Scratch generator trial only (AIF_OUT scratch dir with trailing slash + `empirics-futon/`; registry via `git -C /home/joe/code/futon2 show HEAD:holes/labs/wm-contract/aif-equations.edn` plus, in the scratch copy only, whatever re-point placing your module requires): report every class change against p4ng HEAD's `empirics-futon/aif-lean-dag.edn`; expected [R7 R3] Π → imported and nothing else, and give the exact registry edit your trial used. Commit only your new mathlib4 file.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
