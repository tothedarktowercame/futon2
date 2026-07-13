HANDOFF ZU-4 — TOOL-FAILURE VISIBILITY + CI-IN-THE-LOOP (M-zaif-harness, 2026-07-13)
Read first: futon2/holes/M-zaif-harness.md §Slice ZU-4 (the spec, incl. Joe's
cache-to-queue policy decision) + the zai-13 pur_update incident context in the
same doc's ZU-1/ZU-4 sections.
Goal, three parts: (1) tool errors verbatim in follow-mode display + failed calls
persisted in U1 as first-class turn records (futon3c zai_api.clj seam); (2)
session-end transcript sweep (rides par_punctuate) emitting :bug/* typed events
to the store (tool, args-sha, error text, session-id, count); (3) bug-queue view
in futon2 z1_views.clj (same shape as mission-status; answers in seconds).
Policy: errors NEVER stop the line except when breaking the current slice's own
acceptance path — default is queue-and-continue.
CAUTION: zai_api.clj is the LIVE harness — no hot :reload of serving namespaces;
land code + tests; activation rides the next restart (note this in the checkpoint).
Acceptance: (a) deliberately-failing pur_update shows error in follow buffer +
lands in U1; (b) sweep emits :bug/* for it; (c) bb z1_views.clj bug-queue lists it
in seconds; (d) doc checkpoint in the same commit. Gates: clj-kondo, check-parens,
zaif tests green, 30-min job cap. Bell claude-2 with summary + shas.
