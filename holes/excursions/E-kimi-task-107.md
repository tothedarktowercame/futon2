# E-kimi-task-107 — H-interp: can the locator-question reading give an outcome statement a checkable locator (read-only)

**Requisition:** in-progress — dispatched 2026-09-27T15:27:34Z to kimi-3 as invoke-1790522854100-25399-92264610

Clocked in by claude-8 for kimi-3 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# HINTERP-GRAIN-D3: can the existing locator-question reading give an outcome statement a checkable locator? (read-only discovery; ⟨1⟩3 H-interp of PROOF-2a-PLAN)

HINTERP-GRAIN-D3 (claude-8 → kimi-3). READ-ONLY: change no file, make no commit, run no flight and no click, dispatch no seat, write to no store. Bell claude-8 back with the report.

Background (HINTERP-GRAIN-D and D2, accepted). The wired want source yields lifecycle exit lines; outcome statements (`futon2.wm.extract-outcomes/read-outcomes`) never become wants. An outcome statement carries a quote and cue spans and nothing that says whether it holds. `cascade_problems.clj:81-85` admits only tokens with a locator of class C3, C4, C5, C6 or C8 (Joe, 2026-09-17: every token the model predicts must be observable by a mechanical check), and `:192-196` refuses the WHOLE target when any want lacks one. D2 proposed a new class whose check always answers :unknown; claude-8 does not want a check that can only give one answer. The machine already has a reading step that asks a seat for locators: the stores `locator-questions`, `locator-declines`, `locators` (src/futon2/aif/mission_reading.clj, flight_runner.clj, flight.clj; wires r2-store-locator-questions → r3-store-locator-questions and siblings).

Questions, each with file:line, and a real call in your own process where marked (`clojure -M -e …` in futon2 under `timeout 300`; temporary directories only; never Drawbridge :6768):

1. THE EXISTING PATH. What triggers a locator question today, what text is the seat given, what may it answer (which locator classes, what shape), how is an answer admitted into `locators`, what is a decline, and where do admitted locators enter a flight's wants (`flight.clj` `source-wants :a-exits`, the `machine` locators merged at about :83)? RUN: print one real request's prompt text built for an M-futon-seams criterion without issuing it.

2. FIT. Could the same path take an outcome statement (its quote as the stated text) in place of an exit criterion with no code change, a small one (name it), or not at all (why)? Which existing locator classes could express the hand unit's tokens in test/fixtures/want-interp-library/M-futon-seams-instance-6@futon3c-6149272b.edn — take `:flag-retired`, `:signature-declared` and `:call-sites-enumerated` and write, for each, the locator a seat could honestly give (for example a C5 or C6 check that a name is absent from the code, a C8 registered test), or say none fits and why.

3. BEFORE AN ANSWER EXISTS. Between asking and an admitted locator, what is the outcome statement in the flight record today for an exit criterion in the same position (`:unlocated` with `:reason :verdict-not-stated`? `:out-of-view`?), and does that state refuse the target, stop the flight, or let it proceed on the located wants? Quote the site.

Report: answers 1–3; then ONE proposed packet, at most about 200 lines with its test, for this sequence: outcome statement → locator question → admitted locator of an EXISTING class → want; an outcome with no admitted locator is listed with a typed reason and is not yet a want. No new locator class, no change to the flight's stop rule. If that sequence cannot be built within those limits, say exactly which limit breaks. Do not write the packet's code.
