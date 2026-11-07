# E-kimi-task-386 — Euclid I.17 replay + proposition_17 lemma

**Requisition:** completed — 2026-11-07T12:35:49Z, job invoke-1794054559011-15127-4868569d, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I17 (E-euclid-tutor), for kimi-4, from claude-9.

Your aba2a9f (I.16 first conjunct + proposition_16) is accepted. My checks: hyps alone and steps without sum_angles_onlyif do not entail ∠a:c:d > ∠c:b:a (sat); dropping opposing_opposing_same_side is refused at its consumer; the equality is not entailed; _fact_holds answers the goal; main 1-15,16a,16b pass; 109 engine tests. Note: the proposition_16 premises use b≠c, a≠c, c off AB rather than formTriangle's three line inequalities (Relations.lean:58); I checked these are equivalent given the incidences, as at PROP6. Keep that comment pattern.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges ≤ 120 lines) and commit early.

Goal: replay Book I.17 (/home/joe/code/LeanEuclid/Book/Prop17.lean): formTriangle a b c AB BC AC → ∠a:b:c + ∠b:c:a < ∟ + ∟. Lean: extend_point BC b c as d; proposition_16 a b c d AB BC AC; proposition_13 a c b d AC BC; euclid_finish.
Files: scripts/euclid_engine.py, scripts/euclid_engine_test.py only.
Do:
1. A goal/fact form for "sum of two angles < 2∟" (e.g. "_lt_sum_rt_ang" u v w x y z, an LA query ∠u:v:w + ∠x:y:z − 2∟ < 0), handled in BOTH replay's goal check and _fact_holds (my 8d2ada7 found _gt_ang missing from _fact_holds; do not repeat that), and in apply_step's make loop if you make it a lemma conclusion.
2. PROP17_HYPS_DATA (formTriangle destructured as at PROP16), PROP17_STEPS_DATA (Lean's three steps plus any named engine-only steps, explained), PROP17_GOAL.
3. CONSTRUCTIONS "proposition_17" as a cited lemma verbatim from Prop17.lean (for I.18 onwards). Test: refused when c is on AB.
4. main() "17"; all existing footprints unchanged; pin I.17's.
5. Bad cases kept as tests: (a) drop proposition_16: goal not entailed; (b) drop proposition_13: goal not entailed; (c) the non-strict/wrong form (sum > 2∟, or sum = 2∟) not entailed and store sat; (d) hyps alone do not entail the goal.
Gates: euclid_engine_test (all), count + wall. Do not touch certify, review, trace or viewer files (kimi-2 has trace/viewer dirty). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha and the I.17 footprint.
