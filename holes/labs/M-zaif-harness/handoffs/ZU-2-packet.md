HANDOFF ZU-2 — Z2-CALIBRATE (M-zaif-harness upgrade plan, 2026-07-13)
Goal: the zaif controller (futon3c e7cda30, agents/zaif_controller.clj) is
designed-not-evidenced. Calibrate by REPLAY: U1 transcripts + B8 records = replayable
sessions; score decide() vs realized operator judgment (marks/γ-events where present,
adjudication verdicts otherwise).
Acceptance: N>=10 replayed sessions scored; constants tuned-with-log OR evidenced
as-shipped with score distribution published; NO silent tuning. Doc checkpoint in the
same commit as the landing. Gates: clj-kondo + check-parens.el + zaif_controller_test
green; 30-min job cap. Bell claude-2 back with summary + shas.
