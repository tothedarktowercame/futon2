# E-kimi-task-88 — PROVER-CB-D: provenance through a collection callback for [R2 R6]/[R2 R4] interp (PROOF-2a <2>2b, discovery)

**Requisition:** completed — 2026-09-27T02:06:12Z, job invoke-1790474125428-25223-392115a1, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition header and git commit both record the kimi job completed with state done; spot-check of task-98 found its deliverable (ready-delta-g) in src. _(WM status classification by zai-1, high confidence; not yet confirmed by the author.)_

Clocked in by claude-9 for kimi-8 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# PROVER-CB-D — provenance through a collection callback, and a candidate's patterns from the interpretations (DISCOVERY for MAP-2B-P6)

MAP-2B-P6 (codex-2, invoke-1790473969761-25219-39fc57fb; probe /tmp/map2b-p6/probe.clj, .log) stopped: at `interpretation_construction.clj:319`, `construction/containment-order` is called as `(mapv (fn [c] … (construction/containment-order c) …) (sort-by … (:family result)))`; arg 1 is a compiled candidate whose `:patterns` containment-order reads. `wiring/sym-sources` follows let/loop bindings, not callback parameters: `{:ok? false :why :unbound-symbol}`. Also `co-apply-kernel` is at `cascade_model_manifest.clj:375`, `order-use` in efe.clj.

Answer in `futon3c/holes/labs/M-wm-wiring/PROVER-CB-D.md`: (1) the full data path from the interpretations (`merge-published` → sources `:interpretations`) to the candidate `c`'s `:patterns` (every hop, file:line, and which are keyed reads, positional, or constructed); (2) the smallest checked prover rule(s) that would attribute the callback hop (`mapv`/`map`/`reduce` over a collection whose provenance is known, the fn's parameter bound to an element) — with the bad cases it must refuse (a callback over an unrelated collection; a parameter shadowed); (3) whether the candidate's construction from the interpretations is attributable by existing rules or needs a second rule; (4) the same for the path into R4's kernel (`co-apply-kernel`/`order-use`); (5) the packets, prover first. Read only; commit only the note.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
