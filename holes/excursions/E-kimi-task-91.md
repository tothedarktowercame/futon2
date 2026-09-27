# E-kimi-task-91 — H13-D: discovery of what still stands between the Holes table and no open row (PROOF-2a <1>3)

**Requisition:** in-progress — dispatched 2026-09-27T02:14:48Z to kimi-1 as invoke-1790475288156-25235-1a99d621

Clocked in by claude-8 for kimi-1 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# H13-D: what still stands between the Holes table and "no open row" (⟨1⟩3 of PROOF-2a-PLAN) — discovery, read-only

H13-D (claude-8 → kimi-1), discovery under the coding-handoff protocol: read, measure, report; no source, registry, map or test change; one report file `futon2/holes/labs/wm-contract/H13-D.md` committed by explicit path. Joe opened ⟨1⟩3 as a line of work 2026-09-27 ~02:12Z. The step (PROOF-2a-PLAN.md ⟨1⟩3, line ~123): ACCEPT = every row of the Holes table is CLOSED or has a landed packet with a warrant; specifically (a) H-G-target part 2 ("the Lean module's own statement says it is owed"), (b) H-C-reach (the served-by rule beyond one mission), (c) the W_c checker's site (registry `:wc` is `:node nil` because the map's `:wc-checker` had no site), (d) H-publish's implementation. The Holes table is in `holes/labs/wm-contract/PROOF-2a-THEOREM-draft-2026-09-24.md` (rows from line ~544; the ownership table from ~700): read every row, not only the four named, and classify each as CLOSED / landed-with-warrant / OPEN with the evidence pin.

For each OPEN item, at futon2 / futon3c / mathlib4 HEAD (name the shas you read):
1. **What exists.** Commits, warrants (`futon3c/data/test-registry/namespace-ledger.edn` — find the evidence id by namespace), files, the statement in the Lean module for (a) (search DarkTower/WarMachine for the H-G-target module and quote the sentence that says part 2 is owed), the proposer/verifier split for (b) (H-C-REACH-I/I2/I3 landed; the proposer is the flight's read step — is it wired at HEAD? cite the flight_runner site or its absence), for (c) the map's `:wc-checker` box NOW has a site (futon3c 775aaa6d, `holes/labs/M-futon-seams/exemplar/proof2a_check.clj`) — so the registry's `:wc :node nil` and its `:node-absence` note are stale: say what node the symbol belongs to (which registry row consumes the verdict; `enactment_habit.clj:6-9` says it is input data to increment) and what edge it would contribute, as a proposed registry edit (do not make it); for (d) the H-PUBLISH-D census (68 resolutions: A 44 legacy, B 9, C 15) — recount at HEAD with the same method (say the script), and state what A1 (f38e20b2) closed and what remains for classes B and C; note that ⟨2⟩3 lane A witnessed `observe-publication-fn` reading a discharged obligation from a real run record (futon2 6d98d37c9), so the READ side exists — the question is the WRITE side.
2. **What closes it**, as one packet each: files, the test that pins it, the definition of done in one sentence, whether it needs a ruling from Joe (say what the yes/no question would be) or settles by definitions on the record.
3. **Estimated total LoC** per packet, in the plan's vocabulary (implementation + tests + declarations), and the seats that can safely run in parallel given shared files.
4. **Anything not yours**, unfixed: a row the table calls CLOSED whose evidence pin no longer holds at HEAD, for instance.

Gates: none to run (read-only) — but any number you report is computed by a command you name, never recalled. Commit the report by explicit path (`git status` first; never `git add -A`, never `--amend`, never `git stash`; other lanes' files untouched). No flight, no click, no load into :6768.
