# WM-04 provenance and occurrence packet, revision: claude-3 review 2

**Reviewer verdict: ACCEPT the revised bounded DEP1/DEP2 packet** at futon2 `8beb626c`, together with the owner-assessment addition in `b4d9f746`. Every correction required by review 1 (`1b1e96b6`) is incorporated accurately. The executable readback now asserts the execution join, and my rerun reproduces all three outputs byte for byte. Three wording points remain (below); none changes a conclusion, so none blocks acceptance.

Requested by codex-8 (bell `invoke-1789574590634-21515-cef1c52c`). I did not repeat the source discovery.

## What I checked

1. **Diff scope.** `8beb626c` changes 9 files, all under this run directory. `b4d9f746` adds `OWNER-ASSESSMENT.md` and `owner-assessment.json`, plus 18 lines in `REPORT.md`. No source, DAG or checklist change.
2. **Corrections against review 1:**
   - **C1 (wake payloads attributed to joe):** present.
   - **C2 (timestamps):** the parent's `21:11:16.58Z` is added; all four times are preserved, and 21:10 is called an approximate summary time.
   - **Edited-quote caveat:** present, and it names the 1–2-before-10–20 instruction.
   - **Execution join:** the opportunity, execution, phase times, source and enacted target all match my findings. The report correctly says F11's times and outcome cannot be used for the shared-memory target, and it calls the `:authenticated-operator-task-pin` classification the record's own claim, not new authentication.
   - **Belief-domain mismatch (D2):** present, and carried into item 4.
   - **Migration and backup (D3):** present.
   - **Contract question 2:** replaced by the population decision, as review 1 suggested.
3. **Readback rerun.** I ran `bb readback.clj` with its output directory redirected to `/tmp`; it exits 0 with both success lines. `trace-readback.edn`, `policy-occurrences.edn` and `execution-join.edn` are byte-identical to the retained files. The new assertions compare the right things:
   - the ordinary decision without `:softmax-weights` equals the trace decision without `:softmax-weights-by-candidate-id`;
   - both weight multisets have 148 values and are equal;
   - the backup decision equals the migrated one;
   - the ordinary target is shared-memory, and the enacted and brief targets are both F11.
4. **Gates.** clj-kondo 0 errors and 0 warnings; check-parens OK, exit 0.
5. **Pins.** All 4 entries in `revision-source-pins.json` match at review time. The phases log's mtime is 2026-09-15 16:42:38, equal to the pinned `mtime_ns`, so it has not been appended since pinning. The log is append-only, though, so a later whole-file hash check will fail even though lines 6833–6869 stay the same.
6. **Owner material.** `OWNER-ASSESSMENT.md` withdraws the owner's earlier "authenticated origin" and single-effective-time claims, which agrees with C1 and C2. It accepts the displaced-recommendation account and keeps sample adequacy, stopping authority and WM-12/outcome-domain dependencies open. The report correctly declines to adopt the owner's global zero-count claim.

## Remaining wording points (non-blocking)

- **W1.** The report says the join was found "through the phases log and morning-brief item, which is outside those globbed roots". The phases log `data/wm-full-loop-phases.edn.log` *was* one of the 14 searched roots; only the morning-brief item was outside them. The search missed the join because the click's times differ from the trace timestamp by 0.6 s, so a literal-string search could never match.
- **W2.** "The same session has 17 user-role turns attributed to joe" counts the retained 79-record query window (20:00–00:00Z), not the whole session.
- **W3.** Because the phases log is append-only (point 5), the note should cite the line range and content as the stable reference, not the whole-file hash.

## Limits

- Acceptance covers the packet's bounded claims: record retrieval and linkage, the displaced-selection account, and proposed contract questions. It does not cover authentication of the operator turn, admission of any evidence into a manifest, a population or eligibility rule, labels, A, or any checklist/DAG state.
- No store writes, clicks, shared-JVM loads, source edits or dispatch.
