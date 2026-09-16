# WM-04 provenance and occurrence packet: claude-3 review

**Reviewer verdict: ACCEPT DEP1 (original-record retrieval) with two wording corrections. DO NOT ACCEPT the DEP2 occurrence account as written.** A record joining the September 12 selection to a specific click exists in futon2 `data/`, outside the searched `wm-full-loop*` roots. It shows that the selected action was **not enacted**: an operator task pin displaced it in that click. The report's absence statements are literally bounded and true, but the "missing execution join" and proposed contract question 2 should be replaced by this join before WM-04 uses the packet as shared input.

Subject: futon2 `b61f1a6d` (author codex-8), `REPORT.md` and retained evidence. Requested by codex-8 (bell `invoke-1789573999215-21493-fad4dd01`). The uncommitted `REPORT.md` addendum and `OWNER-PLANNING.md` in the working tree at review time are outside this review and were not staged.

## What I checked

1. **Record bytes.** I re-fetched the decision, the user turn, its reply parent and the session query from `:7070` myself. All four SHA-256 values equal the retained `*-fetch.json` values (`review-claude-3/refetch-sha256.txt`). The session query returns `count 79`, and it contains all three records. The query copy of the user turn equals the direct fetch.
2. **Readback.** I reran `readback.clj` with only its output directory redirected to `/tmp`. It exits 0, and `trace-readback.edn` and `policy-occurrences.edn` are byte-identical to the retained files.
3. **Pins.** I recomputed all 72 `source-pins.json` entries (size and SHA-256), with no drift. The 65 policy files plus 7 others (trace, resolver, observation validator, `claude-repl.el`, `http.clj`, two WM-05 documents) are all present.
4. **Searches.** I reran both `rg` commands over the same 14 roots. The exact timestamp gives 0 files (exit 1) and the policy gives 65 files. I also tried the epoch-ms form (`1789234089498`), the epoch-s form and `2026-09-12T17:28:09`, and all return 0.
5. **Gates.** clj-kondo on `readback.clj` gives 0 errors and 0 warnings; check-parens exits 0.

## DEP1: accepted, with corrections

The retrieved content matches the report. The decision `6e6f56a1…` records `decided-by joe`, `recorded-by claude-4`, subject mission `M-shared-memory-control-build-test`. The user turn `emacs-d200387d…` (author joe, role user, same session, `in-reply-to` the parent) contains the instruction literally. The parent `emacs-b8fe23c2…` names the mission and asks Joe to decide the enactment rung. The report correctly says that retrieval is not authentication.

- **C1. The author and role fields are attached to agent-written text in the same window.** Of the 17 `role user` turns in the retained 79-record query, all have `evidence/author joe`, and 8 are park-wake payloads beginning `WAKE (park N …)` written by agents. So these fields record what was entered into the REPL input, not who typed it. The quoted turn reads as operator speech and is not a wake payload, but that distinction rests on its content, not on the fields. The report's "available attribution" should say this.
- **C2. The timestamp discrepancy has a third point.** The parent assistant turn, which asks the question, is at `21:11:16.58Z`. So the summary's `decided-at 21:10Z` falls *before* the question was asked; it cannot be the decision instant. The nearest earlier user turn (`21:09:42`, "OK, so let's go back to the 'food' issue") is not the quoted text. The report preserves 21:10, 21:16:27 and 21:17:39 without choosing between them, which is right. It should add 21:11:16 and say that 21:10 is an approximate summary time.
- Minor: the decision's `quoted` field is an edited paraphrase of the turn ("…", "it is" for "it's"). It also omits "Sure, before we go to 10-20, we need to do 1-2", which is where the 1–2 runs rule actually comes from. The literal turn, not `quoted`, is the source to cite.
- Scope note: `evidence/subject` attaches the decision to this mission, but its text turns off the Phase 8 enactment rung for the War Machine generally. The report's "Exact mission subject" is accurate for the attachment. The report already says scope must follow content.

## DEP2: the execution join exists, and it says "not enacted"

The report says run/cohort/attempt/checkpoint binding and action start/completion are "not established" by the trace or the literal correlation search. That is true of that search. It is not true of the retained data.

**The click.** `data/wm-full-loop-phases.edn.log` lines 6833–6869 (`review-claude-3/phases-log-lines-6833-6869.txt`) record opportunity `duree-click-on-demand/2026-09-12T17:23:07.717766103Z/ed3826f3-…`, runner execution `ea1-418bb56e…--attempt-001`. Its phases, all in UTC:

| Phase | Time |
|---|---|
| selection | 17:26:16 → 17:27:26 |
| construction | 17:27:30 → 17:27:34 |
| author-dispatch | starts 17:28:10.105, 0.6 s after the trace record's `17:28:09.498` |
| delivery-qa | 17:34:21 |

**Its brief item.** `data/wm-morning-brief/items/ea1-418bb56e…--attempt-001.edn` holds `:selection-review :selection-reasons :ordinary-selector-decision`. I compared it with the trace record's `:decision` (`review-claude-3/join-probe.clj`, `.out`):
- Both maps have 26 keys, and 24 values are equal, including `:action` (target `M-shared-memory-control-build-test`) and `:selected-policy-id pi-s-9dbc2ceb…`.
- The remaining pair is the same softmax weights stored under two keys: `:softmax-weights`, keyed by action map in the item, and `:softmax-weights-by-candidate-id` in the trace. Both have 148 entries, and the sorted weight vectors are equal.

**What was enacted.** The same `:selection-reasons` has:
- `:source :authenticated-operator-task-pin`
- `:operator "Joe"`
- run config `RUN4-F11-production-successor-2026-09-12-v2/run-config.edn`
- `:enacted-candidate-action` target `M-f11-find-production-successor`

The item's `:selected-target`, the QA ref in the phases log, the author (codex-20, commit `98d0dcb1`) and the outcome (`:build-failed`, reviewer codex-18 rejected) all belong to M-f11. The QA ref cites decision `6e6f56a1…` among its evidence IDs.

**Meaning.** The September 12 record is the ordinary selector's recommendation inside the RUN4-F11 click. An operator task pin overrode it, and nothing was dispatched for `M-shared-memory-control-build-test` in that click. This is stronger than "no occurrence inferred": this selection has no action start, completion, close or disposition of its own to observe. As a design anchor it is a *displaced controller head*, not an unjoined execution.

Sources are pinned in `review-claude-3/join-source-pins.txt`; the data files are git-ignored.

### Further DEP2 findings

- **D2. Why the target-belief lookup returns nil.** `:mu-pre` and `:mu-post` each have 417 keys: 414 `arxana/stack/futon-v1/…` strings and 3 keywords. None names a mission (`absence-probe.out`). The lookup returns nil because this belief state has no mission-level coordinates, not because a target row was dropped. The contract question about state/observation relationship (item 4) should start from that.
- **D3. The pinned trace file was rewritten after the selection.** `wm-trace-2026-09-12.edn` has mtime 19:55; `wm-trace-2026-09-12.edn.pre-migration-backup` (mtime 17:28, unpinned) exists. Record 3's `:decision` is identical in both; the migration added `:accumulation-state` and `:accumulation-initialization`. The report pins and reads the migrated file without saying so.
- **D4. Search scope.** Only `data/wm-full-loop*` roots were searched, so the phases log (which was searched) was not connected to the morning-brief items (which were not). The two timestamps differ by 0.6 s, so a literal match could never have found the join. A time-window search over the phases log finds it directly.

## Proposed contract questions

- Item 1: keep, with C1 and C2 added.
- Item 2: replace "determine the exact producer of the execution/close context". The producer is known: the RUN4-F11 full-loop click, together with its brief item and phases log. The question for WM-04 becomes whether a displaced selector head can ever be an observation subject. If it cannot, a prospectively commissioned occurrence must be an enacted candidate, and it must retain the fields the report lists.
- Items 3–5: reasonable as stated, and correctly marked as proposed, not completed. Item 4 should note D2.

The packet's statements that no label, sample, observer, A, DAG or checklist change is made hold for the commit: its 19 files are all under the run directory.

## Limits

No evidence-store writes, no production click, no shared-JVM load, no source edits, no dispatch. I did not open the four trip reports that also name this opportunity. Authentication of the operator turn beyond the stored fields remains open, as the report says. I did not amend `REPORT.md`, because the correction changes its conclusions and it belongs to the author.
