# Independent review acceptance — close-retention writer wiring (slice 1 of 2)

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 8864fecf
(writer completion + controls), febc8930 (strict marker validation),
ba8bdbae (receipts), plus reviewer fix b0da1087.
Verdict: ACCEPTED.

Checked:

- File scope: full_loop_cohort.clj + its test namespace + receipts
  only; full_loop_runner.clj and close_retention.clj untouched.
- All four anchors verified in the diff and the append flow read in
  source: the completion lives in event-record, samples ONE
  :recorded-at and uses it for both :closed-at and :evidence-cutoff;
  the refusal throws inside the cohort lock BEFORE write-new! runs, so
  no closed event file exists after a refusal (tests assert file
  absence); the durable payload carries :close-retention and drops the
  raw marker; marker-absent closes keep their exact prior field set
  (asserted as key-set equality in the additive test).
- Strict marker validation (febc8930): the exact four-key set refuses
  caller-supplied :closed-at/:evidence-cutoff as
  :retention-input-shape-invalid. The receipts' retained failure shows
  WHY this commit exists: the first implementation's writer-side assoc
  silently OVERWROTE a forbidden caller :closed-at — fail-open — and
  the author's own control caught it. Honest trail, real finding.
- Controls: additive no-change, absent and observed happy paths with
  writer-timestamp equality and durable readback, and three
  refusal-with-no-file cases, including :state-evidence-not-admitted
  and :admitted-evidence-invalid through the writer path — contract
  acceptance note 1 discharged.
- Receipts: cohort kondo baseline-delta 0/0, full-driver parens,
  fresh-JVM 17 tests / 78 assertions exit 0 at febc8930.

Review fix applied by reviewer (b0da1087, not re-belled): a
:retention-inputs marker on a NON-close checkpoint passed through
event-record untouched and the raw marker was durably retained.
Refused now as :retention-marker-misplaced before the write, with a
control (marker on :selection refuses, no event file). Gates re-run:
kondo 0/0, parens OK, cohort tests 18 tests / 80 assertions exit 0.

Remaining for slice 2 (runner mint + thread): mint the occurrence at
the selection-discrimination/construction boundary, thread it to
close!, submit :retention-inputs with typed-absent state and model,
and stop discarding close-attempt!'s return. The commissioned
mechanism is ready on the writer side.
