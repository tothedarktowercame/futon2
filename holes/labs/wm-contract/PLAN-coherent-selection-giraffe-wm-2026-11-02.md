# PLAN — one coherent selection across Giraffe and the War Machine

**Status:** AGREED between claude-2 and claude-16 (2026-11-02 01:50Z); for Joe's word. Nothing in the WM is built yet; the review sheet exists.

**Joe, 2026-11-02 ~01:40Z:** "right now there are two systems, Giraffe and War Machine, neither of which is able to really do useful work … because everything is blocked … one status that we could put in there is a good candidate for automated work … which could push it up the war machine ranking, even if it's not the most important unblocker … the top priority items according to both systems are all blocked so I think what we need is a review of those proposed tasks and I'm happy to work on that interactively with Claude 16 … we need to make a plan for how we're going to build something that's coherent."

## The state we are planning from (pass 20261102T013012.768Z-26ad19e6, placements 1/2a/2b)

- 600 tasks in the Giraffe order; 508 ready; 40 unblock anything.
- Placement actions of the 40 unblockers: retire 10, held 9, not-read 7, close-and-carry 7, close/merge 4, **work 2**, unplaced 1. The largest cone (M-war-machine-aif-completion, 11 dependents) is a retire row.
- 68 ready tasks have a work row with a stated finish; none unblocks more than one task.
- The WM's two policy classes rank by cone and unblocks (Giraffe mechanics) and the record never said whose priorities those were. Joe's readings rank by wing, route, finish.
- **The bottleneck is not technical: the top of the field is blocked on Joe's decisions.** The first useful step is the review session, not more building.

## The plan

1. **One field, two lanes.** Giraffe keeps the dependency structure (ready / undetermined / not-ready, cone, unblocks, stale markers). The commons map keeps the placement (wing, molecule, route, beneficiary, action, finish). The same rows answer two questions: what the machine should do next (the **automated lane**, the only lane the WM reads) and what Joe should do next (the **interactive lane**). The WM says in `:why` which evidence it used. Tickets are part of the field and are not placed yet.
   **Warning (claude-16):** the retire and held readings are not reliable enough to act on (pass 2b marked M-apm-solutions retire while Joe wants the APM prelims finished: one known error in 49). Retire and held rows never lower a task's standing, in ranks or in cones, until Joe has ruled.
2. **The marker `:automation-candidate`** (Joe's phrase), computed in Giraffe beside `:stale-markers` so a pass stays deterministic and reports it the same way. True when the placement action is `work`, who-acts is an agent, finish-kind is **computed or an artifact** (not judged), and the finish is repo-local. The reading that supplies who-acts / finish-kind / repo-local / blocked-by-hold is `placements-who-acts.tsv` (claude-16, running over the 91 work rows and the 27 not-read rows). Lean builds are computed finishes, which is why the prelim problems may suit the marker well. The WM reads the marker as evidence that lowers the risk term; it never excludes anything.
   **Bytes:** the TSVs live in `~/notes`, which Giraffe cannot pin. They move to a committed path (probably under `futon3c/holes/`) with their sha in the run manifest, after a privacy check (roles only; the review sheet scanned clean, the TSVs not yet) and with Joe's word, since committing is his call.
3. **The review of the blocked top: done.** `~/notes/commons-map/review-unblockers-2026-11-02.md` (built by `review_sheet.bb` from the pass and the placements): 47 rows ordered by Giraffe rank, columns rank, id, status, cone, action, wing/route, unblocks, evidence, decision; the decision is a short question ("retire, or does it still matter?", "keep held, or un-hold with what?"). Actions: held 14, retire 12, not-read 7, close-and-carry 7, work 2, close 2, merges 2, unplaced 1. "What un-holds it" is the evidence quote for now and improves as Joe rules. Joe reviews it interactively with claude-16. Until then the WM selects from nothing in it and reports it as a disposal batch.
4. **Policies as ways of selecting (L5-29)**, reporting per L5-28 (a)–(h), the marker as evidence in all four:
   - *clear the closeable backlog*: supported as-is from the close, close-and-carry and merge rows; the agent's task is to prepare a batch, only Joe closes;
   - *unblock the most*: supported by Giraffe but misleading today (2 of the 40 unblockers are work rows); cones recomputed over rows Joe has not retired or held, once he rules;
   - *advance a route with a person and a date*: not supported yet; 25 of 91 work rows name a beneficiary; persons and dates stay in futon7; needs a `bet` column holding a bet id only;
   - *pay what the body depends on first*: not supported as a selection; the real invariants (hosting, the evidence copy, subscriptions) are not documents and so not in the field; they need standing items with a due state, with `zone-health` results arriving as observations (data gap for claude-16, then a reading for claude-2).
5. **The gate (L5-28)** holds before any selection click, including "at least one Joe decision batch has run".
6. **Owners and order.**
   1. claude-16: the who-acts reading, then Joe's interactive review (sheet ready now).
   2. claude-16: privacy check; with Joe's word, the move of the TSVs to a committed path.
   3. claude-2: the WM reads placements and says so in `:why`; then the marker as a term, Lean first.
   4. claude-16: the `bet` column and the standing invariants.
   5. claude-2: the four cascades (L5-29); then the first gated selection click.
   Dispatches otherwise paused by Joe.
