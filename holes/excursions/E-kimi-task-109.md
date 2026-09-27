# E-kimi-task-109 — How a lifecycle exit's written verdict can be observed per phase with an existing locator class (read-only)

**Requisition:** in-progress — dispatched 2026-09-27T16:07:04Z to kimi-5 as invoke-1790525224756-25443-1bc34860

Clocked in by claude-8 for kimi-5 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# EXIT-VERDICT-D: how a lifecycle exit's written verdict can be observed per phase (read-only discovery; ⟨1⟩3 H-interp of PROOF-2a-PLAN)

EXIT-VERDICT-D (claude-8 → kimi-5). READ-ONLY: change no file, make no commit, run no flight or click, dispatch no seat, write to no store. Bell claude-8 back with the report.

Ruling (Joe, 2026-09-27 about 16:06Z, agreeing with claude-8's recommendation): for a lifecycle exit that is a person's judgment ("A human has read the proposal and agrees…", "The design feels inevitable…"), the observation is a WRITTEN VERDICT in the mission's own phase section. The person judges; the machine observes that the judgment was recorded.

Background. futon2 b8b57c730 (`src/futon2/aif/lifecycle_exits.clj`) supplies, from futon4 holes/mission-lifecycle.md, one `:role :how` criterion per phase for which a lifecycle-shaped mission states no exit of its own; a supplied exit is listed `:no-admitted-locator` until it has a locator of class C3/C4/C5/C6/C8. Today the reader locates an in-text exit by a C4 locator whose declaration is the criterion's own text followed by `**Met.**` (`src/futon2/aif/mission_criteria.clj:88-99, :124-146`), so one criterion's verdict cannot satisfy another. A supplied exit's words are not in the mission, so that shape cannot be used.

Questions, each with file:line, real calls in your own process where marked (`clojure -M -e …` in futon2, `python3` for the script, under `timeout 300`; never Drawbridge :6768):

1. THE C4 CHECK. In futon2 `src/futon2/aif/observation_checks.clj`: exactly what `check-decl-in-file` / `decl-present?` observe (whole file or a region? one line or several? how is a multi-line declaration matched?). Could an existing class observe "this line appears inside the section under heading H" without a code change? If not, what is the smallest change, and is it a change to a check's behaviour or a new field on the locator?

2. WHAT MISSIONS WRITE TODAY (RUN). For the 50 lifecycle-shaped missions (census of LIFECYCLE-EXITS-D, E-kimi-task-108): per phase section, is there a line matching the reader's verdict pattern `\*\*(?:Met|Not met|Not started)`, and what does the whole line say? Print the distinct line shapes with counts and three examples each. Also read futon3c `scripts/verdict_check.py`: what it takes as "the first verdict line per phase section", and against what it checks it.

3. CANDIDATE SHAPES. For each, say whether the existing C4 check can observe it as it is, whether one phase's verdict could satisfy another phase's exit (the falsifier the present shape guards against), and what a mission author has to write:
   (a) a verdict line that names its phase, e.g. a line beginning `**MAP exit: Met.**`;
   (b) a bare `**Met.**` line inside the phase's section, located by section;
   (c) the author copies the exit line from the definition and adds the verdict (today's M-futon-seams practice), which makes the exit in-text and not supplied;
   (d) the status field of a lifecycle.edn beside the mission (futon3c holes/labs/M-futon-seams/lifecycle.edn), which the reader today sets aside as `:data-only-no-checkable-class`.

Report: answers 1–3 with printed values; then ONE proposed packet, at most about 150 lines with its test, that gives each supplied exit a locator of an EXISTING class observing a written verdict for THAT phase only, with the bad case named (a `**Met.**` under ARGUE must not make the MAP exit read true). Prefer the shape that needs no change to any check. Do not write the code.
