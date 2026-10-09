# E-kimi-task-82 — LEAN-DAG-G1-I: an import counts only if the reached module declares the term (PROOF-2a <2>1b G1)

**Requisition:** completed — 2026-09-27T01:15:02Z, job invoke-1790471039215-25165-f6b00157, state done

**VERDICT (2026-10-09, provisional):** DONE — Header states the requisition is completed with job id and timestamp. _(WM status classification by zai-5, high confidence; not yet confirmed by the author.)_

Clocked in by claude-9 for kimi-5 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# LEAN-DAG-G1-I — an import counts only if the reached source module actually declares the term

**Context.** p4ng `empirics-futon/gen_lean_dag.bb` classes a registry edge `:imported` when some consumer module at the importer's node reaches, by import closure, some module placed at the source node (`classify-term`, the `(some … closure …)` branch; `:via` lists the reached source modules). It does not check that the reached module declares (or binds, per the G0 hints) the term. This is the plan's G1, queued 2026-09-26 13:13Z and never done (PROOF-2a-PLAN ⟨2⟩1b: "a module reached that does not declare the term should not count as imported"). Two live instances:
- [R6 R4] `:r` reads imported `:via [PolicyPosteriorAtMachine]` only (futon2 `proof2/packets/W9-D.md` §2): the posterior module does not declare the containment order.
- mathlib4 b79b4ab666 (new `Proof2/PrefixFreeEnergyPosterior.lean`) plus a new R6 registry row would make [R2 R6] `interp` imported `:via ObservationAtMachine`, which declares the observation law, not the interpretation (W11-D, W9-D §4).

**One behaviour.** For `:imported`, require at least one `:via` module to declare the source term: the source row's `:lean` target (its declaration's owner module is in `:via`), or for an exogenous term the G0 `:lean-term` binder/declaration in that module. When the closure reaches source modules but none declares the term, the term is NOT imported: fall through to the existing parametric/absent classification, and record `:reached-without-term [modules]` on the term so the figure/EDN shows why. Keep `:via-shared-module-only` behaviour. Put the rule in the script header's method notes.

**Tests** (`empirics-futon/test_gen_lean_dag.sh`, fixture trees as the existing cases): (1) the bad case: consumer imports a module at the source node that does not declare the term → not `:imported`, `:reached-without-term` set; (2) consumer imports the declaring module → `:imported` unchanged; (3) exogenous term with a `:lean-term` hint declared in the reached module → `:imported`. All existing cases still pass.

**Acceptance on the real inputs** (run to a scratch AIF_OUT with trailing slash + `empirics-futon/` subdir; registry via `git -C /home/joe/code/futon2 show HEAD:holes/labs/wm-contract/aif-equations.edn` into a file; do NOT write the committed p4ng outputs): report every edge whose class changes against p4ng bc4e22c's `empirics-futon/aif-lean-dag.edn`, with each term's `:via` and why no reached module declares it. Expected at least [R6 R4] `:r`. For each moved edge, say whether the demotion is right. Then re-run the W11-2 trial (the new R6 row as codex-1 used it — text in futon2 PROOF-2a-PLAN ⟨2⟩1d LOG, "W11-2-LEAN") and confirm [R8 R6] imported and [R2 R6] NOT imported.

Gates: clj-kondo (no new warnings; copy to .clj for check-parens), all test cases ok. Commit only `gen_lean_dag.bb` and `test_gen_lean_dag.sh` in p4ng (other files in p4ng's tree are another lane's uncommitted work: do not stage them). Do not regenerate the committed outputs; claude-9 does that with the registry change.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
