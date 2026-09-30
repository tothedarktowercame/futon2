# HEAD recovery pilot — ten mission HEADs from Joe's recorded words (2026-09-30)

zai-1 for claude-1 (War Machine selection seam). Joe's idea: mission HEADs
are supposed to be in his words; for missions created since recording
began, find the recorded words associated with the creation. No mission
file was edited. Verbatim only — no paraphrase, no tidying, no invented
HEADs.

Method per mission: git creation (`git log --diff-filter=A --follow`),
then the evidence store text search
(`GET :7073/api/alpha/evidence/text-search?q=<q>&limit=50&hydrate=true`;
README-fts read first), keeping only operator turns (an entry whose body
is `event "chat-turn"`, `role "user"` AND whose `evidence/author` is
`"joe"` — see "What I could not tell"), then the on-disk turn files
(batches + live), then same-session window turns ±hours around the
creating commit. Store discipline held: 16 queries total across the ten
missions (≤4 each, limit 50, sequential, no paging, no /health).

## Table

| mission | created (UTC) | committed by | class | passages | earliest operator turn found |
|---|---|---|---|---|---|
| M-formal-war-machine | 2026-08-25T11:17:54 | Joseph Corneli (`e0295c8e3`) | **A** | 1 | 2026-08-25T11:15:23 |
| M-G-wm-wiring | 2026-09-17T19:49:56 | Joseph Corneli (`d9b906f31`) | **C** | 0 | (nearby turns from 2026-09-17T12:37, none about the mission) |
| M-a-wmc-scaling | 2026-09-20T18:03:16 | Joseph Corneli (`5271311d4`) | **B** | 2 | 2026-09-20T14:59:07 |
| M-run-produces-its-own-brief | 2026-09-01T07:34:05 | Joseph Corneli (`e3e5f3684`) | **B** | 1 | 2026-09-01T07:26:55 |
| M-f11-find-production-successor | 2026-09-12T16:16:09 | Joseph Corneli (`ad6e034db`) | **B** | 1 | 2026-09-26T17:33:40 (post-creation) |
| M-custom-harness | 2026-07-04T18:57:33Z | Joseph Corneli (`f15682272`) | **A** | 2 | 2026-07-04T17:26:58 |
| M-eoi-outbox-management | 2026-06-12T18:38:41Z | Joseph Corneli (`fd2be4c`) | **A** | 1 | 2026-06-09T20:06:58 |
| M-single-entry-point | 2026-05-03T17:36:43Z | Joseph Corneli (`5f18bc2f`) | **B** | 2 | 2026-07-08T06:44:28 (post-creation) |
| M-codex-sorry-loop | 2026-08-01T16:16:18Z | Joseph Corneli (`e0adbc3a`) | **C** | 0 | (nearby turns 2026-07-31T18:19:39, none about the mission) |
| M-experiment-build-match | 2026-08-01T15:15:05Z | Joseph Corneli (`029351177`) | **B** | 1 | 2026-08-01T15:44:35 (29 min post-creation) |

Counts: **A 3, B 5, C 2, D 0.**

## Verbatim passages

### M-formal-war-machine — A (2 minutes before the creating commit)

> Let's not worry about those other commits.  I think we should take the
> "build a Lean model of the War Machine" idea discussed above and write a
> new mission M-formal-war-machine.md that adapts the APM approach.  There
> may alse be a cross-reference to ./futon5/holes/M-formal-patterns.md but
> the latter one is a fairly speculative mission, whereas the
> M-apm-demonstration is running right now, and the Lean to Clojure spec
> work has definitely helped it run better

— 2026-08-25T11:15:23.845745835Z, turn `claude-13-turn-415` (claude-13
session), file
`/home/joe/code/storage/operator-turns/batches/2026-08-22_2026-09-21_block007/claude-13-turn-415.json`.

### M-a-wmc-scaling — B (commissioning discussion ~3h before; direct words only after)

> OK, let's consider that C is now specified to a level where *something*
> compatible with my views on the matter can be implemented.  I think we
> should move on to A.  I had asked codex-3 to look at
> nttcslab/variance-wmc from Github, we could reread the notes in
> *codex-repl:codex-3* from line 45

— 2026-09-20T14:59:07Z, turn `claude-12-turn-208` (claude-12 session),
file
`/home/joe/code/storage/operator-turns/batches/2026-08-22_2026-09-21_block031/claude-12-turn-208.json`.
("A" is the successor item the mission became; the mission is never
named.)

> Hi, I'd like a pre-post-plop-2026 read of M-a-wmc-scaling.md —

— 2026-09-20T19:58:45.144316414Z, evidence id `emacs-b76a80d7a6bf9dc7e462e13d9d731555`, body mission-id `M-a-wmc-scaling` (after creation).

### M-run-produces-its-own-brief — B (8 minutes before the commit; partly describes)

> So I agree we don't need to restart right away. We can restart once
> we've gotten... A few things in place when we've done the analysis that
> you were talking about and so forth. As for this morning brief, I
> suppose in a sense we're having a prototype of that right now because
> we're walking through the decisions that have been made or the decisions
> that need to be made after the end of an overnight run.

— 2026-09-01T07:26:55Z, turn `claude-20-turn-87` (claude-20 voice
session), file
`/home/joe/code/storage/operator-turns/batches/2026-08-22_2026-09-21_block015/claude-20-turn-87.json`. The C449 wording ("a run must produce its
own brief") itself appears only in the commit message, not in any
operator turn found.

### M-f11-find-production-successor — B (words exist, only later)

> As predicted we hit the limit, let's restart with Codex helpers

— 2026-09-26T17:33:40.893055879Z, evidence id
`emacs-a1f2891de58d9b872c8be0e11fdaaa2f`, body mission-id
`M-f11-find-production-successor`.

### M-custom-harness — A (at/before creation; commit 18:57Z)

> So, I think we should focus now on the zai-7 M-custom-harness work and
> get it finalised.  I have been directin zai-8 to look at some
> exploratory issues.  We could come back to the futon1b idea later.

— 2026-07-04T17:26:58.023920249Z, evidence id
`e-9712457b-0a5d-4229-9e30-aed4ca6f1ca4`, body mission-id
`M-custom-harness`.

> Not urgent, just useful real-world testing for M-custom-harness

— 2026-07-04T17:46:51.103243764Z, evidence id
`e-ed7d36ed-e4d8-4810-903f-474c767da31c`.

### M-eoi-outbox-management — A (three days before creation; the commissioning words)

> So, Cluster A should go in a new mission such as
> M-eoi-outbox-management.md — the exact wording is that FUTON should
> author cold outreach letters, it's not asserted that it needs to send
> them.  The work could be parallel to the "Arxana Ledger" with a pipeline
> whereby FUTON writes EOIs (that's new) and I review and send them
> (working on my Rembrandt signature more than my John Hancock this time).

— 2026-06-09T20:06:58.624065624Z, evidence id
`e-efc5e109-0e9b-4133-b0a7-d2c3d1805e89`, body mission-id
`M-memes-arrows-patterns-diagrams` (the join names the session's clocked
mission; the TEXT commissions M-eoi-outbox-management).

### M-single-entry-point — B (words exist, two months later)

> Let's ask Zai-10 for the the single-entry-point binding now.

— 2026-07-08T06:44:28Z, store text search "single entry point"
(operator turn; evidence id in pilot.json).

> But I thought our three zai-sourced packets, latest being
> single-entry-point, had passed through A3, which would make them ready
> for A4?  I don't think we need M-learning-loop all in one go.

— 2026-07-08T07:38:33Z, same query.

### M-experiment-build-match — B (the describing words are 29 minutes after the commit)

> Experiment-build-match, 3 layers. codex-8 = layer 1 (CLean->Lean render
> + navigability gate); codex-9 = layers 2+3 (malli config validation +
> core.logic checker relation). ON BELLS: (1) codex-8 NEGATI…

— 2026-08-01T15:44:35Z, store text search "experiment build match"
(operator turn; truncated at the store's own 260-char preview — the full
text is in pilot.json where the store returned it). A pre-creation turn
the same session (15:03:50Z) directs the same two jobs without naming the
mission, so it is not quoted as commissioning.

### M-G-wm-wiring — C

The creating commit is itself the answer: `d9b906f31` "Commit the work
that was sitting uncommitted in the lab tree" — the mission file entered
git as part of an agent-work sweep. 123 operator turns exist in the ±8h
window (from 2026-09-17T12:37:59Z, claude-4 session, about the War
Machine's flat-decision removal), but none names or commissions this
mission. Not quoted as passages because none is about the mission.

### M-codex-sorry-loop — C

Two operator turns carry the mission join ("OK that succeeded",
2026-07-31T18:19:39Z; "OK with me!", 2026-08-04T17:31:50Z) — neither
says what the mission is for. The file itself entered git in a sweep
commit ("Sweep the untracked evidence base…").

## What I could not tell

- **Earliest operator turn in the store at all**: 2026-02-18T15:34:37Z
  (`chat-turn`, author `joe`, session `s-debug`, "Can you summarize what
  changed under the hood?") — measured in the RM1 bounded author=joe walk
  of the same store, not re-measured here.
- **Which fields identify an operator turn**: I treated an entry as an
  operator turn when body `event = "chat-turn"`, body `role = "user"`,
  AND `evidence/author = "joe"` (equivalently `evidence/origin.actor
  = "joe"` / `origin.kind = "operator"`; all three agreed on every entry
  I kept). Caveat found and NOT resolved: some `author "joe"` user turns
  are relayed agent output ("joe: 8joe: 2xbclaude-20⇐auto-bellback: …"),
  so authorship alone can misattribute; I excluded turns whose text
  begins with a relay marker, but a systematic relay filter does not
  exist.
- Voice turns typed into a claude-20 session carry `agent_id claude-20`;
  the operator is the author field, not the agent_id.
- The text index is relevance-ranked with limit 50 and no paging (by
  discipline); passages beyond the top 50 for a query were not seen.
  Full-text of a turn is sometimes only a preview (~260 chars); where the
  store returned only a preview, the quote is the preview, verbatim.
- M-run-produces-its-own-brief's exact commissioning sentence exists (as
  the commit's own words "C449: O11 dissolved, and a run must produce its
  own brief") but I could not find those words in any operator turn; the
  claude-20 turn quoted is the nearest recorded discussion.
