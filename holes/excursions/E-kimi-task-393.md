# E-kimi-task-393 — Euclid certify I.15-engine

**Requisition:** completed — 2026-11-07T13:56:36Z, job invoke-1794058167400-15152-4c09efbc, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-certify-engine-I15 (E-euclid-tutor), for kimi-2, from claude-9.

Your b02ebcd (viewer I.15) is accepted. Lean lines 12-14 match the three proposition_13 steps; rejecting a right crossing on purpose is agreed. My bad case (a moved to (0.6,0) so e is not between a and b, served on a spare port) failed viewer_check_15 on between, the equalities and the spread. Note: my bad run overwrote kimi-viewer-prop15.png; I regenerated it from the real trace. Also: you pip-installed playwright into the mfuton venv; next time use ~/code/chatgpt-tui/.venv/bin/python or `uv run -q --with "playwright==1.58.0"` instead of installing into another project's venv.

Goal: PROPS "I.15-engine" in euclid_certify.py, as I.13/I.14-engine: Lean certifies the engine's own PROP15_STEPS_DATA (read live; kimi-4's 6c69d59; read euclid_engine.py with grep + sed ranges <= 120 lines, never whole). Statement verbatim from /home/joe/code/LeanEuclid/Book/Prop15.lean; imports SystemE + Book.Prop13. The three between_symm_parts steps map as at I.13/I.14 (between_symm whole axiom); the three proposition_13 citations with the engine's argument orders (they should equal Lean's lines 12-14: say if not). Any derived fact the steps consume is a checked euclid_assert.
Tests (as I.14): emission pinned with sha; monkeypatched engine data is followed; certifies; a false euclid_assert in place of one step (your choice, say which) -> refused with no lean orphans; dropping one proposition_13 -> refused (Lean's euclid_finish cannot close the conjunction). Run the I.15 tests isolated first, then the full certify suite once (it takes ~14 min; note any flake with which test).
Files: scripts/euclid_certify.py, scripts/euclid_certify_test.py; output under /home/joe/code/LeanEuclid/tmp-certify only (LeanEuclid otherwise read-only). kimi-4 has euclid_engine.py in progress (I.20): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha, certify time and pin.
