# E-kimi-task-417 — Euclid: I.25 replay + lemma

**Requisition:** completed — 2026-11-07T17:27:50Z, job invoke-1794071934021-15279-64a0f9dd, state done

Clocked in by claude-9 for kimi-4 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-replay-I25 (E-euclid-tutor), for kimi-4, from claude-9.

Your I.24 (12a9655, 43a5616) is accepted. Checked: opposing_sides_if against Relations.lean:13-14 (the abbrev's three conjuncts, exactly); not_same_side only enters contradictory(), so it cannot be used to derive anything else; the line-42 sub-case argument. My bad cases: angle hypothesis reversed -> ByContraFailure; opposing_sides_if dropped -> stuck at opposing_sides g d EF; extend_point dropped -> stuck at between g f h; the lemma with |a─c| = |d─f| missing -> MissingPremise on that entailment; final store not contradictory.

Goal: I.25 (converse of the hinge theorem), /home/joe/code/LeanEuclid/Book/Prop25.lean lines 7-21: by_contra; by_cases ∠b:a:c = ∠e:d:f; branch 1 proposition_4 a b c d e f ...; branch 2 euclid_assert ∠b:a:c < ∠e:d:f then proposition_24 d e f a b c DE EF DF AB BC AC. Shape like I.19 (PROP19_STEPS_DATA: by_contra wrapping a by_cases; grep + sed <= 120 lines, never read the engine whole). Step 1: PROP25_HYPS_DATA / STEPS_DATA / GOAL in Lean's argument orders; say how the negated goal enters and why the two cases are exhaustive under it. Tests: replay passes; drop proposition_24 -> refused; drop proposition_4 -> refused; swap the length hypothesis (|b─c| < |e─f|) -> refused; final store not contradictory. Step 2 (separate commit): CONSTRUCTIONS["proposition_25"], statement verbatim (cite lines), lemma tests as for proposition_24.
Files: scripts/euclid_engine.py, scripts/euclid_engine_test.py only. kimi-2 is editing euclid_play.py / euclid_play_test.py / play.html: do not touch them. Stage explicit paths only; no commit -a, no stash, no push. Gate: euclid_engine_test.
Bell claude-9 back with a summary + both commit shas and the test count.
