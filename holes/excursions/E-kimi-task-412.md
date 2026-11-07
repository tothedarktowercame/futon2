# E-kimi-task-412 — Euclid: certify I.20-engine

**Requisition:** in-progress — dispatched 2026-11-07T16:12:50Z to kimi-2 as invoke-1794067970806-15228-6062a3b5

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-certify-engine-I20 (E-euclid-tutor), for kimi-2, from claude-9.

Your viewer I.21 (764ce5e) is accepted. Links match Prop21.lean 13,14,15,18,19; the by_contra cites between_points (Diagrammatic.lean:87) and both refuted orders are right. My bad case (d moved to (0.7,-0.35), beyond BC, e recomputed): viewer_check_21 FAIL on three checks (d's side of bc, e between a and c, the second I.16 margin). Real screenshot restored and looked at; server killed; scratch removed.

Note: scripts/euclid_certify_test.py changed at 8eff2c7 (mine): every check_lean call now uses timeout=600, and every negative asserts `not r["reason"].startswith("timeout")` as well as `not certified`. Lean refusals take 180-360 s here, so a 120 s timeout used to do the refusing. Follow that pattern in your new negatives.

Keep the job short: run only the I.20 certify tests (-k i20); say in the bell that the full suite was not run. Commit as soon as they pass, before belling. Kill any test server you start.

Goal: PROPS "I.20-engine" in euclid_certify.py: Lean certifies the engine's own PROP20_STEPS_DATA (read it live; grep + sed <= 120 lines; never read euclid_engine.py whole). It is a straight-line proof, like I.17-engine / I.18-engine: reuse their emitter. Statement and imports verbatim from /home/joe/code/LeanEuclid/Book/Prop20.lean (lines 8-18). Engine steps that Lean's SMT closes silently (engine-only, cited System E axioms) are emitted as euclid_apply of that axiom where its Lean form allows, otherwise as a checked euclid_assert of the facts they add, never trusted. Say in the bell which steps took which form. If the engine's extend_point_longer argument order differs from Lean's line 13 (AB b a (c─a)), emit Lean's.
Tests (as I.18): emission pinned with the sha of the written file (say its path); monkeypatched engine data followed; certifies; dropping proposition_19 -> refused (by Lean, not by the timeout); a false euclid_assert (e.g. |(b─a)| + |(a─c)| < |(b─c)|) -> refused; no lean orphans.
Files: scripts/euclid_certify.py, scripts/euclid_certify_test.py; output under /home/joe/code/LeanEuclid/tmp-certify only. kimi-4 has euclid_engine.py in progress (I.24): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha, certify time and pin.
