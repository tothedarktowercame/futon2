# E-kimi-task-365 — E-euclid-tutor binding clash: finish zai-11's engine work with entailment-checked identification

**Requisition:** in-progress — dispatched 2026-11-07T08:33:21Z to kimi-1 as invoke-1794040401355-15051-c48c8d3d

Clocked in by claude-9 for kimi-1 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-binding-clash-2 (E-euclid-tutor), for kimi-1, from claude-9.

Context: zai-11 started P-euclid-binding-clash (~/notes/packets/P-euclid-binding-clash.md, read it first) and was cut off by its provider's usage limit. Its UNCOMMITTED work is in the shared checkout /home/joe/code/futon0: scripts/euclid_engine.py, scripts/euclid_engine_test.py, scripts/euclid_search.py (read `git diff` on those three). You are finishing it. Nobody else is editing those three files now.

What zai-11 built (keep it): State.declared / State.reserved; assume() declares names from "h"/"hypothesis"/"given" facts and reserves names that only other provenances (h-sm, by_contra branches) mention; apply_step refuses a fresh binding to a declared name with MissingPremise(("fresh", name)); a reserved name takes exactly one binding. Three new tests. I.5's GB/FC hypotheses were relabelled "h" -> "h-sm".

What is wrong (found in review, my run): euclid_review_test.py::test_fixture_b_useless_step_valid_no_progress now FAILS. The fixture's step 2 is `line_from_points a b` bound as AB, when the hypothesis line AB already exists. That is a legitimate move: Lean accepts it (as AB shadows the old variable), and in the engine identifying the new line with AB is sound, because a and b are already on AB and two distinct points determine the line. The step adds nothing; review must call it valid / no-progress, as before.

Design amendment (do this): a binding to a DECLARED name is an identification claim. Accept it iff every fact the step would add, with the fresh base replaced by that name, already holds in the store (relational facts present in s.facts or derivable as the engine's premise checks do; metric facts entailed via la_entails_eq / la_entails_lt / la_entails_eq_ang, lines 50 and 506-518). If accepted, the step adds nothing (report added == []). Otherwise refuse with MissingPremise(("fresh", name)) and an explanation that the step would identify the new object with the existing one without that following. Why sound: the step is existential; an existing object that already satisfies every fact it states is a valid witness.
Bad cases that must still refuse: proposition_1 (a, b, AB) bound {"d": "b"} (|bb| = |ab| is not entailed); a reserved name bound a second time.
Must accept: the useless-step fixture (line a b as AB); proposition_1 a b AB bound to an existing point c that already has |ca| = |ab| = |cb| (construct the state).

Also check: I.5's "h" -> "h-sm" relabel. Does it change any committed review export (scripts/euclid_review_export_test.py: committed traces must match a fresh export) or any footprint? If it does, prefer a design that leaves the I.5 data alone (for example, treat a name that a hypothesis mentions but no hypothesis puts on a point as reserved), and say which one you chose.

Files: scripts/euclid_engine.py, scripts/euclid_engine_test.py, scripts/euclid_search.py only. Do NOT edit euclid_review.py; if its student-facing text for the refusal reads badly, quote the text in your bell and I will fix it.
Gates (report counts and wall time for each): euclid_engine_test, euclid_search_test, euclid_review_test, euclid_clones_test, euclid_clone_diagram_test, euclid_trace_test, euclid_review_export_test (the last takes ~5 min). Python: ~/code/futon6/.venv/bin/python -m pytest, run from scripts/. Also `python3 euclid_engine.py 1,2,3,4,5,10,11` must end with the same final facts/constraints counts as before your change (I.11: 318 facts, 158 constraints).
Git: stage explicit paths only; no commit -a, no stash, no push. Other seats' dirty files: leave them. Commit with a message that credits zai-11's part.
Bell claude-9 back with a summary + commit shas.
