# Independent review acceptance — observed Status rubric v1

Reviewer: claude-15, 2026-09-14. Scope: codex-23 commits 3dee9855
(rubric + authority draft), 4e8b303e (receipts). Verdict: ACCEPTED.

Checked:

- File scope: two new documents plus receipts; nothing under src/ or
  test/; SPEC-measured-a-annotation-v1.md untouched. Zero-mass holds —
  no annotation, count, or mass anywhere in the diff.
- Every cited hash recomputed independently: belief.clj (status-set at
  37-42 is exactly the seven statuses), interest-event-vocabulary
  .flexiarg (lines 70-84 are the semantic grounding, including the
  82-84 reopening-resets-to-live wording the rubric relies on), and
  M-interim-director.md — all three whole-file sha256s match.
- The checkpoint-map reading is honest: the 257-258 note really does
  say the rationales paraphrase Joe pending adjustment; evt-close-2
  (278-286) really carries the described fields with :evidence/refs as
  strings, not retained bytes — so "grounded candidate shape, never an
  existing measured-A label" is the correct classification, and the
  3-grounded/4-absent/0-qualifying tally follows from the sources, not
  from the note's own assertions.
- Joe's ruling bounds all present and nowhere weakened: keyword
  choice, reused interest event, renamed argmax, model posterior, and
  close disposition are all in the never-qualifies list (plus interest
  :posterior-state, first appearance, elapsed time, and
  absence-of-contrary-evidence); zero cells unsmoothed; :reopened
  demands prior independently accepted terminal state AND renewed-live
  evidence AND an authorized reopening decision.
- Authority draft: exactly the six-key shape the accepted validator
  demands; all seven criterion ids; the rubric doc bound by sha256
  (recomputed: 5999521d... matches); observer/reviewer/acceptance are
  explicitly typed to-be-bound placeholders — the draft deliberately
  cannot pass the validator until a-labels binds real seats, which is
  the correct failure direction.
- Receipts: full check-parens driver recorded, one-EDN-form gate,
  six source pins all matching my recomputation, tally consistent.

Consequences recorded for downstream planning (not defects):

1. :reopened is doubly absent: its criterion requires a PRIOR accepted
   terminal annotation, and zero annotations exist — it cannot be the
   first observed status by construction.
2. The retained CP-close events cannot be promoted to labels as-is
   (no literal evidence bytes, no independent acceptance, rationales
   awaiting Joe's wording adjustment). a-labels therefore needs NEW
   qualifying observation acquisition — likely new retention at live
   closes — not relabeling of existing records. The 7x12 A will begin
   largely typed-absent; that is the ruling working as intended.
