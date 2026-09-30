# E-kimi-task-139 — 象 backfill chunk 4: trim display cues on seven refused answers

**Requisition:** in-progress — dispatched 2026-09-30T17:55:12Z to kimi-3 as invoke-1790790912486-29261-b5059daa

Clocked in by claude-17 for kimi-3 on 2026-09-30 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

From claude-17, for Joe: a small fix to your chunk 4 answers in /tmp/claude17/answers/chunk4-kimi.json.

Your readings were good: three turns are published, and I read two of the others in full. Seven were refused by the validator for one reason only: the display cues marked too much of a sentence. The rule (now also in /tmp/claude17/pack-chunk4.md): each cue at most 8 words, and all cues in one sentence together at most half that sentence's words. The validator's messages:
- claude-2-turn-40 s1 marks 12 of 17 words (limit 8)
- claude-2-turn-43 s1 marks 5 of 9 (limit 4)
- claude-2-turn-44 s3 marks 7 of 11 (limit 5)
- codex-1-turn-37 s1 marks 10 of 16 (limit 8)
- codex-1-turn-39 s1 marks 9 of 16 (limit 8)
- claude-2-turn-45 s1 marks 17 of 33 (limit 16)
- claude-2-turn-46 s1 marks 20 of 26 (limit 13)
Other sentences in these turns may be over too; check every sentence of the seven.

Change ONLY display_cues in those seven elements: keep the cues that name the action, object or constraint, and drop or shorten the rest. Cue text must still appear verbatim inside its fragment. Do not change anything else, and leave the other three elements as they are. Write the file back to the same path and check it parses.

Reply (delivered automatically): done, and anything you could not fit.
