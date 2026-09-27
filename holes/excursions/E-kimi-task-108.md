# E-kimi-task-108 — Where lifecycle exits come from today across all missions, and what supplying them from the definition needs (read-only)

**Requisition:** completed — 2026-09-27T15:55:03Z, job invoke-1790523929466-25416-444d0de6, state done

Clocked in by claude-8 for kimi-4 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# LIFECYCLE-EXITS-D: where a mission's lifecycle exits come from today, across all missions (read-only discovery; ⟨1⟩3 H-interp of PROOF-2a-PLAN)

LIFECYCLE-EXITS-D (claude-8 → kimi-4). READ-ONLY: change no file, make no commit, run no flight or click, dispatch no seat, write to no store. Bell claude-8 back with the report.

Ruling (Joe, 2026-09-27): a mission's outcome statements are its primary wants (the "why"); the lifecycle's phase exits are its secondary wants (the "how"). They are wanted for every mission because it is a mission, and should come from the lifecycle DEFINITION (futon4/holes/mission-lifecycle.md: one "**Exit criterion:**" per phase at about lines 45, 68, 113, 153, 191, 231, 266, 293), not from copies of those lines inside a mission's text. HINTERP-GRAIN-D found that M-futon-seams yields six `:exit/*` wants because its text repeats the template's lines.

Questions, with file:line, and real calls in your own process where marked (`clojure -M -e …` in futon2 under `timeout 300`; never Drawbridge :6768):

1. HOW THE READER FINDS EXITS. In futon2 `src/futon2/aif/mission_criteria.clj` (`criteria`, `criterion`, `wants`, `data-only-phases`): what text pattern makes a criterion, how a phase is recognised, what makes a criterion located (the inline `**Met.**` verdict), and why HEAD and IDENTIFY are set aside as `:data-only-no-checkable-class`.

2. CENSUS (RUN). For every `futon3c/holes/missions/M-*.md` and `futon2/holes/missions/M-*.md` at HEAD: is it lifecycle-shaped (which of the eight phase headings it has); how many criteria the real reader finds; how many carry a verdict; how many of the lifecycle's eight exits are NOT found in its text. Print one line per mission and the totals. Do not build a flight; call the reader on the text.

3. WHAT "FROM THE DEFINITION" NEEDS. If the eight exits were supplied from the lifecycle file for every lifecycle-shaped mission: (a) what is each exit's token (today it is derived from the line's text in the mission — show the derivation); would tokens for missions that did copy the lines change? (b) where would each exit's verdict be read — the mission's own phase section — and what in the reader finds a verdict today when the criterion line itself is absent from the mission? (c) which phase a mission is IN: is there an existing reader of current phase (the lifecycle.edn beside some missions, e.g. futon3c holes/labs/M-futon-seams/lifecycle.edn), and what does it record?

Report: answers 1–3 with printed values; then ONE proposed packet, at most about 200 lines with its test, that supplies the exits from the definition with `:role :how`, keeps existing tokens stable where a mission copied the lines, and changes no stop rule. Say which limit breaks if it cannot. Do not write the code.
