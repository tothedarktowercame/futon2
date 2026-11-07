# E-kimi-task-402 — replay I.23

**Requisition:** in-progress — dispatched 2026-11-07T14:58:58Z to kimi-4 as invoke-1794063538841-15193-96e13b74

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I23 (E-euclid-tutor), for kimi-4, from claude-9.

Your a853bf1 + 26b0000 (I.22' replay and lemma) are accepted. My checks: the lemma is Prop22.lean:34-40 verbatim; between_trans_out is Diagrammatic.lean:78 verbatim; with inequality 3 reversed (|aa'| > |bb'| + |cc'|, row confirmed in the store this time) the first proposition_3 is refused at gt_len f e'' a a'; with the by_contra step removed replay fails "conclusion fact missing: not_between g f e"; adding between g f e to the final store makes it contradictory; the real final store is not.

Same reading rule (never read euclid_engine.py whole; grep + sed ranges <= 120 lines). Commit after step 1, then after step 2, before belling.

Goal: replay I.23 (copy an angle onto a given line at a given point). Lean: /home/joe/code/LeanEuclid/Book/Prop23.lean, theorem proposition_23 (lines 8-32): distinctPointsOnLine a b AB ∧ formRectilinearAngle d c e CD CE -> ∃ f, f ≠ a ∧ ∠f:a:b = ∠d:c:e. The proof is a by_cases on d.onLine CE.
1. The main branch (¬ d.onLine CE, Lean lines 22-32): line_from_points d e as DE; the three proposition_20 citations (lines 24-26, Lean's arg orders); proposition_22' c d c e e d a b CD CE DE AB as (f, g); line_from_points a f, f g; proposition_8 c d e a f g CD DE CE FA FG AB. Goal: neq f a and ∠f:a:b = ∠d:c:e (say which goal query kind; _eq_ang if it exists, else thread a new one through all five sites). Check that proposition_22''s three gt_sum_len entail premises are met by exactly the three proposition_20 rows (say so). Tests: this branch replays (as PROP23_MAIN_* or inside the by_cases, your call); hyps alone do not entail the goal; dropping one proposition_20 -> refused at proposition_22''s entail premise; store not contradictory.
2. The degenerate branch (d.onLine CE, lines 13-21, "Omitted by Euclid"): a nested by_cases on ∠d:c:e = 0. Branch 0: f = b. Branch ≠0: Lean asserts ∠d:c:e = 2∟ by SMT, then extend_point AB b a as b' and f = b'. Write out what the SMT does with System E citations (degenerated_angle_if / _onlyif, Transfer.lean:63/67; and whatever gives 2∟ when c is between d and e — find it, cite it, do not use proposition_13 unless Lean's SMT could not do without it; say). Then assemble the whole proof as by_cases on d.onLine CE with both branches, the exhaustiveness argument at the step as before. Tests: full replay passes; each degenerate sub-branch refused when its key citation is dropped; store not contradictory; footprints of other replays unchanged.
The cited proposition_23 lemma is the NEXT packet, not this one.
Gates: euclid_engine_test (all). Do not touch certify, trace, review or viewer files (kimi-2 has trace/viewer for I.18). Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas and the steps with sources.
