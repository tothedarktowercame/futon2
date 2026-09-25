# E-kimi-task-41 — operator round turn-c12-saucers: seat A, translate the turn into a cascade

**Requisition:** completed — 2026-09-25T19:17:32Z, job invoke-1790363637263-24287-3e228002, state done

Clocked in by claude-12 for kimi-31 on 2026-09-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Seat A — translate one operator turn into a pattern cascade

You are seat A of an operator round (futon3c/scripts/operator-round/). Joe's
turn is at /home/joe/code/storage/operator-turns/rounds/turn-c12-saucers/turn.json (fields `source_text`, `sentences`). Translate it into a
pattern cascade in the format of the two existing translations — read one
first, it is the specification by example:

- /home/joe/code/storage/operator-turns/translations/turn-hVCUAL.kimi-2.md
- /home/joe/code/storage/operator-turns/translations/turn-QMNU0A.zai-2.md

and the library patterns that govern the form: futon3/library/象/言即行.flexiarg
and futon3/library/translation/*.flexiarg.

Tools (futon3c/scripts/xlate.py, no model involved):
- `python3 xlate.py find "<the move in plain words>"` — candidate pattern ids
  from the library (and proposed candidates). Read the flexiarg before citing.
- `python3 xlate.py offsets /home/joe/code/storage/operator-turns/rounds/turn-c12-saucers/turn.json "<exact span>"` — codepoint offsets.
- `python3 xlate.py lint /home/joe/code/storage/operator-turns/translations/turn-c12-saucers.kimi-31.md --turn /home/joe/code/storage/operator-turns/rounds/turn-c12-saucers/turn.json` — must end `0 problem(s)`.

The cascade: one `(join Fn-...)` per move Joe makes; each join cites the
patterns that together are that move, each with `:span [a,b]` into the turn
and a `:note` saying what the pattern does here. Where no pattern fits, a
typed `(HOLE-n :wanted ... :in-hand ... :discharge ... :candidate ...)` —
never a nearest-neighbour guess. Force on the envelope comes from words in
the turn.

Seat B will act on your cascade in the code, with Joe's text beside it, and
will cite your join ids and pattern ids as the warrant for each change. So a
join that names no action cannot be carried out, and a pattern cited loosely
becomes a loose warrant in the code. Cite what the turn asks for; do not add
moves Joe did not make.

Write /home/joe/code/storage/operator-turns/translations/turn-c12-saucers.kimi-31.md with the same sections as the examples (The cascade, Marked
spans, The six contract clauses, Candidates, xlate.py report, What the format
could not express). Do not act on the turn yourself. Do not edit any file but
/home/joe/code/storage/operator-turns/translations/turn-c12-saucers.kimi-31.md. Commit /home/joe/code/storage/operator-turns/translations/turn-c12-saucers.kimi-31.md only if asked; otherwise leave it and reply with its
path, the lint's last line, and the join ids.
