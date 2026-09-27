# E-kimi-task-77 — W11-D: module layout closing the absent [R8 R6] F edge (PROOF-2a <2>1d, discovery)

**Requisition:** in-progress — dispatched 2026-09-27T00:27:21Z to kimi-5 as invoke-1790468842850-25135-7e402842

Clocked in by claude-9 for kimi-5 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# W11-D — module layout for the two absent F edges (DISCOVERY ONLY, no Lean committed)

**Goal.** In the Lean dependency map (p4ng `empirics-futon/gen_lean_dag.bb`, outputs committed at p4ng bc4e22c from mathlib4 8d98c63f97 and futon2 589b29ab4), the registry edge **[R8 R6] F_π is `:absent`**: `Proof2/PrefixFreeEnergyAtMachine.lean` (the R8 module, `prefixF`) imports `Proof2/PolicyPosteriorAtMachine.lean` (the R6 posterior module) — only, it appears, for `machineWeightsAtPrefixF` at the end of the file (≈ lines 395-480) — so the posterior cannot import the F module back. The plan names the closer W11 (PROOF-2a-PLAN ⟨2⟩1d, W7/W7b log entry): a cascade-typed policy set, "the P0 rebind of `:policy-set` (a second R6 module at `EnactmentHabit.PolicyKey` the prefix module imports for π while the posterior imports the prefix module for F)". `MachinePolicySet.lean`'s header marks it non-conformant (flat `Candidate`) and forbids binding G/EFE/selection statements to it.

Also in the same map: **[R6 R17] π** and **[R6 R8] π** — check whether the same cascade-typed policy set closes [R6 R17] (currently term-present-not-imported via `habitPrior`'s `menu`).

Leave [R8 R3] alone (whether F_π really enters the state belief update is a pending ruling).

Produce ONE file, `futon2/holes/labs/wm-contract/proof2/packets/W11-D.md`, stating:
1. The exact import graph today among PrefixFreeEnergyAtMachine, PolicyPosteriorAtMachine, ActionAtMachine, EnactmentHabit, MachinePolicySet, BeliefStepAtMachine (and anything else that imports the posterior), and which declarations each import is actually used for.
2. The proposed layout: which declarations move to which (new or existing) module, the new module name(s), and which registry rows' `:lean` / `:lean-at` must be re-pointed (row id, old → new), so that `gen_lean_dag.bb` classes [R8 R6] imported without regressing any edge. Read how the generator places modules at nodes (it resolves each row's `:lean` to its module; see `classify-term` and `node-modules`) and predict every edge's class after the change: list each edge that moves and confirm no other moves. You may trial the prediction by running the generator against scratch copies (`LEAN_ROOT=<scratch copy of mathlib4 with your trial files> AIF_EQ=<scratch registry> AIF_OUT=<scratch dir>/` — AIF_OUT needs a trailing slash and an `empirics-futon/` subdir); it reads source text and runs no Lean. Do not write in the real mathlib4 or p4ng trees.
3. Any cycle the layout creates, and whether any existing theorem would have to change statement (not just location).
4. The implementation packet(s) this implies, one module per packet if possible, with `lake build` warrant per module.

Commit only W11-D.md (futon2).
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
