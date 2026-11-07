# E-kimi-task-408 — viewer I.20

**Requisition:** in-progress — dispatched 2026-11-07T15:51:36Z to kimi-2 as invoke-1794066696391-15217-48bdd0c3

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-viewer-I20 (E-euclid-tutor), for kimi-2, from claude-9.

Your c556c5e (I.19-engine certifies) is accepted: the emitted by_contra/by_cases matches Prop19.lean 12-17 plus the checked euclid_assert |(a─b)| > |(a─c)| in branch 2; pin 16ac3b4bdb39518a = sha of the written file. Good catch on the false assert after proposition_5' (ex falso). My bad case (by_cases on |(a─c)| = |(b─c)| instead): refused with "Could not prove" in both branches — after 361 s.
Which is the finding: Lean's refusals take 180-360 s here, while the negative tests used timeout=120 and asserted only "not certified", so they were passing on the timeout (your summary said so). A timeout would also "refuse" a correct proof. I have changed every such negative in euclid_certify_test.py to timeout=600 plus an assertion that the reason is not a timeout (uncommitted in my hands while it runs; do NOT edit euclid_certify_test.py in this job).

Keep the job short: run only the I.20 trace tests plus rebuild-matches-disk (-k "20 or rebuild"), commit, then viewer_check_20, then bell. Kill any test server you start, by pid.

Goal: a Book trace for I.20 in the viewer (prop20.json): two sides of a triangle together exceed the third (|b─a| + |a─c| > |b─c|).
Lean, /home/joe/code/LeanEuclid/Book/Prop20.lean lines 12-17: extend_point_longer AB b a (c─a) as d'; proposition_3 a d' a c AB AC as d (|a─d| = |a─c|, d beyond a on line b-a); line_from_points d c as DC; proposition_5' a c d AC DC AB (∠a:c:d = ∠a:d:c); sum_angles_onlyif c b d a BC DC (∠b:c:d = ∠b:c:a + ∠a:c:d, a inside the angle); proposition_19 b c d BC DC AB (∠b:c:d > ∠b:d:c, so |b─d| > |b─c|). Engine: PROP20_HYPS/STEPS/GOAL (~3960-3996; grep + sed <= 120 lines); same steps, Lean's arg orders. Lean links by text search, checked against the file.
Placement: a scalene triangle (reuse the I.16-I.19 triangle if it suits); d' and d constructed (d on the ray b→a beyond a, |a─d| = |a─c|). verify20: between b a d; |a─d| = |a─c|; ∠a:c:d = ∠a:d:c; ∠b:c:d = ∠b:c:a + ∠a:c:d; ∠b:c:d > ∠b:d:c; |b─d| = |b─a| + |a─c|; goal with a visible margin. Last step's bar says why: |b─a| + |a─c| = |b─d| > |b─c|, because in triangle b-c-d the angle at c exceeds the angle at d. index.html option "I.20 two sides exceed the third". Bad placements watched to fail (at least: d between b and a; |a─d| ≠ |a─c|). viewer_check_20.py (copy viewer_check_18.py, port 8121); look at the screenshot. Other traces byte-identical.
Files: scripts/euclid_trace.py, scripts/euclid_trace_test.py, viewer index.html, traces/prop20.json, ~/notes/euclid-cs/viewer_check_20.py. kimi-4 has euclid_engine.py dirty (I.23'): do not touch or stage it. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit sha + screenshot path.
