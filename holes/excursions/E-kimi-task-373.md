# E-kimi-task-373 — E-euclid-tutor: replay Book I.13 (angle sum) in the engine (reassigned from kimi-1 after API timeout)

**Requisition:** in-progress — dispatched 2026-11-07T09:53:31Z to kimi-3 as invoke-1794045211249-15064-954adb26

Clocked in by claude-9 for kimi-3 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Note (claude-9): this packet was first sent to kimi-1, whose job died at 09:53Z on a model API timeout before any edits. You (kimi-3) are new to E-euclid-tutor. Orientation: the engine is /home/joe/code/futon0/scripts/euclid_engine.py (large; read with grep + targeted sed ranges, not whole). Look at how PROP12_HYPS_DATA / PROP12_STEPS_DATA, the CONSTRUCTIONS entries for proposition_10 and exists_point_opposite, _by_contra, and main()'s replay queries are written, and copy that style. Tests: ~/code/futon6/.venv/bin/python -m pytest -q <file> run from scripts/. The first paragraph below is addressed to kimi-1 about earlier work; it is context only.

Packet P-euclid-replay-I13 (E-euclid-tutor), for kimi-1, from claude-9.

Your 29aa6a3 (I.12 viewer trace) is accepted. I opened kimi-viewer-prop12.png myself, reran viewer_check_12.py against :8100 (ALL OK), and ran it against a copy of prop12.json with h moved 0.3 along AB: it fails on the angle (108.4 deg). Your three bad cases are right. I found one more that got through: push e and g outward along AB, symmetric about h. h stays the midpoint, |c-e| = |c-g| and the angle stays right, so verify12 passed though e, g are off circle EFG. Fixed in 6636755 (|c-e| = |c-d|, e and g on AB, e != g, plus a test). The lesson again: a check named after a construction must check what the construction makes (here: on the circle), not only what the proof later uses.

Goal: replay Book I.13 (Book/Prop13.lean, /home/joe/code/LeanEuclid) in the engine: ∠c:b:a + ∠a:b:d = ∟ + ∟.
The Lean proof is by_cases ∠c:b:a = ∠a:b:d; branch 1 is euclid_finish alone; branch 2 is proposition_11' d c b a CD as e, line_from_points b e as BE, euclid_finish. Most of the work is the angle arithmetic Lean leaves to SMT.
Files: /home/joe/code/futon0/scripts/euclid_engine.py and euclid_engine_test.py only.

Do:
1. Add proposition_11' as a cited rule, verbatim from Book/Prop11.lean:24 (premises dpl a b AB, between a c b, not_on_line x AB; makes f with same_side f x AB and ∠a:c:f = ∟). Any axiom you add must be read against its SystemE line, cited in the rule.
2. The case split: the engine has by_contra (used in I.6, I.10). Decide whether I.13's by_cases can be expressed with it, or needs a small by_cases construct (both branches must reach the goal; the split must be exhaustive by trichotomy/excluded middle of the linear store). State the decision in a comment. If you add a construct, add a test where one branch does NOT reach the goal and the whole step is refused.
3. PROP13_HYPS_DATA / PROP13_STEPS_DATA, with the goal checked by LA entailment of ∠c:b:a + ∠a:b:d = 2∟ in main() and a test. Any SMT-sourced input (provenance h-sm) must be named and justified in a comment; prefer engine steps.
4. Bad cases you must build and keep as tests: (a) drop the between d b c hypothesis: the goal must not be entailed; (b) the goal with the wrong sum (∠c:b:a + ∠a:b:d = ∟) is not entailed; (c) proposition_11' with x on AB is refused (MissingPremise).
5. Footprints: engine main final counts for I.1-I.12 unchanged (37/8, 112/38, 60/22, 198/79, 442/227, 163/51, 318/158, 178/68); report I.13's.
Gates: all six files (euclid_engine_test, euclid_search_test, euclid_review_test, euclid_clones_test, euclid_clone_diagram_test, euclid_trace_test): counts and wall time. kimi-2 is editing euclid_review.py / euclid_review_test.py (and maybe euclid_search.py): do not touch or stage them. Stage explicit paths only; no commit -a, no stash, no push.
Bell claude-9 back with a summary + commit shas.
