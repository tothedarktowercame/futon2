# E-kimi-task-81 — JOIN-TERM-D: whether each declared edge's crediting field carries its term (PROOF-2a <2>2b, discovery)

**Requisition:** completed — 2026-09-27T01:11:39Z, job invoke-1790470838887-25161-f0a166c6, state done

Clocked in by claude-9 for kimi-8 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# JOIN-TERM-D — does the field that credits a "declared" edge carry the edge's term? (DISCOVERY ONLY)

**Context.** The registry/map join (futon3c `holes/labs/M-wm-wiring/spike/wm_vs_equation_dag.bb`, output `wm-vs-equation-dag.edn` at 090658ae) classes an edge `:declared` when ANY map field runs from a box of the source node to a box of the importer node (`:fields-by-var`). It never checks that the field carries the edge's term (`:symbols`). claude-9 found two false credits: [CTAU-TOKEN R5] C_τ credited by `[:want {:record :cascade-spec}]`; [R6 R16] π/Q(π) credited only by `[:beta {:record :precision}]`. ⟨2⟩2b's acceptance counts `:declared`, so this decides what that count proves.

Produce `futon3c/holes/labs/M-wm-wiring/JOIN-TERM-D.md` (+ `.edn`, one entry per edge):
1. For each of the 10 `:declared` edges: each crediting field, and whether it carries the edge's term (read the writer and reader at their sites; say how you know). Verdict per edge: `:carries`, `:does-not-carry` (a false credit), or `:cannot-tell` (why).
2. For the false credits: is there a field that does carry the term (declared elsewhere, or declarable)? Or is the edge really unwired at map level?
3. A rule the join could apply mechanically: e.g. a declared correspondence from equation symbols to map fields (where would it live — registry row, map box, a separate table?), with the failure behaviour when a correspondence is missing. Sketch it against the 10 edges and say which would pass. Do not implement it.
4. Whether the same weakness affects the wire ledger's evidence (`wm-wire-ledger.edn` is per field, so probably not; confirm).

Read only. Do not edit the join, map, registry, ledger or plan; codex-1 is editing the registry right now (RC-2B-I). Commit only your two files.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
