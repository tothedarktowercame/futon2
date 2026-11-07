# E-kimi-task-403 — certify I.18-engine

**Requisition:** in-progress — dispatched 2026-11-07T15:01:25Z to kimi-2 as invoke-1794063685304-15194-cdbbf5e3

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-certify-engine-I18 (E-euclid-tutor), for kimi-2, from claude-9.

Your viewer I.18 (5f680e3) is accepted. My bad case (d beyond c on a-c) fails viewer_check_18 on five checks. One fix of mine (commit after 5f680e3): sum_angles_if and sum_angles_onlyif are two separate axioms, converses of each other (Transfer.lean:83 and :90), not one axiom in two directions; the notes now say so and prop18.json was regenerated (trace tests -k "18 or rebuild" 14 passed). Thanks for killing the server.

Keep the job short: run only the I.18 certify tests (-k i18); say in the bell that the full suite was not run. Commit as soon as they pass, before belling.

Goal: PROPS "I.18-engine" in euclid_certify.py, as I.17-engine: Lean certifies the engine's own PROP18_STEPS_DATA (read live; euclid_engine.py ~3640-3675; grep + sed <= 120 lines). Statement and imports verbatim from /home/joe/code/LeanEuclid/Book/Prop18.lean (lines 1-10). Mapping: proposition_3 (as q d), line_from_points, proposition_16, proposition_5' with the engine's args; sum_angles_onlyif is emitted as sum_angles_onlyif (NOT sum_angles_if): Lean then checks the engine's own direction. Its premises include b.sameSide d BC... (read the axiom at Transfer.lean:90 and check what Lean must establish for them; if euclid_finish cannot discharge them, report what is missing rather than adding steps silently; derived facts become checked euclid_assert as before).
Tests (as I.17): emission pinned with sha of the written file (say its path); monkeypatched engine data followed; certifies; a false euclid_assert (e.g. ∠ a:b:d < ∠ a:d:b after proposition_5') -> refused, no lean orphans; dropping proposition_16 -> refused.
Files: scripts/euclid_certify.py, scripts/euclid_certify_test.py; output under /home/joe/code/LeanEuclid/tmp-certify only. kimi-4 has euclid_engine.py in progress (I.23): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push. Kill any test server you start.
Bell claude-9 back with a summary + commit sha, certify time and pin.
