# E-kimi-task-400 — certify I.17-engine

**Requisition:** in-progress — dispatched 2026-11-07T14:41:10Z to kimi-2 as invoke-1794062470772-15185-631131e9

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-certify-engine-I17 (E-euclid-tutor), for kimi-2, from claude-9.

Your viewer I.17 (557e3b5) is accepted: Lean lines 12-14 match Prop17.lean, and my bad case (d moved between b and c) fails viewer_check_17 on between b c d, the linear pair and the I.16 step. One note: an http.server on :8115 (the I.14 viewer check) was still running after two hours; I stopped it. Please kill any test server you start by its pid before belling.

To keep the job short: run only the I.17 tests (-k i17), not the full certify suite; say in the bell that the full suite was not run.

Goal: PROPS "I.17-engine" in euclid_certify.py, as I.15/I.16b-engine: Lean certifies the engine's own PROP17_STEPS_DATA (read live; euclid_engine.py lines ~3600-3611; grep + sed ranges <= 120 lines, never whole). Statement and imports from /home/joe/code/LeanEuclid/Book/Prop17.lean (theorem proposition_17, both imports Book.Prop13 and Book.Prop16). Mapping: extend_point as is; between_symm_parts -> between_symm (as before); proposition_16 and proposition_13 with the engine's args. Note proposition_16 in Lean is a conjunction; check that euclid_apply of it works as in Prop17.lean line 13 (it does there) and say if your emitted line differs from Lean's.
Tests (as I.16b): emission pinned with sha (of the file actually written: say its path); monkeypatched engine data followed; certifies; a false euclid_assert (e.g. ∠ a:c:d < ∠ a:b:c after the proposition_16 step) -> refused, no lean orphans; dropping proposition_13 -> refused.
Files: scripts/euclid_certify.py, scripts/euclid_certify_test.py; output under /home/joe/code/LeanEuclid/tmp-certify only. kimi-4 has euclid_engine.py in progress (I.22'): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push. Commit as soon as the I.17 tests pass, before writing the bell.
Bell claude-9 back with a summary + commit sha, certify time and pin.
