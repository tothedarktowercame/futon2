# E-kimi-task-106 — H-interp: how an outcome-statement want would be observed and assembled (read-only)

**Requisition:** completed — 2026-09-27T15:20:11Z, job invoke-1790521708256-25384-ade59e73, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition header states completed with state done. _(WM status classification by zai-2, high confidence; not yet confirmed by the author.)_

Clocked in by claude-8 for kimi-2 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# HINTERP-GRAIN-D2: how an outcome-statement want would be observed and assembled (read-only discovery; ⟨1⟩3 H-interp of PROOF-2a-PLAN)

HINTERP-GRAIN-D2 (claude-8 → kimi-2). READ-ONLY: change no file, make no commit, run no flight and no click, dispatch no seat. Bell claude-8 back with the report.

Background (HINTERP-GRAIN-D, E-kimi-task-105, accepted): the wired ask step passes 6 of 6 tokens at lifecycle-exit-line grain on M-futon-seams (`flight_driver.clj:100-108` always builds `{:kind :a-exits}`; `flight.clj` `source-wants :a-exits` reads `mission-criteria/wants`). The outcome-statement reader `futon2.wm.extract-outcomes/read-outcomes` (scripts/futon2/wm/extract_outcomes.clj:861) is called only by the read step (`served_by_reading.clj:119`). The hand unit's want tokens are outcome statements. The intended change is ADDITIVE: a want source that yields the outcome statements as wants, composed with the existing exit-line and checkbox wants, nothing removed. Before anyone writes it, three things must be known.

Questions, each answered with file:line and, where marked, a real call in your own process (`clojure -M -e …` in futon2 under `timeout 300`; never Drawbridge :6768):

1. OBSERVATION. For an `:exit/*` want, where do its `:locators` entry and its `:universe` boolean come from (`flight.clj` `source-wants :a-exits`, `mission-criteria`, `observation-checks`)? What would an `:outcome/*` want have in their place today — is there any existing reader that says whether an outcome statement holds (the read step's served-by record? a verdict in the mission text?) — or nothing? RUN: `read-outcomes` on M-futon-seams; print the kept outcomes with every field each carries.

2. ASSEMBLY. `flight.clj`'s comment says "A criterion with no stated verdict is a want with no locator; assembly then refuses the target naming it". Find that refusal (site, typed reason). If outcome wants were added with no locator, would the WHOLE target be refused, or only those wants set aside? Is there an existing typed way for a want to be carried as unobserved without refusing the target (for example the `:out-of-view` entries with `:reason :data-only-no-checkable-class`, or `:unknown` in the universe)? Name it and its consumers.

3. DOWNSTREAM. With six exit wants plus N outcome wants in one flight: what does `wi/unproduced-wants` ask about, what does the constructor's want set become (`cascade_problems.clj`, `:want`), and what does the flight's progress rule (`flight.clj` `advanced`, about :204) count? Would exit wants that are already true and outcome wants that are unobserved change the no-progress stop?

Report: answers 1–3 with sites and printed values; then ONE proposed packet for the additive want source, at most about 200 lines including its test, stating exactly what an outcome want carries for observation (and, if the honest answer is "typed unobserved", which existing typed form it uses) — do not write it. Anything you could not run: the error verbatim.
