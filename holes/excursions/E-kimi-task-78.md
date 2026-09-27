# E-kimi-task-78 — JOIN-2B-I: registry/map join reports each edge's <2>2b inventory class (PROOF-2a <2>2b)

**Requisition:** completed — 2026-09-27T00:33:37Z, job invoke-1790468845444-25136-2f128c8e, state done

Clocked in by claude-9 for kimi-8 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# JOIN-2B-I — the registry/map join says which ⟨2⟩2b class each edge is in

**Goal.** ⟨2⟩2b's acceptance (PROOF-2a-PLAN, restated 13:46Z): every equation-DAG edge is declared on the map, OR recorded in the registry's `:holes` as `:not-realised`/`:path-dependent` (with a ⟨2⟩2d packet queued), OR attributed by a checked positional rule; "the join reports 0 edges in no class". Nothing computes that today. The join is `futon3c/holes/labs/M-wm-wiring/spike/wm_vs_equation_dag.bb` (usage `bb wm_vs_equation_dag.bb [map-rev] > out.edn`); its committed output `wm-vs-equation-dag.edn` dates from 2026-09-26 11:13 (31 edges). Run read-only at HEAD today it reports 40 edges, by var 9 `:declared` / 23 `:boxed-no-field` / 8 `:unboxed`.

**Change (one script, one behaviour):**
1. Read the registry from a revision, like the map: add an optional second argument / env `REGISTRY_REV` read via `git -C /home/joe/code/futon2 show <rev>:holes/labs/wm-contract/aif-equations.edn`, and record both revisions in the output. (The working-copy registry currently carries another lane's uncommitted edit; publication runs must not read it.)
2. For each edge add `:inventory` ∈ `:declared` (by-var `:declared`) | `:hole` (the edge appears in the registry's top-level `:holes`, under `:edge` or `:edges`; carry that entry's `:status`) | `:code-path-note` (the importer row records that the term reaches it through another row's update — find how RC7+RC8, futon2 9c4cc59a, recorded this on `:belief-state` for [R2 R1] [R16 R1] [R4 R1], and match exactly that field; do not match on prose) | `:none`. Declared wins over hole.
3. `:summary-inventory` counts, and `:inventory-none` the sorted list of `:none` edges.
4. Regenerate `wm-vs-equation-dag.edn` from committed map HEAD and registry HEAD, and commit it.

**Tests.** A test (follow `spike/wm_coverage_file_test.clj`'s pattern, or a bb test beside the script) on small fixtures: each class produced; an edge both declared and in `:holes` counts `:declared`; a `:holes` entry naming an edge not in the DAG is reported, not silently ignored; the bad case — a hole entry with a different edge — does not classify the edge as `:hole`. Warrant it if it is a Clojure test namespace.

Do not change the map, the registry, the prover or the plan. Commit only the script, its test and the regenerated EDN (futon3c).
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
