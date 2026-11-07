# E-kimi-task-406 — certify I.19-engine

**Requisition:** completed — 2026-11-07T15:44:19Z, job invoke-1794065095416-15205-f81b0164, state done

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-certify-engine-I19 (E-euclid-tutor), for kimi-2, from claude-9.

Your viewer I.19 (01fbbe4) is accepted. Links match Prop19.lean 12-16; the step-4 bar reads correctly (proposition_18 a c b gives ∠b:c:a > ∠a:b:c). My bad case (a moved to (1.1,1.1), so ∠a:b:c < ∠b:c:a): viewer_check_19 FAIL on hypothesis and goal margins; real screenshot restored, server killed, scratch removed. Good that you caught the prop-17 note clobber before committing (rebuild-matches-disk confirms prop17.json is unchanged).

Keep the job short: run only the I.19 certify tests (-k i19); say in the bell that the full suite was not run. Commit as soon as they pass, before belling. Kill any test server you start.

Goal: PROPS "I.19-engine" in euclid_certify.py: Lean certifies the engine's own PROP19_STEPS_DATA (read live; grep + sed <= 120 lines). It is a by_contra with two branches, like I.14-engine: reuse how I.14-engine emits by_contra / by_cases (check Lean's shape in /home/joe/code/LeanEuclid/Book/Prop19.lean lines 12-17: by_contra; by_cases |(a─c)| = |(a─b)|; two bullets, each euclid_apply then euclid_finish). The engine's branch split must be emitted as a Lean by_cases whose two cases are exactly the engine's branch hypotheses (eq_len a c a b, then gt_len a b a c — under the negated goal Lean gets the second bullet as ¬(|a─c| = |a─b|); say whether euclid_finish closes it with that or whether the engine's gt_len form needs an assert, and if so emit it as a checked euclid_assert, never trusted). Statement and imports verbatim from Prop19.lean.
Tests (as I.18): emission pinned with sha of the written file (say its path); monkeypatched engine data followed; certifies; dropping proposition_18 from branch 2 -> refused; a false euclid_assert in branch 1 (e.g. ∠ a:b:c < ∠ b:c:a) -> refused; no lean orphans.
Files: scripts/euclid_certify.py, scripts/euclid_certify_test.py; output under /home/joe/code/LeanEuclid/tmp-certify only. kimi-4 has euclid_engine.py in progress (I.22''): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha, certify time and pin.
