# HEAD recovery, batch 01 — ten more missions from Joe's recorded words (2026-09-30)

zai-1 for claude-1. Method, operator-turn test and relay exclusion exactly
as the pilot (`../PILOT.md`): git creation, then the evidence store text
search, then the on-disk turn files, then same-session window turns.
Operator turn = body `event "chat-turn"` + `role "user"` +
`evidence/author "joe"`, with relayed agent output (turn text beginning
with relay markers like `joe: …⇐auto-bellback`) excluded. No mission file
edited; no HEAD invented for C or D. Store discipline held: **10 queries
total** (one per mission, limit 50, sequential, no paging, no /health);
none took over 20 seconds.

## Table

| mission | created (UTC) | committed by | class | passages | earliest operator turn found |
|---|---|---|---|---|---|
| M-warrant-limit | 2026-09-28T17:58:47 | Joseph Corneli (`934ac5df`) | **B** | 1 | 2026-09-28T20:28:55 (post-creation) |
| M-inbox-zero-claim-lifecycle | 2026-09-27T18:28:58 | Joseph Corneli (`bbca7a7e`) | **B** | 1 | 2026-09-27T19:21:08 (post-creation) |
| M-wm-wiring | 2026-09-25T15:20:09 | Joseph Corneli (`ad9fb4e8`) | **B** | 1 | 2026-09-25T16:57:49 (post-creation) |
| M-turns-first | 2026-09-17T19:50:47 | Joseph Corneli (`11649128`) | **C** | 0 | (none about the mission; sweep commit) |
| M-u88-contextual-preferences | 2026-09-10T17:21:34 | Joseph Corneli (`cf2aee0c0`) | **B** | 1 | 2026-09-11T01:04:15 (post-creation) |
| M-zaif-harness-v1 | 2026-09-02T12:06:11 | Joseph Corneli (`6362cb425`) | **A** | 2 | 2026-09-02T11:52:19 (14 min before) |
| M-sigils-reconsidered | 2026-08-17T13:18:35 | Joseph Corneli (`59bfe27a`) | **A** | 1 | 2026-08-17T13:17:21 (74 s before) |
| M-what-is-it-who-is-it-for | 2026-08-17T13:16:05 | Joseph Corneli (`0902df5`) | **C** | 0 | (none about the mission) |
| M-capability-levels | 2026-08-17T11:19:56 | Joseph Corneli (`35598df`) | **A** | 1 | 2026-08-15T15:26:37 (2 days before) |
| M-evidence-landscape-index | 2026-08-17T07:47:03 | Joseph Corneli (`4561ff9`) | **A** | 1 | 2026-08-15T17:01:32 (2 days before) |

Counts: **A 4, B 4, C 2, D 0.**

## Verbatim passages

### M-zaif-harness-v1 — A (the commissioning words 14 and 6 minutes before the commit; voice turns)

> So I was thinking for U6, maybe instead of ants, we could use the zai.
> AIF model idea, which may already exist as a mission. And that way we'd
> get a practical mission that we could run through the system. And break
> down into components so we could test each component. Of the war
> machine. And the kind of cool outcome is basically we're going to get
> the crew for the war machine. By building it ourselves, because
> ultimately we can use these Zaif agents as the crew. And I was thinking
> just for fun, we could pull the Chipwits fourth repository from GitHub
> and look at some of the assets in there and just see if we couldn't make
> a little fun analogy between Chipwits and these agentic models and their
> harnesses.

— 2026-09-02T11:52:19.214287246Z, turn `claude-1-turn-104` (claude-1
voice session), file
`/home/joe/code/storage/operator-turns/batches/2026-08-22_2026-09-21_block016/claude-1-turn-104.json`.

> Okay, so this is a good development, and I think that we should look at
> any historical Zaif missions, because they do exist with a basic
> specification on some level. But I think we need to take that historical
> document and mash it up with the new chipwits. Considerations. And see
> if we can make... A nice set of unit tests. For each of the R number
> nodes that will exercise them. And help us. Build a new… Addition of
> the Zaiv. Harness.

— 2026-09-02T12:00:07.135550672Z, turn `claude-1-turn-105`, same session,
file `…/block016/claude-1-turn-105.json`.

### M-sigils-reconsidered — A (74 seconds before the creating commit)

> Well, I'm not ready to rule on it, we can create a M-sigils-reconsidered
> with the findings from your survey, but there would be a case "for" them
> to add later.  For now, the 8 index rows can be added w/o sigils

— 2026-08-17T13:17:21.935364046Z, evidence id
`emacs-9ea75683b0f2caa686602521ee1855c0`, body mission-id
`M-apm-demonstration` (the session's clocked mission; the TEXT
commissions this one).

### M-capability-levels — A (two days before creation; the naming turn)

> Maybe ladder is the wrong word, how about M-capability-levels.md —
> that's clearer, and also akin to the CodeSignal platform's lingo

— 2026-08-15T15:26:37.880276816Z, evidence id
`emacs-3f6b635c544bf62d155b761fdfca411a`, body mission-id
`M-apm-demonstration`.

### M-evidence-landscape-index — A (two days before creation; the proposal)

> So, what about futon1b/holes/M-evidence-landscape-index.md capturing
> these ideas, to be developed as a formal futonic mission?

— 2026-08-15T17:01:32.955986384Z, evidence id
`emacs-f6f55353c9db9c054bf69e7127207c5d`, body mission-id
`M-apm-demonstration`.

### M-warrant-limit — B (words exist, only after creation)

> OK; then I'd say it's not really scope creep, but either "getting it
> right" or "building necessary tooling to make that efficient".  E.g. the
> test-registry is because it is untenable to rerun tests endlessly and
> I'd rather spend a day making a well-functioning warrant system that we
> can reuse than spend endless days on tests.

— 2026-09-28T21:02:35.754715736Z, evidence id
`emacs-ab787b2100a07ecc1c2f81b5a752b522`, body mission-id
`M-warrant-limit`. (Also 20:28:55, `emacs-05e0725c1dd89d84ae663d3ed196cd41`:
"OK, I'd consider that we can now get back to the War Machine PROOF-2a…")

### M-inbox-zero-claim-lifecycle — B (a live example, 53 minutes after creation)

> Here's a live example of how inbox zero is working now.  The messages:
> to claude-1 detail seemingly irrelevant files to the work in progress.

— 2026-09-27T19:21:08.429498932Z, evidence id
`emacs-6aa2374e245e9d00909e0bce0e59074f`, body mission-id
`M-inbox-zero-claim-lifecycle`. (Truncated at the sentence boundary — the
turn continues with the inbox-zero message verbatim, in batch.json.)

### M-wm-wiring — B (24 operator turns carry the mission join; the earliest is 97 minutes after creation)

> So, the mission lifecycle would say that DERIVE is next, though we may
> have already done that step.

— 2026-09-25T16:57:49.692160004Z, evidence id
`emacs-62562e9735392b15eff02d03adb8355e`, body mission-id `M-wm-wiring`.
(The mission's creating commit itself names the HEAD: "the outer loop as
a cascade is its HEAD" — but those words are the commit's, not a found
operator turn.)

### M-u88-contextual-preferences — B (a post-creation review message, partly describing)

> Review activated U88 mission/pin freeze and actual production
> eligibility; effective live consumers now all true. Complete pre-go
> checks without dispatch/acceptance or another restart.

— 2026-09-11T01:04:15.755652916Z, evidence id
`emacs-2db5ff8c76c6cb2a29d2484f5301cc20`, no mission-id join (matched by
text). Caveat: this is an operator wake/checklist message quoting a park
state — kept because its head is operator-voiced, but it is not a
commissioning passage. The creating commit ("Prepare U88 fixture
mission…") reads as agent-prepared work committed by Joe.

### M-turns-first — C

Created by the sweep commit `11649128` "Commit M-turns-first and two
small edits left behind in the lab tree" (same family as the pilot's
M-G-wm-wiring): the mission file entered git as agent work swept in. No
operator turn carries its name; keyword window search (±8h, "turns
first") found nothing. Nearby operator turns that day exist (the lab
session), none about this mission.

### M-what-is-it-who-is-it-for — C

Created the same 2026-08-17 afternoon in a batch of HEAD-capture commits.
No operator turn names it; keyword window search found nothing; the
day's operator turns are about other missions (M-apm-demonstration,
M-evidence-landscape-index, the interview prep). Not quoted: nothing is
about this mission.

## What I could not tell

- **Earliest operator turn seen in this batch**: 2026-08-15T15:26:37Z
  (the M-capability-levels naming turn). The earliest in the store at all
  remains the pilot's 2026-02-18T15:34:37Z (measured in the RM1 bounded
  author=joe walk; not re-measured here).
- **Which fields identify an operator turn**: unchanged from the pilot —
  body `event "chat-turn"` + `role "user"` + `evidence/author "joe"`
  (origin.actor/origin.kind agreed on every kept entry), relay-marked
  turns excluded. Two hard cases this batch, both stated rather than
  silently resolved: the M-u88 passage is an operator wake/checklist
  message that embeds park-state text (kept, flagged); voice turns
  transcribed with their disfluencies (the zaif passages) are verbatim
  including the trailing fragments.
- The relevance index with limit 50 and no paging means passages beyond
  each query's top 50 were not seen; for M-sigils-reconsidered (1
  operator hit among a very large result set) and the 08-17 missions
  especially, pre-creation words not containing the mission id could not
  be found by this method (the naming turn must contain the id to match).
- M-wm-wiring's HEAD content is named in its creating commit message,
  which is operator-committed but not shown to be operator-authored; I
  did not treat commit messages as Joe's words.
