# NOTE — claude-15's REPL buffer, preserved and mined (2026-08-31, claude-1; commissioned by Joe)

## What this is

`claude-15-repl-buffer-snapshot-2026-08-31.txt` (7,393 lines, 878 KB) is a verbatim
snapshot of the Emacs buffer `*claude-repl:claude-15*`, taken 2026-08-31 while the
buffer was still live, after claude-15's session became unusable that morning.
Precedent: the claude-13 turns of 08-27, which survived only because someone copied
them out of `/tmp` after compaction dropped them from the transcript
(`claude-13-repl-turns-2026-08-27/`). This snapshot closes the same exposure for
claude-15 before any Emacs restart loses it.

## How the session ended (buffer lines 7355–7393)

After delivering the two held morning reports (wave-2 harvest gate; NOUNS-D1), the
seat failed on Joe's next message with `Failed to authenticate: OAuth session expired
and could not be refreshed`, and then every subsequent request — including a bare
"hello" — was refused by the model-side safeguard with detail `[reasoning_extraction]`
(request ids `req_011CeaXQrvGBWw12bEDuq3CC`, `req_011CeaXSwzv2P8wrVYQ3FzAt`). The
session context was ~448k tokens at the time. Nothing in Joe's messages caused this;
the accumulated context itself tripped the flag, persistently. The work survived the
seat because the state lived in `BUILD-ledger.md` / `BUILD-status.md` / the problem
records, and claude-20 resumed from those. The only artefact at risk was this buffer.

## Map of the buffer

| lines | contents | committed elsewhere? |
|---|---|---|
| 1–~2300 | 08-29/30: glossary formalisation, worksheets, P-validated-R5, spider pilot + wave 1, Snatch packets A/B/B′ | yes — the records those turns produced |
| ~2300–6500 | 08-30 build day: adapter, holes ledger, R-node lanes, R19, AUD, EDGES-D1, coherence job | yes — ledger/records |
| 6505–6530 | Joe states I_evidence_consumed aloud; the six same-day falsifier instances | precept: futon4 `69106ec`, futon2 `c8a6157`. Joe's spoken wording: **buffer only** |
| 6532–6563 | **The PLoP legacy-loop investigation**: Joe's "who reads the output of that loop?" prompt; claude-15's three-thread census; "the helix flattened to a circle, then stopped" | findings: `NOTE-cascade-consumers-census.md` (`22379b5`). The dialogue and the summary prose: **buffer only** |
| 6565–6585 | The evidence tetrahedron (§0.12) stated and recorded; "the detection that finally fired was you asking a question, not an instrument firing" | §0.12: futon4 `169afa9`. Wording: buffer only |
| 6587–6720 | wmRunsOnce/TickRunRecord; WM-RUN1; wave-2 blocked → misdiagnosed by codex-20 (queue) → rediagnosed by claude-15 (zai context-limit 400s under an idle-looking roster) → repaired via reset-session | records `a9761fa`, `798cced` |
| 6721–7100 | WM-RUN2 (route 9 hops, 3 conformant); EDGES-D1 gate (13 handoffs, 8 freehand); §0.13; claude-20 exchanges (R16 contested; exemplar-check default-scope fix) | ledger/records |
| 7095–7190 | Bedtime arc: §0.14 automation octahedron, §0.15 handoff formulas, §0.16 nouns seed, NOUNS-D1 dispatch | records committed; Joe's spoken framing: **buffer only** |
| 7190–7355 | Overnight: NOUNS-D1 gated (scheduled-runner discovery); two deadline wakes handled check-not-panic; wave-2 harvest gated | records committed |
| 7355–7393 | The session's death (above) | **buffer only** |

## What the buffer adds over the committed records

1. Joe's spoken prompts — the precept statements and the tetrahedral moves in his own
   words, which the records paraphrase.
2. The connective tissue: what claude-15 checked at each gate and in what order,
   including its own errors as they happened (the invented 40-char authority tail;
   the exit-code-through-a-pipe mistake, twice).
3. The complete failure-and-succession record of the seat itself, with request ids.
4. The per-turn cost/context lines (a session-economics trace: ~$105, ctx 377k→448k
   over the evening).

Consumer of this note (I_evidence_consumed): the futon-2026 case study
(`p4ng/sec-case-study-vetting.tex`) cites this snapshot as its source for the
session-death incident and the investigation dialogue.
