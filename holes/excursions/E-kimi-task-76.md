# E-kimi-task-76 — ARENA-D: scope the reader-complete arena absence packet (PROOF-2a <2>2d, discovery)

**Requisition:** in-progress — dispatched 2026-09-27T00:27:19Z to kimi-7 as invoke-1790468839898-25134-3d270662

Clocked in by claude-9 for kimi-7 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# ARENA-D — scope the reader-complete arena absence packet (DISCOVERY ONLY, no code change)

**Goal.** LP-A-D (futon2 `holes/labs/wm-contract/proof2/packets/LP-A-D.md`, §3 "Arena reachability and consumers" and the bullet "Arena zero/missing likelihood") found two real witnesses in the arena belief update, `futon2/src/futon2/aif/belief.clj`: a sparse-valid A whose missing cell is read as 0, and a dense impossible observation whose zero total `normalise` (≈ lines 51-60) turns into a uniform distribution. LP-A-D says the fix is NOT an isolated `normalise` edit: the entity update's outcome, the health/prediction aggregation, the carry/provenance and the trace must change together, shipped-A output must stay identical, and "C9's existing malformed-model throw contract" must be preserved, with the missing-cell classification placed relative to it by explicit agreement.

Write the spec that implementation packet needs. Produce ONE file, `futon2/holes/labs/wm-contract/proof2/packets/ARENA-D.md`, answering with file:line at futon2 HEAD:

1. Every production caller of `update-step` / `normalise` (and any other belief.clj entry that can hit the missing-cell or zero-total path), and which of them are on the live tick path (`scripts/futon2/report/war_machine.clj` `judge`, the arena belief events, `apply-arena-belief-events`).
2. Where a missing A cell is currently defaulted (the validator's missing-cell default LP-A-D mentions) and what C9's malformed-model throw is (find C9 in the plan / packets / code): which inputs throw today, which are silently defaulted.
3. The typed outcomes you propose for (a) a missing cell and (b) a zero total, as data shapes, and where each is decided relative to C9's throw (before, after, or instead), with the reason.
4. Every downstream reader that would receive the new outcome: the entity update result, the health/prediction aggregation, the belief carry (`reconcile-belief-carry`?), provenance (war_machine ≈1049-1050 arena provenance), the trace record — and for each, what it must do with an absent entity (no uniform, no previous-belief substitution, no fabricated numeric health).
5. The test list for the implementation: the two witnesses through the REAL validator/filter, fed to the next reader and round-tripped through the trace; the shipped-A healthy output byte-identical (name the existing test/fixture that pins it, or say none exists); the C9 throw cases unchanged.
6. An estimate of the implementation's size and whether it should itself be split (e.g. entity outcome first, readers second).

Do not change code, the registry, the map or the plan. Commit only ARENA-D.md.
## Common terms (claude-9, PROOF-2a, 2026-09-27)

You are working one packet of PROOF-2a (plan: futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md). Joe directed claude-9 to start the ⟨2⟩1 / ⟨2⟩2b / ⟨2⟩2d residuals that can be done now; other seats are working ⟨2⟩3 concurrently in the same trees.

- Shared trees: commit ONLY your own paths/hunks (`git commit -- <paths>`, never `git add -A`/`commit -a`). futon2's `holes/labs/wm-contract/aif-equations.edn` currently has ANOTHER lane's uncommitted edit: do not stage it, do not overwrite it. If you must read the registry, read `git show HEAD:holes/labs/wm-contract/aif-equations.edn`.
- Do not edit PROOF-2a-PLAN.md; claude-9 logs your return there.
- Do not `load-file` anything into the shared JVM. Do not restart anything.
- Gates for any code you change: clj-kondo (no new warnings), `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` on Clojure (copy a `.bb` to a `.clj` name first, the checker ignores `.bb`), and the narrowest relevant tests, registered as a test-registry warrant where a Clojure test namespace exists (`clojure -M -m futon3c.test-registry.validation register <spec.edn>`, artifact-dir /home/joe/code/storage/test-registry/).
- A check/guard you add: construct the bad case it is named for and show it caught.
- When finished, bell claude-9 back (`--from <you> --to claude-9`) with: a summary, commit shas, warrant ids, and anything you found that the packet's premise got wrong.
