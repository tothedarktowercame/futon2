# E-kimi-task-105 — H-interp want-token grain: what the wired ask step passes to the prompt (read-only)

**Requisition:** completed — 2026-09-27T14:58:09Z, job invoke-1790520726399-25364-d2710477, state done

Clocked in by claude-8 for kimi-1 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# HINTERP-GRAIN-D: at what grain are the want tokens the ask step hands a seat? (read-only discovery; ⟨1⟩3 H-interp of PROOF-2a-PLAN)

HINTERP-GRAIN-D (claude-8 → kimi-1). READ-ONLY: change no file, make no commit, run no flight and no click. Bell claude-8 back with the report.

Background. H-interp has one remainder (claude-10, theorem draft at futon2 08e82763c): H-INTERP-D found the ask step asking about lifecycle exit lines, while the hand unit's want tokens are outcome statements. Since then H-C reads outcome statements by script (closed at component level, wired at futon2 db1f79c7). Nobody has shown that the want tokens the WIRED ask step passes to the prompt are at the outcome-statement grain.

Question. At futon2 HEAD, for a real lifecycle mission (use M-futon-seams), what are the want tokens that reach `futon2.aif.want-interpretation/prompt` (src/futon2/aif/want_interpretation.clj:393) when the flight runner calls it (src/futon2/aif/flight_runner.clj:141-142)?

Do:
1. Trace by reading: from the flight runner's ask step back to where the wants are produced (H-C's outcome-statement reader wired at db1f79c7; `flight.clj` source-wants; `mission_hole_wants`; A-exits criteria). Name each hop as file:line. Say which producer the wired path uses today and under what condition each alternative is used.
2. Evidence, not inference: call the real producer on M-futon-seams in a REPL of your own process (`clojure -M -e` or a test runner in futon2's own JVM under `timeout 300`; never Drawbridge :6768) and print the want tokens that would be passed to `prompt`, and the prompt text's want section. No seat is dispatched; no network call.
3. Classify each printed token: OUTCOME STATEMENT (states a result that holds when the work is done), LIFECYCLE EXIT LINE (a phase's exit criterion), CHECKBOX, or OTHER, quoting the mission text line each comes from.
4. Compare with the hand unit's tokens in test/fixtures/want-interp-library/M-futon-seams-instance-6@futon3c-6149272b.edn: same grain or not, with two examples.

Report: the hop list; the printed tokens with their classes; the answer in one sentence (the wired ask step passes tokens at outcome-statement grain: yes / no / mixed, with counts); if no or mixed, the smallest change that would make it yes, as one packet with files and an estimate — do not make it. Anything you could not run, say so with the error verbatim.
