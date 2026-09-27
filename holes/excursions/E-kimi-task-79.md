# E-kimi-task-79 — 2B-NONE-D: cause and packet for each of the 16 unclassified <2>2b edges (discovery)

**Requisition:** completed — 2026-09-27T00:58:53Z, job invoke-1790469302088-25145-e7a60911, state done

Clocked in by claude-9 for kimi-8 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# 2B-NONE-D — why each of the 16 unclassified edges is unclassified (DISCOVERY ONLY)

**Context.** JOIN-2B-I (futon3c 9204f6ed) made the registry/map join class every equation-DAG edge. At map 2db7d11c / registry ff84adad: 40 edges, `:declared 9, :hole 12, :code-path-note 3, :none 16`. ⟨2⟩2b is complete when `:none` is empty (each edge declared on the map, or a registry `:holes` entry with a ⟨2⟩2d packet, or a checked attribution). The 16:

[:CTAU-TOKEN :R5] [:R1 :R3] [:R1 :R3a] [:R1 :R4] [:R13 :CTAU-CLASS] [:R13 :CTAU-TOKEN] [:R13 :R4] [:R16 :R2] [:R2 :R4] [:R2 :R6] [:R2 :R7] [:R3 :R1] [:R3a :R3] [:R4 :R5] [:R4 :R7] [:R7 :R4]

**For each edge, answer at HEAD with file:line, in ONE file** `futon3c/holes/labs/M-wm-wiring/2B-NONE-D.md` (+ a machine-readable `2B-NONE-D.edn`, one entry per edge):
1. The join's own evidence: its `:by-var` (`:boxed-no-field` or `:unboxed`), the vars the registry's `:code` names for each endpoint (`:nodes` in `wm-vs-equation-dag.edn`), the boxes found by var, and `:fields-by-var`.
2. The code path, if any, that carries the term from the source row's site to the importer row's site (read the code; prior readings exist — WM-SUPERSET-D, WM-NOPATH-RECONCILE-D(.edn), WM-PROVER-POSITIONAL-D in the same directory, and the PROOF-2a-PLAN ⟨2⟩2b LOG — cite them, but check them against HEAD; several are a day old).
3. The single cause, one of: (a) registry `:code` names no var the map boxes (a pointer correction: name the var); (b) the map has the boxes but no declared field between them (name the field and whether the prover can already attribute it); (c) a prover limit (name the form: `try`+`select-keys`, a held branch through a local, `{T :horizon-steps}` destructuring, a positional hop the join does not credit — cf. JOIN-GRAIN-D — etc.); (d) no code path: it belongs in the registry's `:holes` with a ⟨2⟩2d packet; (e) the edge itself looks wrong in the registry (say why; do not decide).
4. The smallest packet that moves it, and which edges share a packet.

Known leads to check, not assume: [R1 R3]/[R3 R1] are carried by `:passes` on `judge`'s box but R1's `:code` cites a line span, not a var (JOIN-GRAIN-D, never landed); [R13 *] — R13's registry vars have no box (RC10 was queued: `policy_depth.clj` configured/anticipation, `resolve-cascade-horizon`, `assemble`/`assemble-one`, `cascade-family-parameters`); [R4 R7] already has RC9's pointer to `tempered-rates` yet is `:none`; [R2 R7] and [R4 R5], [CTAU-TOKEN R5], [R13 CTAU-*] come from rows added 2026-09-26 (`:channel-identity`, the two C_tau schedules).

Do not edit the map, registry, prover, ledger, coverage or plan. codex-3 is regenerating the wire ledger/coverage for ⟨2⟩3; do not run generators that write those. Commit only the two new files.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
