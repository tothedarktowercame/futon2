# E-kimi-task-423 — Euclid: play page plain words

**Requisition:** in-progress — dispatched 2026-11-07T19:05:03Z to kimi-2 as invoke-1794078303917-15367-014b4fee

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-plain-words (E-euclid-tutor), for kimi-2, from claude-9.

Your coverage referee (d63d5e6) is accepted with fixes of mine; read them: b3ef275 (a case covers a possibility only if the possibility entails the case; the conclusion likewise), 30194c8 (crossed splits whose missing member is a conjunction, phrased with "and" in play.html; same_side/not_same_side; stored "either" disjunctions; symmetric side facts), 974487e (peq/neq symmetric). Every engine split in I.6-I.25 is covered.

Goal: the page speaks to a student, not to the engine's author. Files: viewer/play.html, scripts/euclid_play.py (only the text functions you add; kimi-4 may edit its GOALS table, stage only your hunks), scripts/euclid_play_test.py.
1. Refusals in plain words. Today a student sees e.g. "Not yet shown: a ≠ a", "bad arity for ...", "by_contra branch [('same_side', ...)] did not reach a contradiction (witness of non-contradiction: None)", "unknown rule 'x' (not in I.5's palette)". Write a student-register message for every refusal path in play() (missing premise by fact kind; a point named twice; a case that is not contradictory: "This case does not lead to a contradiction yet: keep going inside it, or check its assumptions"; a case that does not reach the conclusion; arity; a proposition not yet proved; a new name already in use, the ("fresh", name) premise: "b already exists; choose a new name, or show that the new point is b"). Keep the engine's raw text available under a "details" toggle, never instead of it.
2. The rule dropdown labelled by what each rule does, in a sentence a student understands ("draw the line through two points", "draw the circle with centre a through b", "where two circles meet", "I.4: two triangles with two sides and the angle between them equal"), grouped: constructions, facts about points/lines/circles (the diagrammatic axioms), facts about lengths/angles, earlier propositions. The engine name stays visible in small type. A table in euclid_play.py (RULE_LABELS) covering every palette entry; a test that every CONSTRUCTIONS name has a label (so a new rule fails the test until labelled).
3. Argument labels: where rule_info's arg names are a, b, L, show a hint from the rule's premises when cheap (e.g. for intersection_circles "first circle", "second circle"); otherwise leave as is. Do not spend long on this one.
Tests: the label-coverage test; one test per refusal kind that the student text is produced and contains no Python repr (no "('", no "None"); extend ~/notes/euclid-cs/play_check.py (port 8124): a refused move shows the plain text, "details" reveals the raw one; screenshot ~/notes/euclid-cs/kimi-play-words.png (look at it). Never use port 8100. Kill servers by pid.
Rules: do not modify euclid_engine.py, euclid_trace.py, index.html, or the coverage functions. Explicit paths only; no commit -a, no stash, no push. Commit when checks pass, before belling.
Bell claude-9 back with a summary + commit sha, test counts, and five before/after refusal texts.
