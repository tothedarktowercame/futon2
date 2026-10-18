# War Machine readiness review — 18 October 2026

Reviewer: claude-4, for Joe, under the futon2/CLAUDE.md ground rules
(read-only; proposals only). Reviewed at futon2 `0975c7df7`, covering commits
since 2026-10-11 (395 commits, almost all without an independent reviewer).
Five read-only reviewers each took one stage of the click path; their full
reports, with file:line references and what each did and did not check, are in
`review-2026-10-18/`. Items marked **[checked]** below were re-read in the code
by claude-4 after the reviewer reported them.

Question asked: is the machine ready for the next live click, or will it hit
another predictable error right away?

## Verdict

Not ready. Selection looks sound on the live path. A live click would most
likely stop at target reading; if it got past that it would stop in the D4
open-cascade resume; and if the holes were somehow closed, the build/close
path it would then enter has had no passing end-to-end test since 10-15 and
does not receive the cascade's frontier. None of the stages after selection
can be held in the debugger, so ground rule 4 (repair live, `:retry`) cannot
be followed where the failures will occur. The ordinary-click budget is 0, so
`wm_click.sh` would refuse cleanly until Joe allocates more.

The recorded flight is good work and real evidence through compilation and
journal validation, but two of its inputs (the operational authoring and the
pattern-apply executor) exist only offline, and it never ran the production
seam (`execute-selected-cascade!`) or the D4 resume.

## Predicted stops, in the order a live click meets them

### Target reading (`selected_target_reading.clj`; reports 1 and 5)

1. **[checked] NullPointerException on a misquoted first answer.** When the
   offset step fails, `validate` returns without setting `last-output`;
   `cascade-failures` then calls `(.isFile output)` on nil (line 413). The
   click ends `:selected-target-reading-repair-threw` after one contact, with
   no correction round. Regression from `3037b082f`; untested.
2. **[checked] Offset diagnostics inverted.** Before `3037b082f` the
   correction prompt always carried the exact offset diagnostics; now they are
   sent only `(when (zero? (:exit validated0)) …)` (line 435), i.e. only when
   the Python validator passed. Offset errors, the commonest Zai failure on
   10-16, get a citation message and no offset detail.
3. **The first prompt never names `pattern_refs`** or says a citation is
   required (the validator contract calls it `pattern_references`). Earlier
   readings that cited were by the Codex `wm-author` seat; the one Zai reading
   (click 5) cited nothing. Every Zai reading will spend its single
   correction round on this.
4. **[checked] The corrected answer is not re-checked by `cascade-failures`**
   before publication (only the Python validator reruns), so a second
   citation-less answer is refused at publication, as in click 5. (Report 1
   reads publication as the third check; it is, but only after the
   correction round is spent.)
5. **One correction round, not held.** A wrong pattern id among nine fails
   the whole answer; a failed correction exchange falls back to the first
   answer and reports a misleading publication refusal.
6. **Decoder neighbours still refused:** prose before a bare object, a second
   fence in the trailing note, three backticks inside the JSON text. Two bare
   objects are accepted, contrary to the docstring. The `0a45789b8` rule would
   not have accepted the actual click-3 reply (it already had a trailing
   note), so click 4 rediscovered a failure visible in saved bytes; no test
   loads either saved Zai reply.

### Occurrence squares (report 1)

7. The square prompt asks for `occurrence_id` but shows data keyed
   `occurrence-id`; the author must copy a 64-character `analysis_digest`
   exactly, a check every test and the replay bypass by overwriting the field.
   Zai has never authored squares live, and square authoring has no
   correction round. The void case is handled the same live as in the replay.

### Post-selection cascade execution (reports 2, 3, 4; three independent findings)

8. **[checked] No live operational authoring.** Only `recorded_flight.clj`
   supplies `:cascade-operational-authoring`; live, `seam-authoring`
   (`cascade_decision.clj:1470`) returns empty clauses, so every cascade
   compiles all-holes and the `:cascade-ready` branch added in `0975c7df7`
   is unreachable.
9. **[checked, by reading] D4 handler built without the job ledger.**
   `open-cascade-resume-handler` (`full_loop_runtime.clj:104`) builds its
   Agency contacts from the options it is constructed with, before the runner
   mints `:registered-run/job-ledger`; `dispatch!` then throws
   (`full_loop_runner.clj:1699`). The selected-target-reading port has the
   fix for exactly this (comment at `full_loop_runtime.clj:206`). Test stubs
   the ledger-minting step.
10. **Hole-id keys.** Hole ids are vectors; JSON decisions come back with
    keyword keys, so the hole-set comparison (`open_cascade_resume.clj:215`)
    always fails. The test passes only because its hole id is `:h`.
11. **[checked] The retry ignores the repair.** The retry at
    `cascade_decision.clj:1670` reruns `execute-selected-cascade!` with the
    same `opts`, hence the same empty authoring and the same holes; `resume!`
    reports `:resumed` and the click closes `:incomplete`.
12. **No live `:wm/pattern-apply` executor.** `route-frontier` tags steps with
    `:full-loop-build-review-close`, which nothing reads. If a cascade did
    reach the build path, the `{:co-apply}` map is turned into a vector
    (`full_loop_runner.clj:2427`), the frontier never reaches the author, no
    `:enacted` journal entries are written, and a partly authored cascade
    counts as ready with its remaining holes dropped. Open question (report 3)
    for Joe and codex-68: does "routed with no executor behind it" meet the
    definition of done in E-implementing-G-over-cascades §10?

## Debugger coverage (report 4) [checked]

Selection and redecision run inside `run-phase!`; the selected-target reading,
`execute-selected-cascade!` and the D4 resume (`full_loop_runner.clj`
~6485-6521) do not. A failure in any of them ends the click. This is why
clicks 3-5 have no debugger dwell records.

## Recording and readiness

- `ee7275ffc` closes cascade machine errors (`:identity-forged`, validation,
  compilation, journal, routing) as `:abstained`; other unadmitted outcomes
  (e.g. `:selection-validation-failed`) still degrade to `:build-failed` /
  `:invalid-close-outcome` and lose the reason (click 1's record shows this).
- The readiness scan probes only the click-1 failure (class emission), checks
  serving identity only for `full_loop_runner.clj`, and its warrant list omits
  selected-target-reading, handoff-contract, the cascade seam and the
  open-cascade-resume tests. Budget and beta are not checked.
- The "ambient `:no-selection`" failures in `full-loop-runner-test` are real:
  since `772590b0a` (10-15) the fixtures fail the canonical-F gate, so build →
  review → grounding → close has no passing end-to-end test.
- Beta authority of the closed 10-17 campaign is re-registered on every click;
  slot counts are in memory and a JVM restart restores four. `declaration-for`
  uses a slot even when beta would carry. This bears on whether the next click
  is authorized at all (rule 5).
- Three copies of the seat-readiness rule disagree (the click-3 admission race).

## Lower severity

- Recorded tick evaluation reads `:trace-written?`; production writes
  `:traceWritten`, set when the selection trace is written, not at close.
- Class emission (`48e8ae2d0`) is a genuine model but reduces to probability
  of reaching the goal, which progress already scores; its CTAU gate checks
  shape and self-declared flags; normalisation differs between H=1 and H≥2.
- Q7 exporter counts every selected run's class certificate
  (`:status :not-supplied`) as an absence; Q7's walker ignores the enactment
  record. The preference receipt certifies one C while G consumes two. Q9's
  recorded reason is out of date.
- `6b9f02660`'s new identity refusals have no negative controls; its
  fallback is unreachable live but would build `reading-record-id` as `"?/..."`.
- `executable-cascade/save!` can throw untyped when two prints of the same
  value order sets differently.
- Trace records are ~11-12 MB; every write re-parses the day's file; a
  fingerprint drift would rebuild the index from 1.4 GB inside the serving JVM.
- Warrants named in the TN exist and match their commits (now stale, as
  expected); none minted for `03c8830af`, `3037b082f`, `48e8ae2d0` or the four
  newest commits. `GET /api/alpha/evidence/<id>` returned not-found for all
  eight.

## Suggested order of work (a proposal for codex-68, who decides)

1. Small fixes at target reading: nil guard (1), restore offset diagnostics
   (2), name `pattern_refs` and the citation rule in the first prompt (3),
   re-run `cascade-failures` on the corrected answer (4), the square prompt key
   (7). Regressions that load the two saved Zai replies byte-for-byte.
2. Put target reading, cascade execution and the D4 resume inside `run-phase!`
   so their failures hold for `:retry`.
3. Decide where live operational authoring comes from and make the D4 retry
   consume the reviewed answer (8, 11), with the ledger (9) and hole-id (10)
   fixes. A test that does not stub the retry.
4. Then the frontier hand-off into build/close (12) and a passing end-to-end
   `full-loop-runner-test`.
5. Extend the readiness scan to the stages above before any new click.
