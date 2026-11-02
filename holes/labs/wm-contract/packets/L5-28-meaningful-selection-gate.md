# L5-28 — A selection is reported as ways of selecting, and a click waits for a meaningful one

**Status:** PROPOSAL to Joe (claude-2 with claude-16, 2026-11-02 01:28Z). Not dispatched; dispatches are paused by Joe's word.

**Joe, 2026-11-02 ~01:25Z:** "when it selects something, it would give a reason, not just a number … that g is supposed to be computed over policies, those policies should be telling us about different possible selections of tasks, not just sort of randomly weighting tasks and saying this has a high rating and that has a low rating; it's about different ways of selecting the tasks … I don't really want to run the outer loop followed by the inner loop if we don't have some assurance that we're selecting meaningful tasks first."

## What the record says today (click 2, `wm-click-8ed79a0b`)

Per policy class a sentence of counts: "only-what-can-start-now, giraffe-order picked M-interim-director: ready gate left 503 of 587; by-cone took cone 12 (runner-up M-autoclock-in at cone 4); by-unblocks took unblocks 1 … G 2.5033", and the decision "chose M-interim-director (G 2.5033) over stopping (13.6183) and 1 other class". Beside it, `:per-task-rating` (588 rows) and `:top-ten` (L5-19). The two classes both rank the same Giraffe field by Giraffe mechanics; they are not two ways of selecting. The pick's remaining wants were all Joe's decisions; the author refused; the mission has since been closed by Joe (futon7 `8b8995b`, 23:54Z).

## The report, per policy class (proposal)

- (a) the way of selecting in words: the cascade's patterns and what each step does to the field;
- (b) what it proposes, and what it would have proposed instead;
- (c) why G prefers it: which term moved (risk / ambiguity / novelty) and from what evidence;
- (d) what the pick would change in the world if done;
- (e) where the pick sits in the backlog structure: wing, route, named beneficiary, finish check (claude-16's `~/notes/commons-map/placements-*.tsv`); a missing placement is said in words;
- (f) who has to act: an item whose remaining wants are Joe's decisions is not automatable, and the report says so before any dispatch;
- (g) what it was chosen over, as ways of selecting: "this way picks X because …; that way picks Y because …";
- (h) the prediction: what we expect to see if it works, checkable at the next click.

`:per-task-rating` and `:top-ten` leave the report and stay in the trace. G terms appear only as "which term moved, on what evidence". Exclusions and losers are recorded by cluster as well as by task, so "why not this cluster?" can be answered. Aiming a run at a cluster overnight is a policy with Joe's stated priority as evidence, not an override. If the only honest candidates need Joe, the output is a disposal batch for him, not a stop. An income bet the machine cannot serve is said plainly.

## The policy candidates

Policies come from how work was really selected, not from brainstorming. Recorded tonight (claude-16, transcript as evidence): pay what the body depends on first; clear the closeable backlog; advance a route that has a person and a date; unblock the most. Cascades for these are claude-2's to construct (L5-29, after Joe agrees).

## The gate

Before a selection click:
1. a fresh Giraffe pass after the latest close;
2. each proposed pick has a placement row with action `work` and a stated finish, or the record says why not (advisory until the TSV join covers the field; placements-2b is running);
3. at least two policies whose picks differ for a stated reason, not just a rank order;
4. the `:why` reads as a paragraph Joe could follow without the numbers;
5. the prediction is written down first.

After:
- the worker accepts the task as workable;
- no remaining want is Joe's decision;
- the report matches what the worker found;
- a refusal is treated as a selection defect, not noise.

## What is not a selection

The one click Joe authorized tonight (Codex cast) is the out-of-band repair check that discharges injury d984347f; it calls no selection and is not evidence of meaningful selection.
