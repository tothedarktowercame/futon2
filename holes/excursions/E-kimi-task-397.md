# E-kimi-task-397 — Euclid certify I.16b-engine

**Requisition:** in-progress — dispatched 2026-11-07T14:16:16Z to kimi-2 as invoke-1794060976390-15172-c4c7a95b

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-certify-engine-I16b (E-euclid-tutor), for kimi-2, from claude-9.

Your viewer I.16 work was good; the job hit the Kimi API timeout (HttpTimeoutException) while your tests ran, before the commit and viewer_check_16. I reviewed it, regenerated prop16.json (byte-identical), wrote ~/notes/euclid-cs/viewer_check_16.py (ALL OK on the real trace; FAIL with d between b and c) and committed it as c3df0da. Nothing for you to redo.

To keep the job short (the timeout seems to hit long jobs): run only the I.16b tests (-k i16), not the full certify suite; tell me in the bell that the full suite was not run.

Goal: PROPS "I.16b-engine" in euclid_certify.py, as I.13/I.14/I.15-engine: Lean certifies the engine's own PROP16B_STEPS_DATA (read live; read euclid_engine.py with grep + sed ranges <= 120 lines, never whole). The Lean statement is proposition_16 (both conjuncts) from /home/joe/code/LeanEuclid/Book/Prop16.lean; the engine proves only the second. Emit a theorem with the SECOND conjunct as its statement (∠ a:c:d > ∠ b:a:c), named e.g. proposition_16b, and say so in a comment; do not change Prop16.lean. Imports: those Prop16.lean has. Mapping: between_symm_parts -> between_symm (as before); pasch_3_opposing -> pasch_3 (as I.14); opposing_opposing_same_side is DERIVED -> checked euclid_assert (as I.14); sum_angles_onlyif as is; proposition_10/3/4/15 and extend_point_longer with the engine's args (compare with Lean's lines 26-32 and say if any differ).
Tests (as I.15): emission pinned with sha (of the file actually written: say its path); monkeypatched engine data followed; certifies; a false euclid_assert in place of the derived same-side fact -> refused, no lean orphans; dropping proposition_4 -> refused.
Files: scripts/euclid_certify.py, scripts/euclid_certify_test.py; output under /home/joe/code/LeanEuclid/tmp-certify only. kimi-4 has euclid_engine.py possibly in progress (I.22): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push. Commit as soon as the I.16b tests pass, before writing the bell.
Bell claude-9 back with a summary + commit sha, certify time and pin.
