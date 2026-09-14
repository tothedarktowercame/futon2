# Exact-subject review acceptance — blinded observation #2

Reviewer: claude-15 (authorized), 2026-09-14. Scope: codex-25 commit
1c5240b1. Verdict: OBSERVATION ACCEPTED. Finding of record:
:evidence-insufficient — again the correct result, and this time it
isolates the remaining gaps with precision.

Checked:

- Consulted list is exactly the ten permitted files with matching
  pins; referenced evidence ids correctly NOT followed; per-criterion
  arguments cite manifest ids + exact blinded fields; the still-live
  keyword acknowledged as a boundary decision and never used as
  support evidence — exactly the discipline the rubric demands.
- UNREDACTED cross-check: all three gaps the observer named are real
  retention absences, not blinding artifacts. (1) The limb receipt's
  stdout/stderr digests pin bytes retained NOWHERE — integrity of
  something uninspectable. (2) The standing decision carries
  identity/decision/actors/time/evidence-ids but no account of HOW
  support increased. (3) The build cell's :reviews vector is EMPTY in
  the raw record: the first-round-approve path retains
  :validation {approved?, review-job, gate} but no review text — only
  revision rounds retain :text (exercise 1's attempt had a
  reject->revise cycle, which is why text existed there).
- :strengthened correctly falls: prior standing and the still-live
  boundary are now retained, but the review-grade support-gain
  account is genuinely absent. Insufficiency, not ambiguity: zero
  complete matches.

Institutional trajectory across the two exercises: exercise 1 -> the
JOINS were missing (fixed: occurrence, cutoff, deposits); exercise 2
-> the joins hold and the SEMANTIC CONTENT is missing. Retained gap
list for the next iteration:
1. limb receipts must retain their output bytes as admitted evidence
   (companion output file in evidence/, referenced by the receipt),
   not digests of unretained bytes;
2. the standing decision needs an :explanation of the support change,
   review-grade, not a keyword;
3. the build cell must retain first-round review text, not only
   revision-round text.
