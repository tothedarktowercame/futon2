# PLAN — one coherent selection across Giraffe and the War Machine

**Status:** DRAFT skeleton (claude-2, 2026-11-02 01:47Z) for co-authoring with claude-16; for Joe's agreement. Nothing here is built.

**Joe, 2026-11-02 ~01:40Z:** "right now there are two systems, Giraffe and War Machine, neither of which is able to really do useful work … because everything is blocked … one status that we could put in there is a good candidate for automated work … which could push it up the war machine ranking, even if it's not the most important unblocker … the top priority items according to both systems are all blocked so I think what we need is a review of those proposed tasks and I'm happy to work on that interactively with Claude 16 … we need to make a plan for how we're going to build something that's coherent."

## The state we are planning from (fresh pass 20261102T013012.768Z-26ad19e6, placements 1/2a/2b)

- 600 tasks in the Giraffe order; 508 ready; 40 unblock anything.
- Placement actions of the 40 unblockers: retire 10, held 9, not-read 7, close-and-carry 7, close/merge 4, **work 2**, unplaced 1. The largest cone (M-war-machine-aif-completion, 11 dependents) is a retire row.
- 68 ready tasks have a work row with a stated finish; none unblocks more than one task.
- The WM's two policy classes rank by cone and unblocks (Giraffe mechanics) and the record never said whose priorities those were. Joe's readings rank by wing, route, finish.

## The plan (skeleton)

1. **One field, two readers.** Giraffe keeps the dependency structure (ready / undetermined / not-ready, cone, unblocks, stale markers). The commons map keeps the placement (wing, molecule, route, beneficiary, action, finish). The War Machine reads both as evidence and says which it used. Owner of the join: claude-16 (map) + claude-2 (WM consumption).
2. **A status for automation candidates.** A marker, computed not declared: a task is an automation candidate when its placement action is `work` with a stated finish, no remaining want is Joe's decision, and the finish names a repo-local artifact. Where it is computed (Giraffe marker beside `:stale-markers`, or a column in the placements TSV, or both) is claude-16's call. The WM reads it as evidence that lowers the risk term, so a candidate can outrank a bigger unblocker; it never excludes anything.
3. **The review of the blocked top.** A review sheet of the 40 unblockers (and the held/retire rows among the top 50 of the Giraffe order) with: action, what it unblocks, what un-holds it, and the one-line decision Joe can give (close / retire / un-hold with a next step / leave). Joe reviews it interactively with claude-16. The WM does not select from this set until the review has run; it reports it as a disposal batch.
4. **Policies as ways of selecting (L5-29).** Four cascades from how Joe actually selects (pay what the body depends on first; clear the closeable backlog; advance a route with a person and a date; unblock the most), each constructed from the joined field, each reporting per L5-28 (a)–(h). The automation-candidate marker is evidence in all four.
5. **The gate (L5-28)** holds before any selection click; the first gated click runs after 3 has produced at least one Joe decision batch.
6. **Owners and order.** claude-16: 2 (marker) and 3 (review sheet) first. claude-2: 1 (WM reads placements and the marker, says so in `:why`), then 4. Lean first for anything the WM reads as a term. Dispatches otherwise paused by Joe.

## Open questions for claude-16

- Where should the automation-candidate marker be computed, and from which bytes?
- The review sheet's shape and where it lives (not business records; roles only).
- Which of the four ways of selecting your map already supports directly (route with a person and a date needs the beneficiary and finish columns; "pay what the body depends on" needs a notion of the body).
