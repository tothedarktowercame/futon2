# E-kimi-task-399 — Euclid proposition_22' replay + lemma

**Requisition:** in-progress — dispatched 2026-11-07T14:37:30Z to kimi-4 as invoke-1794062250421-15182-056cb427

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I22prime (E-euclid-tutor), for kimi-4, from claude-9.

Your d52cedd (I.22) is accepted. Good discovery: intersection_circle_circle_1 with a = i, b = h, and the three-branch by_cases on g, f, i (I checked the dead order f-g-i forces i = h). I checked _fm_sat raises FMTooLarge on overflow rather than guessing, so the pivot change cannot make a false entailment. My checks: three goals hold, store not contradictory, replay 3.9 s; with inequality 3 reversed or made an equality, refused at icc1's outside h DKL.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines). Commit after step 1, then after step 2.

Goal: proposition_22' (the variant I.23 cites) in the engine: replay, then cited lemma.
Lean, /home/joe/code/LeanEuclid/Book/Prop22.lean, theorem proposition_22' (second theorem): given a line FE with f, e on it, plus the three lengths and triangle inequalities, ∃ k g, g on FE ∧ ¬(between g f e) ∧ |f─k| = |aa'| ∧ |f─g| = |bb'| ∧ |k─g| = |cc'|. Steps: two extend_point_longer, three proposition_3, two circle_from_points, intersection_circle_line_extending_points, intersection_circles, two point_on_circle_onlyif. It is I.22's construction started from a given segment instead of an arbitrary point: reuse your I.22 renderings (icc1, the by_cases).
1. PROP22P_HYPS/STEPS/GOAL with Lean's argument orders; main() "22'"; the goal includes on_line g FE and ¬(between g f e): say how the negated between is expressed as a goal query (the engine may need a goal kind or a positive equivalent from trichotomy: say which, cite) — if it needs a new kind, thread it everywhere as before. Tests: replay passes; hyps alone entail none; drop inequality 3 -> refused; store not contradictory.
2. CONSTRUCTIONS "proposition_22'" verbatim (makes k and g with all the conclusions; existential construction, like proposition_3). Tests: cites and fires on a fresh state; refused when one triangle inequality is missing.
Gates: euclid_engine_test (all). Do not touch certify, trace, review or viewer files (kimi-2 has trace/viewer for I.17). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas and the steps with sources.
