# E-kimi-task-383 — Euclid I.16 second conjunct replay

**Requisition:** completed — 2026-11-07T12:13:40Z, job invoke-1794052700372-15114-858fbe62, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I16b (E-euclid-tutor), for kimi-4, from claude-9.

Your 6c69d59 (I.15) is accepted. My checks: hypotheses alone, the symm-parts steps alone, and each single citation alone entail neither equality; the full proof entails both; main 1-15 pass; 97 engine tests.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges ≤ 120 lines) and commit early.

Goal: the SECOND conjunct of Book I.16 (/home/joe/code/LeanEuclid/Book/Prop16.lean, read it: 33 lines), the part Euclid proves: from formTriangle a b c AB BC AC ∧ between b c d, show ∠a:c:d > ∠b:a:c. Lean's second bullet (lines ~25-32): proposition_10 a c AC as e; line_from_points b e as BE; extend_point_longer BE b e (b─e) as f'; proposition_3 e f' b e BE BE as f; line_from_points f c as FC; proposition_15 a c b f e AC BE; proposition_4 e b a e f c BE AB AC BE FC AC; euclid_finish. The first conjunct (Lean's "Omitted by Euclid" bullet) is a later packet; do not do it here.
Files: scripts/euclid_engine.py, scripts/euclid_engine_test.py only.
Do:
1. CONSTRUCTIONS "proposition_15" as a cited lemma, verbatim from Book/Prop15.lean:6 (premises = its hypotheses; makes both angle equalities as metric facts). Bad case test: citing it with between a e b replaced by between e a b is refused.
2. "extend_point_longer" verbatim from its SystemE source (grep LeanEuclid/SystemE for it; cite file:line). If proposition_3 / proposition_4 argument orders in the engine differ from Lean's, map them and say so in a comment.
3. PROP16B_HYPS_DATA (formTriangle destructured as at earlier props; between b c d), PROP16B_STEPS_DATA following Lean's bullet, PROP16B_GOAL = ∠a:c:d > ∠b:a:c by LA entailment (strict). The strict inequality needs ∠a:c:d = ∠a:c:f + ∠f:c:d with ∠f:c:d > 0, i.e. diagram facts that put f inside angle a:c:d (Lean's SMT finds them). Supply each one as a cited engine step (sum_angles_onlyif, pasch/same-side rules already in the engine, or a new rule verbatim from SystemE with file:line); no h-sm. If one cannot be obtained, STOP and report which fact and why, rather than adding it as an assumption.
4. main() gains "16b"; footprints I.1-I.15 unchanged; pin I.16b's.
5. Bad cases kept as tests: (a) drop proposition_4: goal not entailed; (b) drop proposition_15: goal not entailed; (c) the non-strict/wrong direction ∠a:c:d < ∠b:a:c is not entailed (and the store stays sat).
Gates: euclid_engine_test (all), count + wall. Do not touch certify (kimi-2 has euclid_certify*.py dirty), review, trace or viewer files. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha, the I.16b footprint, and the list of diagram facts with their sources.
