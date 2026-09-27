# E-kimi-task-80 — W9-D: co-application kernel at C1's containment order, the honest closer for [R2 R6] (PROOF-2a <2>1d, discovery)

**Requisition:** completed — 2026-09-27T01:03:07Z, job invoke-1790470281794-25156-c56d8bf6, state done

Clocked in by claude-9 for kimi-5 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# W9-D — the co-application kernel at C1's containment order (DISCOVERY ONLY)

**Goal.** In the Lean dependency map (p4ng bc4e22c) the registry edge **[R2 R6] `interp`** is term-present-not-imported: `:containment-order` (R6, `Proof2.ContainmentOrder.containmentOrder`, parameter `pat`) takes the interpreted patterns as a parameter and nothing instantiates it at the machine's interpretations. The plan names the closer W9 (PROOF-2a-PLAN ⟨2⟩1d, the W4 log entry): "the co-application kernel at C1's order": `:co-application-kernel`'s module does not import `ContainmentOrder`, so `:r` enters R4 only at node grain. Related: the registry's `:interp` placement note (futon2 `aif-equations.edn`, the `:interp` exogenous entry: R2 vs R6 placement is "recorded, not decided"). Also: W11-D (futon2 `proof2/packets/W11-D.md` §2) found that its layout would make [R2 R6] blue INCIDENTALLY (via ObservationAtMachine); W9 must say what the honest closer is, so that blue is either earned or refused.

Produce ONE file `futon2/holes/labs/wm-contract/proof2/packets/W9-D.md`:
1. The rows involved (`:containment-order`, `:co-application-kernel`, `:interp`), their `:lean`, `:imports`, `:formal`, and the Lean declarations they name, with file:line in mathlib4 HEAD.
2. What "the machine's interpretations" are as a Lean term: is there a machine-side declaration (an `…AtMachine` module) that supplies `pat`/interp from the observation (R2) side, or must one be written? What the code does (futon2: the interpretations from `want-interpretation`/`agency-answer-fn`, the containment order's producer).
3. The proposed module(s): what is instantiated at what, which registry rows are re-pointed (row, old → new), and the predicted class of every edge that moves (you may trial the generator against scratch copies as W11-D did — never write the real p4ng outputs, never read the real registry working copy: use `git show HEAD:`). Say explicitly whether [R2 R6] becomes imported by a genuine use of the interpretation, and whether [R6 R4] `:r` stops being node-grain only.
4. Whether W9 and W11-2 (the new `PrefixFreeEnergyPosterior` module + a new R6 row, being built now) interact.
5. The implementation packet(s), one module each, with `lake build` warrants.

Commit only W9-D.md.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
