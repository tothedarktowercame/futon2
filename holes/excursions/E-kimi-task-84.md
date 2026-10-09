# E-kimi-task-84 — G1-DEMOTIONS-D: what closes each edge the declaration rule demoted (PROOF-2a <2>1, discovery)

**Requisition:** completed — 2026-09-27T01:32:22Z, job invoke-1790471807828-25177-9915ce56, state done

**VERDICT (2026-10-09, provisional):** DONE — Header states the requisition is completed with job id and timestamp. _(WM status classification by zai-5, high confidence; not yet confirmed by the author.)_

Clocked in by claude-9 for kimi-5 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# G1-DEMOTIONS-D — what closes each edge the declaration rule demoted (DISCOVERY ONLY)

**Context.** p4ng `gen_lean_dag.bb` now requires a reached source module to DECLARE the term before an edge counts `:imported` (G1: kimi-5 61aef7b; claude-9's any-definer fix on top — see `git log -2` in p4ng). On committed inputs (mathlib4 b79b4ab666, futon2 HEAD registry via `git show`) the census went 30/6/3 → 17/11/11 imported/parametric/absent of 40. Each demoted term carries `:reached-without-term [modules]`. Reproduce into a scratch dir (never the committed p4ng outputs): `AIF_EQ=<git show HEAD:… > file> AIF_OUT=/tmp/<dir>/ bb empirics-futon/gen_lean_dag.bb` with `/tmp/<dir>/empirics-futon/` created.

For EACH demoted edge, produce an entry in `futon2/holes/labs/wm-contract/proof2/packets/G1-DEMOTIONS-D.md` (+ `.edn`): the term, the reached module(s), the declaration the registry's source row names and its owner module, and the cause — one of:
- (h) an exogenous term with no `:lean`/`:lean-term` hint: nothing can declare it (A and B at R4, 10 terms). Name the Lean binder or declaration that IS the machine's A / B in each consumer (e.g. `tokenLikelihood`'s rates, `transition-row`), i.e. the `:lean-term` hint each importer row needs, checked in the Lean.
- (m) a registry/Lean mismatch: the row's `:lean` names one declaration while the Lean consumer actually uses another (lead: [R3a R7] ε — the registry's `:prediction-error` row names `sensoryPredictionError`, but W5's `machineChannelPrecision` is over the STATE prediction error from `StatePredictionErrorAtMachine`; which one does the `:precision` row's formal mean?).
- (p) the declaring module is not imported by the consumer: a real Lean gap (leads: [R5 R6] G — `MachineQ` not reached, only `Holes`/`OutcomeRiskKL`; [R7 R3] Π — only `AdjudicationCounts` reached, not `ChannelPrecisionAtMachine`; [R4 R5] Q(o|π) — `RolloutAtMachine` not reached).
- (w) closed by pending work: the π edges via `PolicyPosteriorAtMachine` (owner `MachinePolicySet`, W11-1's rebind, a decision now with Joe); [R6 R4] `:r` (W9-1-LEAN in flight).
Then the smallest packet per cause and which edges share one; for (h), the exact registry hints; for (p), whether the fix is an import, an instantiation module, or a re-point.

Read only. Do not edit mathlib4, the registry, p4ng or the plan. Commit only the two new files.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
