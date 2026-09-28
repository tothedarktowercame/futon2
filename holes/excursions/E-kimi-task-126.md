# E-kimi-task-126 — CREEP-D: account of PROOF-2 sections complete then reopened (read-only)

**Requisition:** in-progress — dispatched 2026-09-28T20:44:52Z to kimi-2 as invoke-1790628292365-26258-b545e0a1

Clocked in by claude-1 for kimi-2 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# CREEP-D — when were the War Machine proof sections "complete", and what reopened them?

From claude-1 (PROOF-2a lead, taking over from claude-8). Bell claude-1 back with a summary. READ ONLY: no edits to any repository, no commits, no tests, nothing reloaded. Write only your report file.

## Why
Joe, 2026-09-28: "I had previously gotten most of the sections to be 'complete' so there was a clear case of mission creep here and I will want an account of that." Today's plan, `/home/joe/code/futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md`, has never had more than 5 of its 16 rows Completed (history checked by claude-1). So the "mostly complete" state Joe remembers is in an EARLIER document. Find it, and find what turned complete work back into open work.

## Documents to read, with their git history (futon2, `git log -p --follow -- <path>`)
All under `/home/joe/code/futon2/holes/labs/wm-contract/`:
- `PROOF-wm-works-2026-09-22.md`
- `CHECKLIST-fundamentals-2026-09-15.md` and `FUNDAMENTALS.edn`
- `PROOF-2-THEOREM-draft-2026-09-24.md`, `PROOF-2-STRATEGY-draft-2026-09-24.md`, `PROOF-2-ARCH-draft-2026-09-24.md`, `PROOF-2-ASSUME-draft-2026-09-24.md`
- `PROOF-2a-THEOREM-draft-2026-09-24.md` (note its 2026-09-26 commits: "ruling, the map complete and every wire verified before flights resume" 1c974bb28, "PROOF-2a stays; the wiring as an adjacency matrix" cc50f6d89, "the equations typeset; twelve rows are prose only" 630c0e8a4)
- `PROOF-2a-PLAN.md` (its "PREMISE, corrected 2026-09-26" paragraph)

## Questions
1. Which document(s) and which revision(s) showed most sections complete? Give the sha, date, and the list of sections with their status words at that revision. Quote the status text; do not paraphrase.
2. For each section that was complete then and is open now: the commit where it stopped being complete (sha, date, author/agent if the message or text names one), and the reason the text gives.
3. Classify each reopening as exactly one of:
   - **new-obligation**: a requirement not present when it was marked complete (e.g. a new layer of evidence, a new criterion), 
   - **stricter-bar**: the same requirement with a higher acceptance standard,
   - **defect-found**: the earlier completion was wrong (something claimed did not in fact work),
   - **restructure**: the same work re-filed under a new document or numbering, with no real change of state.
4. For each: who asked for it. Look for Joe's words quoted in the text ("Joe:", "Joe's order", "ruling", "Joe, 2026-09-2x"). Say "Joe (quoted: ...)" or "agent (name) without a Joe quote" or "not found".
5. Obligations added to PROOF-2a-PLAN.md after 2026-09-26 12:42Z that did not exist in any earlier document (e.g. ⟨2⟩3b second-layer wire tests, the M-warrant-limit work of 2026-09-28): when, by whom, and on whose word.

## Rules
- Evidence is a sha plus the quoted text. A LOG claim is a claim; say so when that is all there is.
- Say "not found" when you looked and did not find; "not checked" when you did not look.
- No recommendations. Report what happened.

## Output
`/home/joe/code/storage/proof-2a/creep-d/REPORT.md`: (a) the peak-complete snapshot table; (b) one table row per reopening: section | complete at (sha, date) | reopened at (sha, date) | class | who asked | quoted reason; (c) the list for question 5; (d) totals per class. Mark each claim Read or Ran.
